package io.github.wattramp.datatypes

import io.github.wattramp.WattRampExtension
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.engine.TestState
import io.hammerhead.karooext.extension.DataTypeImpl
import io.hammerhead.karooext.internal.Emitter
import io.hammerhead.karooext.models.DataPoint
import io.hammerhead.karooext.models.StreamState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Numeric data field showing predicted FTP based on current test data.
 *
 * For Ramp test: Max 1-min power × 0.75
 * For 20-min test: Current average × 0.95
 * For 8-min test: Current average × 0.90
 *
 * Updates in real-time so athlete can see projected result.
 */
class FtpPredictionDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "ftp-prediction") {

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
                            calculatePredictedFtp(state)
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

    private fun calculatePredictedFtp(state: TestState.Running): Double {
        return when (state.protocol) {
            ProtocolType.RAMP -> {
                // FTP = Max 1-min power × 0.75
                if (state.maxOneMinutePower > 0) {
                    state.maxOneMinutePower * 0.75
                } else {
                    0.0
                }
            }
            ProtocolType.TWENTY_MINUTE -> {
                // FTP = 20-min average × 0.95
                if (state.averagePower > 0) {
                    state.averagePower * 0.95
                } else {
                    0.0
                }
            }
            ProtocolType.EIGHT_MINUTE -> {
                // FTP = 8-min average × 0.90
                if (state.averagePower > 0) {
                    state.averagePower * 0.90
                } else {
                    0.0
                }
            }
        }
    }
}
