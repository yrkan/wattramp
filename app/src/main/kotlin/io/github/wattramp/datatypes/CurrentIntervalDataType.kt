package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing current interval information:
 * - Phase name (e.g., "RAMP STEP 7", "20-MIN TEST")
 * - Target power
 * - Time remaining
 * - Progress bars
 */
class CurrentIntervalDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "current-interval") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_current_interval_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_current_interval_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_current_interval_medium
        LayoutSize.LARGE -> R.layout.datatype_current_interval_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.phase_name, config, TextSizeCalculator.Role.SECONDARY)

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                views.setAdaptiveTextSize(R.id.progress_percent, config, TextSizeCalculator.Role.LABEL)
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setAdaptiveTextSize(R.id.progress_percent, config, TextSizeCalculator.Role.TERTIARY)
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                views.setAdaptiveTextSize(R.id.target_power, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.time_remaining, config, TextSizeCalculator.Role.LABEL)
            }
            LayoutSize.LARGE -> {
                views.setAdaptiveTextSize(R.id.target_power, config, TextSizeCalculator.Role.PRIMARY)
                views.setAdaptiveTextSize(R.id.time_remaining, config, TextSizeCalculator.Role.TERTIARY)
                views.setAdaptiveTextSize(R.id.progress_percent, config, TextSizeCalculator.Role.LABEL)
            }
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> updateIdleState(views)
            is TestState.Running -> updateRunningState(views, state)
            is TestState.Paused -> updatePausedState(views)
            is TestState.Completed -> updateCompletedState(views, state)
            is TestState.Failed -> updateFailedState(views, state)
        }
    }

    private fun updateIdleState(views: RemoteViews) {
        views.setTextViewText(R.id.phase_name, getString(R.string.df_no_test))

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                setIntervalProgress(views, 0)
                views.setTextViewText(R.id.target_power, "Start test")
                views.setTextViewText(R.id.time_remaining, "from app")
            }
            LayoutSize.LARGE -> {
                setIntervalProgress(views, 0)
                views.setTextViewText(R.id.target_power, "Start a test")
                views.setTextViewText(R.id.time_remaining, "from WattRamp app")
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
        }
    }

    private fun updateRunningState(views: RemoteViews, state: TestState.Running) {
        // Phase name
        val phaseName = when {
            state.currentStep != null -> "RAMP STEP ${state.currentStep}"
            else -> state.phase.displayName.uppercase()
        }
        views.setTextViewText(R.id.phase_name, phaseName)

        // Calculate progress values
        val testProgress = state.progressPercent.toInt().coerceIn(0, 100)
        val intervalDuration = state.currentInterval.durationMs
        val intervalElapsed = intervalDuration - state.timeRemainingInInterval
        val intervalProgress = if (intervalDuration > 0) {
            ((intervalElapsed.toDouble() / intervalDuration) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                setTestProgress(views, testProgress)
                views.setTextViewText(R.id.progress_percent, "${testProgress}%")
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                setTestProgress(views, testProgress)
                views.setTextViewText(R.id.progress_percent, "${testProgress}%")
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                setIntervalProgress(views, intervalProgress)
                val targetText = state.targetPower?.let { "Target: ${it}W" } ?: "MAX EFFORT"
                views.setTextViewText(R.id.target_power, targetText)

                val remainingSeconds = (state.timeRemainingInInterval / 1000).toInt()
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                views.setTextViewText(R.id.time_remaining, "${minutes}:${seconds.toString().padStart(2, '0')} remaining")
            }
            LayoutSize.LARGE -> {
                setIntervalProgress(views, intervalProgress)
                val targetText = state.targetPower?.let { "Target: ${it}W" } ?: "MAX EFFORT"
                views.setTextViewText(R.id.target_power, targetText)

                val remainingSeconds = (state.timeRemainingInInterval / 1000).toInt()
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                views.setTextViewText(R.id.time_remaining, "${minutes}:${seconds.toString().padStart(2, '0')} remaining")

                setTestProgress(views, testProgress)
                views.setTextViewText(R.id.progress_percent, "${testProgress}%")
            }
        }
    }

    private fun updatePausedState(views: RemoteViews) {
        views.setTextViewText(R.id.phase_name, getString(R.string.df_paused).uppercase())

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                setTestProgress(views, 50)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                setTestProgress(views, 50)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                setIntervalProgress(views, 50)
                views.setTextViewText(R.id.target_power, "Ride paused")
                views.setTextViewText(R.id.time_remaining, "Resume to continue")
            }
            LayoutSize.LARGE -> {
                setIntervalProgress(views, 50)
                views.setTextViewText(R.id.target_power, "Ride paused")
                views.setTextViewText(R.id.time_remaining, "Resume to continue")
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
        }
    }

    private fun updateCompletedState(views: RemoteViews, state: TestState.Completed) {
        views.setTextViewText(R.id.phase_name, getString(R.string.df_complete).uppercase() + "!")

        val ftpText = "FTP: ${state.result.calculatedFtp}W"
        val change = state.result.ftpChange?.let {
            val sign = if (it >= 0) "+" else ""
            "$sign${it}W"
        } ?: ""

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                setTestProgress(views, 100)
                views.setTextViewText(R.id.progress_percent, "100%")
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                setTestProgress(views, 100)
                views.setTextViewText(R.id.progress_percent, "100%")
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                setIntervalProgress(views, 100)
                views.setTextViewText(R.id.target_power, ftpText)
                views.setTextViewText(R.id.time_remaining, change)
            }
            LayoutSize.LARGE -> {
                setIntervalProgress(views, 100)
                views.setTextViewText(R.id.target_power, ftpText)
                views.setTextViewText(R.id.time_remaining, change)
                setTestProgress(views, 100)
                views.setTextViewText(R.id.progress_percent, "100%")
            }
        }
    }

    private fun updateFailedState(views: RemoteViews, state: TestState.Failed) {
        views.setTextViewText(R.id.phase_name, getString(R.string.df_stopped).uppercase())

        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
            LayoutSize.MEDIUM, LayoutSize.NARROW -> {
                setIntervalProgress(views, 0)
                views.setTextViewText(R.id.target_power, state.reason.name)
                views.setTextViewText(R.id.time_remaining, "")
            }
            LayoutSize.LARGE -> {
                setIntervalProgress(views, 0)
                views.setTextViewText(R.id.target_power, state.reason.name)
                views.setTextViewText(R.id.time_remaining, "")
                setTestProgress(views, 0)
                views.setTextViewText(R.id.progress_percent, "")
            }
        }
    }

    private fun setIntervalProgress(views: RemoteViews, progress: Int) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.NARROW ||
            currentLayoutSize == LayoutSize.LARGE) {
            views.setProgressBar(R.id.interval_progress, 100, progress, false)
        }
    }

    private fun setTestProgress(views: RemoteViews, progress: Int) {
        if (currentLayoutSize == LayoutSize.SMALL || currentLayoutSize == LayoutSize.SMALL_WIDE ||
            currentLayoutSize == LayoutSize.MEDIUM_WIDE || currentLayoutSize == LayoutSize.LARGE) {
            views.setProgressBar(R.id.test_progress, 100, progress, false)
        }
    }
}
