package io.github.wattramp.protocols

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.data.PreferencesRepository
import java.util.UUID

/**
 * Classic 20-Minute FTP Test Protocol
 *
 * Structure:
 * - 20 min warmup at 50-65% FTP
 * - 5 min blow-out at 105-110% FTP
 * - 5 min recovery at 50% FTP
 * - 20 min MAX effort test
 * - 10 min cooldown at 50% FTP
 *
 * FTP = 20-min Average Power × 0.95
 */
class TwentyMinTest(
    private val currentFtp: Int = 200
) : BaseTestProtocol() {

    companion object {
        private const val WARMUP_DURATION_MS = 20 * 60_000L
        private const val BLOWOUT_DURATION_MS = 5 * 60_000L
        private const val RECOVERY_DURATION_MS = 5 * 60_000L
        private const val TEST_DURATION_MS = 20 * 60_000L
        private const val COOLDOWN_DURATION_MS = 10 * 60_000L
        private const val MAX_TEST_SAMPLES = 1500 // 20 minutes + buffer
    }

    // Bounded list for test interval samples only
    private val testPowerSamples = java.util.concurrent.CopyOnWriteArrayList<Int>()

    override val type = ProtocolType.TWENTY_MINUTE

    override val intervals: List<Interval> = listOf(
        Interval(
            name = "Warmup",
            phase = TestPhase.WARMUP,
            durationMs = WARMUP_DURATION_MS,
            targetPowerPercent = 0.55
        ),
        Interval(
            name = "Blow-out",
            phase = TestPhase.BLOWOUT,
            durationMs = BLOWOUT_DURATION_MS,
            targetPowerPercent = 1.05
        ),
        Interval(
            name = "Recovery",
            phase = TestPhase.RECOVERY,
            durationMs = RECOVERY_DURATION_MS,
            targetPowerPercent = 0.50
        ),
        Interval(
            name = "20-MIN TEST",
            phase = TestPhase.TESTING,
            durationMs = TEST_DURATION_MS,
            targetPowerPercent = null // MAX effort
        ),
        Interval(
            name = "Cooldown",
            phase = TestPhase.COOLDOWN,
            durationMs = COOLDOWN_DURATION_MS,
            targetPowerPercent = 0.50
        )
    )

    private val testIntervalStartMs: Long
        get() = WARMUP_DURATION_MS + BLOWOUT_DURATION_MS + RECOVERY_DURATION_MS

    private val testIntervalEndMs: Long
        get() = testIntervalStartMs + TEST_DURATION_MS

    override fun getTargetPower(elapsedMs: Long, currentFtp: Int): Int? {
        val interval = getCurrentInterval(elapsedMs) ?: return null

        return when (interval.phase) {
            TestPhase.TESTING -> null // MAX effort - no target
            else -> interval.targetPowerPercent?.let { (currentFtp * it).toInt() }
        }
    }

    override fun onPowerSample(power: Int, elapsedMs: Long) {
        // Use bounded base class method
        addPowerSample(power, elapsedMs)

        // Collect samples only during the 20-min test interval with bounds
        if (elapsedMs in testIntervalStartMs until testIntervalEndMs) {
            if (testPowerSamples.size < MAX_TEST_SAMPLES) {
                testPowerSamples.add(power)
            }
        }
    }

    override fun calculateFtp(method: PreferencesRepository.FtpCalcMethod): Int {
        if (testPowerSamples.isEmpty()) return 0

        val avgPower = testPowerSamples.average()
        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.93
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.95
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.97
        }

        return (avgPower * coefficient).toInt()
    }

    override fun getTestResult(
        startTime: Long,
        previousFtp: Int?,
        method: PreferencesRepository.FtpCalcMethod
    ): TestResult {
        val calculatedFtp = calculateFtp(method)
        val avgPower = testPowerSamples.average().toInt()

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.93
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.95
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.97
        }

        return TestResult(
            id = UUID.randomUUID().toString(),
            timestamp = startTime,
            protocol = ProtocolType.TWENTY_MINUTE,
            calculatedFtp = calculatedFtp,
            previousFtp = previousFtp,
            maxOneMinutePower = null,
            averagePower = avgPower,
            testDurationMs = totalDurationMs,
            stepsCompleted = null,
            formula = "$avgPower x $coefficient = $calculatedFtp"
        )
    }

    override fun getProgressDisplay(elapsedMs: Long): String {
        val interval = getCurrentInterval(elapsedMs) ?: return "Complete"

        return when (interval.phase) {
            TestPhase.TESTING -> {
                val testElapsed = elapsedMs - testIntervalStartMs
                val testRemaining = TEST_DURATION_MS - testElapsed
                val minutes = (testRemaining / 60_000).toInt()
                val seconds = ((testRemaining % 60_000) / 1000).toInt()
                "${minutes}:${seconds.toString().padStart(2, '0')}"
            }
            else -> interval.name
        }
    }

    fun getTestTimeRemaining(elapsedMs: Long): Long {
        if (elapsedMs < testIntervalStartMs) return TEST_DURATION_MS
        if (elapsedMs >= testIntervalEndMs) return 0
        return testIntervalEndMs - elapsedMs
    }

    fun getTestTimeElapsed(elapsedMs: Long): Long {
        if (elapsedMs < testIntervalStartMs) return 0
        if (elapsedMs >= testIntervalEndMs) return TEST_DURATION_MS
        return elapsedMs - testIntervalStartMs
    }

    fun isInTestInterval(elapsedMs: Long): Boolean {
        return elapsedMs in testIntervalStartMs until testIntervalEndMs
    }

    override fun reset() {
        super.reset()
        testPowerSamples.clear()
    }
}
