package com.yindong.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yindong.music.data.model.Song
import com.yindong.music.ui.components.AddToPlaylistSheet
import com.yindong.music.ui.components.SongItem
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.Red600
import com.yindong.music.ui.theme.TagBlue
import com.yindong.music.ui.theme.TagGreen
import com.yindong.music.ui.theme.TagOrange
import com.yindong.music.ui.theme.TagPurple
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.CardGlassHighlightDark
import com.yindong.music.ui.theme.CardGlassHighlightLight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.isSystemInDarkTheme
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun PlaylistScreen(
    viewModel: MusicViewModel,
) {
    val categories = listOf("华语", "流行", "摇滚", "民谣", "电子", "轻音乐", "古风", "说唱", "欧美", "日韩", "粤语", "R&B", "爵士", "乡村")
    var selectedCategory by remember { mutableIntStateOf(0) }
    // 切换列表/网格视图: false=歌曲列表, true=封面网格
    var showGrid by remember { mutableStateOf(true) }
    // 添加到歌单弹窗
    var songToAdd by remember { mutableStateOf<Song?>(null) }

    LaunchedEffect(selectedCategory) {
        kotlinx.coroutines.delay(300L)
        viewModel.searchCategory(categories[selectedCategory])
    }

    // 添加到歌单弹窗
    songToAdd?.let { song ->
        AddToPlaylistSheet(
            song = song,
            playlists = viewModel.userPlaylists,
            onSelect = { playlistId ->
                viewModel.addSongToPlaylist(playlistId, song)
            },
            onDismiss = { songToAdd = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── 顶部标题 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(TagBlue.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.LibraryMusic, null, tint = TagBlue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "歌单广场",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "海量曲库，分类发现好歌",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
        }

        // ── 精选推荐卡片 ──
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(vertical = 8.dp),
        ) {
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.WorkspacePremium,
                    title = "华语经典",
                    gradient = listOf(Red500, Red600),
                    onClick = { viewModel.searchCategory("华语经典"); selectedCategory = 0 },
                )
            }
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.Headphones,
                    title = "欧美热歌",
                    gradient = listOf(TagBlue, Color(0xFF1565C0)),
                    onClick = { viewModel.searchCategory("欧美热歌"); selectedCategory = 8 },
                )
            }
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.Nightlight,
                    title = "睡前轻音乐",
                    gradient = listOf(TagPurple, Color(0xFF6A1B9A)),
                    onClick = { viewModel.searchCategory("睡前轻音乐"); selectedCategory = 5 },
                )
            }
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.SportsEsports,
                    title = "游戏BGM",
                    gradient = listOf(TagGreen, Color(0xFF2E7D32)),
                    onClick = { viewModel.searchCategory("游戏BGM"); selectedCategory = 3 },
                )
            }
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.FavoriteBorder,
                    title = "甜蜜恋爱",
                    gradient = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)),
                    onClick = { viewModel.searchCategory("甜蜜恋爱"); selectedCategory = 1 },
                )
            }
            item {
                PlaylistQuickCard(
                    icon = Icons.Default.Spa,
                    title = "瑜伽冥想",
                    gradient = listOf(TagOrange, Color(0xFFE65100)),
                    onClick = { viewModel.searchCategory("瑜伽冥想"); selectedCategory = 5 },
                )
            }
        }

        // ── 分类标签栏 ──
        ScrollableTabRow(
            selectedTabIndex = selectedCategory,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            edgePadding = 16.dp,
            indicator = {},
            divider = {},
        ) {
            categories.forEachIndexed { index, category ->
                val selected = selectedCategory == index
                val isDark = isSystemInDarkTheme()
                Tab(
                    selected = selected,
                    onClick = { selectedCategory = index },
                    modifier = Modifier.padding(horizontal = 3.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                when {
                                    selected -> Red500.copy(alpha = 0.75f)
                                    isDark -> CardGlassBackgroundDark
                                    else -> CardGlassBackgroundLight
                                }
                            )
                            .border(
                                0.5.dp,
                                when {
                                    selected -> Red500.copy(alpha = 0.4f)
                                    isDark -> CardGlassBorderDark
                                    else -> CardGlassBorderLight
                                },
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text(
                            category,
                            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 内容区域 ──
        if (viewModel.isCategoryLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Red500, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "正在搜索「${categories[selectedCategory]}」...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else if (viewModel.categorySongs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("暂无结果", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("换个分类试试吧", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        } else {
            // 操作栏: 播放全部 + 视图切换
            val isDark = isSystemInDarkTheme()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Red500.copy(alpha = if (isDark) 0.8f else 0.9f))
                        .border(0.5.dp, Red500.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                        .clickable { viewModel.playAllFromList(viewModel.categorySongs) }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("播放全部", color = Color.White, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "共 ${viewModel.categorySongs.size} 首",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                // 视图切换按钮
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
                        .border(0.5.dp, if (isDark) CardGlassBorderDark else CardGlassBorderLight, RoundedCornerShape(16.dp))
                        .padding(2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (showGrid) Red500.copy(alpha = 0.8f) else Color.Transparent)
                            .border(
                                if (showGrid) 0.5.dp else 0.dp,
                                if (showGrid) Red500.copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showGrid = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            Icons.Default.LibraryMusic, null,
                            tint = if (showGrid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (!showGrid) Red500.copy(alpha = 0.8f) else Color.Transparent)
                            .border(
                                if (!showGrid) 0.5.dp else 0.dp,
                                if (!showGrid) Red500.copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { showGrid = false }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    ) {
                        Icon(
                            Icons.Default.MusicNote, null,
                            tint = if (!showGrid) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            if (showGrid) {
                // ── 封面网格视图 ──
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 160.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(viewModel.categorySongs) { index, song ->
                        SongCoverCard(
                            song = song,
                            index = index,
                            onClick = {
                                viewModel.onlineResults = viewModel.categorySongs
                                viewModel.playSong(song)
                            },
                            onLongClick = { songToAdd = song },
                        )
                    }
                }
            } else {
                // ── 列表视图 ──
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp),
                ) {
                    itemsIndexed(viewModel.categorySongs) { index, song ->
                        SongItem(
                            song = song,
                            index = index,
                            onSongClick = {
                                viewModel.onlineResults = viewModel.categorySongs
                                viewModel.playSong(song)
                            },
                            onMoreClick = { songToAdd = song },
                        )
                    }
                }
            }
        }
    }
}


/** 歌曲封面卡片 (网格视图) */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongCoverCard(
    song: Song,
    index: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val fallbackGradient = remember(song.id) {
        val idx = ((song.id % CoverGradients.size).toInt() + CoverGradients.size) % CoverGradients.size
        CoverGradients[idx]
    }

    Column(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        // 封面
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(14.dp))
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
                    Icons.Default.MusicNote,
                    null,
                    tint = Color.White.copy(alpha = 0.35f),
                    modifier = Modifier.size(36.dp).align(Alignment.Center),
                )
            }

            // 序号角标
            if (index < 3) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (index) {
                                0 -> Red500
                                1 -> Color(0xFFFF6B35)
                                else -> Color(0xFFFFAB00)
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // 平台标签
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Text(
                    song.platform,
                    color = Color.White,
                    fontSize = 8.sp,
                    maxLines = 1,
                )
            }

            // 播放按钮
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // 歌名
        Text(
            song.title,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Medium,
            lineHeight = 16.sp,
        )
        // 歌手
        Text(
            song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 14.sp,
        )
    }
}

/** 精选推荐小卡片 - 毛玻璃拟态风格 */
@Composable
private fun PlaylistQuickCard(
    icon: ImageVector,
    title: String,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .width(100.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
            .border(0.6.dp, if (isDark) CardGlassBorderDark else CardGlassBorderLight, RoundedCornerShape(16.dp))
            .drawBehind {
                drawRect(
                    color = if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                    alpha = 0.5f
                )
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gradient[0].copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(this.size.width * 0.3f, this.size.height * 0.3f),
                        radius = this.size.maxDimension * 0.5f
                    )
                )
            }
            .clickable { onClick() }
            .padding(10.dp),
    ) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize(),
        ) {
            Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
            Text(title, color = if (isDark) Color.White else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}