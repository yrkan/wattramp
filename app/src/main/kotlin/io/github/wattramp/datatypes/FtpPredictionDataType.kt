package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing predicted FTP based on current test data.
 *
 * For Ramp test: Max 1-min power × 0.75
 * For 20-min test: Current average × 0.95
 * For 8-min test: Current average × 0.90
 *
 * Updates in real-time so athlete can see projected result.
 */
class FtpPredictionDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "ftp-prediction") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_ftp_prediction_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_ftp_prediction_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_ftp_prediction_medium
        LayoutSize.LARGE -> R.layout.datatype_ftp_prediction_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_ftp))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_ftp_name))
            }
        }

        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.status, config, TextSizeCalculator.Role.TERTIARY)
        }

        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.wkg, config, TextSizeCalculator.Role.LABEL)
        }
    }

    override fun updateViews(views: RemoteViews, state: TestState) {
        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_no_test))
                setWkgText(views, "")
            }

            is TestState.Running -> {
                val predictedFtp = calculatePredictedFtp(state)
                if (predictedFtp > 0) {
                    views.setTextViewText(R.id.value, predictedFtp.toInt().toString())
                    views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                    setStatusText(views, state.protocol.shortName)
                    // Could calculate W/kg here if we have weight
                    setWkgText(views, "")
                } else {
                    views.setTextViewText(R.id.value, NO_DATA)
                    views.setTextViewText(R.id.unit, "")
                    setStatusText(views, "Calculating...")
                    setWkgText(views, "")
                }
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_paused))
                setWkgText(views, "")
            }

            is TestState.Completed -> {
                val ftp = state.result.calculatedFtp
                views.setTextViewText(R.id.value, ftp.toString())
                views.setTextViewText(R.id.unit, getString(R.string.df_watt))
                setStatusText(views, getString(R.string.df_complete))
                setWkgText(views, "")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStatusText(views, getString(R.string.df_stopped))
                setWkgText(views, "")
            }
        }
    }

    private fun calculatePredictedFtp(state: TestState.Running): Double {
        return when (state.protocol) {
            ProtocolType.RAMP -> {
                if (state.maxOneMinutePower > 0) {
                    state.maxOneMinutePower * 0.75
                } else {
                    0.0
                }
            }
            ProtocolType.TWENTY_MINUTE -> {
                if (state.averagePower > 0) {
                    state.averagePower * 0.95
                } else {
                    0.0
                }
            }
            ProtocolType.EIGHT_MINUTE -> {
                if (state.averagePower > 0) {
                    state.averagePower * 0.90
                } else {
                    0.0
                }
            }
        }
    }

    private fun setStatusText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.status, text)
        }
    }

    private fun setWkgText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.wkg, text)
        }
    }
}
