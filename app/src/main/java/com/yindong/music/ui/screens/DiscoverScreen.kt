@file:OptIn(ExperimentalLayoutApi::class)

package com.yindong.music.ui.screens

import com.yindong.music.data.api.RecommendPlaylist
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yindong.music.data.model.Song
import com.yindong.music.ui.components.BannerPager
import com.yindong.music.ui.components.HeadsetBanner
import com.yindong.music.ui.theme.NebulaBlue
import com.yindong.music.ui.theme.NebulaPink
import com.yindong.music.ui.theme.NebulaViolet
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.TagBlue
import com.yindong.music.ui.theme.TagGreen
import com.yindong.music.ui.theme.TagOrange
import com.yindong.music.ui.theme.TagPurple
import com.yindong.music.ui.theme.TextHint
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.TextTertiary
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.CardGlassHighlightDark
import com.yindong.music.ui.theme.CardGlassHighlightLight
import com.yindong.music.viewmodel.MusicViewModel
import java.util.Calendar

@Composable
fun DiscoverScreen(
    viewModel: MusicViewModel,
    onSearchClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onPlaylistClick: (Long) -> Unit,
    onHotChartClick: () -> Unit = {},
    onPlaylistSquareClick: () -> Unit = {},
    onImportPlaylistClick: () -> Unit = {},
    onExternalPlaylistClick: (platform: String, playlistId: String) -> Unit = { _, _ -> },
    onAiAudioEffectClick: () -> Unit = {},
) {
    fun doQuickSearch(keyword: String) {
        viewModel.quickSearch(keyword)
        onSearchClick()
    }

    // 进入发现页时加载推荐歌单和热歌榜
    LaunchedEffect(Unit) {
        if (viewModel.recommendPlaylists.isEmpty()) {
            viewModel.loadRecommendPlaylists()
        }
        if (viewModel.hotChartSongs.isEmpty()) {
            viewModel.loadHotChart()
        }
    }

    // 监听歌单导入成功 → 自动跳转歌单详情
    val importState = viewModel.importState
    LaunchedEffect(importState) {
        if (importState is MusicViewModel.ImportState.Success) {
            onPlaylistClick(importState.playlistId)
            viewModel.resetImportState()
        }
    }

    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 6..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        in 18..22 -> "晚上好"
        else -> "夜深了"
    }
    val greetingEmoji = when (hour) {
        in 6..17 -> "\u2600\uFE0F"   // ☀️
        else     -> "\uD83C\uDF19"   // 🌙
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── 顶部: 问候 + 搜索栏 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to NebulaViolet.copy(alpha = 0.08f),
                            0.5f to NebulaViolet.copy(alpha = 0.03f),
                            1.0f to Color.Transparent,
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                "$greeting $greetingEmoji",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            val hotQuotes = remember {
                listOf(
                    "听歌的人最怕听懂了歌词",
                    "耳机是别人的世界 音乐是自己的故事",
                    "总有一首歌 让你想起一个人",
                    "最怕空气突然安静 最怕回忆突然翻涌",
                    "我们听的不是歌 是自己的故事",
                    "有些歌不忍听 因为歌词写的就是自己",
                    "音乐是灵魂的避难所",
                    "把耳机分你一半 听我喜欢的人",
                    "愿你出走半生 归来仍是少年",
                    "世界那么大 能遇见 不容易",
                    "你听过最孤独的一句话是什么",
                    "后来的我们什么都有了 却没有了我们",
                )
            }
            val quoteIndex = remember { (System.currentTimeMillis() % hotQuotes.size).toInt() }
            val randomQuote = hotQuotes[quoteIndex]
            Text(randomQuote, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))

            // 搜索栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onSearchClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Search, null, tint = TextHint, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("搜索你喜欢的音乐", color = TextHint, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(NebulaViolet),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 耳机连接提示横幅
        HeadsetBanner(
            isVisible = viewModel.showHeadsetBanner,
            deviceName = viewModel.connectedHeadsetName,
            onDismiss = { viewModel.dismissHeadsetBanner() },
            onEnterAudioEffect = onAiAudioEffectClick
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // ── Banner ──
            item {
                Spacer(Modifier.height(12.dp))
                BannerPager(
                    banners = viewModel.banners,
                    onBannerClick = { banner ->
                        if (banner.platform.isNotEmpty() && banner.playlistId.isNotEmpty()) {
                            onExternalPlaylistClick(banner.platform, banner.playlistId)
                        }
                    },
                    onPlayAllClick = { banner ->
                        if (banner.platform.isNotEmpty() && banner.playlistId.isNotEmpty()) {
                            viewModel.viewExternalPlaylist(banner.platform, banner.playlistId)
                        }
                    },
                )
            }

            // ── 快捷入口 ──
            item {
                Spacer(Modifier.height(20.dp))
                val dayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString() }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    QuickEntry(Icons.Default.CalendarMonth, "每日推荐", listOf(NebulaViolet, NebulaBlue), dayNumber = dayOfMonth, onClick = { onHotChartClick() })
                    QuickEntry(Icons.Default.BarChart, "排行榜", listOf(TagGreen, Color(0xFF2E7D32)), onClick = { doQuickSearch("热歌榜") })
                    QuickEntry(Icons.Default.QueueMusic, "歌单广场", listOf(TagBlue, Color(0xFF1565C0)), onClick = { doQuickSearch("华语流行") })
                    QuickEntry(Icons.Default.Download, "导入歌单", listOf(Color(0xFF00B4DB), Color(0xFF0083B0)), onClick = { onImportPlaylistClick() })
                }
            }

            // ── 精选歌单推荐 ──
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Star, null, tint = TagOrange, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("精选歌单", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPlaylistSquareClick() }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("更多", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                // 导入歌单中的loading提示
                if (viewModel.importState is MusicViewModel.ImportState.Loading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("正在加载歌单...", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
                val playlists = viewModel.recommendPlaylists
                if (playlists.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        playlists.forEach { playlist ->
                            item {
                                RecommendPlaylistCard(
                                    playlist = playlist,
                                    onClick = {
                                        viewModel.viewExternalPlaylist(playlist.platform, playlist.playlistId)
                                    },
                                )
                            }
                        }
                    }
                } else {
                    // loading 或无数据时显示 fallback 卡片
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val fallback = listOf(
                            "QQ音乐热歌" to CoverGradients[0],
                            "网易云热歌" to CoverGradients[1],
                            "酷我热歌" to CoverGradients[2],
                            "酷狗热歌" to CoverGradients[3],
                        )
                        fallback.forEach { (title, gradient) ->
                            item {
                                PlaylistCoverCard(
                                    title = title,
                                    playCount = "",
                                    gradient = gradient,
                                    onClick = { doQuickSearch(title) },
                                )
                            }
                        }
                    }
                }
            }
            // ── 新歌速递 ──
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = NebulaViolet, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("新歌速递", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { doQuickSearch("最新歌曲") }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("更多", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                        Icon(Icons.Default.ChevronRight, null, tint = TextTertiary, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                val hotSongs = viewModel.hotChartSongs.take(10)
                if (hotSongs.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        hotSongs.forEach { song ->
                            item {
                                HotSongCoverCard(
                                    song = song,
                                    onClick = {
                                        viewModel.onlineResults = viewModel.hotChartSongs
                                        viewModel.playSong(song)
                                    },
                                )
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        val fallbackSongs = listOf(
                            Pair("热歌推荐", "加载中..."),
                            Pair("新歌速递", "加载中..."),
                            Pair("流行热歌", "加载中..."),
                        )
                        fallbackSongs.forEach { (title, subtitle) ->
                            item {
                                NewSongChip(title = title, subtitle = subtitle, onClick = { doQuickSearch(title) })
                            }
                        }
                    }
                }
            }

            // ── 热搜排行榜 ──
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.TrendingUp, null, tint = NebulaPink, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("热搜排行", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Text("点击即搜", style = MaterialTheme.typography.labelSmall, color = TextTertiary)
                }
                Spacer(Modifier.height(12.dp))
            }

            // 左右两列排行
            val searches = viewModel.hotSearches
            val leftCol = searches.filterIndexed { i, _ -> i % 2 == 0 }
            val rightCol = searches.filterIndexed { i, _ -> i % 2 == 1 }

            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column(Modifier.weight(1f)) {
                        leftCol.forEachIndexed { idx, keyword ->
                            val rank = idx * 2 + 1
                            HotSearchItem(rank, keyword, onClick = { doQuickSearch(keyword) })
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        rightCol.forEachIndexed { idx, keyword ->
                            val rank = idx * 2 + 2
                            HotSearchItem(rank, keyword, onClick = { doQuickSearch(keyword) })
                        }
                    }
                }
            }

            // ── 场景推荐 ──
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Headphones, null, tint = TagPurple, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("场景推荐", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
            }

            item {
                val scenePlaylists = viewModel.recommendPlaylists.drop(8).take(3)
                if (scenePlaylists.size >= 3) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        scenePlaylists.forEach { playlist ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onExternalPlaylistClick(playlist.platform, playlist.playlistId) }
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1.2f)
                                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                                ) {
                                    if (playlist.coverUrl.isNotEmpty()) {
                                        AsyncImage(
                                            model = playlist.coverUrl,
                                            contentDescription = playlist.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    }
                                }
                                Text(
                                    playlist.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SceneCard(icon = Icons.Default.Nightlight, title = "睡前", subtitle = "轻柔入梦", color = TagPurple, modifier = Modifier.weight(1f), onClick = { doQuickSearch("睡前轻音乐") })
                        SceneCard(icon = Icons.Default.SportsEsports, title = "游戏", subtitle = "燃战BGM", color = TagGreen, modifier = Modifier.weight(1f), onClick = { doQuickSearch("游戏BGM") })
                        SceneCard(icon = Icons.Default.Favorite, title = "恋爱", subtitle = "甜蜜时光", color = NebulaPink, modifier = Modifier.weight(1f), onClick = { doQuickSearch("甜蜜恋爱") })
                    }
                }
            }

            // ── 猜你喜欢 ──
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "猜你喜欢",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(10.dp))
                // 从搜索历史 + 热歌榜歌手中生成推荐词
                val guessKeywords = remember(viewModel.searchHistory, viewModel.hotChartSongs) {
                    val fromHistory = viewModel.searchHistory.take(5)
                    val fromHotArtists = viewModel.hotChartSongs
                        .map { it.artist }
                        .distinct()
                        .filterNot { a -> fromHistory.any { it.contains(a) } }
                        .take(10)
                    (fromHistory + fromHotArtists).distinct().take(12)
                }
                if (guessKeywords.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        guessKeywords.forEach { keyword ->
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                                    .clickable { doQuickSearch(keyword) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                Text(keyword, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ═══ 快捷入口图标 ═══

@Composable
private fun QuickEntry(
    icon: ImageVector,
    title: String,
    colors: List<Color>,
    dayNumber: String? = null,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
                .border(0.6.dp, if (isDark) CardGlassBorderDark else CardGlassBorderLight, RoundedCornerShape(18.dp))
                .drawBehind {
                    drawRect(
                        color = if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                        alpha = 0.5f
                    )
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(colors[0].copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(this.size.width * 0.3f, this.size.height * 0.3f),
                            radius = this.size.maxDimension * 0.5f
                        )
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (dayNumber != null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(16.dp))
                    Text(
                        dayNumber,
                        color = if (isDark) Color.White else TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 15.sp,
                    )
                }
            } else {
                Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            fontSize = 11.sp,
            color = TextPrimary,
            maxLines = 1,
        )
    }
}

// ═══ 歌单封面卡片 ═══

@Composable
private fun PlaylistCoverCard(
    title: String,
    playCount: String,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2A3449), Color(0xFF1E2535)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
                .border(0.8.dp, Color(0x40FFFFFF), RoundedCornerShape(20.dp)),
        ) {
            // 装饰: 大光圈 - 紫色发光
            Box(
                Modifier.size(70.dp).align(Alignment.TopEnd)
                    .clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.08f)),
            )
            // 装饰: 小光圈
            Box(
                Modifier.size(40.dp).align(Alignment.BottomStart)
                    .clip(CircleShape).background(PrimaryPurple.copy(alpha = 0.05f)),
            )
            // 音乐图标
            Icon(
                Icons.Default.LibraryMusic,
                null,
                tint = PrimaryPurple.copy(alpha = 0.35f),
                modifier = Modifier.size(48.dp).align(Alignment.Center),
            )
            // 播放量标签
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(3.dp))
                Text(playCount, color = Color.White, fontSize = 9.sp)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}


// ═══ 推荐歌单卡片 (真实封面) ═══

@Composable
private fun RecommendPlaylistCard(
    playlist: RecommendPlaylist,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
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
                Icon(
                    Icons.Default.LibraryMusic,
                    null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }
            // 平台标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(playlist.platform, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            // 播放量标签
            if (playlist.playCount > 0) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.30f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(formatPlayCount(playlist.playCount), color = Color.White, fontSize = 9.sp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            playlist.name,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

// 格式化播放量
private fun formatPlayCount(count: Long): String {
    return when {
        count >= 100_000_000 -> "${count / 100_000_000}亿"
        count >= 10_000 -> "${count / 10_000}万"
        count >= 1000 -> "${count / 1000}k"
        else -> "$count"
    }
}

// ═══ 热歌封面卡片 (真实数据) ═══

@Composable
private fun HotSongCoverCard(
    song: Song,
    onClick: () -> Unit,
) {
    val fallbackGradient = remember(song.id) {
        val idx = ((song.id % CoverGradients.size).toInt() + CoverGradients.size) % CoverGradients.size
        CoverGradients[idx]
    }

    Column(
        modifier = Modifier
            .width(130.dp)
            .clickable { onClick() },
    ) {
        Box(
            modifier = Modifier
                .size(130.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (song.coverUrl.isEmpty()) Brush.linearGradient(fallbackGradient)
                    else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))
                ),
        ) {
            if (song.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.LibraryMusic,
                    null,
                    tint = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }
            // 平台标签
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(song.platform, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Medium)
            }
            // 播放按钮
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ═══ 新歌标签 ═══

@Composable
private fun NewSongChip(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .width(155.dp)
            .height(68.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 左侧色条
        Box(
            Modifier
                .width(3.5.dp)
                .height(34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(listOf(NebulaViolet, NebulaViolet.copy(alpha = 0.4f)))
                ),
        )
        Spacer(Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextTertiary, maxLines = 1)
        }
    }
}

// ═══ 场景卡片 ═══

@Composable
private fun SceneCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.12f), color.copy(alpha = 0.03f))
                )
            )
            .border(1.dp, color.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(2.dp))
        Text(subtitle, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

// ═══ 热搜排行条目 ═══

@Composable
private fun HotSearchItem(rank: Int, keyword: String, onClick: () -> Unit) {
    val rankColor = when (rank) {
        1 -> NebulaViolet
        2 -> NebulaPink
        3 -> Color(0xFFFFAB00)
        else -> TextTertiary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(if (rank <= 3) Modifier.background(rankColor.copy(alpha = 0.06f)) else Modifier)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .then(if (rank <= 3) Modifier.background(rankColor.copy(alpha = 0.15f)) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$rank",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = rankColor,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            keyword,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = if (rank <= 3) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (rank <= 3) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(rankColor.copy(alpha = 0.12f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("HOT", fontSize = 8.sp, color = rankColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}
