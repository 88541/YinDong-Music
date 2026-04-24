package com.yindong.music.data

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.security.MessageDigest

/**
 * 更新管理器 — 服务器后台控制APP版本更新
 *
 * 功能:
 *   1. 从 RemoteConfig 读取版本信息 (已在启动时拉取)
 *   2. 对比当前版本与服务器最新版本
 *   3. 弹窗提示更新 (支持强制/可选)
 *   4. 使用系统 DownloadManager 下载APK
 *   5. 下载完成后自动触发安装
 */
class UpdateManager(private val activity: Activity) {

    companion object {
        private const val TAG = "UpdateManager"
        private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")
    }

    private var downloadId: Long = -1
    private var downloadReceiver: BroadcastReceiver? = null

    /** 在 Activity.onDestroy 中调用，防止 BroadcastReceiver 泄漏 */
    fun cleanup() {
        try {
            downloadReceiver?.let { activity.unregisterReceiver(it) }
        } catch (_: Exception) { /* 未注册时忽略 */ }
        downloadReceiver = null
    }

    /**
     * 检查更新 — 对比版本号，有新版本则弹窗
     */
    fun checkUpdate() {
        if (!RemoteConfig.isLoaded) {
            Log.d(TAG, "远程配置未加载，跳过更新检查")
            return
        }

        val serverVersion = RemoteConfig.latestVersion
        if (serverVersion.isEmpty()) {
            Log.d(TAG, "服务器未配置最新版本号，跳过")
            return
        }

        val currentVersion = try {
            activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.0.0"
        } catch (_: Exception) { "0.0.0" }

        Log.d(TAG, "版本对比: 当前=$currentVersion, 服务器=$serverVersion, 强制=${RemoteConfig.forceUpdate}")

        if (compareVersion(currentVersion, serverVersion) >= 0) {
            Log.d(TAG, "已是最新版本")
            return
        }

        if (!hasSecureUpdateConfig()) {
            Log.e(TAG, "更新配置不安全，已拒绝更新流程: url=${RemoteConfig.updateUrl}, sha256=${RemoteConfig.updateApkSha256}")
            activity.runOnUiThread {
                if (RemoteConfig.forceUpdate) {
                    AlertDialog.Builder(activity)
                        .setTitle("更新配置异常")
                        .setMessage("当前版本需要更新，但更新包校验信息缺失或非法，请联系管理员修复后再试。")
                        .setCancelable(false)
                        .setPositiveButton("退出应用") { _, _ -> activity.finish() }
                        .show()
                } else {
                    Toast.makeText(activity, "更新配置不安全，已自动跳过", Toast.LENGTH_LONG).show()
                }
            }
            return
        }

        // 有新版本 → 在主线程弹窗
        activity.runOnUiThread {
            showUpdateDialog(currentVersion, serverVersion)
        }
    }

    /**
     * 显示更新弹窗
     */
    private fun showUpdateDialog(currentVersion: String, newVersion: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        val message = buildString {
            append("发现新版本: v$newVersion\n")
            append("当前版本: v$currentVersion\n\n")
            if (RemoteConfig.updateLog.isNotEmpty()) {
                append("更新内容:\n${RemoteConfig.updateLog}\n\n")
            }
            if (RemoteConfig.forceUpdate) {
                append("⚠️ 此版本为强制更新，必须更新后才能使用")
            }
        }

        val builder = AlertDialog.Builder(activity)
            .setTitle("🚀 发现新版本")
            .setMessage(message)
            .setCancelable(!RemoteConfig.forceUpdate)
            .setPositiveButton("立即更新") { _, _ ->
                startDownload()
            }

        if (!RemoteConfig.forceUpdate) {
            // 非强制更新：可以跳过
            builder.setNegativeButton("稍后再说", null)
        } else {
            // 强制更新：退出应用
            builder.setNegativeButton("退出应用") { _, _ ->
                activity.finish()
            }
        }

        builder.show()
    }

    /**
     * 使用系统 DownloadManager 下载APK
     */
    private fun startDownload() {
        val url = RemoteConfig.updateUrl
        if (url.isEmpty()) {
            Toast.makeText(activity, "下载地址未配置", Toast.LENGTH_SHORT).show()
            // 尝试用浏览器打开
            return
        }
        if (!url.startsWith("https://")) {
            Toast.makeText(activity, "更新地址必须为HTTPS", Toast.LENGTH_LONG).show()
            Log.e(TAG, "拒绝非HTTPS更新地址: $url")
            return
        }
        if (!hasSecureUpdateConfig()) {
            Toast.makeText(activity, "更新配置不安全，已阻止下载", Toast.LENGTH_LONG).show()
            Log.e(TAG, "已阻止不安全更新下载: url=$url, sha256=${RemoteConfig.updateApkSha256}")
            return
        }

        try {
            // 清理旧APK
            val apkFile = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "cloud_music_update.apk"
            )
            if (apkFile.exists()) apkFile.delete()

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("新众和夜雨音乐更新")
                setDescription("正在下载 v${RemoteConfig.latestVersion}...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(
                    activity,
                    Environment.DIRECTORY_DOWNLOADS,
                    "cloud_music_update.apk"
                )
                setMimeType("application/vnd.android.package-archive")
            }

            val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            downloadId = dm.enqueue(request)

            Toast.makeText(activity, "开始下载更新...", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "下载已开始: downloadId=$downloadId, url=$url")

            // 注册下载完成广播
            registerDownloadReceiver()

        } catch (e: Exception) {
            Log.e(TAG, "下载失败", e)
            Toast.makeText(activity, "下载失败，尝试浏览器下载", Toast.LENGTH_SHORT).show()
            // 降级：用浏览器打开下载链接
            try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {}
        }
    }

    /**
     * 注册下载完成的广播接收器
     */
    private fun registerDownloadReceiver() {
        downloadReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    Log.d(TAG, "下载完成: $id")
                    installApk()
                    // 取消注册
                    try { activity.unregisterReceiver(this) } catch (_: Exception) {}
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            activity.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_EXPORTED
            )
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            activity.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    /**
     * 触发APK安装
     */
    private fun installApk() {
        try {
            val apkFile = File(
                activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
                "cloud_music_update.apk"
            )

            if (!apkFile.exists()) {
                Toast.makeText(activity, "APK文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            val verifyError = verifyDownloadedApk(apkFile)
            if (verifyError != null) {
                Log.e(TAG, "APK校验失败: $verifyError")
                Toast.makeText(activity, "安装已阻止: $verifyError", Toast.LENGTH_LONG).show()
                return
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.fileprovider",
                    apkFile
                )
            } else {
                Uri.fromFile(apkFile)
            }

            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            activity.startActivity(intent)

            Log.d(TAG, "已触发APK安装: $uri")
        } catch (e: Exception) {
            Log.e(TAG, "安装APK失败", e)
            Toast.makeText(activity, "安装失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * 校验下载的 APK：
     * 1) 包名必须与当前应用一致
     * 2) 若服务端下发 update_apk_sha256，则必须匹配
     */
    private fun verifyDownloadedApk(apkFile: File): String? {
        val archiveInfo = activity.packageManager.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: return "无法解析安装包"
        if (archiveInfo.packageName != activity.packageName) {
            return "安装包包名不匹配"
        }

        val expectedSha = RemoteConfig.updateApkSha256.trim().lowercase()
        if (!SHA256_REGEX.matches(expectedSha)) {
            return "更新配置缺少合法SHA256"
        }
        val actualSha = sha256Of(apkFile)
        if (actualSha != expectedSha) {
            return "SHA256校验失败"
        }
        return null
    }

    private fun hasSecureUpdateConfig(): Boolean {
        val url = RemoteConfig.updateUrl.trim()
        val expectedSha = RemoteConfig.updateApkSha256.trim().lowercase()
        if (url.isEmpty() || !url.startsWith("https://")) return false
        return SHA256_REGEX.matches(expectedSha)
    }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val read = input.read(buf)
                if (read <= 0) break
                digest.update(buf, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * 版本号比较: a < b → 负数, a == b → 0, a > b → 正数
     * 支持格式: "1.0.0", "2.2.0", "1.10.3"
     */
    private fun compareVersion(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val len = maxOf(parts1.size, parts2.size)
        for (i in 0 until len) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }
}
