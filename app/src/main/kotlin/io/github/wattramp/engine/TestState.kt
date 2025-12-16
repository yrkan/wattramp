package io.github.wattramp.engine

import io.github.wattramp.data.Interval
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.TestResult

/**
 * Represents the current state of the test engine.
 */
sealed class TestState {
    /**
     * No test is running.
     */
    data object Idle : TestState()

    /**
     * Test is actively running.
     */
    data class Running(
        val protocol: ProtocolType,
        val phase: TestPhase,
        val currentInterval: Interval,
        val intervalIndex: Int,
        val elapsedMs: Long,
        val timeRemainingInInterval: Long,
        val currentPower: Int,
        val targetPower: Int?,
        val currentStep: Int? = null, // For Ramp test
        val estimatedTotalSteps: Int? = null, // For Ramp test
        val maxOneMinutePower: Int = 0,
        val averagePower: Int = 0,
        // Heart rate and cadence from Karoo
        val heartRate: Int = 0,
        val cadence: Int = 0,
        // User's max HR from Karoo profile for accurate zone calculations
        val userMaxHr: Int = 190
    ) : TestState() {

        // HR zone calculation based on user's actual max HR from Karoo profile
        val hrZone: Int
            get() {
                if (heartRate == 0) return 0
                val maxHr = userMaxHr.coerceAtLeast(150) // Sanity check
                val percentOfMax = (heartRate.toDouble() / maxHr) * 100
                return when {
                    percentOfMax < 60 -> 1  // Zone 1: Recovery
                    percentOfMax < 70 -> 2  // Zone 2: Endurance
                    percentOfMax < 80 -> 3  // Zone 3: Tempo
                    percentOfMax < 90 -> 4  // Zone 4: Threshold
                    else -> 5               // Zone 5: VO2max
                }
            }

        // Cadence warning thresholds
        val isCadenceLow: Boolean
            get() = cadence in 1..69

        val isCadenceCritical: Boolean
            get() = cadence in 1..59

        val isInTargetZone: Boolean
            get() = targetPower?.let { target ->
                val tolerance = target * 0.05
                currentPower >= (target - tolerance) && currentPower <= (target + tolerance)
            } ?: true // No target = always in zone

        val deviation: Int
            get() = targetPower?.let { currentPower - it } ?: 0

        val deviationPercent: Double
            get() = targetPower?.let {
                if (it > 0) ((currentPower - it).toDouble() / it) * 100 else 0.0
            } ?: 0.0

        // Accurate progress calculation based on protocol type and total duration
        val progressPercent: Double
            get() = when (protocol) {
                ProtocolType.RAMP -> {
                    // For Ramp test: estimate based on current step vs expected
                    currentStep?.let { step ->
                        estimatedTotalSteps?.let { total ->
                            if (total > 0) (step.toDouble() / total) * 100 else 0.0
                        }
                    } ?: 0.0
                }
                ProtocolType.TWENTY_MINUTE -> {
                    // Total duration: 20 + 5 + 5 + 20 + 10 = 60 minutes
                    val totalDurationMs = 60 * 60_000L
                    (elapsedMs.toDouble() / totalDurationMs) * 100
                }
                ProtocolType.EIGHT_MINUTE -> {
                    // Total duration: 15 + 8 + 10 + 8 + 10 = 51 minutes
                    val totalDurationMs = 51 * 60_000L
                    (elapsedMs.toDouble() / totalDurationMs) * 100
                }
            }.coerceIn(0.0, 100.0)
    }

    /**
     * Test is paused (ride paused).
     */
    data class Paused(
        val protocol: ProtocolType,
        val elapsedMs: Long,
        val pausedAt: Long
    ) : TestState()

    /**
     * Test has completed successfully.
     */
    data class Completed(
        val result: TestResult
    ) : TestState()

    /**
     * Test was stopped or failed.
     */
    data class Failed(
        val protocol: ProtocolType,
        val reason: FailureReason,
        val partialResult: TestResult?
    ) : TestState()
}

enum class FailureReason {
    USER_STOPPED,
    POWER_DROPOUT,
    RIDE_ENDED,
    ERROR
}
