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
        val cadence: Int = 0
    ) : TestState() {

        // HR zone calculation (based on max HR estimate: 220 - age, default ~190)
        val hrZone: Int
            get() = when {
                heartRate == 0 -> 0
                heartRate < 114 -> 1  // < 60% of ~190
                heartRate < 133 -> 2  // 60-70%
                heartRate < 152 -> 3  // 70-80%
                heartRate < 171 -> 4  // 80-90%
                else -> 5             // > 90%
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

        val progressPercent: Double
            get() = when (protocol) {
                ProtocolType.RAMP -> {
                    // Estimate based on current step vs expected
                    currentStep?.let { step ->
                        estimatedTotalSteps?.let { total ->
                            (step.toDouble() / total) * 100
                        }
                    } ?: 0.0
                }
                else -> {
                    // Based on total duration
                    val totalDuration = currentInterval.durationMs * 5 // Rough estimate
                    (elapsedMs.toDouble() / totalDuration) * 100
                }
            }
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

/**
 * Events that can be sent to the TestEngine.
 */
sealed class TestEvent {
    data class StartTest(val protocol: ProtocolType) : TestEvent()
    data object StopTest : TestEvent()
    data object PauseTest : TestEvent()
    data object ResumeTest : TestEvent()
    data class PowerUpdate(val power: Int) : TestEvent()
    data class TimeUpdate(val elapsedMs: Long) : TestEvent()
    data object RideEnded : TestEvent()
}

/**
 * Effects that the TestEngine can produce.
 */
sealed class TestEffect {
    data class ShowAlert(
        val id: String,
        val title: String,
        val detail: String? = null,
        val playSound: Boolean = false,
        val wakeScreen: Boolean = false,
        val autoDismissMs: Long? = 5000L
    ) : TestEffect()

    data class UpdateDataFields(
        val state: TestState.Running
    ) : TestEffect()

    data class SaveResult(
        val result: TestResult
    ) : TestEffect()

    data object PlayBeep : TestEffect()
    data object WakeScreen : TestEffect()
}
