package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.ui.theme.*

/**
 * Home screen - Garmin Edge style.
 *
 * Design principles:
 * - Grid-based data fields with clear borders
 * - High contrast for outdoor readability
 * - Protocol selection with clear visual feedback
 * - Prominent start action
 */
@Composable
fun HomeScreen(
    currentFtp: Int,
    onStartTest: (ProtocolType) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToZones: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedProtocol by remember { mutableStateOf(ProtocolType.RAMP) }

    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp
    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // ═══════════════════════════════════════════════════════════════════
        // HEADER BAR - App name + navigation
        // ═══════════════════════════════════════════════════════════════════
        HeaderBar(
            onZones = onNavigateToZones,
            onHistory = onNavigateToHistory,
            onSettings = onNavigateToSettings,
            isCompact = isCompact
        )

        // ═══════════════════════════════════════════════════════════════════
        // FTP DATA FIELD - Current FTP as hero metric
        // ═══════════════════════════════════════════════════════════════════
        FtpDataField(
            ftp = currentFtp,
            borderColor = borderColor,
            isCompact = isCompact
        )

        // ═══════════════════════════════════════════════════════════════════
        // PROTOCOL SELECTOR - Test type selection
        // ═══════════════════════════════════════════════════════════════════
        ProtocolSelector(
            selected = selectedProtocol,
            onSelect = { selectedProtocol = it },
            borderColor = borderColor,
            isCompact = isCompact,
            modifier = Modifier.weight(1f)
        )

        // ═══════════════════════════════════════════════════════════════════
        // HELP LINK - Subtle tutorial access
        // ═══════════════════════════════════════════════════════════════════
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToTutorial)
                .padding(top = 8.dp, bottom = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    Icons.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.tutorial_link),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant
                )
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // START BUTTON - Primary action
        // ═══════════════════════════════════════════════════════════════════
        Button(
            onClick = { onStartTest(selectedProtocol) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 52.dp else 60.dp),
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = Primary,
                contentColor = Color.Black
            )
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_start),
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp
            )
        }
    }
}

// =============================================================================
// HEADER BAR
// =============================================================================

@Composable
private fun HeaderBar(
    onZones: () -> Unit,
    onHistory: () -> Unit,
    onSettings: () -> Unit,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Primary)
            .padding(horizontal = 8.dp, vertical = if (isCompact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo with bolt icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
            )
            Text(
                text = "WattRamp",
                fontSize = if (isCompact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Navigation icons
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            NavIconButton(
                icon = Icons.Outlined.BarChart,
                onClick = onZones,
                isCompact = isCompact
            )
            NavIconButton(
                icon = Icons.Outlined.History,
                onClick = onHistory,
                isCompact = isCompact
            )
            NavIconButton(
                icon = Icons.Outlined.Settings,
                onClick = onSettings,
                isCompact = isCompact
            )
        }
    }
}

@Composable
private fun NavIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    isCompact: Boolean
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(if (isCompact) 32.dp else 36.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(if (isCompact) 18.dp else 20.dp)
        )
    }
}

// =============================================================================
// FTP DATA FIELD
// =============================================================================

@Composable
private fun FtpDataField(
    ftp: Int,
    borderColor: Color,
    isCompact: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .padding(vertical = if (isCompact) 12.dp else 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Label
            Text(
                text = stringResource(R.string.home_ftp),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurfaceVariant,
                letterSpacing = 2.sp
            )

            // FTP Value
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$ftp",
                    fontSize = if (isCompact) 64.sp else 76.sp,
                    fontWeight = FontWeight.Black,
                    color = OnSurface,
                    lineHeight = if (isCompact) 64.sp else 76.sp
                )
                Text(
                    text = "W",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary,
                    modifier = Modifier.padding(bottom = if (isCompact) 8.dp else 12.dp, start = 4.dp)
                )
            }
        }
    }
}

// =============================================================================
// PROTOCOL SELECTOR
// =============================================================================

@Composable
private fun ProtocolSelector(
    selected: ProtocolType,
    onSelect: (ProtocolType) -> Unit,
    borderColor: Color,
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_select_test),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        // Protocol cards
        Column(modifier = Modifier.fillMaxWidth()) {
            ProtocolType.entries.forEach { protocol ->
                ProtocolCard(
                    protocol = protocol,
                    isSelected = protocol == selected,
                    onClick = { onSelect(protocol) },
                    borderColor = borderColor,
                    isCompact = isCompact
                )
            }
        }
    }
}

@Composable
private fun ProtocolCard(
    protocol: ProtocolType,
    isSelected: Boolean,
    onClick: () -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    val name = when (protocol) {
        ProtocolType.RAMP -> stringResource(R.string.protocol_ramp)
        ProtocolType.TWENTY_MINUTE -> stringResource(R.string.protocol_20min)
        ProtocolType.EIGHT_MINUTE -> stringResource(R.string.protocol_8min)
    }
    val duration = when (protocol) {
        ProtocolType.RAMP -> stringResource(R.string.protocol_ramp_duration)
        ProtocolType.TWENTY_MINUTE -> stringResource(R.string.protocol_20min_duration)
        ProtocolType.EIGHT_MINUTE -> stringResource(R.string.protocol_8min_duration)
    }
    val coefficient = "${(protocol.ftpCoefficient * 100).toInt()}%"
    val accentColor = when (protocol) {
        ProtocolType.RAMP -> Primary
        ProtocolType.TWENTY_MINUTE -> Zone3
        ProtocolType.EIGHT_MINUTE -> Zone2
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Background)
            .clickable(onClick = onClick)
            .padding(vertical = if (isCompact) 10.dp else 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(if (isCompact) 36.dp else 44.dp)
                .background(if (isSelected) accentColor else accentColor.copy(alpha = 0.2f))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Protocol info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = if (isCompact) 15.sp else 17.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) accentColor else OnSurface
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Coefficient badge
                Text(
                    text = "×$coefficient",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = OnSurfaceVariant.copy(alpha = 0.5f)
                )
                // Duration
                Text(
                    text = duration,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnSurfaceVariant
                )
            }
        }

        // Selection checkmark
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .size(if (isCompact) 22.dp else 26.dp)
                    .padding(end = 12.dp)
            )
        } else {
            Icon(
                Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = OnSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .size(if (isCompact) 22.dp else 26.dp)
                    .padding(end = 12.dp)
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
}
