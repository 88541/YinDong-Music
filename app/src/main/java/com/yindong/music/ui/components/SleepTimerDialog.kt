package com.yindong.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yindong.music.ui.theme.Red500
import com.yindong.music.viewmodel.MusicViewModel

/**
 * 睡眠定时器弹窗 — 可在任何页面复用
 * 显示定时选项或正在倒计时的状态
 */
@Composable
fun SleepTimerDialog(viewModel: MusicViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Timer, null, tint = Red500) },
        title = { Text("定时停止播放") },
        text = {
            Column {
                if (viewModel.isSleepTimerRunning) {
                    val totalSec = (viewModel.sleepTimerMs / 1000).toInt()
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    Box(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Red500.copy(alpha = 0.06f)).padding(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "正在倒计时",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "%d:%02d".format(min, sec),
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = Red500,
                            )
                            Spacer(Modifier.height(16.dp))
                            Row(
                                Modifier.clip(RoundedCornerShape(20.dp))
                                    .background(Red500.copy(0.12f))
                                    .clickable { viewModel.cancelSleepTimer(); onDismiss() }
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.TimerOff, null, tint = Red500, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("取消定时", color = Red500, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else {
                    Text(
                        "到时间后自动暂停音乐，适合睡前使用",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        15 to "15 分钟",
                        30 to "30 分钟",
                        45 to "45 分钟",
                        60 to "1 小时",
                        90 to "1.5 小时",
                    ).forEach { (minutes, label) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.startSleepTimer(minutes); onDismiss() }
                                .padding(horizontal = 8.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(14.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
