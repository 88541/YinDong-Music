package com.dirror.music.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * 搜索历史管理类
 * 使用 SharedPreferences 存储，最多保留 20 条
 */
class SearchHistoryManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "search_history_prefs"
        private const val KEY_HISTORY = "search_history"
        private const val MAX_HISTORY_COUNT = 20
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    /**
     * 添加搜索历史
     * @param keyword 搜索关键词
     */
    fun addHistory(keyword: String) {
        if (keyword.isBlank()) return

        val history = getHistory().toMutableList()

        // 如果已存在，先移除（移到最前面）
        history.remove(keyword)

        // 添加到开头
        history.add(0, keyword)

        // 限制数量
        if (history.size > MAX_HISTORY_COUNT) {
            history.removeAt(history.size - 1)
        }

        // 保存
        saveHistory(history)
    }

    /**
     * 获取搜索历史列表
     * @return 搜索历史列表（最新的在前面）
     */
    fun getHistory(): List<String> {
        val json = prefs.getString(KEY_HISTORY, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    /**
     * 删除单条搜索历史
     * @param keyword 要删除的关键词
     */
    fun removeHistory(keyword: String) {
        val history = getHistory().toMutableList()
        history.remove(keyword)
        saveHistory(history)
    }

    /**
     * 删除指定位置的搜索历史
     * @param position 位置索引
     */
    fun removeHistoryAt(position: Int) {
        val history = getHistory().toMutableList()
        if (position >= 0 && position < history.size) {
            history.removeAt(position)
            saveHistory(history)
        }
    }

    /**
     * 清空所有搜索历史
     */
    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }

    /**
     * 检查是否已存在该关键词
     * @param keyword 关键词
     * @return 是否存在
     */
    fun contains(keyword: String): Boolean {
        return getHistory().contains(keyword)
    }

    /**
     * 获取历史记录数量
     */
    fun getHistoryCount(): Int {
        return getHistory().size
    }

    /**
     * 保存历史记录
     */
    private fun saveHistory(history: List<String>) {
        val json = gson.toJson(history)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }
}
