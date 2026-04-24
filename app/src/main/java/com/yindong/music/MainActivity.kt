package com.yindong.music

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.yindong.music.ui.theme.AppTheme
import com.yindong.music.ui.theme.AppThemes
import com.yindong.music.ui.theme.ThemeManager
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.Modifier
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.yindong.music.data.LocalStorage
import com.yindong.music.data.RemoteConfig
import com.yindong.music.data.UpdateManager
import com.yindong.music.security.CriticalUiProtector
import com.yindong.music.security.SecurityGuard
import com.yindong.music.ui.navigation.AppNavigation
import com.yindong.music.ui.screens.DisclaimerScreen
import com.yindong.music.ui.theme.CloudMusicTheme
import com.yindong.music.viewmodel.MusicViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val musicViewModel: MusicViewModel by viewModels()
    private var lastExternalPluginIntent: String? = null
    private var updateManager: UpdateManager? = null
    private var criticalUiWatchdogJob: Job? = null
    private var officialContactFallbackShown = false
    private var lastUiTamperReason: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 全屏延伸到系统栏（状态栏叠加显示）
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Android 13+ 请求通知权限（媒体控制通知必须）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        // Android 12+ 请求蓝牙连接权限（检测蓝牙耳机需要）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1002)
            }
        }
        
        // 🛡️ 安全加固检查
        performSecurityCheck()
        startCriticalUiWatchdog()

        // LocalStorage 已在 CloudMusicApplication.onCreate() 中初始化
        // 这里做一次安全兄底，防止极端情况
        LocalStorage.init(this)
        
        // 从服务器拉取远程配置 (控制APP一切行为)
        fetchRemoteConfig()

        // 支持从外部应用直接"打开/分享" .js 插件并自动导入
        handleExternalPluginIntent(intent)
        
        setContent {
            val viewModel = musicViewModel
            var disclaimerAccepted by remember {
                mutableStateOf(LocalStorage.loadDisclaimerAccepted())
            }

            if (!disclaimerAccepted) {
                DisclaimerScreen(
                    onAccept = {
                        LocalStorage.saveDisclaimerAccepted(true)
                        disclaimerAccepted = true
                    },
                    onDecline = { finish() },
                )
            } else {
                var currentTheme by remember {
                    mutableStateOf(ThemeManager.getCurrentTheme(this@MainActivity))
                }

                LaunchedEffect(Unit) {
                    ThemeManager.initTheme(this@MainActivity)
                    currentTheme = ThemeManager.getCurrentTheme(this@MainActivity)
                }
                LaunchedEffect(Unit) {
                    ThemeManager.themeState.collectLatest { theme ->
                        currentTheme = theme
                    }
                }

                CloudMusicTheme(appTheme = currentTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }

    private fun isUiEntryMissingReason(reason: String): Boolean {
        return reason.startsWith("mine_community_entry_missing") ||
            reason.startsWith("mine_critical_button_missing")
    }

    private fun showOfficialContactFallbackDialog(trigger: String) {
        if (officialContactFallbackShown) return
        officialContactFallbackShown = true
        val groupQq = RemoteConfig.officialCommunityQq.ifBlank { CriticalUiProtector.communityQqNumber() }
        val joinUrl = RemoteConfig.officialCommunityJoinUrl.ifBlank { CriticalUiProtector.communityJoinUrl() }
        val title = RemoteConfig.officialCommunityTitle
            .ifBlank { CriticalUiProtector.communityEntryTitle() }
            .ifBlank { "QQ官方群" }
        val subtitle = RemoteConfig.officialCommunitySubtitle
            .ifBlank { CriticalUiProtector.communityEntrySubtitle() }
            .ifBlank { "官方群联系方式" }
        val message = buildString {
            append(subtitle)
            append("\n\n官方群QQ：")
            append(groupQq)
            if (trigger != "remote_config") {
                append("\n\n检测到界面异常，已启用服务器兜底展示。")
            }
        }
        val builder = AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
        if (joinUrl.isNotBlank()) {
            builder.setPositiveButton("打开官方群") { _, _ ->
                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(joinUrl))) }
                    .onFailure { Toast.makeText(this, "打开官方群失败", Toast.LENGTH_SHORT).show() }
            }
            builder.setNegativeButton("关闭", null)
        } else {
            builder.setPositiveButton("知道了", null)
        }
        builder.show()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleExternalPluginIntent(intent)
    }

    private fun handleExternalPluginIntent(incoming: Intent?) {
        if (incoming == null) return

        val uri: Uri? = when (incoming.action) {
            Intent.ACTION_VIEW -> incoming.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    incoming.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    incoming.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris: ArrayList<Uri>? =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        incoming.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                    }
                uris?.firstOrNull()
            }
            else -> null
        }

        if (uri == null) return

        val mime = incoming.type?.lowercase().orEmpty()
        val raw = (uri.lastPathSegment ?: uri.toString()).lowercase()
        val looksLikeJs = mime.contains("javascript") || raw.endsWith(".js") || raw.contains(".js?")
        if (!looksLikeJs) return
        val intentFingerprint = "${incoming.action}:${uri}"
        if (intentFingerprint == lastExternalPluginIntent) return
        lastExternalPluginIntent = intentFingerprint

        AlertDialog.Builder(this)
            .setTitle("导入JS插件")
            .setMessage("检测到外部插件文件：\n$uri\n\n仅导入你信任的来源。")
            .setNegativeButton("取消", null)
            .setPositiveButton("导入") { _, _ ->
                lifecycleScope.launch {
                    val result = musicViewModel.importLxPlugin(uri)
                    if (result.isSuccess) {
                        Toast.makeText(this@MainActivity, "JS插件导入成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "导入失败: ${result.exceptionOrNull()?.message ?: "未知错误"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    /**
     * 安全加固检查：反调试 / 反Root / 反模拟器 / 签名校验 / 反Hook / 反抓包
     * 在 IO 线程执行完整检测，根据风险等级决定处置策略
     */
    private fun performSecurityCheck() {
        lifecycleScope.launch(Dispatchers.IO) {
            val report = SecurityGuard.performAllChecks()
            if (CriticalUiProtector.isStaticPolicyTampered() && BuildConfig.SECURITY_KILL_ON_RISK) {
                SecurityGuard.killProcess()
                return@launch
            }

            if (BuildConfig.DEBUG) {
                // Debug 模式只记录日志，不强制终止
                Log.d("SecurityGuard", "Security Report: $report")
                return@launch
            }

            // ── Release 模式处置策略 ──

            // 高危: 调试器 / 签名篡改 / Hook → 直接终止进程
            if (report.hasHighRisk && BuildConfig.SECURITY_KILL_ON_RISK) {
                SecurityGuard.killProcess()
                return@launch
            }

            // 中危: Root / 模拟器 / 可调试 → 仅记录日志，不打扰用户
            if (report.hasMediumRisk) {
                Log.w("SecurityGuard", "Medium risk detected: $report")
            }
        }
    }

    private fun startCriticalUiWatchdog() {
        criticalUiWatchdogJob?.cancel()
        criticalUiWatchdogJob = lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                val reason = CriticalUiProtector.blockReason()
                if (!reason.isNullOrEmpty()) {
                    Log.e("CriticalUiProtector", "关键界面篡改风险: $reason")
                    if (isUiEntryMissingReason(reason)) {
                        if (lastUiTamperReason != reason) {
                            lastUiTamperReason = reason
                            withContext(Dispatchers.Main) {
                                showOfficialContactFallbackDialog(trigger = "watchdog:$reason")
                            }
                        }
                        delay(2_000)
                        continue
                    }
                    if (BuildConfig.SECURITY_KILL_ON_RISK) {
                        SecurityGuard.killProcess()
                    }
                    return@launch
                }
                delay(2_000)
            }
        }
    }

    override fun onDestroy() {
        criticalUiWatchdogJob?.cancel()
        criticalUiWatchdogJob = null
        updateManager?.cleanup()
        updateManager = null
        super.onDestroy()
    }

    /**
     * 从服务器拉取远程配置，服务器后台控制APP一切行为:
     * - 总开关 (app_enabled)
     * - 公告弹窗 (announcement)
     * - 版本强更 (force_update)
     * - 音乐平台开关 (music_platforms)
     * - 功能开关 (features)
     * - UI控制 (ui_config)
     * - 频率限制 (rate_limits)
     * - API地址 (api_host)
     */
    private fun fetchRemoteConfig() {
        lifecycleScope.launch {
            val success = RemoteConfig.fetch()
            if (!success) {
                Log.w("MainActivity", "远程配置拉取失败，使用默认配置")
                return@launch
            }

            // ── 1. 总开关：服务器关闭则APP不可用 ──
            if (!RemoteConfig.appEnabled) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("服务维护中")
                        .setMessage(RemoteConfig.announcement.ifEmpty { "应用暂时不可用，请稍后再试" })
                        .setCancelable(false)
                        .setPositiveButton("退出") { _, _ -> finish() }
                        .show()
                }
                return@launch
            }

            // ── 2. 版本更新检查 (支持强制/可选更新 + APK下载安装) ──
            updateManager = UpdateManager(this@MainActivity)
            updateManager?.checkUpdate()

            // ── 3. 公告弹窗 ──
            if (RemoteConfig.showAnnouncement && RemoteConfig.announcement.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(this@MainActivity)
                        .setTitle("公告")
                        .setMessage(RemoteConfig.announcement)
                        .setPositiveButton("知道了", null)
                        .show()
                }
            }

            // ── 4. 顶部通知栏提示 ──
            if (RemoteConfig.noticeBarText.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, RemoteConfig.noticeBarText, Toast.LENGTH_LONG).show()
                }
            }

            // ── 5. 官方联系兜底展示（后端可强制下发） ──
            if (RemoteConfig.forceShowOfficialContact) {
                withContext(Dispatchers.Main) {
                    showOfficialContactFallbackDialog(trigger = "remote_config")
                }
            }
        }
    }
}
