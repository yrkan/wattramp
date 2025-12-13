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
 * Numeric data field showing average power during the test.
 */
class AveragePowerDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "average-power") {

    private var streamJob: Job? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        streamJob = CoroutineScope(Dispatchers.Main).launch {
            wattRampExtension.testEngine.state.collectLatest { state ->
                val value = when (state) {
                    is TestState.Running -> {
                        state.averagePower.toDouble()
                    }
                    is TestState.Completed -> {
                        state.result.averagePower?.toDouble() ?: 0.0
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
