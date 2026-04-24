package com.yindong.music

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.graphics.Typeface
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat

/**
 * 悬浮歌词服务
 *
 * 在其他App上方显示当前正在播放的歌词，可拖动位置。
 * 需要 SYSTEM_ALERT_WINDOW 权限。
 */
class FloatingLyricsService : Service() {

    companion object {
        private const val TAG = "FloatingLyrics"
        private const val UPDATE_INTERVAL = 150L
        private const val NOTIFICATION_CHANNEL_ID = "floating_lyrics_channel"
        private const val NOTIFICATION_ID = 1001

        // ── 共享状态（ViewModel 写入，Service 读取）──
        @Volatile var currentLyricText: String = ""
        @Volatile var nextLyricText: String = ""
        @Volatile var isPlaying: Boolean = false
        @Volatile var isActive: Boolean = false
        @Volatile var lyricColor: Int = Color.WHITE
        @Volatile var lyricSize: Float = 15f

        // ── 播放控制回调（ViewModel 设置，Service 调用）──
        var onPlayPause: (() -> Unit)? = null
        var onPrevious: (() -> Unit)? = null
        var onNext: (() -> Unit)? = null

        fun start(context: Context) {
            if (isActive) return
            try {
                context.startService(Intent(context, FloatingLyricsService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "启动悬浮歌词失败", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, FloatingLyricsService::class.java))
            } catch (e: Exception) {
                Log.e(TAG, "停止悬浮歌词失败", e)
            }
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var lyricTextView: TextView? = null
    private var nextLyricTextView: TextView? = null
    private var playPauseBtn: TextView? = null
    private var playControlRow: LinearLayout? = null
    private var controlRow: LinearLayout? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isLocked = false
    private var controlsVisible = false

    // ── 拖动相关 ──
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateLyricDisplay()
            handler.postDelayed(this, UPDATE_INTERVAL)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "未授予悬浮窗权限，停止服务")
            stopSelf()
            return
        }
        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        isActive = true
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            createFloatingView()
            handler.post(updateRunnable)
            Log.d(TAG, "悬浮歌词已启动")
        } catch (e: Exception) {
            Log.e(TAG, "悬浮歌词启动失败", e)
            isActive = false
            stopSelf()
        }
    }

    override fun onDestroy() {
        isActive = false
        handler.removeCallbacks(updateRunnable)
        try {
            floatingView?.let { windowManager?.removeView(it) }
        } catch (e: Exception) {
            Log.w(TAG, "移除悬浮窗异常", e)
        }
        floatingView = null
        lyricTextView = null
        nextLyricTextView = null
        Log.d(TAG, "悬浮歌词已停止")
        super.onDestroy()
    }

    // ═══════════════════════════════════════════
    //  创建悬浮窗
    // ═══════════════════════════════════════════

    @SuppressLint("ClickableViewAccessibility")
    private fun createFloatingView() {
        val dm = resources.displayMetrics
        val screenWidth = dm.widthPixels

        // ── 根容器（无背景，纯透明）──
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(6), dp(16), dp(6))
        }

        // ── 歌词文字（当前行）──
        lyricTextView = TextView(this).apply {
            text = "♪ 等待歌词..."
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            letterSpacing = 0.04f
            gravity = Gravity.CENTER
            maxLines = 2
            paint.isAntiAlias = true
            paint.isSubpixelText = true
            paint.hinting = android.graphics.Paint.HINTING_ON
            paint.flags = paint.flags or android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.SUBPIXEL_TEXT_FLAG
            includeFontPadding = false
            setShadowLayer(3f, 0f, 1.5f, 0xDD000000.toInt())
        }
        rootLayout.addView(lyricTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // ── 下一行歌词（预览）──
        nextLyricTextView = TextView(this).apply {
            text = ""
            setTextColor(0x99FFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
            letterSpacing = 0.03f
            gravity = Gravity.CENTER
            maxLines = 1
            setPadding(0, dp(2), 0, 0)
            paint.isAntiAlias = true
            paint.isSubpixelText = true
            paint.flags = paint.flags or android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.SUBPIXEL_TEXT_FLAG
            includeFontPadding = false
            setShadowLayer(2f, 0f, 1f, 0xAA000000.toInt())
        }
        rootLayout.addView(nextLyricTextView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // ── 播放控制行（上一首 / 播放暂停 / 下一首）──
        playControlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(4), 0, 0)
            visibility = View.GONE
        }

        // 上一首
        val prevBtn = TextView(this).apply {
            text = "⏮"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(2), dp(10), dp(2))
            setOnClickListener { onPrevious?.invoke() }
        }
        playControlRow!!.addView(prevBtn, LinearLayout.LayoutParams(dp(40), dp(30)))

        // 播放/暂停
        playPauseBtn = TextView(this).apply {
            text = if (isPlaying) "⏸" else "▶"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(2), dp(10), dp(2))
            setOnClickListener { onPlayPause?.invoke() }
        }
        playControlRow!!.addView(playPauseBtn, LinearLayout.LayoutParams(dp(40), dp(30)))

        // 下一首
        val nextBtn = TextView(this).apply {
            text = "⏭"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(2), dp(10), dp(2))
            setOnClickListener { onNext?.invoke() }
        }
        playControlRow!!.addView(nextBtn, LinearLayout.LayoutParams(dp(40), dp(30)))

        rootLayout.addView(playControlRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // ── 工具行（锁定 + 关闭）──
        controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
            visibility = View.GONE
        }

        // 锁定按钮
        val lockBtn = TextView(this).apply {
            text = "🔒"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener {
                isLocked = !isLocked
                text = if (isLocked) "🔴" else "🔒"
                if (isLocked) hideControls()
            }
        }
        controlRow!!.addView(lockBtn, LinearLayout.LayoutParams(dp(36), dp(24)))

        // 关闭按钮
        val closeBtn = TextView(this).apply {
            text = "✕"
            setTextColor(0xAAFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setOnClickListener { stopSelf() }
        }
        controlRow!!.addView(closeBtn, LinearLayout.LayoutParams(dp(36), dp(24)))

        rootLayout.addView(controlRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        // ── WindowManager 参数 ──
        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dp(80) // 距顶部 80dp
            width = (screenWidth * 0.85).toInt()
        }

        // ── 拖动 + 点击手势 ──
        var lastClickTime = 0L
        var isDragging = false
        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (!isLocked && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(rootLayout, params)
                        } catch (_: Exception) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        val now = System.currentTimeMillis()
                        if (isLocked) {
                            // 双击解锁
                            if (now - lastClickTime < 300) {
                                isLocked = false
                                lockBtn.text = "🔒"
                                showControls()
                            }
                        } else {
                            if (now - lastClickTime < 300) {
                                // 双击打开App
                                openApp()
                            } else {
                                // 单击切换控制栏（延迟执行，等确认不是双击）
                                handler.removeCallbacks(toggleRunnable)
                                handler.postDelayed(toggleRunnable, 320)
                            }
                        }
                        lastClickTime = now
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            floatingView = rootLayout
        } catch (e: Exception) {
            Log.e(TAG, "添加悬浮窗失败", e)
        }
    }

    // ═══════════════════════════════════════════
    //  双击打开App
    // ═══════════════════════════════════════════

    private val toggleRunnable = Runnable { toggleControls() }

    private fun openApp() {
        handler.removeCallbacks(toggleRunnable)
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (intent != null) startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, "打开App失败", e)
        }
    }

    // ═══════════════════════════════════════════
    //  控制栏显示/隐藏
    // ═══════════════════════════════════════════

    private val autoHideRunnable = Runnable { hideControls() }

    private fun showControls() {
        controlsVisible = true
        playControlRow?.visibility = View.VISIBLE
        controlRow?.visibility = View.VISIBLE
        handler.removeCallbacks(autoHideRunnable)
        handler.postDelayed(autoHideRunnable, 3000)
    }

    private fun hideControls() {
        controlsVisible = false
        playControlRow?.visibility = View.GONE
        controlRow?.visibility = View.GONE
        handler.removeCallbacks(autoHideRunnable)
    }

    private fun toggleControls() {
        if (controlsVisible) hideControls() else showControls()
    }

    // ═══════════════════════════════════════════
    //  歌词刷新
    // ═══════════════════════════════════════════

    private var lastText = ""
    private var lastPlayingState = false
    private var lastColor = Color.WHITE
    private var lastSize = 15f

    private fun updateLyricDisplay() {
        val text = currentLyricText
        val next = nextLyricText
        // 同步歌词颜色
        if (lyricColor != lastColor) {
            lastColor = lyricColor
            lyricTextView?.setTextColor(lyricColor)
            nextLyricTextView?.setTextColor((lyricColor and 0x00FFFFFF) or 0x99000000.toInt())
        }
        // 同步字体大小
        if (lyricSize != lastSize) {
            lastSize = lyricSize
            lyricTextView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, lyricSize)
            nextLyricTextView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, (lyricSize * 0.73f))
        }
        if (text != lastText) {
            lastText = text
            lyricTextView?.text = text.ifEmpty { "♪ 等待歌词..." }
            lyricTextView?.alpha = 0.5f
            lyricTextView?.animate()?.alpha(1f)?.setDuration(200)?.start()
        }
        nextLyricTextView?.text = next
        nextLyricTextView?.visibility = if (next.isEmpty()) View.GONE else View.VISIBLE
        // 同步播放/暂停按钮
        if (isPlaying != lastPlayingState) {
            lastPlayingState = isPlaying
            playPauseBtn?.text = if (isPlaying) "⏸" else "▶"
        }
    }

    // ═══════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════

    private fun dp(value: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics
        ).toInt()
    }

    private fun createRoundedBackground(color: Int, radius: Float): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "悬浮歌词",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "悬浮歌词显示服务"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("悬浮歌词")
            .setContentText("正在显示悬浮歌词")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }
}
