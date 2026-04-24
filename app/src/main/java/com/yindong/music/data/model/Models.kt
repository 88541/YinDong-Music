package com.yindong.music.data.model

/** 单行歌词 (LRC 格式解析后) */
data class LyricLine(val timeMs: Long, val text: String, val ttext: String = "")

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long = 0L,
    /** 专辑封面图 URL (支持异步更新) */
    var coverUrl: String = "",
    /** 歌手头像 URL */
    val artistPicUrl: String = "",
    val isLiked: Boolean = false,
    val platform: String = "QQ音乐",
    val platformId: String = "",
    /** 搜索直接返回的播放 URL，非空时跳过 API 解析直接播放 */
    val directUrl: String = "",
    /** 原始 LRC 歌词文本（抖音/汽水等解析时附带） */
    val lrcText: String = "",
    /** 插件搜索返回的原始 JSON，用于 musicUrl 等二次请求 */
    val pluginRawJson: String = "",
    /** 插件搜索时使用的源 key（如 "tx", "kw"），播放/歌词请求必须用此源 */
    val lxSourceKey: String = "",
    /** 搜索该歌曲时所用插件的 ID，播放/歌词请求优先用此插件（保证 pluginRawJson 格式匹配） */
    val lxPluginId: String = "",
)

data class Playlist(
    val id: Long,
    val name: String,
    val coverUrl: String = "",
    val creator: String = "",
    val playCount: Long = 0,
    val songCount: Int = 0,
    val description: String = "",
    val tags: List<String> = emptyList(),
    val songs: List<Song> = emptyList(),
)

data class Banner(
    val id: Long,
    val title: String = "",
    val subtitle: String = "",
    val color: Long = 0xFFEC4141,
    val colorEnd: Long = 0,
    val imageUrl: String = "",
    val platform: String = "",
    val playlistId: String = "",
)
