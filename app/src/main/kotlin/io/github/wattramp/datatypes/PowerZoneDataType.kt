package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing current power zone status.
 * Shows "IN ZONE", "TOO LOW", "TOO HIGH", or "MAX" based on target.
 */
class PowerZoneDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "power-zone") {

    private enum class ZoneStatus {
        IN_ZONE,
        TOO_LOW,
        TOO_HIGH,
        MAX_EFFORT,
        NO_TEST
    }

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_power_zone_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_power_zone_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_power_zone_medium
        LayoutSize.LARGE -> R.layout.datatype_power_zone_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_zone))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_zone_name))
            }
        }

        // Deviation text for layouts that have it
        if (currentLayoutSize != LayoutSize.SMALL) {
            views.setAdaptiveTextSize(R.id.deviation, config, TextSizeCalculator.Role.SECONDARY)
        }

        // Target text for large layout
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.target, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                showZoneStatus(views, ZoneStatus.NO_TEST, 0, 0)
            }

            is TestState.Running -> {
                val target = state.targetPower
                val current = state.currentPower

                if (target == null || target <= 0) {
                    // Max effort interval
                    showZoneStatus(views, ZoneStatus.MAX_EFFORT, 0, 0)
                } else {
                    val deviation = current - target
                    val zoneStatus = when {
                        state.isInTargetZone -> ZoneStatus.IN_ZONE
                        current < target -> ZoneStatus.TOO_LOW
                        else -> ZoneStatus.TOO_HIGH
                    }
                    showZoneStatus(views, zoneStatus, deviation, target)
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, getString(R.string.df_paused))
                views.setTextColor(R.id.value, getColor(R.color.text_primary))
                setDeviationText(views, "")
                setTargetText(views, "")
            }

            is TestState.Completed -> {
                views.setTextViewText(R.id.value, getString(R.string.df_complete))
                views.setTextColor(R.id.value, getColor(R.color.status_optimal))
                setDeviationText(views, "")
                setTargetText(views, "")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, getString(R.string.df_stopped))
                views.setTextColor(R.id.value, getColor(R.color.text_primary))
                setDeviationText(views, "")
                setTargetText(views, "")
            }
        }
    }

    private fun showZoneStatus(views: RemoteViews, status: ZoneStatus, deviation: Int, target: Int) {
        val (statusText, colorRes) = when (status) {
            ZoneStatus.IN_ZONE -> getString(R.string.df_in_zone) to R.color.status_optimal
            ZoneStatus.TOO_LOW -> getString(R.string.df_too_low) to R.color.status_attention
            ZoneStatus.TOO_HIGH -> getString(R.string.df_too_high) to R.color.status_problem
            ZoneStatus.MAX_EFFORT -> getString(R.string.df_max_effort) to R.color.status_problem
            ZoneStatus.NO_TEST -> NO_DATA to R.color.text_primary
        }

        views.setTextViewText(R.id.value, statusText)
        views.setTextColor(R.id.value, getColor(colorRes))

        // Deviation text
        if (status == ZoneStatus.NO_TEST || status == ZoneStatus.MAX_EFFORT) {
            setDeviationText(views, "")
            setTargetText(views, "")
        } else {
            val sign = if (deviation >= 0) "+" else ""
            setDeviationText(views, "${sign}${deviation}W")

            // Target info for large layout
            if (target > 0) {
                setTargetText(views, "Target: ${target}W")
            }
        }
    }

    private fun setDeviationText(views: RemoteViews, text: String) {
        if (currentLayoutSize != LayoutSize.SMALL) {
            views.setTextViewText(R.id.deviation, text)
        }
    }

    private fun setTargetText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.target, text)
        }
    }
}
