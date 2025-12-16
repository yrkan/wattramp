package io.github.wattramp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestResult
import io.github.wattramp.ui.theme.*

/**
 * Chart display mode toggle.
 */
enum class ChartMode {
    BAR,    // Bar chart with trend line (default)
    TREND,  // Smooth Bezier curve only
    PROTOCOL // Separate lines per protocol
}

/**
 * Enhanced FTP trend chart with smooth Bezier curves.
 *
 * Features:
 * - Smooth curve through all FTP points
 * - Highlighted latest value
 * - Min/max value labels
 */
@Composable
fun FtpTrendChart(
    results: List<TestResult>,
    modifier: Modifier = Modifier
) {
    if (results.size < 2) return

    val ftpValues = results.map { it.calculatedFtp }
    val minFtp = (ftpValues.minOrNull() ?: 100) - 10
    val maxFtp = (ftpValues.maxOrNull() ?: 300) + 10
    val range = (maxFtp - minFtp).toFloat()

    // Capture colors before Canvas
    val primaryColor = Primary
    val surfaceVariantColor = SurfaceVariant
    val onSurfaceColor = OnSurface
    val backgroundColor = Background

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingTop = 8.dp.toPx()
            val chartHeight = height - paddingTop * 2

            // Calculate points
            val points = ftpValues.mapIndexed { index, ftp ->
                val x = if (ftpValues.size == 1) width / 2
                        else index * (width / (ftpValues.size - 1).coerceAtLeast(1))
                val y = paddingTop + chartHeight - ((ftp - minFtp) / range * chartHeight)
                Offset(x, y)
            }

            // Draw grid lines
            val gridLines = 3
            for (i in 0..gridLines) {
                val y = paddingTop + (chartHeight * i / gridLines)
                drawLine(
                    color = surfaceVariantColor.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw smooth Bezier curve
            if (points.size >= 2) {
                val path = createSmoothPath(points)
                drawPath(
                    path = path,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Draw points
            points.forEachIndexed { index, point ->
                val isLatest = index == points.lastIndex
                drawCircle(
                    color = if (isLatest) primaryColor else onSurfaceColor,
                    radius = if (isLatest) 6.dp.toPx() else 4.dp.toPx(),
                    center = point
                )
                if (isLatest) {
                    drawCircle(
                        color = backgroundColor,
                        radius = 3.dp.toPx(),
                        center = point
                    )
                }
            }
        }

        // Value labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${minFtp + 10}W",
                fontSize = 8.sp,
                color = OnSurfaceVariant
            )
            Text(
                text = "${ftpValues.last()}W",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Primary
            )
        }
    }
}

/**
 * Protocol comparison chart showing separate lines per protocol type.
 */
@Composable
fun ProtocolComparisonChart(
    results: List<TestResult>,
    modifier: Modifier = Modifier
) {
    if (results.isEmpty()) return

    // Group by protocol
    val rampResults = results.filter { it.protocol == ProtocolType.RAMP }
    val twentyMinResults = results.filter { it.protocol == ProtocolType.TWENTY_MINUTE }
    val eightMinResults = results.filter { it.protocol == ProtocolType.EIGHT_MINUTE }

    // Find global min/max
    val allFtp = results.map { it.calculatedFtp }
    val minFtp = (allFtp.minOrNull() ?: 100) - 10
    val maxFtp = (allFtp.maxOrNull() ?: 300) + 10
    val range = (maxFtp - minFtp).toFloat()

    // Capture colors
    val rampColor = Primary
    val twentyMinColor = Zone3
    val eightMinColor = Zone2
    val surfaceVariantColor = SurfaceVariant

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val paddingTop = 8.dp.toPx()
            val chartHeight = height - paddingTop * 2

            // Draw grid
            for (i in 0..3) {
                val y = paddingTop + (chartHeight * i / 3)
                drawLine(
                    color = surfaceVariantColor.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw each protocol's line
            drawProtocolLine(rampResults, results, minFtp, range, paddingTop, chartHeight, rampColor)
            drawProtocolLine(twentyMinResults, results, minFtp, range, paddingTop, chartHeight, twentyMinColor)
            drawProtocolLine(eightMinResults, results, minFtp, range, paddingTop, chartHeight, eightMinColor)
        }

        // Legend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (rampResults.isNotEmpty()) {
                LegendItem("RAMP", Primary)
            }
            if (twentyMinResults.isNotEmpty()) {
                LegendItem("20m", Zone3)
            }
            if (eightMinResults.isNotEmpty()) {
                LegendItem("8m", Zone2)
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Canvas(modifier = Modifier.size(8.dp)) {
            drawCircle(color = color)
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Chart mode toggle buttons.
 */
@Composable
fun ChartModeToggle(
    currentMode: ChartMode,
    onModeChange: (ChartMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(SurfaceVariant)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ChartMode.entries.forEach { mode ->
            val isSelected = mode == currentMode
            Text(
                text = when (mode) {
                    ChartMode.BAR -> "BAR"
                    ChartMode.TREND -> "TREND"
                    ChartMode.PROTOCOL -> "BY TYPE"
                },
                fontSize = 9.sp,
                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                color = if (isSelected) Primary else OnSurfaceVariant,
                modifier = Modifier
                    .clickable { onModeChange(mode) }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Create a smooth Bezier path through the given points.
 */
private fun createSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path

    path.moveTo(points[0].x, points[0].y)

    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    // Cubic Bezier smoothing
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val curr = points[i]

        // Control points for smooth curve
        val controlX1 = prev.x + (curr.x - prev.x) / 3
        val controlX2 = prev.x + (curr.x - prev.x) * 2 / 3

        // Keep vertical smooth by averaging
        val controlY1 = if (i == 1) prev.y else {
            val prevPrev = points[i - 2]
            prev.y + (curr.y - prevPrev.y) / 6
        }
        val controlY2 = if (i == points.size - 1) curr.y else {
            val next = points[i + 1]
            curr.y - (next.y - prev.y) / 6
        }

        path.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
    }

    return path
}

/**
 * Draw a protocol's data line.
 */
private fun DrawScope.drawProtocolLine(
    protocolResults: List<TestResult>,
    allResults: List<TestResult>,
    minFtp: Int,
    range: Float,
    paddingTop: Float,
    chartHeight: Float,
    color: Color
) {
    if (protocolResults.isEmpty()) return

    val width = size.width
    val totalResults = allResults.size

    // Calculate points based on position in overall timeline
    val points = protocolResults.map { result ->
        val index = allResults.indexOf(result)
        val x = if (totalResults == 1) width / 2
                else index * (width / (totalResults - 1).coerceAtLeast(1))
        val y = paddingTop + chartHeight - ((result.calculatedFtp - minFtp) / range * chartHeight)
        Offset(x, y)
    }

    // Draw line connecting points
    if (points.size >= 2) {
        val path = createSmoothPath(points)
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }

    // Draw points
    points.forEach { point ->
        drawCircle(
            color = color,
            radius = 4.dp.toPx(),
            center = point
        )
    }
}
