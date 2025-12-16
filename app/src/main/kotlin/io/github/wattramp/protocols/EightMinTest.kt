package io.github.wattramp.protocols

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.data.PreferencesRepository
import java.util.UUID

/**
 * 8-Minute FTP Test Protocol
 *
 * Structure:
 * - 15 min warmup with accelerations
 * - 8 min MAX effort test #1
 * - 10 min recovery at 40-50% FTP
 * - 8 min MAX effort test #2
 * - 10 min cooldown at 50% FTP
 *
 * FTP = Average(8min_1, 8min_2) × 0.90
 */
class EightMinTest(
    private val currentFtp: Int = PreferencesRepository.DEFAULT_FTP
) : BaseTestProtocol() {

    companion object {
        private const val WARMUP_DURATION_MS = 15 * 60_000L
        private const val TEST_DURATION_MS = 8 * 60_000L
        private const val RECOVERY_DURATION_MS = 10 * 60_000L
        private const val COOLDOWN_DURATION_MS = 10 * 60_000L
        private const val MAX_TEST_SAMPLES = 600 // 8 minutes + buffer
    }

    // Bounded lists for test interval samples
    private val firstTestPowerSamples = java.util.concurrent.CopyOnWriteArrayList<Int>()
    private val secondTestPowerSamples = java.util.concurrent.CopyOnWriteArrayList<Int>()

    override val type = ProtocolType.EIGHT_MINUTE

    override val intervals: List<Interval> = listOf(
        Interval(
            name = "Warmup",
            phase = TestPhase.WARMUP,
            durationMs = WARMUP_DURATION_MS,
            targetPowerPercent = 0.60
        ),
        Interval(
            name = "8-MIN TEST #1",
            phase = TestPhase.TESTING,
            durationMs = TEST_DURATION_MS,
            targetPowerPercent = null // MAX effort
        ),
        Interval(
            name = "Recovery",
            phase = TestPhase.RECOVERY,
            durationMs = RECOVERY_DURATION_MS,
            targetPowerPercent = 0.45
        ),
        Interval(
            name = "8-MIN TEST #2",
            phase = TestPhase.TESTING_2,
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

    private val firstTestStartMs = WARMUP_DURATION_MS
    private val firstTestEndMs = firstTestStartMs + TEST_DURATION_MS
    private val secondTestStartMs = firstTestEndMs + RECOVERY_DURATION_MS
    private val secondTestEndMs = secondTestStartMs + TEST_DURATION_MS

    override fun getTargetPower(elapsedMs: Long, currentFtp: Int): Int? {
        val interval = getCurrentInterval(elapsedMs) ?: return null

        return when (interval.phase) {
            TestPhase.TESTING, TestPhase.TESTING_2 -> null // MAX effort
            else -> interval.targetPowerPercent?.let { (currentFtp * it).toInt() }
        }
    }

    override fun onPowerSample(power: Int, elapsedMs: Long) {
        // Use bounded base class method
        addPowerSample(power, elapsedMs)

        // Collect samples during test intervals with bounds
        when {
            elapsedMs in firstTestStartMs until firstTestEndMs -> {
                if (firstTestPowerSamples.size < MAX_TEST_SAMPLES) {
                    firstTestPowerSamples.add(power)
                }
            }
            elapsedMs in secondTestStartMs until secondTestEndMs -> {
                if (secondTestPowerSamples.size < MAX_TEST_SAMPLES) {
                    secondTestPowerSamples.add(power)
                }
            }
        }
    }

    override fun calculateFtp(method: PreferencesRepository.FtpCalcMethod): Int {
        if (firstTestPowerSamples.isEmpty() || secondTestPowerSamples.isEmpty()) {
            // If only one test completed, use that
            val samples = firstTestPowerSamples.ifEmpty { secondTestPowerSamples }
            if (samples.isEmpty()) return 0
            return (samples.average() * 0.90).toInt()
        }

        val avg1 = firstTestPowerSamples.average()
        val avg2 = secondTestPowerSamples.average()
        val combinedAvg = (avg1 + avg2) / 2

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.88
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.90
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.92
        }

        return (combinedAvg * coefficient).toInt()
    }

    override fun getTestResult(
        startTime: Long,
        previousFtp: Int?,
        method: PreferencesRepository.FtpCalcMethod
    ): TestResult {
        val calculatedFtp = calculateFtp(method)

        val avg1 = if (firstTestPowerSamples.isNotEmpty())
            firstTestPowerSamples.average().toInt() else 0
        val avg2 = if (secondTestPowerSamples.isNotEmpty())
            secondTestPowerSamples.average().toInt() else 0
        val combinedAvg = if (avg1 > 0 && avg2 > 0) (avg1 + avg2) / 2 else maxOf(avg1, avg2)

        val coefficient = when (method) {
            PreferencesRepository.FtpCalcMethod.CONSERVATIVE -> 0.88
            PreferencesRepository.FtpCalcMethod.STANDARD -> 0.90
            PreferencesRepository.FtpCalcMethod.AGGRESSIVE -> 0.92
        }

        return TestResult(
            id = UUID.randomUUID().toString(),
            timestamp = startTime,
            protocol = ProtocolType.EIGHT_MINUTE,
            calculatedFtp = calculatedFtp,
            previousFtp = previousFtp,
            maxOneMinutePower = null,
            averagePower = combinedAvg,
            testDurationMs = totalDurationMs,
            stepsCompleted = null,
            formula = "Avg($avg1, $avg2) x $coefficient = $calculatedFtp"
        )
    }

    override fun getProgressDisplay(elapsedMs: Long): String {
        val interval = getCurrentInterval(elapsedMs) ?: return "Complete"
        val phase = interval.phase

        return when (phase) {
            TestPhase.TESTING -> {
                val testElapsed = elapsedMs - firstTestStartMs
                val testRemaining = TEST_DURATION_MS - testElapsed
                formatTime(testRemaining) + " #1"
            }
            TestPhase.TESTING_2 -> {
                val testElapsed = elapsedMs - secondTestStartMs
                val testRemaining = TEST_DURATION_MS - testElapsed
                formatTime(testRemaining) + " #2"
            }
            else -> interval.name
        }
    }

    private fun formatTime(ms: Long): String {
        val minutes = (ms / 60_000).toInt()
        val seconds = ((ms % 60_000) / 1000).toInt()
        return "${minutes}:${seconds.toString().padStart(2, '0')}"
    }

    fun isInFirstTest(elapsedMs: Long): Boolean {
        return elapsedMs in firstTestStartMs until firstTestEndMs
    }

    fun isInSecondTest(elapsedMs: Long): Boolean {
        return elapsedMs in secondTestStartMs until secondTestEndMs
    }

    fun isInAnyTest(elapsedMs: Long): Boolean {
        return isInFirstTest(elapsedMs) || isInSecondTest(elapsedMs)
    }

    fun getFirstTestAverage(): Int? {
        return if (firstTestPowerSamples.isNotEmpty())
            firstTestPowerSamples.average().toInt() else null
    }

    fun getSecondTestAverage(): Int? {
        return if (secondTestPowerSamples.isNotEmpty())
            secondTestPowerSamples.average().toInt() else null
    }

    override fun reset() {
        super.reset()
        firstTestPowerSamples.clear()
        secondTestPowerSamples.clear()
    }
}
