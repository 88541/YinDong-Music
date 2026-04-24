package com.yindong.music.data.lx

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.yindong.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Internal holder for a single loaded plugin's runtime and metadata.
 */
private data class PluginSlot(
    val id: String,
    val uri: String,
    val info: LxPluginInfo,
    val sources: List<LxSourceMeta>,
    val runtime: LxJsRuntime,
)

class LxPluginManager(
    private val context: Context,
    private val contentResolver: ContentResolver,
    private val quickJsFactory: () -> QuickJsBridge,
    private val client: OkHttpClient = OkHttpClient(),
    var logCallback: ((String) -> Unit)? = null,
) {
    companion object {
        private const val TAG = "LxPluginManager"
    }

    /** All loaded plugins keyed by plugin ID (script SHA-256 hash). */
    private val plugins = mutableMapOf<String, PluginSlot>()

    // ── Legacy single-plugin compat properties ──

    val pluginInfo: LxPluginInfo?
        get() = plugins.values.firstOrNull()?.info

    val sources: List<LxSourceMeta>
        get() = plugins.values.flatMap { it.sources }

    // ═══════════════════════════════════════════
    //  Multi-plugin API
    // ═══════════════════════════════════════════

    /**
     * Load a plugin from [pluginUri]. Returns [LxPluginEntry] on success.
     * If a plugin with the same ID (hash) is already loaded, it is replaced.
     */
    @Suppress("BlockingMethodInNonBlockingContext")
    fun loadPlugin(
        pluginUri: String,
        options: LxRuntimeOptions = LxRuntimeOptions(),
    ): Result<LxPluginEntry> = runCatching {
        val (script, hash) = readScriptAndHash(pluginUri)
        val info = LxPluginHeaderParser.parse(script)

        // Close existing runtime for this plugin if any
        plugins[hash]?.runtime?.close()

        val runtime = LxJsRuntime(context, quickJsFactory(), options, logCallback)
        runtime.loadPluginScript(script)

        val inited = runtime.getInitedPayload() ?: JSONObject()
        val srcObj = inited.optJSONObject("sources") ?: JSONObject()
        val srcList = mutableListOf<LxSourceMeta>()
        srcObj.keys().forEach { key ->
            val s = srcObj.optJSONObject(key) ?: JSONObject()
            srcList += LxSourceMeta(
                key = key,
                name = s.optString("name", key),
                qualitys = s.optJSONArray("qualitys").toStringList(),
                actions = s.optJSONArray("actions").toStringList(),
            )
        }

        val slot = PluginSlot(
            id = hash,
            uri = pluginUri,
            info = info,
            sources = srcList,
            runtime = runtime,
        )
        plugins[hash] = slot

        LxPluginEntry(
            id = hash,
            uri = pluginUri,
            info = info,
            sources = srcList.map { it.key },
            initialized = runtime.hasHandler(),
        )
    }

    /**
     * Legacy single-plugin load (delegates to [loadPlugin]).
     * Returns (info, sources) pair for backward compatibility.
     */
    fun load(
        pluginUri: String,
        options: LxRuntimeOptions = LxRuntimeOptions(),
    ): Result<Pair<LxPluginInfo, List<LxSourceMeta>>> = runCatching {
        val entry = loadPlugin(pluginUri, options).getOrThrow()
        val slot = plugins[entry.id]!!
        slot.info to slot.sources
    }

    /** Remove a specific plugin by its ID. */
    fun removePlugin(pluginId: String) {
        plugins.remove(pluginId)?.runtime?.close()
    }

    /** Remove all loaded plugins. */
    fun remove() {
        plugins.values.forEach { it.runtime.close() }
        plugins.clear()
    }

    /** Get the entry of a loaded plugin. */
    fun getPluginEntry(pluginId: String): LxPluginEntry? {
        val slot = plugins[pluginId] ?: return null
        return LxPluginEntry(
            id = slot.id,
            uri = slot.uri,
            info = slot.info,
            sources = slot.sources.map { it.key },
            initialized = slot.runtime.hasHandler(),
        )
    }

    /** Get all loaded plugin entries. */
    fun getAllPluginEntries(): List<LxPluginEntry> {
        return plugins.values.map { slot ->
            LxPluginEntry(
                id = slot.id,
                uri = slot.uri,
                info = slot.info,
                sources = slot.sources.map { it.key },
                initialized = slot.runtime.hasHandler(),
            )
        }
    }

    /** Check if a specific plugin is loaded. */
    fun isPluginLoaded(pluginId: String): Boolean = plugins.containsKey(pluginId)

    /** Get the number of loaded plugins. */
    fun pluginCount(): Int = plugins.size

    /** Check if a specific source of a plugin supports the given action (e.g. "search", "musicUrl", "lyric"). */
    fun sourceSupportsAction(pluginId: String, source: String, action: String): Boolean {
        val slot = resolvePlugin(pluginId) ?: return true // plugin not found → don't block, let it fail naturally
        val meta = slot.sources.find { it.key == source }
            ?: return true // source not in metadata (plugin may not have inited) → allow attempt
        // If the plugin declares no actions list at all, assume all actions are supported
        if (meta.actions.isEmpty()) return true
        return action in meta.actions
    }

    /** Check if the plugin has a working request handler registered. */
    fun isPluginInitialized(pluginId: String): Boolean {
        val slot = resolvePlugin(pluginId) ?: return false
        return slot.runtime.hasHandler()
    }

    // ═══════════════════════════════════════════
    //  Search / MusicUrl (with pluginId)
    // ═══════════════════════════════════════════

    /**
     * Search using a specific plugin.
     * @param pluginId The plugin to use for the search. If blank, uses the first loaded plugin.
     */
    suspend fun search(pluginId: String, source: String, keyword: String, timeoutMs: Long): List<Song> {
        val slot = resolvePlugin(pluginId)
            ?: throw IllegalStateException("plugin not loaded: $pluginId")
        return doSearch(slot, source, keyword)
    }

    /** Legacy: search using the first (or only) loaded plugin. */
    suspend fun search(source: String, keyword: String, timeoutMs: Long): List<Song> {
        val slot = plugins.values.firstOrNull()
            ?: throw IllegalStateException("no plugin loaded")
        return doSearch(slot, source, keyword)
    }

    /**
     * Get music URL using a specific plugin.
     * @param pluginId The plugin to use. If blank, uses the first loaded plugin.
     * @param quality LX quality string: "128k", "320k", "flac" etc. Defaults to "320k".
     */
    suspend fun musicUrl(pluginId: String, source: String, song: Song, timeoutMs: Long, quality: String = "320k"): LxMusicUrlResult {
        val slot = resolvePlugin(pluginId)
            ?: throw IllegalStateException("plugin not loaded: $pluginId")
        return doMusicUrl(slot, source, song, quality)
    }

    /** Legacy: musicUrl using the first (or only) loaded plugin. */
    suspend fun musicUrl(source: String, song: Song, timeoutMs: Long, quality: String = "320k"): LxMusicUrlResult {
        val slot = plugins.values.firstOrNull()
            ?: throw IllegalStateException("no plugin loaded")
        return doMusicUrl(slot, source, song, quality)
    }

    /**
     * Get lyrics using a specific plugin.
     * @param pluginId The plugin to use. If blank, uses the first loaded plugin.
     */
    suspend fun lyric(pluginId: String, source: String, song: Song, timeoutMs: Long): LxLyricResult {
        val slot = resolvePlugin(pluginId)
            ?: throw IllegalStateException("plugin not loaded: $pluginId")
        return doLyric(slot, source, song)
    }

    /** Legacy: lyric using the first (or only) loaded plugin. */
    suspend fun lyric(source: String, song: Song, timeoutMs: Long): LxLyricResult {
        val slot = plugins.values.firstOrNull()
            ?: throw IllegalStateException("no plugin loaded")
        return doLyric(slot, source, song)
    }

    // ═══════════════════════════════════════════
    //  Internal helpers
    // ═══════════════════════════════════════════

    private fun resolvePlugin(pluginId: String): PluginSlot? {
        if (pluginId.isBlank()) return plugins.values.firstOrNull()
        return plugins[pluginId]
    }

    /**
     * Map LX plugin source key to a user-friendly display name.
     * Well-known keys are always mapped to the correct name first;
     * for unknown keys the plugin's own source metadata is used as fallback.
     */
    private fun sourceDisplayName(slot: PluginSlot, sourceKey: String): String {
        // 1. Well-known source key mappings (take priority)
        val lower = sourceKey.lowercase()
        val wellKnown: String? = when {
            lower == "tx" || lower.startsWith("qq") || lower.contains("qqmusic") || lower == "qsvip" -> "QQ音乐"
            lower == "wy" || lower == "netease" || lower.startsWith("163") -> "网易云"
            lower == "kw" || lower.startsWith("kuwo") -> "酷我音乐"
            lower == "kg" || lower.startsWith("kugou") -> "酷狗音乐"
            lower == "mg" || lower.startsWith("migu") -> "咪咕音乐"
            lower == "dy" || lower.startsWith("douyin") -> "抖音"
            else -> null
        }
        if (wellKnown != null) return wellKnown

        // 2. Use the plugin's own source name for unknown keys
        val meta = slot.sources.find { it.key == sourceKey }
        if (meta != null && meta.name.isNotBlank() && meta.name != meta.key) {
            return meta.name
        }
        return sourceKey
    }

    private suspend fun doSearch(slot: PluginSlot, source: String, keyword: String): List<Song> {
        val payload = JSONObject()
            .put("source", source)
            .put("action", "search")
            .put("info", JSONObject().put("keyword", keyword))
        Log.d(TAG, "搜索请求: plugin=${slot.info.name}, source=$source, keyword=$keyword")
        val ret = slot.runtime.callRequest(payload)
        Log.d(TAG, "搜索返回: ${ret.toString().take(500)}")
        val error = ret.optString("error")
        if (error.isNotBlank()) {
            Log.e(TAG, "搜索错误: $error")
            throw IllegalStateException(error)
        }
        // Plugins may nest results in "data" or return directly as "list"/"songs"
        val dataObj = ret.optJSONObject("data")
        val list = ret.optJSONArray("list")
            ?: ret.optJSONArray("songs")
            ?: dataObj?.optJSONArray("list")
            ?: dataObj?.optJSONArray("songs")
            ?: JSONArray()
        Log.d(TAG, "搜索结果数量: ${list.length()}")
        return (0 until list.length()).mapNotNull { i ->
            val o = list.optJSONObject(i) ?: return@mapNotNull null
            // Different plugins use different field names
            val songId = o.optString("songmid").ifBlank {
                o.optString("id").ifBlank { o.optString("hash") }
            }
            val title = o.optString("name").ifBlank { o.optString("songname") }
            // artists: could be string "singer", array of strings, or array of objects [{name:"..."}]
            val artist = o.optString("singer").ifBlank {
                val artistArr = o.optJSONArray("artists")
                if (artistArr != null) {
                    (0 until artistArr.length()).mapNotNull { idx ->
                        val item = artistArr.opt(idx)
                        when (item) {
                            is String -> item.takeIf { it.isNotBlank() }
                            is JSONObject -> item.optString("name").takeIf { it.isNotBlank() }
                            else -> item?.toString()?.takeIf { it.isNotBlank() }
                        }
                    }.joinToString("/")
                } else o.optString("artist")
            }
            val album = o.optString("albumName").ifBlank {
                val albumObj = o.opt("album")
                when (albumObj) {
                    is String -> albumObj
                    is JSONObject -> albumObj.optString("name")
                    else -> albumObj?.toString().orEmpty()
                }
            }
            // Cover URL: plugins use various field names
            val coverUrl = o.optString("img").ifBlank {
                o.optString("pic").ifBlank {
                    o.optString("cover").ifBlank {
                        o.optString("albumPic").ifBlank {
                            o.optString("picture").ifBlank {
                                // Some plugins nest cover in album object
                                val albumObj = o.optJSONObject("album")
                                albumObj?.optString("pic").orEmpty().ifBlank {
                                    albumObj?.optString("img").orEmpty()
                                }
                            }
                        }
                    }
                }
            }
            // Duration: "interval" is in seconds (LX convention), "duration" may be in ms
            val durationMs = o.optLong("interval", 0L).let { interval ->
                if (interval > 0) interval * 1000L
                else o.optLong("duration", 0L).let { dur ->
                    // If duration looks like seconds (< 10000), convert to ms
                    if (dur in 1..9999) dur * 1000L else dur
                }
            }
            // LRC text: some plugins include lyrics in search results
            val lrcText = o.optString("lrc").ifBlank {
                o.optString("lyric").ifBlank {
                    o.optString("lrcText")
                }
            }
            if (songId.isBlank() && title.isBlank()) return@mapNotNull null
            Song(
                id = songId.hashCode().toLong(),
                title = title,
                artist = artist,
                album = album,
                duration = durationMs,
                coverUrl = coverUrl,
                lrcText = lrcText,
                platform = sourceDisplayName(slot, source),
                platformId = songId,
                pluginRawJson = o.toString(),
                lxSourceKey = source,
                lxPluginId = slot.id,
            )
        }
    }

    private suspend fun doMusicUrl(slot: PluginSlot, source: String, song: Song, quality: String = "320k"): LxMusicUrlResult {
        // Build the musicInfo object from the original search result JSON.
        // Plugins need fields like songmid, hash, strMediaMid, albumid, etc.
        val musicInfo = if (song.pluginRawJson.isNotBlank()) {
            try {
                val raw = JSONObject(song.pluginRawJson)
                if (!raw.has("name")) raw.put("name", song.title)
                if (!raw.has("singer")) raw.put("singer", song.artist)
                if (!raw.has("types")) {
                    val typesArr = JSONArray()
                    listOf("128k", "320k", "flac").forEach { typesArr.put(JSONObject().put("type", it)) }
                    raw.put("types", typesArr)
                }
                // Ensure platform-specific ID aliases exist
                ensurePlatformIdAliases(raw, source, song.platformId)
                raw
            } catch (_: Exception) {
                buildFallbackMusicInfo(source, song)
            }
        } else {
            buildFallbackMusicInfo(source, song)
        }
        // LX Music v2+ protocol: info = { type, musicInfo }
        // Also keep type inside musicInfo for v1 compat
        musicInfo.put("type", quality)
        val info = JSONObject()
            .put("type", quality)
            .put("musicInfo", musicInfo)
        val payload = JSONObject().put("source", source).put("action", "musicUrl").put("info", info)
        Log.d(TAG, "获取播放链接: plugin=${slot.info.name}, source=$source, id=${song.platformId}")
        val ret = slot.runtime.callRequest(payload)
        Log.d(TAG, "播放链接返回: ${ret.toString().take(300)}")
        // Try multiple possible URL field names
        val url = ret.optString("url").ifBlank {
            ret.optString("musicUrl").ifBlank {
                ret.optJSONObject("data")?.optString("url").orEmpty()
            }
        }
        val headers = mutableMapOf<String, String>()
        val headersObj = ret.optJSONObject("headers")
            ?: ret.optJSONObject("data")?.optJSONObject("headers")
            ?: JSONObject()
        headersObj.keys().forEach { k ->
            headers[k] = headersObj.optString(k)
        }
        if (url.isBlank()) throw IllegalStateException(ret.optString("error", "plugin return empty url"))
        return LxMusicUrlResult(url, headers)
    }

    private suspend fun doLyric(slot: PluginSlot, source: String, song: Song): LxLyricResult {
        // Build musicInfo from original search result JSON
        val musicInfo = if (song.pluginRawJson.isNotBlank()) {
            try {
                JSONObject(song.pluginRawJson).also { raw ->
                    if (!raw.has("name")) raw.put("name", song.title)
                    if (!raw.has("singer")) raw.put("singer", song.artist)
                    ensurePlatformIdAliases(raw, source, song.platformId)
                }
            } catch (_: Exception) {
                buildFallbackMusicInfo(source, song)
            }
        } else {
            buildFallbackMusicInfo(source, song)
        }
        val info = JSONObject()
            .put("type", "lrc")
            .put("musicInfo", musicInfo)
        val payload = JSONObject()
            .put("source", source)
            .put("action", "lyric")
            .put("info", info)
        Log.d(TAG, "获取歌词: plugin=${slot.info.name}, source=$source, id=${song.platformId}")
        val ret = slot.runtime.callRequest(payload)
        Log.d(TAG, "歌词返回: ${ret.toString().take(300)}")
        val error = ret.optString("error")
        if (error.isNotBlank()) {
            Log.w(TAG, "歌词获取错误: $error")
            return LxLyricResult()
        }
        // Plugin may return: { lyricText: "..." } or { lyric: "..." } or { lrcText: "..." }
        // or directly a string URL/text
        val lyric = ret.optString("lyricText").ifBlank {
            ret.optString("lyric").ifBlank {
                ret.optString("lrcText").ifBlank {
                    ret.optString("url").ifBlank {
                        ret.optJSONObject("data")?.optString("lyric").orEmpty()
                    }
                }
            }
        }
        val tlyric = ret.optString("tlyricText").ifBlank {
            ret.optString("tlyric").ifBlank {
                ret.optJSONObject("data")?.optString("tlyric").orEmpty()
            }
        }
        return LxLyricResult(lyric = lyric, tlyric = tlyric)
    }

    /**
     * Load a plugin directly from raw script text and a virtual URI (for persistence).
     * Used by online import where the script is downloaded from a URL.
     */
    fun loadPluginFromScript(
        script: String,
        virtualUri: String,
        options: LxRuntimeOptions = LxRuntimeOptions(),
    ): Result<LxPluginEntry> = runCatching {
        val bytes = script.toByteArray(Charsets.UTF_8)
        if (bytes.size > 2 * 1024 * 1024) throw IllegalArgumentException("插件大小不能超过2MB")
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { b -> "%02x".format(b) }
        val info = LxPluginHeaderParser.parse(script)

        plugins[hash]?.runtime?.close()

        val runtime = LxJsRuntime(context, quickJsFactory(), options, logCallback)
        runtime.loadPluginScript(script)

        val inited = runtime.getInitedPayload() ?: JSONObject()
        val srcObj = inited.optJSONObject("sources") ?: JSONObject()
        val srcList = mutableListOf<LxSourceMeta>()
        srcObj.keys().forEach { key ->
            val s = srcObj.optJSONObject(key) ?: JSONObject()
            srcList += LxSourceMeta(
                key = key,
                name = s.optString("name", key),
                qualitys = s.optJSONArray("qualitys").toStringList(),
                actions = s.optJSONArray("actions").toStringList(),
            )
        }

        val slot = PluginSlot(
            id = hash,
            uri = virtualUri,
            info = info,
            sources = srcList,
            runtime = runtime,
        )
        plugins[hash] = slot

        LxPluginEntry(
            id = hash,
            uri = virtualUri,
            info = info,
            sources = srcList.map { it.key },
            initialized = runtime.hasHandler(),
        )
    }

    /**
     * Build a fallback musicInfo JSONObject when pluginRawJson is missing
     * (e.g. song came from server API, not plugin search).
     * Includes platform-specific ID field aliases that plugins expect.
     */
    private fun buildFallbackMusicInfo(source: String, song: Song): JSONObject {
        val obj = JSONObject()
            .put("id", song.platformId)
            .put("name", song.title)
            .put("singer", song.artist)
            .put("artists", JSONArray().put(JSONObject().put("name", song.artist)))
        val typesArr = JSONArray()
        listOf("128k", "320k", "flac").forEach { typesArr.put(JSONObject().put("type", it)) }
        obj.put("types", typesArr)
        ensurePlatformIdAliases(obj, source, song.platformId)
        return obj
    }

    /**
     * Ensure platform-specific ID aliases exist in musicInfo.
     * Different plugins read different field names for the song identifier:
     * - QQ: songmid, strMediaMid
     * - Kugou: hash
     * - Kuwo: rid, musicrid
     * - Netease: id (already present)
     * - Migu: copyrightId, id
     */
    private fun ensurePlatformIdAliases(obj: JSONObject, source: String, platformId: String) {
        val lower = source.lowercase()
        when {
            lower == "tx" || lower.startsWith("qq") || lower == "qsvip" -> {
                if (!obj.has("songmid")) obj.put("songmid", platformId)
                if (!obj.has("strMediaMid")) obj.put("strMediaMid", platformId)
                if (!obj.has("mid")) obj.put("mid", platformId)
            }
            lower == "kg" || lower.startsWith("kugou") -> {
                if (!obj.has("hash")) obj.put("hash", platformId)
            }
            lower == "kw" || lower.startsWith("kuwo") -> {
                if (!obj.has("rid")) obj.put("rid", platformId)
                if (!obj.has("musicrid")) obj.put("musicrid", "MUSIC_$platformId")
            }
            lower == "mg" || lower.startsWith("migu") -> {
                if (!obj.has("copyrightId")) obj.put("copyrightId", platformId)
            }
        }
    }

    fun readScriptAndHash(pluginUri: String): Pair<String, String> {
        val maxPluginSize = 2 * 1024 * 1024
        val bytes = contentResolver.openInputStream(Uri.parse(pluginUri))?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                total += n
                if (total > maxPluginSize) throw IllegalArgumentException("插件大小不能超过2MB")
                output.write(buffer, 0, n)
            }
            output.toByteArray()
        } ?: throw IllegalStateException("read plugin failed")
        val text = bytes.toString(Charsets.UTF_8)
        val hash = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { b -> "%02x".format(b) }
        return text to hash
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { idx -> optString(idx).takeIf { it.isNotBlank() } }
}
