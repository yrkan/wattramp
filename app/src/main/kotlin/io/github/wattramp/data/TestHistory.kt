package io.github.wattramp.data

import kotlinx.serialization.Serializable

/**
 * Represents a completed FTP test result stored in history.
 */
@Serializable
data class TestResult(
    val id: String,
    val timestamp: Long,
    val protocol: ProtocolType,
    val calculatedFtp: Int,
    val previousFtp: Int?,
    val maxOneMinutePower: Int?, // For Ramp test
    val averagePower: Int?,      // For 20-min and 8-min tests
    val testDurationMs: Long,
    val stepsCompleted: Int?,    // For Ramp test
    val formula: String,         // e.g., "329 x 0.75 = 247"
    val saved: Boolean = false,  // Whether FTP was saved to Karoo
    // Extended analytics (v1.4.0)
    val normalizedPower: Int? = null,     // NP: 30-sec rolling avg to 4th power
    val variabilityIndex: Double? = null, // VI: NP / Avg Power (1.0-1.2 typical)
    val averageHeartRate: Int? = null,    // Avg HR during test
    val efficiencyFactor: Double? = null  // EF: NP / Avg HR
) {
    val ftpChange: Int?
        get() = previousFtp?.let { calculatedFtp - it }

    val ftpChangePercent: Double?
        get() = previousFtp?.let {
            if (it > 0) ((calculatedFtp - it).toDouble() / it) * 100 else null
        }
}

/**
 * Container for test history with serialization support.
 * Limits history to MAX_HISTORY_SIZE results to prevent unbounded growth.
 */
@Serializable
data class TestHistoryData(
    val results: List<TestResult> = emptyList()
) {
    companion object {
        const val MAX_HISTORY_SIZE = 100
    }

    fun addResult(result: TestResult): TestHistoryData {
        // Add new result at the beginning and limit total size
        val newResults = (listOf(result) + results).take(MAX_HISTORY_SIZE)
        return copy(results = newResults)
    }

    fun getLatestFtp(): Int? {
        return results.firstOrNull { it.saved }?.calculatedFtp
    }

    fun getResultsForProtocol(protocol: ProtocolType): List<TestResult> {
        return results.filter { it.protocol == protocol }
    }

    fun getResultsInDateRange(startMs: Long, endMs: Long): List<TestResult> {
        return results.filter { it.timestamp in startMs..endMs }
    }
}
