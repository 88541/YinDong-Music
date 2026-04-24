package com.yindong.music.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class GlassButtonStyle {
    FILLED,
    OUTLINED,
    GRADIENT,
    GHOST,
    NEUMORPHIC
}

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassButtonStyle = GlassButtonStyle.GRADIENT,
    gradientColors: List<Color> = ButtonGradientPrimary,
    contentColor: Color = Color.White,
    cornerRadius: Dp = 16.dp,
    borderWidth: Dp = 1.dp,
    paddingValues: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    val isDark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isDark) Color(0x15151828) else Color(0x80E2E8F0)
            isPressed -> if (isDark) ButtonGlassBackgroundPressedDark else ButtonGlassBackgroundPressedLight
            else -> if (isDark) ButtonGlassBackgroundDark else ButtonGlassBackgroundLight
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            !enabled -> if (isDark) Color(0x20FFFFFF) else Color(0x10000000)
            isPressed -> if (isDark) ButtonBorderActiveDark else ButtonBorderActiveLight
            else -> if (isDark) ButtonBorderDark else ButtonBorderLight
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "borderColor"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                when (style) {
                    GlassButtonStyle.GRADIENT -> Modifier
                        .background(Brush.linearGradient(gradientColors))
                        .drawBehind {
                            drawRect(
                                color = Color.White.copy(alpha = if (isPressed) 0.15f else 0.08f)
                            )
                        }
                        .shadow(
                            elevation = if (isPressed) 4.dp else 8.dp,
                            shape = shape,
                            ambientColor = if (isDark) ButtonShadowGlowDark else ButtonShadowGlowLight,
                            spotColor = if (isDark) ButtonShadowGlowDark else ButtonShadowGlowLight
                        )

                    GlassButtonStyle.FILLED -> Modifier
                        .background(backgroundColor)
                        .border(borderWidth, borderColor, shape)
                        .drawBehind {
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryPurple.copy(alpha = 0.12f),
                                        Color.Transparent
                                    ),
                                    center = Offset(size.width * 0.3f, size.height * 0.3f),
                                    radius = size.maxDimension * 0.6f
                                )
                            )
                        }

                    GlassButtonStyle.OUTLINED -> Modifier
                        .background(backgroundColor.copy(alpha = 0.3f))
                        .border(borderWidth, borderColor, shape)

                    GlassButtonStyle.GHOST -> Modifier
                        .background(Color.Transparent)

                    GlassButtonStyle.NEUMORPHIC -> Modifier
                        .neumorphismElevated(
                            backgroundColor = backgroundColor,
                            cornerRadius = cornerRadius,
                            elevation = if (isPressed) 3.dp else 6.dp
                        )
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = enabled,
                role = Role.Button
            )
            .padding(paddingValues),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun GlassTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: GlassButtonStyle = GlassButtonStyle.GHOST,
    gradientColors: List<Color> = ButtonGradientPrimary,
    contentColor: Color = PrimaryPurple,
    cornerRadius: Dp = 12.dp,
    fontSize: Int = 14,
    fontWeight: FontWeight = FontWeight.Medium,
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        style = style,
        gradientColors = gradientColors,
        contentColor = contentColor,
        cornerRadius = cornerRadius,
        paddingValues = 12.dp,
    ) {
        Text(
            text = text,
            color = contentColor,
            fontSize = fontSize.sp,
            fontWeight = fontWeight,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    iconSize: Dp = 24.dp,
    tint: Color = if (isDarkTheme()) Color.White else PrimaryPurple,
    containerSize: Dp = 44.dp,
    showBackground: Boolean = true,
    style: GlassButtonStyle = GlassButtonStyle.OUTLINED,
) {
    val isDark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            isPressed -> if (isDark) IconButtonBgHoverDark else IconButtonBgHoverLight
            else -> if (showBackground) (if (isDark) IconButtonBgDark else IconButtonBgLight) else Color.Transparent
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "bgColor"
    )

    val borderAlpha by animateColorAsState(
        targetValue = when {
            isPressed -> if (isDark) Color(0x40FFFFFF) else Color(0x20000000)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "borderAlpha"
    )

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .then(
                if (style == GlassButtonStyle.NEUMORPHIC) {
                    Modifier.neumorphismElevated(
                        backgroundColor = bgColor,
                        cornerRadius = containerSize / 2,
                        elevation = if (isPressed) 2.dp else 4.dp
                    )
                } else {
                    Modifier
                        .background(bgColor)
                        .border(
                            width = if (isPressed) 0.8.dp else 0.dp,
                            color = borderAlpha,
                            shape = CircleShape
                        )
                        .drawBehind {
                            if (showBackground && isDark) {
                                drawCircle(
                                    color = PrimaryPurple.copy(alpha = if (isPressed) 0.12f else 0.06f)
                                )
                            }
                        }
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                enabled = enabled,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint.copy(alpha = if (enabled) 1f else 0.4f),
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GlassIconToggleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    activeIcon: ImageVector? = null,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeTint: Color = PrimaryPurple,
    inactiveTint: Color = if (isDarkTheme()) IconInactive else GlassTextTertiaryLight,
    iconSize: Dp = 24.dp,
    containerSize: Dp = 44.dp,
) {
    val isDark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            isActive -> if (isDark) Color(0x257B68EE) else Color(0x157B68EE)
            isPressed -> if (isDark) IconButtonBgHoverDark else IconButtonBgHoverLight
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "bgColor"
    )

    val currentTint = if (isActive) activeTint else inactiveTint
    val displayIcon = if (isActive && activeIcon != null) activeIcon else icon

    Box(
        modifier = modifier
            .size(containerSize)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (isActive || isPressed) 0.8.dp else 0.dp,
                color = if (isActive) (if (isDark) Color(0x407B68EE) else Color(0x307B68EE)) else Color.Transparent,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = displayIcon,
            contentDescription = contentDescription,
            tint = currentTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun GlassPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    gradientColors: List<Color> = ButtonGradientPrimary,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "scale"
    )

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(Brush.linearGradient(gradientColors))
            .drawBehind {
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = size.toPx() * 0.35f,
                    center = Offset(size.toPx() * 0.3f, size.toPx() * 0.25f)
                )
            }
            .shadow(
                elevation = 12.dp,
                shape = CircleShape,
                ambientColor = if (isDarkTheme()) ButtonShadowGlowDark else ButtonShadowGlowLight,
                spotColor = if (isDarkTheme()) ButtonShadowGlowDark else ButtonShadowGlowLight
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Filled.PlayArrow,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = Color.White,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

@Composable
fun GlassMiniPlayButton(
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val isDark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            isPressed -> if (isDark) Color(0x307B68EE) else Color(0x207B68EE)
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "bgColor"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 0.5.dp,
                color = if (isDark) Color(0x25FFFFFF) else Color(0x12000000),
                shape = CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
            contentDescription = if (isPlaying) "暂停" else "播放",
            tint = PrimaryPurple,
            modifier = Modifier.size(size * 0.75f)
        )
    }
}

@Composable
fun GlassChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectedColor: Color = PrimaryPurple,
    unselectedColor: Color = if (isDarkTheme()) GlassDarkSurfaceVariant else GlassLightSurfaceVariant,
) {
    val isDark = isDarkTheme()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isPressed && selected -> selectedColor.copy(alpha = 0.9f)
            isPressed && !selected -> unselectedColor.copy(alpha = 0.8f)
            selected -> selectedColor.copy(alpha = 0.75f)
            else -> unselectedColor.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )

    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else (if (isDark) GlassTextSecondaryLight else GlassTextSecondaryDark),
        animationSpec = tween(durationMillis = 150),
        label = "textColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (selected) selectedColor.copy(alpha = 0.5f) else Color.Transparent,
        animationSpec = tween(durationMillis = 150),
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .border(0.5.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                role = Role.Button
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.3.sp
        )
    }
}
