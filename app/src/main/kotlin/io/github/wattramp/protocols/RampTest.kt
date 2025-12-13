package io.github.wattramp.protocols

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.data.PreferencesRepository
import java.util.ArrayDeque
import java.util.UUID

/**
 * Ramp Test Protocol
 *
 * Progressive test with power increasing every minute until failure.
 * FTP = Max 1-minute Power × 0.75
 */
class RampTest(
    private val startPower: Int = 100,
    private val stepIncrement: Int = 20,
    private val warmupDurationMin: Int = 5,
    private val cooldownDurationMin: Int = 5
) : BaseTestProtocol() {

    companion object {
        private const val STEP_DURATION_MS = 60_000L // 1 minute per step
        private const val MAX_STEPS = 30 // Safety limit: 30 steps max
        private const val POWER_DROP_THRESHOLD = 0.70 // 30% below target = failure
        private const val CONSECUTIVE_LOW_POWER_SECONDS = 10
    }

    private val warmupDurationMs = warmupDurationMin * 60_000L
    private val cooldownDurationMs = cooldownDurationMin * 60_000L

    // Rolling buffer for 1-minute power average
    private val oneMinuteBuffer = ArrayDeque<Int>(60)
    private var maxOneMinutePower = 0.0
    private var totalSteps = 0
    private var consecutiveLowPowerCount = 0
    private var testEndTimeMs: Long? = null

    override val type = ProtocolType.RAMP

    override val intervals: List<Interval>
        get() {
            val list = mutableListOf<Interval>()

            // Warmup
            list.add(
                Interval(
                    name = "Warmup",
                    phase = TestPhase.WARMUP,
                    durationMs = warmupDurationMs,
                    targetPowerPercent = 0.50
                )
            )

            // Ramp intervals (create enough for MAX_STEPS)
            for (step in 1..MAX_STEPS) {
                list.add(
                    Interval(
                        name = "Ramp Step $step",
                        phase = TestPhase.TESTING,
                        durationMs = STEP_DURATION_MS,
                        targetPowerPercent = null, // Calculated dynamically
                        isRamp = true,
                        rampStartPower = startPower,
                        rampStepWatts = stepIncrement
                    )
                )
            }

            // Cooldown
            list.add(
                Interval(
                    name = "Cooldown",
                    phase = TestPhase.COOLDOWN,
                    durationMs = cooldownDurationMs,
                    targetPowerPercent = 0.50
                )
            )

            return list
        }

    override fun getTargetPower(elapsedMs: Long, currentFtp: Int): Int? {
        val interval = getCurrentInterval(elapsedMs) ?: return null

        return when (interval.phase) {
            TestPhase.WARMUP -> startPower
            TestPhase.COOLDOWN -> startPower
            TestPhase.TESTING -> {
                val testElapsedMs = elapsedMs - warmupDurationMs
                if (testElapsedMs < 0) return startPower

                val currentStep = (testElapsedMs / STEP_DURATION_MS).toInt()
                startPower + (currentStep * stepIncrement)
            }
            else -> null
        }
    }

    override fun onPowerSample(power: Int, elapsedMs: Long) {
        powerSamples.add(PowerSample(power, elapsedMs))

        // Only track during test phase
        if (elapsedMs < warmupDurationMs) return

        // Add to rolling 1-minute buffer
        oneMinuteBuffer.addLast(power)
        if (oneMinuteBuffer.size > 60) {
            oneMinuteBuffer.removeFirst()
        }

        // Calculate current 1-minute average
        if (oneMinuteBuffer.size >= 60) {
            val currentAvg = oneMinuteBuffer.average()
            if (currentAvg > maxOneMinutePower) {
                maxOneMinutePower = currentAvg
            }
        }

        // Track steps completed
        val testElapsedMs = elapsedMs - warmupDurationMs
        totalSteps = ((testElapsedMs / STEP_DURATION_MS).toInt() + 1).coerceAtLeast(1)
    }

    override fun shouldEndTest(currentPower: Int, targetPower: Int): Boolean {
        if (targetPower <= 0) return false

        // Check if power dropped significantly
        val threshold = (targetPower * POWER_DROP_THRESHOLD).toInt()
        if (currentPower < threshold) {
            consecutiveLowPowerCount++
            if (consecutiveLowPowerCount >= CONSECUTIVE_LOW_POWER_SECONDS) {
                testEndTimeMs = System.currentTimeMillis()
                return true
            }
        } else {
            consecutiveLowPowerCount = 0
        }

        return false
    }

    override fun calculateFtp(method: PreferencesRepository.FtpCalcMethod): Int {
        if (maxOneMinutePower <= 0) {
            // Fallback: use max power from samples
            val maxPower = powerSamples
                .filter { it.timestampMs >= warmupDurationMs }
                .maxOfOrNull { it.power } ?: 0
            return (maxPower * 0.75).toInt()
        }

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.72
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.75
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.77
        }

        return (maxOneMinutePower * coefficient).toInt()
    }

    override fun getTestResult(
        startTime: Long,
        previousFtp: Int?,
        method: PreferencesRepository.FtpCalcMethod
    ): TestResult {
        val calculatedFtp = calculateFtp(method)
        val maxPower = maxOneMinutePower.toInt()

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.72
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.75
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.77
        }

        return TestResult(
            id = UUID.randomUUID().toString(),
            timestamp = startTime,
            protocol = ProtocolType.RAMP,
            calculatedFtp = calculatedFtp,
            previousFtp = previousFtp,
            maxOneMinutePower = maxPower,
            averagePower = null,
            testDurationMs = testEndTimeMs?.let { it - startTime }
                ?: (warmupDurationMs + (totalSteps * STEP_DURATION_MS)),
            stepsCompleted = totalSteps,
            formula = "$maxPower x $coefficient = $calculatedFtp"
        )
    }

    override fun getProgressDisplay(elapsedMs: Long): String {
        if (elapsedMs < warmupDurationMs) {
            return "Warmup"
        }

        val testElapsedMs = elapsedMs - warmupDurationMs
        val currentStep = (testElapsedMs / STEP_DURATION_MS).toInt() + 1
        return "Step $currentStep"
    }

    fun getCurrentStep(elapsedMs: Long): Int {
        if (elapsedMs < warmupDurationMs) return 0
        val testElapsedMs = elapsedMs - warmupDurationMs
        return (testElapsedMs / STEP_DURATION_MS).toInt() + 1
    }

    fun getEstimatedTotalSteps(currentFtp: Int): Int {
        // Estimate: test ends around 120% of FTP
        val estimatedMaxPower = currentFtp * 1.20
        return ((estimatedMaxPower - startPower) / stepIncrement).toInt() + 1
    }

    override fun reset() {
        super.reset()
        oneMinuteBuffer.clear()
        maxOneMinutePower = 0.0
        totalSteps = 0
        consecutiveLowPowerCount = 0
        testEndTimeMs = null
    }
}
