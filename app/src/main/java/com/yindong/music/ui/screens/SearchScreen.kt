@file:OptIn(ExperimentalLayoutApi::class)

package com.yindong.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.yindong.music.R
import com.yindong.music.data.model.Song
import coil.compose.AsyncImage
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.TagBlue
import com.yindong.music.ui.theme.TagGreen
import com.yindong.music.ui.theme.TagOrange
import com.yindong.music.ui.theme.TagPurple
import com.yindong.music.ui.theme.TagRed
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun SearchScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
) {
    // 平台列表（带图标）- 混合使用本地图标和网络图标
    // Pair<平台名称, Pair<图标类型, 颜色>>
    // 图标类型: 本地资源ID (Int) 或 网络URL (String)
    val platforms = listOf(
        Triple("网易云", R.drawable.netease_logo as Any, TagRed),
        Triple("酷我音乐", "https://www.kuwo.cn/favicon.ico" as Any, TagOrange),
        Triple("酷狗音乐【不可用】", R.drawable.kugou_logo as Any, TagBlue),
        Triple("QQ音乐", R.drawable.qqmusic_logo as Any, TagGreen),
    )
    var selectedPlatform by remember { mutableStateOf("网易云") }
    var showPlatformSelector by remember { mutableStateOf(false) }
    val sortModes = listOf("默认", "歌名", "时长")
    var selectedSortMode by remember { mutableStateOf("默认") }

    // 只显示当前选择平台的结果
    val filteredResults = viewModel.onlineResults.filter { it.platform == selectedPlatform }
    val sortedResults = when (selectedSortMode) {
        "歌名" -> filteredResults.sortedBy { it.title.lowercase() }
        "时长" -> filteredResults.sortedByDescending { it.duration }
        else -> filteredResults
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        // ── 搜索栏（带平台选择） ──
        SearchTopBarWithPlatform(
            query = viewModel.searchQuery,
            selectedPlatform = selectedPlatform,
            platforms = platforms,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            onPlatformClick = { showPlatformSelector = true },
            onSearch = {
                val q = viewModel.searchQuery.trim()
                if (viewModel.isShareLink(q)) {
                    viewModel.parseAndPlay(q)
                } else {
                    // 单平台搜索
                    viewModel.performSinglePlatformSearch(q, selectedPlatform)
                }
            },
        )

        // 链接解析中
        if (viewModel.isParsing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp), color = Red500, strokeWidth = 2.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("正在解析链接...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        // 链接解析错误
        viewModel.parseError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = Red500, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        // 搜索错误提示
        viewModel.searchError?.let { error ->
            Text(error, style = MaterialTheme.typography.bodySmall, color = Red500, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        when {
            // ── 有搜索结果：显示结果 ──
            viewModel.onlineResults.isNotEmpty() || viewModel.isSearching -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {
                    // 搜索中
                    if (viewModel.isSearching) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(36.dp), color = Red500, strokeWidth = 2.5.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "正在搜索中...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    // 在线搜索结果
                    if (viewModel.onlineResults.isNotEmpty()) {
                        // 播放全部按钮
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Red500)
                                        .clickable { viewModel.playAllFromList(sortedResults) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("播放全部", color = Color.White, style = MaterialTheme.typography.labelMedium)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    "找到 ${sortedResults.size} 首相关歌曲",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // 排序方式
                        item {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp),
                            ) {
                                item {
                                    Text(
                                        "排序",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp),
                                    )
                                }
                                items(sortModes) { mode ->
                                    val selected = selectedSortMode == mode
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (selected) Red500.copy(alpha = 0.12f)
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedSortMode = mode }
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Text(
                                            text = mode,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (selected) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        )
                                    }
                                }
                            }
                        }

                        if (sortedResults.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 36.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "该平台暂无结果，换个平台试试",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            itemsIndexed(sortedResults) { index, song ->
                                OnlineResultItem(
                                    song = song,
                                    index = index,
                                    viewModel = viewModel,
                                    allSongs = sortedResults,
                                    onClick = { viewModel.playPlaylist(sortedResults, index) },
                                )
                            }
                        }

                        // 加载更多按钮
                        if (viewModel.onlineResults.isNotEmpty() && viewModel.hasMoreResults) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (viewModel.isLoadingMore) {
                                        // 加载中状态
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Red500,
                                                strokeWidth = 2.dp,
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "加载中...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    } else {
                                        // 加载更多按钮
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { viewModel.loadMoreResults() }
                                                .padding(vertical = 14.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "加载更多",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Medium,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }

            // ── 有输入但无结果：显示搜索建议 ──
            viewModel.searchQuery.isNotEmpty() -> {
                // 显示搜索建议
                if (viewModel.searchSuggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        item {
                            Text(
                                "搜索建议",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(viewModel.searchSuggestions) { suggestion ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSearchQuery(suggestion.keyword)
                                        viewModel.performOnlineSearch(suggestion.keyword)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Search,
                                    null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    suggestion.keyword,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("按回车键搜索", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("搜索 QQ/网易云/酷我/酷狗 4大平台", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // ── 无输入：显示推荐 + 热搜 + 历史 ──
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp),
                ) {
                    // ── 链接解析提示 ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(Red500.copy(alpha = 0.08f), TagPurple.copy(alpha = 0.06f))
                                    )
                                )
                                .padding(14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Link, null, tint = Red500, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("粘贴链接解析", style = MaterialTheme.typography.titleSmall, color = Red500, fontWeight = FontWeight.SemiBold)
                                    Text("支持抖音/汽水音乐分享链接自动解析播放", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }

                    // ── 大家都在搜 ──
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.LocalFireDepartment, null, tint = Red500, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("大家都在搜", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    item {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            viewModel.hotSearches.forEachIndexed { index, keyword ->
                                val isTop3 = index < 3
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(
                                            if (isTop3) Red500.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            viewModel.updateSearchQuery(keyword)
                                            viewModel.performOnlineSearch(keyword)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (isTop3) {
                                        Text(
                                            "${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Red500,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        keyword,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isTop3) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isTop3) FontWeight.SemiBold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }

                    // ── 快速发现 ──
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Star, null, tint = TagOrange, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("快速发现", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            item {
                                DiscoverChip(
                                    icon = Icons.Default.TrendingUp,
                                    label = "抖音热歌",
                                    gradient = listOf(Red500, Color(0xFFFF6B6B)),
                                    onClick = {
                                        viewModel.updateSearchQuery("抖音热歌")
                                        viewModel.performOnlineSearch("抖音热歌")
                                    },
                                )
                            }
                            item {
                                DiscoverChip(
                                    icon = Icons.Default.Favorite,
                                    label = "伤感情歌",
                                    gradient = listOf(TagPurple, Color(0xFFCE93D8)),
                                    onClick = {
                                        viewModel.updateSearchQuery("伤感情歌")
                                        viewModel.performOnlineSearch("伤感情歌")
                                    },
                                )
                            }
                            item {
                                DiscoverChip(
                                    icon = Icons.Default.MusicNote,
                                    label = "怀旧金曲",
                                    gradient = listOf(TagOrange, Color(0xFFFFCC02)),
                                    onClick = {
                                        viewModel.updateSearchQuery("怀旧金曲")
                                        viewModel.performOnlineSearch("怀旧金曲")
                                    },
                                )
                            }
                            item {
                                DiscoverChip(
                                    icon = Icons.Default.MusicNote,
                                    label = "车载音乐",
                                    gradient = listOf(TagBlue, Color(0xFF64B5F6)),
                                    onClick = {
                                        viewModel.updateSearchQuery("车载音乐")
                                        viewModel.performOnlineSearch("车载音乐")
                                    },
                                )
                            }
                            item {
                                DiscoverChip(
                                    icon = Icons.Default.MusicNote,
                                    label = "古风音乐",
                                    gradient = listOf(TagGreen, Color(0xFFA5D6A7)),
                                    onClick = {
                                        viewModel.updateSearchQuery("古风音乐")
                                        viewModel.performOnlineSearch("古风音乐")
                                    },
                                )
                            }
                        }
                    }

                    // ── 搜索历史 ──
                    if (viewModel.searchHistory.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("搜索历史", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                                }
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "清除历史",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp).clickable { viewModel.clearSearchHistory() },
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(viewModel.searchHistory.take(10)) { keyword ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateSearchQuery(keyword)
                                        viewModel.performOnlineSearch(keyword)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(keyword, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        // 平台选择弹窗
        if (showPlatformSelector) {
            PlatformSelectorDialog(
                platforms = platforms,
                selectedPlatform = selectedPlatform,
                onPlatformSelected = { selectedPlatform = it },
                onDismiss = { showPlatformSelector = false },
            )
        }
    }
}

/** 快速发现标签 */
@Composable
private fun DiscoverChip(
    icon: ImageVector,
    label: String,
    gradient: List<Color>,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.horizontalGradient(gradient))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}

/** 在线搜索结果条目 (带封面、序号和更多操作按钮) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineResultItem(
    song: Song,
    index: Int,
    viewModel: MusicViewModel,
    allSongs: List<Song>,
    onClick: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 序号
        Text(
            "${index + 1}",
            style = MaterialTheme.typography.bodySmall,
            color = if (index < 3) Red500 else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp),
            fontWeight = if (index < 3) FontWeight.Bold else FontWeight.Normal,
        )

        // 专辑封面
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            if (song.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = song.coverUrl,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // 歌曲信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                song.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 平台标签
                val platformColor = when (song.platform) {
                    "QQ音乐" -> TagGreen
                    "网易云" -> Red500
                    "酷我音乐" -> TagBlue
                    "酷狗音乐" -> TagOrange
                    else -> TagPurple
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(platformColor.copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                ) {
                    Text(
                        song.platform,
                        style = MaterialTheme.typography.labelSmall,
                        color = platformColor,
                        fontSize = 9.sp,
                    )
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "${song.artist} · ${song.album}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // ••• 更多操作按钮
        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "更多",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // ── 歌曲操作底部弹窗 ──
    if (showMenu) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            sheetState = sheetState,
        ) {
            // 歌曲信息头部
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center,
                ) {
                    if (song.coverUrl.isNotEmpty()) {
                        AsyncImage(model = song.coverUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${song.artist} - ${song.album}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

            // 下一首播放
            SongActionItem(
                icon = Icons.Default.QueuePlayNext,
                label = "下一首播放",
                onClick = {
                    viewModel.playNextInQueue(song)
                    showMenu = false
                },
            )

            // 收藏
            val isFav = viewModel.isFavorite(song)
            SongActionItem(
                icon = if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                label = if (isFav) "取消收藏" else "收藏",
                tint = if (isFav) Red500 else MaterialTheme.colorScheme.onSurface,
                onClick = {
                    viewModel.toggleFavorite(song)
                    showMenu = false
                },
            )

            // 添加到歌单
            SongActionItem(
                icon = Icons.Default.PlaylistAdd,
                label = "添加到歌单",
                onClick = {
                    showPlaylistPicker = true
                    showMenu = false
                },
            )

            // 下载 (仅插件模式显示)
            if (viewModel.apiMode == "lx_plugin") {
                SongActionItem(
                    icon = Icons.Default.Download,
                    label = "下载",
                    onClick = {
                        viewModel.downloadSong(song)
                        showMenu = false
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    // ── 歌单选择弹窗 ──
    if (showPlaylistPicker) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showPlaylistPicker = false },
            sheetState = sheetState,
        ) {
            Text(
                "添加到歌单",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            if (viewModel.myPlaylists.isEmpty()) {
                Text(
                    "暂无歌单，请先创建歌单",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                )
            } else {
                viewModel.myPlaylists.forEach { playlist ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.addToPlaylist(playlist.id, song)
                                viewModel.showToast("已添加到\u300c${playlist.name}\u300d")
                                showPlaylistPicker = false
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(playlist.name, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${playlist.songCount}首",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 底部弹窗操作项 */
@Composable
private fun SongActionItem(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSearch: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = {
                Text(
                    "搜索你喜欢的音乐 或 粘贴链接",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Red500,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(24.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Red500)
                .clickable { onSearch() }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "搜索",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
    }
}

/**
 * 带平台选择的搜索栏
 */
@Composable
private fun SearchTopBarWithPlatform(
    query: String,
    selectedPlatform: String,
    platforms: List<Triple<String, Any, Color>>,
    onQueryChange: (String) -> Unit,
    onPlatformClick: () -> Unit,
    onSearch: () -> Unit,
) {
    val selectedPlatformInfo = platforms.find { it.first == selectedPlatform }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 平台选择图标 - 支持本地SVG和网络图标
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(selectedPlatformInfo?.third ?: MaterialTheme.colorScheme.surfaceVariant)
                .clickable { onPlatformClick() }
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            selectedPlatformInfo?.let { (_, iconSource, _) ->
                when (iconSource) {
                    is Int -> {
                        // 本地资源
                        Image(
                            painter = painterResource(id = iconSource),
                            contentDescription = selectedPlatform,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    is String -> {
                        // 网络图片
                        AsyncImage(
                            model = iconSource,
                            contentDescription = selectedPlatform,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .height(48.dp),
            placeholder = {
                Text(
                    "搜索音乐、歌手",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "清除",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Red500,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            ),
            shape = RoundedCornerShape(24.dp),
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(Red500)
                .clickable { onSearch() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "搜索",
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * 平台选择弹窗
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlatformSelectorDialog(
    platforms: List<Triple<String, Any, Color>>,
    selectedPlatform: String,
    onPlatformSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                "选择音乐平台",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
            )

            platforms.forEach { (name, iconSource, color) ->
                val isSelected = selectedPlatform == name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) color.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable {
                            onPlatformSelected(name)
                            onDismiss()
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.2f))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (iconSource) {
                            is Int -> {
                                // 本地资源
                                Image(
                                    painter = painterResource(id = iconSource),
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            is String -> {
                                // 网络图片
                                AsyncImage(
                                    model = iconSource,
                                    contentDescription = name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已选择",
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
