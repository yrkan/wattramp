package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.TestResult
import io.github.wattramp.ui.theme.*

/**
 * Result screen - Garmin Edge style.
 *
 * Design principles:
 * - Grid-based data fields with clear borders
 * - Huge FTP result as hero metric
 * - High contrast for outdoor readability
 * - Color for status only (improvement/decline)
 * - Clean action buttons
 */
@Composable
fun ResultScreen(
    result: TestResult,
    userWeight: Float = 70f,
    onSaveToKaroo: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate W/kg
    val wattsPerKg = if (userWeight > 0) result.calculatedFtp / userWeight.toDouble() else 0.0
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 480.dp
    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // SUCCESS HEADER (FIXED) - Compact
        // ═══════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Success)
                .padding(vertical = if (isCompact) 4.dp else 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.result_complete),
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 2.sp
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // CONTENT (no scroll needed with compact layout)
        // ═══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // MAIN FTP DISPLAY - Compact hero metric
            // ═══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .garminBorder(borderColor)
                    .padding(vertical = if (isCompact) 8.dp else 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // FTP Value with W suffix inline
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "${result.calculatedFtp}",
                            fontSize = if (isCompact) 56.sp else 72.sp,
                            fontWeight = FontWeight.Black,
                            color = Primary,
                            lineHeight = if (isCompact) 56.sp else 72.sp
                        )
                        Text(
                            text = "W",
                            fontSize = if (isCompact) 20.sp else 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant,
                            modifier = Modifier.padding(bottom = if (isCompact) 8.dp else 10.dp, start = 2.dp)
                        )

                        // Change indicator inline
                        result.ftpChange?.let { change ->
                            val isPositive = change >= 0
                            val color = if (isPositive) Success else Error
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "${if (isPositive) "+" else ""}${change}",
                                fontSize = if (isCompact) 20.sp else 24.sp,
                                fontWeight = FontWeight.Black,
                                color = color,
                                modifier = Modifier.padding(bottom = if (isCompact) 8.dp else 10.dp)
                            )
                        }
                    }

                    // W/kg display
                    if (wattsPerKg > 0) {
                        Text(
                            text = String.format("%.2f W/kg", wattsPerKg),
                            fontSize = if (isCompact) 16.sp else 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceVariant
                        )
                    }
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // STATS GRID - Compact Garmin Edge style data fields
            // ═══════════════════════════════════════════════════════════════════
            val cellHeight = if (isCompact) 40.dp else 46.dp

            Row(modifier = Modifier.fillMaxWidth()) {
                // Protocol (use shortName for compact display)
                DataField(
                    label = stringResource(R.string.result_test),
                    value = result.protocol.shortName,
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )

                // Duration
                DataField(
                    label = stringResource(R.string.result_time),
                    value = formatDuration(result.testDurationMs),
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )

                // Previous FTP
                DataField(
                    label = stringResource(R.string.result_prev),
                    value = result.previousFtp?.let { "${it}W" } ?: "--",
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                // Max power
                DataField(
                    label = stringResource(R.string.result_max),
                    value = result.maxOneMinutePower?.let { "${it}W" } ?: "--",
                    valueColor = Primary,
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )

                // Average power
                DataField(
                    label = stringResource(R.string.result_avg),
                    value = result.averagePower?.let { "${it}W" } ?: "--",
                    valueColor = Primary,
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )

                // FTP coefficient (for context)
                DataField(
                    label = stringResource(R.string.result_coeff),
                    value = "${(result.protocol.ftpCoefficient * 100).toInt()}%",
                    modifier = Modifier.weight(1f).height(cellHeight),
                    borderColor = borderColor,
                    isCompact = isCompact
                )
            }

            // ═══════════════════════════════════════════════════════════════════
            // ANALYTICS ROW - NP, VI, EF (only show if available)
            // ═══════════════════════════════════════════════════════════════════
            if (result.normalizedPower != null || result.variabilityIndex != null || result.efficiencyFactor != null) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Normalized Power
                    DataField(
                        label = stringResource(R.string.result_np),
                        value = result.normalizedPower?.let { "${it}W" } ?: "--",
                        valueColor = Zone4,
                        modifier = Modifier.weight(1f).height(cellHeight),
                        borderColor = borderColor,
                        isCompact = isCompact
                    )

                    // Variability Index
                    DataField(
                        label = stringResource(R.string.result_vi),
                        value = result.variabilityIndex?.let { String.format("%.2f", it) } ?: "--",
                        valueColor = Zone4,
                        modifier = Modifier.weight(1f).height(cellHeight),
                        borderColor = borderColor,
                        isCompact = isCompact
                    )

                    // Efficiency Factor or Avg HR
                    if (result.efficiencyFactor != null) {
                        DataField(
                            label = stringResource(R.string.result_ef),
                            value = String.format("%.2f", result.efficiencyFactor),
                            valueColor = Zone4,
                            modifier = Modifier.weight(1f).height(cellHeight),
                            borderColor = borderColor,
                            isCompact = isCompact
                        )
                    } else {
                        DataField(
                            label = stringResource(R.string.result_hr),
                            value = result.averageHeartRate?.let { "${it}" } ?: "--",
                            modifier = Modifier.weight(1f).height(cellHeight),
                            borderColor = borderColor,
                            isCompact = isCompact
                        )
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // ACTION BUTTONS (FIXED) - Compact
        // ═══════════════════════════════════════════════════════════════════

        // Save button - Primary action
        Button(
            onClick = onSaveToKaroo,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 42.dp else 48.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.result_save),
                fontSize = if (isCompact) 14.sp else 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // Discard button - Secondary action
        TextButton(
            onClick = onDiscard,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 32.dp else 36.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
        ) {
            Text(
                text = stringResource(R.string.result_discard),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// =============================================================================
// DATA FIELD COMPONENT - Garmin Edge style (Compact)
// =============================================================================

@Composable
private fun DataField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurface,
    borderColor: Color = SurfaceVariant,
    isCompact: Boolean = false
) {
    Box(
        modifier = modifier
            .garminBorder(borderColor)
            .padding(vertical = if (isCompact) 3.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Label at top
            Text(
                text = label,
                fontSize = if (isCompact) 8.sp else 9.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp
            )

            // Value
            Text(
                text = value,
                fontSize = if (isCompact) 15.sp else 17.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (seconds > 0) "${minutes}m ${seconds}s" else "${minutes}m"
}
