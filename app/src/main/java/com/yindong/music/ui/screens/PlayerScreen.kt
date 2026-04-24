package com.yindong.music.ui.screens

import android.graphics.Color as AndroidColor
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.luminance
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.List
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material3.Switch
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.yindong.music.data.LocalStorage
import com.yindong.music.data.api.MusicApiConfig
import com.yindong.music.data.model.LyricLine
import com.yindong.music.data.model.Playlist
import com.yindong.music.data.model.Song
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.core.view.WindowCompat
import com.yindong.music.ui.theme.NebulaPink
import com.yindong.music.ui.theme.NebulaViolet
import com.yindong.music.viewmodel.MusicViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.palette.graphics.Palette
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onOpenAiAudioEffect: () -> Unit = {},
) {
    val song = viewModel.currentSong
    if (song == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1A1A1A))
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = NebulaViolet)
                Spacer(modifier = Modifier.height(16.dp))
                Text("加载中...", color = Color.White.copy(alpha = 0.7f))
            }
        }
        return
    }
    var showLyrics by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(0f) }
    var showQMenu by remember { mutableStateOf(false) }
    val showBottomSheet = remember { mutableStateOf(false) }
    val showNewPlaylist = remember { mutableStateOf(false) }
    val showQueue = remember { mutableStateOf(false) }
    val showTimerSheet = remember { mutableStateOf(false) }
    val showEqualizer = remember { mutableStateOf(false) }
    val showLyricsSheet = remember { mutableStateOf(false) }
    val showAddToPlaylist = remember { mutableStateOf(false) }
    val showDownloadQuality = remember { mutableStateOf(false) }
    LaunchedEffect(showEqualizer.value) {
        if (showEqualizer.value) {
            showEqualizer.value = false
            onOpenAiAudioEffect()
        }
    }

    // ── 下载音质选择弹窗 ──
    if (showDownloadQuality.value && song != null && viewModel.apiMode == "lx_plugin") {
        val qualities = remember(song.pluginRawJson) { viewModel.getAvailableQualities(song) }
        AlertDialog(
            onDismissRequest = { showDownloadQuality.value = false },
            title = { Text("选择下载音质", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("${song.title} - ${song.artist}", style = MaterialTheme.typography.bodySmall, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(12.dp))
                    qualities.forEach { (key, displayName) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    showDownloadQuality.value = false
                                    viewModel.downloadSong(song, key)
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isLossless = key.contains("flac", true) || key.contains("wav", true) || key.contains("master", true)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isLossless) Color(0xFF4CAF50).copy(0.15f) else NebulaViolet.copy(0.1f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isLossless) Color(0xFF4CAF50) else NebulaViolet
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.Download, null, tint = Color.Gray.copy(0.5f), modifier = Modifier.size(18.dp))
                        }
                    }
                    if (qualities.isEmpty()) {
                        Text("无可用音质", color = Color.Gray)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDownloadQuality.value = false }) { Text("取消") }
            }
        )
    }

    LaunchedEffect(viewModel.isPlaying) { while (viewModel.isPlaying) { rotation += 0.3f; if (rotation >= 360f) rotation -= 360f; delay(16L) } }
    LaunchedEffect(viewModel.isPlaying, viewModel.lyrics) { if (viewModel.isPlaying || viewModel.lyrics.isNotEmpty()) showLyrics = true }

    // ── 添加到歌单弹窗 ──
    if (showAddToPlaylist.value) {
        ModalBottomSheet(
            onDismissRequest = { showAddToPlaylist.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("添加到歌单", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                if (viewModel.userPlaylists.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text("暂无歌单，请先创建歌单", color = Color.White.copy(0.6f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(viewModel.userPlaylists, key = { it.id }) { playlist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addSongToPlaylist(playlist.id, song)
                                        showAddToPlaylist.value = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 歌单封面
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF333333)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (playlist.coverUrl.isNotEmpty()) {
                                        AsyncImage(
                                            playlist.coverUrl,
                                            null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(24.dp))
                                    }
                                }
                                
                                Spacer(Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        playlist.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${playlist.songCount}首",
                                        color = Color.White.copy(0.5f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                
                                // 检查歌曲是否已在歌单中
                                val isInPlaylist = playlist.songs.any { it.platformId == song.platformId && it.platform == song.platform }
                                if (isInPlaylist) {
                                    Text("已添加", color = NebulaViolet, style = MaterialTheme.typography.bodySmall)
                                } else {
                                    Icon(Icons.Default.Add, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 创建新歌单按钮
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF333333))
                        .clickable {
                            showAddToPlaylist.value = false
                            showNewPlaylist.value = true
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("创建新歌单", color = Color.White, fontWeight = FontWeight.Medium)
                }
                
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    // ── 播放队列弹窗 ──
    if (showQueue.value) {
        var selectedTab by remember { mutableStateOf("当前播放") }
        ModalBottomSheet(
            onDismissRequest = { showQueue.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF1E1E1E),
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text("播放队列", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                
                // 标签切换
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("当前播放", "历史播放").forEach { tab ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selectedTab == tab) NebulaViolet else Color(0xFF333333))
                                .clickable { selectedTab = tab }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(tab, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }
                
                // 操作按钮（仅在当前播放标签显示）
                if (selectedTab == "当前播放") {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF333333))
                                .clickable { 
                                    val queue = viewModel.getQueue()
                                    if (queue.isNotEmpty()) {
                                        queue.forEach { song ->
                                            viewModel.toggleLike(song)
                                        }
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("添加到收藏", color = Color.White.copy(0.9f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF333333))
                                .clickable { 
                                    val queue = viewModel.getQueue()
                                    if (queue.isNotEmpty()) {
                                        showAddToPlaylist.value = true
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("添加到歌单", color = Color.White.copy(0.9f), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                // 内容区域 - 使用 LazyColumn 显示所有歌曲
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (selectedTab == "当前播放") {
                        val queue = viewModel.getQueue()
                        if (queue.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("播放队列为空", color = Color.White.copy(0.7f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(queue) { index, songItem ->
                                    val isCurrent = viewModel.currentSong?.platformId == songItem.platformId && 
                                                   viewModel.currentSong?.platform == songItem.platform
                                    
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isCurrent) Color(0xFF333333) else Color.Transparent)
                                            .clickable {
                                                viewModel.playSong(songItem)
                                                showQueue.value = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 序号
                                        Text(
                                            "${index + 1}",
                                            modifier = Modifier.width(36.dp),
                                            color = if (isCurrent) NebulaViolet else Color.White.copy(0.5f),
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp
                                        )
                                        
                                        // 歌曲信息
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                songItem.title,
                                                color = if (isCurrent) Color.White else Color.White.copy(0.85f),
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                songItem.artist,
                                                color = Color.White.copy(0.5f),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        // 删除按钮
                                        IconButton(
                                            onClick = { viewModel.removeFromQueue(songItem) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                
                                // 清空按钮
                                item {
                                    Spacer(Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFF333333))
                                            .clickable { viewModel.clearQueue() }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("清空队列", color = Color.White.copy(0.7f), fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }
                    } else {
                        // 历史播放
                        val history = viewModel.playHistory
                        if (history.isEmpty()) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂无历史播放", color = Color.White.copy(0.7f))
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(history) { index, songItem ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable {
                                                viewModel.playSong(songItem)
                                                showQueue.value = false
                                            }
                                            .padding(horizontal = 12.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 序号
                                        Text(
                                            "${index + 1}",
                                            modifier = Modifier.width(36.dp),
                                            color = Color.White.copy(0.5f),
                                            fontSize = 16.sp
                                        )
                                        
                                        // 歌曲信息
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.Start
                                        ) {
                                            Text(
                                                songItem.title,
                                                color = Color.White.copy(0.85f),
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                songItem.artist,
                                                color = Color.White.copy(0.5f),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        // 删除按钮
                                        IconButton(
                                            onClick = { viewModel.removeFromHistory(songItem) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, null, tint = Color.White.copy(0.4f), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                                
                                item {
                                    Spacer(Modifier.height(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // ── 创建歌单对话框 ──
    if (showNewPlaylist.value) {
        var playlistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewPlaylist.value = false },
            title = { Text("创建歌单", color = Color.White) },
            text = {
                OutlinedTextField(
                    value = playlistName,
                    onValueChange = { playlistName = it },
                    label = { Text("歌单名称", color = Color.White.copy(0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NebulaViolet,
                        unfocusedBorderColor = Color.White.copy(0.3f),
                        focusedLabelColor = NebulaViolet,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White.copy(0.8f)
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playlistName.isNotEmpty() && song != null) {
                        val newPlaylist = Playlist(
                            id = System.currentTimeMillis(),
                            name = playlistName,
                            coverUrl = song.coverUrl,
                            songCount = 1,
                            songs = listOf(song)
                        )
                        viewModel.createPlaylist(playlistName)
                        showNewPlaylist.value = false
                    }
                }) {
                    Text("创建", color = NebulaViolet)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewPlaylist.value = false }) {
                    Text("取消", color = Color.White.copy(0.7f))
                }
            },
            containerColor = Color(0xFF1E1E1E),
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
    
    // 唱针角度动画
    val dur = if (viewModel.totalDuration > 0) viewModel.totalDuration else song.duration

    val coverColors = rememberCoverColors(song.coverUrl)
    // 动画过渡提取色，切歌时平滑过渡
    val animBase     by animateColorAsState(coverColors.base,      tween(800), label = "aBase")
    val animDominant by animateColorAsState(coverColors.dominant,  tween(800), label = "aDom")
    val animMuted    by animateColorAsState(coverColors.muted,     tween(800), label = "aMut")
    val animSecondary by animateColorAsState(coverColors.secondary, tween(800), label = "aSec")
    val baseBg = if (showLyrics) animBase else Color(0xFF1A1A1A)

    if (showLyricsSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showLyricsSheet.value = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = baseBg,
        ) {
            Box(Modifier.fillMaxWidth().height(520.dp)) {
                if (viewModel.lyrics.isNotEmpty()) {
                    LyricsViewImmersive(viewModel.lyrics, viewModel.currentLyricIndex, baseBg, currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null, normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null, lyricFontSize = viewModel.lyricFontSize)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("暂无歌词", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }

    // 状态栏透明，让渐变背景延伸到状态栏区域；离开播放器时自动恢复
    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = view.context as? android.app.Activity
        val window = activity?.window
        val savedStatusBarColor = window?.statusBarColor ?: 0
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val savedLightStatusBars = controller?.isAppearanceLightStatusBars ?: false

        window?.statusBarColor = android.graphics.Color.TRANSPARENT
        controller?.isAppearanceLightStatusBars = false

        onDispose {
            window?.statusBarColor = savedStatusBarColor
            controller?.isAppearanceLightStatusBars = savedLightStatusBars
        }
    }

    // ═══ 纯色背景 (网易云风格: 封面主色纯色底 + 微妙纵向渐变) ═══
    Box(Modifier.fillMaxSize()) {
        // 纯色底 + 上下微妙明暗变化
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to animDominant,
                    0.45f to animBase,
                    1f to animMuted,
                )
            )
        )

        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {

            // 根据播放器样式显示不同UI
            when (viewModel.playerStyle) {
                MusicViewModel.PlayerStyle.IMMERSIVE -> {
                    // ═══ 沉浸式歌词模式 ═══
                    if (viewModel.lyrics.isNotEmpty()) {
                        ImmersiveLyricsPlayer(song, viewModel, baseBg, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                    } else {
                        CoverArtPlayer(song, viewModel, baseBg, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                    }
                }
                MusicViewModel.PlayerStyle.COVER -> {
                    // ═══ 封面大图模式 ═══
                    CoverArtPlayer(song, viewModel, baseBg, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                }
                MusicViewModel.PlayerStyle.MINIMAL -> {
                    // ═══ 极简模式 ═══
                    MinimalPlayer(song, viewModel, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                }
                MusicViewModel.PlayerStyle.WAVE -> {
                    // ═══ 波形可视化模式 ═══
                    WaveformPlayer(song, viewModel, baseBg, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                }
                MusicViewModel.PlayerStyle.DISC_LYRICS -> {
                    // ═══ 唱片歌词模式 ═══
                    DiscLyricsPlayer(song, viewModel, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
                }
                else -> {
                    // ═══ 黑胶唱片模式 (默认) ═══
                    VinylPlayer(song, viewModel, rotation, onBack, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality, baseBg)
                }
            }
        }
    }
}

// ═══ 黑胶唱片模式 ═══
@Composable
private fun VinylPlayer(
    song: Song,
    viewModel: MusicViewModel,
    rotation: Float,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
    baseBg: Color,
) {
    var showVinylLyrics by remember { mutableStateOf(false) }

    if (showVinylLyrics && viewModel.lyrics.isNotEmpty()) {
        // ═══ 纯沉浸式歌词模式（无任何UI控件）═══
        Box(
            Modifier
                .fillMaxSize()
                .background(baseBg)
                .clickable { showVinylLyrics = false }
        ) {
            LyricsViewImmersive(
                lyrics = viewModel.lyrics,
                currentIndex = viewModel.currentLyricIndex,
                baseBg = baseBg,
                topPadding = 0.dp,
                bottomPadding = 0.dp,
                currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null,
                normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null,
                lyricFontSize = viewModel.lyricFontSize,
                onSeekTo = { timeMs -> viewModel.seekTo(timeMs / dur.toFloat()) }
            )
        }
    } else {
        // ═══ 封面模式（正常显示所有控件）═══
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
                }
                QualityBadge(viewModel)
            }

            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularCoverWithSpectrum(
                    coverUrl = song.coverUrl,
                    isPlaying = viewModel.isPlaying,
                    audioAmplitude = viewModel.audioAmplitude,
                    isBuffering = viewModel.isBuffering,
                    modifier = Modifier.size(380.dp),
                    onClick = { if (viewModel.lyrics.isNotEmpty()) showVinylLyrics = true }
                )
                
                if (viewModel.lyrics.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val idx = viewModel.currentLyricIndex
                        val cur = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].text else ""
                        Text(cur, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 32.dp))
                        val nxt = if (idx + 1 in viewModel.lyrics.indices) viewModel.lyrics[idx + 1].text else ""
                        if (nxt.isNotEmpty()) {
                            Text(nxt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 32.dp))
                        }
                    }
                }
            }

            StandardBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
        }
    }
}

// ═══ 沉浸式歌词播放器 ═══
@Composable
private fun ImmersiveLyricsPlayer(
    song: Song,
    viewModel: MusicViewModel,
    baseBg: Color,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    Column(Modifier.fillMaxSize()) {
        // 极简顶部（透明背景）
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(song.title, style = MaterialTheme.typography.bodyMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.5f), maxLines = 1)
            }
            QualityBadge(viewModel)
            Spacer(Modifier.width(8.dp))
        }

        // 歌词区域
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LyricsViewImmersive(
                lyrics = viewModel.lyrics,
                currentIndex = viewModel.currentLyricIndex,
                baseBg = baseBg,
                topPadding = 8.dp,
                bottomPadding = 8.dp,
                currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null,
                normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null,
                lyricFontSize = viewModel.lyricFontSize
            )
        }

        // 底部控制
        ImmersiveBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
    }
}

@Composable
private fun ImmersiveBottomControls(
    song: Song,
    viewModel: MusicViewModel,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // 时间显示（放在进度条上方，避免遮挡）
        var isDragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.progress }
                .collect { if (!isDragging) dragValue = it }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmt((dragValue * dur).toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDragging) Color.White else Color.White.copy(0.5f)
            )
            Text(fmt(dur), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.5f))
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 进度条
        SeekBar(
            progress = dragValue,
            onSeekStart = {
                isDragging = true
                viewModel.isSeeking = true
            },
            onSeek = { dragValue = it },
            onSeekFinished = {
                viewModel.seekTo(it)
                isDragging = false
                viewModel.isSeeking = false
            },
            activeColor = Color.White,
            inactiveColor = Color.White.copy(0.15f),
        )

        Spacer(Modifier.height(16.dp))

        // 主播放控制（参考截图：播放模式/上一首/播放暂停/下一首/列表）
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { viewModel.togglePlayMode() }, Modifier.size(44.dp)) {
                Icon(
                    when (viewModel.playMode) {
                        MusicViewModel.PlayMode.LOOP -> Icons.Default.Repeat
                        MusicViewModel.PlayMode.SINGLE -> Icons.Default.RepeatOne
                        MusicViewModel.PlayMode.SHUFFLE -> Icons.Default.Shuffle
                    },
                    null,
                    tint = Color.White.copy(0.9f),
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = { viewModel.playPrevious() }, Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Box(
                Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.15f))
                    .clickable { viewModel.togglePlay() },
                contentAlignment = Alignment.Center,
            ) {
                if (viewModel.isBuffering) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            IconButton(onClick = { viewModel.playNext() }, Modifier.size(48.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { showQueue.value = true }, Modifier.size(44.dp)) {
                Icon(Icons.Default.List, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // 其它功能（仍然放底部，不放顶部）
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isFav = viewModel.isFavorite(song)
            IconButton(onClick = { viewModel.toggleLike(song) }, Modifier.size(36.dp)) {
                Icon(
                    if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    null,
                    tint = if (isFav) NebulaPink else Color.White.copy(0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = { showAddToPlaylist.value = true }, Modifier.size(36.dp)) {
                Icon(Icons.Default.PlaylistAdd, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showLyricsSheet.value = true }, Modifier.size(36.dp)) {
                Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { viewModel.toggleFloatingLyrics() }, Modifier.size(36.dp)) {
                Text(
                    "词",
                    color = if (viewModel.isFloatingLyricsEnabled) NebulaViolet else Color.White.copy(0.6f),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            IconButton(onClick = { showEqualizer.value = true }, Modifier.size(36.dp)) {
                Icon(Icons.Default.Equalizer, null, tint = Color.White.copy(0.6f), modifier = Modifier.size(20.dp))
            }
            // Download button — 仅插件模式显示
            if (viewModel.apiMode == "lx_plugin") {
                IconButton(onClick = { showDownloadQuality.value = true }, Modifier.size(36.dp)) {
                    val dlState = viewModel.downloadState
                    val isThisDownloading = dlState is MusicViewModel.DownloadState.Downloading && dlState.songId == song.platformId
                    val isDownloaded = viewModel.isDownloaded(song)
                    if (isThisDownloading) {
                        CircularProgressIndicator(
                            progress = { (dlState as MusicViewModel.DownloadState.Downloading).progress },
                            modifier = Modifier.size(20.dp),
                            color = NebulaViolet,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            null,
                            tint = if (isDownloaded) NebulaViolet else Color.White.copy(0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// ═══ 封面大图模式 ═══
@Composable
private fun CoverArtPlayer(
    song: Song,
    viewModel: MusicViewModel,
    baseBg: Color,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    var showCoverLyrics by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
            }
            QualityBadge(viewModel)
        }

        if (showCoverLyrics && viewModel.lyrics.isNotEmpty()) {
            // ── 全屏歌词视图（点击回到封面） ──
            Box(Modifier.weight(1f).fillMaxWidth().clickable { showCoverLyrics = false }) {
                LyricsViewImmersive(
                    lyrics = viewModel.lyrics,
                    currentIndex = viewModel.currentLyricIndex,
                    baseBg = Color.Transparent,
                    topPadding = 8.dp,
                    bottomPadding = 8.dp,
                    currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null,
                    normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null,
                    lyricFontSize = viewModel.lyricFontSize
                )
            }
        } else {
            // ── 大封面（点击进入歌词） ──
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clickable { if (viewModel.lyrics.isNotEmpty()) showCoverLyrics = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier
                            .size(300.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF333333)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                song.coverUrl,
                                null,
                                Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(100.dp))
                        }
                        if (viewModel.isBuffering) {
                            CircularProgressIndicator(Modifier.size(40.dp), color = Color.White.copy(0.7f), strokeWidth = 2.dp)
                        }
                    }
                    // 内嵌歌词
                    if (viewModel.lyrics.isNotEmpty()) {
                        val idx = viewModel.currentLyricIndex
                        Spacer(Modifier.height(12.dp))
                        val cur = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].text else ""
                        Text(cur, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
                        val curT = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].ttext else ""
                        if (curT.isNotEmpty()) {
                            Text(curT, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.55f), textAlign = TextAlign.Center, maxLines = 1)
                        }
                        val nxt = if (idx + 1 in viewModel.lyrics.indices) viewModel.lyrics[idx + 1].text else ""
                        if (nxt.isNotEmpty()) {
                            Text(nxt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), textAlign = TextAlign.Center, maxLines = 1)
                        }
                    }
                }
            }
        }

        // 底部完整控件
        StandardBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
    }
}

// ═══ 极简模式 ═══
@Composable
private fun MinimalPlayer(
    song: Song,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    var showMinimalLyrics by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.weight(1f))
            QualityBadge(viewModel)
        }

        if (showMinimalLyrics && viewModel.lyrics.isNotEmpty()) {
            // ── 全屏歌词视图（点击回到封面） ──
            Box(Modifier.weight(1f).fillMaxWidth().clickable { showMinimalLyrics = false }) {
                LyricsViewImmersive(
                    lyrics = viewModel.lyrics,
                    currentIndex = viewModel.currentLyricIndex,
                    baseBg = Color.Transparent,
                    topPadding = 8.dp,
                    bottomPadding = 8.dp,
                    currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null,
                    normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null,
                    lyricFontSize = viewModel.lyricFontSize
                )
            }
        } else {
            // ── 封面 + 歌曲信息（点击进入歌词） ──
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable { if (viewModel.lyrics.isNotEmpty()) showMinimalLyrics = true },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // 圆形封面
                    Box(
                        Modifier.size(140.dp).clip(RoundedCornerShape(70.dp)).background(Color(0xFF333333)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.coverUrl.isNotEmpty()) {
                            AsyncImage(song.coverUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(60.dp))
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(song.title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = Color.White, textAlign = TextAlign.Center, maxLines = 2)
                    Spacer(Modifier.height(6.dp))
                    Text(song.artist, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(0.6f), textAlign = TextAlign.Center)
                    // 内嵌歌词
                    if (viewModel.lyrics.isNotEmpty()) {
                        val idx = viewModel.currentLyricIndex
                        Spacer(Modifier.height(16.dp))
                        val cur = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].text else ""
                        Text(cur, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 24.dp))
                        val curT = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].ttext else ""
                        if (curT.isNotEmpty()) {
                            Text(curT, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.55f), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                        val nxt = if (idx + 1 in viewModel.lyrics.indices) viewModel.lyrics[idx + 1].text else ""
                        if (nxt.isNotEmpty()) {
                            Text(nxt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }
            }
        }

        // 底部完整控件
        StandardBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
    }
}

// ═══ 波形可视化模式 ═══
@Composable
private fun WaveformPlayer(
    song: Song,
    viewModel: MusicViewModel,
    baseBg: Color,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    // 波形动画
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.6f))
            }
            QualityBadge(viewModel)
        }

        // 波形区域
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 小封面
                Box(
                    Modifier.size(100.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF333333)),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.coverUrl.isNotEmpty()) {
                        AsyncImage(song.coverUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(44.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
                // 条形波形动画
                Row(Modifier.fillMaxWidth(0.75f).height(100.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    repeat(24) { index ->
                        val animatedHeight by infiniteTransition.animateFloat(
                            initialValue = 0.15f,
                            targetValue = if (viewModel.isPlaying) 0.85f else 0.2f,
                            animationSpec = infiniteRepeatable(
                                tween(250 + (index * 40), easing = FastOutSlowInEasing),
                                RepeatMode.Reverse
                            ),
                            label = "bar$index"
                        )
                        Box(
                            Modifier
                                .width(5.dp)
                                .fillMaxHeight(animatedHeight.coerceIn(0.1f, 1f))
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(alpha = if (viewModel.isPlaying) 0.9f else 0.4f),
                                            Color.White.copy(alpha = if (viewModel.isPlaying) 0.4f else 0.15f),
                                        )
                                    )
                                )
                        )
                    }
                }
                // 内嵌歌词
                if (viewModel.lyrics.isNotEmpty()) {
                    val idx = viewModel.currentLyricIndex
                    Spacer(Modifier.height(12.dp))
                    val cur = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].text else ""
                    Text(cur, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White, textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(horizontal = 24.dp))
                    val curT = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].ttext else ""
                    if (curT.isNotEmpty()) {
                        Text(curT, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.55f), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                    val nxt = if (idx + 1 in viewModel.lyrics.indices) viewModel.lyrics[idx + 1].text else ""
                    if (nxt.isNotEmpty()) {
                        Text(nxt, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.4f), textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
            if (viewModel.isBuffering) {
                CircularProgressIndicator(Modifier.size(40.dp), color = Color.White.copy(0.7f), strokeWidth = 2.dp)
            }
        }

        // 底部完整控件
        StandardBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
    }
}

// ═══ 唱片歌词模式 ═══
@Composable
private fun DiscLyricsPlayer(
    song: Song,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    // 使用共享的封面颜色提取
    val coverColors = rememberCoverColors(song.coverUrl)
    // 从真实提取色派生浅色背景
    val bgColor = remember(coverColors) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(
            android.graphics.Color.argb(
                255,
                (coverColors.dominant.red * 255).toInt(),
                (coverColors.dominant.green * 255).toInt(),
                (coverColors.dominant.blue * 255).toInt()
            ), hsv
        )
        hsv[1] = hsv[1].coerceIn(0.04f, 0.20f)
        hsv[2] = hsv[2].coerceAtLeast(0.90f)
        Color(AndroidColor.HSVToColor(hsv))
    }
    // 从真实 vibrant 派生强调色
    val accentColor = remember(coverColors) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(
            android.graphics.Color.argb(
                255,
                (coverColors.vibrant.red * 255).toInt(),
                (coverColors.vibrant.green * 255).toInt(),
                (coverColors.vibrant.blue * 255).toInt()
            ), hsv
        )
        hsv[1] = hsv[1].coerceIn(0.25f, 0.60f)
        hsv[2] = hsv[2].coerceIn(0.25f, 0.45f)
        Color(AndroidColor.HSVToColor(hsv))
    }

    // 动画过渡背景色
    val animBg by animateColorAsState(bgColor, tween(600), label = "discBg")
    val animAccent by animateColorAsState(accentColor, tween(600), label = "discAccent")

    // 本地切换：封面 ↔ 全屏歌词
    var showFullLyrics by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(animBg)) {
        Column(Modifier.fillMaxSize()) {
            // 顶部栏
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 歌曲信息（左上）
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(
                        song.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = animAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = animAccent.copy(0.6f))
                }
                QualityBadge(viewModel)
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = animAccent.copy(0.7f), modifier = Modifier.size(26.dp))
                }
            }

            if (showFullLyrics && viewModel.lyrics.isNotEmpty()) {
                // ── 全屏歌词视图（点击回到封面） ──
                Box(Modifier.weight(1f).fillMaxWidth().clickable { showFullLyrics = false }) {
                    LyricsViewImmersive(
                        lyrics = viewModel.lyrics,
                        currentIndex = viewModel.currentLyricIndex,
                        baseBg = animBg,
                        topPadding = 8.dp,
                        bottomPadding = 8.dp,
                        currentLyricColor = if (viewModel.lyricCurrentColor != 0) Color(viewModel.lyricCurrentColor) else null,
                        normalLyricColor = if (viewModel.lyricNormalColor != 0) Color(viewModel.lyricNormalColor) else null,
                        lyricFontSize = viewModel.lyricFontSize
                    )
                }
            } else {
                // ── 封面 + 简要歌词视图 ──
                // 圆形封面（点击进入全屏歌词）
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .clickable { if (viewModel.lyrics.isNotEmpty()) showFullLyrics = true },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .size(220.dp)
                            .clip(CircleShape)
                            .background(animAccent.copy(0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (song.coverUrl.isNotEmpty()) {
                            AsyncImage(
                                song.coverUrl, null,
                                Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.MusicNote, null, tint = animAccent.copy(0.3f), modifier = Modifier.size(80.dp))
                        }
                    }
                    if (viewModel.isBuffering) {
                        CircularProgressIndicator(Modifier.size(40.dp), color = animAccent.copy(0.5f), strokeWidth = 2.dp)
                    }
                }

                // 歌词区域（简要：当前+下一句）
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = 20.dp, bottom = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (viewModel.lyrics.isNotEmpty()) {
                        val idx = viewModel.currentLyricIndex
                        Column(
                            Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val currentText = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].text else ""
                            Text(
                                currentText,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 28.sp,
                                ),
                                color = animAccent,
                                textAlign = TextAlign.Center,
                                maxLines = 3,
                            )
                            val currentTText = if (idx in viewModel.lyrics.indices) viewModel.lyrics[idx].ttext else ""
                            if (currentTText.isNotEmpty()) {
                                Text(
                                    currentTText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = animAccent.copy(0.6f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            val nextText = if (idx + 1 in viewModel.lyrics.indices) viewModel.lyrics[idx + 1].text else ""
                            if (nextText.isNotEmpty()) {
                                Text(
                                    nextText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = animAccent.copy(0.45f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                )
                            }
                        }
                    } else {
                        Text(
                            "纯音乐，请欣赏",
                            style = MaterialTheme.typography.bodyLarge,
                            color = animAccent.copy(0.35f),
                        )
                    }
                }
            }

            // 底部控件（和黑胶唱片一样）
            StandardBottomControls(song, viewModel, dur, showQueue, showBottomSheet, showEqualizer, showLyricsSheet, showNewPlaylist, showAddToPlaylist, showDownloadQuality)
        }
    }
}

// ═══ 自定义进度条（完全自控触摸，绕过 Material3 Slider 手势问题） ═══
@Composable
private fun SeekBar(
    progress: Float,
    onSeekStart: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekFinished: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(0.15f),
    thumbColor: Color = Color.White,
) {
    val trackHeightPx = with(LocalDensity.current) { 3.dp.toPx() }
    val thumbRadiusPx = with(LocalDensity.current) { 7.dp.toPx() }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        val w = size.width.coerceAtLeast(1).toFloat()
                        val initial = (down.position.x / w).coerceIn(0f, 1f)
                        onSeekStart()
                        onSeek(initial)

                        var current = initial
                        try {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                val change = event.changes.firstOrNull() ?: break
                                if (change.pressed) {
                                    change.consume()
                                    current = (change.position.x / w).coerceIn(0f, 1f)
                                    onSeek(current)
                                } else {
                                    change.consume()
                                    break
                                }
                            }
                        } finally {
                            onSeekFinished(current)
                        }
                    }
                }
            }
    ) {
        val centerY = size.height / 2
        val w = size.width
        val p = progress.coerceIn(0f, 1f)

        // Inactive track
        drawLine(inactiveColor, Offset(0f, centerY), Offset(w, centerY), trackHeightPx, StrokeCap.Round)

        // Active track
        val activeEnd = p * w
        if (activeEnd > 0f) {
            drawLine(activeColor, Offset(0f, centerY), Offset(activeEnd, centerY), trackHeightPx, StrokeCap.Round)
        }

        // Thumb
        drawCircle(thumbColor, thumbRadiusPx, Offset(activeEnd, centerY))
    }
}

// ═══ 标准底部控件 ═══
@Composable
private fun StandardBottomControls(
    song: Song,
    viewModel: MusicViewModel,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showBottomSheet: MutableState<Boolean>,
    showEqualizer: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>,
    showNewPlaylist: MutableState<Boolean>,
    showAddToPlaylist: MutableState<Boolean>,
    showDownloadQuality: MutableState<Boolean>,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
        // 进度条 + 时间
        var isDragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.progress }
                .collect { if (!isDragging) dragValue = it }
        }

        SeekBar(
            progress = dragValue,
            onSeekStart = {
                isDragging = true
                viewModel.isSeeking = true
            },
            onSeek = { dragValue = it },
            onSeekFinished = {
                viewModel.seekTo(it)
                isDragging = false
                viewModel.isSeeking = false
            },
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmt((dragValue * dur).toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDragging) Color.White else Color.White.copy(0.4f)
            )
            Text(fmt(dur), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
        }

        Spacer(Modifier.height(12.dp))

        // 主要播放控制
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.playPrevious() }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
            Box(Modifier.size(68.dp).clip(CircleShape).background(Color.White).clickable { viewModel.togglePlay() }, contentAlignment = Alignment.Center) {
                if (viewModel.isBuffering) CircularProgressIndicator(Modifier.size(30.dp), color = Color.Black, strokeWidth = 2.5.dp)
                else Icon(if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(38.dp))
            }
            IconButton(onClick = { viewModel.playNext() }, Modifier.size(52.dp)) { Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(36.dp)) }
        }

        Spacer(Modifier.height(8.dp))

        // 辅助功能按钮
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            val isFav = viewModel.isFavorite(song)
            IconButton(onClick = { viewModel.toggleLike(song) }, Modifier.size(40.dp)) {
                Icon(if (isFav) Icons.Default.Favorite else Icons.Default.FavoriteBorder, null, tint = if (isFav) NebulaPink else Color.White.copy(0.8f), modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { showAddToPlaylist.value = true }, Modifier.size(40.dp)) {
                Icon(Icons.Default.PlaylistAdd, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { viewModel.togglePlayMode() }, Modifier.size(40.dp)) {
                Icon(when (viewModel.playMode) { MusicViewModel.PlayMode.LOOP -> Icons.Default.Repeat; MusicViewModel.PlayMode.SINGLE -> Icons.Default.RepeatOne; MusicViewModel.PlayMode.SHUFFLE -> Icons.Default.Shuffle }, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = { viewModel.toggleFloatingLyrics() }, Modifier.size(40.dp)) {
                Text("词", color = if (viewModel.isFloatingLyricsEnabled) NebulaViolet else Color.White.copy(0.8f), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
            }
            IconButton(onClick = { showQueue.value = true }, Modifier.size(40.dp)) { Icon(Icons.Default.List, null, tint = Color.White.copy(0.85f), modifier = Modifier.size(22.dp)) }
            IconButton(onClick = { showEqualizer.value = true }, Modifier.size(40.dp)) {
                Icon(Icons.Default.Equalizer, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(22.dp))
            }
            // Download button — 仅插件模式显示
            if (viewModel.apiMode == "lx_plugin") {
                IconButton(onClick = { showDownloadQuality.value = true }, Modifier.size(40.dp)) {
                    val dlState = viewModel.downloadState
                    val isThisDownloading = dlState is MusicViewModel.DownloadState.Downloading && dlState.songId == song.platformId
                    val isDownloaded = viewModel.isDownloaded(song)
                    if (isThisDownloading) {
                        CircularProgressIndicator(
                            progress = { (dlState as MusicViewModel.DownloadState.Downloading).progress },
                            modifier = Modifier.size(22.dp),
                            color = NebulaViolet,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Default.Download,
                            null,
                            tint = if (isDownloaded) NebulaViolet else Color.White.copy(0.8f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { showBottomSheet.value = true }, Modifier.size(40.dp)) {
                Icon(Icons.Default.MoreHoriz, null, tint = Color.White.copy(0.8f), modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

// ═══ 极简底部控件 ═══
@Composable
private fun MinimalBottomControls(
    viewModel: MusicViewModel,
    dur: Long,
    showQueue: MutableState<Boolean>,
    showLyricsSheet: MutableState<Boolean>? = null,
    showNewPlaylist: MutableState<Boolean>? = null,
    showAddToPlaylist: MutableState<Boolean>? = null,
    song: Song? = null,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        // 时间显示
        var isDragging by remember { mutableStateOf(false) }
        var dragValue by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            snapshotFlow { viewModel.progress }
                .collect { if (!isDragging) dragValue = it }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                fmt((dragValue * dur).toLong()),
                style = MaterialTheme.typography.labelSmall,
                color = if (isDragging) Color.White else Color.White.copy(0.4f)
            )
            Text(fmt(dur), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.4f))
        }
        
        Spacer(Modifier.height(8.dp))
        
        // 进度条
        SeekBar(
            progress = dragValue,
            onSeekStart = {
                isDragging = true
                viewModel.isSeeking = true
            },
            onSeek = { dragValue = it },
            onSeekFinished = {
                viewModel.seekTo(it)
                isDragging = false
                viewModel.isSeeking = false
            },
            inactiveColor = Color.White.copy(0.2f),
        )

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.togglePlayMode() }, Modifier.size(36.dp)) {
                Icon(when (viewModel.playMode) { MusicViewModel.PlayMode.LOOP -> Icons.Default.Repeat; MusicViewModel.PlayMode.SINGLE -> Icons.Default.RepeatOne; MusicViewModel.PlayMode.SHUFFLE -> Icons.Default.Shuffle }, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { viewModel.playPrevious() }, Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            IconButton(onClick = { viewModel.togglePlay() }, Modifier.size(56.dp)) {
                if (viewModel.isBuffering) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(if (viewModel.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            IconButton(onClick = { viewModel.playNext() }, Modifier.size(44.dp)) {
                Icon(Icons.Default.SkipNext, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            if (showAddToPlaylist != null && song != null) {
                IconButton(onClick = { showAddToPlaylist.value = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.PlaylistAdd, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(20.dp))
                }
            }
            if (showLyricsSheet != null) {
                IconButton(onClick = { showLyricsSheet.value = true }, Modifier.size(36.dp)) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(20.dp))
                }
            }
            IconButton(onClick = { showQueue.value = true }, Modifier.size(36.dp)) {
                Icon(Icons.Default.List, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(22.dp))
            }
        }
    }
}

// ═══ 黑胶唱片 (正方形封面 + 真实音频律动 + 3D倾斜 + 光泽 + 光晕) ═══
@Composable
private fun VinylDisc(rotation: Float, coverUrl: String, isPlaying: Boolean, audioAmplitude: Float, onClick: (() -> Unit)? = null) {
    var pointerX by remember { mutableFloatStateOf(0.5f) }
    var pointerY by remember { mutableFloatStateOf(0.5f) }
    var isActive by remember { mutableStateOf(false) }

    val tiltX by animateFloatAsState(
        targetValue = if (isActive) (pointerX - 0.5f) * 22f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltX",
    )
    val tiltY by animateFloatAsState(
        targetValue = if (isActive) -(pointerY - 0.5f) * 18f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "tiltY",
    )

    val shineX by animateFloatAsState(targetValue = pointerX, animationSpec = tween(120), label = "shX")
    val shineY by animateFloatAsState(targetValue = pointerY, animationSpec = tween(120), label = "shY")

    val glowScale by animateFloatAsState(
        targetValue = if (isPlaying) (if (isActive) 1.12f else 1.05f) else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 200f),
        label = "glowScale",
    )
    val shineAlpha by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(250),
        label = "shAlpha",
    )

    // 🎵 使用真实音频数据驱动封面律动（来自 AudioProcessor）
    val musicPulseScale = if (isPlaying) {
        // 只要有振幅数据就应用缩放（最小1.0，最大1.15）
        audioAmplitude.coerceIn(1.0f, 1.15f)
    } else {
        1f
    }

    // 平滑过渡动画（避免跳动过于突兀）
    val animatedPulse by animateFloatAsState(
        targetValue = musicPulseScale,
        animationSpec = tween(50, easing = LinearEasing),
        label = "pulseAnim"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(280.dp)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onClick() }
                } else {
                    Modifier
                }
            )
            .pointerInput(Unit) {
                val w = size.width.coerceAtLeast(1)
                val h = size.height.coerceAtLeast(1)
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: continue
                        if (change.pressed) {
                            isActive = true
                            pointerX = (change.position.x / w).coerceIn(0f, 1f)
                            pointerY = (change.position.y / h).coerceIn(0f, 1f)
                        } else {
                            isActive = false
                        }
                    }
                }
            },
    ) {
        Box(
            Modifier
                .size(270.dp)
                .graphicsLayer { scaleX = glowScale; scaleY = glowScale }
                .drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF71C4FF).copy(alpha = 0.15f),
                                Color.Transparent,
                            ),
                            center = Offset(size.width / 2, size.height / 2),
                            radius = size.maxDimension * 0.6f,
                        ),
                    )
                },
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(260.dp)
                .clip(RoundedCornerShape(12.dp))
                .graphicsLayer {
                    rotationY = tiltX
                    rotationX = tiltY
                    cameraDistance = 14f * density
                },
        ) {
            Box(
                Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1A1A))
                    .graphicsLayer {
                        scaleX = animatedPulse
                        scaleY = animatedPulse
                    }
            ) {
                if (coverUrl.isNotEmpty()) {
                    AsyncImage(
                        coverUrl,
                        null,
                        Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(80.dp))
                    }
                }
            }

            Box(
                Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .graphicsLayer { alpha = shineAlpha }
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.04f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * shineX, size.height * shineY),
                                radius = size.maxDimension * 0.5f,
                            ),
                        )
                    },
            )

            Box(
                Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .drawBehind {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = if (isActive) 0.07f else 0.02f),
                                    Color.Transparent,
                                    Color.White.copy(alpha = if (isActive) 0.04f else 0.01f),
                                ),
                                start = Offset(size.width * shineX, 0f),
                                end = Offset(size.width * (1f - shineX), size.height),
                            ),
                        )
                    },
            )
        }
        
        // 🎵 正方形音频可视化频谱 - 超大版本
        SquareAudioVisualizer(
            audioAmplitude = audioAmplitude,
            isPlaying = isPlaying,
            modifier = Modifier.size(420.dp)
        )
    }
}

/**
 * 圆形封面 + 环形放射状频谱组件
 * 模仿图片中的设计：圆形封面 + 周围放射状频谱条
 */
@Composable
private fun CircularCoverWithSpectrum(
    coverUrl: String,
    isPlaying: Boolean,
    audioAmplitude: Float,
    isBuffering: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 环形频谱参数 - 超高密度、更宽更高
    val spectrumBars = 128
    val barWidth = 4f
    val minBarLength = 15f
    val maxBarLength = 80f
    val coverSizeDp = 270.dp
    val coverSizePx = with(LocalDensity.current) { coverSizeDp.toPx() }
    val innerRadius = coverSizePx / 2 // 封面半径，确保频谱从封面边缘开始
    
    // 频谱状态
    val barScales = remember { List(spectrumBars) { Animatable(0.15f) } }
    
    // 封面旋转角度
    val rotation = remember { Animatable(0f) }
    
    // 目标值
    val targetScale = if (isPlaying) audioAmplitude else 1f
    
    // 封面旋转动画
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            rotation.animateTo(
                targetValue = rotation.value + 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(20000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }
    
    // 更新频谱动画 - 增强跳动效果
    LaunchedEffect(targetScale, isPlaying) {
        barScales.forEachIndexed { index, animatable ->
            launch {
                val position = index.toFloat() / spectrumBars
                val waveDelay = (index % 16) * 4L

                delay(waveDelay)

                val target = if (isPlaying) {
                    // 增强振幅响应
                    val baseResponse = kotlin.math.sin(position * kotlin.math.PI.toFloat() * 2) * 0.5f + 0.5f
                    val amplitudeBoost = (targetScale - 1f) * baseResponse * 6f // 从4f增加到6f
                    val randomVariation = (Math.random().toFloat() - 0.5f) * 0.1f // 添加随机变化
                    (0.25f + amplitudeBoost + randomVariation).coerceIn(0.2f, 1f)
                } else {
                    0.2f
                }

                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(50, easing = LinearEasing) // 更快的动画
                )
            }
        }
    }
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            for (i in 0 until spectrumBars) {
                val angle = (i.toFloat() / spectrumBars) * 360f - 90f
                val scale = barScales[i].value
                // 短线条，像第二张图片那样
                val currentBarLength = minBarLength + (maxBarLength - minBarLength) * scale
                
                val radian = Math.toRadians(angle.toDouble())
                
                // 频谱条从封面边缘向外延伸
                val startX = centerX + innerRadius * kotlin.math.cos(radian).toFloat()
                val startY = centerY + innerRadius * kotlin.math.sin(radian).toFloat()
                
                val endX = centerX + (innerRadius + currentBarLength) * kotlin.math.cos(radian).toFloat()
                val endY = centerY + (innerRadius + currentBarLength) * kotlin.math.sin(radian).toFloat()
                
                // 紫色系配色 - 更亮更明显
                val colorAlpha = 0.7f + scale * 0.3f
                val barColor = when {
                    scale > 0.7f -> Color(0xFF9C27B0).copy(alpha = colorAlpha) // 深紫
                    scale > 0.45f -> Color(0xFFBA68C8).copy(alpha = colorAlpha) // 中紫
                    else -> Color(0xFFE1BEE7).copy(alpha = colorAlpha * 0.8f)  // 浅紫
                }
                
                // 外发光 - 更明显
                drawLine(
                    color = barColor.copy(alpha = 0.35f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = barWidth + 4f,
                    cap = StrokeCap.Round
                )
                
                // 主体 - 更亮
                drawLine(
                    color = barColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = barWidth,
                    cap = StrokeCap.Round
                )
                
                // 顶部高光
                if (scale > 0.35f) {
                    val highlightEndX = startX + (endX - startX) * 0.4f
                    val highlightEndY = startY + (endY - startY) * 0.4f
                    drawLine(
                        color = Color.White.copy(alpha = 0.6f * scale),
                        start = Offset(startX, startY),
                        end = Offset(highlightEndX, highlightEndY),
                        strokeWidth = barWidth * 0.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            
            // 封面外圈发光
            if (isPlaying) {
                val glowAlpha = (0.1f + (audioAmplitude - 1f) * 0.15f).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFF7C4DFF).copy(alpha = glowAlpha),
                    radius = innerRadius + 5f,
                    center = Offset(centerX, centerY)
                )
            }
        }
        
        // 旋转的圆形封面
        Box(
            modifier = Modifier
                .size(coverSizeDp)
                .graphicsLayer { rotationZ = rotation.value }
                .clip(CircleShape)
                .then(if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onClick() } else Modifier)
        ) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center).size(40.dp),
                    color = Color.White.copy(0.7f),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

/**
 * 正方形音频可视化频谱组件 - 霓虹玫瑰风格
 */
@Composable
private fun SquareAudioVisualizer(
    audioAmplitude: Float,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // 每个边的频谱条数 - 33条
    val barsPerSide = 33
    // 动画目标值 - 高潮时放大1.5倍
    val targetScale = if (isPlaying) audioAmplitude else 1f
    val highEnergyScale = if (targetScale > 1.08f) targetScale * 1.5f else targetScale
    
    // 使用 remember 存储每个频谱条的状态
    val barScales = remember { List(barsPerSide * 4) { Animatable(0.05f) } }
    
    // 模拟频谱数据 - 创建呼吸感的波形
    val spectrumData = remember { List(barsPerSide * 4) { index ->
        val position = index % barsPerSide
        val normalizedPos = position.toFloat() / barsPerSide
        // 中间高两边低的弧形分布，模拟自然呼吸
        val archResponse = kotlin.math.sin(normalizedPos * kotlin.math.PI.toFloat())
        // 添加一些随机性
        val randomOffset = (index % 7) * 0.05f
        (archResponse * 0.7f + 0.3f + randomOffset).coerceIn(0.2f, 1f)
    }}
    
    // 更新频谱条动画 - 呼吸感律动
    LaunchedEffect(highEnergyScale, isPlaying) {
        barScales.forEachIndexed { index, animatable ->
            launch {
                val baseResponse = spectrumData[index]
                // 根据位置错开动画，形成波浪效果
                val waveDelay = (index % barsPerSide) * 10L
                val sideDelay = (index / barsPerSide) * 20L
                
                delay(waveDelay + sideDelay)
                
                val target = if (isPlaying) {
                    // 基础呼吸高度 + 振幅响应
                    val breathBase = 0.15f + baseResponse * 0.2f
                    val amplitudeBoost = (highEnergyScale - 1f) * baseResponse * 2.5f
                    val emotionalVariation = kotlin.math.sin(index * 0.5f) * 0.1f
                    (breathBase + amplitudeBoost + emotionalVariation).coerceIn(0.05f, 1f)
                } else {
                    0.05f // 暂停时几乎隐藏
                }
                
                animatable.animateTo(
                    targetValue = target,
                    animationSpec = tween(80, easing = LinearEasing)
                )
            }
        }
    }
    
    // 🎨 玫瑰霓虹配色 - 外紫内红渐变
    val neonColors = listOf(
        Color(0xFF8B1A5C), // 深紫红（外）
        Color(0xFFC2185B), // 玫红
        Color(0xFFE91E63), // 粉红
        Color(0xFFFF4081), // 亮粉
        Color(0xFFFF6B9D), // 浅粉（内）
        Color(0xFFFFA726)  // 火焰橙（高光）
    )
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            
            // 封面尺寸
            val coverSize = 260.dp.toPx()
            val halfCover = coverSize / 2
            
            // 频谱条尺寸 - 超超超大版本
            val barWidth = 10f
            val maxBarHeight = 180f
            val spacing = 2f
            val gap = 6f // 与封面的间隙
            
            // 计算总宽度和起始偏移
            val totalBarSpace = barsPerSide * (barWidth + spacing) - spacing
            val offset = (coverSize - totalBarSpace) / 2
            
            // 绘制四条边的频谱
            for (side in 0 until 4) {
                for (i in 0 until barsPerSide) {
                    val barIndex = side * barsPerSide + i
                    val scale = barScales.getOrNull(barIndex)?.value ?: 0.05f
                    val currentBarHeight = maxBarHeight * scale
                    
                    // 根据高度选择渐变色
                    val colorProgress = scale.coerceIn(0f, 1f)
                    val colorIndex = (colorProgress * (neonColors.size - 1)).toInt()
                        .coerceIn(0, neonColors.size - 2)
                    val nextColorIndex = (colorIndex + 1).coerceAtMost(neonColors.size - 1)
                    val colorFraction = colorProgress * (neonColors.size - 1) - colorIndex
                    
                    val barColor = androidx.compose.ui.graphics.lerp(
                        neonColors[colorIndex],
                        neonColors[nextColorIndex],
                        colorFraction
                    )
                    
                    val x: Float
                    val y: Float
                    val rotation: Float
                    
                    when (side) {
                        0 -> { // 上边 - 向上延伸
                            x = centerX - halfCover + offset + i * (barWidth + spacing) + barWidth / 2
                            y = centerY - halfCover - gap - currentBarHeight / 2
                            rotation = 0f
                        }
                        1 -> { // 右边 - 向右延伸
                            x = centerX + halfCover + gap + currentBarHeight / 2
                            y = centerY - halfCover + offset + i * (barWidth + spacing) + barWidth / 2
                            rotation = 90f
                        }
                        2 -> { // 下边 - 向下延伸
                            x = centerX + halfCover - offset - i * (barWidth + spacing) - barWidth / 2
                            y = centerY + halfCover + gap + currentBarHeight / 2
                            rotation = 180f
                        }
                        else -> { // 左边 - 向左延伸
                            x = centerX - halfCover - gap - currentBarHeight / 2
                            y = centerY + halfCover - offset - i * (barWidth + spacing) - barWidth / 2
                            rotation = 270f
                        }
                    }
                    
                    // 绘制频谱条 - 霓虹光感多层效果
                    rotate(rotation, pivot = Offset(x, y)) {
                        // 💫 外层辉光（10-20%透明度）- 深海中的自发光感
                        if (scale > 0.15f) {
                            val glowAlpha = 0.12f * scale
                            // 第一层外发光
                            drawRoundRect(
                                color = barColor.copy(alpha = glowAlpha * 0.5f),
                                topLeft = Offset(x - barWidth / 2 - 6f, y - currentBarHeight / 2 - 6f),
                                size = Size(barWidth + 12f, currentBarHeight + 12f),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                            // 第二层内发光
                            drawRoundRect(
                                color = barColor.copy(alpha = glowAlpha),
                                topLeft = Offset(x - barWidth / 2 - 3f, y - currentBarHeight / 2 - 3f),
                                size = Size(barWidth + 6f, currentBarHeight + 6f),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                        
                        // 🌹 主体条 - 柔和圆角（2-4px）
                        drawRoundRect(
                            color = barColor.copy(alpha = 0.75f + scale * 0.15f),
                            topLeft = Offset(x - barWidth / 2, y - currentBarHeight / 2),
                            size = Size(barWidth, currentBarHeight),
                            cornerRadius = CornerRadius(3f, 3f) // 柔和圆角
                        )
                        
                        // ✨ 内层光芯 - 通透感
                        if (scale > 0.2f) {
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.25f * scale),
                                topLeft = Offset(x - barWidth / 2 + 0.5f, y - currentBarHeight / 2 + 1f),
                                size = Size(barWidth - 1f, currentBarHeight * 0.4f),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }
                        
                        // 🔥 顶部火焰高光
                        if (scale > 0.3f) {
                            val flameHeight = currentBarHeight * 0.15f
                            drawRoundRect(
                                color = neonColors[5].copy(alpha = 0.6f * scale),
                                topLeft = Offset(x - barWidth / 2 + 0.5f, y - currentBarHeight / 2),
                                size = Size(barWidth - 1f, flameHeight),
                                cornerRadius = CornerRadius(1.5f, 1.5f)
                            )
                        }
                    }
                }
            }
            // 四角装饰已删除
        }
    }
}



// ═══ 沉浸式歌词 ═══
@Composable
private fun LyricsViewImmersive(
    lyrics: List<LyricLine>,
    currentIndex: Int,
    baseBg: Color,
    topPadding: Dp = 8.dp,
    bottomPadding: Dp = 8.dp,
    currentLyricColor: Color? = null,
    normalLyricColor: Color? = null,
    lyricFontSize: Int = 0,
    onSeekTo: ((Long) -> Unit)? = null,
) {
    Box(Modifier.fillMaxSize()) {
        if (lyrics.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.3f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("\u7eaf\u97f3\u4e50\uff0c\u8bf7\u6b23\u8d4f", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(0.5f), textAlign = TextAlign.Center)
                }
            }
            return
        }

        val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, currentIndex))
        var containerHeight by remember { mutableIntStateOf(0) }
        var userScrolling by remember { mutableStateOf(false) }
        var lastScrolledIndex by remember { mutableIntStateOf(-1) }
        var isFirstLayout by remember { mutableStateOf(true) }

        var isDragging by remember { mutableStateOf(false) }
        var dragTargetIndex by remember { mutableIntStateOf(currentIndex) }

        // 在Composable上下文中获取density
        val density = LocalDensity.current.density

        // 切歌时立即重置滚动状态，避免旧歌词慢慢滚动到新位置
        LaunchedEffect(lyrics) {
            isFirstLayout = true
            lastScrolledIndex = -1
            userScrolling = false
            if (lyrics.isNotEmpty()) {
                listState.scrollToItem(0)
            }
        }

        // Detect user manual scroll vs auto scroll
        LaunchedEffect(listState.isScrollInProgress) {
            if (listState.isScrollInProgress && lastScrolledIndex != currentIndex) {
                userScrolling = true
            }
        }
        LaunchedEffect(userScrolling) {
            if (userScrolling) {
                delay(3000L)
                userScrolling = false
            }
        }

        // Pure smooth scroll — no instant jumps
        LaunchedEffect(currentIndex) {
            if (containerHeight <= 0 || currentIndex !in lyrics.indices || userScrolling) return@LaunchedEffect
            if (lastScrolledIndex == currentIndex) return@LaunchedEffect
            lastScrolledIndex = currentIndex

            // First time or song change: instant position without animation
            if (isFirstLayout) {
                isFirstLayout = false
                listState.scrollToItem(currentIndex, -containerHeight / 2)
                return@LaunchedEffect
            }

            // Check if target is already visible — if so, just smooth-scroll the delta
            val visibleInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
            if (visibleInfo != null) {
                val itemCenter = visibleInfo.offset + visibleInfo.size / 2
                val screenCenter = containerHeight / 2
                val delta = (itemCenter - screenCenter).toFloat()
                if (kotlin.math.abs(delta) > 1f) {
                    listState.animateScrollBy(delta, tween(500, easing = FastOutSlowInEasing))
                }
            } else {
                // Target not visible — jump close first, then smooth adjust
                listState.scrollToItem(maxOf(0, currentIndex - 2))
                // Wait for layout to settle
                delay(16)
                val info = listState.layoutInfo.visibleItemsInfo.find { it.index == currentIndex }
                if (info != null) {
                    val itemCenter = info.offset + info.size / 2
                    val screenCenter = containerHeight / 2
                    val delta = (itemCenter - screenCenter).toFloat()
                    listState.animateScrollBy(delta, tween(300, easing = FastOutSlowInEasing))
                }
            }
        }

        val baseFontSize = if (lyricFontSize > 0) lyricFontSize.toFloat() else 17f
        val isLightBg = baseBg.luminance() > 0.6f
        val currentColor = currentLyricColor ?: if (isLightBg) Color(0xFF16A34A) else Color(0xFF22C55E)
        val normalColor = normalLyricColor ?: if (isLightBg) Color(0xFF2D2D2D) else Color.White

        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        if (onSeekTo == null) return@detectVerticalDragGestures
                        isDragging = true
                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        val targetItem = visibleItems.findLast { it.offset < offset.y }
                        if (targetItem != null) {
                            dragTargetIndex = targetItem.index.coerceIn(0, lyrics.size - 1)
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragTargetIndex in lyrics.indices && onSeekTo != null) {
                            onSeekTo(lyrics[dragTargetIndex].timeMs)
                        }
                    },
                    onDragCancel = { isDragging = false },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (!isDragging || onSeekTo == null) return@detectVerticalDragGestures

                        val visibleItems = listState.layoutInfo.visibleItemsInfo
                        if (visibleItems.isNotEmpty()) {
                            val centerY = containerHeight / 2f
                            val closestItem = visibleItems.minByOrNull { kotlin.math.abs((it.offset + it.size / 2f) - centerY) }
                            if (closestItem != null) {
                                val avgItemHeight = if (visibleItems.size > 1) {
                                    kotlin.math.abs(visibleItems[1].offset - visibleItems[0].offset).toFloat()
                                } else {
                                    60f * density
                                }
                                val indexDelta = (-dragAmount / avgItemHeight).toInt()
                                dragTargetIndex = (closestItem.index + indexDelta).coerceIn(0, lyrics.size - 1)
                            }
                        }
                    }
                )
            }
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { containerHeight = it.size.height },
                state = listState,
                contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            items(lyrics.size, key = { "${it}_${lyrics[it].timeMs}" }) { index ->
                val line = lyrics[index]
                val cur = index == currentIndex
                val isDragTarget = isDragging && index == dragTargetIndex
                val distance = kotlin.math.abs(index - currentIndex)
                val alpha = when {
                    isDragTarget -> 1f
                    cur -> 1f
                    distance == 1 -> 0.6f
                    distance == 2 -> 0.4f
                    distance == 3 -> 0.3f
                    else -> 0.2f
                }

                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(0.9f)) {
                        if (isDragTarget) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    line.text,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontSize = baseFontSize.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = (baseFontSize * 1.4f).sp
                                    ),
                                    color = currentColor,
                                    textAlign = TextAlign.Start,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        fmt(line.timeMs),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = currentColor.copy(alpha = 0.8f),
                                        fontSize = 13.sp
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.PlayArrow, null, tint = currentColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Text(
                                line.text,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontSize = baseFontSize.sp,
                                    fontWeight = if (cur) FontWeight.Bold else FontWeight.Normal,
                                    lineHeight = (baseFontSize * 1.4f).sp
                                ),
                                color = if (cur) currentColor else normalColor.copy(alpha = alpha),
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (line.ttext.isNotEmpty() && !isDragging) {
                                Text(
                                    line.ttext,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = (baseFontSize * 0.82f).sp,
                                        fontWeight = FontWeight.Normal,
                                        lineHeight = (baseFontSize * 1.2f).sp
                                    ),
                                    color = if (cur) currentColor.copy(alpha = 0.7f) else normalColor.copy(alpha = alpha * 0.6f),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

private data class CoverColors(
    val base: Color,
    val vibrant: Color,
    val accent: Color,
    val secondary: Color,
    val dominant: Color,
    val muted: Color,
    val lightAccent: Color,
)

private val DefaultCoverColors = CoverColors(
    base = Color(0xFF1A1A1A),
    vibrant = Color(0xFF2A4A8A),
    accent = Color(0xFF6A3A8A),
    secondary = Color(0xFF1A2A3A),
    dominant = Color(0xFF222222),
    muted = Color(0xFF2A2A3A),
    lightAccent = Color(0xFF3A5A7A),
)

/** 轻微降低亮度但保留真实色相和饱和度 */
private fun darkenFaithful(rgb: Int, maxV: Float = 0.35f, minS: Float = 0.20f): Int {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(rgb, hsv)
    hsv[1] = hsv[1].coerceAtLeast(minS)          // 保证最低饱和度
    hsv[2] = hsv[2].coerceAtMost(maxV)            // 降低亮度供背景使用
    return AndroidColor.HSVToColor(hsv)
}

@Composable
private fun rememberCoverColors(coverUrl: String): CoverColors {
    val context = LocalContext.current
    var colors by remember(coverUrl) { mutableStateOf(DefaultCoverColors) }

    LaunchedEffect(coverUrl) {
        if (coverUrl.isBlank()) { colors = DefaultCoverColors; return@LaunchedEffect }

        try {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(coverUrl)
                .allowHardware(false)
                .size(256) // 小尺寸足够提取颜色，更快
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return@LaunchedEffect
                val palette = Palette.from(bitmap)
                    .maximumColorCount(24)   // 更多色板槽位 → 更真实
                    .generate()

                val vibrantSw    = palette.vibrantSwatch
                val darkVibrantSw = palette.darkVibrantSwatch
                val mutedSw      = palette.mutedSwatch
                val darkMutedSw  = palette.darkMutedSwatch
                val lightVibSw   = palette.lightVibrantSwatch
                val lightMutSw   = palette.lightMutedSwatch
                val dominantSw   = palette.dominantSwatch

                // ── base: 主背景 ──
                val baseRgb = darkenFaithful(
                    darkVibrantSw?.rgb ?: dominantSw?.rgb ?: 0xFF1A1A1A.toInt(),
                    maxV = 0.30f, minS = 0.22f
                )

                // ── vibrant: 鲜艳色 ──
                val vibRgb = darkenFaithful(
                    vibrantSw?.rgb ?: lightVibSw?.rgb ?: baseRgb,
                    maxV = 0.45f, minS = 0.30f
                )

                // ── accent: lightVibrant / muted ──
                val accentRgb = darkenFaithful(
                    lightVibSw?.rgb ?: mutedSw?.rgb ?: vibrantSw?.rgb ?: baseRgb,
                    maxV = 0.40f, minS = 0.22f
                )

                // ── secondary: muted 真实色 ──
                val secRgb = darkenFaithful(
                    mutedSw?.rgb ?: darkMutedSw?.rgb ?: baseRgb,
                    maxV = 0.28f, minS = 0.18f
                )

                // ── dominant: 封面占比最大色 ──
                val domRgb = darkenFaithful(
                    dominantSw?.rgb ?: darkMutedSw?.rgb ?: baseRgb,
                    maxV = 0.32f, minS = 0.18f
                )

                // ── muted: 柔和色调 ──
                val mutRgb = darkenFaithful(
                    darkMutedSw?.rgb ?: mutedSw?.rgb ?: baseRgb,
                    maxV = 0.25f, minS = 0.12f
                )

                // ── lightAccent: 点缀色 ──
                val laRgb = darkenFaithful(
                    lightVibSw?.rgb ?: lightMutSw?.rgb ?: vibrantSw?.rgb ?: baseRgb,
                    maxV = 0.50f, minS = 0.25f
                )

                colors = CoverColors(
                    base = Color(baseRgb),
                    vibrant = Color(vibRgb),
                    accent = Color(accentRgb),
                    secondary = Color(secRgb),
                    dominant = Color(domRgb),
                    muted = Color(mutRgb),
                    lightAccent = Color(laRgb),
                )
            }
        } catch (_: Exception) {}
    }

    return colors
}

@Composable
private fun QualityBadge(viewModel: MusicViewModel) {
    var showQualityMenu by remember { mutableStateOf(false) }
    val qColor = when (viewModel.selectedQuality) {
        MusicApiConfig.Quality.STANDARD -> Color(0xFFAAAAAA)
        MusicApiConfig.Quality.EXHIGH -> Color(0xFF4A90D9)
        MusicApiConfig.Quality.LOSSLESS -> Color(0xFF4CAF50)
    }
    Box {
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(qColor.copy(0.15f))
                .clickable { showQualityMenu = true }
                .padding(horizontal = 12.dp, vertical = 5.dp)
        ) {
            Text(viewModel.selectedQuality.label, fontSize = 11.sp, color = qColor, fontWeight = FontWeight.SemiBold)
        }
        DropdownMenu(
            expanded = showQualityMenu,
            onDismissRequest = { showQualityMenu = false },
            modifier = Modifier.background(Color(0xFF1E1E1E))
        ) {
            MusicApiConfig.Quality.entries.forEach { quality ->
                val qualityColor = when (quality) {
                    MusicApiConfig.Quality.STANDARD -> Color(0xFFAAAAAA)
                    MusicApiConfig.Quality.EXHIGH -> Color(0xFF4A90D9)
                    MusicApiConfig.Quality.LOSSLESS -> Color(0xFF4CAF50)
                }
                DropdownMenuItem(
                    onClick = { viewModel.setQuality(quality); showQualityMenu = false },
                    text = { Text(quality.label, color = qualityColor) }
                )
            }
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 4.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp)); Spacer(Modifier.width(16.dp)); Text(title, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun fmt(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }

// ═══ 滚动倒计时 (参考 React Counter 组件的滚动数字动画) ═══

@Composable
private fun SleepTimerCounter(remainingMs: Long, onCancel: () -> Unit) {
    val totalSec = (remainingMs / 1000).toInt()
    val minutes = totalSec / 60
    val seconds = totalSec % 60

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White.copy(alpha = 0.10f))
            .clickable { onCancel() }
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Timer, null, tint = NebulaViolet.copy(0.8f), modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(8.dp))

        val fs = 24.sp
        val col = Color.White

        RollingDigit(targetDigit = minutes / 10, fontSize = fs, color = col)
        RollingDigit(targetDigit = minutes % 10, fontSize = fs, color = col)
        Text(":", fontSize = fs, color = col.copy(0.35f), fontWeight = FontWeight.Light, modifier = Modifier.padding(horizontal = 1.dp))
        RollingDigit(targetDigit = seconds / 10, fontSize = fs, color = col)
        RollingDigit(targetDigit = seconds % 10, fontSize = fs, color = col)

        Spacer(Modifier.width(8.dp))
        Icon(Icons.Default.Close, null, tint = Color.White.copy(0.25f), modifier = Modifier.size(12.dp))
    }
}

/**
 * 单个滚动数字 — 参考 React Counter 的 Number/Digit 组件
 * 10个数字(0-9)堆叠排列，通过 Y 轴偏移 + 弹性动画实现滚动切换
 * 使用连续累加值 + mod 10 实现自然方向的翻滚 (避免 0→9 穿越中间数字)
 */
@Composable
private fun RollingDigit(targetDigit: Int, fontSize: androidx.compose.ui.unit.TextUnit, color: Color) {
    // 连续累加目标值 (可以 < 0)，用于正确方向的滚动
    val continuousTarget = remember { mutableFloatStateOf(targetDigit.toFloat()) }
    val animValue = remember { Animatable(targetDigit.toFloat()) }
    var prevDigit by remember { mutableIntStateOf(targetDigit) }

    LaunchedEffect(targetDigit) {
        if (targetDigit != prevDigit) {
            // 计算最短滚动路径 (类似 React Counter 的 offset 逻辑)
            val forward = ((targetDigit - prevDigit) + 10) % 10
            val backward = forward - 10
            val step = if (forward < kotlin.math.abs(backward)) forward.toFloat() else backward.toFloat()

            continuousTarget.floatValue += step
            prevDigit = targetDigit
            animValue.animateTo(
                continuousTarget.floatValue,
                spring(dampingRatio = 0.75f, stiffness = 100f),
            )
        }
    }

    val density = LocalDensity.current
    val digitHeightPx = with(density) { fontSize.toPx() * 1.3f }
    val digitHeightDp = with(density) { digitHeightPx.toDp() }
    val charWidthDp = with(density) { (fontSize.toPx() * 0.62f).toDp() }

    Box(
        modifier = Modifier
            .width(charWidthDp)
            .height(digitHeightDp)
            .clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        val current = animValue.value
        for (number in 0..9) {
            // 与 React 的 Number 组件相同的偏移计算
            val placeDigit = ((current % 10f) + 10f) % 10f
            val rawOffset = ((10f + number - placeDigit) % 10f)
            val offset = if (rawOffset > 5f) rawOffset - 10f else rawOffset
            val yPx = offset * digitHeightPx

            if (kotlin.math.abs(yPx) <= digitHeightPx * 2f) {
                val alpha = (1f - kotlin.math.abs(offset) / 2.5f).coerceIn(0.12f, 1f)
                Text(
                    "$number",
                    fontSize = fontSize,
                    color = color.copy(alpha = alpha),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset { IntOffset(0, yPx.toInt()) },
                )
            }
        }
    }
}

// ═══ 时光模式播放器（日落风景背景）═══
@Composable
private fun TimelapsePlayer(
    song: Song,
    viewModel: MusicViewModel,
    rotation: Float,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>
) {
    // 暖色调渐变背景
    val timeBasedGradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFB347).copy(alpha = 0.9f),  // 暖橙
            Color(0xFFFFCC33).copy(alpha = 0.8f),  // 金黄
            Color(0xFFFF6B6B).copy(alpha = 0.7f),  // 珊瑚红
            Color(0xFF4A5568).copy(alpha = 0.9f),  // 深灰蓝
        ),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    Box(Modifier.fillMaxSize()) {
        // 背景渐变
        Box(Modifier.fillMaxSize().background(timeBasedGradient))

        // 装饰性圆形（模拟太阳/月亮）
        Box(
            Modifier
                .size(200.dp)
                .offset(y = (-50).dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE4B5).copy(alpha = 0.6f),
                            Color(0xFFFFB347).copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(Modifier.fillMaxSize()) {
            // 顶部栏
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                }
                Spacer(Modifier.width(48.dp))
            }

            // 唱片区域 - 使用透明边框效果
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 外圈光晕
                Box(
                    Modifier
                        .size(340.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )

                // 旋转唱片
                Box(
                    Modifier
                        .size(300.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .background(Color(0xFF2D3748).copy(alpha = 0.8f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (song.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            song.coverUrl,
                            null,
                            Modifier
                                .size(200.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = Color.White.copy(0.5f), modifier = Modifier.size(80.dp))
                    }
                }

                // 飞鸟装饰（简单的V形图标模拟）
                Row(Modifier.offset(y = (-120).dp)) {
                    repeat(3) { i ->
                        Text(
                            "V",
                            modifier = Modifier.offset(x = (i * 20 - 20).dp),
                            color = Color(0xFF2D3748).copy(alpha = 0.4f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // 底部控制 - 半透明毛玻璃效果
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .background(Color(0xFF2D3748).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                MinimalBottomControls(viewModel, dur, showQueue)
            }
        }
    }
}

// ═══ 青春彩胶播放器（透明唱片+弥散烟雾）═══
@Composable
private fun ColorVinylPlayer(
    song: Song,
    viewModel: MusicViewModel,
    rotation: Float,
    onBack: () -> Unit,
    dur: Long,
    showQueue: MutableState<Boolean>
) {
    val coverColors = rememberCoverColors(song.coverUrl)

    Box(Modifier.fillMaxSize()) {
        // 基于封面的多色渐变背景
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to coverColors.vibrant.copy(alpha = 0.35f),
                        0.5f to coverColors.base.copy(alpha = 0.25f),
                        1f to coverColors.secondary.copy(alpha = 0.30f),
                    )
                )
        )

        // 弥散烟雾效果（多层模糊圆形）
        val infiniteTransition = rememberInfiniteTransition(label = "smoke")
        val smokeOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
            label = "smoke"
        )

        // 烟雾层1 - 使用封面鲜艳色
        Box(
            Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (50 + smokeOffset * 20).dp)
                .alpha(0.3f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            coverColors.vibrant.copy(alpha = 0.5f),
                            coverColors.accent.copy(alpha = 0.3f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        // 烟雾层2 - 使用封面次要色
        Box(
            Modifier
                .size(350.dp)
                .offset(x = 150.dp, y = (100 - smokeOffset * 15).dp)
                .alpha(0.25f)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            coverColors.secondary.copy(alpha = 0.4f),
                            coverColors.base.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    ),
                    CircleShape
                )
        )

        Column(Modifier.fillMaxSize()) {
            // 顶部栏
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(26.dp)) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(song.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                    Text(song.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                }
                Spacer(Modifier.width(48.dp))
            }

            // 透明唱片区域
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                // 外圈彩虹光晕
                Box(
                    Modifier
                        .size(320.dp)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFF9AA2).copy(alpha = 0.3f),
                                    Color(0xFFB5EAD7).copy(alpha = 0.3f),
                                    Color(0xFFE2F0CB).copy(alpha = 0.3f),
                                    Color(0xFFFFDAC1).copy(alpha = 0.3f),
                                    Color(0xFFFF9AA2).copy(alpha = 0.3f),
                                )
                            ),
                            CircleShape
                        )
                        .graphicsLayer { rotationZ = rotation * 0.5f }
                )

                // 透明质感唱片
                Box(
                    Modifier
                        .size(280.dp)
                        .graphicsLayer { rotationZ = rotation }
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // 封面
                    if (song.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            song.coverUrl,
                            null,
                            Modifier
                                .size(180.dp)
                                .clip(CircleShape)
                                .border(4.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 内圈高光
                    Box(
                        Modifier
                            .size(160.dp)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    ) {}
                }
            }

            // 底部控制 - 半透明毛玻璃效果
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .background(Color(0xFF2D3748).copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                MinimalBottomControls(viewModel, dur, showQueue)
            }
        }
    }
}

// ... (其他代码保持不变)
