package io.github.wattramp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.data.TestHistoryData
import io.github.wattramp.data.TestResult
import io.github.wattramp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * History screen - Garmin Edge style.
 * FTP progress chart, visual test rows, high contrast.
 */
@Composable
fun HistoryScreen(
    history: TestHistoryData,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header - Primary color block
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.history_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 2.sp
                )
            }

            // Test count
            if (history.results.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.history_tests, history.results.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.7f),
                    modifier = Modifier.padding(end = 12.dp)
                )
            }
        }

        if (history.results.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Timeline,
                        contentDescription = null,
                        tint = OnSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.history_no_tests),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = OnSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.history_no_tests_hint),
                        fontSize = 12.sp,
                        color = OnSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Current FTP block
                item {
                    CurrentFtpBlock(history = history)
                }

                // Progress chart
                if (history.results.size >= 2) {
                    item {
                        SectionHeader(stringResource(R.string.history_ftp_progress))
                        FtpProgressChart(
                            results = history.results.take(8).reversed(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Surface)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Stats row
                item {
                    SectionHeader(stringResource(R.string.history_statistics))
                    StatsRow(history = history)
                }

                // Tests list
                item {
                    SectionHeader(stringResource(R.string.history_all_tests))
                }

                // Test items
                itemsIndexed(history.results) { index, result ->
                    TestRow(
                        result = result,
                        index = index + 1,
                        isLast = index == history.results.lastIndex
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun CurrentFtpBlock(history: TestHistoryData) {
    val latestFtp = history.results.firstOrNull()?.calculatedFtp ?: 0
    val previousFtp = history.results.getOrNull(1)?.calculatedFtp
    val change = previousFtp?.let { latestFtp - it }
    val changePercent = previousFtp?.let { ((latestFtp - it).toFloat() / it * 100) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Current FTP
        Column {
            Text(
                text = stringResource(R.string.history_current_ftp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$latestFtp",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = OnSurface,
                    lineHeight = 56.sp
                )
                Text(
                    text = stringResource(R.string.unit_watts),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )
            }
        }

        // Change indicator
        change?.let { ch ->
            Column(
                horizontalAlignment = Alignment.End
            ) {
                val isPositive = ch >= 0
                val color = if (isPositive) Success else Error

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${if (isPositive) "+" else ""}${ch}W",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = color
                    )
                }
                changePercent?.let { pct ->
                    Text(
                        text = "${if (pct >= 0) "+" else ""}${String.format("%.1f", pct)}%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = color.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FtpProgressChart(
    results: List<TestResult>,
    modifier: Modifier = Modifier
) {
    if (results.size < 2) return

    val ftpValues = results.map { it.calculatedFtp }
    val minFtp = (ftpValues.minOrNull() ?: 100) - 20
    val maxFtp = (ftpValues.maxOrNull() ?: 300) + 20
    val range = (maxFtp - minFtp).toFloat()

    // Capture colors before entering Canvas scope
    val primaryColor = Primary
    val onSurfaceColor = OnSurface

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / ftpValues.size * 0.7f
        val spacing = width / ftpValues.size

        // Draw bars
        ftpValues.forEachIndexed { index, ftp ->
            val barHeight = ((ftp - minFtp) / range * height * 0.85f)
            val x = index * spacing + (spacing - barWidth) / 2
            val y = height - barHeight

            // Bar
            drawRoundRect(
                color = if (index == ftpValues.lastIndex) primaryColor else primaryColor.copy(alpha = 0.6f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }

        // Trend line
        val path = Path()
        ftpValues.forEachIndexed { index, ftp ->
            val x = index * spacing + spacing / 2
            val y = height - ((ftp - minFtp) / range * height * 0.85f) - 4.dp.toPx()
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = onSurfaceColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )

        // Points
        ftpValues.forEachIndexed { index, ftp ->
            val x = index * spacing + spacing / 2
            val y = height - ((ftp - minFtp) / range * height * 0.85f) - 4.dp.toPx()
            drawCircle(
                color = if (index == ftpValues.lastIndex) primaryColor else onSurfaceColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun StatsRow(history: TestHistoryData) {
    val best = history.results.maxOfOrNull { it.calculatedFtp } ?: 0
    val avg = history.results.map { it.calculatedFtp }.average().toInt()
    val totalGain = history.results.lastOrNull()?.calculatedFtp?.let { first ->
        history.results.firstOrNull()?.calculatedFtp?.minus(first)
    } ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface)
    ) {
        StatCell(
            value = "$best",
            label = stringResource(R.string.history_best_ftp),
            unit = stringResource(R.string.unit_watts),
            valueColor = Primary,
            modifier = Modifier.weight(1f)
        )
        StatCell(
            value = "$avg",
            label = stringResource(R.string.history_average),
            unit = stringResource(R.string.unit_watts),
            modifier = Modifier.weight(1f)
        )
        StatCell(
            value = "${if (totalGain >= 0) "+" else ""}$totalGain",
            label = stringResource(R.string.history_total_gain),
            unit = stringResource(R.string.unit_watts),
            valueColor = if (totalGain >= 0) Success else Error,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCell(
    value: String,
    label: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    valueColor: Color = OnSurface
) {
    Column(
        modifier = modifier
            .background(Surface)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = valueColor
            )
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = valueColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 9.sp,
            color = OnSurfaceVariant
        )
    }
}

@Composable
private fun TestRow(
    result: TestResult,
    index: Int,
    isLast: Boolean
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    val date = Date(result.timestamp)

    // Protocol color
    val protocolColor = when (result.protocol) {
        ProtocolType.RAMP -> Primary
        ProtocolType.TWENTY_MINUTE -> Zone3
        ProtocolType.EIGHT_MINUTE -> Zone2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index/Protocol color block
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(52.dp)
                .background(protocolColor),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "#$index",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                Text(
                    text = when (result.protocol) {
                        ProtocolType.RAMP -> "RAMP"
                        ProtocolType.TWENTY_MINUTE -> "20m"
                        ProtocolType.EIGHT_MINUTE -> "8m"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
        }

        // Test info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = result.protocol.shortName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = dateFormat.format(date),
                fontSize = 10.sp,
                color = OnSurfaceVariant
            )
        }

        // FTP result
        Column(
            horizontalAlignment = Alignment.End,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${result.calculatedFtp}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Primary
                )
                Text(
                    text = stringResource(R.string.unit_watts),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 2.dp, start = 2.dp)
                )
            }

            // Change
            result.ftpChange?.let { change ->
                Text(
                    text = "${if (change >= 0) "+" else ""}${change}W",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (change >= 0) Success else Error
                )
            }
        }
    }

    // Separator (except last)
    if (!isLast) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SurfaceVariant.copy(alpha = 0.3f))
        )
    }
}
