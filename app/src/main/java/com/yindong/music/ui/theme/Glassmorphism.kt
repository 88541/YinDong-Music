package com.yindong.music.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 毛玻璃效果工具类 - 通透轻盈风格
 * 提供各种毛玻璃和拟态风格的修饰符和组件
 */

/**
 * 玻璃态背景修饰符 - 基础版本
 * 通透轻盈风格，低透明度背景
 */
fun Modifier.glassmorphism(
    backgroundColor: Color = GlassDarkSurface,
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 0.5.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))

/**
 * 玻璃态背景修饰符 - 带模糊效果（Android 12+）
 * backdrop-filter: blur(20px) 效果
 */
@Composable
fun Modifier.glassmorphismBlur(
    backgroundColor: Color = GlassDarkSurface,
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 0.5.dp,
    blurRadius: Dp = 20.dp,
): Modifier {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return if (canBlur) {
        this
            .blur(blurRadius, BlurredEdgeTreatment(RoundedCornerShape(cornerRadius)))
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))
    } else {
        this.glassmorphism(backgroundColor, borderColor, cornerRadius, borderWidth)
    }
}

/**
 * 拟态风格修饰符 - 凸起效果
 * 柔和阴影，营造层次感
 */
fun Modifier.neumorphismElevated(
    backgroundColor: Color = NeumorphDarkBackground,
    shadowLight: Color = NeumorphShadowLight,
    shadowDark: Color = NeumorphShadowDark,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 6.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .drawBehind {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        // 上层光效（左上）- 柔和
        frameworkPaint.color = shadowLight.toArgb()
        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            -elevation.toPx() / 3,
            -elevation.toPx() / 3,
            shadowLight.toArgb()
        )
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(0f, 0f),
            size = Size(size.width / 2, size.height / 2)
        )

        // 下层阴影（右下）- 柔和
        frameworkPaint.color = shadowDark.toArgb()
        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            elevation.toPx() / 3,
            elevation.toPx() / 3,
            shadowDark.toArgb()
        )
        drawRect(
            color = Color.Transparent,
            topLeft = Offset(size.width / 2, size.height / 2),
            size = Size(size.width / 2, size.height / 2)
        )
    }

/**
 * 拟态风格修饰符 - 凹陷效果
 */
fun Modifier.neumorphismPressed(
    backgroundColor: Color = NeumorphDarkBackground,
    shadowLight: Color = NeumorphShadowLight,
    shadowDark: Color = NeumorphShadowDark,
    cornerRadius: Dp = 20.dp,
    elevation: Dp = 3.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(backgroundColor)
    .drawBehind {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()

        // 内阴影效果
        frameworkPaint.color = shadowDark.toArgb()
        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            -elevation.toPx() / 3,
            -elevation.toPx() / 3,
            shadowDark.toArgb()
        )

        frameworkPaint.color = shadowLight.toArgb()
        frameworkPaint.setShadowLayer(
            elevation.toPx(),
            elevation.toPx() / 3,
            elevation.toPx() / 3,
            shadowLight.toArgb()
        )
    }

/**
 * 渐变玻璃态修饰符
 * 清透渐变效果
 */
fun Modifier.glassmorphismGradient(
    gradientColors: List<Color> = listOf(
        PrimaryPurple.copy(alpha = 0.15f),
        AccentCyan.copy(alpha = 0.1f)
    ),
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 0.5.dp,
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(Brush.linearGradient(gradientColors))
    .border(borderWidth, borderColor, RoundedCornerShape(cornerRadius))

/**
 * 玻璃态卡片组件 - 通透轻盈
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = GlassBorder,
    cornerRadius: Dp = 20.dp,
    borderWidth: Dp = 0.5.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glassmorphism(backgroundColor, borderColor, cornerRadius, borderWidth),
        content = content
    )
}

/**
 * 拟态按钮容器
 */
@Composable
fun NeumorphButton(
    modifier: Modifier = Modifier,
    pressed: Boolean = false,
    backgroundColor: Color = NeumorphDarkBackground,
    cornerRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = if (pressed) {
            modifier.neumorphismPressed(backgroundColor = backgroundColor, cornerRadius = cornerRadius)
        } else {
            modifier.neumorphismElevated(backgroundColor = backgroundColor, cornerRadius = cornerRadius)
        },
        content = content
    )
}

/**
 * 玻璃态底部导航背景 - 通透轻盈
 */
@Composable
fun GlassBottomNav(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glassmorphism(
                backgroundColor = GlassDarkSurface.copy(alpha = 0.6f),
                borderColor = GlassBorder,
                cornerRadius = 28.dp,
                borderWidth = 0.5.dp
            ),
        content = content
    )
}

/**
 * 玻璃态顶部栏背景
 */
@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .glassmorphism(
                backgroundColor = GlassDarkSurface.copy(alpha = 0.5f),
                borderColor = GlassBorder,
                cornerRadius = 0.dp,
                borderWidth = 0.dp
            ),
        content = content
    )
}

/**
 * 动态模糊背景（需要Android 12+）
 * backdrop-filter: blur(20px)
 */
@Composable
fun BlurBackground(
    modifier: Modifier = Modifier,
    blurRadius: Dp = 20.dp,
    backgroundColor: Color = GlassDarkBackground,
    content: @Composable BoxScope.() -> Unit,
) {
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    Box(
        modifier = if (canBlur) {
            modifier
                .blur(blurRadius, BlurredEdgeTreatment.Unbounded)
                .background(backgroundColor)
        } else {
            modifier.background(backgroundColor)
        },
        content = content
    )
}

/**
 * 柔和阴影修饰符 - 轻盈通透
 */
fun Modifier.softShadow(
    color: Color = Color(0x20000000),
    blurRadius: Dp = 12.dp,
    offsetY: Dp = 3.dp,
): Modifier = this.drawBehind {
    val paint = Paint()
    val frameworkPaint = paint.asFrameworkPaint()
    frameworkPaint.color = color.toArgb()
    frameworkPaint.setShadowLayer(
        blurRadius.toPx(),
        0f,
        offsetY.toPx(),
        color.toArgb()
    )
    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.drawRect(
            0f,
            0f,
            size.width,
            size.height,
            frameworkPaint
        )
        canvas.nativeCanvas.restore()
    }
}

/**
 * 内发光效果修饰符 - 柔和
 */
fun Modifier.innerGlow(
    color: Color = PrimaryPurple.copy(alpha = 0.2f),
    blurRadius: Dp = 16.dp,
): Modifier = this.drawWithContent {
    drawContent()
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(color, Color.Transparent),
            center = center,
            radius = size.minDimension / 2
        ),
        size = size
    )
}

/**
 * 获取当前主题的玻璃态颜色
 */
@Composable
fun glassColors(): GlassColors {
    val isDark = isDarkTheme()
    return if (isDark) {
        GlassColors(
            background = GlassDarkBackground,
            surface = GlassDarkSurface,
            surfaceVariant = GlassDarkSurfaceVariant,
            surfaceElevated = GlassDarkSurfaceElevated,
            border = GlassBorder,
            borderStrong = GlassBorderStrong,
            shadowLight = NeumorphShadowLight,
            shadowDark = NeumorphShadowDark,
        )
    } else {
        GlassColors(
            background = GlassLightBackground,
            surface = GlassLightSurface,
            surfaceVariant = GlassLightSurfaceVariant,
            surfaceElevated = GlassLightSurfaceElevated,
            border = Color(0x15000000),
            borderStrong = Color(0x25000000),
            shadowLight = NeumorphShadowLight,
            shadowDark = NeumorphShadowDark,
        )
    }
}

/**
 * 玻璃态颜色数据类
 */
data class GlassColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val border: Color,
    val borderStrong: Color,
    val shadowLight: Color,
    val shadowDark: Color,
)
