package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.data.ProtocolType
import io.github.wattramp.ui.theme.*

/**
 * Pre-test checklist screen shown after pressing START.
 * User can proceed to test or go back.
 */
@Composable
fun ChecklistScreen(
    protocol: ProtocolType,
    onStartTest: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Primary)
                .padding(horizontal = 12.dp, vertical = if (isCompact) 8.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                Icons.Outlined.Checklist,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.checklist_title),
                fontSize = if (isCompact) 14.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // Checklist items
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isCompact) 10.dp else 14.dp)
        ) {
            ChecklistItem(
                icon = Icons.Outlined.Tune,
                text = stringResource(R.string.checklist_calibrate),
                description = stringResource(R.string.checklist_calibrate_desc),
                color = Zone5,
                isCompact = isCompact
            )
            ChecklistItem(
                icon = Icons.Outlined.Bluetooth,
                text = stringResource(R.string.checklist_sensors),
                description = stringResource(R.string.checklist_sensors_desc),
                color = Zone4,
                isCompact = isCompact
            )
            ChecklistItem(
                icon = Icons.Outlined.WaterDrop,
                text = stringResource(R.string.checklist_hydration),
                description = stringResource(R.string.checklist_hydration_desc),
                color = Zone2,
                isCompact = isCompact
            )
            ChecklistItem(
                icon = Icons.Outlined.Speed,
                text = stringResource(R.string.checklist_ftp),
                description = stringResource(R.string.checklist_ftp_desc),
                color = Primary,
                isCompact = isCompact
            )
        }

        // Start button
        Button(
            onClick = onStartTest,
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
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.home_start),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 3.sp
            )
        }
    }
}

@Composable
private fun ChecklistItem(
    icon: ImageVector,
    text: String,
    description: String,
    color: Color,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(8.dp)
            )
            .padding(if (isCompact) 10.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (isCompact) 22.dp else 26.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                fontSize = if (isCompact) 13.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = description,
                fontSize = if (isCompact) 11.sp else 12.sp,
                color = OnSurfaceVariant,
                lineHeight = if (isCompact) 14.sp else 16.sp
            )
        }
    }
}
