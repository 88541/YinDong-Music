package com.dirror.music.util

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

/**
 * 悬浮歌词设置管理类
 */
object FloatingLyricsSettings {

    private const val PREFS_NAME = "floating_lyrics_settings"
    
    // 设置项Key
    private const val KEY_TEXT_SIZE = "text_size"
    private const val KEY_TEXT_COLOR = "text_color"
    private const val KEY_BACKGROUND_COLOR = "background_color"
    private const val KEY_BACKGROUND_ALPHA = "background_alpha"
    private const val KEY_WINDOW_WIDTH = "window_width"
    private const val KEY_WINDOW_HEIGHT = "window_height"
    private const val KEY_SHOW_NEXT_LINE = "show_next_line"
    private const val KEY_CUSTOM_TEXT = "custom_text"

    // 默认值
    const val DEFAULT_TEXT_SIZE = 18f
    const val DEFAULT_TEXT_COLOR = Color.WHITE
    const val DEFAULT_BACKGROUND_COLOR = Color.BLACK
    const val DEFAULT_BACKGROUND_ALPHA = 128 // 0-255
    const val DEFAULT_WINDOW_WIDTH = -2 // WRAP_CONTENT，默认自适应
    const val DEFAULT_WINDOW_HEIGHT = -2 // WRAP_CONTENT，默认自适应
    const val DEFAULT_SHOW_NEXT_LINE = true
    const val DEFAULT_CUSTOM_TEXT = "聆听好音乐"
    
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    // 文字大小
    fun getTextSize(context: Context): Float {
        return getPrefs(context).getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
    }
    
    fun setTextSize(context: Context, size: Float) {
        getPrefs(context).edit().putFloat(KEY_TEXT_SIZE, size).apply()
    }
    
    // 文字颜色
    fun getTextColor(context: Context): Int {
        return getPrefs(context).getInt(KEY_TEXT_COLOR, DEFAULT_TEXT_COLOR)
    }
    
    fun setTextColor(context: Context, color: Int) {
        getPrefs(context).edit().putInt(KEY_TEXT_COLOR, color).apply()
    }
    
    // 背景颜色
    fun getBackgroundColor(context: Context): Int {
        return getPrefs(context).getInt(KEY_BACKGROUND_COLOR, DEFAULT_BACKGROUND_COLOR)
    }
    
    fun setBackgroundColor(context: Context, color: Int) {
        getPrefs(context).edit().putInt(KEY_BACKGROUND_COLOR, color).apply()
    }
    
    // 背景透明度
    fun getBackgroundAlpha(context: Context): Int {
        return getPrefs(context).getInt(KEY_BACKGROUND_ALPHA, DEFAULT_BACKGROUND_ALPHA)
    }
    
    fun setBackgroundAlpha(context: Context, alpha: Int) {
        getPrefs(context).edit().putInt(KEY_BACKGROUND_ALPHA, alpha).apply()
    }
    
    // 窗口宽度
    fun getWindowWidth(context: Context): Int {
        return getPrefs(context).getInt(KEY_WINDOW_WIDTH, DEFAULT_WINDOW_WIDTH)
    }

    fun setWindowWidth(context: Context, width: Int) {
        getPrefs(context).edit().putInt(KEY_WINDOW_WIDTH, width).apply()
    }

    // 窗口高度
    fun getWindowHeight(context: Context): Int {
        return getPrefs(context).getInt(KEY_WINDOW_HEIGHT, DEFAULT_WINDOW_HEIGHT)
    }

    fun setWindowHeight(context: Context, height: Int) {
        getPrefs(context).edit().putInt(KEY_WINDOW_HEIGHT, height).apply()
    }
    
    // 是否显示下一句
    fun getShowNextLine(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_NEXT_LINE, DEFAULT_SHOW_NEXT_LINE)
    }

    fun setShowNextLine(context: Context, show: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_NEXT_LINE, show).apply()
    }

    // 自定义提示文字
    fun getCustomText(context: Context): String {
        return getPrefs(context).getString(KEY_CUSTOM_TEXT, DEFAULT_CUSTOM_TEXT) ?: DEFAULT_CUSTOM_TEXT
    }

    fun setCustomText(context: Context, text: String) {
        getPrefs(context).edit().putString(KEY_CUSTOM_TEXT, text).apply()
    }

    // 重置为默认设置
    fun resetToDefault(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
