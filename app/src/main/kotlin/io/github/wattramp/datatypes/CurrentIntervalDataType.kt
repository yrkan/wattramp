package io.github.wattramp.datatypes

import android.content.Context
import android.widget.RemoteViews
import io.github.wattramp.R
import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.internal.ViewEmitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.UpdateGraphicConfig
import io.hammerhead.karooext.models.ViewConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Graphical data field (2x1) showing current interval information:
 * - Phase name (e.g., "RAMP STEP 7", "20-MIN TEST")
 * - Target power
 * - Time remaining
 * - Progress bars
 */
class CurrentIntervalDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "current-interval") {

    private var viewJob: Job? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        // This data type is graphical, so we don't emit numeric values
        emitter.onNext(StreamState.Streaming(
            DataPoint(dataTypeId = dataTypeId, values = emptyMap())
        ))
    }

    override fun startView(context: Context, config: ViewConfig, emitter: ViewEmitter) {
        // Configure as graphical view without header
        emitter.onNext(UpdateGraphicConfig(showHeader = false))

        viewJob = CoroutineScope(Dispatchers.Main).launch {
            wattRampExtension.testEngine.state.collectLatest { state ->
                val remoteViews = createRemoteViews(context, state)
                emitter.updateView(remoteViews)
            }
        }

        emitter.setCancellable {
            viewJob?.cancel()
        }
    }

    private fun createRemoteViews(context: Context, state: TestState): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.datatype_current_interval)

        when (state) {
            is TestState.Idle -> {
                views.setTextViewText(R.id.phase_name, "NO TEST")
                views.setTextViewText(R.id.target_power, "Start a test")
                views.setTextViewText(R.id.time_remaining, "from WattRamp app")
                views.setProgressBar(R.id.interval_progress, 100, 0, false)
                views.setProgressBar(R.id.test_progress, 100, 0, false)
                views.setTextViewText(R.id.progress_percent, "")
            }

            is TestState.Running -> {
                // Phase name
                val phaseName = when {
                    state.currentStep != null -> "RAMP STEP ${state.currentStep}"
                    else -> state.phase.displayName.uppercase()
                }
                views.setTextViewText(R.id.phase_name, phaseName)

                // Target power
                val targetText = state.targetPower?.let { "Target: ${it}W" } ?: "MAX EFFORT"
                views.setTextViewText(R.id.target_power, targetText)

                // Time remaining
                val remainingSeconds = (state.timeRemainingInInterval / 1000).toInt()
                val minutes = remainingSeconds / 60
                val seconds = remainingSeconds % 60
                views.setTextViewText(
                    R.id.time_remaining,
                    "${minutes}:${seconds.toString().padStart(2, '0')} remaining"
                )

                // Interval progress
                val intervalDuration = state.currentInterval.durationMs
                val intervalElapsed = intervalDuration - state.timeRemainingInInterval
                val intervalProgress = ((intervalElapsed.toDouble() / intervalDuration) * 100).toInt()
                views.setProgressBar(R.id.interval_progress, 100, intervalProgress, false)

                // Test progress
                val testProgress = state.progressPercent.toInt().coerceIn(0, 100)
                views.setProgressBar(R.id.test_progress, 100, testProgress, false)
                views.setTextViewText(R.id.progress_percent, "${testProgress}%")
            }

            is TestState.Paused -> {
                views.setTextViewText(R.id.phase_name, "PAUSED")
                views.setTextViewText(R.id.target_power, "Ride paused")
                views.setTextViewText(R.id.time_remaining, "Resume to continue")
                views.setProgressBar(R.id.interval_progress, 100, 50, false)
                views.setProgressBar(R.id.test_progress, 100, 0, false)
                views.setTextViewText(R.id.progress_percent, "")
            }

            is TestState.Completed -> {
                views.setTextViewText(R.id.phase_name, "COMPLETE!")
                views.setTextViewText(R.id.target_power, "FTP: ${state.result.calculatedFtp}W")
                val change = state.result.ftpChange?.let {
                    val sign = if (it >= 0) "+" else ""
                    "$sign${it}W"
                } ?: ""
                views.setTextViewText(R.id.time_remaining, change)
                views.setProgressBar(R.id.interval_progress, 100, 100, false)
                views.setProgressBar(R.id.test_progress, 100, 100, false)
                views.setTextViewText(R.id.progress_percent, "100%")
            }

            is TestState.Failed -> {
                views.setTextViewText(R.id.phase_name, "STOPPED")
                views.setTextViewText(R.id.target_power, state.reason.name)
                views.setTextViewText(R.id.time_remaining, "")
                views.setProgressBar(R.id.interval_progress, 100, 0, false)
                views.setProgressBar(R.id.test_progress, 100, 0, false)
                views.setTextViewText(R.id.progress_percent, "")
            }
        }

        return views
    }
}
