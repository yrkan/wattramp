package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing elapsed test time in MM:SS format.
 * Shows phase info on medium/large layouts.
 */
class ElapsedTimeDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "elapsed-time") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_elapsed_time_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_elapsed_time_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_elapsed_time_medium
        LayoutSize.LARGE -> R.layout.datatype_elapsed_time_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_time))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_elapsed_name))
            }
        }

        // Phase text for medium/large
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.phase, config, TextSizeCalculator.Role.TERTIARY)
        }

        // Remaining time for large
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.remaining, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.value, "00:00")
                setPhaseText(views, getString(R.string.df_no_test))
                setRemainingText(views, "")
            }

            is TestState.Running -> {
                val elapsedSeconds = (state.elapsedMs / 1000).toInt()
                views.setTextViewText(R.id.value, formatTime(elapsedSeconds))

                // Show current phase
                setPhaseText(views, state.phase.displayName)

                // Show remaining time in phase if available
                val remaining = state.timeRemainingInInterval
                if (remaining > 0) {
                    val remainingSeconds = (remaining / 1000).toInt()
                    setRemainingText(views, "${getString(R.string.df_remaining)}: ${formatTime(remainingSeconds)}")
                } else {
                    setRemainingText(views, "")
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, "--:--")
                setPhaseText(views, getString(R.string.df_paused))
                setRemainingText(views, "")
            }

            is TestState.Completed -> {
                val elapsedSeconds = (state.result.testDurationMs / 1000).toInt()
                views.setTextViewText(R.id.value, formatTime(elapsedSeconds))
                setPhaseText(views, getString(R.string.df_complete))
                setRemainingText(views, "")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, "--:--")
                setPhaseText(views, getString(R.string.df_stopped))
                setRemainingText(views, "")
            }
        }
    }

    private fun setPhaseText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.phase, text)
        }
    }

    private fun setRemainingText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.remaining, text)
        }
    }
}
