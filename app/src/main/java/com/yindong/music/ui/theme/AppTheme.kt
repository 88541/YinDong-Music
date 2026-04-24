package com.yindong.music.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppTheme(
    val id: String,
    val name: String,
    val nameEn: String,
    val isDark: Boolean,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color,
    val surfaceColor: Color,
    val accentGradientStart: Color,
    val accentGradientEnd: Color,
)

val AppThemes = listOf(
    AppTheme(
        id = "dark_glass",
        name = "深色毛玻璃",
        nameEn = "Dark Glass",
        isDark = true,
        primaryColor = PrimaryPurple,
        secondaryColor = AccentCyan,
        backgroundColor = GlassDarkBackground,
        surfaceColor = GlassDarkSurface,
        accentGradientStart = PrimaryPurple,
        accentGradientEnd = AccentCyan,
    ),
    AppTheme(
        id = "light_glass",
        name = "浅色毛玻璃",
        nameEn = "Light Glass",
        isDark = false,
        primaryColor = PrimaryPurple,
        secondaryColor = AccentCyan,
        backgroundColor = GlassLightBackground,
        surfaceColor = GlassLightSurface,
        accentGradientStart = PrimaryPurple,
        accentGradientEnd = AccentCyan,
    ),
    AppTheme(
        id = "dark_night_violet",
        name = "暗夜紫",
        nameEn = "Night Violet",
        isDark = true,
        primaryColor = Color(0xFF9B59B6),
        secondaryColor = Color(0xFF8E44AD),
        backgroundColor = Color(0xFF0D0D1A),
        surfaceColor = Color(0xFF161629),
        accentGradientStart = Color(0xFF9B59B6),
        accentGradientEnd = Color(0xFF6C3483),
    ),
    AppTheme(
        id = "dark_aurora_blue",
        name = "极光蓝",
        nameEn = "Aurora Blue",
        isDark = true,
        primaryColor = Color(0xFF3498DB),
        secondaryColor = Color(0xFF2980B9),
        backgroundColor = Color(0xFF0A1628),
        surfaceColor = Color(0xFF152238),
        accentGradientStart = Color(0xFF3498DB),
        accentGradientEnd = Color(0xFF1ABC9C),
    ),
    AppTheme(
        id = "dark_rose_gold",
        name = "玫瑰金",
        nameEn = "Rose Gold",
        isDark = true,
        primaryColor = Color(0xFFE74C3C),
        secondaryColor = Color(0xFFF39C12),
        backgroundColor = Color(0xFF1A1210),
        surfaceColor = Color(0xFF2A1F1C),
        accentGradientStart = Color(0xFFE74C3C),
        accentGradientEnd = Color(0xFFF39C12),
    ),
    AppTheme(
        id = "light_sakura",
        name = "樱花粉",
        nameEn = "Sakura Pink",
        isDark = false,
        primaryColor = Color(0xFFFF6B9D),
        secondaryColor = Color(0xFFC44569),
        backgroundColor = Color(0xFFFFF5F7),
        surfaceColor = Color(0xFFFFFFFF),
        accentGradientStart = Color(0xFFFF6B9D),
        accentGradientEnd = Color(0xFFFFB8D0),
    ),
    AppTheme(
        id = "dark_forest_green",
        name = "森林绿",
        nameEn = "Forest Green",
        isDark = true,
        primaryColor = Color(0xFF27AE60),
        secondaryColor = Color(0xFF2ECC71),
        backgroundColor = Color(0xFF0A1610),
        surfaceColor = Color(0xFF142420),
        accentGradientStart = Color(0xFF27AE60),
        accentGradientEnd = Color(0xFF2ECC71),
    ),
    AppTheme(
        id = "light_ocean_wave",
        name = "海浪蓝",
        nameEn = "Ocean Wave",
        isDark = false,
        primaryColor = Color(0xFF00B4DB),
        secondaryColor = Color(0xFF0083B0),
        backgroundColor = Color(0xFFF0F9FF),
        surfaceColor = Color(0xFFFFFFFF),
        accentGradientStart = Color(0xFF00B4DB),
        accentGradientEnd = Color(0xFF0083B0),
    ),
)

fun getThemeById(themeId: String): AppTheme {
    return AppThemes.find { it.id == themeId } ?: AppThemes[0]
}

@Composable
fun getColorSchemeForTheme(theme: AppTheme): ColorScheme {
    return if (theme.isDark) {
        darkColorScheme(
            primary = theme.primaryColor,
            onPrimary = Color.White,
            primaryContainer = theme.primaryColor.copy(alpha = 0.2f),
            onPrimaryContainer = Color.White,

            secondary = theme.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = theme.secondaryColor.copy(alpha = 0.15f),
            onSecondaryContainer = Color.White,

            tertiary = TagPinkSoft,
            onTertiary = Color.White,
            tertiaryContainer = TagPinkSoft.copy(alpha = 0.15f),
            onTertiaryContainer = Color.White,

            background = theme.backgroundColor,
            onBackground = if (theme.isDark) Color.White else Color(0xFF1A1F2E),

            surface = theme.surfaceColor,
            onSurface = if (theme.isDark) Color.White else Color(0xFF1A1F2E),
            surfaceVariant = theme.surfaceColor.copy(alpha = if (theme.isDark) 0.85f else 0.95f),
            onSurfaceVariant = if (theme.isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF4A5568),

            surfaceTint = theme.primaryColor.copy(alpha = 0.08f),
            surfaceContainerLowest = theme.backgroundColor,
            surfaceContainerLow = theme.backgroundColor.copy(alpha = if (theme.isDark) 0.95f else 1f),
            surfaceContainer = theme.surfaceColor,
            surfaceContainerHigh = if (theme.isDark) Color(0x33FFFFFF) else Color(0xFFFFFFFF),
            surfaceContainerHighest = if (theme.isDark) Color(0x30FFFFFF) else Color(0xFFFFFFFF),

            outline = if (theme.isDark) Color(0x30FFFFFF) else Color(0x15000000),
            outlineVariant = if (theme.isDark) Color(0x10FFFFFF) else Color(0x10000000),

            error = ErrorSoft,
            onError = Color.White,
            errorContainer = ErrorSoft.copy(alpha = 0.15f),
            onErrorContainer = Color.White,

            inverseSurface = if (theme.isDark) GlassLightSurface else GlassDarkSurface,
            inverseOnSurface = if (theme.isDark) GlassTextPrimaryLight else GlassTextPrimaryDark,
            inversePrimary = theme.primaryColor,

            scrim = Color(0x80000000),
        )
    } else {
        lightColorScheme(
            primary = theme.primaryColor,
            onPrimary = Color.White,
            primaryContainer = theme.primaryColor.copy(alpha = 0.1f),
            onPrimaryContainer = theme.primaryColor.copy(alpha = 0.9f),

            secondary = theme.secondaryColor,
            onSecondary = Color.White,
            secondaryContainer = theme.secondaryColor.copy(alpha = 0.1f),
            onSecondaryContainer = theme.secondaryColor.copy(alpha = 0.9f),

            tertiary = TagPinkSoft,
            onTertiary = Color.White,
            tertiaryContainer = TagPinkSoft.copy(alpha = 0.1f),
            onTertiaryContainer = Color(0xFFC2185B),

            background = theme.backgroundColor,
            onBackground = Color(0xFF1A1F2E),

            surface = theme.surfaceColor,
            onSurface = Color(0xFF1A1F2E),
            surfaceVariant = theme.surfaceColor.copy(alpha = 0.95f),
            onSurfaceVariant = Color(0xFF4A5568),

            surfaceTint = theme.primaryColor.copy(alpha = 0.05f),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = theme.backgroundColor,
            surfaceContainer = theme.surfaceColor,
            surfaceContainerHigh = Color(0xFFFFFFFF),
            surfaceContainerHighest = Color(0xFFFFFFFF),

            outline = Color(0x15000000),
            outlineVariant = Color(0x10000000),

            error = Color(0xFFF56565),
            onError = Color.White,
            errorContainer = Color(0xFFFFF5F5),
            onErrorContainer = Color(0xFFC53030),

            inverseSurface = GlassDarkSurface,
            inverseOnSurface = GlassTextPrimaryDark,
            inversePrimary = theme.primaryColor,

            scrim = Color(0x60000000),
        )
    }
}
