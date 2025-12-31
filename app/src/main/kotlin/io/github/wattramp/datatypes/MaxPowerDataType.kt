package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing max 1-minute power during the test.
 * Critical for Ramp test FTP calculation (Max 1-min × 0.75).
 */
class MaxPowerDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "max-power") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_max_power_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_max_power_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_max_power_medium
        LayoutSize.LARGE -> R.layout.datatype_max_power_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_max))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_max_name))
            }
        }

        // Status text for medium/large
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.status, config, TextSizeCalculator.Role.TERTIARY)
        }

        // FTP estimate text for large
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.ftp, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_no_test))
                setFtpText(views, "")
            }

            is TestState.Running -> {
                val maxPower = state.maxOneMinutePower.toInt()
                if (maxPower > 0) {
                    views.setTextViewText(R.id.value, maxPower.toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, "1-min avg")

                    // Show FTP estimate for Ramp test
                    if (state.protocol == ProtocolType.RAMP && currentLayoutSize == LayoutSize.LARGE) {
                        val ftpEstimate = (maxPower * 0.75).toInt()
                        setFtpText(views, "FTP: ~${ftpEstimate}W")
                    } else {
                        setFtpText(views, "")
                    }
                } else {
                    views.setTextViewText(R.id.value, "0")
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, "1-min avg")
                    setFtpText(views, "")
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_paused))
                setFtpText(views, "")
            }

            is TestState.Completed -> {
                val maxPower = state.result.maxOneMinutePower
                if (maxPower != null && maxPower > 0) {
                    views.setTextViewText(R.id.value, maxPower.toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, "1-min avg")

                    // Show final FTP for Ramp test
                    if (state.result.protocol == ProtocolType.RAMP && currentLayoutSize == LayoutSize.LARGE) {
                        setFtpText(views, "FTP: ${state.result.calculatedFtp}W")
                    } else {
                        setFtpText(views, "")
                    }
                } else {
                    views.setTextViewText(R.id.value, NO_DATA)
                    views.setTextViewText(R.id.unit, "")
                    setStatusText(views, getString(R.string.df_complete))
                    setFtpText(views, "")
                }
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_stopped))
                setFtpText(views, "")
            }
        }
    }

    private fun setStatusText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.status, text)
        }
    }

    private fun setFtpText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.ftp, text)
        }
    }
}
