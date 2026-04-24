package com.yindong.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yindong.music.ui.theme.Red500
import com.yindong.music.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    viewModel: MusicViewModel,
    showQueue: Boolean,
    onDismiss: () -> Unit,
) {
    if (!showQueue) return

    var selectedTab by remember { mutableStateOf("当前播放") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                            .background(if (selectedTab == tab) Red500 else Color(0xFF333333))
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
                                        if (!viewModel.isFavorite(song)) {
                                            viewModel.toggleLike(song)
                                        }
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
                            .clickable { }
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
                                            onDismiss()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 序号
                                    Text(
                                        "${index + 1}",
                                        modifier = Modifier.width(36.dp),
                                        color = if (isCurrent) Red500 else Color.White.copy(0.5f),
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
                                            onDismiss()
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
