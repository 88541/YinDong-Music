package com.yindong.music.data.lx

data class LxPluginInfo(
    val name: String = "",
    val version: String = "",
    val author: String = "",
    val description: String = "",
    val homepage: String = "",
)

data class LxSourceMeta(
    val key: String,
    val name: String = key,
    val qualitys: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
)

data class LxSearchSong(
    val id: String,
    val name: String,
    val artists: List<String>,
    val source: String,
)

data class LxMusicUrlResult(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

data class LxLyricResult(
    val lyric: String = "",
    val tlyric: String = "",
)

data class LxRuntimeOptions(
    val callTimeoutMs: Long = 5000L,
    val allowHttp: Boolean = false,
    val hostWhitelist: Set<String> = emptySet(),
)

/** Represents a loaded plugin entry (for persistence and UI). */
data class LxPluginEntry(
    val id: String,               // unique ID (SHA-256 hash of script)
    val uri: String,              // content URI string
    val info: LxPluginInfo = LxPluginInfo(),
    val sources: List<String> = emptyList(),  // source keys this plugin provides
    val initialized: Boolean = true,          // whether handler was registered successfully
)
