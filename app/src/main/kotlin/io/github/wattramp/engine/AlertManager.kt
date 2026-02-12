package io.github.wattramp.engine

import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.ProtocolType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.TurnScreenOn
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages in-ride alerts and notifications during FTP tests.
 * Thread-safe implementation using atomic types.
 */
class AlertManager(private val extension: WattRampExtension) {

    // Thread-safe state tracking using atomic types
    private val lastPhase = AtomicReference<TestPhase?>(null)
    private val halfwayAlertShown = AtomicBoolean(false)
    private val final30Shown = AtomicBoolean(false)
    private val getReadyShown = AtomicBoolean(false)
    private val lowCadenceWarningCount = AtomicInteger(0)
    private val powerLostAlertShown = AtomicBoolean(false)

    companion object {
        private const val ALERT_INTERVAL_CHANGE = "wattramp_interval_change"
        private const val ALERT_GET_READY = "wattramp_get_ready"
        private const val ALERT_GO = "wattramp_go"
        private const val ALERT_HALFWAY = "wattramp_halfway"
        private const val ALERT_FINAL_30 = "wattramp_final_30"
        private const val ALERT_COMPLETE = "wattramp_complete"
        private const val ALERT_PUSH_HARDER = "wattramp_push_harder"
        private const val ALERT_LOW_CADENCE = "wattramp_low_cadence"
        private const val ALERT_POWER_LOST = "wattramp_power_lost"

        // Power dropout threshold in milliseconds
        private const val POWER_DROPOUT_THRESHOLD_MS = 5000L
    }

    /**
     * Check and trigger alerts based on current test state.
     */
    fun checkAlerts(
        state: TestState.Running,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean,
        motivationEnabled: Boolean
    ) {
        // Phase change alert - use compareAndSet for thread safety
        val previousPhase = lastPhase.get()
        if (state.phase != previousPhase) {
            if (lastPhase.compareAndSet(previousPhase, state.phase)) {
                onPhaseChange(state, soundEnabled, screenWakeEnabled)
                resetPhaseAlerts()
            }
        }

        // Time-based alerts during test intervals
        if (state.phase.isTestInterval) {
            checkTestIntervalAlerts(state, soundEnabled, screenWakeEnabled, motivationEnabled)
        }

        // Ramp step change
        if (state.protocol == ProtocolType.RAMP && state.phase == TestPhase.TESTING) {
            checkRampStepAlerts(state, soundEnabled, screenWakeEnabled)
        }

        // Power too low warning
        if (state.targetPower != null && state.targetPower > 0) {
            checkPowerWarning(state, soundEnabled)
        }

        // Cadence warning (only during test phases)
        if (state.phase.isTestInterval) {
            checkCadenceWarning(state, soundEnabled)
        }
    }

    private fun onPhaseChange(
        state: TestState.Running,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean
    ) {
        val (title, detail) = when (state.phase) {
            TestPhase.WARMUP -> "WARMUP" to "Easy spinning to prepare"
            TestPhase.BLOWOUT -> "BLOW-OUT" to "High intensity for 5 minutes"
            TestPhase.RECOVERY -> "RECOVERY" to "Easy spinning, recover"
            TestPhase.TESTING -> "GO! TEST STARTED" to "Give it everything!"
            TestPhase.TESTING_2 -> "GO! TEST #2" to "One more effort!"
            TestPhase.COOLDOWN -> "COOLDOWN" to "Easy spinning, well done!"
            else -> return
        }

        // Use ALERT_GO for test start phases to trigger distinctive double-beep
        val alertId = if (state.phase == TestPhase.TESTING || state.phase == TestPhase.TESTING_2) {
            ALERT_GO
        } else {
            ALERT_INTERVAL_CHANGE
        }

        showAlert(
            id = alertId,
            title = title,
            detail = detail,
            playSound = soundEnabled && state.phase.isTestInterval,
            wakeScreen = screenWakeEnabled
        )
    }

    private fun checkTestIntervalAlerts(
        state: TestState.Running,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean,
        motivationEnabled: Boolean
    ) {
        val remainingMs = state.timeRemainingInInterval

        // Get ready alert (10 seconds before phase change)
        // Only for non-Ramp tests where we know the duration
        if (state.protocol != ProtocolType.RAMP) {
            if (remainingMs in 9_000L..11_000L && getReadyShown.compareAndSet(false, true)) {
                showAlert(
                    id = ALERT_GET_READY,
                    title = "Almost there!",
                    detail = "10 seconds remaining",
                    playSound = false,
                    wakeScreen = false
                )
            }
        }

        // Halfway alert
        val intervalDuration = state.currentInterval.durationMs
        val halfway = intervalDuration / 2
        val elapsedInInterval = intervalDuration - remainingMs

        if (motivationEnabled &&
            elapsedInInterval >= halfway && elapsedInInterval < halfway + 2000 &&
            halfwayAlertShown.compareAndSet(false, true)
        ) {
            showAlert(
                id = ALERT_HALFWAY,
                title = "HALFWAY!",
                detail = "Keep pushing!",
                playSound = false,
                wakeScreen = false
            )
        }

        // Final 30 seconds
        if (remainingMs in 29_000L..31_000L && final30Shown.compareAndSet(false, true)) {
            showAlert(
                id = ALERT_FINAL_30,
                title = "FINAL 30 SECONDS!",
                detail = "Finish strong!",
                playSound = soundEnabled,
                wakeScreen = screenWakeEnabled
            )
        }
    }

    private val lastRampStep = AtomicInteger(0)
    private val lowPowerWarningCount = AtomicInteger(0)

    private fun checkRampStepAlerts(
        state: TestState.Running,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean
    ) {
        val currentStep = state.currentStep ?: return
        val targetPower = state.targetPower ?: return

        val previousStep = lastRampStep.get()
        if (currentStep > previousStep && currentStep > 0) {
            if (lastRampStep.compareAndSet(previousStep, currentStep)) {
                showAlert(
                    id = ALERT_INTERVAL_CHANGE,
                    title = "RAMP UP!",
                    detail = "Target: ${targetPower}W",
                    playSound = soundEnabled,
                    wakeScreen = screenWakeEnabled
                )
            }
        }
    }

    private fun checkPowerWarning(state: TestState.Running, soundEnabled: Boolean) {
        val target = state.targetPower ?: return
        if (target <= 0) return

        // Only warn if more than 15% below target
        val threshold = target * 0.85
        if (state.currentPower < threshold) {
            val count = lowPowerWarningCount.incrementAndGet()
            // Show warning every 10 seconds of low power
            if (count >= 10 && count % 10 == 0) {
                showAlert(
                    id = ALERT_PUSH_HARDER,
                    title = "PUSH HARDER!",
                    detail = "Below target: ${state.currentPower}W < ${target}W",
                    playSound = soundEnabled,
                    wakeScreen = false,
                    autoDismissMs = 3000L
                )
            }
        } else {
            lowPowerWarningCount.set(0)
        }
    }

    private fun checkCadenceWarning(state: TestState.Running, soundEnabled: Boolean) {
        if (state.cadence > 0 && state.isCadenceLow) {
            val count = lowCadenceWarningCount.incrementAndGet()
            // Show warning every 10 seconds of low cadence
            if (count >= 5 && count % 10 == 0) {
                showAlert(
                    id = ALERT_LOW_CADENCE,
                    title = "LOW CADENCE!",
                    detail = "${state.cadence} RPM - increase rhythm",
                    playSound = soundEnabled && state.isCadenceCritical,
                    wakeScreen = false,
                    autoDismissMs = 3000L
                )
            }
        } else {
            lowCadenceWarningCount.set(0)
        }
    }

    /**
     * Check if power sensor data has been lost and show alert.
     * Called from TestEngine with the timestamp of last received power data.
     */
    fun checkPowerSensorDropout(
        lastPowerReceivedMs: Long,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean
    ) {
        if (lastPowerReceivedMs == 0L) return

        val timeSinceLastPower = System.currentTimeMillis() - lastPowerReceivedMs

        if (timeSinceLastPower > POWER_DROPOUT_THRESHOLD_MS) {
            // Show alert only once until power returns
            if (powerLostAlertShown.compareAndSet(false, true)) {
                showAlert(
                    id = ALERT_POWER_LOST,
                    title = "⚠️ POWER LOST",
                    detail = "Check your power meter connection",
                    playSound = soundEnabled,
                    wakeScreen = screenWakeEnabled,
                    autoDismissMs = null // Don't auto-dismiss - important alert
                )
            }
        } else {
            // Power is back - reset the alert flag
            powerLostAlertShown.set(false)
        }
    }

    /**
     * Show test complete alert with results.
     */
    fun showCompleteAlert(
        calculatedFtp: Int,
        previousFtp: Int?,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean
    ) {
        val change = previousFtp?.let { calculatedFtp - it }
        val changeText = change?.let {
            val sign = if (it >= 0) "+" else ""
            " ($sign${it}W)"
        } ?: ""

        showAlert(
            id = ALERT_COMPLETE,
            title = "TEST COMPLETE!",
            detail = "FTP: ${calculatedFtp}W$changeText",
            playSound = soundEnabled,
            wakeScreen = screenWakeEnabled,
            autoDismissMs = null // Don't auto-dismiss
        )
    }

    private fun showAlert(
        id: String,
        title: String,
        detail: String?,
        playSound: Boolean,
        wakeScreen: Boolean,
        autoDismissMs: Long? = 5000L
    ) {
        try {
            if (wakeScreen) {
                val screenResult = extension.karooSystem.dispatch(TurnScreenOn)
                if (!screenResult) {
                    android.util.Log.w("AlertManager", "Failed to turn screen on")
                }
            }

            val alertResult = extension.karooSystem.dispatch(
                InRideAlert(
                    id = id,
                    icon = R.drawable.ic_wattramp,
                    title = title,
                    detail = detail,
                    autoDismissMs = autoDismissMs,
                    backgroundColor = R.color.alert_bg,
                    textColor = R.color.alert_text
                )
            )
            if (!alertResult) {
                android.util.Log.w("AlertManager", "Failed to dispatch alert: $id")
            }

            // Play beep sound using Karoo's internal beeper
            if (playSound) {
                try {
                    // PlayBeepPattern takes a list of Tones with frequency and duration
                    // 1000Hz is a clear audible frequency
                    val tones = when (id) {
                        ALERT_GO, ALERT_COMPLETE -> listOf(
                            PlayBeepPattern.Tone(frequency = 1000, durationMs = 200),
                            PlayBeepPattern.Tone(frequency = null, durationMs = 100), // Silence
                            PlayBeepPattern.Tone(frequency = 1000, durationMs = 200)
                        )
                        ALERT_FINAL_30 -> listOf(
                            PlayBeepPattern.Tone(frequency = 1200, durationMs = 200),
                            PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                            PlayBeepPattern.Tone(frequency = 1200, durationMs = 200),
                            PlayBeepPattern.Tone(frequency = null, durationMs = 100),
                            PlayBeepPattern.Tone(frequency = 1200, durationMs = 200)
                        )
                        else -> listOf(
                            PlayBeepPattern.Tone(frequency = 800, durationMs = 150)
                        )
                    }
                    val beepResult = extension.karooSystem.dispatch(PlayBeepPattern(tones))
                    if (!beepResult) {
                        android.util.Log.w("AlertManager", "Failed to play beep for alert: $id")
                    }
                } catch (e: Exception) {
                    android.util.Log.w("AlertManager", "Failed to play beep: ${e.message}")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AlertManager", "Failed to show alert: ${e.message}")
        }
    }

    private fun resetPhaseAlerts() {
        halfwayAlertShown.set(false)
        final30Shown.set(false)
        getReadyShown.set(false)
        lowPowerWarningCount.set(0)
        powerLostAlertShown.set(false)
    }

    /**
     * Reset all alert state for a new test.
     */
    fun reset() {
        lastPhase.set(null)
        lastRampStep.set(0)
        resetPhaseAlerts()
    }
}
