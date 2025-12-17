package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.wattramp.R
import io.github.wattramp.ui.theme.*

/**
 * Compact sensor warning screen - shown when no power meter detected.
 */
@Composable
fun SensorWarningScreen(
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(if (isCompact) 16.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Warning icon
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = Zone4,
                    modifier = Modifier.size(if (isCompact) 36.dp else 44.dp)
                )

                Spacer(modifier = Modifier.height(if (isCompact) 10.dp else 14.dp))

                // Title
                Text(
                    text = stringResource(R.string.sensor_warning_title),
                    fontSize = if (isCompact) 16.sp else 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isCompact) 6.dp else 8.dp))

                // Message
                Text(
                    text = stringResource(R.string.sensor_warning_message),
                    fontSize = if (isCompact) 13.sp else 14.sp,
                    color = OnSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(if (isCompact) 16.dp else 20.dp))

                // Buttons - vertical layout, Cancel first (top), Start second (bottom)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Cancel (secondary action - top)
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 40.dp else 44.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.sensor_warning_cancel),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Start (primary action - bottom)
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isCompact) 44.dp else 48.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Zone4,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.sensor_warning_start),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
