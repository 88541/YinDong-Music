package com.yindong.music.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 应用自定义颜色类 - 通透轻盈的毛玻璃拟态风格
 */
data class AppColors(
    // 主色调
    val accentPrimary: Color,
    val accentSecondary: Color,
    val accentTertiary: Color,

    // 文字颜色
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textHint: Color,

    // 玻璃态背景色 - 通透轻盈
    val glassBackground: Color,
    val glassSurface: Color,
    val glassSurfaceVariant: Color,
    val glassBorder: Color,

    // 拟态阴影色
    val neumorphShadowLight: Color,
    val neumorphShadowDark: Color,
    val neumorphBackground: Color,

    // 分隔线
    val divider: Color,

    // 标签颜色
    val tagBlue: Color,
    val tagGreen: Color,
    val tagOrange: Color,
    val tagPurple: Color,
    val tagPink: Color,
    val tagCyan: Color,

    // 渐变色系
    val gradientStart: Color,
    val gradientEnd: Color,

    // 状态色
    val success: Color,
    val warning: Color,
    val error: Color,
    val info: Color,
)

/**
 * 深色主题 - 通透轻盈的毛玻璃拟态风格
 * 深蓝黑渐变背景，半透明卡片，营造层次感
 */
val DarkAppColors = AppColors(
    // 主色调 - 中紫/亮青双色系
    accentPrimary = Color(0xFF7B68EE),
    accentSecondary = Color(0xFF00D2FF),
    accentTertiary = Color(0xFF9D8CF0),

    // 文字颜色 - 黑色
    textPrimary = Color(0xFF000000),
    textSecondary = Color(0xFF333333),
    textTertiary = Color(0xFF666666),
    textHint = Color(0x80000000),

    // 玻璃态背景色 - 深色不透明，文字清晰
    glassBackground = Color(0xFF0A0F1E),
    glassSurface = Color(0xFF151A28),
    glassSurfaceVariant = Color(0xFF1E2433),
    glassBorder = Color(0x30FFFFFF),

    // 拟态阴影色
    neumorphShadowLight = Color(0x20FFFFFF),
    neumorphShadowDark = Color(0x40000000),
    neumorphBackground = Color(0xFF1A1F2E),

    // 分隔线 - 极淡白色
    divider = Color(0x10FFFFFF),

    // 标签颜色 - 清透柔和
    tagBlue = Color(0xFF64B5F6),
    tagGreen = Color(0xFF81C784),
    tagOrange = Color(0xFFFFB74D),
    tagPurple = Color(0xFFBA68C8),
    tagPink = Color(0xFFF06292),
    tagCyan = Color(0xFF4DD0E1),

    // 渐变色系 - 深蓝黑渐变
    gradientStart = Color(0xFF0A0F1E),
    gradientEnd = Color(0xFF1A1F2E),

    // 状态色
    success = Color(0xFF81C784),
    warning = Color(0xFFFFB74D),
    error = Color(0xFFE57373),
    info = Color(0xFF64B5F6),
)

/**
 * 浅色主题 - 通透轻盈的毛玻璃拟态风格
 */
val LightAppColors = AppColors(
    // 主色调
    accentPrimary = Color(0xFF7B68EE),
    accentSecondary = Color(0xFF00D2FF),
    accentTertiary = Color(0xFF9D8CF0),

    // 文字颜色
    textPrimary = Color(0xFF1A1F2E),
    textSecondary = Color(0xFF4A5568),
    textTertiary = Color(0xFF718096),
    textHint = Color(0x801A1F2E),

    // 玻璃态背景色 - 半透明白色
    glassBackground = Color(0xFFF7FAFC),
    glassSurface = Color(0xCCFFFFFF),
    glassSurfaceVariant = Color(0xE6FFFFFF),
    glassBorder = Color(0x20000000),

    // 拟态阴影色
    neumorphShadowLight = Color(0xFFFFFFFF),
    neumorphShadowDark = Color(0x20000000),
    neumorphBackground = Color(0xFFE2E8F0),

    // 分隔线
    divider = Color(0x15000000),

    // 标签颜色
    tagBlue = Color(0xFF4299E1),
    tagGreen = Color(0xFF48BB78),
    tagOrange = Color(0xFFED8936),
    tagPurple = Color(0xFF9F7AEA),
    tagPink = Color(0xFFED64A6),
    tagCyan = Color(0xFF38B2AC),

    // 渐变色系
    gradientStart = Color(0xFFF7FAFC),
    gradientEnd = Color(0xFFE2E8F0),

    // 状态色
    success = Color(0xFF48BB78),
    warning = Color(0xFFED8936),
    error = Color(0xFFF56565),
    info = Color(0xFF4299E1),
)

/**
 * LocalComposition 用于在 Compose 中传递 AppColors
 */
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// ============================================
// 通透轻盈的毛玻璃拟态风格颜色常量
// ============================================

// 主色调 - 中紫/亮青
val PrimaryPurple = Color(0xFF7B68EE)
val PrimaryPurpleLight = Color(0xFF9D8CF0)
val PrimaryPurpleDark = Color(0xFF6B5DD3)

// 辅助色 - 亮青
val AccentCyan = Color(0xFF00D2FF)
val AccentCyanLight = Color(0xFF5CE1FF)
val AccentCyanDark = Color(0xFF00B8E6)

// 深蓝黑渐变背景
val DeepBlueBlack = Color(0xFF0A0F1E)
val DeepBlueBlackLight = Color(0xFF1A1F2E)

// 深色背景 - 通透轻盈毛玻璃风格
val GlassDarkBackground = Color(0xFF0A0F1E)
val GlassDarkSurface = Color(0xFF151A28)
val GlassDarkSurfaceVariant = Color(0xFF1E2433)
val GlassDarkSurfaceElevated = Color(0xFF283040)

// 浅色背景 - 通透轻盈毛玻璃风格
val GlassLightBackground = Color(0xFFF7FAFC)
val GlassLightSurface = Color(0xCCFFFFFF)
val GlassLightSurfaceVariant = Color(0xE6FFFFFF)
val GlassLightSurfaceElevated = Color(0xFFFFFFFF)

// 拟态背景色
val NeumorphDarkBackground = Color(0xFF1A1F2E)
val NeumorphLightBackground = Color(0xFFE2E8F0)

// 拟态阴影色 - 柔和
val NeumorphShadowLight = Color(0x20FFFFFF)
val NeumorphShadowDark = Color(0x40000000)
val NeumorphShadowLightStrong = Color(0x30FFFFFF)
val NeumorphShadowDarkStrong = Color(0x50000000)

// 玻璃边框色
val GlassBorder = Color(0x30FFFFFF)
val GlassBorderStrong = Color(0x50FFFFFF)

// 文字颜色 - 深色主题
val GlassTextPrimaryDark = Color(0xFF000000)
val GlassTextSecondaryDark = Color(0xFF333333)
val GlassTextTertiaryDark = Color(0xFF666666)

// 文字颜色 - 浅色主题
val GlassTextPrimaryLight = Color(0xFF1A1F2E)
val GlassTextSecondaryLight = Color(0xFF4A5568)
val GlassTextTertiaryLight = Color(0xFF718096)

// 图标颜色
val IconInactive = Color(0xFFD4E0FF)
val IconActive = PrimaryPurple

// 分割线
val DividerGlass = Color(0x10FFFFFF)

// 渐变颜色集合 - 清透风格
val GlassGradients: List<List<Color>> = listOf(
    listOf(Color(0xFF7B68EE), Color(0xFF00D2FF)), // 紫青渐变
    listOf(Color(0xFF00D2FF), Color(0xFF5CE1FF)), // 青蓝渐变
    listOf(Color(0xFFF06292), Color(0xFFEC407A)), // 粉红渐变
    listOf(Color(0xFF81C784), Color(0xFF66BB6A)), // 绿色渐变
    listOf(Color(0xFFFFB74D), Color(0xFFFFA726)), // 橙色渐变
    listOf(Color(0xFFBA68C8), Color(0xFFAB47BC)), // 紫红渐变
    listOf(Color(0xFF64B5F6), Color(0xFF42A5F5)), // 蓝色渐变
    listOf(Color(0xFF4DD0E1), Color(0xFF26C6DA)), // 青绿渐变
    listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)), // 靛蓝渐变
    listOf(Color(0xFFFF8A65), Color(0xFFFF7043)), // 深橙渐变
)

// 标签颜色 - 清透柔和
val TagRedSoft = Color(0xFFEF5350)
val TagBlueSoft = Color(0xFF64B5F6)
val TagGreenSoft = Color(0xFF81C784)
val TagOrangeSoft = Color(0xFFFFB74D)
val TagPurpleSoft = Color(0xFFBA68C8)
val TagPinkSoft = Color(0xFFF06292)
val TagCyanSoft = Color(0xFF4DD0E1)

// 状态色 - 柔和清透
val SuccessSoft = Color(0xFF81C784)
val WarningSoft = Color(0xFFFFB74D)
val ErrorSoft = Color(0xFFE57373)
val InfoSoft = Color(0xFF64B5F6)

// 兼容旧代码的颜色别名
val AccentRed = PrimaryPurple
val AccentRedLight = PrimaryPurpleLight
val AccentRedDark = PrimaryPurpleDark
val AccentBlue = AccentCyan
val AccentBlueLight = AccentCyanLight
val AccentBlueDark = AccentCyanDark
val DarkBackground = GlassDarkBackground
val DarkSurface = GlassDarkSurface
val DarkSurfaceVariant = GlassDarkSurfaceVariant
val LightBackground = GlassLightBackground
val LightSurface = GlassLightSurface
val LightSurfaceVariant = GlassLightSurfaceVariant
val TextPrimary = GlassTextPrimaryDark
val TextSecondary = GlassTextSecondaryDark
val TextTertiary = GlassTextTertiaryDark
val TextHint = Color(0x80000000)
val GradientStart = DeepBlueBlack
val GradientEnd = DeepBlueBlackLight
val Red500 = PrimaryPurple
val Red600 = PrimaryPurpleDark
val TagRed = TagRedSoft
val TagBlue = TagBlueSoft
val TagGreen = TagGreenSoft
val TagOrange = TagOrangeSoft
val TagPurple = TagPurpleSoft
val DividerColor = DividerGlass
val NebulaPink = TagPinkSoft
val NebulaViolet = PrimaryPurple
val NebulaBlue = AccentCyan

// 封面渐变色集合 - 兼容旧代码
val CoverGradients: List<List<Color>> = GlassGradients

// ============================================
// 按钮专用颜色 - 毛玻璃拟态风格
// ============================================

// 按钮玻璃背景色 - 深色主题 (更透明的毛玻璃效果)
val ButtonGlassBackgroundDark = Color(0x3D151A28)
val ButtonGlassBackgroundHoverDark = Color(0x4D1E2433)
val ButtonGlassBackgroundPressedDark = Color(0x550F1424)

// 按钮玻璃背景色 - 浅色主题
val ButtonGlassBackgroundLight = Color(0xBFFFFFFF)
val ButtonGlassBackgroundHoverLight = Color(0xCCFFFFFF)
val ButtonGlassBackgroundPressedLight = Color(0x99FFFFFF)

// 按钮渐变色
val ButtonGradientPrimary = listOf(Color(0xFF7B68EE), Color(0xFF00D2FF))
val ButtonGradientSecondary = listOf(Color(0xFF00D2FF), Color(0xFF5CE1FF))
val ButtonGradientAccent = listOf(Color(0xFFF06292), Color(0xFF9D8CF0))
val ButtonGradientSuccess = listOf(Color(0xFF81C784), Color(0xFF4DD0E1))
val ButtonGradientWarning = listOf(Color(0xFFFFB74D), Color(0xFFFF8A65))

// 按钮边框色 (毛玻璃风格 - 更柔和)
val ButtonBorderDark = Color(0x35FFFFFF)
val ButtonBorderLight = Color(0x1A000000)
val ButtonBorderActiveDark = Color(0x557B68EE)
val ButtonBorderActiveLight = Color(0x757B68EE)

// 按钮阴影色 (更柔和的光晕)
val ButtonShadowDark = Color(0x20000000)
val ButtonShadowGlowDark = Color(0x357B68EE)
val ButtonShadowLight = Color(0x0E00000)
val ButtonShadowGlowLight = Color(0x257B68EE)

// 图标按钮样式 (毛玻璃效果)
val IconButtonBgDark = Color(0x18151828)
val IconButtonBgHoverDark = Color(0x281E2433)
val IconButtonBgLight = Color(0x10FFFFFF)
val IconButtonBgHoverLight = Color(0x20FFFFFF)

// ============================================
// 卡片/快捷入口专用 - 毛玻璃拟态风格
// ============================================

// 快捷入口卡片背景 (半透明毛玻璃)
val CardGlassBackgroundDark = Color(0x35151A28)
val CardGlassBackgroundLight = Color(0xC8FFFFFF)

// 卡片边框 (柔和光晕)
val CardGlassBorderDark = Color(0x30FFFFFF)
val CardGlassBorderLight = Color(0x18000000)

// 卡片高光效果 (内发光)
val CardGlassHighlightDark = Color(0x15FFFFFF)
val CardGlassHighlightLight = Color(0x25FFFFFF)
