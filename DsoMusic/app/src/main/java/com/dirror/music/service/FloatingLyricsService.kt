package com.dirror.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.dirror.music.App
import com.dirror.music.R
import com.dirror.music.ui.activity.FloatingLyricsSettingsActivity
import com.dirror.music.ui.player.PlayerActivity
import com.dirror.music.util.FloatingLyricsSettings

/**
 * 歌词悬浮窗服务
 */
class FloatingLyricsService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var tvLyrics: TextView? = null
    private var tvLyricsNext: TextView? = null
    private var lyricsContainer: FrameLayout? = null
    private var lyricsDisplayArea: LinearLayout? = null
    private var controlPanel: LinearLayout? = null
    private var btnLock: ImageView? = null
    private var btnClose: ImageView? = null
    private var btnSettings: ImageView? = null
    private var btnPrevious: ImageView? = null
    private var btnPlayPause: ImageView? = null
    private var btnNext: ImageView? = null

    private var isLocked = false
    private var isControlPanelVisible = false  // 默认只显示歌词
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isClick = false
    private val CLICK_THRESHOLD = 10  // 点击阈值，小于这个移动距离视为点击

    private val layoutParams by lazy {
        WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            gravity = Gravity.TOP or Gravity.START  // 改为左上角定位，支持自由移动
            width = WindowManager.LayoutParams.WRAP_CONTENT  // 宽度自适应内容
            height = WindowManager.LayoutParams.WRAP_CONTENT
            x = 0
            y = 200
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        // 先应用窗口大小设置
        initWindowSize()
        createFloatingWindow()
        applySettings()
        startForegroundService()
    }

    private fun initWindowSize() {
        // 在创建窗口前初始化窗口大小
        val windowWidth = FloatingLyricsSettings.getWindowWidth(this)
        val windowHeight = FloatingLyricsSettings.getWindowHeight(this)
        // 如果设置了具体数值（大于0），则使用；否则使用 WRAP_CONTENT
        layoutParams.width = if (windowWidth > 0) windowWidth else WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = if (windowHeight > 0) windowHeight else WindowManager.LayoutParams.WRAP_CONTENT
    }

    private fun startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "悬浮歌词",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, PlayerActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("悬浮歌词运行中")
            .setContentText("点击返回播放器")
            .setSmallIcon(R.drawable.ic_play)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createFloatingWindow() {
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_lyrics, null)
        
        // 初始化视图
        tvLyrics = floatingView?.findViewById(R.id.tvLyrics)
        tvLyricsNext = floatingView?.findViewById(R.id.tvLyricsNext)
        lyricsContainer = floatingView?.findViewById(R.id.lyricsContainer)
        lyricsDisplayArea = floatingView?.findViewById(R.id.lyricsDisplayArea)
        controlPanel = floatingView?.findViewById(R.id.controlPanel)
        btnLock = floatingView?.findViewById(R.id.btnLock)
        btnClose = floatingView?.findViewById(R.id.btnClose)
        btnSettings = floatingView?.findViewById(R.id.btnSettings)
        btnPrevious = floatingView?.findViewById(R.id.btnPrevious)
        btnPlayPause = floatingView?.findViewById(R.id.btnPlayPause)
        btnNext = floatingView?.findViewById(R.id.btnNext)

        // 设置按钮点击事件
        setupButtonListeners()

        // 初始化显示状态 - 默认只显示歌词
        updateVisibility()

        // 设置触摸监听（拖动和点击）
        setupTouchListener()

        try {
            windowManager?.addView(floatingView, layoutParams)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            if (isLocked) return@setOnTouchListener false
            
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    
                    // 如果移动距离超过阈值，则视为拖动而非点击
                    if (Math.abs(deltaX) > CLICK_THRESHOLD || Math.abs(deltaY) > CLICK_THRESHOLD) {
                        isClick = false
                    }
                    
                    layoutParams.x = initialX + deltaX.toInt()
                    layoutParams.y = initialY + deltaY.toInt()
                    windowManager?.updateViewLayout(floatingView, layoutParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // 如果是点击（移动距离很小），则切换控制面板
                    if (isClick) {
                        toggleControlPanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun toggleControlPanel() {
        isControlPanelVisible = !isControlPanelVisible
        updateVisibility()
    }

    private fun updateVisibility() {
        if (isControlPanelVisible) {
            // 显示控制面板，隐藏歌词
            lyricsDisplayArea?.visibility = View.GONE
            controlPanel?.visibility = View.VISIBLE
        } else {
            // 显示歌词，隐藏控制面板
            lyricsDisplayArea?.visibility = View.VISIBLE
            controlPanel?.visibility = View.GONE
        }
    }

    private fun setupButtonListeners() {
        // 关闭按钮
        btnClose?.setOnClickListener {
            stopSelf()
        }

        // 锁定按钮
        btnLock?.setOnClickListener {
            isLocked = !isLocked
            btnLock?.setImageResource(
                if (isLocked) R.drawable.ic_lock else R.drawable.ic_lock_open
            )
        }

        // 设置按钮
        btnSettings?.setOnClickListener {
            val intent = Intent(this, FloatingLyricsSettingsActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        }

        // 上一首
        btnPrevious?.setOnClickListener {
            App.musicController.value?.playPrevious()
        }

        // 播放/暂停
        btnPlayPause?.setOnClickListener {
            App.musicController.value?.changePlayState()
            updatePlayPauseIcon()
        }

        // 下一首
        btnNext?.setOnClickListener {
            App.musicController.value?.playNext()
        }
    }

    private fun updatePlayPauseIcon() {
        val isPlaying = App.musicController.value?.isPlaying()?.value ?: false
        btnPlayPause?.setImageResource(
            if (isPlaying) R.drawable.ic_pause_btn else R.drawable.ic_play
        )
    }

    private fun applySettings() {
        // 应用文字大小
        val textSize = FloatingLyricsSettings.getTextSize(this)
        tvLyrics?.textSize = textSize
        tvLyricsNext?.textSize = textSize - 4

        // 应用文字颜色
        val textColor = FloatingLyricsSettings.getTextColor(this)
        tvLyrics?.setTextColor(textColor)
        tvLyricsNext?.setTextColor(Color.argb(128, Color.red(textColor), Color.green(textColor), Color.blue(textColor)))

        // 应用背景颜色和透明度
        val backgroundColor = FloatingLyricsSettings.getBackgroundColor(this)
        val alpha = FloatingLyricsSettings.getBackgroundAlpha(this)
        val colorWithAlpha = Color.argb(
            alpha,
            Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor)
        )
        lyricsContainer?.setBackgroundColor(colorWithAlpha)

        // 应用是否显示下一句
        val showNextLine = FloatingLyricsSettings.getShowNextLine(this)
        tvLyricsNext?.visibility = if (showNextLine) View.VISIBLE else View.GONE

        // 应用窗口大小
        applyWindowSize()
    }

    private fun applyWindowSize() {
        val windowWidth = FloatingLyricsSettings.getWindowWidth(this)
        val windowHeight = FloatingLyricsSettings.getWindowHeight(this)

        // 更新布局参数
        layoutParams.width = if (windowWidth > 0) windowWidth else WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.height = if (windowHeight > 0) windowHeight else WindowManager.LayoutParams.WRAP_CONTENT

        // 更新悬浮窗布局
        try {
            if (floatingView != null && floatingView?.parent != null) {
                windowManager?.updateViewLayout(floatingView, layoutParams)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateSongInfo() {
        // 歌曲信息已在控制面板标题中显示，无需额外更新
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_LYRICS -> {
                val currentLine = intent.getStringExtra(EXTRA_CURRENT_LINE) ?: ""
                val nextLine = intent.getStringExtra(EXTRA_NEXT_LINE) ?: ""
                updateLyrics(currentLine, nextLine)
            }
            ACTION_HIDE -> hideFloatingWindow()
            ACTION_SHOW -> showFloatingWindow()
            ACTION_REFRESH_SETTINGS -> applySettings()
            ACTION_UPDATE_SONG_INFO -> updateSongInfo()
        }
        return START_STICKY
    }

    private fun updateLyrics(current: String, next: String) {
        // 在主线程更新 UI
        floatingView?.post {
            tvLyrics?.text = current
            tvLyricsNext?.text = next
        }
    }

    private fun hideFloatingWindow() {
        floatingView?.visibility = View.GONE
    }

    private fun showFloatingWindow() {
        floatingView?.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }

    companion object {
        const val ACTION_UPDATE_LYRICS = "action_update_lyrics"
        const val ACTION_HIDE = "action_hide"
        const val ACTION_SHOW = "action_show"
        const val ACTION_REFRESH_SETTINGS = "action_refresh_settings"
        const val ACTION_UPDATE_SONG_INFO = "action_update_song_info"
        const val EXTRA_CURRENT_LINE = "extra_current_line"
        const val EXTRA_NEXT_LINE = "extra_next_line"
        
        private const val CHANNEL_ID = "floating_lyrics_channel"
        private const val NOTIFICATION_ID = 1001

        fun startService(context: android.content.Context) {
            val intent = Intent(context, FloatingLyricsService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: android.content.Context) {
            val intent = Intent(context, FloatingLyricsService::class.java)
            context.stopService(intent)
        }

        fun updateLyrics(context: android.content.Context, current: String, next: String) {
            val intent = Intent(context, FloatingLyricsService::class.java).apply {
                action = ACTION_UPDATE_LYRICS
                putExtra(EXTRA_CURRENT_LINE, current)
                putExtra(EXTRA_NEXT_LINE, next)
            }
            context.startService(intent)
        }

        fun refreshSettings(context: android.content.Context) {
            val intent = Intent(context, FloatingLyricsService::class.java).apply {
                action = ACTION_REFRESH_SETTINGS
            }
            context.startService(intent)
        }

        fun updateSongInfo(context: android.content.Context) {
            val intent = Intent(context, FloatingLyricsService::class.java).apply {
                action = ACTION_UPDATE_SONG_INFO
            }
            context.startService(intent)
        }
    }
}
