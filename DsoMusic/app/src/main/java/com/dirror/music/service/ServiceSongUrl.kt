package com.dirror.music.service

import android.content.ContentUris
import android.net.Uri
import android.util.Log
import com.dirror.music.App
import com.dirror.music.data.LyricViewData
import com.dirror.music.music.bilibili.BilibiliUrl
import com.dirror.music.music.kuwo.SearchSong
import com.dirror.music.music.netease.NewSearchSong
import com.dirror.music.music.netease.SongUrl
import com.dirror.music.music.qq.PlayUrl
import com.dirror.music.music.standard.SearchLyric
import com.dirror.music.music.standard.data.*
import com.dirror.music.plugin.PluginConstants
import com.dirror.music.plugin.PluginSupport
import com.dirror.music.util.Api
import com.dirror.music.util.AppConfig
import com.dirror.music.util.Config
import com.dirror.music.util.runOnMainThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 获取歌曲 URL
 */
object ServiceSongUrl {

    inline fun getUrlProxy(song: StandardSongData, crossinline success: (Any?) -> Unit) {
        getUrl(song) {
            GlobalScope.launch {
                withContext(Dispatchers.Main) {
                    success.invoke(it)
                }
            }
        }
    }

    inline fun getUrl(song: StandardSongData, crossinline success: (Any?) -> Unit) {
        PluginSupport.setSong(song)
        val pluginUrl = PluginSupport.apply(PluginConstants.POINT_SONG_URL)
        if (pluginUrl != null && pluginUrl is String) {
            success.invoke(pluginUrl)
            return
        }
        when (song.source) {
            SOURCE_NETEASE -> {
                // 使用新的网易云API获取播放链接，使用用户设置的音质
                val quality = AppConfig.soundQuality
                NewSearchSong.getSongUrl(song.id ?: "", quality) { url ->
                    if (!url.isNullOrEmpty()) {
                        success.invoke(url)
                    } else {
                        // 如果新API获取失败，尝试使用旧的方法
                        SongUrl.getSongUrlCookie(song.id ?: "") {
                            success.invoke(it)
                        }
                    }
                }
            }
            SOURCE_LOCAL -> {
                val id = song.id?.toLong() ?: 0
                val contentUri: Uri =
                    ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                success.invoke(contentUri)
            }
            SOURCE_QQ -> {
                GlobalScope.launch {
                    success.invoke(PlayUrl.getPlayUrl(song.id ?: ""))
                }
            }
            SOURCE_DIRROR -> {
                GlobalScope.launch {
                    success.invoke(song.dirrorInfo?.url)
                }
            }
            SOURCE_KUWO -> {
                GlobalScope.launch {
                    val url = SearchSong.getUrl(song.id ?: "")
                    success.invoke(url)
                }
            }
            SOURCE_BILIBILI -> {
                GlobalScope.launch {
                    success.invoke(BilibiliUrl.getPlayUrl(song.id ?: ""))
                }
            }
            SOURCE_NETEASE_CLOUD -> {
                SongUrl.getSongUrlCookie(song.id ?: "") {
                    success.invoke(it)
                }
            }
            else -> success.invoke(null)
        }
    }

    fun getLyric(song: StandardSongData, success: (LyricViewData) -> Unit) {
        Log.d("ServiceSongUrl", "获取歌词 - 歌曲ID: ${song.id}, 来源: ${song.source}")
        if (song.source == SOURCE_NETEASE) {
            // 使用新的网易云API获取歌词
            NewSearchSong.getLyric(song.id ?: "") { originalLyric, translatedLyric ->
                runOnMainThread {
                    val lyric = originalLyric ?: ""
                    val tlyric = translatedLyric ?: ""
                    Log.d("ServiceSongUrl", "获取到歌词 - 原歌词长度: ${lyric.length}, 翻译歌词长度: ${tlyric.length}")
                    success.invoke(LyricViewData(lyric, tlyric))
                }
            }
        } else {
            SearchLyric.getLyricString(song) { string ->
                runOnMainThread {
                    Log.d("ServiceSongUrl", "从其他源获取歌词长度: ${string.length}")
                    success.invoke(LyricViewData(string, ""))
                }
            }
        }
    }

    suspend fun getUrlFromOther(song: StandardSongData): String {
        Api.getFromKuWo(song)?.apply {
            SearchSong.getUrl(id ?: "").let {
                return it
            }
        }
        Api.getFromQQ(song)?.apply {
            PlayUrl.getPlayUrl(id ?: "").let {
                return it
            }


        }
        return ""
    }

    private fun getArtistName(artists: List<StandardSongData.StandardArtistData>?): String {
        val sb = StringBuilder()
        artists?.forEach {
            if (sb.isNotEmpty()) {
                sb.append(" ")
            }
            sb.append(it.name)
        }
        return sb.toString()
    }

}