package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.PowerZone
import io.github.wattramp.ui.theme.*

/**
 * Power zones screen - Garmin Edge style.
 * Optimized for Karoo 2 (240x400) and Karoo 3 (320x480).
 */
@Composable
fun ZonesScreen(
    ftp: Int,
    userWeight: Float = 70f,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate W/kg
    val wattsPerKg = if (userWeight > 0) ftp / userWeight.toDouble() else 0.0
    // Capture theme colors
    val zone1Color = Zone1
    val zone2Color = Zone2
    val zone3Color = Zone3
    val zone4Color = Zone4
    val zone5Color = Zone5
    val zone6Color = Zone6
    val zone7Color = Zone7

    val zones = PowerZone.getZones(
        zone1Color, zone2Color, zone3Color, zone4Color,
        zone5Color, zone6Color, zone7Color
    )
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp
    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // HEADER BAR
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(horizontal = 4.dp, vertical = if (isCompact) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.zones_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }

            // FTP value and W/kg
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = "${ftp}W",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
                if (wattsPerKg > 0) {
                    Text(
                        text = String.format("%.2f W/kg", wattsPerKg),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // COMPACT ZONE BAR
        // ═══════════════════════════════════════════════════════════════════
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .garminBorder(borderColor)
        ) {
            zones.forEach { zone ->
                val weight = when (zone.number) {
                    1 -> 0.55f
                    2 -> 0.20f
                    3 -> 0.15f
                    4 -> 0.15f
                    5 -> 0.15f
                    6 -> 0.30f
                    7 -> 0.25f
                    else -> 0.1f
                }
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(zone.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${zone.number}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // ZONES LIST - Compact cards
        // ═══════════════════════════════════════════════════════════════════
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(zones) { _, zone ->
                ZoneRow(
                    zone = zone,
                    ftp = ftp,
                    borderColor = borderColor,
                    isCompact = isCompact
                )
            }
        }
    }
}

// =============================================================================
// ZONE ROW - Compact Garmin style
// =============================================================================

@Composable
private fun ZoneRow(
    zone: PowerZone,
    ftp: Int,
    borderColor: Color,
    isCompact: Boolean
) {
    val zoneName = when (zone.number) {
        1 -> stringResource(R.string.zone_1)
        2 -> stringResource(R.string.zone_2)
        3 -> stringResource(R.string.zone_3)
        4 -> stringResource(R.string.zone_4)
        5 -> stringResource(R.string.zone_5)
        6 -> stringResource(R.string.zone_6)
        7 -> stringResource(R.string.zone_7)
        else -> zone.shortName
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .height(if (isCompact) 44.dp else 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Zone color block with number
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .background(zone.color),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${zone.number}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black
            )
        }

        // Zone name
        Text(
            text = zoneName.uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            maxLines = 1
        )

        // Power range - right aligned, colored
        Text(
            text = zone.getRange(ftp),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = zone.color,
            textAlign = TextAlign.End,
            modifier = Modifier.padding(end = 8.dp)
        )
    }
}

// =============================================================================
// GARMIN BORDER MODIFIER
// =============================================================================

private fun Modifier.garminBorder(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 1.dp.toPx()

    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
}
