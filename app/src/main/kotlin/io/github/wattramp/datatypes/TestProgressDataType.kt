package io.github.wattramp.datatypes

import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.models.ViewConfig

/**
 * Graphical data field showing test progress.
 * For Ramp: Shows step and percentage.
 * For 20-min/8-min: Shows percentage with time info.
 */
class TestProgressDataType(
    wattRampExtension: WattRampExtension
) : BaseDataType(wattRampExtension, "test-progress") {

    override fun getLayoutResId(size: LayoutSize) = when (size) {
        LayoutSize.SMALL -> R.layout.datatype_test_progress_small
        LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> R.layout.datatype_test_progress_small_wide
        LayoutSize.MEDIUM, LayoutSize.NARROW -> R.layout.datatype_test_progress_medium
        LayoutSize.LARGE -> R.layout.datatype_test_progress_large
    }

    override fun onViewCreated(views: RemoteViews, config: ViewConfig) {
        views.setAdaptiveTextSize(R.id.value, config, TextSizeCalculator.Role.PRIMARY)
        views.setAdaptiveTextSize(R.id.label, config, TextSizeCalculator.Role.LABEL)
        views.setAdaptiveTextSize(R.id.unit, config, TextSizeCalculator.Role.LABEL)

        // Set label based on size
        when (currentLayoutSize) {
            LayoutSize.SMALL, LayoutSize.SMALL_WIDE, LayoutSize.MEDIUM_WIDE -> {
                views.setTextViewText(R.id.label, getString(R.string.df_prog))
            }
            else -> {
                views.setTextViewText(R.id.label, getString(R.string.datatype_progress_name))
            }
        }

        // Step text for medium/large
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setAdaptiveTextSize(R.id.step, config, TextSizeCalculator.Role.TERTIARY)
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
                setStepText(views, getString(R.string.df_no_test))
                setPhaseText(views, "")
            }

            is TestState.Running -> {
                val progress = state.progressPercent.toInt()
                views.setTextViewText(R.id.value, progress.toString())
                views.setTextViewText(R.id.unit, getString(R.string.df_percent))

                // Step info based on protocol
                val stepText = when (state.protocol) {
                    ProtocolType.RAMP -> {
                        val step = state.currentStep ?: 0
                        val total = state.estimatedTotalSteps ?: 15
                        if (step > 0) {
                            "Step $step/~$total"
                        } else {
                            state.phase.displayName
                        }
                    }
                    ProtocolType.TWENTY_MINUTE, ProtocolType.EIGHT_MINUTE -> {
                        val elapsed = (state.elapsedMs / 1000).toInt()
                        val minutes = elapsed / 60
                        val seconds = elapsed % 60
                        "${minutes}:${seconds.toString().padStart(2, '0')}"
                    }
                }
                setStepText(views, stepText)
                setPhaseText(views, state.phase.displayName)
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStepText(views, getString(R.string.df_paused))
                setPhaseText(views, "")
            }

            is TestState.Completed -> {
                views.setTextViewText(R.id.value, "100")
                views.setTextViewText(R.id.unit, getString(R.string.df_percent))
                setStepText(views, getString(R.string.df_complete))
                setPhaseText(views, "")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.value, NO_DATA)
                views.setTextViewText(R.id.unit, "")
                setStepText(views, getString(R.string.df_stopped))
                setPhaseText(views, "")
            }
        }
    }

    private fun setStepText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.MEDIUM || currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.step, text)
        }
    }

    private fun setPhaseText(views: RemoteViews, text: String) {
        if (currentLayoutSize == LayoutSize.LARGE) {
            views.setTextViewText(R.id.phase, text)
        }
    }
}
