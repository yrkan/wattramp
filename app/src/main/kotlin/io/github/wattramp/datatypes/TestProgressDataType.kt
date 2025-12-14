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
 * Numeric data field (1x1) showing test progress.
 * For Ramp: "Step 7/~15"
 * For 20-min/8-min: "12:34 / 20:00" or percentage
 */
class TestProgressDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "test-progress") {

    private var streamScope: CoroutineScope? = null

    // Store additional info for formatting
    @Volatile
    private var currentStep: Int = 0
    @Volatile
    private var estimatedSteps: Int = 0
    @Volatile
    private var elapsedSeconds: Int = 0
    @Volatile
    private var protocol: ProtocolType? = null

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
                            protocol = state.protocol
                            currentStep = state.currentStep ?: 0
                            estimatedSteps = state.estimatedTotalSteps ?: 15
                            elapsedSeconds = (state.elapsedMs / 1000).toInt()

                            // Return progress percentage for numeric display
                            state.progressPercent
                        }
                        is TestState.Completed -> {
                            100.0
                        }
                        else -> {
                            protocol = null
                            0.0
                        }
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
     * Get formatted progress string based on protocol.
     */
    fun getProgressString(): String {
        return when (protocol) {
            ProtocolType.RAMP -> {
                if (currentStep > 0) {
                    "Step $currentStep/~$estimatedSteps"
                } else {
                    "Warmup"
                }
            }
            ProtocolType.TWENTY_MINUTE, ProtocolType.EIGHT_MINUTE -> {
                val minutes = elapsedSeconds / 60
                val seconds = elapsedSeconds % 60
                "${minutes}:${seconds.toString().padStart(2, '0')}"
            }
            null -> "--"
        }
    }
}
