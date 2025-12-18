package io.github.wattramp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.ui.theme.*
import kotlin.math.sin

private const val TOTAL_PAGES = 8

// Pre-generated noise data for diagrams (stable, no recomposition)
private val RAMP_NOISE = floatArrayOf(
    0.02f, -0.03f, 0.05f, -0.02f, 0.04f, -0.01f, 0.03f, -0.04f, 0.02f, -0.03f,
    0.04f, -0.02f, 0.05f, -0.03f, 0.02f, -0.04f, 0.03f, -0.01f, 0.04f, -0.02f,
    0.03f, -0.03f, 0.05f, -0.02f, 0.04f, -0.04f, 0.02f, -0.01f, 0.03f, -0.03f
)

private val TEST_NOISE = floatArrayOf(
    0.03f, -0.02f, 0.04f, -0.03f, 0.02f, -0.04f, 0.05f, -0.01f, 0.03f, -0.02f,
    0.04f, -0.03f, 0.02f, -0.02f, 0.03f, -0.04f, 0.05f, -0.01f, 0.02f, -0.03f
)

@Composable
fun TutorialScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Compact header
        TutorialHeader(
            currentPage = currentPage,
            totalPages = TOTAL_PAGES,
            onBack = onNavigateBack
        )

        // Page content - simple when, no pager
        Box(modifier = Modifier.weight(1f)) {
            when (currentPage) {
                0 -> FtpPage()
                1 -> RampTestPage()
                2 -> TwentyMinTestPage()
                3 -> EightMinTestPage()
                4 -> PowerZonesPage()
                5 -> DuringTestPage()
                6 -> ResultsPage()
                7 -> TipsPage()
            }
        }

        // Navigation buttons
        NavigationBar(
            currentPage = currentPage,
            totalPages = TOTAL_PAGES,
            onPrevious = { if (currentPage > 0) currentPage-- },
            onNext = { if (currentPage < TOTAL_PAGES - 1) currentPage++ },
            onDone = onNavigateBack
        )
    }
}

@Composable
private fun NavigationBar(
    currentPage: Int,
    totalPages: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Previous button
        TextButton(
            onClick = onPrevious,
            enabled = currentPage > 0,
            modifier = Modifier.width(80.dp),
            shape = RectangleShape
        ) {
            if (currentPage > 0) {
                Icon(Icons.Default.ChevronLeft, null, modifier = Modifier.size(16.dp))
                Text("PREV", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Page indicator dots
        Row(horizontalArrangement = Arrangement.Center) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(if (index == currentPage) 6.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (index == currentPage) Primary else OnSurfaceVariant.copy(alpha = 0.3f))
                )
            }
        }

        // Next / Done button
        TextButton(
            onClick = { if (currentPage < totalPages - 1) onNext() else onDone() },
            modifier = Modifier.width(80.dp),
            shape = RectangleShape
        ) {
            if (currentPage < totalPages - 1) {
                Text("NEXT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
            } else {
                Text("DONE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Success)
            }
        }
    }
}

@Composable
private fun TutorialHeader(
    currentPage: Int,
    totalPages: Int,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary)
            .padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = stringResource(R.string.tutorial_title),
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 1.sp
            )
        }

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.Black.copy(alpha = 0.2f),
            modifier = Modifier.padding(end = 6.dp)
        ) {
            Text(
                text = "${currentPage + 1}/$totalPages",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// Unified tutorial list item - consistent style across all pages
@Composable
private fun TutorialListItem(icon: ImageVector, text: String, color: Color = Primary) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 9.sp, color = OnSurface)
    }
}

// =============================================================================
// PAGE 1: WHAT IS FTP?
// =============================================================================

@Composable
private fun FtpPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.tutorial_ftp_title),
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = Primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // FTP circle (no animation for performance)
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "250",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    color = Primary
                )
                Text(
                    text = "WATTS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.tutorial_ftp_desc),
            fontSize = 10.sp,
            color = OnSurface,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.tutorial_ftp_why),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Primary
        )

        Spacer(modifier = Modifier.height(6.dp))

        TutorialListItem(Icons.Default.Speed, stringResource(R.string.tutorial_ftp_reason1), Zone4)
        TutorialListItem(Icons.Default.TrendingUp, stringResource(R.string.tutorial_ftp_reason2), Zone3)
        TutorialListItem(Icons.Default.Timer, stringResource(R.string.tutorial_ftp_reason3), Zone5)
    }
}

// =============================================================================
// PAGE 2: RAMP TEST - Realistic power graph
// =============================================================================

@Composable
private fun RampTestPage() {
    ProtocolPage(
        title = stringResource(R.string.tutorial_ramp_title),
        subtitle = stringResource(R.string.tutorial_ramp_subtitle),
        duration = stringResource(R.string.tutorial_ramp_duration),
        formula = stringResource(R.string.tutorial_ramp_formula),
        description = stringResource(R.string.tutorial_ramp_desc),
        bestFor = stringResource(R.string.tutorial_ramp_best),
        color = Primary,
        diagramContent = { RealisticRampDiagram() }
    )
}

@Composable
private fun RealisticRampDiagram() {
    val primaryColor = Primary
    val gridColor = OnSurfaceVariant.copy(alpha = 0.15f)
    val fillGradientTop = Primary.copy(alpha = 0.6f)
    val fillGradientBottom = Primary.copy(alpha = 0.1f)
    val peakColor = Zone5

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height
        val padding = 4f
        val graphW = w - padding * 2
        val graphH = h - padding * 2

        // Draw grid
        drawPowerGrid(gridColor, padding, graphW, graphH)

        // Generate ramp power data with pre-computed noise
        val points = mutableListOf<Offset>()
        val numPoints = 60  // Reduced for performance

        for (i in 0..numPoints) {
            val progress = i.toFloat() / numPoints
            val x = padding + graphW * progress

            // Base ramp: starts at 30%, ends at 100%
            val baseY = 0.3f + progress * 0.7f

            // Use pre-generated noise
            val noise = RAMP_NOISE[i % RAMP_NOISE.size] * (0.5f + progress * 0.5f)

            // Power fades at the end (exhaustion)
            val exhaustionFactor = if (progress > 0.9f) 1f - (progress - 0.9f) * 3f else 1f

            val finalY = (baseY + noise) * exhaustionFactor
            val y = padding + graphH * (1f - finalY.coerceIn(0f, 1f))
            points.add(Offset(x, y))
        }

        // Draw filled area under curve
        val fillPath = Path().apply {
            moveTo(points.first().x, padding + graphH)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, padding + graphH)
            close()
        }

        drawPath(
            fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillGradientTop, fillGradientBottom),
                startY = padding,
                endY = padding + graphH
            )
        )

        // Draw power line
        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) moveTo(point.x, point.y) else lineTo(point.x, point.y)
            }
        }

        drawPath(linePath, color = primaryColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

        // Draw max power marker at peak
        val peakPoint = points.minByOrNull { it.y } ?: points.last()
        drawCircle(color = peakColor, radius = 5f, center = peakPoint)
        drawCircle(color = Color.White, radius = 2.5f, center = peakPoint)
    }
}

// =============================================================================
// PAGE 3: 20-MIN TEST - Realistic sustained effort
// =============================================================================

@Composable
private fun TwentyMinTestPage() {
    ProtocolPage(
        title = stringResource(R.string.tutorial_20min_title),
        subtitle = stringResource(R.string.tutorial_20min_subtitle),
        duration = stringResource(R.string.tutorial_20min_duration),
        formula = stringResource(R.string.tutorial_20min_formula),
        description = stringResource(R.string.tutorial_20min_desc),
        bestFor = stringResource(R.string.tutorial_20min_best),
        color = Zone4,
        diagramContent = { Realistic20MinDiagram() }
    )
}

@Composable
private fun Realistic20MinDiagram() {
    val warmupColor = Zone2
    val testColor = Zone4
    val cooldownColor = Zone1
    val gridColor = OnSurfaceVariant.copy(alpha = 0.15f)
    val avgLineColor = Primary.copy(alpha = 0.7f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height
        val padding = 4f
        val graphW = w - padding * 2
        val graphH = h - padding * 2

        drawPowerGrid(gridColor, padding, graphW, graphH)

        val points = mutableListOf<Offset>()
        val numPoints = 50  // Reduced for performance

        // Phases: warmup 25%, test 60%, cooldown 15%
        for (i in 0..numPoints) {
            val progress = i.toFloat() / numPoints
            val x = padding + graphW * progress

            val baseY = when {
                progress < 0.15f -> 0.2f + (progress / 0.15f) * 0.15f
                progress < 0.20f -> 0.35f + ((progress - 0.15f) / 0.05f) * 0.3f
                progress < 0.25f -> 0.65f - ((progress - 0.20f) / 0.05f) * 0.35f
                progress < 0.85f -> 0.75f - ((progress - 0.25f) / 0.6f) * 0.08f
                else -> 0.67f - ((progress - 0.85f) / 0.15f) * 0.45f
            }

            val noise = TEST_NOISE[i % TEST_NOISE.size]
            val y = padding + graphH * (1f - (baseY + noise).coerceIn(0f, 1f))
            points.add(Offset(x, y))
        }

        // Draw phase backgrounds
        val warmupEnd = padding + graphW * 0.25f
        val testEnd = padding + graphW * 0.85f

        drawRect(warmupColor.copy(alpha = 0.15f), Offset(padding, padding),
            androidx.compose.ui.geometry.Size(warmupEnd - padding, graphH))
        drawRect(testColor.copy(alpha = 0.2f), Offset(warmupEnd, padding),
            androidx.compose.ui.geometry.Size(testEnd - warmupEnd, graphH))
        drawRect(cooldownColor.copy(alpha = 0.15f), Offset(testEnd, padding),
            androidx.compose.ui.geometry.Size(padding + graphW - testEnd, graphH))

        // Draw power line
        val linePath = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(linePath, testColor, style = Stroke(width = 2f, cap = StrokeCap.Round))

        // Average line
        val avgY = padding + graphH * (1f - 0.72f)
        drawLine(avgLineColor, Offset(warmupEnd, avgY), Offset(testEnd, avgY), strokeWidth = 1.5f,
            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
    }
}

// =============================================================================
// PAGE 4: 8-MIN TEST - Two interval blocks
// =============================================================================

@Composable
private fun EightMinTestPage() {
    ProtocolPage(
        title = stringResource(R.string.tutorial_8min_title),
        subtitle = stringResource(R.string.tutorial_8min_subtitle),
        duration = stringResource(R.string.tutorial_8min_duration),
        formula = stringResource(R.string.tutorial_8min_formula),
        description = stringResource(R.string.tutorial_8min_desc),
        bestFor = stringResource(R.string.tutorial_8min_best),
        color = Zone4,
        diagramContent = { Realistic8MinDiagram() }
    )
}

@Composable
private fun Realistic8MinDiagram() {
    val effortColor = Zone4
    val recoveryColor = Zone1
    val gridColor = OnSurfaceVariant.copy(alpha = 0.15f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val w = size.width
        val h = size.height
        val padding = 4f
        val graphW = w - padding * 2
        val graphH = h - padding * 2

        drawPowerGrid(gridColor, padding, graphW, graphH)

        val points = mutableListOf<Offset>()
        val numPoints = 50  // Reduced for performance

        // Phases: warmup 10%, interval1 30%, recovery 20%, interval2 30%, cooldown 10%
        for (i in 0..numPoints) {
            val progress = i.toFloat() / numPoints
            val x = padding + graphW * progress

            val baseY = when {
                progress < 0.10f -> 0.25f + (progress / 0.10f) * 0.1f
                progress < 0.40f -> 0.78f - ((progress - 0.10f) / 0.30f) * 0.06f + sin((progress - 0.10f) / 0.30f * 8f) * 0.02f
                progress < 0.60f -> 0.72f - ((progress - 0.40f) / 0.20f) * 0.4f
                progress < 0.90f -> 0.75f - ((progress - 0.60f) / 0.30f) * 0.08f + sin((progress - 0.60f) / 0.30f * 8f) * 0.02f
                else -> 0.67f - ((progress - 0.90f) / 0.10f) * 0.45f
            }

            val noise = TEST_NOISE[i % TEST_NOISE.size]
            val y = padding + graphH * (1f - (baseY + noise).coerceIn(0f, 1f))
            points.add(Offset(x, y))
        }

        // Draw zone backgrounds
        val interval1Start = padding + graphW * 0.10f
        val interval1End = padding + graphW * 0.40f
        val interval2Start = padding + graphW * 0.60f
        val interval2End = padding + graphW * 0.90f

        drawRect(effortColor.copy(alpha = 0.2f), Offset(interval1Start, padding),
            androidx.compose.ui.geometry.Size(interval1End - interval1Start, graphH))
        drawRect(effortColor.copy(alpha = 0.2f), Offset(interval2Start, padding),
            androidx.compose.ui.geometry.Size(interval2End - interval2Start, graphH))
        drawRect(recoveryColor.copy(alpha = 0.15f), Offset(interval1End, padding),
            androidx.compose.ui.geometry.Size(interval2Start - interval1End, graphH))

        // Draw power line
        val linePath = Path().apply {
            points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(linePath, effortColor, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

// Helper function to draw power grid
private fun DrawScope.drawPowerGrid(
    color: Color,
    padding: Float,
    graphW: Float,
    graphH: Float
) {
    // Horizontal grid lines (power levels)
    for (i in 0..4) {
        val y = padding + graphH * i / 4
        drawLine(
            color = color,
            start = Offset(padding, y),
            end = Offset(padding + graphW, y),
            strokeWidth = 0.5f
        )
    }
    // Vertical grid lines (time)
    for (i in 0..5) {
        val x = padding + graphW * i / 5
        drawLine(
            color = color,
            start = Offset(x, padding),
            end = Offset(x, padding + graphH),
            strokeWidth = 0.5f
        )
    }
}

// =============================================================================
// PROTOCOL PAGE TEMPLATE
// =============================================================================

@Composable
private fun ProtocolPage(
    title: String,
    subtitle: String,
    duration: String,
    formula: String,
    description: String,
    bestFor: String,
    color: Color,
    diagramContent: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = color,
            letterSpacing = 1.sp
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Diagram with border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SurfaceVariant.copy(alpha = 0.2f))
                .padding(4.dp)
        ) {
            diagramContent()
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Duration & Formula chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            CompactChip(duration, color)
            CompactChip(formula, color)
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = description,
            fontSize = 10.sp,
            color = OnSurface,
            textAlign = TextAlign.Center,
            lineHeight = 13.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = bestFor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = color,
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}

@Composable
private fun CompactChip(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

// =============================================================================
// PAGE 5: POWER ZONES
// =============================================================================

@Composable
private fun PowerZonesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.tutorial_zones_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Primary,
            letterSpacing = 1.sp
        )

        Text(
            text = stringResource(R.string.tutorial_zones_desc),
            fontSize = 9.sp,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Zone color bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(3.dp))
        ) {
            listOf(Zone1, Zone2, Zone3, Zone4, Zone5, Zone6, Zone7).forEachIndexed { i, c ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(c),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        CompactZoneItem(1, Zone1, stringResource(R.string.tutorial_zone1_desc), "<55%")
        CompactZoneItem(2, Zone2, stringResource(R.string.tutorial_zone2_desc), "55-75%")
        CompactZoneItem(3, Zone3, stringResource(R.string.tutorial_zone3_desc), "75-90%")
        CompactZoneItem(4, Zone4, stringResource(R.string.tutorial_zone4_desc), "90-105%")
        CompactZoneItem(5, Zone5, stringResource(R.string.tutorial_zone5_desc), "105-120%")
        CompactZoneItem(6, Zone6, stringResource(R.string.tutorial_zone6_desc), "120-150%")
        CompactZoneItem(7, Zone7, stringResource(R.string.tutorial_zone7_desc), ">150%")
    }
}

@Composable
private fun CompactZoneItem(num: Int, color: Color, desc: String, range: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp)).background(color),
            contentAlignment = Alignment.Center
        ) {
            Text("$num", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(desc, fontSize = 9.sp, color = OnSurface, modifier = Modifier.weight(1f))
        Text(range, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// =============================================================================
// PAGE 6: DURING TEST
// =============================================================================

@Composable
private fun DuringTestPage() {
    val borderColor = SurfaceVariant.copy(alpha = 0.4f)
    val graphLineColor = Primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tutorial_during_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Compact Running Screen mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Background)
        ) {
            Column {
                // Status bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(InZone)
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("STEP 8", fontSize = 7.sp, fontWeight = FontWeight.Black, color = Color.Black)
                    Text("0:45", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
                }

                // Main Power - compact
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBottomBorder(borderColor)
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("247", fontSize = 28.sp, fontWeight = FontWeight.Black, color = InZone, lineHeight = 28.sp)
                        Row {
                            Text("→260", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                            Text(" -13", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Warning)
                        }
                    }
                }

                // Mini power graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .drawBottomBorder(borderColor)
                        .background(SurfaceVariant.copy(alpha = 0.1f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                        val points = listOf(0.4f, 0.5f, 0.6f, 0.55f, 0.7f, 0.65f, 0.8f, 0.75f, 0.85f, 0.8f)
                        val path = Path().apply {
                            points.forEachIndexed { i, y ->
                                val x = size.width * i / (points.size - 1)
                                val yPos = size.height * (1 - y)
                                if (i == 0) moveTo(x, yPos) else lineTo(x, yPos)
                            }
                        }
                        drawPath(path, graphLineColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
                    }
                }

                // Metrics Row 1
                Row(Modifier.fillMaxWidth()) {
                    MiniDataField("AVG", "198", "w", Modifier.weight(1f), borderColor)
                    MiniDataField("MAX", "247", "w", Modifier.weight(1f), borderColor, Primary)
                    MiniDataField("TIME", "7:15", null, Modifier.weight(1f), borderColor)
                    MiniDataField("STEP", "8/15", null, Modifier.weight(1f), borderColor)
                }

                // Metrics Row 2
                Row(Modifier.fillMaxWidth()) {
                    MiniDataField("HR Z4", "165", null, Modifier.weight(1f), borderColor, Zone4)
                    MiniDataField("CAD", "88", null, Modifier.weight(1f), borderColor, Success)
                    MiniDataField("DEV", "-5%", null, Modifier.weight(1f), borderColor, Warning)
                    MiniDataField("PROG", "53%", null, Modifier.weight(1f), borderColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.tutorial_during_desc),
            fontSize = 9.sp,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Key explanations only
        TutorialListItem(Icons.Default.Bolt, stringResource(R.string.tutorial_during_power), Primary)
        TutorialListItem(Icons.Default.GpsFixed, stringResource(R.string.tutorial_during_target), Zone4)
        TutorialListItem(Icons.Default.Timeline, stringResource(R.string.tutorial_during_time), Zone3)
        TutorialListItem(Icons.Default.Favorite, stringResource(R.string.tutorial_during_hr), Zone5)
    }
}

@Composable
private fun MiniDataField(
    label: String,
    value: String,
    unit: String?,
    modifier: Modifier,
    borderColor: Color,
    valueColor: Color = OnSurface
) {
    Box(
        modifier = modifier
            .drawBottomBorder(borderColor)
            .drawRightBorder(borderColor)
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 6.sp, fontWeight = FontWeight.Medium, color = OnSurfaceVariant, letterSpacing = 0.5.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = valueColor)
                unit?.let {
                    Text(it, fontSize = 6.sp, color = OnSurfaceVariant, modifier = Modifier.padding(start = 1.dp, bottom = 1.dp))
                }
            }
        }
    }
}

// Border helpers
private fun Modifier.drawBottomBorder(color: Color) = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1f
    )
}

private fun Modifier.drawRightBorder(color: Color) = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = 1f
    )
}

// =============================================================================
// PAGE 7: RESULTS
// =============================================================================

@Composable
private fun ResultsPage() {
    val borderColor = SurfaceVariant.copy(alpha = 0.4f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.tutorial_results_title),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            color = Primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(3.dp))

        // Compact Result Screen mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(Background)
        ) {
            Column {
                // Success header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Success)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color.Black, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TEST COMPLETE", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Black, letterSpacing = 1.sp)
                }

                // Main FTP display - compact
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .drawBottomBorder(borderColor)
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("265", fontSize = 26.sp, fontWeight = FontWeight.Black, color = Primary, lineHeight = 26.sp)
                            Text("W", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant,
                                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("+15", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Success,
                                modifier = Modifier.padding(bottom = 4.dp))
                        }
                        Text("3.79 W/kg", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = OnSurfaceVariant)
                    }
                }

                // Stats Row 1
                Row(Modifier.fillMaxWidth()) {
                    MiniDataField("TEST", "RAMP", null, Modifier.weight(1f), borderColor)
                    MiniDataField("TIME", "18:45", null, Modifier.weight(1f), borderColor)
                    MiniDataField("PREV", "250W", null, Modifier.weight(1f), borderColor)
                }

                // Stats Row 2
                Row(Modifier.fillMaxWidth()) {
                    MiniDataField("MAX", "353", "w", Modifier.weight(1f), borderColor, Primary)
                    MiniDataField("AVG", "245", "w", Modifier.weight(1f), borderColor, Primary)
                    MiniDataField("COEF", "75%", null, Modifier.weight(1f), borderColor)
                }

                // Stats Row 3
                Row(Modifier.fillMaxWidth()) {
                    MiniDataField("NP", "258", "w", Modifier.weight(1f), borderColor, Zone4)
                    MiniDataField("VI", "1.05", null, Modifier.weight(1f), borderColor, Zone4)
                    MiniDataField("EF", "1.52", null, Modifier.weight(1f), borderColor, Zone4)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.tutorial_results_desc),
            fontSize = 9.sp,
            color = OnSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Key explanations
        TutorialListItem(Icons.Default.Bolt, stringResource(R.string.tutorial_results_ftp), Primary)
        TutorialListItem(Icons.Default.FitnessCenter, stringResource(R.string.tutorial_results_wkg), Zone3)
        TutorialListItem(Icons.Default.Analytics, stringResource(R.string.tutorial_results_np), Zone4)
        TutorialListItem(Icons.Default.Speed, stringResource(R.string.tutorial_results_ef), Zone5)
    }
}

// =============================================================================
// PAGE 8: TIPS
// =============================================================================

@Composable
private fun TipsPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.tutorial_tips_title),
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = Primary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        TutorialListItem(Icons.Default.Tune, stringResource(R.string.tutorial_tip1), Primary)
        TutorialListItem(Icons.Default.Whatshot, stringResource(R.string.tutorial_tip2), Zone5)
        TutorialListItem(Icons.Default.WaterDrop, stringResource(R.string.tutorial_tip3), Zone2)
        TutorialListItem(Icons.Default.SettingsRemote, stringResource(R.string.tutorial_tip4), Zone4)
        TutorialListItem(Icons.Default.Hotel, stringResource(R.string.tutorial_tip5), Zone3)
        TutorialListItem(Icons.Default.TrendingUp, stringResource(R.string.tutorial_tip6), Zone6)
        TutorialListItem(Icons.Default.Event, stringResource(R.string.tutorial_tip7), Primary)
    }
}
