package io.github.wattramp.protocols

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.data.PreferencesRepository
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Ramp Test Protocol
 *
 * Progressive test with power increasing every minute until failure.
 * FTP = Max 1-minute Power × 0.75
 *
 * Thread-safe implementation using atomic types for mutable state.
 */
class RampTest(
    private val startPower: Int = PreferencesRepository.DEFAULT_RAMP_START,
    private val stepIncrement: Int = PreferencesRepository.DEFAULT_RAMP_STEP,
    private val warmupDurationMin: Int = PreferencesRepository.DEFAULT_WARMUP_DURATION,
    private val cooldownDurationMin: Int = PreferencesRepository.DEFAULT_COOLDOWN_DURATION
) : BaseTestProtocol() {

    companion object {
        private const val STEP_DURATION_MS = 60_000L // 1 minute per step
        private const val MAX_STEPS = 30 // Safety limit: 30 steps max
        private const val POWER_DROP_THRESHOLD = 0.70 // 30% below target = failure
        private const val CONSECUTIVE_LOW_POWER_SECONDS = 10
    }

    private val warmupDurationMs = warmupDurationMin * 60_000L
    private val cooldownDurationMs = cooldownDurationMin * 60_000L

    // Rolling buffer for 1-minute power average - synchronized access
    private val oneMinuteBuffer = ArrayDeque<Int>(65)
    private val bufferLock = Any()

    // Thread-safe state using atomic types
    private val maxOneMinutePower = AtomicReference(0.0)
    private val totalSteps = AtomicInteger(0)
    private val consecutiveLowPowerCount = AtomicInteger(0)
    private val testEndTimeMs = AtomicLong(0L) // 0 means not ended
    private val hasSeenValidPower = AtomicReference(false) // Track if we've seen real power data
    private val currentElapsedMs = AtomicLong(0L) // Track current elapsed time

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
        // Add to base class samples with bounds (handled by BaseTestProtocol)
        addPowerSample(power, elapsedMs)

        // Track elapsed time for shouldEndTest
        currentElapsedMs.set(elapsedMs)

        // Track if we've seen valid power (> 50W indicates actual pedaling)
        if (power > 50) {
            hasSeenValidPower.set(true)
        }

        // Only track for max power calculation during test phase
        if (elapsedMs < warmupDurationMs) return

        // Thread-safe update of rolling buffer
        synchronized(bufferLock) {
            oneMinuteBuffer.addLast(power)
            if (oneMinuteBuffer.size > 60) {
                oneMinuteBuffer.removeFirst()
            }

            // Calculate current 1-minute average
            if (oneMinuteBuffer.size >= 60) {
                val currentAvg = oneMinuteBuffer.average()
                // Atomically update max if current is higher
                maxOneMinutePower.updateAndGet { current ->
                    if (currentAvg > current) currentAvg else current
                }
            }
        }

        // Track steps completed
        val testElapsedMs = elapsedMs - warmupDurationMs
        totalSteps.set(((testElapsedMs / STEP_DURATION_MS).toInt() + 1).coerceAtLeast(1))
    }

    override fun shouldEndTest(currentPower: Int, targetPower: Int): Boolean {
        if (targetPower <= 0) return false

        // Don't end test during warmup phase
        val elapsed = currentElapsedMs.get()
        if (elapsed < warmupDurationMs) {
            consecutiveLowPowerCount.set(0)
            return false
        }

        // Don't end test if we haven't seen valid power data yet
        // This prevents false endings when sensors are still connecting
        if (!hasSeenValidPower.get()) {
            return false
        }

        // Check if power dropped significantly below target
        val threshold = (targetPower * POWER_DROP_THRESHOLD).toInt()
        if (currentPower < threshold) {
            val count = consecutiveLowPowerCount.incrementAndGet()
            if (count >= CONSECUTIVE_LOW_POWER_SECONDS) {
                testEndTimeMs.set(System.currentTimeMillis())
                return true
            }
        } else {
            consecutiveLowPowerCount.set(0)
        }

        return false
    }

    override fun calculateFtp(method: PreferencesRepository.FtpCalcMethod): Int {
        val maxPower = maxOneMinutePower.get()
        if (maxPower <= 0) {
            // Fallback: use max power from samples
            val maxSamplePower = getPowerSamplesAfter(warmupDurationMs)
                .maxOfOrNull { it.power } ?: 0
            return (maxSamplePower * 0.75).toInt()
        }

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.72
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.75
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.77
        }

        return (maxPower * coefficient).toInt()
    }

    override fun getTestResult(
        startTime: Long,
        previousFtp: Int?,
        method: PreferencesRepository.FtpCalcMethod
    ): TestResult {
        val calculatedFtp = calculateFtp(method)
        val maxPower = maxOneMinutePower.get().toInt()
        val steps = totalSteps.get()
        val endTime = testEndTimeMs.get()

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
            testDurationMs = if (endTime > 0) endTime - startTime
                else (warmupDurationMs + (steps * STEP_DURATION_MS)),
            stepsCompleted = steps,
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
        synchronized(bufferLock) {
            oneMinuteBuffer.clear()
        }
        maxOneMinutePower.set(0.0)
        totalSteps.set(0)
        consecutiveLowPowerCount.set(0)
        testEndTimeMs.set(0L)
        hasSeenValidPower.set(false)
        currentElapsedMs.set(0L)
    }
}
