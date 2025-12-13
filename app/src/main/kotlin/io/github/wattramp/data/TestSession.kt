package io.github.wattramp.data

import kotlinx.serialization.Serializable

/**
 * Represents the current state of an active FTP test session.
 */
@Serializable
data class TestSession(
    val protocol: ProtocolType,
    val startTimeMs: Long,
    val currentPhase: TestPhase,
    val currentIntervalIndex: Int = 0,
    val elapsedTimeMs: Long = 0,
    val powerSamples: List<Int> = emptyList(),
    val maxOneMinutePower: Double = 0.0,
    val testIntervalPowerSamples: List<Int> = emptyList(),
    val secondTestIntervalPowerSamples: List<Int> = emptyList(), // For 8-min test
    val isActive: Boolean = true
) {
    val currentTargetPower: Int
        get() = when (protocol) {
            ProtocolType.RAMP -> calculateRampTargetPower()
            ProtocolType.TWENTY_MINUTE -> 0 // MAX effort during test
            ProtocolType.EIGHT_MINUTE -> 0 // MAX effort during test
        }

    private fun calculateRampTargetPower(): Int {
        // Will be calculated by protocol
        return 0
    }
}

@Serializable
enum class ProtocolType {
    RAMP,
    TWENTY_MINUTE,
    EIGHT_MINUTE;

    val displayName: String
        get() = when (this) {
            RAMP -> "Ramp Test"
            TWENTY_MINUTE -> "20-Minute Test"
            EIGHT_MINUTE -> "8-Minute Test"
        }

    /** Short name for compact displays (Karoo 2/3) */
    val shortName: String
        get() = when (this) {
            RAMP -> "RAMP"
            TWENTY_MINUTE -> "20MIN"
            EIGHT_MINUTE -> "8MIN"
        }

    val ftpCoefficient: Double
        get() = when (this) {
            RAMP -> 0.75
            TWENTY_MINUTE -> 0.95
            EIGHT_MINUTE -> 0.90
        }
}

@Serializable
enum class TestPhase {
    IDLE,
    WARMUP,
    BLOWOUT,      // 20-min test only
    RECOVERY,     // 20-min and 8-min tests
    TESTING,
    TESTING_2,    // 8-min test second interval
    COOLDOWN,
    COMPLETED,
    FAILED;

    val isTestInterval: Boolean
        get() = this == TESTING || this == TESTING_2

    val displayName: String
        get() = when (this) {
            IDLE -> "Ready"
            WARMUP -> "Warmup"
            BLOWOUT -> "Blow-out"
            RECOVERY -> "Recovery"
            TESTING -> "Testing"
            TESTING_2 -> "Testing #2"
            COOLDOWN -> "Cooldown"
            COMPLETED -> "Completed"
            FAILED -> "Failed"
        }
}

/**
 * Represents a single interval in a test protocol.
 */
@Serializable
data class Interval(
    val name: String,
    val phase: TestPhase,
    val durationMs: Long,
    val targetPowerPercent: Double?, // null = max effort, percentage of FTP
    val isRamp: Boolean = false,
    val rampStartPower: Int = 0,
    val rampStepWatts: Int = 0
) {
    companion object {
        const val ONE_MINUTE_MS = 60_000L
        const val FIVE_MINUTES_MS = 5 * ONE_MINUTE_MS
        const val EIGHT_MINUTES_MS = 8 * ONE_MINUTE_MS
        const val TEN_MINUTES_MS = 10 * ONE_MINUTE_MS
        const val FIFTEEN_MINUTES_MS = 15 * ONE_MINUTE_MS
        const val TWENTY_MINUTES_MS = 20 * ONE_MINUTE_MS
    }
}
