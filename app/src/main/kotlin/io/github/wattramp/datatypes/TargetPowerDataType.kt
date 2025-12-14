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
 * Numeric data field (1x1) showing target power for current interval.
 * Shows "MAX" for max effort intervals or target wattage.
 */
class TargetPowerDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "target-power") {

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
                            state.targetPower?.toDouble() ?: -1.0 // -1 indicates MAX
                        }
                        is TestState.Completed -> {
                            state.result.calculatedFtp.toDouble()
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

    /**
     * Format the value for display.
     * Returns "MAX" for -1, otherwise shows watts.
     */
    fun formatValue(value: Double): String {
        return when {
            value < 0 -> "MAX"
            value == 0.0 -> "--"
            else -> "${value.toInt()}W"
        }
    }
}
