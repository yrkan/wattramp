package io.github.wattramp.datatypes

import io.github.wattramp.WattRampExtension
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Numeric data field showing elapsed test time in seconds.
 * Can be formatted as MM:SS on the device.
 */
class ElapsedTimeDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "elapsed-time") {

    private var streamJob: Job? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        streamJob = CoroutineScope(Dispatchers.Main).launch {
            wattRampExtension.testEngine.state.collectLatest { state ->
                val value = when (state) {
                    is TestState.Running -> {
                        (state.elapsedMs / 1000).toDouble()
                    }
                    is TestState.Completed -> {
                        (state.result.testDurationMs / 1000).toDouble()
                    }
                    else -> 0.0
                }

                emitter.onNext(
                    StreamState.Streaming(
                        DataPoint(dataTypeId = dataTypeId, values = mapOf("single" to value))
                    )
                )
            }
        }

        emitter.setCancellable {
            streamJob?.cancel()
        }
    }
}
