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
 * Numeric data field (1x1) showing current power zone status.
 * Shows "IN ZONE", "TOO LOW", or "TOO HIGH" with deviation.
 */
class PowerZoneDataType(
    private val wattRampExtension: WattRampExtension
) : DataTypeImpl("wattramp", "power-zone") {

    private var streamScope: CoroutineScope? = null

    // Store zone info for display
    @Volatile
    private var zoneStatus: ZoneStatus = ZoneStatus.NO_TEST
    @Volatile
    private var deviation: Int = 0

    enum class ZoneStatus {
        IN_ZONE,
        TOO_LOW,
        TOO_HIGH,
        MAX_EFFORT,
        NO_TEST
    }

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
                            val target = state.targetPower

                            if (target == null || target <= 0) {
                                // Max effort interval
                                zoneStatus = ZoneStatus.MAX_EFFORT
                                deviation = 0
                                0.0 // No target
                            } else {
                                deviation = state.currentPower - target
                                zoneStatus = when {
                                    state.isInTargetZone -> ZoneStatus.IN_ZONE
                                    state.currentPower < target -> ZoneStatus.TOO_LOW
                                    else -> ZoneStatus.TOO_HIGH
                                }

                                // Return deviation as the value
                                deviation.toDouble()
                            }
                        }
                        else -> {
                            zoneStatus = ZoneStatus.NO_TEST
                            deviation = 0
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
     * Get the current zone status string for display.
     */
    fun getZoneStatusString(): String {
        return when (zoneStatus) {
            ZoneStatus.IN_ZONE -> "IN ZONE"
            ZoneStatus.TOO_LOW -> "TOO LOW"
            ZoneStatus.TOO_HIGH -> "TOO HIGH"
            ZoneStatus.MAX_EFFORT -> "MAX"
            ZoneStatus.NO_TEST -> "--"
        }
    }

    /**
     * Get deviation string with sign.
     */
    fun getDeviationString(): String {
        if (zoneStatus == ZoneStatus.NO_TEST || zoneStatus == ZoneStatus.MAX_EFFORT) {
            return ""
        }
        val sign = if (deviation >= 0) "+" else ""
        return "${sign}${deviation}W"
    }

    /**
     * Get color resource for current zone status.
     */
    fun getZoneColorRes(): Int {
        return when (zoneStatus) {
            ZoneStatus.IN_ZONE -> io.github.wattramp.R.color.in_zone
            ZoneStatus.TOO_LOW -> io.github.wattramp.R.color.out_of_zone
            ZoneStatus.TOO_HIGH -> io.github.wattramp.R.color.warning
            ZoneStatus.MAX_EFFORT -> io.github.wattramp.R.color.zone_6
            ZoneStatus.NO_TEST -> io.github.wattramp.R.color.zone_1
        }
    }
}
