package com.yindong.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Input
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yindong.music.data.model.Playlist
import com.yindong.music.data.model.Song
import com.yindong.music.ui.components.AddToPlaylistSheet
import com.yindong.music.ui.components.SongItem
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.DarkSurfaceVariant
import com.yindong.music.ui.theme.DividerColor
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.TextTertiary
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onPlayerClick: () -> Unit,
) {
    val playlist = viewModel.getUserPlaylistById(playlistId) ?: return
    val songs = playlist.songs

    // 添加到歌单弹窗
    var songToAdd by remember { mutableStateOf<Song?>(null) }
    songToAdd?.let { song ->
        AddToPlaylistSheet(
            song = song,
            playlists = viewModel.userPlaylists,
            onSelect = { targetId -> viewModel.addSongToPlaylist(targetId, song) },
            onDismiss = { songToAdd = null },
        )
    }

    // 自动为没有封面的歌曲搜索匹配封面
    LaunchedEffect(playlistId) {
        viewModel.fetchMissingCovers(playlistId)
    }

    val coverColors = remember(playlist.id) {
        val idx = ((playlist.id % CoverGradients.size).toInt() + CoverGradients.size) % CoverGradients.size
        CoverGradients[idx]
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // Header
            item {
                PlaylistHeader(
                    playlist = playlist,
                    coverColors = coverColors,
                    onBack = onBack,
                    viewModel = viewModel,
                    onImport = { viewModel.exportPlaylistCopy(playlistId) },
                )
            }

            // Play all
            item {
                PlayAllButton(
                    songCount = songs.size,
                    isLoading = false,
                    onClick = {
                        if (songs.isNotEmpty()) {
                            viewModel.playAllFromList(songs)
                        }
                    },
                )
            }

            // Songs
            itemsIndexed(songs) { index, song ->
                SongItem(
                    song = song,
                    index = index,
                    onSongClick = {
                        viewModel.playPlaylist(songs, index)
                    },
                    onMoreClick = { songToAdd = song },
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    coverColors: List<Color>,
    onBack: () -> Unit,
    viewModel: MusicViewModel,
    onImport: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        coverColors[0].copy(alpha = 0.5f),
                        coverColors[1].copy(alpha = 0.2f),
                        Color.Transparent,
                    )
                )
            )
            .statusBarsPadding(),
    ) {
        Column {
            IconButton(onClick = onBack, modifier = Modifier.padding(4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Color.White)
            }

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                Box(
                    modifier = Modifier.size(130.dp).clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(coverColors)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (playlist.coverUrl.isNotEmpty()) {
                        AsyncImage(
                            model = playlist.coverUrl,
                            contentDescription = playlist.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        val songCovers = playlist.songs.filter { it.coverUrl.isNotEmpty() }.map { it.coverUrl }.distinct().take(4)
                        if (songCovers.size >= 4) {
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
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(playlist.name, style = MaterialTheme.typography.titleLarge, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(DarkSurfaceVariant), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(14.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(playlist.creator, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (playlist.description.isNotEmpty()) {
                        Text(playlist.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary.copy(alpha = 0.7f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        playlist.tags.take(3).forEach { tag ->
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                            ) {
                                Text(tag, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            // Action chips (functional)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                ActionChip(Icons.Default.Input, "导出") { onImport() }
                ActionChip(Icons.Default.Share, "分享") { viewModel.showToast("分享功能即将上线") }
                ActionChip(Icons.Default.Download, "下载") { viewModel.showToast("下载功能即将上线") }
                ActionChip(Icons.Default.CheckCircle, "已收藏") { viewModel.showToast("已收藏") }
            }
        }
    }
}

@Composable
private fun ActionChip(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(DarkSurfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Icon(icon, contentDescription = label, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun PlayAllButton(songCount: Int, isLoading: Boolean, onClick: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.PlayCircleFilled, contentDescription = "播放全部", tint = Red500, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("播放全部", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(modifier = Modifier.width(6.dp))
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Red500, strokeWidth = 2.dp)
            } else {
                Text("($songCount)", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
            }

            Spacer(modifier = Modifier.weight(1f))

            Icon(Icons.Default.Sort, contentDescription = "排序", tint = TextTertiary, modifier = Modifier.size(20.dp))
        }

        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
    }
}
