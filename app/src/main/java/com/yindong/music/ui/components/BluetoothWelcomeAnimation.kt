package com.yindong.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseOut
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.yindong.music.ui.theme.AccentCyan
import com.yindong.music.ui.theme.NebulaBlue
import com.yindong.music.ui.theme.NebulaViolet
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun BluetoothWelcomeAnimation(
    isVisible: Boolean,
    deviceName: String?,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showContent by remember { mutableStateOf(false) }
    var animationPhase by remember { mutableStateOf(0) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            showContent = true
            animationPhase = 0
            delay(500)
            animationPhase = 1
            delay(1500)
            animationPhase = 2
            delay(1000)
            animationPhase = 3
            delay(800)
            showContent = false
            delay(300)
            onAnimationComplete()
        }
    }

    AnimatedVisibility(
        visible = isVisible && showContent,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        val isDark = isSystemInDarkTheme()
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDark) {
                            listOf(
                                Color(0xFF0D0D1A).copy(alpha = 0.97f),
                                Color(0xFF1A1A2E).copy(alpha = 0.97f),
                                Color(0xFF0D0D1A).copy(alpha = 0.97f)
                            )
                        } else {
                            listOf(
                                Color(0xFFF8F9FF).copy(alpha = 0.97f),
                                Color(0xFFEEF0FF).copy(alpha = 0.97f),
                                Color(0xFFF8F9FF).copy(alpha = 0.97f)
                            )
                        }
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            ParticleBackground(animationPhase >= 1)
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                BluetoothIconAnimation(
                    phase = animationPhase,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                WelcomeTextAnimation(
                    phase = animationPhase,
                    deviceName = deviceName,
                    isDark = isDark
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                FeatureBadgesAnimation(phase = animationPhase)
                
                Spacer(modifier = Modifier.height(40.dp))
                
                SoundWaveAnimation(
                    isVisible = animationPhase >= 2,
                    isDark = isDark
                )
            }
            
            if (animationPhase >= 3) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    NebulaBlue.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                radius = 800f
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun ParticleBackground(active: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    val particles = remember {
        List(30) { index ->
            ParticleData(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                size = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.3f,
                delay = Random.nextFloat() * 1000f
            )
        }
    }
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        if (!active) return@Canvas
        
        particles.forEach { particle ->
            val progress = ((time * 10000 + particle.delay) % 10000) / 10000
            val alpha = when {
                progress < 0.1f -> progress * 10
                progress > 0.9f -> (1f - progress) * 10
                else -> 1f
            } * 0.6f
            
            val x = particle.startX * size.width
            val y = (particle.startY + progress * particle.speed) % 1f * size.height
            
            drawCircle(
                color = NebulaBlue.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

private data class ParticleData(
    val startX: Float,
    val startY: Float,
    val size: Float,
    val speed: Float,
    val delay: Float
)

@Composable
private fun BluetoothIconAnimation(
    phase: Int,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bluetooth_icon")
    
    val scale by animateFloatAsState(
        targetValue = when (phase) {
            0 -> 0.3f
            1 -> 1f
            2 -> 1.1f
            else -> 1.2f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val rotation by animateFloatAsState(
        targetValue = when (phase) {
            0 -> -180f
            1 -> 0f
            else -> 0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "rotation"
    )
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_scale"
    )
    
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_alpha"
    )
    
    Box(
        modifier = Modifier.size(140.dp),
        contentAlignment = Alignment.Center
    ) {
        if (phase >= 1) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                
                for (i in 0..2) {
                    val delay = i * 0.33f
                    val adjustedScale = ((ringScale + delay) % 1f) * 1.5f + 1f
                    val adjustedAlpha = ringAlpha * (1f - (adjustedScale - 1f) / 1.5f)
                    
                    drawCircle(
                        color = NebulaBlue.copy(alpha = adjustedAlpha * 0.3f),
                        radius = size.minDimension * 0.25f * adjustedScale,
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }
        
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(scale * (if (phase >= 1) pulseScale else 1f))
                .graphicsLayer { rotationZ = rotation }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            NebulaBlue.copy(alpha = 0.3f * glowAlpha),
                            NebulaViolet.copy(alpha = 0.15f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = 150f
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                NebulaBlue,
                                NebulaViolet
                            )
                        )
                    )
                    .drawBehind {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.3f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.3f, size.height * 0.3f)
                            ),
                            radius = size.minDimension
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        
        if (phase >= 2) {
            HeadsetFloatingIcons()
        }
    }
}

@Composable
private fun HeadsetFloatingIcons() {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_icons")
    
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )
    
    val positions = remember {
        listOf(
            Pair(70f, -60f),
            Pair(-70f, -50f),
            Pair(80f, 50f),
            Pair(-75f, 60f)
        )
    }
    
    positions.forEachIndexed { index, (baseX, baseY) ->
        val angleOffset = index * PI / 2
        val floatX = (cos(time * 2 * PI + angleOffset) * 8).toFloat()
        val floatY = (sin(time * 2 * PI + angleOffset) * 8).toFloat()
        
        Icon(
            imageVector = if (index % 2 == 0) Icons.Default.Headphones else Icons.Default.MusicNote,
            contentDescription = null,
            tint = NebulaBlue.copy(alpha = 0.6f),
            modifier = Modifier
                .offset(x = baseX.dp + floatX.dp, y = baseY.dp + floatY.dp)
                .size(24.dp)
        )
    }
}

@Composable
private fun WelcomeTextAnimation(
    phase: Int,
    deviceName: String?,
    isDark: Boolean
) {
    val titleAlpha by animateFloatAsState(
        targetValue = if (phase >= 1) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label = "title_alpha"
    )
    
    val titleOffset by animateFloatAsState(
        targetValue = if (phase >= 1) 0f else 20f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "title_offset"
    )
    
    val subtitleAlpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(500, easing = EaseOut),
        label = "subtitle_alpha"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "蓝牙耳机已连接",
            color = if (isDark) Color.White else Color(0xFF1A1A2E),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .alpha(titleAlpha)
                .offset(y = titleOffset.dp)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = deviceName ?: "Bluetooth Headset",
            color = NebulaBlue,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(titleAlpha)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "3D 空间音效已就绪",
            color = if (isDark) Color(0xFFB8B8D1) else Color(0xFF666680),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(subtitleAlpha)
        )
    }
}

@Composable
private fun FeatureBadgesAnimation(phase: Int) {
    val alpha by animateFloatAsState(
        targetValue = if (phase >= 2) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "badges_alpha"
    )
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.alpha(alpha)
    ) {
        FeatureBadge("双耳独立渲染", NebulaBlue)
        FeatureBadge("沉浸式体验", NebulaViolet)
        FeatureBadge("低延迟", AccentCyan)
    }
}

@Composable
private fun FeatureBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.15f))
            .border(
                width = 1.dp,
                color = color.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SoundWaveAnimation(
    isVisible: Boolean,
    isDark: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_wave")
    
    val waveValues = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 600 + index * 100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave_$index"
        )
    }
    
    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            waveValues.forEach { wave ->
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((20 + wave.value * 20).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    NebulaBlue,
                                    NebulaViolet
                                )
                            )
                        )
                )
            }
        }
    }
}
