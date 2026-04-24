package com.yindong.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yindong.music.data.api.LinkParser
import com.yindong.music.viewmodel.MusicViewModel

@Composable
fun ImportPlaylistScreen(
    viewModel: MusicViewModel,
    onBack: () -> Unit,
    onNavigateToPlaylist: (Long) -> Unit,
) {
    var inputText by remember { mutableStateOf("") }
    var parseError by remember { mutableStateOf<String?>(null) }
    var showFaq by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current
    val cs = MaterialTheme.colorScheme
    val importState = viewModel.importState
    val isLoading = importState is MusicViewModel.ImportState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, bottom = 160.dp) // Added bottom padding
    ) {
        // ── 标题栏 ──
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                viewModel.cancelImport()
                onBack()
            }) {
                Icon(Icons.Default.ArrowBack, "返回", tint = cs.onBackground)
            }
            Column {
                Text(
                    "导入外部歌单",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = cs.onBackground,
                )
                Text(
                    "支持从酷我音乐、酷狗音乐、网易云音乐、QQ音乐导入歌单",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── 使用提示 ──
        InfoTip("打开对应平台App，找到想导入的歌单，点击分享并复制链接，回到本App粘贴即可一键导入。")
        Spacer(Modifier.height(8.dp))
        InfoTip("仅支持导入官方公开歌单，私密歌单或需登录才能查看的歌单暂无法识别，请先将歌单设为公开。")

        Spacer(Modifier.height(24.dp))

        // ── 输入框 ──
        OutlinedTextField(
            value = inputText,
            onValueChange = {
                inputText = it
                parseError = null
                if (importState is MusicViewModel.ImportState.Error) viewModel.resetImportState()
            },
            placeholder = { Text("粘贴歌单链接或歌单分享口令") },
            trailingIcon = {
                IconButton(onClick = {
                    val clip = clipboardManager.getText()?.text ?: ""
                    if (clip.isNotEmpty()) {
                        inputText = clip
                        parseError = null
                    }
                }) {
                    Icon(Icons.Default.ContentPaste, "粘贴", tint = cs.onSurfaceVariant)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3,
            shape = RoundedCornerShape(12.dp),
        )

        // ── 错误提示 ──
        parseError?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (importState is MusicViewModel.ImportState.Error) {
            Spacer(Modifier.height(8.dp))
            Text(
                (importState as MusicViewModel.ImportState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(20.dp))

        // ── 操作按钮 ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = {
                    parseError = null
                    val parsed = LinkParser.parse(inputText)
                    if (parsed == null) {
                        parseError = "无法识别歌单链接，请检查输入是否正确"
                        return@Button
                    }
                    viewModel.importExternalPlaylist(parsed.platform, parsed.playlistId)
                },
                enabled = inputText.isNotBlank() && !isLoading,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary.copy(alpha = 0.9f),
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("导入中...")
                } else {
                    Text("开始导入")
                }
            }

            OutlinedButton(
                onClick = {
                    viewModel.cancelImport()
                    onBack()
                },
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text("取消导入")
            }
        }

        // ── 导入成功结果 ──
        if (importState is MusicViewModel.ImportState.Success) {
            val success = importState as MusicViewModel.ImportState.Success
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4CAF50).copy(alpha = 0.1f))
                    .clickable {
                        viewModel.resetImportState()
                        onNavigateToPlaylist(success.playlistId)
                    }
                    .padding(16.dp),
            ) {
                Column {
                    Text(
                        "导入成功",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "歌单「${success.playlistName}」已导入 ${success.songCount} 首歌曲",
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "点击查看歌单 →",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50),
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // ── FAQ 常见问题 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { showFaq = !showFaq }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Info, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("常见问题", style = MaterialTheme.typography.titleSmall, color = cs.onBackground)
            Spacer(Modifier.weight(1f))
            Icon(
                if (showFaq) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint = cs.onSurfaceVariant,
            )
        }

        AnimatedVisibility(
            visible = showFaq,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(modifier = Modifier.padding(start = 28.dp)) {
                FaqItem(
                    "如何获取歌单链接？",
                    "打开对应平台的App或网页版，进入歌单详情页，点击「分享」按钮，选择「复制链接」，粘贴到输入框中即可开始导入。",
                )
                Spacer(Modifier.height(16.dp))
                FaqItem(
                    "为什么导入失败？",
                    "请检查链接是否完整、歌单是否为公开状态，以及当前网络是否正常。若链接来自浏览器，请复制完整地址栏内容后重试。",
                )
                Spacer(Modifier.height(16.dp))
                FaqItem(
                    "导入后歌曲不全怎么办？",
                    "部分歌曲可能因版权限制或已下架而无法匹配成功。你可在导入结果中查看未成功的曲目，尝试手动搜索添加。",
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun InfoTip(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = cs.onSurfaceVariant,
            modifier = Modifier.size(16.dp).padding(top = 2.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, lineHeight = 18.sp)
    }
}

@Composable
private fun FaqItem(title: String, content: String) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = cs.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
            lineHeight = 18.sp,
        )
    }
}
