package com.yindong.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.yindong.music.ui.theme.AccentCyan
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.CardGlassHighlightDark
import com.yindong.music.ui.theme.CardGlassHighlightLight
import com.yindong.music.ui.theme.NebulaBlue
import com.yindong.music.ui.theme.NebulaViolet
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary

/**
 * 耳机连接提示横幅组件 - 带专属动画效果
 * 当检测到耳机连接时显示，提供3D音效入口
 */
@Composable
fun HeadsetBanner(
    isVisible: Boolean,
    deviceName: String?,
    onDismiss: () -> Unit,
    onEnterAudioEffect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var hasAnimated by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) + fadeIn(animationSpec = tween(500)),
        exit = shrinkVertically(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        ) + fadeOut(),
        modifier = modifier
    ) {
        LaunchedEffect(Unit) { hasAnimated = true }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val isDark = isSystemInDarkTheme()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
                    .border(
                        width = 1.dp,
                        color = if (isDark) CardGlassBorderDark else CardGlassBorderLight,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .drawBehind {
                        drawRect(
                            if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                            topLeft = Offset(0f, 0f),
                            size = Size(this.size.width, this.size.height * 0.35f)
                        )
                    }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BluetoothPulseIcon()
                        
                        Text(
                            text = "3D 耳机模式",
                            color = NebulaBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NebulaViolet.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Bluetooth",
                                color = NebulaViolet,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onDismiss() }
                            .background(Color.White.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedHeadsetIcon()

                    Column {
                        Text(
                            text = "精选歌单",
                            color = Color(0xFFFFD700),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "已检测到耳机接入 · ${deviceName ?: "蓝牙耳机"} · 双耳独立3D声场渲染已就绪",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NebulaViolet.copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = if (isDark) CardGlassBorderDark else CardGlassBorderLight,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .drawBehind {
                            drawRect(
                                if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                                topLeft = Offset(0f, 0f),
                                size = Size(this.size.width, this.size.height * 0.4f)
                            )
                        }
                        .clickable { onEnterAudioEffect() }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "进入音效中心",
                            color = NebulaBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "›",
                            color = NebulaBlue,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothPulseIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "bluetooth_pulse")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NebulaBlue.copy(alpha = 0.25f * alpha),
                        Color.Transparent
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(NebulaBlue.copy(alpha = 0.08f * alpha))
                .scale(scale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = "蓝牙",
                tint = NebulaBlue.copy(alpha = alpha),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AnimatedHeadsetIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "headset_animation")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headset_scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headset_glow"
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )

    Box(modifier = Modifier.size(44.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            val baseRadius = size.minDimension * 0.38f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        NebulaBlue.copy(alpha = 0.15f * glowAlpha),
                        Color.Transparent
                    ),
                    radius = baseRadius * ringScale
                ),
                center = Offset(centerX, centerY),
                radius = baseRadius * ringScale
            )
            
            drawCircle(
                color = NebulaBlue.copy(alpha = ringAlpha * 0.3f),
                center = Offset(centerX, centerY),
                radius = baseRadius * 0.85f * ringScale,
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f))
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            NebulaViolet.copy(alpha = 0.2f + 0.1f * glowAlpha),
                            NebulaBlue.copy(alpha = 0.1f + 0.05f * glowAlpha)
                        )
                    )
                )
                .scale(pulseScale),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = "耳机",
                tint = NebulaBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun HeadsetMiniBanner(
    isVisible: Boolean,
    deviceName: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    
    val infiniteTransition = rememberInfiniteTransition(label = "mini_headset")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mini_scale"
    )

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
                .border(
                    width = 1.dp,
                    color = if (isDark) CardGlassBorderDark else CardGlassBorderLight,
                    shape = RoundedCornerShape(16.dp)
                )
                .drawBehind {
                    drawRect(
                        if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                        topLeft = Offset(0f, 0f),
                        size = Size(this.size.width, this.size.height * 0.4f)
                    )
                }
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .scale(pulseScale),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Headphones,
                contentDescription = null,
                tint = NebulaBlue,
                modifier = Modifier.size(22.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "3D 耳机模式已开启",
                    color = NebulaBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = deviceName ?: "蓝牙耳机已连接",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Text(
                text = "进入音效 ›",
                color = NebulaBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
