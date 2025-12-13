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
    onSaveToKaroo: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp
    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // SUCCESS HEADER
        // ═══════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Success)
                .padding(vertical = if (isCompact) 6.dp else 10.dp),
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
                    modifier = Modifier.size(if (isCompact) 18.dp else 22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.result_complete),
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 2.sp
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // MAIN FTP DISPLAY - Hero metric
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
                    text = stringResource(R.string.result_new_ftp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant,
                    letterSpacing = 2.sp
                )

                // FTP Value - HUGE
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${result.calculatedFtp}",
                        fontSize = if (isCompact) 80.sp else 100.sp,
                        fontWeight = FontWeight.Black,
                        color = Primary,
                        lineHeight = if (isCompact) 80.sp else 100.sp
                    )
                    Text(
                        text = "W",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant,
                        modifier = Modifier.padding(bottom = if (isCompact) 12.dp else 16.dp, start = 4.dp)
                    )
                }

                // Change indicator
                result.ftpChange?.let { change ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        val isPositive = change >= 0
                        val color = if (isPositive) Success else Error
                        val icon = if (isPositive) "▲" else "▼"

                        Text(
                            text = icon,
                            fontSize = 20.sp,
                            color = color
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${if (isPositive) "+" else ""}${change}W",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = color
                        )

                        // Percentage change
                        result.previousFtp?.let { prev ->
                            if (prev > 0) {
                                val pct = (change.toFloat() / prev * 100).toInt()
                                Text(
                                    text = " (${if (pct >= 0) "+" else ""}$pct%)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // STATS GRID - Garmin Edge style data fields
        // ═══════════════════════════════════════════════════════════════════
        val cellHeight = if (isCompact) 48.dp else 56.dp

        Row(modifier = Modifier.fillMaxWidth()) {
            // Protocol (use shortName for compact display)
            DataField(
                label = stringResource(R.string.result_test),
                value = result.protocol.shortName,
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // Duration
            DataField(
                label = stringResource(R.string.result_time),
                value = formatDuration(result.testDurationMs),
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // Previous FTP
            DataField(
                label = stringResource(R.string.result_prev),
                value = result.previousFtp?.let { "${it}W" } ?: "--",
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Max power
            DataField(
                label = stringResource(R.string.result_max),
                value = result.maxOneMinutePower?.let { "${it}W" } ?: "--",
                valueColor = Primary,
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // Average power
            DataField(
                label = stringResource(R.string.result_avg),
                value = result.averagePower?.let { "${it}W" } ?: "--",
                valueColor = Primary,
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )

            // FTP coefficient (for context)
            DataField(
                label = stringResource(R.string.result_coeff),
                value = "${(result.protocol.ftpCoefficient * 100).toInt()}%",
                modifier = Modifier.weight(1f).height(cellHeight),
                borderColor = borderColor
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // ACTION BUTTONS
        // ═══════════════════════════════════════════════════════════════════

        // Save button - Primary action
        Button(
            onClick = onSaveToKaroo,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 48.dp else 56.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                Icons.Default.Save,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.result_save),
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
        }

        // Discard button - Secondary action
        TextButton(
            onClick = onDiscard,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 36.dp else 42.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.textButtonColors(contentColor = OnSurfaceVariant)
        ) {
            Text(
                text = stringResource(R.string.result_discard),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
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
    valueColor: Color = OnSurface,
    borderColor: Color = SurfaceVariant
) {
    Box(
        modifier = modifier
            .garminBorder(borderColor)
            .padding(vertical = 6.dp),
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

            // Value
            Text(
                text = value,
                fontSize = 18.sp,
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
