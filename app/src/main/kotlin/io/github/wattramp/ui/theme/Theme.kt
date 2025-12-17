package io.github.wattramp.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.wattramp.data.PreferencesRepository.AppTheme

// =============================================================================
// THEME COLORS - Switchable between Garmin and Wahoo styles
// =============================================================================

data class WattRampColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val onPrimary: Color,
    val secondary: Color,
    val accent: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val inZone: Color,
    val outOfZone: Color,
    val zone1: Color,
    val zone2: Color,
    val zone3: Color,
    val zone4: Color,
    val zone5: Color,
    val zone6: Color,
    val zone7: Color
)

// =============================================================================
// ORANGE THEME - Orange accent, high contrast outdoor
// =============================================================================

val OrangeColors = WattRampColors(
    primary = Color(0xFFFF6600),        // Orange
    primaryDark = Color(0xFFCC5200),
    primaryLight = Color(0xFFFF8533),
    onPrimary = Color(0xFF000000),
    secondary = Color(0xFFFFCC00),      // Yellow
    accent = Color(0xFFFF6600),
    background = Color(0xFF000000),     // Pure black
    surface = Color(0xFF111111),
    surfaceVariant = Color(0xFF1A1A1A),
    surfaceElevated = Color(0xFF222222),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFFAAAAAA),
    success = Color(0xFF00FF66),
    warning = Color(0xFFFFCC00),
    error = Color(0xFFFF3333),
    inZone = Color(0xFF00FF66),
    outOfZone = Color(0xFFFF3333),
    zone1 = Color(0xFF888888),
    zone2 = Color(0xFF0088FF),
    zone3 = Color(0xFF00CC66),
    zone4 = Color(0xFFFFCC00),
    zone5 = Color(0xFFFF6600),
    zone6 = Color(0xFFFF3333),
    zone7 = Color(0xFFCC00FF)
)

// =============================================================================
// BLUE THEME - Electric blue with vibrant accents
// =============================================================================

val BlueColors = WattRampColors(
    primary = Color(0xFF00B4FF),        // Electric blue
    primaryDark = Color(0xFF0088CC),
    primaryLight = Color(0xFF66D4FF),
    onPrimary = Color(0xFF000000),      // Black text on bright blue
    secondary = Color(0xFFFF00AA),      // Magenta accent
    accent = Color(0xFF00FFCC),         // Cyan accent
    background = Color(0xFF0A0A0F),     // Deep blue-black
    surface = Color(0xFF12121A),        // Slight blue tint
    surfaceVariant = Color(0xFF1A1A25),
    surfaceElevated = Color(0xFF252533),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF8888AA), // Blue-gray
    success = Color(0xFF00FF88),        // Bright green
    warning = Color(0xFFFFCC00),        // Golden yellow
    error = Color(0xFFFF3366),          // Pink-red
    inZone = Color(0xFF00FF88),
    outOfZone = Color(0xFFFF3366),
    zone1 = Color(0xFF6677AA),          // Blue-gray
    zone2 = Color(0xFF00B4FF),          // Electric blue
    zone3 = Color(0xFF00DDAA),          // Turquoise
    zone4 = Color(0xFFFFCC00),          // Gold
    zone5 = Color(0xFFFF6600),          // Orange
    zone6 = Color(0xFFFF3366),          // Pink-red
    zone7 = Color(0xFFCC44FF)           // Bright purple
)

// =============================================================================
// THEME STATE - Global color provider
// =============================================================================

val LocalWattRampColors = staticCompositionLocalOf { OrangeColors }

// Expose current theme colors as global variables for easy access
val Primary: Color @Composable get() = LocalWattRampColors.current.primary
val PrimaryDark: Color @Composable get() = LocalWattRampColors.current.primaryDark
val PrimaryLight: Color @Composable get() = LocalWattRampColors.current.primaryLight
val OnPrimary: Color @Composable get() = LocalWattRampColors.current.onPrimary
val Secondary: Color @Composable get() = LocalWattRampColors.current.secondary
val Accent: Color @Composable get() = LocalWattRampColors.current.accent
val Background: Color @Composable get() = LocalWattRampColors.current.background
val Surface: Color @Composable get() = LocalWattRampColors.current.surface
val SurfaceVariant: Color @Composable get() = LocalWattRampColors.current.surfaceVariant
val SurfaceElevated: Color @Composable get() = LocalWattRampColors.current.surfaceElevated
val OnBackground: Color @Composable get() = LocalWattRampColors.current.onBackground
val OnSurface: Color @Composable get() = LocalWattRampColors.current.onSurface
val OnSurfaceVariant: Color @Composable get() = LocalWattRampColors.current.onSurfaceVariant
val Success: Color @Composable get() = LocalWattRampColors.current.success
val Warning: Color @Composable get() = LocalWattRampColors.current.warning
val Error: Color @Composable get() = LocalWattRampColors.current.error
val InZone: Color @Composable get() = LocalWattRampColors.current.inZone
val OutOfZone: Color @Composable get() = LocalWattRampColors.current.outOfZone
val Zone1: Color @Composable get() = LocalWattRampColors.current.zone1
val Zone2: Color @Composable get() = LocalWattRampColors.current.zone2
val Zone3: Color @Composable get() = LocalWattRampColors.current.zone3
val Zone4: Color @Composable get() = LocalWattRampColors.current.zone4
val Zone5: Color @Composable get() = LocalWattRampColors.current.zone5
val Zone6: Color @Composable get() = LocalWattRampColors.current.zone6
val Zone7: Color @Composable get() = LocalWattRampColors.current.zone7

// =============================================================================
// TYPOGRAPHY
// =============================================================================

val WattRampTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// =============================================================================
// THEME COMPOSABLE
// =============================================================================

@Composable
fun WattRampTheme(
    appTheme: AppTheme = AppTheme.ORANGE,
    content: @Composable () -> Unit
) {
    val colors = when (appTheme) {
        AppTheme.ORANGE -> OrangeColors
        AppTheme.BLUE -> BlueColors
    }

    val colorScheme = darkColorScheme(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryDark,
        secondary = colors.secondary,
        onSecondary = colors.onPrimary,
        secondaryContainer = colors.primaryDark,
        tertiary = colors.accent,
        background = colors.background,
        onBackground = colors.onBackground,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceVariant = colors.surfaceVariant,
        onSurfaceVariant = colors.onSurfaceVariant,
        error = colors.error,
        onError = Color.White
    )

    CompositionLocalProvider(LocalWattRampColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = WattRampTypography,
            content = content
        )
    }
}
