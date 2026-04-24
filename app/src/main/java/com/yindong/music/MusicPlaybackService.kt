package com.yindong.music

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * 媒体播放前台服务
 * onCreate 立即调用 startForeground() 满足 Android 要求
 * Media3 随后会用带封面/歌名/控制按钮的媒体通知自动替换这个占位通知
 */
class MusicPlaybackService : MediaSessionService() {

    companion object {
        private const val TAG = "MusicService"
        private const val CHANNEL_ID = "music_playback"
        private const val NOTIFICATION_ID = 100
    }

    @UnstableApi
    override fun onCreate() {
        super.onCreate()
        // 必须立即调用 startForeground，否则 Android 会抛 ForegroundServiceDidNotStartInTimeException
        try {
            val nm = getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(CHANNEL_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW).apply {
                        setSound(null, null)
                        setShowBadge(false)
                    }
                )
            }
            val notification = Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("新众和夜雨音乐")
                .setContentText("准备播放...")
                .setOngoing(true)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "前台占位通知已发出，等待Media3替换为媒体通知")
        } catch (e: Exception) {
            Log.e(TAG, "startForeground 失败", e)
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        Log.d(TAG, "onGetSession: session=${MediaSessionHolder.session != null}")
        return MediaSessionHolder.session
    }

    @UnstableApi
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (MediaSessionHolder.session == null) {
            Log.w(TAG, "Session 为空，停止服务")
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return try {
            super.onStartCommand(intent, flags, startId)
        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand 异常", e)
            stopSelf(startId)
            START_NOT_STICKY
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val session = MediaSessionHolder.session
            if (session == null || !session.player.playWhenReady || session.player.mediaItemCount == 0) {
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e(TAG, "onTaskRemoved 异常", e)
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    @UnstableApi
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "服务已销毁")
    }
}

/** ViewModel 和 Service 之间共享 MediaSession 的桥梁 */
object MediaSessionHolder {
    @Volatile
    var session: MediaSession? = null
}
