package com.yindong.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yindong.music.data.api.RecommendPlaylist
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import androidx.compose.foundation.isSystemInDarkTheme
import com.yindong.music.viewmodel.MusicViewModel

// 平台颜色映射
private val platformColorMap = mapOf(
    "QQ音乐" to Color(0xFF12B7F5),
    "网易云" to Color(0xFFE53935),
    "网易云音乐" to Color(0xFFE53935),
    "酷我音乐" to Color(0xFFFF9800),
    "酷狗音乐" to Color(0xFF2196F3),
    "咪咕音乐" to Color(0xFF9C27B0),
    "抖音" to Color(0xFF111111),
)
private val defaultPlatformColor = Color(0xFF607D8B)

// 平台简称
private fun platformShort(name: String): String = when (name) {
    "QQ音乐" -> "QQ"
    "网易云", "网易云音乐" -> "网易"
    "酷我音乐" -> "酷我"
    "酷狗音乐" -> "酷狗"
    "咪咕音乐" -> "咪咕"
    else -> name
}

@Composable
fun PlaylistSquareScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onPlaylistClick: (String, String) -> Unit,
) {
    LaunchedEffect(Unit) {
        if (viewModel.recommendPlaylists.isEmpty()) {
            viewModel.loadRecommendPlaylists()
        }
    }

    val cs = MaterialTheme.colorScheme
    var selectedTab by remember { mutableStateOf("全部") }
    val allPlaylists = viewModel.recommendPlaylists
    val platforms = remember(allPlaylists) {
        listOf("全部") + allPlaylists.map { it.platform }.distinct()
    }
    val filteredPlaylists = remember(allPlaylists, selectedTab) {
        if (selectedTab == "全部") allPlaylists else allPlaylists.filter { it.platform == selectedTab }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .statusBarsPadding(),
    ) {
        // ── 顶部标题栏 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Red500.copy(alpha = 0.06f),
                            1.0f to Color.Transparent,
                        )
                    )
                )
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, "返回", tint = cs.onBackground)
            }
            Icon(
                Icons.Default.QueueMusic, null,
                tint = Red500,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    "歌单广场",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onBackground,
                )
                Text(
                    "${allPlaylists.size} 个精选歌单 · ${platforms.size - 1} 个平台",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        // ── 平台筛选 Tab ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            platforms.forEach { tab ->
                val isSelected = selectedTab == tab
                val tabColor = if (tab == "全部") Red500 else platformColorMap[tab] ?: defaultPlatformColor
                val isDark = isSystemInDarkTheme()
                val animBg by animateColorAsState(
                    targetValue = when {
                        isSelected -> tabColor.copy(alpha = 0.75f)
                        isDark -> CardGlassBackgroundDark
                        else -> CardGlassBackgroundLight
                    },
                    animationSpec = tween(200), label = "tabBg",
                )
                val animFg by animateColorAsState(
                    targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    animationSpec = tween(200), label = "tabFg",
                )
                val animBorder by animateColorAsState(
                    targetValue = when {
                        isSelected -> tabColor.copy(alpha = 0.4f)
                        isDark -> CardGlassBorderDark
                        else -> CardGlassBorderLight
                    },
                    animationSpec = tween(200), label = "tabBorder",
                )
                val count = if (tab == "全部") allPlaylists.size
                    else allPlaylists.count { it.platform == tab }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(animBg)
                        .border(0.5.dp, animBorder, RoundedCornerShape(20.dp))
                        .clickable { selectedTab = tab }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 平台色小圆点
                    if (tab != "全部") {
                        Box(
                            Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White.copy(alpha = 0.8f) else tabColor),
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        tab,
                        color = animFg,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    // 数量角标
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "$count",
                        color = animFg.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
            }
        }

        // ── 内容区域 ──
        if (allPlaylists.isEmpty() && viewModel.isRecommendLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Red500, strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("加载精选歌单中...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (filteredPlaylists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Headphones, null,
                        tint = cs.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("暂无歌单数据", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 160.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(filteredPlaylists, key = { it.platform + it.playlistId }) { playlist ->
                    PlaylistGridItem(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist.platform, playlist.playlistId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridItem(
    playlist: RecommendPlaylist,
    onClick: () -> Unit,
) {
    val pColor = platformColorMap[playlist.platform] ?: defaultPlatformColor
    val gradientIndex = (playlist.playlistId.hashCode() and 0x7FFFFFFF) % CoverGradients.size
    val fallbackGradient = CoverGradients[gradientIndex]

    Column(
        modifier = Modifier.clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .shadow(4.dp, RoundedCornerShape(14.dp))
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (playlist.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                // 无封面时显示渐变背景 + 装饰元素
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = fallbackGradient,
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(300f, 300f),
                        )
                    ),
                ) {
                    // 装饰光圈
                    Box(
                        Modifier
                            .size(50.dp)
                            .offset(x = (-10).dp, y = (-10).dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                    )
                    Box(
                        Modifier
                            .size(30.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f)),
                    )
                    Icon(
                        Icons.Default.LibraryMusic, null,
                        tint = Color.White.copy(alpha = 0.35f),
                        modifier = Modifier.size(40.dp).align(Alignment.Center),
                    )
                }
            }

            // 底部渐变遮罩 (让封面上的信息更清晰)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                        )
                    ),
            )

            // 平台彩色标签 (左上)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(pColor.copy(alpha = 0.9f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    platformShort(playlist.platform),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                )
            }

            // 播放量 (右上)
            if (playlist.playCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.45f))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(9.dp))
                    Spacer(Modifier.width(2.dp))
                    Text(formatCount(playlist.playCount), color = Color.White, fontSize = 9.sp)
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        // 歌单名称
        Text(
            playlist.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 100_000_000 -> String.format("%.1f亿", count / 100_000_000.0)
        count >= 10_000 -> String.format("%.1f万", count / 10_000.0)
        count >= 1000 -> String.format("%.1fk", count / 1000.0)
        else -> "$count"
    }
}
