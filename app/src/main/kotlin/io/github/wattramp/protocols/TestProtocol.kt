package io.github.wattramp.protocols

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult
import io.github.wattramp.data.PreferencesRepository

/**
 * Interface for FTP test protocols.
 * Each protocol defines its structure, target power calculations, and FTP calculation.
 */
interface TestProtocol {
    val type: ProtocolType
    val intervals: List<Interval>
    val totalDurationMs: Long

    /**
     * Get the target power for a given elapsed time.
     * @return target power in watts, or null for max effort intervals
     */
    fun getTargetPower(elapsedMs: Long, currentFtp: Int): Int?

    /**
     * Get the current interval based on elapsed time.
     */
    fun getCurrentInterval(elapsedMs: Long): Interval?

    /**
     * Get the index of the current interval.
     */
    fun getCurrentIntervalIndex(elapsedMs: Long): Int

    /**
     * Get time remaining in the current interval.
     */
    fun getTimeRemainingInInterval(elapsedMs: Long): Long

    /**
     * Get the current test phase.
     */
    fun getCurrentPhase(elapsedMs: Long): TestPhase

    /**
     * Process a power sample during the test.
     */
    fun onPowerSample(power: Int, elapsedMs: Long)

    /**
     * Calculate FTP based on collected data.
     */
    fun calculateFtp(method: PreferencesRepository.FtpCalcMethod): Int

    /**
     * Check if the test should automatically end (for Ramp test when power drops).
     */
    fun shouldEndTest(currentPower: Int, targetPower: Int): Boolean

    /**
     * Get the test result after completion.
     */
    fun getTestResult(
        startTime: Long,
        previousFtp: Int?,
        method: PreferencesRepository.FtpCalcMethod
    ): TestResult

    /**
     * Get display string for current step/progress.
     */
    fun getProgressDisplay(elapsedMs: Long): String

    /**
     * Reset the protocol state for a new test.
     */
    fun reset()
}

/**
 * Base implementation with common functionality.
 * Includes bounded power sample storage to prevent unbounded memory growth.
 */
abstract class BaseTestProtocol : TestProtocol {
    protected var testStartTimeMs: Long = 0L

    // Bounded power sample storage using thread-safe list
    private val powerSamples = java.util.concurrent.CopyOnWriteArrayList<PowerSample>()
    private val samplesLock = Any()

    companion object {
        // Maximum samples to store (~67 minutes at 1 sample/sec)
        private const val MAX_POWER_SAMPLES = 4000
    }

    data class PowerSample(val power: Int, val timestampMs: Long)

    /**
     * Thread-safe method to add power sample with bounded storage.
     */
    protected fun addPowerSample(power: Int, timestampMs: Long) {
        if (powerSamples.size < MAX_POWER_SAMPLES) {
            powerSamples.add(PowerSample(power, timestampMs))
        }
    }

    /**
     * Get power samples after a specific timestamp.
     */
    protected fun getPowerSamplesAfter(afterMs: Long): List<PowerSample> {
        return powerSamples.filter { it.timestampMs >= afterMs }
    }

    /**
     * Get all power samples (snapshot).
     */
    protected fun getAllPowerSamples(): List<PowerSample> {
        return powerSamples.toList()
    }

    override val totalDurationMs: Long
        get() = intervals.sumOf { it.durationMs }

    override fun getCurrentInterval(elapsedMs: Long): Interval? {
        var accumulated = 0L
        for (interval in intervals) {
            accumulated += interval.durationMs
            if (elapsedMs < accumulated) {
                return interval
            }
        }
        return intervals.lastOrNull()
    }

    override fun getCurrentIntervalIndex(elapsedMs: Long): Int {
        var accumulated = 0L
        for ((index, interval) in intervals.withIndex()) {
            accumulated += interval.durationMs
            if (elapsedMs < accumulated) {
                return index
            }
        }
        return intervals.lastIndex.coerceAtLeast(0)
    }

    override fun getTimeRemainingInInterval(elapsedMs: Long): Long {
        var accumulated = 0L
        for (interval in intervals) {
            val intervalEnd = accumulated + interval.durationMs
            if (elapsedMs < intervalEnd) {
                return intervalEnd - elapsedMs
            }
            accumulated = intervalEnd
        }
        return 0L
    }

    override fun getCurrentPhase(elapsedMs: Long): TestPhase {
        return getCurrentInterval(elapsedMs)?.phase ?: TestPhase.COMPLETED
    }

    override fun shouldEndTest(currentPower: Int, targetPower: Int): Boolean {
        // Default: don't auto-end, subclasses can override
        return false
    }

    override fun reset() {
        testStartTimeMs = System.currentTimeMillis()
        powerSamples.clear() // Thread-safe clear of CopyOnWriteArrayList
    }

    protected fun getElapsedInCurrentInterval(elapsedMs: Long): Long {
        var accumulated = 0L
        for (interval in intervals) {
            val intervalEnd = accumulated + interval.durationMs
            if (elapsedMs < intervalEnd) {
                return elapsedMs - accumulated
            }
            accumulated = intervalEnd
        }
        return 0L
    }
}
