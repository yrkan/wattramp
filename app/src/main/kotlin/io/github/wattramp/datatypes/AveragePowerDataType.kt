package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing average power during the test.
 */
class AveragePowerDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "average-power") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_average_power_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_average_power_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_average_power_medium
        LayoutSize.LARGE -> R.layout.datatype_average_power_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_avg))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_avg_name))
            }
        }

        // Status text for medium/large
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.status, config, TextSizeCalculator.Role.TERTIARY)
        }

        // Phase text for large
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.phase, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_no_test))
                setPhaseText(views, "")
            }

            is TestState.Running -> {
                val avgPower = state.averagePower
                if (avgPower > 0) {
                    views.setTextViewText(R.id.value, avgPower.toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, state.protocol.shortName)
                    setPhaseText(views, state.phase.displayName)
                } else {
                    views.setTextViewText(R.id.value, "0")
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, state.protocol.shortName)
                    setPhaseText(views, state.phase.displayName)
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_paused))
                setPhaseText(views, "")
            }

            is TestState.Completed -> {
                val avgPower = state.result.averagePower
                if (avgPower != null && avgPower > 0) {
                    views.setTextViewText(R.id.value, avgPower.toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                } else {
                    views.setTextViewText(R.id.value, NO_DATA)
                    views.setTextViewText(R.id.unit, "")
                }
                setStatusText(views, getString(R.string.df_complete))
                setPhaseText(views, "")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_stopped))
                setPhaseText(views, "")
            }
        }
    }

    private fun setStatusText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.status, text)
        }
    }

    private fun setPhaseText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.phase, text)
        }
    }
}
