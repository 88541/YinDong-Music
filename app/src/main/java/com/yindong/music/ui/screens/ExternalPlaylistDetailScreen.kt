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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yindong.music.data.model.Song
import com.yindong.music.ui.components.AddToPlaylistSheet
import com.yindong.music.ui.components.SongItem
import com.yindong.music.ui.theme.CoverGradients
import com.yindong.music.ui.theme.DividerColor
import com.yindong.music.ui.theme.Red500
import com.yindong.music.ui.theme.TextPrimary
import com.yindong.music.ui.theme.TextSecondary
import com.yindong.music.ui.theme.TextTertiary
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun ExternalPlaylistDetailScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onSaveToMine: () -> Unit,
    onPlayerClick: () -> Unit,
) {
    val playlist = viewModel.externalViewPlaylist
    val isLoading = viewModel.isExternalViewLoading

    var songToAdd by remember { mutableStateOf<Song?>(null) }
    songToAdd?.let { song ->
        AddToPlaylistSheet(
            song = song,
            playlists = viewModel.userPlaylists,
            onSelect = { targetId -> viewModel.addSongToPlaylist(targetId, song) },
            onDismiss = { songToAdd = null },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Red500, strokeWidth = 3.dp)
                    Spacer(Modifier.height(16.dp))
                    Text("加载歌单中...", color = TextSecondary, fontSize = 13.sp)
                }
            }
        } else if (playlist == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("歌单加载失败", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else {
            val coverColors = remember(playlist.name) {
                CoverGradients[(playlist.name.hashCode() and 0x7FFFFFFF) % CoverGradients.size]
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                // Header
                item {
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White)
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
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
                                        Icon(
                                            Icons.Default.MusicNote, null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp),
                                        )
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(
                                        playlist.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "来源: ${playlist.creator}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        "${playlist.songs.size} 首歌曲",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary.copy(alpha = 0.7f),
                                    )
                                }
                            }

                            // Save button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Red500)
                                        .clickable { onSaveToMine() }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                ) {
                                    Icon(Icons.Default.Bookmark, "保存", tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("保存到我的歌单", style = MaterialTheme.typography.bodySmall, color = Color.White, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                // Play all
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (playlist.songs.isNotEmpty()) {
                                        viewModel.playAllFromList(playlist.songs)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.PlayCircleFilled, "播放全部", tint = Red500, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("播放全部", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Spacer(Modifier.width(6.dp))
                            Text("(${playlist.songs.size})", style = MaterialTheme.typography.bodySmall, color = TextTertiary)
                        }
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }

                // Songs
                itemsIndexed(playlist.songs) { index, song ->
                    SongItem(
                        song = song,
                        index = index,
                        onSongClick = {
                            viewModel.playPlaylist(playlist.songs, index)
                        },
                        onMoreClick = { songToAdd = song },
                    )
                }

                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
