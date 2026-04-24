package com.yindong.music.ui.screens
import android.content.Intent
import android.net.Uri

import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.HighQuality
import com.yindong.music.data.api.MusicApiConfig
import com.yindong.music.data.CrashLogEntry
import com.yindong.music.data.RemoteConfig
import coil.compose.AsyncImage
import com.yindong.music.data.model.Playlist
import com.yindong.music.data.model.Song
import com.yindong.music.security.CriticalUiProtector
import com.yindong.music.ui.components.SleepTimerDialog
import com.yindong.music.ui.components.SongItem
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.CardGlassHighlightDark
import com.yindong.music.ui.theme.CardGlassHighlightLight
import com.yindong.music.ui.theme.AppTheme
import com.yindong.music.ui.theme.AppThemes
import com.yindong.music.ui.theme.ThemeManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.geometry.Offset
import com.yindong.music.viewmodel.MusicViewModel
import kotlinx.coroutines.launch

@Composable
fun MineScreen(
    viewModel: MusicViewModel,
    onSearchClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onPlaylistClick: (Long) -> Unit,
    onAiAudioEffectClick: () -> Unit = {},
    onImportPlaylistClick: () -> Unit = {},
    onDownloadsClick: () -> Unit = {},
) {
    var showSettings by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showFavorites by remember { mutableStateOf(false) }
    var showPluginManager by remember { mutableStateOf(false) }
    var showApiConfig by remember { mutableStateOf(false) }
    var showCreatePlaylist by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }
    var viewingPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var deleteConfirm by remember { mutableStateOf<Playlist?>(null) }
    var songToDelete by remember { mutableStateOf<Song?>(null) }

    val cs = MaterialTheme.colorScheme

    // ── 对话框们 ──
    if (showSettings) SettingsDialog(viewModel) { showSettings = false }
    if (showCreatePlaylist) CreatePlaylistDialog(viewModel) { showCreatePlaylist = false }
    if (showPluginManager) PluginManagerDialog(viewModel, onBack = { showPluginManager = false })
    if (showApiConfig) ApiConfigDialog(viewModel, onDismiss = { showApiConfig = false }, onOpenPluginManager = { showApiConfig = false; showPluginManager = true })
    if (showThemePicker) ThemePickerDialog(onDismiss = { showThemePicker = false })
    deleteConfirm?.let { pl ->
        AlertDialog(
            onDismissRequest = { deleteConfirm = null },
            title = { Text("删除歌单") },
            text = { Text("确定删除「${pl.name}」？") },
            confirmButton = { TextButton(onClick = { viewModel.deletePlaylist(pl.id); deleteConfirm = null; if (viewingPlaylist?.id == pl.id) viewingPlaylist = null }) { Text("删除", color = Red500) } },
            dismissButton = { TextButton(onClick = { deleteConfirm = null }) { Text("取消") } },
        )
    }
    // 删除歌曲确认
    songToDelete?.let { song ->
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text("从歌单删除") },
            text = { Text("确定移除歌曲「${song.title}」？") },
            confirmButton = {
                TextButton(onClick = {
                    viewingPlaylist?.let { pl ->
                        viewModel.removeSongFromPlaylist(pl.id, song)
                    }
                    songToDelete = null
                }) { Text("移除", color = Red500) }
            },
            dismissButton = { TextButton(onClick = { songToDelete = null }) { Text("取消") } },
        )
    }

    // ── 子页面: 收藏 ──
    if (showFavorites) {
        val clipboardManager = LocalClipboardManager.current
        var showAddFavoriteDialog by remember { mutableStateOf(false) }
        var favTitle by remember { mutableStateOf("") }
        var favArtist by remember { mutableStateOf("") }

        var showImportFavoritesDialog by remember { mutableStateOf(false) }
        var importFavoritesText by remember { mutableStateOf("") }

        var showDeleteFavoriteConfirm by remember { mutableStateOf<Song?>(null) }

        if (showAddFavoriteDialog) {
            AlertDialog(
                onDismissRequest = { showAddFavoriteDialog = false },
                title = { Text("添加到收藏") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = favTitle,
                            onValueChange = { favTitle = it },
                            label = { Text("歌名") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = favArtist,
                            onValueChange = { favArtist = it },
                            label = { Text("歌手") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.addFavoriteManual(favTitle, favArtist)
                            showAddFavoriteDialog = false
                            favTitle = ""
                            favArtist = ""
                        }
                    ) { Text("添加", color = Red500) }
                },
                dismissButton = { TextButton(onClick = { showAddFavoriteDialog = false }) { Text("取消") } }
            )
        }

        if (showImportFavoritesDialog) {
            AlertDialog(
                onDismissRequest = { showImportFavoritesDialog = false },
                title = { Text("导入收藏") },
                text = {
                    Column {
                        Text("粘贴收藏列表内容：", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importFavoritesText,
                            onValueChange = { importFavoritesText = it },
                            label = { Text("粘贴收藏内容") },
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            maxLines = 30
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (importFavoritesText.isNotBlank()) {
                                viewModel.importFavoritesFromText(importFavoritesText)
                                showImportFavoritesDialog = false
                                importFavoritesText = ""
                            }
                        }
                    ) { Text("导入", color = Red500) }
                },
                dismissButton = { TextButton(onClick = { showImportFavoritesDialog = false }) { Text("取消") } }
            )
        }

        showDeleteFavoriteConfirm?.let { song ->
            AlertDialog(
                onDismissRequest = { showDeleteFavoriteConfirm = null },
                title = { Text("删除收藏") },
                text = { Text("确定从收藏移除「${song.title}」？") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.removeFavorite(song)
                            showDeleteFavoriteConfirm = null
                        }
                    ) { Text("移除", color = Red500) }
                },
                dismissButton = { TextButton(onClick = { showDeleteFavoriteConfirm = null }) { Text("取消") } }
            )
        }

        // 自动为没有封面的收藏歌曲搜索匹配封面
        LaunchedEffect(Unit) { viewModel.fetchMissingFavCovers() }

        SubPage("我的收藏", "${viewModel.favorites.size} 首", onBack = { showFavorites = false },
            onPlayAll = if (viewModel.favorites.isNotEmpty()) {{ viewModel.playAllFromList(viewModel.favorites) }} else null,
            viewModel = viewModel,
            onAddMusic = { showAddFavoriteDialog = true },
            onCopyPlaylist = {
                val exportText = viewModel.exportFavoritesAsText()
                if (exportText.isBlank()) {
                    viewModel.showToast("没有收藏可导出")
                } else {
                    clipboardManager.setText(AnnotatedString(exportText))
                    viewModel.showToast("已复制到剪贴板")
                }
            },
        ) {
            if (viewModel.favorites.isEmpty()) EmptyHint(Icons.Default.FavoriteBorder, "还没有收藏", "播放时点 ♡ 收藏歌曲")
            else {
                LazyColumn {
                    items(viewModel.favorites) { song ->
                        SongItem(
                            song = song,
                            onSongClick = { viewModel.playSong(song) },
                            onMoreClick = { showDeleteFavoriteConfirm = song },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TextButton(onClick = { showImportFavoritesDialog = true }) {
                        Icon(Icons.Default.Download, null, tint = Red500, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("导入", color = Red500)
                    }
                }
            }
        }
        return
    }

    // ── 子页面: 历史 ──
    if (showHistory) {
        SubPage("最近播放", "${viewModel.playHistory.size} 首", onBack = { showHistory = false },
            onPlayAll = if (viewModel.playHistory.isNotEmpty()) {{ viewModel.playAllFromList(viewModel.playHistory) }} else null,
            viewModel = viewModel,
        ) {
            if (viewModel.playHistory.isEmpty()) EmptyHint(Icons.Default.History, "还没有播放记录", "搜索歌曲开始播放吧")
            else LazyColumn { items(viewModel.playHistory) { SongItem(song = it, onSongClick = { viewModel.playSong(it) }) } }
        }
        return
    }

    // ── 子页面: 歌单详情 ──
    viewingPlaylist?.let { pl ->
        val latest = viewModel.getUserPlaylistById(pl.id) ?: pl
        val clipboardManager = LocalClipboardManager.current
        
        // 复制歌单对话框
        var showCopyDialog by remember { mutableStateOf(false) }
        if (showCopyDialog) {
            val copyText = remember(latest) {
                buildString {
                    appendLine("🎵 ${latest.name}")
                    appendLine("---")
                    latest.songs.forEachIndexed { index, song ->
                        appendLine("${index + 1}. ${song.title} - ${song.artist}")
                    }
                }
            }
            AlertDialog(
                onDismissRequest = { showCopyDialog = false },
                title = { Text("复制歌单") },
                text = {
                    Column {
                        Text("歌单内容已生成，点击复制：", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().height(250.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        ) {
                            Text(
                                copyText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(copyText))
                            viewModel.showToast("歌单已复制到剪贴板")
                            showCopyDialog = false
                        }
                    ) { Text("复制", color = Red500) }
                },
                dismissButton = { TextButton(onClick = { showCopyDialog = false }) { Text("关闭") } }
            )
        }
        
        SubPage(latest.name, "${latest.songs.size} 首", onBack = { viewingPlaylist = null },
            onPlayAll = if (latest.songs.isNotEmpty()) {{ viewModel.playAllFromList(latest.songs) }} else null,
            viewModel = viewModel,
            onAddMusic = onSearchClick,
            onCopyPlaylist = { showCopyDialog = true }
        ) {
            if (latest.songs.isEmpty()) EmptyHint(Icons.Default.List, "歌单还是空的", "点击右上角 + 去搜索添加")
            else LazyColumn {
                items(latest.songs) { song ->
                    SongItem(
                        song = song,
                        onSongClick = { viewModel.playSong(song) },
                        onMoreClick = { songToDelete = song }, // 传递更多点击事件
                    )
                }
            }
        }
        return
    }

    // ═══ 主页面 ═══
    LazyColumn(
        Modifier.fillMaxSize().background(cs.background).statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 160.dp),
    ) {
        // 顶栏
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("我的", style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold), color = cs.onBackground)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, null, tint = cs.onSurfaceVariant)
                }
            }
        }

        // 最近播放 + 收藏 卡片
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.History,
                    title = "最近播放",
                    count = "${viewModel.playHistory.size}",
                    gradient = listOf(Color(0xFFFF6B6B), Color(0xFFEE5A24)),
                    onClick = { showHistory = true },
                )
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Favorite,
                    title = "我的收藏",
                    count = "${viewModel.favorites.size}",
                    gradient = listOf(Color(0xFFE056A0), Color(0xFF9B59B6)),
                    onClick = { showFavorites = true },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Download,
                    title = "下载管理",
                    count = "${viewModel.downloadedSongs.size}",
                    gradient = listOf(Color(0xFF43A047), Color(0xFF2E7D32)),
                    criticalButtonKey = "mine_download",
                    onClick = onDownloadsClick,
                )
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Settings,
                    title = "API设置",
                    count = if (viewModel.apiMode == "lx_plugin") "落雪" else "官方",
                    gradient = listOf(Color(0xFF5C6BC0), Color(0xFF3949AB)),
                    criticalButtonKey = "mine_api",
                    onClick = { showApiConfig = true },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Code,
                    title = "落雪插件",
                    count = if (viewModel.lxPlugins.isEmpty()) "未导入" else "${viewModel.lxPlugins.size} 个",
                    gradient = listOf(Color(0xFFFF8F00), Color(0xFFE65100)),
                    criticalButtonKey = "mine_plugin",
                    onClick = { showPluginManager = true },
                )
                val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.PhonelinkErase,
                    title = "后台播放",
                    count = if (viewModel.isPlaying) "播放中" else "",
                    gradient = listOf(Color(0xFF78909C), Color(0xFF546E7A)),
                    onClick = { activity?.moveTaskToBack(true) },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Equalizer,
                    title = "AI音效",
                    count = "空间声场",
                    gradient = listOf(Color(0xFF00BCD4), Color(0xFF1565C0)),
                    onClick = onAiAudioEffectClick,
                )
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LightMode,
                    title = "主题设置",
                    count = "个性化",
                    gradient = listOf(Color(0xFF7B68EE), Color(0xFF00D2FF)),
                    onClick = { showThemePicker = true },
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LibCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Info,
                    title = "关于应用",
                    count = "v1.0.0",
                    gradient = listOf(Color(0xFF607D8B), Color(0xFF455A64)),
                    onClick = { },
                )
                Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            OfficialCommunityEntryRow(
                viewModel = viewModel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                showSubtitle = true,
            )
        }

        // 我的歌单
        item {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("我的歌单", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onBackground)
                Spacer(Modifier.width(6.dp))
                Text("${viewModel.userPlaylists.size}", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(cs.primary.copy(alpha = 0.15f))
                        .clickable { onImportPlaylistClick() },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Download, null, tint = cs.primary, modifier = Modifier.size(18.dp)) }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Red500)
                        .clickable { showCreatePlaylist = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (viewModel.userPlaylists.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.List, null, tint = cs.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("点击 + 创建你的第一个歌单", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(viewModel.userPlaylists) { pl ->
                PlaylistRow(pl, onClick = { viewingPlaylist = pl }, onDelete = { deleteConfirm = pl })
            }
        }
    }
}

// ═══ 组件 ═══

@Composable
private fun OfficialCommunityEntryRow(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier,
    showSubtitle: Boolean = false,
) {
    val context = LocalContext.current
    val joinTitle = RemoteConfig.officialCommunityTitle.ifBlank { CriticalUiProtector.communityEntryTitle() }
    val groupQq = RemoteConfig.officialCommunityQq.ifBlank { CriticalUiProtector.communityQqNumber() }
    val subtitle = RemoteConfig.officialCommunitySubtitle.ifBlank { CriticalUiProtector.communityEntrySubtitle() }
    val joinUrl = RemoteConfig.officialCommunityJoinUrl.ifBlank { CriticalUiProtector.communityJoinUrl() }

    LaunchedEffect(Unit) {
        CriticalUiProtector.markCommunityEntryRendered()
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Red500.copy(alpha = 0.1f))
            .border(1.dp, Red500.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .clickable {
                if (!CriticalUiProtector.canOpenCommunityEntry()) {
                    viewModel.showToast("关键入口完整性校验失败")
                    return@clickable
                }
                CriticalUiProtector.markCommunityEntryOpened()
                if (joinUrl.isBlank()) {
                    viewModel.showToast("官方群QQ：$groupQq")
                    return@clickable
                }
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(joinUrl))) }
                    .onFailure { viewModel.showToast("打开官方群失败") }
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (showSubtitle) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("👥", fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    joinTitle,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Red500
                )
                Text(
                    "官方群QQ：$groupQq",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Default.ChevronRight, null, tint = Red500.copy(0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun LibCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    count: String,
    gradient: List<Color>,
    criticalButtonKey: String? = null,
    onClick: () -> Unit,
) {
    LaunchedEffect(criticalButtonKey, title) {
        if (!criticalButtonKey.isNullOrBlank()) {
            CriticalUiProtector.markCriticalButtonRendered(criticalButtonKey, title)
        }
    }
    val isDark = isSystemInDarkTheme()
    Box(
        modifier = modifier
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight)
            .border(0.6.dp, if (isDark) CardGlassBorderDark else CardGlassBorderLight, RoundedCornerShape(20.dp))
            .drawBehind {
                drawRect(
                    color = if (isDark) CardGlassHighlightDark else CardGlassHighlightLight,
                    alpha = 0.5f
                )
            }
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(gradient[0].copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(this.size.width * 0.3f, this.size.height * 0.3f),
                        radius = this.size.maxDimension * 0.5f
                    )
                )
            }
            .clickable { onClick() }
            .padding(16.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryPurple.copy(alpha = if (isDark) 0.18f else 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = PrimaryPurple, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(title, color = if (isDark) Color.White else TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.width(6.dp))
                Text(count, color = if (isDark) Color.White.copy(alpha = 0.5f) else TextSecondary.copy(alpha = 0.7f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun PlaylistRow(playlist: Playlist, onClick: () -> Unit, onDelete: () -> Unit) {
    val colors = remember(playlist.id) {
        val idx = ((playlist.id % CoverGradients.size).toInt() + CoverGradients.size) % CoverGradients.size
        CoverGradients[idx]
    }
    val songCovers = remember(playlist.songs) {
        playlist.songs.filter { it.coverUrl.isNotEmpty() }.map { it.coverUrl }.distinct().take(4)
    }
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 歌单封面: 歌单封面图 > 4宫格歌曲封面 > 渐变图标
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(colors)), contentAlignment = Alignment.Center) {
            if (playlist.coverUrl.isNotEmpty()) {
                AsyncImage(
                    model = playlist.coverUrl,
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else if (songCovers.size >= 4) {
                // 4宫格封面
                Column(Modifier.fillMaxSize()) {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(model = songCovers[0], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = songCovers[1], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        AsyncImage(model = songCovers[2], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                        AsyncImage(model = songCovers[3], contentDescription = null, modifier = Modifier.weight(1f).fillMaxHeight(), contentScale = ContentScale.Crop)
                    }
                }
            } else if (songCovers.isNotEmpty()) {
                AsyncImage(
                    model = songCovers.first(),
                    contentDescription = playlist.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(playlist.name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${playlist.songs.size} 首", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onDelete, Modifier.size(36.dp)) {
            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun SubPage(title: String, subtitle: String, onBack: () -> Unit, onPlayAll: (() -> Unit)? = null, viewModel: MusicViewModel? = null, onAddMusic: (() -> Unit)? = null, onCopyPlaylist: (() -> Unit)? = null, content: @Composable () -> Unit) {
    var showTimer by remember { mutableStateOf(false) }
    if (showTimer && viewModel != null) {
        SleepTimerDialog(viewModel) { showTimer = false }
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold))
                if (viewModel?.isSleepTimerRunning == true) {
                    val totalSec = (viewModel.sleepTimerMs / 1000).toInt()
                    Text("定时 ${totalSec / 60}:${"%02d".format(totalSec % 60)}", style = MaterialTheme.typography.bodySmall, color = Red500)
                }
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (viewModel != null) {
                IconButton(onClick = { showTimer = true }) {
                    Icon(Icons.Default.Timer, "定时", tint = if (viewModel.isSleepTimerRunning) Red500 else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 复制歌单按钮
            if (onCopyPlaylist != null) {
                IconButton(onClick = onCopyPlaylist) {
                    Icon(Icons.Default.ContentCopy, "复制歌单", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // 添加音乐按钮
            if (onAddMusic != null) {
                IconButton(onClick = onAddMusic) {
                    Icon(Icons.Default.Add, "添加音乐", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = onBack) { Text("返回", color = Red500) }
        }
        // 全部播放按钮
        if (onPlayAll != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Red500).clickable { onPlayAll() }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("全部播放", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Box(Modifier.fillMaxSize()) { content() }
    }
}

@Composable
private fun EmptyHint(icon: ImageVector, text: String, sub: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(52.dp))
            Spacer(Modifier.height(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f))
        }
    }
}

@Composable
private fun SettingsDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    var showCrashLogs by remember { mutableStateOf(false) }
    var showCrashDetail by remember { mutableStateOf<CrashLogEntry?>(null) }
    var showPluginManager by remember { mutableStateOf(false) }

    // ── 插件管理页 ──
    if (showPluginManager) {
        PluginManagerDialog(viewModel, onBack = { showPluginManager = false })
        return
    }

    // ── 崩溃日志详情页 ──
    showCrashDetail?.let { entry ->
        CrashLogDetailDialog(entry) { showCrashDetail = null }
        return
    }

    // ── 崩溃日志列表页 ──
    if (showCrashLogs) {
        CrashLogListDialog(viewModel, onBack = { showCrashLogs = false }, onDetail = { showCrashDetail = it })
        return
    }

    // ── 主设置页 ──
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text("云音乐") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 音质选择
                var showQualityDialog by remember { mutableStateOf(false) }
                if (showQualityDialog) {
                    AlertDialog(
                        onDismissRequest = { showQualityDialog = false },
                        title = { Text("选择播放音质", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MusicApiConfig.Quality.entries.forEach { q ->
                                    val isSelected = viewModel.selectedQuality == q
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) Red500.copy(0.08f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(1.5.dp, Red500.copy(0.3f), RoundedCornerShape(14.dp))
                                                else Modifier
                                            )
                                            .clickable {
                                                viewModel.setQuality(q)
                                                showQualityDialog = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Red500 else Color.Transparent)
                                                .then(
                                                    if (!isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Column {
                                            Text(
                                                q.label,
                                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                                color = if (isSelected) Red500 else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                if (q.bitrate == 0) "无损音质 (FLAC)" else "${q.bitrate}kbps",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) Red500.copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showQualityDialog = false }) { Text("关闭") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.HighQuality,
                    title = "默认音质",
                    subtitle = viewModel.selectedQuality.label,
                    onClick = { showQualityDialog = true }
                )
                
                Spacer(Modifier.height(8.dp))

                Text("版本 ${viewModel.currentVersion}")
                Spacer(Modifier.height(4.dp))
                val displayAuthorQq = RemoteConfig.officialAuthorQq.ifBlank { CriticalUiProtector.communityQqNumber() }
                Text(
                    "作者联系QQ: $displayAuthorQq",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                OfficialCommunityEntryRow(viewModel = viewModel)

                Spacer(Modifier.height(12.dp))

                // 下载最新版
                SettingsRow(
                    icon = Icons.Default.Download, title = "下载最新版",
                    subtitle = "点击跳转云盘下载页面",
                    onClick = { viewModel.openDownloadPage() },
                )

                Spacer(Modifier.height(8.dp))

                // 播放器样式选择
                var showPlayerStyleDialog by remember { mutableStateOf(false) }
                if (showPlayerStyleDialog) {
                    AlertDialog(
                        onDismissRequest = { showPlayerStyleDialog = false },
                        title = { Text("选择播放器样式", fontWeight = FontWeight.Bold) },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                MusicViewModel.PlayerStyle.entries.forEach { style ->
                                    val isSelected = viewModel.playerStyle == style
                                    Row(
                                        Modifier.fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSelected) Red500.copy(0.08f)
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                                            )
                                            .then(
                                                if (isSelected) Modifier.border(1.5.dp, Red500.copy(0.3f), RoundedCornerShape(14.dp))
                                                else Modifier
                                            )
                                            .clickable {
                                                viewModel.changePlayerStyle(style)
                                                showPlayerStyleDialog = false
                                            }
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) Red500 else Color.Transparent)
                                                .then(
                                                    if (!isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape)
                                                    else Modifier
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        Spacer(Modifier.width(14.dp))
                                        Text(
                                            style.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            color = if (isSelected) Red500 else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showPlayerStyleDialog = false }) { Text("关闭") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.MusicNote,
                    title = "播放器样式",
                    subtitle = viewModel.playerStyle.displayName,
                    onClick = { showPlayerStyleDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 主题切换
                SettingsRow(
                    icon = if (viewModel.isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "界面主题",
                    subtitle = if (viewModel.isDarkMode) "深色模式" else "浅色模式",
                    onClick = { viewModel.toggleDarkMode() },
                )

                Spacer(Modifier.height(8.dp))

                // 悬浮歌词颜色
                var showLyricColorDialog by remember { mutableStateOf(false) }
                val lyricColorOptions = listOf(
                    "白色" to android.graphics.Color.WHITE,
                    "绿色" to android.graphics.Color.parseColor("#4CAF50"),
                    "青色" to android.graphics.Color.parseColor("#00BCD4"),
                    "粉色" to android.graphics.Color.parseColor("#FF4081"),
                    "黄色" to android.graphics.Color.parseColor("#FFEB3B"),
                    "橙色" to android.graphics.Color.parseColor("#FF9800"),
                    "紫色" to android.graphics.Color.parseColor("#AB47BC"),
                    "蓝色" to android.graphics.Color.parseColor("#2196F3"),
                    "红色" to android.graphics.Color.parseColor("#F44336"),
                )
                val currentColorName = lyricColorOptions.find { it.second == viewModel.floatingLyricsColor }?.first ?: "自定义"
                if (showLyricColorDialog) {
                    AlertDialog(
                        onDismissRequest = { showLyricColorDialog = false },
                        title = { Text("悬浮歌词颜色") },
                        text = {
                            Column {
                                lyricColorOptions.forEach { (name, color) ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                viewModel.changeFloatingLyricsColor(color)
                                                showLyricColorDialog = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(Modifier.size(20.dp).clip(CircleShape).background(Color(color)))
                                        Spacer(Modifier.width(12.dp))
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        if (color == viewModel.floatingLyricsColor) {
                                            Spacer(Modifier.weight(1f))
                                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showLyricColorDialog = false }) { Text("取消") }
                        }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.Code,
                    title = "悬浮歌词颜色",
                    subtitle = currentColorName,
                    onClick = { showLyricColorDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 歌词高亮颜色
                var showCurrentColorDialog by remember { mutableStateOf(false) }
                val lyricHighlightOptions = listOf(
                    "自动" to 0,
                    "绿色" to android.graphics.Color.parseColor("#22C55E"),
                    "青色" to android.graphics.Color.parseColor("#00BCD4"),
                    "粉色" to android.graphics.Color.parseColor("#FF4081"),
                    "黄色" to android.graphics.Color.parseColor("#FFEB3B"),
                    "橙色" to android.graphics.Color.parseColor("#FF9800"),
                    "紫色" to android.graphics.Color.parseColor("#AB47BC"),
                    "蓝色" to android.graphics.Color.parseColor("#2196F3"),
                    "红色" to android.graphics.Color.parseColor("#F44336"),
                    "白色" to android.graphics.Color.WHITE,
                )
                val curHighlightName = lyricHighlightOptions.find { it.second == viewModel.lyricCurrentColor }?.first ?: "自定义"
                if (showCurrentColorDialog) {
                    AlertDialog(
                        onDismissRequest = { showCurrentColorDialog = false },
                        title = { Text("歌词高亮颜色") },
                        text = {
                            Column {
                                lyricHighlightOptions.forEach { (name, color) ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.changeLyricCurrentColor(color)
                                                showCurrentColorDialog = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (color != 0) Box(Modifier.size(20.dp).clip(CircleShape).background(Color(color)))
                                        else Box(Modifier.size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF16A34A)))))
                                        Spacer(Modifier.width(12.dp))
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        if (color == viewModel.lyricCurrentColor) {
                                            Spacer(Modifier.weight(1f))
                                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showCurrentColorDialog = false }) { Text("取消") }
                        }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.LightMode,
                    title = "歌词高亮颜色",
                    subtitle = curHighlightName,
                    onClick = { showCurrentColorDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 歌词普通颜色
                var showNormalColorDialog by remember { mutableStateOf(false) }
                val lyricNormalOptions = listOf(
                    "自动" to 0,
                    "白色" to android.graphics.Color.WHITE,
                    "浅灰" to android.graphics.Color.parseColor("#BDBDBD"),
                    "深灰" to android.graphics.Color.parseColor("#616161"),
                    "黑色" to android.graphics.Color.parseColor("#2D2D2D"),
                    "米色" to android.graphics.Color.parseColor("#D7CCC8"),
                )
                val curNormalName = lyricNormalOptions.find { it.second == viewModel.lyricNormalColor }?.first ?: "自定义"
                if (showNormalColorDialog) {
                    AlertDialog(
                        onDismissRequest = { showNormalColorDialog = false },
                        title = { Text("歌词普通颜色") },
                        text = {
                            Column {
                                lyricNormalOptions.forEach { (name, color) ->
                                    Row(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.changeLyricNormalColor(color)
                                                showNormalColorDialog = false
                                            }
                                            .padding(vertical = 10.dp, horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (color != 0) Box(Modifier.size(20.dp).clip(CircleShape).background(Color(color)))
                                        else Box(Modifier.size(20.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Color.White, Color(0xFF2D2D2D)))))
                                        Spacer(Modifier.width(12.dp))
                                        Text(name, style = MaterialTheme.typography.bodyLarge)
                                        if (color == viewModel.lyricNormalColor) {
                                            Spacer(Modifier.weight(1f))
                                            Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showNormalColorDialog = false }) { Text("取消") }
                        }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.DarkMode,
                    title = "歌词普通颜色",
                    subtitle = curNormalName,
                    onClick = { showNormalColorDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 播放器歌词字体大小
                var showLyricFontSizeDialog by remember { mutableStateOf(false) }
                if (showLyricFontSizeDialog) {
                    var sliderSize by remember { mutableFloatStateOf(if (viewModel.lyricFontSize > 0) viewModel.lyricFontSize.toFloat() else 17f) }
                    AlertDialog(
                        onDismissRequest = { showLyricFontSizeDialog = false },
                        title = { Text("播放器歌词大小") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${sliderSize.toInt()} sp", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Slider(
                                    value = sliderSize,
                                    onValueChange = { sliderSize = it },
                                    valueRange = 12f..36f,
                                    steps = 23
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("默认: 17 sp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.changeLyricFontSize(sliderSize.toInt())
                                showLyricFontSizeDialog = false
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                viewModel.changeLyricFontSize(0)
                                showLyricFontSizeDialog = false
                            }) { Text("恢复默认") }
                        }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "播放器歌词大小",
                    subtitle = if (viewModel.lyricFontSize > 0) "${viewModel.lyricFontSize} sp" else "默认 (17 sp)",
                    onClick = { showLyricFontSizeDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 悬浮歌词字体大小
                var showLyricSizeDialog by remember { mutableStateOf(false) }
                if (showLyricSizeDialog) {
                    var sliderSize by remember { mutableFloatStateOf(viewModel.floatingLyricSize.toFloat()) }
                    AlertDialog(
                        onDismissRequest = { showLyricSizeDialog = false },
                        title = { Text("悬浮歌词大小") },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${sliderSize.toInt()} sp", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(12.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("10", style = MaterialTheme.typography.bodySmall)
                                    Slider(
                                        value = sliderSize,
                                        onValueChange = { sliderSize = it },
                                        valueRange = 10f..30f,
                                        steps = 19,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                    )
                                    Text("30", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.changeFloatingLyricSize(sliderSize.toInt())
                                showLyricSizeDialog = false
                            }) { Text("确定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showLyricSizeDialog = false }) { Text("取消") }
                        }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "悬浮歌词大小",
                    subtitle = "${viewModel.floatingLyricSize} sp",
                    onClick = { showLyricSizeDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // QQ Cookie设置
                var showQQCookieDialog by remember { mutableStateOf(false) }
                var qqCookieText by remember { mutableStateOf(viewModel.qqCookie) }
                
                if (showQQCookieDialog) {
                    AlertDialog(
                        onDismissRequest = { showQQCookieDialog = false },
                        title = { Text("QQ Cookie 设置") },
                        text = {
                            Column {
                                Text("设置Cookie后可获取QQ音乐完整版播放链接", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = qqCookieText,
                                    onValueChange = { qqCookieText = it },
                                    label = { Text("Cookie") },
                                    placeholder = { Text("粘贴QQ音乐的Cookie...") },
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    maxLines = 20
                                )
                                Spacer(Modifier.height(8.dp))
                                Text("获取方式：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("1. 浏览器打开 https://y.qq.com/ 并登录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("2. F12打开开发者工具 → Network标签", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("3. 刷新页面，点击任意请求", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("4. 右侧Headers → Request Headers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("5. 复制Cookie整串", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.setQQCookie(qqCookieText)
                                    showQQCookieDialog = false
                                }
                            ) { Text("保存", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showQQCookieDialog = false }) { Text("取消") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "QQ Cookie",
                    subtitle = if (viewModel.qqCookie.isNotEmpty()) "已设置" else "未设置（仅试听）",
                    onClick = { 
                        qqCookieText = viewModel.qqCookie
                        showQQCookieDialog = true 
                    },
                )

                Spacer(Modifier.height(8.dp))

                // 音乐API/LX插件
                var showApiHostDialog by remember { mutableStateOf(false) }
                var isTesting by remember { mutableStateOf(false) }
                var testResult by remember { mutableStateOf<String?>(null) }
                var testKeyword by remember { mutableStateOf("周杰伦") }
                var selectedSourceText by remember { mutableStateOf(viewModel.lxSelectedSource) }
                var testSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
                val scope = rememberCoroutineScope()
                val clipboardManager = LocalClipboardManager.current
                val pluginPicker = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        scope.launch {
                            val result = viewModel.importLxPlugin(uri)
                            if (result.isSuccess) {
                                selectedSourceText = viewModel.lxSelectedSource
                                viewModel.showToast("插件导入成功")
                            } else {
                                viewModel.showToast("导入失败: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }
                
                if (showApiHostDialog) {
                    AlertDialog(
                        onDismissRequest = { showApiHostDialog = false },
                        title = { Text("音乐API配置 / 落雪插件") },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Box(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("新手教程", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Red500)
                                        Spacer(Modifier.height(6.dp))
                                        Text("搜索可用，播放需插件。", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(4.dp))
                                        Text("步骤：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                                        Text("1. 选择落雪插件模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("2. 导入 .js 文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("3. 选择 source", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("4. 测试", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("5. 保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                
                                Text("API模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf("official" to "官方API", "lx_plugin" to "落雪插件").forEach { (mode, label) ->
                                        val sel = viewModel.apiMode == mode
                                        Row(
                                            Modifier.weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (sel) Red500.copy(0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                                .then(if (sel) Modifier.border(1.5.dp, Red500.copy(0.3f), RoundedCornerShape(12.dp)) else Modifier)
                                                .clickable { viewModel.updateApiMode(mode) }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                Modifier.size(18.dp).clip(CircleShape)
                                                    .background(if (sel) Red500 else Color.Transparent)
                                                    .then(if (!sel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape) else Modifier),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (sel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                            Spacer(Modifier.width(8.dp))
                                            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) Red500 else MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                
                                if (viewModel.apiMode == "lx_plugin") {
                                    Text("落雪插件模式：导入本地.js插件并选择source", style = MaterialTheme.typography.bodySmall, color = Red500)
                                } else {
                                    Text("官方API模式：走内置官方逻辑", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(12.dp))
                if (viewModel.apiMode == "lx_plugin") {
                                    // ── 在线导入对话框 ──
                                    var showOnlineImportDialog by remember { mutableStateOf(false) }
                                    var onlineUrl by remember { mutableStateOf("") }
                                    var isOnlineImporting by remember { mutableStateOf(false) }

                                    if (showOnlineImportDialog) {
                                        AlertDialog(
                                            onDismissRequest = { if (!isOnlineImporting) showOnlineImportDialog = false },
                                            title = { Text("在线导入JS插件") },
                                            text = {
                                                Column {
                                                    Text("输入JS插件的下载链接，将自动下载并导入。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(Modifier.height(8.dp))
                                                    OutlinedTextField(
                                                        value = onlineUrl,
                                                        onValueChange = { onlineUrl = it },
                                                        label = { Text("插件URL") },
                                                        placeholder = { Text("https://example.com/plugin.js") },
                                                        modifier = Modifier.fillMaxWidth(),
                                                        singleLine = false,
                                                        maxLines = 3,
                                                        enabled = !isOnlineImporting
                                                    )
                                                    if (isOnlineImporting) {
                                                        Spacer(Modifier.height(8.dp))
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            androidx.compose.material3.CircularProgressIndicator(
                                                                modifier = Modifier.size(16.dp),
                                                                strokeWidth = 2.dp
                                                            )
                                                            Spacer(Modifier.width(8.dp))
                                                            Text("正在下载并导入...", style = MaterialTheme.typography.bodySmall)
                                                        }
                                                    }
                                                }
                                            },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        if (onlineUrl.isBlank()) {
                                                            viewModel.showToast("请输入插件URL")
                                                            return@TextButton
                                                        }
                                                        isOnlineImporting = true
                                                        scope.launch {
                                                            val result = viewModel.importLxPluginFromUrl(onlineUrl.trim())
                                                            isOnlineImporting = false
                                                            if (result.isSuccess) {
                                                                selectedSourceText = viewModel.lxSelectedSource
                                                                viewModel.showToast("在线导入成功")
                                                                showOnlineImportDialog = false
                                                                onlineUrl = ""
                                                            } else {
                                                                viewModel.showToast("导入失败: ${result.exceptionOrNull()?.message}")
                                                            }
                                                        }
                                                    },
                                                    enabled = !isOnlineImporting
                                                ) { Text("导入", color = Red500) }
                                            },
                                            dismissButton = {
                                                TextButton(
                                                    onClick = { showOnlineImportDialog = false },
                                                    enabled = !isOnlineImporting
                                                ) { Text("取消") }
                                            }
                                        )
                                    }

                                    // ── 导入/移除所有 ──
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.Button(
                                            onClick = { pluginPicker.launch(arrayOf("text/javascript", "application/javascript", "text/plain")) },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("导入JS") }
                                        androidx.compose.material3.Button(
                                            onClick = { showOnlineImportDialog = true },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("在线导入") }
                                        androidx.compose.material3.OutlinedButton(
                                            onClick = { viewModel.removeLxPlugin() },
                                            modifier = Modifier.weight(1f)
                                        ) { Text("全部移除") }
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    // ── 已导入插件列表 ──
                                    if (viewModel.lxPlugins.isEmpty()) {
                                        Text("暂未导入插件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        Text("已导入 ${viewModel.lxPlugins.size} 个插件:", style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(4.dp))
                                        viewModel.lxPlugins.forEach { plugin ->
                                            val isSelected = viewModel.lxSelectedPluginId == plugin.id
                                            Row(
                                                Modifier.fillMaxWidth()
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) Red500.copy(0.08f)
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(0.3f)
                                                    )
                                                    .then(
                                                        if (isSelected) Modifier.border(1.dp, Red500.copy(0.3f), RoundedCornerShape(10.dp))
                                                        else Modifier
                                                    )
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            plugin.info.name.ifBlank { "未知插件" },
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSelected) Red500 else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (!plugin.initialized) {
                                                            Spacer(Modifier.width(6.dp))
                                                            Text(
                                                                "初始化失败",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color.White,
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(Color(0xFFF44336))
                                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        if (plugin.initialized)
                                                            "v${plugin.info.version} · ${plugin.info.author} · source: ${plugin.sources.joinToString(",").ifEmpty { "无" }}"
                                                        else
                                                            "v${plugin.info.version} · 插件未注册Handler，可能缺少JS环境支持",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = if (!plugin.initialized) Color(0xFFF44336).copy(0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2, overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                // 删除此插件
                                                IconButton(
                                                    onClick = { viewModel.removeLxPluginById(plugin.id) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    // ── Source 选择 ──
                                    if (viewModel.lxSources.isNotEmpty()) {
                                        Text("选择 Source:", style = MaterialTheme.typography.labelMedium)
                                        Spacer(Modifier.height(4.dp))
                                        Row(
                                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // "全部" option: search all sources
                                            val isAllSel = viewModel.lxSelectedSource == MusicViewModel.LX_SOURCE_ALL
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isAllSel) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant)
                                                    .clickable {
                                                        viewModel.updateLxSelection(viewModel.lxSelectedPluginId, MusicViewModel.LX_SOURCE_ALL)
                                                        selectedSourceText = MusicViewModel.LX_SOURCE_ALL
                                                    }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    "全部",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isAllSel) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            viewModel.lxPlugins.forEach { plugin ->
                                                plugin.sources.forEach { src ->
                                                    val isSel = viewModel.lxSelectedPluginId == plugin.id && viewModel.lxSelectedSource == src
                                                    Box(
                                                        Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSel) Red500 else MaterialTheme.colorScheme.surfaceVariant)
                                                            .clickable {
                                                                viewModel.updateLxSelection(plugin.id, src)
                                                                selectedSourceText = src
                                                            }
                                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Text(
                                                            src,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = selectedSourceText,
                                            onValueChange = {
                                                selectedSourceText = it
                                                viewModel.updateLxSelectedSource(it)
                                            },
                                            label = { Text("当前source") },
                                            placeholder = { Text("如 kw/kg/tx/wy/mg") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))

                                    // ── 测试区域 ──
                                    OutlinedTextField(
                                        value = testKeyword,
                                        onValueChange = { testKeyword = it },
                                        label = { Text("测试搜索关键字") },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                scope.launch {
                                                    isTesting = true
                                                    val r = viewModel.testLxSearch(testKeyword)
                                                    isTesting = false
                                                    if (r.isSuccess) {
                                                        testSongs = r.getOrNull().orEmpty()
                                                        testResult = "search成功: ${testSongs.size} 首"
                                                    } else {
                                                        testResult = "search失败: ${r.exceptionOrNull()?.message}"
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isTesting
                                        ) { Text("测试Search") }
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                val first = testSongs.firstOrNull()
                                                if (first == null) {
                                                    testResult = "请先搜索并选择歌曲"
                                                } else {
                                                    scope.launch {
                                                        isTesting = true
                                                        val r = viewModel.testLxMusicUrl(first)
                                                        isTesting = false
                                                        testResult = if (r.isSuccess) "play成功: ${r.getOrNull()}" else "play失败: ${r.exceptionOrNull()?.message}"
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isTesting
                                        ) { Text("测试Play") }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                }
                                
                                testResult?.let { result ->
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        result,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (result.contains("成功") || result.startsWith("✓")) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    )
                                }

                                // Debug log viewer
                                if (viewModel.apiMode == "lx_plugin" && viewModel.lxDebugLogs.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Runtime Log", style = MaterialTheme.typography.labelMedium)
                                        Row {
                                            TextButton(onClick = {
                                                clipboardManager.setText(AnnotatedString(viewModel.lxDebugLogs.joinToString("\n")))
                                                viewModel.showToast("已复制日志")
                                            }) {
                                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                                            }
                                            TextButton(onClick = { viewModel.clearLxLogs() }) {
                                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 200.dp)
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .padding(8.dp)
                                    ) {
                                        val scrollState = rememberScrollState()
                                        LaunchedEffect(viewModel.lxDebugLogs.size) {
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                        Column(Modifier.verticalScroll(scrollState)) {
                                            viewModel.lxDebugLogs.forEach { line ->
                                                Text(
                                                    line,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        fontSize = 10.sp,
                                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                                    ),
                                                    color = if (line.contains("ERROR") || line.contains("FAILED") || line.contains("WARNING"))
                                                        Color(0xFFF44336)
                                                    else if (line.contains("REQUEST HANDLER SET") || line.contains("done"))
                                                        Color(0xFF4CAF50)
                                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (viewModel.apiMode == "lx_plugin" && selectedSourceText.isNotBlank()) {
                                        viewModel.updateLxSelectedSource(selectedSourceText)
                                    }
                                    showApiHostDialog = false
                                    viewModel.showToast("配置已保存")
                                }
                            ) { Text("保存", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showApiHostDialog = false }) { Text("取消") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.Settings,
                    title = "音乐API配置",
                    subtitle = if (viewModel.apiMode == "lx_plugin") {
                        if (viewModel.lxSelectedSource == MusicViewModel.LX_SOURCE_ALL) "落雪插件 · 全部音源"
                        else "落雪插件 · ${viewModel.lxSelectedSource.ifBlank { "未选择" }}"
                    } else "官方API模式",
                    onClick = { 
                        selectedSourceText = viewModel.lxSelectedSource
                        showApiHostDialog = true 
                    },
                )

                Spacer(Modifier.height(8.dp))

                // 插件管理入口
                SettingsRow(
                    icon = Icons.Default.List,
                    title = "插件管理",
                    subtitle = if (viewModel.lxPlugins.isEmpty()) "未导入插件"
                        else "已加载 ${viewModel.lxPlugins.size} 个插件 · ${viewModel.lxPlugins.count { viewModel.isPluginEnabled(it.id) }} 个启用",
                    onClick = { showPluginManager = true },
                )

                Spacer(Modifier.height(8.dp))

                // 导入歌单（简化版）
                var showImportPlaylistDialog by remember { mutableStateOf(false) }
                var importPlaylistText by remember { mutableStateOf("") }
                var showExportAllPlaylistsDialog by remember { mutableStateOf(false) }
                var exportAllPlaylistsText by remember { mutableStateOf("") }

                if (showImportPlaylistDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportPlaylistDialog = false },
                        title = { Text("导入歌单") },
                        text = {
                            Column {
                                Text("粘贴复制的歌单内容：", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importPlaylistText,
                                    onValueChange = { importPlaylistText = it },
                                    label = { Text("歌单名称和歌曲列表") },
                                    modifier = Modifier.fillMaxWidth().height(300.dp),
                                    maxLines = 30
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (importPlaylistText.isNotBlank()) {
                                        viewModel.importPlaylistFromText(importPlaylistText)
                                        showImportPlaylistDialog = false
                                        importPlaylistText = ""
                                    }
                                }
                            ) { Text("导入", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showImportPlaylistDialog = false }) { Text("取消") } }
                    )
                }

                if (showExportAllPlaylistsDialog) {
                    AlertDialog(
                        onDismissRequest = { showExportAllPlaylistsDialog = false },
                        title = { Text("导出全部歌单") },
                        text = {
                            Column {
                                Text("已生成全部歌单内容，点击复制：", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(260.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        exportAllPlaylistsText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (exportAllPlaylistsText.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(exportAllPlaylistsText))
                                        viewModel.showToast("已复制到剪贴板")
                                    }
                                    showExportAllPlaylistsDialog = false
                                }
                            ) { Text("复制", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showExportAllPlaylistsDialog = false }) { Text("关闭") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.Upload,
                    title = "导出全部歌单",
                    subtitle = "一键复制全部歌单内容",
                    onClick = {
                        exportAllPlaylistsText = viewModel.exportAllPlaylistsAsText()
                        if (exportAllPlaylistsText.isBlank()) {
                            viewModel.showToast("没有歌单可导出")
                        } else {
                            showExportAllPlaylistsDialog = true
                        }
                    },
                )

                Spacer(Modifier.height(8.dp))

                SettingsRow(
                    icon = Icons.Default.Download,
                    title = "导入歌单",
                    subtitle = "粘贴歌单内容导入",
                    onClick = { showImportPlaylistDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // 收藏导出/导入
                var showExportFavoritesDialog by remember { mutableStateOf(false) }
                var showImportFavoritesDialog by remember { mutableStateOf(false) }
                var exportFavoritesText by remember { mutableStateOf("") }
                var importFavoritesText by remember { mutableStateOf("") }

                // 导出收藏对话框
                if (showExportFavoritesDialog) {
                    AlertDialog(
                        onDismissRequest = { showExportFavoritesDialog = false },
                        title = { Text("导出收藏") },
                        text = {
                            Column {
                                Text("已生成收藏列表，点击复制：", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier.fillMaxWidth().height(260.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                        .verticalScroll(rememberScrollState())
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        exportFavoritesText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (exportFavoritesText.isNotBlank()) {
                                        clipboardManager.setText(AnnotatedString(exportFavoritesText))
                                        viewModel.showToast("已复制到剪贴板")
                                    }
                                    showExportFavoritesDialog = false
                                }
                            ) { Text("复制", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showExportFavoritesDialog = false }) { Text("关闭") } }
                    )
                }

                // 导入收藏对话框
                if (showImportFavoritesDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportFavoritesDialog = false },
                        title = { Text("导入收藏") },
                        text = {
                            Column {
                                Text("粘贴收藏列表内容：", style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = importFavoritesText,
                                    onValueChange = { importFavoritesText = it },
                                    label = { Text("粘贴收藏内容") },
                                    modifier = Modifier.fillMaxWidth().height(300.dp),
                                    maxLines = 30
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (importFavoritesText.isNotBlank()) {
                                        viewModel.importFavoritesFromText(importFavoritesText)
                                        showImportFavoritesDialog = false
                                        importFavoritesText = ""
                                    }
                                }
                            ) { Text("导入", color = Red500) }
                        },
                        dismissButton = { TextButton(onClick = { showImportFavoritesDialog = false }) { Text("取消") } }
                    )
                }

                SettingsRow(
                    icon = Icons.Default.Favorite,
                    title = "导出收藏",
                    subtitle = "一键复制全部收藏内容",
                    onClick = {
                        exportFavoritesText = viewModel.exportFavoritesAsText()
                        if (exportFavoritesText.isBlank()) {
                            viewModel.showToast("没有收藏可导出")
                        } else {
                            showExportFavoritesDialog = true
                        }
                    },
                )

                Spacer(Modifier.height(8.dp))

                SettingsRow(
                    icon = Icons.Default.FavoriteBorder,
                    title = "导入收藏",
                    subtitle = "粘贴收藏内容导入",
                    onClick = { showImportFavoritesDialog = true },
                )

                Spacer(Modifier.height(12.dp))

                // ══ 音乐缓存管理 ══
                var showCacheDialog by remember { mutableStateOf(false) }
                if (showCacheDialog) {
                    AlertDialog(
                        onDismissRequest = { showCacheDialog = false },
                        title = { Text("音乐缓存管理") },
                        text = {
                            Column {
                                val cacheSize = viewModel.getCacheSize()
                                Text("当前缓存大小: $cacheSize", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                Text("缓存包括:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• 歌曲音频文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• 专辑封面图片", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("• 歌词文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            viewModel.clearCache()
                                            viewModel.showToast("缓存已清理")
                                            showCacheDialog = false
                                        }
                                    ) { Text("清理缓存") }
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showCacheDialog = false }) { Text("关闭") } }
                    )
                }
                SettingsRow(
                    icon = Icons.Default.Delete,
                    title = "音乐缓存管理",
                    subtitle = "查看和清理本地缓存",
                    onClick = { showCacheDialog = true },
                )

                Spacer(Modifier.height(8.dp))

                // ══ 开发者模式 (直接显示) ══
                SettingsRow(
                    icon = Icons.Default.Code,
                    title = "开发者模式",
                    subtitle = if (viewModel.isDevMode) "已开启" else "查看崩溃日志等调试信息",
                    onClick = {
                        viewModel.toggleDevMode()
                        viewModel.showToast(if (viewModel.isDevMode) "开发者模式已开启" else "开发者模式已关闭")
                    },
                )

                // 开发者模式内容
                if (viewModel.isDevMode) {
                    Spacer(Modifier.height(8.dp))

                    // 崩溃日志
                    val logCount = viewModel.getCrashLogCount()
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.08f))
                            .clickable { showCrashLogs = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.BugReport, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("崩溃日志", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                if (logCount > 0) "$logCount 条记录 — 点击查看详情" else "暂无崩溃记录 ✓",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (logCount > 0) Red500 else Color(0xFF4CAF50),
                            )
                        }
                        if (logCount > 0) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape).background(Red500),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("$logCount", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 免责声明
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "免责声明",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "• 本应用音乐资源来自互联网公开接口，不存储任何音频文件\n" +
                            "• 仅供个人学习试听，请支持正版音乐\n" +
                            "• 版权归原作者及平台所有，侵权请联系删除\n" +
                            "• 使用本应用产生的法律责任由用户自行承担",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

// ═══ API配置对话框 ═══

@Composable
private fun ApiConfigDialog(viewModel: MusicViewModel, onDismiss: () -> Unit, onOpenPluginManager: () -> Unit) {
    var selectedSourceText by remember { mutableStateOf(viewModel.lxSelectedSource) }
    var isTesting by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testKeyword by remember { mutableStateOf("周杰伦") }
    var testSongs by remember { mutableStateOf<List<Song>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("音乐API配置 / 落雪插件") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 新手教程
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("新手教程", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Red500)
                        Spacer(Modifier.height(6.dp))
                        Text("搜索可用，播放需插件。", style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text("步骤：", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                        Text("1. 选择落雪插件模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("2. 导入 .js 文件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("3. 选择 source", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("4. 测试", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("5. 保存", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(12.dp))

                // API模式选择
                Text("API模式", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    listOf("official" to "官方API", "lx_plugin" to "落雪插件").forEach { (mode, label) ->
                        val sel = viewModel.apiMode == mode
                        Row(
                            Modifier.weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) Red500.copy(0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                                .then(if (sel) Modifier.border(1.5.dp, Red500.copy(0.3f), RoundedCornerShape(12.dp)) else Modifier)
                                .clickable { viewModel.updateApiMode(mode) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(18.dp).clip(CircleShape)
                                    .background(if (sel) Red500 else Color.Transparent)
                                    .then(if (!sel) Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), CircleShape) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (sel) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodySmall, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, color = if (sel) Red500 else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                if (viewModel.apiMode == "lx_plugin") {
                    Text("落雪插件模式：导入本地.js插件并选择source", style = MaterialTheme.typography.bodySmall, color = Red500)
                } else {
                    Text("官方API模式：走内置官方逻辑", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(12.dp))

                if (viewModel.apiMode == "lx_plugin") {
                    // 插件管理快捷入口
                    androidx.compose.material3.OutlinedButton(
                        onClick = onOpenPluginManager,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.List, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (viewModel.lxPlugins.isEmpty()) "管理插件 · 未导入"
                            else "管理插件 · ${viewModel.lxPlugins.size} 个插件 · ${viewModel.lxPlugins.count { viewModel.isPluginEnabled(it.id) }} 个启用"
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Source 选择
                    if (viewModel.lxSources.isNotEmpty()) {
                        Text("选择 Source:", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isAllSel = viewModel.lxSelectedSource == MusicViewModel.LX_SOURCE_ALL
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllSel) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        viewModel.updateLxSelection(viewModel.lxSelectedPluginId, MusicViewModel.LX_SOURCE_ALL)
                                        selectedSourceText = MusicViewModel.LX_SOURCE_ALL
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "全部",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isAllSel) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            viewModel.lxPlugins.forEach { plugin ->
                                plugin.sources.forEach { src ->
                                    val isSel = viewModel.lxSelectedPluginId == plugin.id && viewModel.lxSelectedSource == src
                                    Box(
                                        Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) Red500 else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                viewModel.updateLxSelection(plugin.id, src)
                                                selectedSourceText = src
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            src,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = selectedSourceText,
                            onValueChange = {
                                selectedSourceText = it
                                viewModel.updateLxSelectedSource(it)
                            },
                            label = { Text("当前source") },
                            placeholder = { Text("如 kw/kg/tx/wy/mg") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // 测试区域
                    OutlinedTextField(
                        value = testKeyword,
                        onValueChange = { testKeyword = it },
                        label = { Text("测试搜索关键字") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Button(
                            onClick = {
                                scope.launch {
                                    isTesting = true
                                    val r = viewModel.testLxSearch(testKeyword)
                                    isTesting = false
                                    if (r.isSuccess) {
                                        testSongs = r.getOrNull().orEmpty()
                                        testResult = "search成功: ${testSongs.size} 首"
                                    } else {
                                        testResult = "search失败: ${r.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting
                        ) { Text("测试Search") }
                        androidx.compose.material3.Button(
                            onClick = {
                                val first = testSongs.firstOrNull()
                                if (first == null) {
                                    testResult = "请先搜索并选择歌曲"
                                } else {
                                    scope.launch {
                                        isTesting = true
                                        val r = viewModel.testLxMusicUrl(first)
                                        isTesting = false
                                        testResult = if (r.isSuccess) "play成功: ${r.getOrNull()}" else "play失败: ${r.exceptionOrNull()?.message}"
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isTesting
                        ) { Text("测试Play") }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                testResult?.let { result ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (result.contains("成功") || result.startsWith("✓")) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }

                // Debug log viewer
                if (viewModel.apiMode == "lx_plugin" && viewModel.lxDebugLogs.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Runtime Log", style = MaterialTheme.typography.labelMedium)
                        Row {
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(viewModel.lxDebugLogs.joinToString("\n")))
                                viewModel.showToast("已复制日志")
                            }) {
                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.clearLxLogs() }) {
                                Text("Clear", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(8.dp)
                    ) {
                        val scrollState = rememberScrollState()
                        LaunchedEffect(viewModel.lxDebugLogs.size) {
                            scrollState.animateScrollTo(scrollState.maxValue)
                        }
                        Column(Modifier.verticalScroll(scrollState)) {
                            viewModel.lxDebugLogs.forEach { line ->
                                Text(
                                    line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    ),
                                    color = if (line.contains("ERROR") || line.contains("FAILED") || line.contains("WARNING"))
                                        Color(0xFFF44336)
                                    else if (line.contains("REQUEST HANDLER SET") || line.contains("done"))
                                        Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (viewModel.apiMode == "lx_plugin" && selectedSourceText.isNotBlank()) {
                        viewModel.updateLxSelectedSource(selectedSourceText)
                    }
                    onDismiss()
                    viewModel.showToast("配置已保存")
                }
            ) { Text("保存", color = Red500) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

// ═══ 插件管理 ═══

@Composable
private fun PluginManagerDialog(viewModel: MusicViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val pluginPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = viewModel.importLxPlugin(uri)
                if (result.isSuccess) viewModel.showToast("插件导入成功")
                else viewModel.showToast("导入失败: ${result.exceptionOrNull()?.message}")
            }
        }
    }
    var showOnlineImport by remember { mutableStateOf(false) }
    var onlineUrl by remember { mutableStateOf("") }
    var isOnlineImporting by remember { mutableStateOf(false) }
    // 展开的插件 ID
    var expandedPluginId by remember { mutableStateOf<String?>(null) }

    // 在线导入对话框
    if (showOnlineImport) {
        AlertDialog(
            onDismissRequest = { if (!isOnlineImporting) showOnlineImport = false },
            title = { Text("在线导入JS插件") },
            text = {
                Column {
                    Text("输入JS插件的下载链接，将自动下载并导入。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = onlineUrl,
                        onValueChange = { onlineUrl = it },
                        label = { Text("插件URL") },
                        placeholder = { Text("https://example.com/plugin.js") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        enabled = !isOnlineImporting
                    )
                    if (isOnlineImporting) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("正在下载并导入...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (onlineUrl.isBlank()) { viewModel.showToast("请输入插件URL"); return@TextButton }
                        isOnlineImporting = true
                        scope.launch {
                            val result = viewModel.importLxPluginFromUrl(onlineUrl.trim())
                            isOnlineImporting = false
                            if (result.isSuccess) {
                                viewModel.showToast("在线导入成功")
                                showOnlineImport = false; onlineUrl = ""
                            } else {
                                viewModel.showToast("导入失败: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    },
                    enabled = !isOnlineImporting
                ) { Text("导入", color = Red500) }
            },
            dismissButton = { TextButton(onClick = { showOnlineImport = false }, enabled = !isOnlineImporting) { Text("取消") } }
        )
    }

    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.List, null, tint = Red500, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("插件管理", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 操作按钮行
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    androidx.compose.material3.Button(
                        onClick = { pluginPicker.launch(arrayOf("text/javascript", "application/javascript", "text/plain")) },
                        modifier = Modifier.weight(1f)
                    ) { Text("导入JS", fontSize = 12.sp, maxLines = 1) }
                    androidx.compose.material3.Button(
                        onClick = { showOnlineImport = true },
                        modifier = Modifier.weight(1f)
                    ) { Text("在线导入", fontSize = 12.sp, maxLines = 1) }
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.removeLxPlugin() },
                        modifier = Modifier.weight(1f)
                    ) { Text("全部移除", fontSize = 12.sp, maxLines = 1) }
                }
                Spacer(Modifier.height(12.dp))

                if (viewModel.lxPlugins.isEmpty()) {
                    Box(
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.List, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(40.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("暂无插件", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("点击上方按钮导入JS插件", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                        }
                    }
                } else {
                    Text("已加载 ${viewModel.lxPlugins.size} 个插件", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))

                    viewModel.lxPlugins.forEach { plugin ->
                        val enabled = viewModel.isPluginEnabled(plugin.id)
                        val isExpanded = expandedPluginId == plugin.id

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (enabled) MaterialTheme.colorScheme.surfaceVariant.copy(0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(0.2f)
                                )
                                .then(
                                    if (enabled) Modifier.border(1.dp, Red500.copy(0.2f), RoundedCornerShape(12.dp))
                                    else Modifier
                                )
                                .padding(12.dp)
                        ) {
                            // 插件头部: 名称 + Switch + 删除
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            plugin.info.name.ifBlank { "未知插件" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                        )
                                        if (!plugin.initialized) {
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                "初始化失败",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFF44336))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "v${plugin.info.version} · ${plugin.info.author}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(if (enabled) 1f else 0.4f)
                                    )
                                }
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { viewModel.togglePluginEnabled(plugin.id) },
                                    colors = SwitchDefaults.colors(checkedTrackColor = Red500),
                                    modifier = Modifier.height(24.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                IconButton(
                                    onClick = { viewModel.removeLxPluginById(plugin.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(16.dp))
                                }
                            }

                            // 音源信息 + 展开按钮
                            if (plugin.sources.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { expandedPluginId = if (isExpanded) null else plugin.id }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val enabledCount = plugin.sources.count { viewModel.isSourceEnabled(plugin.id, it) }
                                    Text(
                                        "$enabledCount / ${plugin.sources.size} 个音源启用",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (enabled) Red500.copy(0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                                    )
                                    Spacer(Modifier.weight(1f))
                                    Icon(
                                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "展开",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // 展开的音源列表
                            if (isExpanded && plugin.sources.isNotEmpty()) {
                                Spacer(Modifier.height(4.dp))
                                plugin.sources.forEach { src ->
                                    val srcEnabled = viewModel.isSourceEnabled(plugin.id, src)
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (srcEnabled) Red500.copy(0.06f)
                                                else Color.Transparent
                                            )
                                            .clickable { viewModel.toggleSourceEnabled(plugin.id, src) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            src,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = if (srcEnabled) FontWeight.Medium else FontWeight.Normal,
                                            color = if (srcEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.4f)
                                        )
                                        Spacer(Modifier.weight(1f))
                                        Switch(
                                            checked = srcEnabled,
                                            onCheckedChange = { viewModel.toggleSourceEnabled(plugin.id, src) },
                                            colors = SwitchDefaults.colors(checkedTrackColor = Red500),
                                            modifier = Modifier.height(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // 提示信息
                if (viewModel.lxPlugins.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
                            .padding(10.dp)
                    ) {
                        Text(
                            "提示：启用多个插件后，选择“全部”音源即可跨插件搜索和播放。\n禁用的插件或音源将不会参与搜索和播放。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onBack) { Text("完成", color = Red500) } },
    )
}

// ═══ 崩溃日志列表 ═══
@Composable
private fun CrashLogListDialog(viewModel: MusicViewModel, onBack: () -> Unit, onDetail: (CrashLogEntry) -> Unit) {
    val logs = remember { viewModel.fetchCrashLogs() }
    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, null, tint = Red500, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("崩溃日志", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                if (logs.isNotEmpty()) {
                    IconButton(onClick = { viewModel.clearCrashLogs(); onBack() }, Modifier.size(32.dp)) {
                        Icon(Icons.Default.DeleteSweep, "清空", tint = Red500, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        text = {
            if (logs.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.BugReport, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("暂无崩溃记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("运行稳定", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxWidth().height(360.dp)) {
                    items(logs) { entry ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .clickable { onDetail(entry) }
                                .padding(horizontal = 4.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // 崩溃/错误图标
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .background(if (entry.isCrash) Red500.copy(0.1f) else Color(0xFFFF9800).copy(0.1f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (entry.isCrash) "💥" else "⚠️",
                                    fontSize = 16.sp,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (entry.isCrash) "崩溃" else "异常",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (entry.isCrash) Red500 else Color(0xFFFF9800),
                                )
                                Text(
                                    entry.time,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "${entry.sizeKB} KB",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                            )
                            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onBack) { Text("返回") } },
    )
}

// ═══ 崩溃日志详情 ═══
@Composable
private fun CrashLogDetailDialog(entry: CrashLogEntry, onBack: () -> Unit) {
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    AlertDialog(
        onDismissRequest = onBack,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (entry.isCrash) "💥" else "⚠️", fontSize = 18.sp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (entry.isCrash) "崩溃详情" else "异常详情",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                    Text(entry.time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 复制按钮
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(entry.content))
                        android.widget.Toast.makeText(context, "已复制到剪贴板", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    Modifier.size(36.dp),
                ) {
                    Icon(Icons.Default.ContentCopy, "复制", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        },
        text = {
            Box(
                Modifier.fillMaxWidth().height(400.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp),
            ) {
                Text(
                    entry.content,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        lineHeight = 15.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = { TextButton(onClick = onBack) { Text("返回") } },
    )
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, accent: Boolean = false, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .background(if (accent) Red500.copy(alpha = 0.08f) else cs.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (accent) Red500 else cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
        Icon(Icons.Default.ChevronRight, null, tint = cs.onSurfaceVariant.copy(0.4f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CreatePlaylistDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("创建歌单") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("歌单名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        },
        confirmButton = { TextButton(onClick = { viewModel.createPlaylist(name); onDismiss() }) { Text("创建", color = Red500) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ThemePickerDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedThemeId by remember {
        mutableStateOf(ThemeManager.getSavedThemeId(context))
    }

    LaunchedEffect(Unit) {
        val theme = ThemeManager.getThemeFlowSafe(context)
        if (theme != null) {
            selectedThemeId = theme.id
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主题设置") },
        text = {
            LazyColumn(
                modifier = Modifier.height(400.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AppThemes.size) { index ->
                    val theme = AppThemes[index]
                    val isSelected = selectedThemeId == theme.id

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) theme.primaryColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable {
                                selectedThemeId = theme.id
                                kotlinx.coroutines.GlobalScope.launch {
                                    ThemeManager.saveThemeId(context, theme.id)
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(theme.accentGradientStart, theme.accentGradientEnd)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                theme.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                theme.nameEn,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(theme.primaryColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(theme.secondaryColor)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(theme.backgroundColor)
                                )
                            }
                        }

                        if (isSelected) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "已选择",
                                tint = theme.primaryColor,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}
