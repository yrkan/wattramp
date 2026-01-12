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
    onUpdateWeight: (Float) -> Unit,
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
    onStartDemo: () -> Unit,
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

            WeightRow(
                value = settings.userWeight,
                onUpdate = onUpdateWeight,
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

            // Demo Section
            SectionHeader(stringResource(R.string.settings_demo))

            DemoRow(
                onStartDemo = onStartDemo,
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
private fun NumericInputDialog(
    title: String,
    currentValue: String,
    unit: String,
    accentColor: Color,
    minValue: Int,
    maxValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf(currentValue) }
    val isValid = textValue.toIntOrNull()?.let { it in minValue..maxValue } ?: false

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceElevated,
        title = {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Input field with prominent background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Large editable value
                        Box(
                            modifier = Modifier
                                .background(SurfaceVariant)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = textValue,
                                onValueChange = { newValue ->
                                    textValue = newValue.filter { it.isDigit() }.take(3)
                                },
                                modifier = Modifier.widthIn(min = 60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = androidx.compose.ui.graphics.SolidColor(accentColor),
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.Center) {
                                        if (textValue.isEmpty()) {
                                            Text(
                                                text = "---",
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.Black,
                                                color = OnSurfaceVariant
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = unit,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Range hint
                Text(
                    text = stringResource(R.string.settings_range_hint, minValue, maxValue, unit),
                    fontSize = 12.sp,
                    color = if (isValid) OnSurfaceVariant else Error
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    textValue.toIntOrNull()?.let { onConfirm(it) }
                },
                enabled = isValid,
                modifier = Modifier
                    .background(if (isValid) accentColor else SurfaceVariant)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.result_save),
                    color = if (isValid) Color.Black else OnSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.settings_cancel),
                    color = OnSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    )
}

@Composable
private fun FtpRow(
    value: Int,
    onUpdate: (Int) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable { showDialog = true }
            .height(if (isCompact) 48.dp else 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Primary)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_current_ftp),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$value",
                    fontSize = if (isCompact) 20.sp else 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Primary
                )
                Text(
                    text = "W",
                    fontSize = if (isCompact) 12.sp else 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary.copy(alpha = 0.7f)
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            }
        }
    }

    if (showDialog) {
        NumericInputDialog(
            title = stringResource(R.string.settings_current_ftp),
            currentValue = value.toString(),
            unit = "W",
            accentColor = Primary,
            minValue = 50,
            maxValue = 999,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onUpdate(newValue)
                showDialog = false
            }
        )
    }
}

@Composable
private fun WeightRow(
    value: Float,
    onUpdate: (Float) -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable { showDialog = true }
            .height(if (isCompact) 44.dp else 52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Zone3)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_weight),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${value.toInt()}",
                    fontSize = if (isCompact) 18.sp else 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Zone3
                )
                Text(
                    text = "kg",
                    fontSize = if (isCompact) 11.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Zone3.copy(alpha = 0.7f)
                )
                Icon(
                    Icons.Default.Edit,
                    contentDescription = null,
                    tint = OnSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(14.dp)
                )
            }
        }
    }

    if (showDialog) {
        NumericInputDialog(
            title = stringResource(R.string.settings_weight),
            currentValue = value.toInt().toString(),
            unit = "kg",
            accentColor = Zone3,
            minValue = 30,
            maxValue = 200,
            onDismiss = { showDialog = false },
            onConfirm = { newValue ->
                onUpdate(newValue.toFloat())
                showDialog = false
            }
        )
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
            .height(if (isCompact) 42.dp else 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(color.copy(alpha = 0.6f))
        )

        // Label
        Text(
            text = label,
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
            maxLines = 1
        )

        // Controls - more compact
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Decrease
            Box(
                modifier = Modifier
                    .size(if (isCompact) 28.dp else 32.dp)
                    .background(SurfaceVariant)
                    .clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Value - separate value and unit for better fit
            Row(
                modifier = Modifier.width(if (isCompact) 52.dp else 58.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$value",
                    fontSize = if (isCompact) 14.sp else 16.sp,
                    fontWeight = FontWeight.Black,
                    color = color
                )
                Text(
                    text = unit,
                    fontSize = if (isCompact) 10.sp else 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.7f)
                )
            }

            // Increase
            Box(
                modifier = Modifier
                    .size(if (isCompact) 28.dp else 32.dp)
                    .background(SurfaceVariant)
                    .clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = OnSurface,
                    modifier = Modifier.size(16.dp)
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
            .height(if (isCompact) 38.dp else 44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (checked) Success else SurfaceVariant)
        )

        Text(
            text = label,
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
            maxLines = 1
        )

        // Toggle indicator
        Box(
            modifier = Modifier
                .width(if (isCompact) 36.dp else 40.dp)
                .height(if (isCompact) 20.dp else 22.dp)
                .background(if (checked) Success else SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(if (checked) R.string.settings_on else R.string.settings_off),
                fontSize = if (isCompact) 9.sp else 10.sp,
                fontWeight = FontWeight.Black,
                color = if (checked) Color.Black else OnSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.width(6.dp))
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
            .height(if (isCompact) 40.dp else 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Primary.copy(alpha = 0.6f))
        )

        Text(
            text = stringResource(R.string.settings_app_language),
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
            maxLines = 1
        )

        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(SurfaceVariant)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = currentLanguage.displayName,
                    fontSize = if (isCompact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(16.dp)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = language.displayName,
                                    fontSize = 12.sp,
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

        Spacer(modifier = Modifier.width(6.dp))
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
            .height(if (isCompact) 40.dp else 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Color indicator
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Primary)
        )

        Text(
            text = stringResource(R.string.settings_color_scheme),
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = OnSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
            maxLines = 1
        )

        // Theme toggle buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(end = 6.dp)
        ) {
            PreferencesRepository.AppTheme.entries.forEach { theme ->
                val isSelected = theme == currentTheme
                Box(
                    modifier = Modifier
                        .height(if (isCompact) 24.dp else 28.dp)
                        .background(
                            if (isSelected) Primary else SurfaceVariant
                        )
                        .clickable { onSelectTheme(theme) }
                        .padding(horizontal = if (isCompact) 8.dp else 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (theme) {
                            PreferencesRepository.AppTheme.ORANGE -> stringResource(R.string.theme_orange)
                            PreferencesRepository.AppTheme.BLUE -> stringResource(R.string.theme_blue)
                        },
                        fontSize = if (isCompact) 10.sp else 11.sp,
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
            .height(if (isCompact) 36.dp else 42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(errorColor)
        )

        Text(
            text = stringResource(R.string.settings_clear_history),
            fontSize = if (isCompact) 11.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            color = errorColor,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp),
            maxLines = 1
        )

        Icon(
            Icons.Default.DeleteForever,
            contentDescription = null,
            tint = errorColor,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(16.dp)
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
private fun DemoRow(
    onStartDemo: () -> Unit,
    borderColor: Color,
    isCompact: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .garminBorder(borderColor)
            .background(Background)
            .clickable(onClick = onStartDemo)
            .height(if (isCompact) 40.dp else 46.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Primary)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp, end = 4.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_demo_mode),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface
            )
            Text(
                text = stringResource(R.string.settings_demo_desc),
                fontSize = if (isCompact) 8.sp else 9.sp,
                color = OnSurfaceVariant,
                maxLines = 1
            )
        }

        Icon(
            Icons.Default.PlayArrow,
            contentDescription = null,
            tint = Primary,
            modifier = Modifier
                .padding(end = 6.dp)
                .size(18.dp)
        )
    }
}

@Composable
private fun AboutRow(
    borderColor: Color,
    isCompact: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rowHeight = if (isCompact) 36.dp else 42.dp

    Column {
        // Version
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .garminBorder(borderColor)
                .background(Background)
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(OnSurfaceVariant)
            )
            Text(
                text = stringResource(R.string.settings_version),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 4.dp)
            )
            Text(
                text = BuildConfig.VERSION_NAME,
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariant,
                modifier = Modifier.padding(end = 6.dp)
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
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Primary)
            )
            Text(
                text = stringResource(R.string.settings_github),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 4.dp)
            )
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(14.dp)
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
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Zone2)
            )
            Text(
                text = stringResource(R.string.settings_privacy),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 4.dp)
            )
            Icon(
                Icons.Default.OpenInNew,
                contentDescription = null,
                tint = Zone2,
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(14.dp)
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
                .height(rowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(Zone3)
            )
            Text(
                text = stringResource(R.string.settings_contact),
                fontSize = if (isCompact) 11.sp else 12.sp,
                fontWeight = FontWeight.Bold,
                color = OnSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 6.dp, end = 4.dp)
            )
            Text(
                text = stringResource(R.string.settings_contact_email),
                fontSize = if (isCompact) 10.sp else 11.sp,
                fontWeight = FontWeight.Medium,
                color = Zone3,
                modifier = Modifier.padding(end = 6.dp)
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
