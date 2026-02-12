package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing deviation from target power.
 * Positive = above target, Negative = below target.
 * Shows zone status (IN ZONE, TOO LOW, TOO HIGH) on medium/large layouts.
 */
class DeviationDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "deviation") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_deviation_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_deviation_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_deviation_medium
        LayoutSize.LARGE -> R.layout.datatype_deviation_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_dev))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_deviation_name))
            }
        }

        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.status, config, TextSizeCalculator.Role.TERTIARY)
        }

        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.percent, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_no_test))
                setPercentText(views, "")
                setValueColor(views, R.color.text_primary)
            }

            is TestState.Running -> {
                val deviation = state.deviation
                val targetPower = state.targetPower

                if (targetPower != null && targetPower > 0) {
                    // Format value with sign
                    val valueText = if (deviation >= 0) "+$deviation" else deviation.toString()
                    views.setTextViewText(R.id.value, valueText)
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))

                    // Determine zone status and color using dynamic tolerance from settings
                    val (statusText, colorRes) = when {
                        state.isInTargetZone -> getString(R.string.df_in_zone) to R.color.status_optimal
                        deviation < 0 -> getString(R.string.df_too_low) to R.color.status_attention
                        else -> getString(R.string.df_too_high) to R.color.status_problem
                    }

                    setStatusText(views, statusText)
                    setValueColor(views, colorRes)

                    // Calculate percent deviation for large layout
                    if (currentLayoutSize == LayoutSize.LARGE) {
                        val percent = (deviation.toDouble() / targetPower * 100).toInt()
                        val percentText = if (percent >= 0) "+$percent%" else "$percent%"
                        setPercentText(views, percentText)
                    }
                } else {
                    views.setTextViewText(R.id.value, "0")
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, getString(R.string.df_in_zone))
                    setPercentText(views, "")
                    setValueColor(views, R.color.status_optimal)
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_paused))
                setPercentText(views, "")
                setValueColor(views, R.color.text_primary)
            }

            is TestState.Completed -> {
                views.setTextViewText(R.id.value, "0")
                views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                setStatusText(views, getString(R.string.df_complete))
                setPercentText(views, "")
                setValueColor(views, R.color.status_optimal)
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_stopped))
                setPercentText(views, "")
                setValueColor(views, R.color.text_primary)
            }
        }
    }

    private fun setStatusText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.status, text)
        }
    }

    private fun setPercentText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.percent, text)
        }
    }

    private fun setValueColor(views: RemoteViews, colorRes: Int) {
        views.setTextColor(R.id.value, getColor(colorRes))
    }
}
