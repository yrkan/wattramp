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
 * Numeric data field showing deviation from target power.
 * Positive = above target, Negative = below target.
 * Shows 0 when in zone or no target.
 */
class DeviationDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "deviation") {

    private var streamScope: CoroutineScope? = null

    override fun startStream(emitter: Emitter<StreamState>) {
        // Cancel any existing scope first
        streamScope?.cancel()

        // Create new scope with SupervisorJob for proper lifecycle management
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        streamScope = scope

        scope.launch {
            wattRampExtension.testEngine.state.collectLatest { state ->
                if (isActive) {
                    val value = when (state) {
                        is TestState.Running -> {
                            state.deviation.toDouble()
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
        }

        emitter.setCancellable {
            streamScope?.cancel()
            streamScope = null
        }
    }
}
