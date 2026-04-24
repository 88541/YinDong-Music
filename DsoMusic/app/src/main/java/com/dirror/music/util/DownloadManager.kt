package com.dirror.music.util

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.dirror.music.music.netease.NewSearchSong
import com.dirror.music.music.standard.data.StandardSongData

/**
 * 歌曲下载管理器
 */
object DownloadManager {

    private const val TAG = "DownloadManager"

    /**
     * 下载歌曲
     * @param context 上下文
     * @param song 歌曲数据
     * @param quality 音质等级 (standard, exhigh, lossless, hires, jymaster)
     */
    fun downloadSong(context: Context, song: StandardSongData, quality: String = "exhigh") {
        val songId = song.id ?: run {
            toast("歌曲ID为空，无法下载")
            return
        }

        toast("正在获取下载链接...")

        // 获取歌曲播放URL
        NewSearchSong.getSongUrl(songId, quality) { url ->
            if (url.isNullOrEmpty()) {
                runOnMainThread {
                    toast("获取下载链接失败")
                }
                return@getSongUrl
            }

            // 构建文件名
            val artistNames = song.artists?.joinToString("_") { it.name ?: "未知" } ?: "未知"
            val fileName = "${song.name} - $artistNames.mp3"
                .replace("/", "-")
                .replace("\\", "-")
                .replace(":", "-")
                .replace("*", "-")
                .replace("?", "-")
                .replace("\"", "-")
                .replace("<", "-")
                .replace(">", "-")
                .replace("|", "-")

            Log.d(TAG, "开始下载: $fileName, URL: $url")

            // 使用系统下载管理器
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                // 设置通知显示
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // 设置标题
                setTitle(song.name)
                // 设置描述
                setDescription("正在下载...")
                // 设置下载位置
                setDestinationInExternalPublicDir(Environment.DIRECTORY_MUSIC, fileName)
                // 允许网络类型
                setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                // 添加到下载队列
            }

            try {
                val downloadId = downloadManager.enqueue(request)
                Log.d(TAG, "下载任务已添加, ID: $downloadId")
                runOnMainThread {
                    toast("已开始下载: ${song.name}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                runOnMainThread {
                    toast("下载失败: ${e.message}")
                }
            }
        }
    }

    /**
     * 检查是否有下载权限
     */
    fun checkDownloadPermission(context: Context): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Android 10+ 使用 Scoped Storage，不需要 WRITE_EXTERNAL_STORAGE 权限
            true
        } else {
            // Android 9 及以下需要 WRITE_EXTERNAL_STORAGE 权限
            context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 请求下载权限
     */
    fun requestDownloadPermission(activity: androidx.fragment.app.FragmentActivity, requestCode: Int = 1001) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            activity.requestPermissions(
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                requestCode
            )
        }
    }
}
