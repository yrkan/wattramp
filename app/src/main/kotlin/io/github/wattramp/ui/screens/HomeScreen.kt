package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import io.github.wattramp.data.TestSession
import io.github.wattramp.ui.theme.*

/**
 * Home screen - Compact Garmin Edge style.
 *
 * Layout:
 * - Header: App name + navigation icons
 * - Hero: Large centered FTP value
 * - Protocol selector: Horizontal compact cards
 * - Tutorial link
 * - Start button
 */
@Composable
fun HomeScreen(
    currentFtp: Int,
    recoverySession: TestSession?,
    onNavigateToChecklist: (ProtocolType) -> Unit,
    onAcceptRecovery: () -> Unit,
    onDeclineRecovery: () -> Unit,
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
        // HEADER BAR
        // ═══════════════════════════════════════════════════════════════════
        HeaderBar(
            onZones = onNavigateToZones,
            onHistory = onNavigateToHistory,
            onSettings = onNavigateToSettings,
            isCompact = isCompact
        )

        // ═══════════════════════════════════════════════════════════════════
        // RECOVERY BANNER (if needed)
        // ═══════════════════════════════════════════════════════════════════
        if (recoverySession != null) {
            RecoveryBanner(
                session = recoverySession,
                onDismiss = onAcceptRecovery,
                onDiscard = onDeclineRecovery,
                isCompact = isCompact
            )
        }

        // ═══════════════════════════════════════════════════════════════════
        // MAIN CONTENT
        // ═══════════════════════════════════════════════════════════════════
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // ═══════════════════════════════════════════════════════════════════
            // HERO FTP - Large centered value
            // ═══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .garminBorder(borderColor),
                contentAlignment = Alignment.Center
            ) {
                // FTP Value - clean, no label needed (context is clear)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$currentFtp",
                        fontSize = if (isCompact) 80.sp else 96.sp,
                        fontWeight = FontWeight.Black,
                        color = OnSurface,
                        lineHeight = if (isCompact) 80.sp else 96.sp
                    )
                    Text(
                        text = "W",
                        fontSize = if (isCompact) 32.sp else 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Primary,
                        modifier = Modifier.padding(bottom = if (isCompact) 12.dp else 16.dp, start = 4.dp)
                    )
                }
            }

            // ═══════════════════════════════════════════════════════════════════
            // PROTOCOL SELECTOR
            // ═══════════════════════════════════════════════════════════════════
            ProtocolSelector(
                selected = selectedProtocol,
                onSelect = { selectedProtocol = it },
                borderColor = borderColor,
                isCompact = isCompact
            )

            // ═══════════════════════════════════════════════════════════════════
            // TUTORIAL LINK
            // ═══════════════════════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNavigateToTutorial)
                    .padding(vertical = if (isCompact) 8.dp else 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.HelpOutline,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = stringResource(R.string.tutorial_link),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnSurfaceVariant
                    )
                }
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // START BUTTON
        // ═══════════════════════════════════════════════════════════════════
        Button(
            onClick = { onNavigateToChecklist(selectedProtocol) },
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isCompact) 56.dp else 64.dp),
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
            .padding(horizontal = 8.dp, vertical = if (isCompact) 6.dp else 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Icon(
                Icons.Default.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "WattRamp",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                letterSpacing = 0.5.sp
            )
        }

        // Navigation icons
        Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
            NavIconButton(Icons.Outlined.BarChart, onZones)
            NavIconButton(Icons.Outlined.History, onHistory)
            NavIconButton(Icons.Outlined.Settings, onSettings)
        }
    }
}

@Composable
private fun NavIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(22.dp)
        )
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
    isCompact: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceVariant.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.home_select_test),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                letterSpacing = 1.5.sp
            )
        }

        // Protocol cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
        ) {
            ProtocolType.entries.forEach { protocol ->
                ProtocolCard(
                    protocol = protocol,
                    isSelected = protocol == selected,
                    onClick = { onSelect(protocol) },
                    isCompact = isCompact,
                    modifier = Modifier.weight(1f)
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
    isCompact: Boolean,
    modifier: Modifier = Modifier
) {
    val name = when (protocol) {
        ProtocolType.RAMP -> stringResource(R.string.protocol_ramp_short)
        ProtocolType.TWENTY_MINUTE -> stringResource(R.string.protocol_20min_short)
        ProtocolType.EIGHT_MINUTE -> stringResource(R.string.protocol_8min_short)
    }
    val duration = when (protocol) {
        ProtocolType.RAMP -> "~20m"
        ProtocolType.TWENTY_MINUTE -> "~55m"
        ProtocolType.EIGHT_MINUTE -> "~45m"
    }
    val coefficient = "×${(protocol.ftpCoefficient * 100).toInt()}%"
    val accentColor = when (protocol) {
        ProtocolType.RAMP -> Primary
        ProtocolType.TWENTY_MINUTE -> Zone3
        ProtocolType.EIGHT_MINUTE -> Zone2
    }
    val cardBorderColor = SurfaceVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Background)
            .clickable(onClick = onClick)
            .drawBehind {
                // Right border
                drawLine(
                    color = cardBorderColor,
                    start = Offset(size.width, 0f),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(vertical = if (isCompact) 12.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Selection indicator
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isSelected) accentColor else Color.Transparent)
                .border(
                    width = 2.dp,
                    color = if (isSelected) accentColor else OnSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Protocol name
        Text(
            text = name,
            fontSize = if (isCompact) 14.sp else 16.sp,
            fontWeight = FontWeight.Black,
            color = if (isSelected) accentColor else OnSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Duration & coefficient
        Text(
            text = duration,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = OnSurfaceVariant
        )
        Text(
            text = coefficient,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) accentColor.copy(alpha = 0.8f) else OnSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

// =============================================================================
// GARMIN BORDER
// =============================================================================

private fun Modifier.garminBorder(color: Color): Modifier = this.drawBehind {
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )
}

// =============================================================================
// RECOVERY BANNER
// =============================================================================

@Composable
private fun RecoveryBanner(
    session: TestSession,
    onDismiss: () -> Unit,
    onDiscard: () -> Unit,
    isCompact: Boolean
) {
    val elapsedMinutes = (session.elapsedTimeMs / 60_000).toInt()
    val elapsedDisplay = if (elapsedMinutes >= 60) {
        "${elapsedMinutes / 60}h ${elapsedMinutes % 60}m"
    } else {
        "${elapsedMinutes}m"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Zone4.copy(alpha = 0.2f))
            .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Zone4,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.recovery_title),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Zone4,
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${session.protocol.shortName} • $elapsedDisplay",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = OnSurface
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.height(28.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.recovery_keep),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceVariant
                )
            }

            Button(
                onClick = onDiscard,
                modifier = Modifier.height(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Zone4,
                    contentColor = Color.Black
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.recovery_discard),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
