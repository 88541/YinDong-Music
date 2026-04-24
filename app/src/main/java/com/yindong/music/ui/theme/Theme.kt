package com.yindong.music.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 深色主题配色方案 - 通透轻盈的毛玻璃拟态风格
 * 深蓝黑渐变背景，半透明卡片，营造层次感
 */
private val DarkColorScheme = darkColorScheme(
    // 主色调 - 中紫/亮青
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurple.copy(alpha = 0.2f),
    onPrimaryContainer = Color.White,

    // 次要色调 - 亮青
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = AccentCyan.copy(alpha = 0.15f),
    onSecondaryContainer = Color.White,

    // 第三色调
    tertiary = TagPinkSoft,
    onTertiary = Color.White,
    tertiaryContainer = TagPinkSoft.copy(alpha = 0.15f),
    onTertiaryContainer = Color.White,

    // 背景色 - 深蓝黑渐变
    background = GlassDarkBackground,
    onBackground = GlassTextPrimaryDark,

    // 表面色 - 通透轻盈，低透明度
    surface = GlassDarkSurface,
    onSurface = GlassTextPrimaryDark,
    surfaceVariant = GlassDarkSurfaceVariant,
    onSurfaceVariant = GlassTextSecondaryDark,

    // 表面容器色 - 用于卡片、列表等
    surfaceTint = PrimaryPurple.copy(alpha = 0.08f),
    surfaceContainerLowest = Color(0xFF0A0F1E),
    surfaceContainerLow = Color(0xFF10152A),
    surfaceContainer = GlassDarkSurface,
    surfaceContainerHigh = GlassDarkSurfaceElevated,
    surfaceContainerHighest = Color(0x30FFFFFF),

    // 轮廓和分隔线 - 极淡
    outline = GlassBorder,
    outlineVariant = DividerGlass,

    // 错误状态
    error = ErrorSoft,
    onError = Color.White,
    errorContainer = ErrorSoft.copy(alpha = 0.15f),
    onErrorContainer = Color.White,

    // 反色
    inverseSurface = GlassLightSurface,
    inverseOnSurface = GlassTextPrimaryLight,
    inversePrimary = PrimaryPurpleLight,

    // 遮罩
    scrim = Color(0x80000000),
)

/**
 * 浅色主题配色方案 - 通透轻盈的毛玻璃拟态风格
 */
private val LightColorScheme = lightColorScheme(
    // 主色调 - 中紫/亮青
    primary = PrimaryPurple,
    onPrimary = Color.White,
    primaryContainer = PrimaryPurple.copy(alpha = 0.1f),
    onPrimaryContainer = PrimaryPurpleDark,

    // 次要色调 - 亮青
    secondary = AccentCyan,
    onSecondary = Color.White,
    secondaryContainer = AccentCyan.copy(alpha = 0.1f),
    onSecondaryContainer = AccentCyanDark,

    // 第三色调
    tertiary = TagPinkSoft,
    onTertiary = Color.White,
    tertiaryContainer = TagPinkSoft.copy(alpha = 0.1f),
    onTertiaryContainer = Color(0xFFC2185B),

    // 背景色 - 浅灰白
    background = GlassLightBackground,
    onBackground = GlassTextPrimaryLight,

    // 表面色 - 半透明白色
    surface = GlassLightSurface,
    onSurface = GlassTextPrimaryLight,
    surfaceVariant = GlassLightSurfaceVariant,
    onSurfaceVariant = GlassTextSecondaryLight,

    // 表面容器色
    surfaceTint = PrimaryPurple.copy(alpha = 0.05f),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7FAFC),
    surfaceContainer = GlassLightSurface,
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFFFFFFF),

    // 轮廓和分隔线
    outline = Color(0x15000000),
    outlineVariant = Color(0x10000000),

    // 错误状态
    error = Color(0xFFF56565),
    onError = Color.White,
    errorContainer = Color(0xFFFFF5F5),
    onErrorContainer = Color(0xFFC53030),

    // 反色
    inverseSurface = GlassDarkSurface,
    inverseOnSurface = GlassTextPrimaryDark,
    inversePrimary = PrimaryPurpleLight,

    // 遮罩
    scrim = Color(0x60000000),
)

@Composable
fun CloudMusicTheme(
    isDarkTheme: Boolean = true,
    appTheme: AppTheme? = null,
    content: @Composable () -> Unit,
) {
    val theme = appTheme ?: if (isDarkTheme) AppThemes[0] else AppThemes[1]
    val colorScheme = getColorSchemeForTheme(theme)
    val appColors = if (theme.isDark) DarkAppColors else LightAppColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            try {
                val activity = view.context as? Activity ?: return@SideEffect
                val window = activity.window ?: return@SideEffect
                if (!activity.isFinishing && !activity.isDestroyed) {
                    window.statusBarColor = Color.Transparent.toArgb()
                    window.navigationBarColor = Color.Transparent.toArgb()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        @Suppress("DEPRECATION")
                        window.isNavigationBarContrastEnforced = false
                    }

                    val controller = WindowCompat.getInsetsController(window, view)
                    controller.isAppearanceLightStatusBars = !theme.isDark
                    controller.isAppearanceLightNavigationBars = !theme.isDark
                }
            } catch (_: Exception) { }
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content,
        )
    }
}

/**
 * 获取当前主题颜色
 */
@Composable
fun appColors(): AppColors {
    return LocalAppColors.current
}

/**
 * 判断当前是否为深色主题
 */
@Composable
fun isDarkTheme(): Boolean {
    return LocalAppColors.current == DarkAppColors
}
