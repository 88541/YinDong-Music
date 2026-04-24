package com.yindong.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.yindong.music.ui.theme.AccentCyan
import com.yindong.music.ui.theme.GlassBorder
import com.yindong.music.ui.theme.GlassDarkSurface
import com.yindong.music.ui.theme.GlassLightSurface
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.isDarkTheme
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun MiniPlayer(
    viewModel: MusicViewModel,
    onPlayerClick: () -> Unit,
    onQueueClick: () -> Unit = {},
) {
    val song = viewModel.currentSong ?: return
    val isDark = isDarkTheme()
    val backgroundColor = if (isDark) GlassDarkSurface else GlassLightSurface

    // 进度动画 - 使用ViewModel的progress属性（0-1之间的值）
    val progress by animateFloatAsState(
        targetValue = viewModel.progress,
        animationSpec = tween(100),
        label = "progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // 玻璃态MiniPlayer容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            backgroundColor.copy(alpha = 0.95f),
                            backgroundColor.copy(alpha = 0.85f)
                        )
                    )
                )
                .drawBehind {
                    // 顶部发光边框
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                GlassBorder.copy(alpha = 0.5f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = 2f
                        )
                    )
                }
                .clickable { onPlayerClick() },
            contentAlignment = Alignment.Center
        ) {
            Column {
                // 进度条
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = PrimaryPurple,
                    trackColor = Color.Transparent,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .padding(start = 12.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 专辑封面 - 带发光效果
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        PrimaryPurple.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
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
                                    contentDescription = null,
                                    tint = PrimaryPurple,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // 歌曲信息
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            song.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${song.artist} · ${song.platform}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // 播放控制按钮
                    IconButton(onClick = { viewModel.togglePlayPause() }) {
                        if (viewModel.isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = PrimaryPurple,
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                if (viewModel.isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                contentDescription = if (viewModel.isPlaying) "暂停" else "播放",
                                tint = PrimaryPurple,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                    }

                    IconButton(onClick = { onQueueClick() }) {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = "播放列表",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}
