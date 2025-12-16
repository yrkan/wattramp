package io.github.wattramp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.ui.theme.*

/**
 * Pre-test checklist component - informational reminders before starting a test.
 * Expandable/collapsible with dismiss option.
 */
@Composable
fun PreTestChecklist(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceVariant.copy(alpha = 0.3f))
    ) {
        // Header row with expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Outlined.Checklist,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = stringResource(R.string.checklist_title),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    letterSpacing = 1.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.checklist_dismiss),
                        tint = OnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
                // Expand/collapse
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expandable content
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChecklistItem(
                    icon = Icons.Outlined.Tune,
                    text = stringResource(R.string.checklist_calibrate),
                    description = stringResource(R.string.checklist_calibrate_desc)
                )
                ChecklistItem(
                    icon = Icons.Outlined.Bluetooth,
                    text = stringResource(R.string.checklist_sensors),
                    description = stringResource(R.string.checklist_sensors_desc)
                )
                ChecklistItem(
                    icon = Icons.Outlined.WaterDrop,
                    text = stringResource(R.string.checklist_hydration),
                    description = stringResource(R.string.checklist_hydration_desc)
                )
                ChecklistItem(
                    icon = Icons.Outlined.Settings,
                    text = stringResource(R.string.checklist_ftp),
                    description = stringResource(R.string.checklist_ftp_desc)
                )
            }
        }
    }
}

@Composable
private fun ChecklistItem(
    icon: ImageVector,
    text: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column {
            Text(
                text = text,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnSurface
            )
            Text(
                text = description,
                fontSize = 9.sp,
                color = OnSurfaceVariant,
                lineHeight = 12.sp
            )
        }
    }
}
