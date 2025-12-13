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
    val saved: Boolean = false   // Whether FTP was saved to Karoo
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
 */
@Serializable
data class TestHistoryData(
    val results: List<TestResult> = emptyList()
) {
    fun addResult(result: TestResult): TestHistoryData {
        return copy(results = listOf(result) + results)
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
