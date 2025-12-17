package io.github.wattramp.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.engine.TestState
import io.github.wattramp.ui.components.PowerGraph
import io.github.wattramp.ui.theme.*
import java.util.ArrayDeque

/**
 * Running screen - Garmin Edge style data fields.
 *
 * Design principles:
 * - Grid-based data fields with clear borders
 * - Huge primary metric (current power)
 * - High contrast for outdoor readability
 * - Color used only for status indication
 * - Maximum data density
 * - Optimized for Karoo 2 (240x400) and Karoo 3 (320x480)
 */
@Composable
fun RunningScreen(
    runningState: TestState.Running,
    onStopTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Power history for graph using efficient circular buffer (ArrayDeque with O(1) removal)
    val powerHistoryDeque = remember { ArrayDeque<Int>(65) }
    var powerHistoryVersion by remember { mutableIntStateOf(0) }

    LaunchedEffect(runningState.currentPower, runningState.elapsedMs) {
        powerHistoryDeque.addLast(runningState.currentPower)
        // O(1) removal from front instead of O(n) removeAt(0)
        while (powerHistoryDeque.size > 60) {
            powerHistoryDeque.removeFirst()
        }
        // Trigger recomposition by incrementing version
        powerHistoryVersion++
    }

    // Convert to list only when needed for rendering (triggered by version change)
    val powerHistory = remember(powerHistoryVersion) {
        powerHistoryDeque.toList()
    }

    // Capture theme colors for helper functions
    val onSurfaceColor = OnSurface
    val errorColor = Error
    val warningColor = Warning
    val inZoneColor = InZone
    val successColor = Success
    val zone1Color = Zone1
    val zone2Color = Zone2
    val zone3Color = Zone3
    val zone4Color = Zone4
    val zone5Color = Zone5

    // Zone color animation
    val zoneColor by animateColorAsState(
        targetValue = getZoneColor(
            runningState.currentPower,
            runningState.targetPower,
            onSurfaceColor,
            errorColor,
            warningColor,
            inZoneColor
        ),
        animationSpec = tween(200),
        label = "zone"
    )

    // Screen adaptation
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp

    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // TOP STATUS BAR - Inverted, high visibility
        // ═══════════════════════════════════════════════════════════════════
        TopStatusBar(
            phase = runningState.phase.displayName,
            timeRemaining = formatTimeCompact(runningState.timeRemainingInInterval),
            isInZone = runningState.isInTargetZone,
            isCompact = isCompact
        )

        // ═══════════════════════════════════════════════════════════════════
        // MAIN POWER FIELD - Takes dominant space
        // ═══════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .garminBorder(borderColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Label
                Text(
                    text = stringResource(R.string.running_power),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant,
                    letterSpacing = 2.sp
                )

                // Current power - HUGE
                Text(
                    text = "${runningState.currentPower}",
                    fontSize = if (isCompact) 80.sp else 96.sp,
                    fontWeight = FontWeight.Black,
                    color = zoneColor,
                    lineHeight = if (isCompact) 80.sp else 96.sp
                )

                // Target + Deviation
                runningState.targetPower?.let { target ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "→$target",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant
                        )

                        val dev = runningState.currentPower - target
                        if (dev != 0) {
                            Text(
                                text = " ${if (dev > 0) "+" else ""}$dev",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (dev > 0) Warning else Error,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // POWER GRAPH - Compact visualization
        // ═══════════════════════════════════════════════════════════════════
        PowerGraph(
            powerHistory = powerHistory,  // Already a List, no need for toList()
            targetPower = runningState.targetPower,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 36.dp else 44.dp)
                .garminBorder(borderColor)
        )

        // ═══════════════════════════════════════════════════════════════════
        // METRICS GRID - 2x3 or 2x4 data fields
        // ═══════════════════════════════════════════════════════════════════
        MetricsGrid(
            state = runningState,
            borderColor = borderColor,
            isCompact = isCompact,
            zone1Color = zone1Color,
            zone2Color = zone2Color,
            zone3Color = zone3Color,
            zone4Color = zone4Color,
            zone5Color = zone5Color,
            onSurfaceColor = onSurfaceColor,
            warningColor = warningColor,
            successColor = successColor,
            errorColor = errorColor
        )

        // ═══════════════════════════════════════════════════════════════════
        // STOP BUTTON - Minimal, danger styled
        // ═══════════════════════════════════════════════════════════════════
        TextButton(
            onClick = onStopTest,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 32.dp else 38.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = Error)
        ) {
            Text(
                text = "■ ${stringResource(R.string.running_stop)}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
        }
    }
}

// =============================================================================
// TOP STATUS BAR
// =============================================================================

@Composable
private fun TopStatusBar(
    phase: String,
    timeRemaining: String,
    isInZone: Boolean,
    isCompact: Boolean
) {
    val bgColor = if (isInZone) InZone else OutOfZone

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = if (isCompact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Phase name
        Text(
            text = phase.uppercase(),
            fontSize = if (isCompact) 12.sp else 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = 1.sp
        )

        // Time remaining - prominent
        Text(
            text = timeRemaining,
            fontSize = if (isCompact) 18.sp else 22.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black
        )
    }
}

// =============================================================================
// METRICS GRID - Garmin Edge style data fields
// =============================================================================

@Composable
private fun MetricsGrid(
    state: TestState.Running,
    borderColor: Color,
    isCompact: Boolean,
    zone1Color: Color,
    zone2Color: Color,
    zone3Color: Color,
    zone4Color: Color,
    zone5Color: Color,
    onSurfaceColor: Color,
    warningColor: Color,
    successColor: Color,
    errorColor: Color
) {
    val cellHeight = if (isCompact) 44.dp else 52.dp
    val showStep = state.protocol == ProtocolType.RAMP && state.currentStep != null

    Column(modifier = Modifier.fillMaxWidth()) {
        // Row 1: AVG | MAX | TIME (| STEP for Ramp)
        Row(modifier = Modifier.fillMaxWidth()) {
            DataField(
                label = stringResource(R.string.result_avg),
                value = "${state.averagePower}",
                unit = stringResource(R.string.unit_watts).lowercase(),
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )
            DataField(
                label = stringResource(R.string.result_max),
                value = "${state.maxOneMinutePower}",
                unit = stringResource(R.string.unit_watts).lowercase(),
                valueColor = Primary,
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )
            DataField(
                label = stringResource(R.string.running_time),
                value = formatTimeCompact(state.elapsedMs),
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )
            if (showStep) {
                DataField(
                    label = stringResource(R.string.running_step, state.currentStep ?: 0).substringBefore(" "),
                    value = "${state.currentStep}/${state.estimatedTotalSteps ?: "?"}",
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor
                )
            }
        }

        // Row 2: HR | CAD | (empty or additional metrics)
        Row(modifier = Modifier.fillMaxWidth()) {
            // Heart Rate with zone
            DataField(
                label = if (state.hrZone > 0) stringResource(R.string.running_hr_zone, state.hrZone) else stringResource(R.string.running_hr),
                value = if (state.heartRate > 0) "${state.heartRate}" else "--",
                unit = "bpm",
                valueColor = getHrZoneColor(
                    state.hrZone,
                    zone1Color,
                    zone2Color,
                    zone3Color,
                    zone4Color,
                    zone5Color,
                    onSurfaceColor
                ),
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // Cadence with warning
            DataField(
                label = stringResource(R.string.running_cad),
                value = if (state.cadence > 0) "${state.cadence}" else "--",
                unit = "rpm",
                valueColor = when {
                    state.cadence == 0 -> onSurfaceColor
                    state.isCadenceLow -> warningColor
                    else -> successColor
                },
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // Deviation percentage
            DataField(
                label = stringResource(R.string.running_dev),
                value = if (state.targetPower != null && state.targetPower > 0) {
                    "${state.deviationPercent.toInt()}%"
                } else "--",
                valueColor = when {
                    state.isInTargetZone -> successColor
                    state.deviation > 0 -> warningColor
                    else -> errorColor
                },
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            if (showStep) {
                // Progress for Ramp
                DataField(
                    label = stringResource(R.string.running_prog),
                    value = "${state.progressPercent.toInt()}%",
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor
                )
            }
        }
    }
}

// =============================================================================
// DATA FIELD COMPONENT - Garmin Edge style
// =============================================================================

@Composable
private fun DataField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
    valueColor: Color = OnSurface,
    borderColor: Color = SurfaceVariant
) {
    Box(
        modifier = modifier
            .garminBorder(borderColor)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label at top
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp
            )

            // Value + optional unit
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = valueColor
                )
                unit?.let {
                    Text(
                        text = it,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(start = 1.dp, bottom = 2.dp)
                    )
                }
            }
        }
    }
}

// =============================================================================
// GARMIN BORDER MODIFIER
// =============================================================================

private fun Modifier.garminBorder(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 1.dp.toPx()

    // Bottom border
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )

    // Right border
    drawLine(
        color = color,
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

private fun getZoneColor(
    currentPower: Int,
    targetPower: Int?,
    onSurface: Color,
    error: Color,
    warning: Color,
    inZone: Color
): Color {
    if (targetPower == null || targetPower <= 0) return onSurface

    val deviation = (currentPower - targetPower).toFloat() / targetPower

    return when {
        deviation < -0.15 -> error           // Way below target
        deviation < -0.05 -> warning         // Below target
        deviation <= 0.05 -> inZone          // In zone
        deviation <= 0.15 -> warning         // Above target
        else -> error                        // Way above target
    }
}

private fun getHrZoneColor(
    zone: Int,
    zone1: Color,
    zone2: Color,
    zone3: Color,
    zone4: Color,
    zone5: Color,
    onSurface: Color
): Color = when (zone) {
    1 -> zone1
    2 -> zone2
    3 -> zone3
    4 -> zone4
    5 -> zone5
    else -> onSurface
}

private fun formatTimeCompact(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}
