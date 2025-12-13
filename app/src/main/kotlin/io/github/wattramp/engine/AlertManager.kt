package io.github.wattramp.engine

import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.TestPhase
import io.github.wattramp.data.ProtocolType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.TurnScreenOn

/**
 * Manages in-ride alerts and notifications during FTP tests.
 */
class AlertManager(private val extension: WattRampExtension) {

    private var lastAlertId: String? = null
    private var lastPhase: TestPhase? = null
    private var halfwayAlertShown = false
    private var final30Shown = false
    private var getReadyShown = false
    private var lowCadenceWarningCount = 0

    companion object {
        private const val ALERT_INTERVAL_CHANGE = "wattramp_interval_change"
        private const val ALERT_GET_READY = "wattramp_get_ready"
        private const val ALERT_GO = "wattramp_go"
        private const val ALERT_HALFWAY = "wattramp_halfway"
        private const val ALERT_FINAL_30 = "wattramp_final_30"
        private const val ALERT_COMPLETE = "wattramp_complete"
        private const val ALERT_PUSH_HARDER = "wattramp_push_harder"
        private const val ALERT_LOW_CADENCE = "wattramp_low_cadence"
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
        // Phase change alert
        if (state.phase != lastPhase) {
            onPhaseChange(state, soundEnabled, screenWakeEnabled)
            lastPhase = state.phase
            resetPhaseAlerts()
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

        showAlert(
            id = ALERT_INTERVAL_CHANGE,
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
            if (!getReadyShown && remainingMs in 9_000L..11_000L) {
                showAlert(
                    id = ALERT_GET_READY,
                    title = "Almost there!",
                    detail = "10 seconds remaining",
                    playSound = false,
                    wakeScreen = false
                )
                getReadyShown = true
            }
        }

        // Halfway alert
        val intervalDuration = state.currentInterval.durationMs
        val halfway = intervalDuration / 2
        val elapsedInInterval = intervalDuration - remainingMs

        if (motivationEnabled && !halfwayAlertShown &&
            elapsedInInterval >= halfway && elapsedInInterval < halfway + 2000
        ) {
            showAlert(
                id = ALERT_HALFWAY,
                title = "HALFWAY!",
                detail = "Keep pushing!",
                playSound = false,
                wakeScreen = false
            )
            halfwayAlertShown = true
        }

        // Final 30 seconds
        if (!final30Shown && remainingMs in 29_000L..31_000L) {
            showAlert(
                id = ALERT_FINAL_30,
                title = "FINAL 30 SECONDS!",
                detail = "Finish strong!",
                playSound = soundEnabled,
                wakeScreen = screenWakeEnabled
            )
            final30Shown = true
        }
    }

    private var lastRampStep = 0

    private fun checkRampStepAlerts(
        state: TestState.Running,
        soundEnabled: Boolean,
        screenWakeEnabled: Boolean
    ) {
        val currentStep = state.currentStep ?: return
        val targetPower = state.targetPower ?: return

        if (currentStep > lastRampStep && currentStep > 0) {
            showAlert(
                id = ALERT_INTERVAL_CHANGE,
                title = "RAMP UP!",
                detail = "Target: ${targetPower}W",
                playSound = soundEnabled,
                wakeScreen = screenWakeEnabled
            )
            lastRampStep = currentStep
        }
    }

    private var lowPowerWarningCount = 0

    @Suppress("UNUSED_PARAMETER")
    private fun checkPowerWarning(state: TestState.Running, soundEnabled: Boolean) {
        val target = state.targetPower ?: return
        if (target <= 0) return

        // Only warn if more than 15% below target
        val threshold = target * 0.85
        if (state.currentPower < threshold) {
            lowPowerWarningCount++
            // Show warning every 10 seconds of low power
            if (lowPowerWarningCount >= 10 && lowPowerWarningCount % 10 == 0) {
                showAlert(
                    id = ALERT_PUSH_HARDER,
                    title = "PUSH HARDER!",
                    detail = "Below target: ${state.currentPower}W < ${target}W",
                    playSound = false,
                    wakeScreen = false,
                    autoDismissMs = 3000L
                )
            }
        } else {
            lowPowerWarningCount = 0
        }
    }

    private fun checkCadenceWarning(state: TestState.Running, soundEnabled: Boolean) {
        if (state.cadence > 0 && state.isCadenceLow) {
            lowCadenceWarningCount++
            // Show warning every 10 seconds of low cadence
            if (lowCadenceWarningCount >= 5 && lowCadenceWarningCount % 10 == 0) {
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
            lowCadenceWarningCount = 0
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
                extension.karooSystem.dispatch(TurnScreenOn)
            }

            extension.karooSystem.dispatch(
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

            if (playSound) {
                // Note: PlayBeepPattern may need specific pattern configuration
                // This is a placeholder - actual implementation depends on Karoo SDK
            }

            lastAlertId = id
        } catch (e: Exception) {
            // Log error but don't crash
            android.util.Log.e("AlertManager", "Failed to show alert: ${e.message}")
        }
    }

    private fun resetPhaseAlerts() {
        halfwayAlertShown = false
        final30Shown = false
        getReadyShown = false
        lowPowerWarningCount = 0
    }

    /**
     * Reset all alert state for a new test.
     */
    fun reset() {
        lastAlertId = null
        lastPhase = null
        lastRampStep = 0
        resetPhaseAlerts()
    }
}
