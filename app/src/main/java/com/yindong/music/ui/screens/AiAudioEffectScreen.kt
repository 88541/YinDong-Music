package com.yindong.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.yindong.music.data.LocalStorage
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.CardGlassHighlightDark
import com.yindong.music.ui.theme.CardGlassHighlightLight
import com.yindong.music.viewmodel.MusicViewModel
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

// ══════════════════════════════════════════
// AI 音效 — 深色/浅色 双模式色板
// ══════════════════════════════════════════
internal data class AudioEffectColors(
    val primary: Color,
    val light: Color,
    val dim: Color,
    val glow: Color,
    val bg: Color,
    val surface: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val cardHighlight: Color,
    val purpleAccent: Color,
    val blueAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val lineColor: Color,
    val dotBg: Color,
)

internal val LocalAudioEffectColors = staticCompositionLocalOf {
    AudioEffectColors(
        primary = Color(0xFF6C5CE7), light = Color(0xFFA29BFE),
        dim = Color(0xFF636E72), glow = Color(0xFF8B80F9),
        bg = Color(0xFFF8F3FF), surface = Color(0xFFEDE8FC),
        cardBg = CardGlassBackgroundLight, cardBorder = CardGlassBorderLight,
        cardHighlight = CardGlassHighlightLight,
        purpleAccent = Color(0xFFA78BFA), blueAccent = Color(0xFF60A5FA),
        textPrimary = Color(0xFF2D3436), textSecondary = Color(0xFF636E72),
        lineColor = Color(0x15000000), dotBg = Color(0x15A78BFA),
    )
}

@Composable
private fun c(): AudioEffectColors = LocalAudioEffectColors.current

private var _audioColors: AudioEffectColors? = null

val GoldPrimary: Color get() = _audioColors?.primary ?: Color(0xFF6C5CE7)
val GoldLight: Color get() = _audioColors?.light ?: Color(0xFFA29BFE)
val GoldDim: Color get() = _audioColors?.dim ?: Color(0xFF636E72)
val GoldGlow: Color get() = _audioColors?.glow ?: Color(0xFF8B80F9)
val DeepBlack: Color get() = _audioColors?.bg ?: Color(0xFFF8F6FF)
val SurfaceDark: Color get() = _audioColors?.surface ?: Color(0xFFEDE8FC)
val CardBg: Color get() = _audioColors?.cardBg ?: CardGlassBackgroundLight
val CardBorder: Color get() = _audioColors?.cardBorder ?: CardGlassBorderLight
val PurpleAccent: Color get() = _audioColors?.purpleAccent ?: Color(0xFFA78BFA)
val BlueAccent: Color get() = _audioColors?.blueAccent ?: Color(0xFF60A5FA)
val TextDark: Color get() = _audioColors?.textPrimary ?: Color(0xFF2D3436)
val TextMuted: Color get() = _audioColors?.textSecondary ?: Color(0xFF636E72)

@Composable
private fun audioEffectColors(): AudioEffectColors {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        AudioEffectColors(
            primary = Color(0xFF00E5FF),
            light = Color(0xFF7DF3FF),
            dim = Color(0xFF0A5A6E),
            glow = Color(0xFF00D4F0),
            bg = Color(0xFF0A0F1E),
            surface = Color(0xFF0D1525),
            cardBg = CardGlassBackgroundDark,
            cardBorder = CardGlassBorderDark,
            cardHighlight = CardGlassHighlightDark,
            purpleAccent = Color(0xFF8B5CF6),
            blueAccent = Color(0xFF3B82F6),
            textPrimary = Color.White,
            textSecondary = Color.White.copy(alpha = 0.45f),
            lineColor = Color.White.copy(alpha = 0.06f),
            dotBg = Color.White.copy(alpha = 0.06f),
        )
    } else {
        AudioEffectColors(
            primary = Color(0xFF6C5CE7),
            light = Color(0xFFA29BFE),
            dim = Color(0xFF636E72),
            glow = Color(0xFF8B80F9),
            bg = Color(0xFFF8F6FF),
            surface = Color(0xFFEDE8FC),
            cardBg = CardGlassBackgroundLight,
            cardBorder = CardGlassBorderLight,
            cardHighlight = CardGlassHighlightLight,
            purpleAccent = Color(0xFFA78BFA),
            blueAccent = Color(0xFF60A5FA),
            textPrimary = Color(0xFF2D3436),
            textSecondary = Color(0xFF636E72),
            lineColor = Color(0x15000000),
            dotBg = PurpleAccent.copy(alpha = 0.1f),
        )
    }
}

// ══════════════════════════════════════════
// Canvas 矢量图标
// ══════════════════════════════════════════
@Composable
private fun EffectIconCanvas(
    type: String,
    modifier: Modifier = Modifier,
    tint: Color = TextMuted.copy(0.7f),
) {
    Canvas(modifier) {
        val s = size.minDimension
        val cx = size.width / 2f
        val cy = size.height / 2f
        val sw = s * 0.065f
        val stroke = Stroke(sw, cap = StrokeCap.Round)

        when (type) {
            "surround" -> {
                drawCircle(tint, s * 0.07f, Offset(cx, cy))
                for (i in 1..3) {
                    val r = s * (0.1f + i * 0.08f)
                    val a = tint.copy(0.65f - i * 0.12f)
                    val as2 = Size(r * 2, r * 2)
                    val ao = Offset(cx - r, cy - r)
                    drawArc(a, -55f, 110f, false, ao, as2, style = stroke)
                    drawArc(a, 125f, 110f, false, ao, as2, style = stroke)
                }
            }
            "wave" -> {
                for (j in -1..1) {
                    val y0 = cy + j * s * 0.14f
                    val p = Path().apply {
                        moveTo(cx - s * 0.28f, y0)
                        cubicTo(cx - s * 0.1f, y0 - s * 0.1f, cx + s * 0.1f, y0 + s * 0.1f, cx + s * 0.28f, y0)
                    }
                    drawPath(p, tint.copy(0.6f - abs(j) * 0.12f), style = stroke)
                }
            }
            "mic" -> {
                val mw = s * 0.11f; val mh = s * 0.17f
                drawRoundRect(tint, Offset(cx - mw, cy - mh - s * 0.02f), Size(mw * 2, mh * 2), CornerRadius(mw), style = stroke)
                drawArc(tint, 0f, 180f, false, Offset(cx - s * 0.16f, cy - s * 0.02f), Size(s * 0.32f, s * 0.26f), style = stroke)
                drawLine(tint, Offset(cx, cy + s * 0.11f), Offset(cx, cy + s * 0.24f), sw)
                drawLine(tint, Offset(cx - s * 0.08f, cy + s * 0.24f), Offset(cx + s * 0.08f, cy + s * 0.24f), sw)
            }
            "hall" -> {
                val r = s * 0.22f; val fY = cy + s * 0.2f
                drawArc(tint, 180f, 180f, false, Offset(cx - r, cy - s * 0.2f), Size(r * 2, r * 1.2f), style = stroke)
                drawLine(tint, Offset(cx - r, cy - s * 0.2f + r * 0.6f), Offset(cx - r, fY), sw)
                drawLine(tint, Offset(cx + r, cy - s * 0.2f + r * 0.6f), Offset(cx + r, fY), sw)
                drawLine(tint, Offset(cx, cy - s * 0.2f + r * 0.6f), Offset(cx, fY), sw)
                drawLine(tint, Offset(cx - r - s * 0.04f, fY), Offset(cx + r + s * 0.04f, fY), sw)
            }
            "bass" -> {
                val bx = cx - s * 0.04f
                drawRoundRect(tint, Offset(bx - s * 0.07f, cy - s * 0.08f), Size(s * 0.1f, s * 0.16f), CornerRadius(s * 0.02f), style = stroke)
                val p = Path().apply {
                    moveTo(bx + s * 0.03f, cy - s * 0.08f)
                    lineTo(bx + s * 0.16f, cy - s * 0.18f)
                    lineTo(bx + s * 0.16f, cy + s * 0.18f)
                    lineTo(bx + s * 0.03f, cy + s * 0.08f)
                }
                drawPath(p, tint, style = stroke)
                for (i in 1..2) {
                    val wr = s * (0.06f + i * 0.06f)
                    drawArc(tint.copy(0.5f), -35f, 70f, false, Offset(bx + s * 0.16f - wr, cy - wr), Size(wr * 2, wr * 2), style = stroke)
                }
            }
            "beat" -> {
                val hs = floatArrayOf(0.15f, 0.25f, 0.34f, 0.2f, 0.29f)
                val baseY = cy + s * 0.18f; val gap = s * 0.09f; val bw = s * 0.055f
                hs.forEachIndexed { i, h ->
                    val x = cx + (i - 2) * gap
                    drawRoundRect(tint, Offset(x - bw / 2, baseY - s * h), Size(bw, s * h), CornerRadius(bw / 2))
                }
            }
            "headset" -> {
                val hr = s * 0.2f
                drawArc(tint, 195f, 150f, false, Offset(cx - hr, cy - s * 0.15f), Size(hr * 2, hr * 1.4f), style = stroke)
                drawRoundRect(tint, Offset(cx - hr - s * 0.02f, cy + s * 0.04f), Size(s * 0.09f, s * 0.16f), CornerRadius(s * 0.03f), style = stroke)
                drawRoundRect(tint, Offset(cx + hr - s * 0.07f, cy + s * 0.04f), Size(s * 0.09f, s * 0.16f), CornerRadius(s * 0.03f), style = stroke)
            }
            "bolt" -> {
                val p = Path().apply {
                    moveTo(cx + s * 0.04f, cy - s * 0.28f)
                    lineTo(cx - s * 0.1f, cy - s * 0.02f)
                    lineTo(cx + s * 0.02f, cy - s * 0.02f)
                    lineTo(cx - s * 0.04f, cy + s * 0.28f)
                    lineTo(cx + s * 0.1f, cy + s * 0.02f)
                    lineTo(cx - s * 0.02f, cy + s * 0.02f)
                    close()
                }
                drawPath(p, tint)
            }
            "moon" -> {
                val r = s * 0.19f
                drawArc(tint, 45f, 270f, false, Offset(cx - r, cy - r), Size(r * 2, r * 2), style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                drawCircle(tint.copy(0.35f), s * 0.025f, Offset(cx + s * 0.2f, cy - s * 0.15f))
                drawCircle(tint.copy(0.25f), s * 0.02f, Offset(cx + s * 0.14f, cy - s * 0.23f))
            }
            "speaker" -> {
                drawRoundRect(tint, Offset(cx - s * 0.1f, cy - s * 0.06f), Size(s * 0.08f, s * 0.12f), CornerRadius(s * 0.01f))
                val p = Path().apply {
                    moveTo(cx - s * 0.02f, cy - s * 0.06f)
                    lineTo(cx + s * 0.1f, cy - s * 0.18f)
                    lineTo(cx + s * 0.1f, cy + s * 0.18f)
                    lineTo(cx - s * 0.02f, cy + s * 0.06f)
                    close()
                }
                drawPath(p, tint)
                drawArc(tint.copy(0.4f), -30f, 60f, false, Offset(cx + s * 0.1f, cy - s * 0.12f), Size(s * 0.14f, s * 0.24f), style = stroke)
            }
            "note" -> {
                drawCircle(tint, s * 0.09f, Offset(cx - s * 0.04f, cy + s * 0.1f))
                drawLine(tint, Offset(cx + s * 0.05f, cy + s * 0.1f), Offset(cx + s * 0.05f, cy - s * 0.2f), sw)
                val p = Path().apply {
                    moveTo(cx + s * 0.05f, cy - s * 0.2f)
                    cubicTo(cx + s * 0.15f, cy - s * 0.22f, cx + s * 0.18f, cy - s * 0.12f, cx + s * 0.1f, cy - s * 0.1f)
                }
                drawPath(p, tint, style = Stroke(sw * 0.8f, cap = StrokeCap.Round))
            }
        }
    }
}

// ══════════════════════════════════════════
// 音效数据
// ══════════════════════════════════════════
private data class SoundEffect(
    val name: String,
    val subtitle: String,
    val bannerDesc: String,
    val userCount: String,
    val preset: MusicViewModel.EqPreset,
    val icon: String,
    val isFree: Boolean = false,
)

private val featuredEffects = listOf(
    SoundEffect(
        "臻境声场", "宽广深邂的沉浸式声场",
        "AI空间建模引擎出品。让声浪循着方位流淌，营造宽广深邂的沉浸式声场体验。",
        "191万", MusicViewModel.EqPreset.CLASSICAL, "surround",
    ),
    SoundEffect(
        "浩渺声场", "畅享无拘的音乐会",
        "在浩渺的声场中畅享无拘的音乐会",
        "217万", MusicViewModel.EqPreset.LIVE, "wave",
    ),
    SoundEffect(
        "悦耳人声", "聊听近在耳畔的哼唱",
        "高保真人声，聊听近在耳畔的哼唱",
        "1377万", MusicViewModel.EqPreset.VOCAL, "mic",
    ),
)

private val gridEffects = listOf(
    SoundEffect("臻享环绕", "DTS环绕声", "", "757万", MusicViewModel.EqPreset.ELECTRONIC, "DTS", isFree = true),
    SoundEffect("大模型音效", "AI智能均衡", "", "994万", MusicViewModel.EqPreset.POP, "AI"),
    SoundEffect("3D环绕MAX", "极致空间感", "", "7.7万", MusicViewModel.EqPreset.LIVE, "3D"),
    SoundEffect("大模型临境人声", "AI人声增强", "", "234万", MusicViewModel.EqPreset.VOCAL, "mic", isFree = true),
    SoundEffect("深圳音乐厅", "音乐厅声场", "", "5.8万", MusicViewModel.EqPreset.CLASSICAL, "hall"),
    SoundEffect("HIFI现场音效", "高保真现场", "", "1.7万", MusicViewModel.EqPreset.JAZZ, "HIFI"),
    SoundEffect("澎湃低音", "深度低频", "", "963万", MusicViewModel.EqPreset.BASS, "bass"),
    SoundEffect("动感节拍", "鼓点穿透", "", "326万", MusicViewModel.EqPreset.HIPHOP, "beat"),
    SoundEffect("次元增强", "动漫优化", "", "158万", MusicViewModel.EqPreset.ACG, "headset"),
    SoundEffect("电子脉冲", "弹性低频", "", "89万", MusicViewModel.EqPreset.ROCK, "bolt", isFree = true),
    SoundEffect("夜间守护", "柔和护耳", "", "412万", MusicViewModel.EqPreset.NIGHT, "moon"),
    SoundEffect("超级低音炮", "极致低频", "", "68万", MusicViewModel.EqPreset.BASS, "bass"),
)

// ══════════════════════════════════════════
// 主页面
// ══════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AiAudioEffectScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
) {
    val c = audioEffectColors()
    val pagerState = rememberPagerState(pageCount = { featuredEffects.size })
    var selectedEffectName by remember {
        mutableStateOf(
            LocalStorage.loadEqEffectName().ifEmpty {
                if (viewModel.currentPreset != MusicViewModel.EqPreset.FLAT) {
                    (featuredEffects + gridEffects).firstOrNull { it.preset == viewModel.currentPreset }?.name ?: ""
                } else ""
            }
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        c.bg,
                        c.bg.copy(alpha = 0.95f),
                        c.surface,
                        c.bg.copy(alpha = 0.98f),
                    )
                )
            )
            .statusBarsPadding()
    ) {
        CompositionLocalProvider(LocalAudioEffectColors provides c) {
        _audioColors = c
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // ── 顶部栏 ──
            TopBarSection(onBack)

            // ── 精选音效轮播 Banner ──
            HorizontalPager(
                state = pagerState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                pageSpacing = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(440.dp),
            ) { page ->
                BannerCard(
                    effect = featuredEffects[page],
                    animationType = page,
                isActive = selectedEffectName == featuredEffects[page].name,
                    onUse = { selectedEffectName = featuredEffects[page].name; LocalStorage.saveEqEffectName(featuredEffects[page].name); viewModel.applyPreset(featuredEffects[page].preset) },
                )
            }

            // 分页指示器
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(featuredEffects.size) { i ->
                    val active = pagerState.currentPage == i
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (active) 7.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) GoldPrimary else TextMuted.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // ── AI 调音大师 ──
            Spacer(Modifier.height(14.dp))
            AiMasterCard(isActive = selectedEffectName == "AI调音大师", onClick = { selectedEffectName = "AI调音大师"; LocalStorage.saveEqEffectName("AI调音大师"); viewModel.applyPreset(MusicViewModel.EqPreset.POP) })

            // ── 官方精选音效 ──
            Spacer(Modifier.height(22.dp))
            OfficialHeader(count = gridEffects.size)
            Spacer(Modifier.height(12.dp))
            PresetGrid(viewModel, selectedEffectName) { name -> selectedEffectName = name; LocalStorage.saveEqEffectName(name) }

            Spacer(Modifier.height(32.dp))

            // ── 智能均衡器 ──
            SectionTitle("智能均衡器", "实时频率响应曲线")
            Spacer(Modifier.height(10.dp))
            EqualizerSection(viewModel)

            Spacer(Modifier.height(24.dp))

            // ── 空间音效引擎 ──
            SectionTitle("空间音效引擎", "3D 声场渲染")
            Spacer(Modifier.height(10.dp))
            SpatialSection(viewModel)

            Spacer(Modifier.height(24.dp))

            // ── 混响精调 ──
            ReverbSection(viewModel)

            Spacer(Modifier.height(100.dp)) // 为底部栏留空间
        }

        // ── 固定底部栏 ──
        BottomBarSection(
            viewModel = viewModel,
            effectName = selectedEffectName,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
        }
    }
}

// ══════════════════════════════════════════
// 顶部栏 (音动音乐音效 | AI 音效)
// ══════════════════════════════════════════
@Composable
private fun TopBarSection(onBack: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.Default.ArrowBack, contentDescription = "返回",
                tint = TextDark, modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✦ ", fontSize = 14.sp, color = GoldPrimary)
            Text(
                "音动音乐音效", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = TextDark,
            )
            Text("  |  ", fontSize = 14.sp, color = TextMuted.copy(0.5f))
            Text(
                "AI 音效", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                color = GoldPrimary,
            )
        }
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.size(48.dp)) // 占位保持居中
    }
}

// ══════════════════════════════════════════
// 轮播 Banner 卡片
// ══════════════════════════════════════════
@Composable
private fun BannerCard(
    effect: SoundEffect,
    animationType: Int,
    isActive: Boolean,
    onUse: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(18.dp))
            .background(CardGlassBackgroundLight)
            .border(1.dp, CardGlassBorderLight, RoundedCornerShape(18.dp))
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.3f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleAccent.copy(alpha = 0.1f), Color.Transparent),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                        radius = this.size.width * 0.6f
                    ),
                    center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                    radius = this.size.width * 0.6f
                )
            }
            .padding(20.dp)
    ) {
        // ── 标题区 ──
        Text(
            "耳膜福利 精选音效推荐",
            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            effect.bannerDesc,
            fontSize = 12.sp, color = TextMuted,
            maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 16.sp,
        )

        // ── 粒子动画区 ──
        val inf = rememberInfiniteTransition(label = "banner_$animationType")
        val phase by inf.animateFloat(
            0f, (2 * PI).toFloat(),
            infiniteRepeatable(tween(
                when (animationType) { 0 -> 10000; 1 -> 8000; else -> 12000 },
                easing = LinearEasing,
            ), RepeatMode.Restart),
            label = "phase_$animationType",
        )

        Box(
            Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (animationType) {
                0 -> RoseSpiralCanvas(phase, Modifier.fillMaxSize())
                1 -> WaveAuroraCanvas(phase, Modifier.fillMaxSize())
                else -> MountainWavesCanvas(phase, Modifier.fillMaxSize())
            }
        }

        // ── 底部信息 ──
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(PurpleAccent.copy(alpha = 0.1f))
                    .border(1.dp, PurpleAccent.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { EffectIconCanvas(effect.icon, Modifier.size(24.dp), tint = GoldPrimary) }

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        effect.name, fontSize = 16.sp,
                        fontWeight = FontWeight.Bold, color = TextDark,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(effect.subtitle, fontSize = 12.sp, color = TextMuted)
            }

            Icon(
                Icons.Default.FavoriteBorder, null,
                tint = TextMuted, modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            UseButton(isActive = isActive, onClick = onUse)
        }
    }
}

// ══════════════════════════════════════════
// 使用按钮
// ══════════════════════════════════════════
@Composable
private fun UseButton(isActive: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (isActive) GoldPrimary.copy(alpha = 0.12f) else Color.Transparent,
        tween(300), label = "ubg",
    )
    val border by animateColorAsState(
        if (isActive) GoldPrimary.copy(alpha = 0.6f) else CardGlassBorderLight,
        tween(300), label = "ubd",
    )
    Box(
        Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .drawBehind {
                if (!isActive) {
                    drawRect(
                        CardGlassHighlightLight,
                        topLeft = Offset(0f, 0f),
                        size = Size(this.size.width, this.size.height * 0.4f)
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("◆", fontSize = 9.sp, color = GoldPrimary)
            Spacer(Modifier.width(4.dp))
            Text(
                if (isActive) "使用中" else "使用",
                fontSize = 13.sp, color = GoldPrimary, fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ════════════════════════════════════════════
// 粒子动画 1 — 霉虹玫瑰螺旋
// ════════════════════════════════════════════
@Composable
private fun RoseSpiralCanvas(phase: Float, modifier: Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = minOf(size.width, size.height) * 0.38f

        // 中心辉光
        drawCircle(
            Brush.radialGradient(
                listOf(GoldGlow.copy(0.15f), PurpleAccent.copy(0.04f), Color.Transparent),
                center = Offset(cx, cy), radius = maxR * 0.8f,
            ),
            radius = maxR * 0.8f, center = Offset(cx, cy),
        )

        // 多层玫瑰曲线
        for (layer in 0..2) {
            val k = 5
            val layerPhase = phase * (0.6f + layer * 0.25f)
            val layerR = maxR * (1f - layer * 0.12f)
            val alphaBase = 0.6f - layer * 0.15f
            val count = 180 - layer * 30
            val layerColor = when (layer) { 0 -> GoldLight; 1 -> BlueAccent; else -> PurpleAccent }

            for (i in 0..count) {
                val theta = i.toFloat() / count * 2 * PI + layerPhase
                val r = layerR * abs(cos(k * theta).toFloat())
                val x = cx + r * cos(theta).toFloat()
                val y = cy + r * sin(theta).toFloat()
                val sz = 2f - layer * 0.4f

                drawCircle(GoldGlow.copy(alphaBase * 0.2f), sz + 3f, Offset(x, y))
                drawCircle(layerColor.copy(alphaBase), sz, Offset(x, y))
            }
        }

        // 散布粒子
        for (i in 0..40) {
            val angle = i * 0.618f * 2 * PI + phase * 0.3f
            val dist = maxR * (0.25f + (i % 7) * 0.1f)
            val x = cx + dist * cos(angle).toFloat()
            val y = cy + dist * sin(angle).toFloat()
            val pc = if (i % 3 == 0) PurpleAccent else GoldPrimary
            drawCircle(pc.copy(0.18f), 1.5f, Offset(x, y))
        }
    }
}

// ════════════════════════════════════════════
// 粒子动画 2 — 霉虹极光波
// ════════════════════════════════════════════
@Composable
private fun WaveAuroraCanvas(phase: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cy = h * 0.5f

        for (line in 0..9) {
            val amplitude = h * 0.16f * (1f - line * 0.07f)
            val freq = 2.2f + line * 0.35f
            val linePhase = phase + line * 0.55f
            val alpha = 0.45f - line * 0.035f
            val lineColor = when {
                line % 3 == 0 -> GoldGlow
                line % 3 == 1 -> BlueAccent
                else -> PurpleAccent
            }

            val path = Path()
            val mirror = Path()
            path.moveTo(0f, cy)
            mirror.moveTo(0f, cy)

            for (x in 0..w.toInt() step 3) {
                val xf = x.toFloat()
                val envelope = sin(xf / w * PI).toFloat()
                val dy = sin(xf / w * freq * PI + linePhase).toFloat() * amplitude * envelope
                path.lineTo(xf, cy + dy)
                mirror.lineTo(xf, cy - dy)
            }

            val stroke = Stroke(1.4f, cap = StrokeCap.Round)
            drawPath(path, lineColor.copy(alpha), style = stroke)
            drawPath(mirror, lineColor.copy(alpha * 0.55f), style = stroke)
        }

        drawCircle(
            Brush.radialGradient(
                listOf(PurpleAccent.copy(0.08f), GoldPrimary.copy(0.04f), Color.Transparent),
                center = Offset(w / 2, cy), radius = w * 0.3f,
            ),
            radius = w * 0.3f, center = Offset(w / 2, cy),
        )
    }
}

// ════════════════════════════════════════════
// 粒子动画 3 — 数据流山脉
// ════════════════════════════════════════════
@Composable
private fun MountainWavesCanvas(phase: Float, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val baseY = h * 0.55f

        for (layer in 0..5) {
            val amplitude = h * 0.11f * (1f - layer * 0.08f)
            val layerY = baseY - layer * h * 0.035f
            val freq = 2.5f + layer * 0.4f
            val lp = phase * (0.8f + layer * 0.15f) + layer * 0.8f
            val alpha = 0.45f - layer * 0.06f
            val lc = when {
                layer % 3 == 0 -> GoldGlow
                layer % 3 == 1 -> BlueAccent
                else -> PurpleAccent
            }

            val fill = Path()
            fill.moveTo(0f, h)
            for (x in 0..w.toInt() step 3) {
                val xf = x.toFloat()
                val y = layerY + sin(xf / w * freq * PI + lp).toFloat() * amplitude
                if (x == 0) fill.lineTo(0f, y) else fill.lineTo(xf, y)
            }
            fill.lineTo(w, h)
            fill.close()
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(lc.copy(alpha * 0.35f), lc.copy(alpha * 0.05f), Color.Transparent),
                    startY = layerY - amplitude, endY = h,
                ),
            )

            val line = Path()
            for (x in 0..w.toInt() step 3) {
                val xf = x.toFloat()
                val y = layerY + sin(xf / w * freq * PI + lp).toFloat() * amplitude
                if (x == 0) line.moveTo(xf, y) else line.lineTo(xf, y)
            }
            drawPath(line, lc.copy(alpha * 0.7f), style = Stroke(1.2f, cap = StrokeCap.Round))
        }

        for (i in 0..30) {
            val x = w * (i / 30f)
            val y = baseY + sin(x / w * 3f * PI + phase).toFloat() * h * 0.08f
            val pc = if (i % 2 == 0) GoldLight else PurpleAccent
            drawCircle(pc.copy(0.3f), 1.5f, Offset(x, y))
        }
    }
}

// ════════════════════════════════════════════
// AI 调音大师 master 卡片 (科幻全息风格)
// ════════════════════════════════════════════
@Composable
private fun AiMasterCard(isActive: Boolean, onClick: () -> Unit) {
    val inf = rememberInfiniteTransition(label = "master")
    val glow by inf.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse),
        label = "mglow",
    )
    val hue by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "mhue",
    )

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(CardGlassBackgroundLight)
            .border(
                1.dp,
                CardGlassBorderLight,
                RoundedCornerShape(18.dp),
            )
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.3f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(PurpleAccent.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.35f),
                        radius = this.size.minDimension * 0.4f
                    ),
                    center = Offset(this.size.width * 0.5f, this.size.height * 0.35f),
                    radius = this.size.minDimension * 0.4f
                )
            }
            .clickable(onClick = onClick),
    ) {
        // 全息光球动画
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.35f
            val r = size.minDimension * 0.2f * glow

            // 外层辉光 (紫+蓝渐变)
            drawCircle(
                Brush.radialGradient(
                    listOf(PurpleAccent.copy(0.15f), GoldPrimary.copy(0.06f), Color.Transparent),
                    center = Offset(cx, cy), radius = r * 2f,
                ), r * 2f, Offset(cx, cy),
            )
            // 内层光球
            drawCircle(
                Brush.radialGradient(
                    listOf(GoldPrimary.copy(0.15f), PurpleAccent.copy(0.08f), Color.Transparent),
                    center = Offset(cx, cy), radius = r,
                ), r, Offset(cx, cy),
            )
            // 数据流环纹
            for (i in 0..5) {
                val waveR = r * (0.5f + i * 0.15f)
                val rc = if (i % 2 == 0) PurpleAccent else BlueAccent
                drawCircle(rc.copy(0.08f - i * 0.01f), waveR, Offset(cx, cy), style = Stroke(0.8f))
            }
            // 轨道粒子
            for (i in 0..11) {
                val angle = i * PI / 6 + hue * 2 * PI
                val dist = r * (0.8f + (i % 3) * 0.3f)
                val px = cx + dist * cos(angle).toFloat()
                val py = cy + dist * sin(angle).toFloat()
                val pc = if (i % 2 == 0) PurpleAccent else GoldPrimary
                drawCircle(pc.copy(0.5f), 2f, Offset(px, py))
            }
        }

        // PRO 徽章
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(listOf(PurpleAccent.copy(alpha = 0.25f), BlueAccent.copy(alpha = 0.15f)))
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
        }

        // 文字内容
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "AI调音大师 ", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, color = TextDark,
                )
                Text(
                    "master", fontSize = 22.sp,
                    fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                    color = GoldPrimary,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "AI 大模型分析歌曲内容，为每一首歌定制专属音效 ›",
                fontSize = 13.sp, color = TextMuted,
            )
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isActive) GoldPrimary.copy(0.15f) else Color.Transparent)
                    .border(
                        1.dp,
                        Brush.horizontalGradient(
                            if (isActive) listOf(GoldPrimary.copy(0.9f), GoldPrimary.copy(0.7f))
                            else listOf(GoldPrimary.copy(0.7f), PurpleAccent.copy(0.5f))
                        ),
                        RoundedCornerShape(24.dp),
                    )
                    .clickable(onClick = onClick)
                    .padding(horizontal = 36.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (isActive) "◆ 使用中" else "立即使用", fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = GoldPrimary,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ════════════════════════════════════════════
// 官方精选音效 标题
// ════════════════════════════════════════════
@Composable
private fun OfficialHeader(count: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "官方精选音效", fontSize = 18.sp,
            fontWeight = FontWeight.Bold, color = TextDark,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "$count", fontSize = 14.sp,
            color = TextMuted,
        )
        Spacer(Modifier.weight(1f))
        Text(
            "更多 >", fontSize = 13.sp,
            color = TextMuted,
        )
    }
}

// ════════════════════════════════════════════
// 3列网格 (官方音效)
// ════════════════════════════════════════════
@Composable
private fun PresetGrid(viewModel: MusicViewModel, selectedName: String = "", onSelectEffect: (String) -> Unit = {}) {
    val rows = gridEffects.chunked(3)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        rows.forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEach { effect ->
                    PresetCard(
                        effect = effect,
                        isActive = selectedName == effect.name,
                        onClick = { onSelectEffect(effect.name); viewModel.applyPreset(effect.preset) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun DynamicIconCircle(iconType: String, isActive: Boolean) {
    val inf = rememberInfiniteTransition(label = "dicon")
    val rotation by inf.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "drot",
    )
    val pulse by inf.animateFloat(
        0.85f, 1.15f,
        infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "dpulse",
    )
    val orbitPhase by inf.animateFloat(
        0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(5000, easing = LinearEasing), RepeatMode.Restart),
        label = "dorbit",
    )

    val ringColor1 = if (isActive) GoldPrimary else GoldPrimary.copy(0.3f)
    val ringColor2 = if (isActive) PurpleAccent else PurpleAccent.copy(0.2f)
    val glowAlpha = if (isActive) 0.2f else 0.06f

    Box(Modifier.size(60.dp), contentAlignment = Alignment.Center) {
        // 光晕背景
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f; val cy = size.height / 2f
            drawCircle(
                Brush.radialGradient(
                    listOf(GoldPrimary.copy(glowAlpha * pulse), Color.Transparent),
                    center = Offset(cx, cy), radius = size.minDimension * 0.5f,
                ),
                size.minDimension * 0.5f, Offset(cx, cy),
            )
        }

        // 旋转渐变环
        Canvas(Modifier.size(56.dp)) {
            val cx = size.width / 2f; val cy = size.height / 2f
            val r = size.minDimension / 2f - 2f
            val sweepGrad = Brush.sweepGradient(
                0f to ringColor1.copy(0.6f),
                0.25f to ringColor2.copy(0.4f),
                0.5f to Color.Transparent,
                0.75f to ringColor2.copy(0.3f),
                1f to ringColor1.copy(0.6f),
            )
            drawCircle(sweepGrad, r, Offset(cx, cy), style = Stroke(1.8f))

            // 轨道粒子
            for (i in 0..2) {
                val angle = orbitPhase + i * 2 * PI / 3
                val px = cx + r * cos(angle).toFloat()
                val py = cy + r * sin(angle).toFloat()
                val pc = when (i) { 0 -> GoldPrimary; 1 -> PurpleAccent; else -> BlueAccent }
                drawCircle(pc.copy(if (isActive) 0.7f else 0.3f), 2.5f, Offset(px, py))
            }
        }

        // 内圆背景
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(PurpleAccent.copy(alpha = 0.08f), PurpleAccent.copy(alpha = 0.03f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            val ic = iconType
            if (ic == ic.uppercase() && ic.length <= 4 && ic.all { it.isLetterOrDigit() }) {
                Text(
                    ic, fontSize = if (ic.length > 3) 12.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) GoldPrimary.copy(alpha = 0.9f) else TextMuted,
                    letterSpacing = 0.5.sp,
                )
            } else {
                EffectIconCanvas(
                    ic, Modifier.size(28.dp),
                    tint = if (isActive) GoldPrimary.copy(alpha = 0.8f) else TextMuted,
                )
            }
        }
    }
}

@Composable
private fun PresetCard(
    effect: SoundEffect,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(
        if (isActive) GoldPrimary.copy(alpha = 0.5f) else CardGlassBorderLight,
        tween(300), label = "pcb",
    )
    val bgColor by animateColorAsState(
        if (isActive) GoldPrimary.copy(alpha = 0.06f) else CardGlassBackgroundLight,
        tween(300), label = "pcbg",
    )

    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.3f)
                )
                if (isActive) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(GoldPrimary.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                            radius = this.size.width * 0.4f
                        ),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                        radius = this.size.width * 0.4f
                    )
                }
            }
            .clickable(onClick = onClick)
            .padding(bottom = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 顶部: 限免 badge + 收藏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (effect.isFree) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(PurpleAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text("限免", fontSize = 9.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Icon(
                Icons.Default.FavoriteBorder, null,
                tint = TextMuted, modifier = Modifier.size(16.dp),
            )
        }

        // 动态图标
        Spacer(Modifier.height(6.dp))
        DynamicIconCircle(effect.icon, isActive)

        // 名称
        Spacer(Modifier.height(10.dp))
        Text(
            effect.name, fontSize = 14.sp, fontWeight = FontWeight.Bold,
            color = if (isActive) GoldPrimary else TextDark,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )

        // 使用按钮
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(if (isActive) GoldPrimary.copy(alpha = 0.1f) else Color.Transparent)
                .border(
                    1.dp,
                    if (isActive) GoldPrimary.copy(alpha = 0.6f) else CardGlassBorderLight,
                    RoundedCornerShape(16.dp),
                )
                .drawBehind {
                    if (!isActive) {
                        drawRect(
                            CardGlassHighlightLight,
                            topLeft = Offset(0f, 0f),
                            size = Size(this.size.width, this.size.height * 0.4f)
                        )
                    }
                }
                .padding(horizontal = 16.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("◇", fontSize = 10.sp, color = GoldPrimary.copy(if (isActive) 1f else 0.6f))
                Spacer(Modifier.width(4.dp))
                Text(
                    if (isActive) "使用中" else "使用",
                    fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = GoldPrimary.copy(if (isActive) 1f else 0.7f),
                )
            }
        }
    }
}

// ══════════════════════════════════════════
// 分区标题
// ══════════════════════════════════════════
@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(GoldPrimary, GoldDim)))
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title, style = MaterialTheme.typography.titleMedium,
                color = TextDark, fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle, style = MaterialTheme.typography.bodySmall,
                color = TextMuted, letterSpacing = 1.sp,
            )
        }
    }
}

// ══════════════════════════════════════════
// 频谱均衡器
// ══════════════════════════════════════════
@Composable
private fun EqualizerSection(viewModel: MusicViewModel) {
    val bands = viewModel.equalizerBands

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardGlassBackgroundLight)
            .border(1.dp, CardGlassBorderLight, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.25f)
                )
            }
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { viewModel.applyPreset(MusicViewModel.EqPreset.FLAT) }) {
                Text("重置", color = GoldPrimary.copy(0.7f), fontSize = 12.sp)
            }
        }

        var draggingBand by remember { mutableIntStateOf(-1) }

        Canvas(
            Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 12.dp)
                .pointerInput(bands.size) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val bandWidth = size.width.toFloat() / bands.size
                            draggingBand = (offset.x / bandWidth).toInt().coerceIn(0, bands.lastIndex)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (draggingBand >= 0) {
                                val trackTop = 24f
                                val trackBottom = size.height - 24f
                                val trackRange = trackBottom - trackTop
                                val y = change.position.y.coerceIn(trackTop, trackBottom)
                                val level = (15f - (y - trackTop) / trackRange * 30f)
                                    .toInt().coerceIn(-15, 15)
                                viewModel.setEqualizerBand(draggingBand, level)
                            }
                        },
                        onDragEnd = { draggingBand = -1 },
                        onDragCancel = { draggingBand = -1 },
                    )
                }
        ) {
            val bandCount = bands.size
            if (bandCount == 0) return@Canvas
            val bandWidth = size.width / bandCount
            val trackTop = 24f
            val trackBottom = size.height - 24f
            val trackRange = trackBottom - trackTop
            val centerY = trackTop + trackRange / 2

            drawLine(TextMuted.copy(alpha = 0.3f), Offset(0f, centerY), Offset(size.width, centerY), 1f)
            for (db in listOf(-10, -5, 5, 10)) {
                val y = trackTop + trackRange * (1f - (db + 15f) / 30f)
                drawLine(TextMuted.copy(alpha = 0.15f), Offset(0f, y), Offset(size.width, y), 0.5f)
            }

            val points = bands.mapIndexed { i, (_, level) ->
                Offset(bandWidth * i + bandWidth / 2, trackTop + trackRange * (1f - (level + 15f) / 30f))
            }

            if (points.size >= 2) {
                val curvePath = Path()
                curvePath.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val cpx = (points[i - 1].x + points[i].x) / 2
                    curvePath.cubicTo(cpx, points[i - 1].y, cpx, points[i].y, points[i].x, points[i].y)
                }
                drawPath(curvePath, GoldGlow.copy(0.04f), style = Stroke(20f, cap = StrokeCap.Round))
                drawPath(curvePath, GoldGlow.copy(0.08f), style = Stroke(10f, cap = StrokeCap.Round))
                drawPath(curvePath, GoldPrimary.copy(0.3f), style = Stroke(4f, cap = StrokeCap.Round))
                drawPath(curvePath, GoldLight.copy(0.8f), style = Stroke(2f, cap = StrokeCap.Round))

                val fillPath = Path()
                fillPath.addPath(curvePath)
                fillPath.lineTo(points.last().x, trackBottom)
                fillPath.lineTo(points.first().x, trackBottom)
                fillPath.close()
                drawPath(
                    fillPath,
                    Brush.verticalGradient(
                        listOf(GoldPrimary.copy(0.12f), Color.Transparent),
                        startY = centerY - trackRange * 0.3f, endY = trackBottom,
                    ),
                )
            }

            points.forEachIndexed { i, point ->
                val level = bands[i].second
                val isDragging = draggingBand == i

                drawLine(
                    TextMuted.copy(alpha = if (isDragging) 0.4f else 0.15f),
                    Offset(point.x, trackTop), Offset(point.x, trackBottom),
                    1.5f, cap = StrokeCap.Round,
                )
                if (level != 0) {
                    drawLine(GoldPrimary.copy(0.5f), Offset(point.x, centerY), Offset(point.x, point.y), 3f, cap = StrokeCap.Round)
                }
                drawCircle(GoldGlow.copy(if (isDragging) 0.5f else 0.2f), if (isDragging) 16f else 10f, point)
                drawCircle(if (isDragging) TextDark else GoldPrimary, if (isDragging) 7f else 5f, point)
            }
        }

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
            bands.forEach { (freq, _) ->
                Text(
                    viewModel.formatFrequency(freq), modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, fontSize = 8.sp, color = TextMuted,
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
            bands.forEach { (_, db) ->
                Text(
                    if (db > 0) "+$db" else "$db", modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center, fontSize = 9.sp,
                    color = if (db != 0) GoldPrimary.copy(0.8f) else TextMuted.copy(alpha = 0.4f),
                    fontWeight = if (db != 0) FontWeight.Bold else FontWeight.Normal,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ══════════════════════════════════════════
// 空间音效引擎 (2×2 圆弧控制)
// ══════════════════════════════════════════
@Composable
private fun SpatialSection(viewModel: MusicViewModel) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArcControl("低频增强", viewModel.bassBoost, { viewModel.updateBassBoost(it) }, GoldPrimary, "bass", Modifier.weight(1f))
            ArcControl("3D 环绕", viewModel.virtualizer, { viewModel.updateVirtualizer(it) }, GoldLight, "surround", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ArcControl("空间混响", viewModel.reverbWet, { viewModel.updateReverbWet(it) }, GoldGlow, "hall", Modifier.weight(1f))
            ArcControl("响度引擎", viewModel.loudnessGain, { viewModel.updateLoudnessGain(it) }, GoldDim, "speaker", Modifier.weight(1f))
        }
    }
}

@Composable
private fun ArcControl(
    label: String, value: Int, onValueChange: (Int) -> Unit,
    color: Color, icon: String, modifier: Modifier = Modifier, maxValue: Int = 100,
) {
    var showDialog by remember { mutableStateOf(false) }
    val animatedSweep by animateFloatAsState(value.toFloat() / maxValue * 270f, tween(300), label = "arc")

    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardGlassBackgroundLight)
            .border(1.dp, if (value > 0) color.copy(alpha = 0.2f) else CardGlassBorderLight, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.3f)
                )
                if (value > 0) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.06f), Color.Transparent),
                            center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                            radius = this.size.width * 0.35f
                        ),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                        radius = this.size.width * 0.35f
                    )
                }
            }
            .clickable { showDialog = true }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val sw = 8f; val pad = sw / 2 + 8f
                val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                val arcOff = Offset(pad, pad)

                drawArc(TextMuted.copy(alpha = 0.15f), 135f, 270f, false, arcOff, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(color.copy(alpha = 0.1f), 135f, animatedSweep, false, arcOff, arcSize, style = Stroke(sw + 14f, cap = StrokeCap.Round))
                drawArc(color, 135f, animatedSweep, false, arcOff, arcSize, style = Stroke(sw, cap = StrokeCap.Round))

                if (value > 0) {
                    val ea = Math.toRadians((135.0 + animatedSweep))
                    val r = arcSize.width / 2; val cx = arcOff.x + arcSize.width / 2; val cy = arcOff.y + arcSize.height / 2
                    drawCircle(color.copy(alpha = 0.4f), 10f, Offset(cx + r * cos(ea).toFloat(), cy + r * sin(ea).toFloat()))
                    drawCircle(TextDark, 3f, Offset(cx + r * cos(ea).toFloat(), cy + r * sin(ea).toFloat()))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                EffectIconCanvas(icon, Modifier.size(22.dp), tint = color.copy(alpha = 0.7f))
                Text("$value", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (value > 0) color else TextMuted)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
    }

    if (showDialog) {
        ArcEditDialog(
            label = label, value = value, maxValue = maxValue,
            color = color, icon = icon,
            onValueChange = onValueChange, onDismiss = { showDialog = false },
        )
    }
}

@Composable
private fun ArcEditDialog(
    label: String, value: Int, maxValue: Int,
    color: Color, icon: String,
    onValueChange: (Int) -> Unit, onDismiss: () -> Unit,
) {
    val animatedSweep by animateFloatAsState(value.toFloat() / maxValue * 270f, tween(300), label = "darc")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 40.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(CardGlassBackgroundLight)
                .border(
                    1.dp,
                    CardGlassBorderLight,
                    RoundedCornerShape(24.dp),
                )
                .drawBehind {
                    drawRect(
                        CardGlassHighlightLight,
                        topLeft = Offset(0f, 0f),
                        size = Size(this.size.width, this.size.height * 0.25f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                            radius = this.size.width * 0.4f
                        ),
                        center = Offset(this.size.width * 0.5f, this.size.height * 0.4f),
                        radius = this.size.width * 0.4f
                    )
                }
                .clickable(enabled = false, onClick = {})
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 标题
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(20.dp))

            // 弧形显示
            Box(Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val sw = 10f; val pad = sw / 2 + 12f
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val arcOff = Offset(pad, pad)
                    drawArc(TextMuted.copy(alpha = 0.15f), 135f, 270f, false, arcOff, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                    drawArc(color.copy(alpha = 0.12f), 135f, animatedSweep, false, arcOff, arcSize, style = Stroke(sw + 18f, cap = StrokeCap.Round))
                    drawArc(color, 135f, animatedSweep, false, arcOff, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                    if (value > 0) {
                        val ea = Math.toRadians((135.0 + animatedSweep))
                        val r = arcSize.width / 2; val cx = arcOff.x + arcSize.width / 2; val cy = arcOff.y + arcSize.height / 2
                        drawCircle(color.copy(alpha = 0.5f), 12f, Offset(cx + r * cos(ea).toFloat(), cy + r * sin(ea).toFloat()))
                        drawCircle(TextDark, 4f, Offset(cx + r * cos(ea).toFloat(), cy + r * sin(ea).toFloat()))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EffectIconCanvas(icon, Modifier.size(28.dp), tint = color.copy(0.7f))
                    Text("$value", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = if (value > 0) color else TextMuted)
                }
            }

            Spacer(Modifier.height(24.dp))

            // 滑块
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = 0f..maxValue.toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = color,
                    activeTrackColor = color,
                    inactiveTrackColor = TextMuted.copy(alpha = 0.15f),
                ),
            )

            Spacer(Modifier.height(16.dp))

            // +/− 按钮
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ArcStepButton("-10", color) { onValueChange((value - 10).coerceAtLeast(0)) }
                ArcStepButton("-1", color) { onValueChange((value - 1).coerceAtLeast(0)) }
                // 重置
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(TextMuted.copy(alpha = 0.1f))
                        .clickable { onValueChange(0) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("重置", fontSize = 13.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                }
                ArcStepButton("+1", color) { onValueChange((value + 1).coerceAtMost(maxValue)) }
                ArcStepButton("+10", color) { onValueChange((value + 10).coerceAtMost(maxValue)) }
            }

            Spacer(Modifier.height(20.dp))

            // 关闭按钮
            Box(
                Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, color.copy(0.5f), RoundedCornerShape(16.dp))
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("完成", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

@Composable
private fun ArcStepButton(text: String, color: Color, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(0.1f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ══════════════════════════════════════════
// 混响精调 (可折叠)
// ══════════════════════════════════════════
@Composable
private fun ReverbSection(viewModel: MusicViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardGlassBackgroundLight)
            .border(1.dp, CardGlassBorderLight, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.25f)
                )
            }
            .animateContentSize(tween(300))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("混响精调", style = MaterialTheme.typography.titleSmall, color = TextDark, fontWeight = FontWeight.Bold)
                Text("EnvironmentalReverb 硬件引擎", style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = GoldPrimary.copy(0.7f))
        }

        if (expanded) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                GoldSlider("房间大小", viewModel.reverbRoomSize, { viewModel.updateReverbRoomSize(it) }, GoldGlow)
                GoldSlider("高频阻尼", viewModel.reverbDamping, { viewModel.updateReverbDamping(it) }, GoldPrimary)
                GoldSlider("湿信号电平", viewModel.reverbWet, { viewModel.updateReverbWet(it) }, GoldLight)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun GoldSlider(label: String, value: Int, onValueChange: (Int) -> Unit, color: Color, maxValue: Int = 100) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextDark, modifier = Modifier.width(72.dp))
        Slider(
            value = value.toFloat(), onValueChange = { onValueChange(it.toInt()) },
            valueRange = 0f..maxValue.toFloat(), modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(thumbColor = color, activeTrackColor = color, inactiveTrackColor = TextMuted.copy(alpha = 0.15f)),
        )
        Text(
            "$value", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(32.dp),
            textAlign = TextAlign.End, fontWeight = if (value > 0) FontWeight.Bold else FontWeight.Normal,
            color = if (value > 0) color else TextMuted,
        )
    }
}

// ══════════════════════════════════════════
// 底部固定栏
// ══════════════════════════════════════════
@Composable
private fun BottomBarSection(viewModel: MusicViewModel, effectName: String, modifier: Modifier) {
    val preset = viewModel.currentPreset
    val isActive = preset != MusicViewModel.EqPreset.FLAT
    val displayName = if (isActive && effectName.isNotEmpty()) effectName else if (isActive) preset.displayName else "未选择"

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(CardGlassBackgroundLight)
            .border(
                1.dp,
                CardGlassBorderLight,
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
            )
            .drawBehind {
                drawRect(
                    CardGlassHighlightLight,
                    topLeft = Offset(0f, 0f),
                    size = Size(this.size.width, this.size.height * 0.3f)
                )
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isActive) GoldPrimary.copy(alpha = 0.1f) else TextMuted.copy(alpha = 0.08f))
                .drawBehind {
                    if (isActive) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(GoldPrimary.copy(alpha = 0.1f), Color.Transparent),
                                center = Offset(this.size.width * 0.5f, this.size.height * 0.5f),
                                radius = this.size.width * 0.5f
                            ),
                            center = Offset(this.size.width * 0.5f, this.size.height * 0.5f),
                            radius = this.size.width * 0.5f
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) { EffectIconCanvas("note", Modifier.size(24.dp), tint = if (isActive) GoldPrimary else TextMuted) }

        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "开启音效：$displayName", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                color = if (isActive) TextDark else TextMuted,
            )
            Text(
                "适配 耳机/音箱/车载 定制音效 ›",
                fontSize = 11.sp, color = TextMuted,
            )
        }
        Switch(
            checked = viewModel.equalizerEnabled,
            onCheckedChange = { viewModel.toggleEqualizer(it) },
            colors = SwitchDefaults.colors(
                checkedThumbColor = GoldPrimary,
                checkedTrackColor = PurpleAccent.copy(alpha = 0.4f),
                uncheckedThumbColor = TextMuted,
                uncheckedTrackColor = TextMuted.copy(alpha = 0.2f),
            ),
        )
    }
}
