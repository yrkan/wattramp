package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing target power for current interval.
 * Shows target wattage or "MAX" for max effort intervals.
 *
 * Supports adaptive layouts based on field size.
 */
class TargetPowerDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "target-power") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_target_power_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_target_power_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_target_power_medium
        LayoutSize.LARGE -> R.layout.datatype_target_power_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        // Set adaptive text sizes based on ViewConfig
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set localized labels
        when (currentLayoutSize) {
            LayoutSize.SMALL -> {
                views.setTextViewText(R.id.label, getString(R.string.df_tgt))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.df_target))
            }
        }

        // Status text for MEDIUM and LARGE
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.status, config, TextSizeCalculator.Role.TERTIARY)
        }

        // Phase text for LARGE only
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
                val targetPower = state.targetPower
                if (targetPower != null) {
                    views.setTextViewText(R.id.value, targetPower.toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, "")
                } else {
                    // MAX effort
                    views.setTextViewText(R.id.value, getString(R.string.df_max_effort))
                    views.setTextViewText(R.id.unit, "")
                    setStatusText(views, getString(R.string.running_max_effort))
                }
                setPhaseText(views, state.phase.displayName.uppercase())
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_paused))
                setPhaseText(views, "")
            }

            is TestState.Completed -> {
                val ftp = state.result.calculatedFtp
                views.setTextViewText(R.id.value, ftp.toString())
                views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                setStatusText(views, getString(R.string.df_complete))
                setPhaseText(views, "FTP")
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
