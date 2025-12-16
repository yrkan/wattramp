package io.github.wattramp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.net.Uri
import io.github.wattramp.BuildConfig
import io.github.wattramp.R
import io.github.wattramp.data.PreferencesRepository
import io.github.wattramp.ui.theme.*

/**
 * Settings screen - Garmin Edge style.
 * Optimized for Karoo 2 (240x400) and Karoo 3 (320x480).
 */
@Composable
fun SettingsScreen(
    settings: PreferencesRepository.Settings,
    onUpdateFtp: (Int) -> Unit,
    onUpdateRampStart: (Int) -> Unit,
    onUpdateRampStep: (Int) -> Unit,
    onUpdateSoundAlerts: (Boolean) -> Unit,
    onUpdateScreenWake: (Boolean) -> Unit,
    onUpdateShowMotivation: (Boolean) -> Unit,
    onUpdateWarmupDuration: (Int) -> Unit,
    onUpdateCooldownDuration: (Int) -> Unit,
    onUpdateLanguage: (PreferencesRepository.AppLanguage) -> Unit,
    onUpdateTheme: (PreferencesRepository.AppTheme) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val isCompact = screenHeight < 420.dp
    val borderColor = SurfaceVariant.copy(alpha = 0.5f)

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
                .padding(horizontal = 4.dp, vertical = if (isCompact) 2.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                text = stringResource(R.string.settings_title),
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = Color.Black,
                letterSpacing = 1.sp
            )
        }

        // Settings list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // FTP Section
            SectionHeader(stringResource(R.string.settings_power))

            FtpRow(
                value = settings.currentFtp,
                onUpdate = onUpdateFtp,
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Ramp Test Section
            SectionHeader(stringResource(R.string.settings_ramp_test))

            CompactValueRow(
                label = stringResource(R.string.settings_start_power),
                value = settings.rampStartPower,
                unit = "W",
                color = Zone2,
                onIncrease = { onUpdateRampStart((settings.rampStartPower + 10).coerceAtMost(200)) },
                onDecrease = { onUpdateRampStart((settings.rampStartPower - 10).coerceAtLeast(50)) },
                borderColor = borderColor,
                isCompact = isCompact
            )
            CompactValueRow(
                label = stringResource(R.string.settings_step_increment),
                value = settings.rampStep,
                unit = "W/m",
                color = Zone5,
                onIncrease = { onUpdateRampStep((settings.rampStep + 5).coerceAtMost(30)) },
                onDecrease = { onUpdateRampStep((settings.rampStep - 5).coerceAtLeast(10)) },
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Duration Section
            SectionHeader(stringResource(R.string.settings_duration))

            CompactValueRow(
                label = stringResource(R.string.settings_warmup),
                value = settings.warmupDuration,
                unit = "min",
                color = Zone3,
                onIncrease = { onUpdateWarmupDuration((settings.warmupDuration + 1).coerceAtMost(15)) },
                onDecrease = { onUpdateWarmupDuration((settings.warmupDuration - 1).coerceAtLeast(3)) },
                borderColor = borderColor,
                isCompact = isCompact
            )
            CompactValueRow(
                label = stringResource(R.string.settings_cooldown),
                value = settings.cooldownDuration,
                unit = "min",
                color = Zone1,
                onIncrease = { onUpdateCooldownDuration((settings.cooldownDuration + 1).coerceAtMost(15)) },
                onDecrease = { onUpdateCooldownDuration((settings.cooldownDuration - 1).coerceAtLeast(3)) },
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Alerts Section
            SectionHeader(stringResource(R.string.settings_alerts))

            ToggleRow(
                label = stringResource(R.string.settings_sound_alerts),
                checked = settings.soundAlerts,
                onToggle = onUpdateSoundAlerts,
                borderColor = borderColor,
                isCompact = isCompact
            )
            ToggleRow(
                label = stringResource(R.string.settings_screen_on),
                checked = settings.screenWake,
                onToggle = onUpdateScreenWake,
                borderColor = borderColor,
                isCompact = isCompact
            )
            ToggleRow(
                label = stringResource(R.string.settings_motivation),
                checked = settings.showMotivation,
                onToggle = onUpdateShowMotivation,
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Language Section
            SectionHeader(stringResource(R.string.settings_language))

            LanguageRow(
                currentLanguage = settings.language,
                onSelectLanguage = onUpdateLanguage,
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Theme Section
            SectionHeader(stringResource(R.string.settings_theme))

            ThemeRow(
                currentTheme = settings.theme,
                onSelectTheme = onUpdateTheme,
                borderColor = borderColor,
                isCompact = isCompact
            )

            // Data Section
            SectionHeader(stringResource(R.string.settings_data))

            ClearHistoryRow(
                onClearHistory = onClearHistory,
                borderColor = borderColor,
                isCompact = isCompact
            )

            // About Section
            SectionHeader(stringResource(R.string.settings_about))

            AboutRow(
                borderColor = borderColor,
                isCompact = isCompact
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariant,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun FtpRow(
    value: Int,
    onUpdate: (Int) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(value) { mutableStateOf(value.toString()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .height(if (isCompact) 52.dp else 60.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Primary)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_current_ftp),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            if (isEditing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it.filter { c -> c.isDigit() }.take(3) },
                        modifier = Modifier
                            .width(64.dp)
                            .height(40.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = Primary,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary,
                            unfocusedBorderColor = SurfaceVariant
                        ),
                        shape = RectangleShape
                    )
                    IconButton(
                        onClick = {
                            textValue.toIntOrNull()?.let {
                                if (it in 50..500) onUpdate(it)
                            }
                            isEditing = false
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Success)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.clickable { isEditing = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${value}W",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = Primary
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = OnSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactValueRow(
    label: String,
    value: Int,
    unit: String,
    color: Color,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .height(if (isCompact) 44.dp else 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.6f))
        )

        // Label
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            maxLines = 1
        )

        // Controls
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Decrease
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceVariant)
                    .clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Value
            Text(
                text = "$value$unit",
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = color,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(56.dp)
            )

            // Increase
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(SurfaceVariant)
                    .clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(4.dp))
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable { onToggle(!checked) }
            .height(if (isCompact) 40.dp else 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(if (checked) Success else SurfaceVariant)
        )

        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Toggle indicator
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(22.dp)
                .background(if (checked) Success else SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(if (checked) R.string.settings_on else R.string.settings_off),
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (checked) Color.Black else OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun LanguageRow(
    currentLanguage: PreferencesRepository.AppLanguage,
    onSelectLanguage: (PreferencesRepository.AppLanguage) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable { expanded = true }
            .height(if (isCompact) 44.dp else 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Primary.copy(alpha = 0.6f))
        )

        Text(
            text = stringResource(R.string.settings_app_language),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentLanguage.displayName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Surface)
            ) {
                PreferencesRepository.AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (language == currentLanguage) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = language.displayName,
                                    fontSize = 13.sp,
                                    fontWeight = if (language == currentLanguage) FontWeight.Bold else FontWeight.Normal,
                                    color = if (language == currentLanguage) Primary else OnSurface
                                )
                            }
                        },
                        onClick = {
                            onSelectLanguage(language)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))
    }
}

@Composable
private fun ThemeRow(
    currentTheme: PreferencesRepository.AppTheme,
    onSelectTheme: (PreferencesRepository.AppTheme) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .height(if (isCompact) 44.dp else 50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(Primary)
        )

        Text(
            text = stringResource(R.string.settings_color_scheme),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        // Theme toggle buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(end = 8.dp)
        ) {
            PreferencesRepository.AppTheme.entries.forEach { theme ->
                val isSelected = theme == currentTheme
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .background(
                            if (isSelected) Primary else SurfaceVariant
                        )
                        .clickable { onSelectTheme(theme) }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (theme) {
                            PreferencesRepository.AppTheme.ORANGE -> stringResource(R.string.theme_orange)
                            PreferencesRepository.AppTheme.BLUE -> stringResource(R.string.theme_blue)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) {
                            if (theme == PreferencesRepository.AppTheme.BLUE) Color.White else Color.Black
                        } else OnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ClearHistoryRow(
    onClearHistory: () -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    val errorColor = Error
    val surfaceColor = Surface
    val onSurfaceColor = OnSurface
    val onSurfaceVariantColor = OnSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable { showConfirmDialog = true }
            .height(if (isCompact) 40.dp else 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(errorColor)
        )

        Text(
            text = stringResource(R.string.settings_clear_history),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = errorColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        )

        Icon(
            Icons.Default.DeleteForever,
            contentDescription = null,
            tint = errorColor,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp)
        )
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            containerColor = surfaceColor,
            title = {
                Text(
                    text = stringResource(R.string.settings_clear_history_confirm),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = onSurfaceColor
                )
            },
            text = null,
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showConfirmDialog = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_clear),
                        color = errorColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(
                        text = stringResource(R.string.settings_cancel),
                        color = onSurfaceVariantColor
                    )
                }
            }
        )
    }
}

@Composable
private fun AboutRow(
    borderColor: Color,
    isCompact: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    Column {
        // Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
                .background(Background)
                .height(if (isCompact) 40.dp else 46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(OnSurfaceVariant)
            )
            Text(
                text = stringResource(R.string.settings_version),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        // GitHub
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
                .background(Background)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/yrkan/wattramp"))
                    context.startActivity(intent)
                }
                .height(if (isCompact) 40.dp else 46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Primary)
            )
            Text(
                text = stringResource(R.string.settings_github),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
        }

        // Privacy Policy
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
                .background(Background)
                .clickable {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wattramp.com/privacy"))
                    context.startActivity(intent)
                }
                .height(if (isCompact) 40.dp else 46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Zone2)
            )
            Text(
                text = stringResource(R.string.settings_privacy),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Zone2,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(16.dp)
            )
        }

        // Contact
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
                .background(Background)
                .clickable {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:info@wattramp.com")
                    }
                    context.startActivity(intent)
                }
                .height(if (isCompact) 40.dp else 46.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(Zone3)
            )
            Text(
                text = stringResource(R.string.settings_contact),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            Text(
                text = stringResource(R.string.settings_contact_email),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Zone3,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

private fun Modifier.garminBorder(color: Color): Modifier = this.drawBehind {
    val strokeWidth = 1.dp.toPx()
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidth
    )
}
