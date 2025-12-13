package io.github.wattramp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.wattramp.ui.theme.*

/**
 * Compact live power graph - Garmin style.
 * No borders, fills width, minimal height.
 */
@Composable
fun PowerGraph(
    powerHistory: List<Int>,
    targetPower: Int?,
    modifier: Modifier = Modifier
) {
    // Capture colors before Canvas scope (non-composable)
    val surfaceVariantColor = SurfaceVariant
    val primaryColor = Primary
    val accentColor = Accent

    if (powerHistory.size < 2) {
        Box(modifier = modifier.fillMaxWidth().background(surfaceVariantColor))
        return
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .background(surfaceVariantColor)
    ) {
        val width = size.width
        val height = size.height
        val padding = 2.dp.toPx()

        // Calculate range with target included
        val allValues = if (targetPower != null && targetPower > 0) {
            powerHistory + targetPower
        } else {
            powerHistory
        }
        val minPower = (allValues.minOrNull() ?: 100) - 10
        val maxPower = (allValues.maxOrNull() ?: 200) + 10
        val range = (maxPower - minPower).coerceAtLeast(20).toFloat()

        // Target line - dashed style
        targetPower?.let { target ->
            if (target > 0) {
                val targetY = height - padding - ((target - minPower) / range * (height - 2 * padding))

                // Draw dashed line
                var x = 0f
                while (x < width) {
                    drawLine(
                        color = primaryColor.copy(alpha = 0.4f),
                        start = Offset(x, targetY),
                        end = Offset((x + 8.dp.toPx()).coerceAtMost(width), targetY),
                        strokeWidth = 1.dp.toPx()
                    )
                    x += 12.dp.toPx()
                }
            }
        }

        // Power line
        val stepX = width / (powerHistory.size - 1).coerceAtLeast(1)
        val path = Path()

        powerHistory.forEachIndexed { index, power ->
            val x = index * stepX
            val y = height - padding - ((power - minPower) / range * (height - 2 * padding))

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Current point
        val lastX = (powerHistory.lastIndex) * stepX
        val lastY = height - padding - ((powerHistory.last() - minPower) / range * (height - 2 * padding))
        drawCircle(
            color = accentColor,
            radius = 3.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}
