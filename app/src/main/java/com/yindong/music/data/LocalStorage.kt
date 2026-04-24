package com.yindong.music.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.yindong.music.data.model.Playlist
import com.yindong.music.data.model.Song
import com.yindong.music.data.lx.LxPluginEntry
import com.yindong.music.data.lx.LxPluginInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 本地持久化存储
 *
 * 数据保存在 SharedPreferences 中，卸载 app 后清除
 * 支持: 播放历史、搜索历史、音质设置
 */
object LocalStorage {

    private const val PREFS_NAME = "cloud_music_data"
    private const val KEY_PLAY_HISTORY = "play_history"
    private const val KEY_SEARCH_HISTORY = "search_history"
    private const val KEY_QUALITY = "quality"
    private const val KEY_PLAY_COUNT = "play_count"
    private const val KEY_SEARCH_COUNT = "search_count"
    private const val KEY_PLAYLISTS = "user_playlists"
    private const val KEY_FAVORITES = "favorites"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_PLAY_MODE = "play_mode"
    private const val KEY_PLAYER_STYLE = "player_style"
    private const val KEY_EQ_PRESET = "eq_preset"
    private const val KEY_EQ_EFFECT_NAME = "eq_effect_name"
    private const val KEY_DEV_MODE = "dev_mode"
    private const val KEY_QQ_COOKIE = "qq_cookie"
    private const val KEY_API_HOST = "api_host"
    private const val KEY_QQ_API_KEY = "qq_api_key"
    private const val KEY_DOUYIN_API_KEY = "douyin_api_key"
    private const val KEY_NIANXIN_API_KEY = "nianxin_api_key"
    private const val KEY_API_MODE = "api_mode"
    private const val KEY_QQ_MUSIC_API_KEY = "qq_music_api_key"
    private const val KEY_NETEASE_API_KEY = "netease_api_key"
    private const val KEY_KUWO_API_KEY = "kuwo_api_key"
    private const val KEY_MIGU_API_KEY = "migu_api_key"
    private const val KEY_KUGOU_API_KEY = "kugou_api_key"
    private const val KEY_QQ_PLAY_API = "qq_play_api"
    private const val KEY_SERVER_URL = "server_url"
    private const val KEY_DOUYIN_PARSE_API = "douyin_parse_api"
    private const val KEY_FLOATING_LYRIC_COLOR = "floating_lyric_color"
    private const val KEY_FLOATING_LYRIC_SIZE = "floating_lyric_size"
    private const val KEY_LYRIC_CURRENT_COLOR = "lyric_current_color"
    private const val KEY_LYRIC_NORMAL_COLOR = "lyric_normal_color"
    private const val KEY_LYRIC_FONT_SIZE = "lyric_font_size"
    private const val KEY_USER_TOKEN = "user_token"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_SOURCE_ENABLED_PREFIX = "source_enabled_"
    private const val KEY_LX_PLUGIN_URI = "lx_plugin_uri"
    private const val KEY_LX_PLUGIN_HASH = "lx_plugin_hash"
    private const val KEY_LX_PLUGIN_INFO = "lx_plugin_info"
    private const val KEY_LX_SELECTED_SOURCE = "lx_selected_source"
    private const val KEY_LX_TIMEOUT_MS = "lx_timeout_ms"
    private const val KEY_LX_ALLOW_HTTP = "lx_allow_http"
    private const val KEY_LX_PLUGINS = "lx_plugins"
    private const val KEY_LX_SELECTED_PLUGIN_ID = "lx_selected_plugin_id"
    private const val KEY_LX_DISABLED_PLUGINS = "lx_disabled_plugins"
    private const val KEY_LX_DISABLED_SOURCES = "lx_disabled_sources"
    private const val KEY_BUILTIN_PLUGIN_HASH = "builtin_plugin_hash"
    private const val KEY_BUILTIN_PLUGIN_SCRIPT = "builtin_plugin_script"
    private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"

    private var prefs: SharedPreferences? = null
    private var securePrefs: SharedPreferences? = null

    /** 是否已初始化 */
    val isInitialized: Boolean get() = prefs != null

    private const val KEY_EXPORT_VERSION = "export_version"
    private const val EXPORT_FILE_VERSION = 1

    fun init(context: Context) {
        if (prefs == null) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        if (securePrefs == null) {
            try {
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                securePrefs = EncryptedSharedPreferences.create(
                    "cloud_music_secure",
                    masterKeyAlias,
                    context.applicationContext,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (e: Exception) {
                Log.w("LocalStorage", "加密存储不可用: ${e.message}")
            }
        }
    }

    /** 安全获取 prefs，未初始化时返回 null 避免崩溃 */
    private fun safePrefs(): SharedPreferences? = prefs

    // ── 加密存储辅助方法 (敏感数据自动迁移) ──

    private fun saveSecure(key: String, value: String) {
        val sp = securePrefs
        if (sp != null) {
            sp.edit().putString(key, value).apply()
            safePrefs()?.edit()?.remove(key)?.apply()
        } else {
            safePrefs()?.edit()?.putString(key, value)?.apply()
        }
    }

    private fun loadSecure(key: String, default: String = ""): String {
        val sp = securePrefs
        if (sp != null) {
            sp.getString(key, null)?.let { return it }
            // 回退: 从明文prefs迁移到加密prefs
            val plain = safePrefs()?.getString(key, null)
            if (plain != null) {
                sp.edit().putString(key, plain).apply()
                safePrefs()?.edit()?.remove(key)?.apply()
                return plain
            }
            return default
        }
        return safePrefs()?.getString(key, default) ?: default
    }

    private fun saveSecureInt(key: String, value: Int) {
        val sp = securePrefs
        if (sp != null) {
            sp.edit().putInt(key, value).apply()
            safePrefs()?.edit()?.remove(key)?.apply()
        } else {
            safePrefs()?.edit()?.putInt(key, value)?.apply()
        }
    }

    private fun loadSecureInt(key: String, default: Int = 0): Int {
        val sp = securePrefs
        if (sp != null) {
            if (sp.contains(key)) return sp.getInt(key, default)
            val plain = safePrefs()
            if (plain != null && plain.contains(key)) {
                val value = plain.getInt(key, default)
                sp.edit().putInt(key, value).apply()
                plain.edit().remove(key).apply()
                return value
            }
            return default
        }
        return safePrefs()?.getInt(key, default) ?: default
    }

    private fun removeSecure(key: String) {
        securePrefs?.edit()?.remove(key)?.apply()
        safePrefs()?.edit()?.remove(key)?.apply()
    }

    // ── 播放历史 ──

    fun savePlayHistory(songs: List<Song>) {
        val p = safePrefs() ?: return
        val arr = JSONArray()
        songs.take(200).forEach { song ->
            arr.put(songToJson(song))
        }
        p.edit().putString(KEY_PLAY_HISTORY, arr.toString()).apply()
    }

    fun loadPlayHistory(): List<Song> {
        val str = safePrefs()?.getString(KEY_PLAY_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) {
                jsonToSong(arr.getJSONObject(i))?.let { list.add(it) }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── 搜索历史 ──

    fun saveSearchHistory(history: List<String>) {
        val p = safePrefs() ?: return
        val arr = JSONArray()
        history.take(20).forEach { arr.put(it) }
        p.edit().putString(KEY_SEARCH_HISTORY, arr.toString()).apply()
    }

    fun loadSearchHistory(): List<String> {
        val str = safePrefs()?.getString(KEY_SEARCH_HISTORY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                list.add(arr.getString(i))
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── 音质设置 ──

    fun saveQuality(qualityKey: String) {
        safePrefs()?.edit()?.putString(KEY_QUALITY, qualityKey)?.apply()
    }

    fun loadQuality(): String {
        return safePrefs()?.getString(KEY_QUALITY, "exhigh") ?: "exhigh"
    }

    // ── 播放/搜索计数 ──

    fun savePlayCount(count: Int) {
        safePrefs()?.edit()?.putInt(KEY_PLAY_COUNT, count)?.apply()
    }

    fun loadPlayCount(): Int = safePrefs()?.getInt(KEY_PLAY_COUNT, 0) ?: 0

    fun saveSearchCount(count: Int) {
        safePrefs()?.edit()?.putInt(KEY_SEARCH_COUNT, count)?.apply()
    }

    fun loadSearchCount(): Int = safePrefs()?.getInt(KEY_SEARCH_COUNT, 0) ?: 0

    // ── 主题模式 ──

    fun saveDarkMode(isDark: Boolean) {
        safePrefs()?.edit()?.putBoolean(KEY_DARK_MODE, isDark)?.apply()
    }

    fun loadDarkMode(): Boolean = safePrefs()?.getBoolean(KEY_DARK_MODE, false) ?: false

    // ── 播放模式 ──

    fun savePlayMode(mode: String) { safePrefs()?.edit()?.putString(KEY_PLAY_MODE, mode)?.apply() }
    fun loadPlayMode(): String = safePrefs()?.getString(KEY_PLAY_MODE, "LOOP") ?: "LOOP"

    fun savePlayerStyle(style: String) { safePrefs()?.edit()?.putString(KEY_PLAYER_STYLE, style)?.apply() }
    fun loadPlayerStyle(): String = safePrefs()?.getString(KEY_PLAYER_STYLE, "VINYL") ?: "VINYL"

    fun saveEqPreset(name: String) { safePrefs()?.edit()?.putString(KEY_EQ_PRESET, name)?.apply() }
    fun loadEqPreset(): String = safePrefs()?.getString(KEY_EQ_PRESET, "FLAT") ?: "FLAT"

    fun saveEqEffectName(name: String) { safePrefs()?.edit()?.putString(KEY_EQ_EFFECT_NAME, name)?.apply() }
    fun loadEqEffectName(): String = safePrefs()?.getString(KEY_EQ_EFFECT_NAME, "") ?: ""

    // ── 开发者模式 ──

    fun saveDevMode(enabled: Boolean) { safePrefs()?.edit()?.putBoolean(KEY_DEV_MODE, enabled)?.apply() }
    fun loadDevMode(): Boolean = safePrefs()?.getBoolean(KEY_DEV_MODE, false) ?: false

    // ── QQ Cookie (用于获取完整版播放链接) ──

    fun saveQQCookie(cookie: String) { saveSecure(KEY_QQ_COOKIE, cookie) }
    fun loadQQCookie(): String = loadSecure(KEY_QQ_COOKIE)

    // ── API地址 ──

    private const val DEFAULT_API_HOST = ""
    fun saveApiHost(host: String) { safePrefs()?.edit()?.putString(KEY_API_HOST, host)?.apply() }
    fun loadApiHost(): String {
        val saved = safePrefs()?.getString(KEY_API_HOST, DEFAULT_API_HOST) ?: DEFAULT_API_HOST
        return if (saved.isNotEmpty()) saved else DEFAULT_API_HOST
    }

    fun saveQQApiKey(key: String) { saveSecure(KEY_QQ_API_KEY, key) }
    fun loadQQApiKey(): String = loadSecure(KEY_QQ_API_KEY)

    fun saveDouyinApiKey(key: String) { saveSecure(KEY_DOUYIN_API_KEY, key) }
    fun loadDouyinApiKey(): String = loadSecure(KEY_DOUYIN_API_KEY)

    fun saveNianxinApiKey(key: String) { saveSecure(KEY_NIANXIN_API_KEY, key) }
    fun loadNianxinApiKey(): String = loadSecure(KEY_NIANXIN_API_KEY)

    fun saveApiMode(mode: String) { safePrefs()?.edit()?.putString(KEY_API_MODE, mode)?.apply() }
    fun loadApiMode(): String = safePrefs()?.getString(KEY_API_MODE, "official") ?: "official"
    fun saveLxPluginUri(uri: String) { safePrefs()?.edit()?.putString(KEY_LX_PLUGIN_URI, uri)?.apply() }
    fun loadLxPluginUri(): String = safePrefs()?.getString(KEY_LX_PLUGIN_URI, "") ?: ""
    fun saveLxPluginHash(hash: String) { safePrefs()?.edit()?.putString(KEY_LX_PLUGIN_HASH, hash)?.apply() }
    fun loadLxPluginHash(): String = safePrefs()?.getString(KEY_LX_PLUGIN_HASH, "") ?: ""
    fun saveLxSelectedSource(source: String) { safePrefs()?.edit()?.putString(KEY_LX_SELECTED_SOURCE, source)?.apply() }
    fun loadLxSelectedSource(): String = safePrefs()?.getString(KEY_LX_SELECTED_SOURCE, "") ?: ""
    fun saveLxTimeoutMs(timeoutMs: Long) { safePrefs()?.edit()?.putLong(KEY_LX_TIMEOUT_MS, timeoutMs)?.apply() }
    fun loadLxTimeoutMs(): Long = safePrefs()?.getLong(KEY_LX_TIMEOUT_MS, 5000L) ?: 5000L
    fun saveLxAllowHttp(allow: Boolean) { safePrefs()?.edit()?.putBoolean(KEY_LX_ALLOW_HTTP, allow)?.apply() }
    fun loadLxAllowHttp(): Boolean = safePrefs()?.getBoolean(KEY_LX_ALLOW_HTTP, false) ?: false
    fun saveLxPluginInfo(info: LxPluginInfo) {
        val obj = JSONObject()
            .put("name", info.name)
            .put("version", info.version)
            .put("author", info.author)
            .put("description", info.description)
            .put("homepage", info.homepage)
        safePrefs()?.edit()?.putString(KEY_LX_PLUGIN_INFO, obj.toString())?.apply()
    }
    fun loadLxPluginInfo(): LxPluginInfo {
        val raw = safePrefs()?.getString(KEY_LX_PLUGIN_INFO, "").orEmpty()
        if (raw.isBlank()) return LxPluginInfo()
        return try {
            val obj = JSONObject(raw)
            LxPluginInfo(
                name = obj.optString("name"),
                version = obj.optString("version"),
                author = obj.optString("author"),
                description = obj.optString("description"),
                homepage = obj.optString("homepage"),
            )
        } catch (_: Exception) {
            LxPluginInfo()
        }
    }

    // ── 多插件持久化 ──

    fun saveLxPlugins(plugins: List<LxPluginEntry>) {
        val arr = JSONArray()
        plugins.forEach { p ->
            arr.put(JSONObject().apply {
                put("id", p.id)
                put("uri", p.uri)
                put("name", p.info.name)
                put("version", p.info.version)
                put("author", p.info.author)
                put("description", p.info.description)
                put("homepage", p.info.homepage)
                put("sources", JSONArray(p.sources))
            })
        }
        safePrefs()?.edit()?.putString(KEY_LX_PLUGINS, arr.toString())?.apply()
    }

    fun loadLxPlugins(): List<LxPluginEntry> {
        val raw = safePrefs()?.getString(KEY_LX_PLUGINS, "").orEmpty()
        if (raw.isBlank()) {
            // Migrate from single-plugin format if present
            val singleUri = loadLxPluginUri()
            val singleHash = loadLxPluginHash()
            val singleInfo = loadLxPluginInfo()
            if (singleUri.isNotBlank() && singleHash.isNotBlank()) {
                return listOf(LxPluginEntry(id = singleHash, uri = singleUri, info = singleInfo))
            }
            return emptyList()
        }
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                val srcArr = obj.optJSONArray("sources")
                val sources = if (srcArr != null) {
                    (0 until srcArr.length()).mapNotNull { j -> srcArr.optString(j).takeIf { it.isNotBlank() } }
                } else emptyList()
                LxPluginEntry(
                    id = obj.optString("id"),
                    uri = obj.optString("uri"),
                    info = LxPluginInfo(
                        name = obj.optString("name"),
                        version = obj.optString("version"),
                        author = obj.optString("author"),
                        description = obj.optString("description"),
                        homepage = obj.optString("homepage"),
                    ),
                    sources = sources,
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun saveLxSelectedPluginId(id: String) { safePrefs()?.edit()?.putString(KEY_LX_SELECTED_PLUGIN_ID, id)?.apply() }
    fun loadLxSelectedPluginId(): String = safePrefs()?.getString(KEY_LX_SELECTED_PLUGIN_ID, "") ?: ""

    // ── 插件启用/禁用状态 ──

    fun saveDisabledPlugins(ids: Set<String>) {
        safePrefs()?.edit()?.putStringSet(KEY_LX_DISABLED_PLUGINS, ids)?.apply()
    }

    fun loadDisabledPlugins(): Set<String> {
        return safePrefs()?.getStringSet(KEY_LX_DISABLED_PLUGINS, emptySet()) ?: emptySet()
    }

    /** key 格式: "pluginId:sourceKey" */
    fun saveDisabledSources(keys: Set<String>) {
        safePrefs()?.edit()?.putStringSet(KEY_LX_DISABLED_SOURCES, keys)?.apply()
    }

    fun loadDisabledSources(): Set<String> {
        return safePrefs()?.getStringSet(KEY_LX_DISABLED_SOURCES, emptySet()) ?: emptySet()
    }

    // ── 内置插件缓存 ──

    fun saveBuiltinPluginHash(hash: String) { safePrefs()?.edit()?.putString(KEY_BUILTIN_PLUGIN_HASH, hash)?.apply() }
    fun loadBuiltinPluginHash(): String = safePrefs()?.getString(KEY_BUILTIN_PLUGIN_HASH, "") ?: ""

    fun saveBuiltinPluginScript(script: String) { safePrefs()?.edit()?.putString(KEY_BUILTIN_PLUGIN_SCRIPT, script)?.apply() }
    fun loadBuiltinPluginScript(): String = safePrefs()?.getString(KEY_BUILTIN_PLUGIN_SCRIPT, "") ?: ""

    // ── 免责声明已同意 ──
    fun saveDisclaimerAccepted(accepted: Boolean) { safePrefs()?.edit()?.putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted)?.apply() }
    fun loadDisclaimerAccepted(): Boolean = safePrefs()?.getBoolean(KEY_DISCLAIMER_ACCEPTED, false) ?: false

    fun saveQQMusicApiKey(key: String) { saveSecure(KEY_QQ_MUSIC_API_KEY, key) }
    fun loadQQMusicApiKey(): String = loadSecure(KEY_QQ_MUSIC_API_KEY)

    fun saveNeteaseApiKey(key: String) { saveSecure(KEY_NETEASE_API_KEY, key) }
    fun loadNeteaseApiKey(): String = loadSecure(KEY_NETEASE_API_KEY)

    fun saveKuwoApiKey(key: String) { saveSecure(KEY_KUWO_API_KEY, key) }
    fun loadKuwoApiKey(): String = loadSecure(KEY_KUWO_API_KEY)

    fun saveMiguApiKey(key: String) { saveSecure(KEY_MIGU_API_KEY, key) }
    fun loadMiguApiKey(): String = loadSecure(KEY_MIGU_API_KEY)

    fun saveKugouApiKey(key: String) { saveSecure(KEY_KUGOU_API_KEY, key) }
    fun loadKugouApiKey(): String = loadSecure(KEY_KUGOU_API_KEY)

    // ── 用户自定义API地址 ──

    fun saveQQPlayApi(url: String) { safePrefs()?.edit()?.putString(KEY_QQ_PLAY_API, url)?.apply() }
    fun loadQQPlayApi(): String = safePrefs()?.getString(KEY_QQ_PLAY_API, "") ?: ""

    // ── 服务器地址 ──
    fun saveServerUrl(url: String) { safePrefs()?.edit()?.putString(KEY_SERVER_URL, url)?.apply() }
    fun loadServerUrl(): String = safePrefs()?.getString(KEY_SERVER_URL, "") ?: ""

    fun saveDouyinParseApi(url: String) { safePrefs()?.edit()?.putString(KEY_DOUYIN_PARSE_API, url)?.apply() }
    fun loadDouyinParseApi(): String = safePrefs()?.getString(KEY_DOUYIN_PARSE_API, "") ?: ""

    // ── 悬浮歌词颜色 ──
    fun saveFloatingLyricColor(color: Int) { safePrefs()?.edit()?.putInt(KEY_FLOATING_LYRIC_COLOR, color)?.apply() }
    fun loadFloatingLyricColor(): Int = safePrefs()?.getInt(KEY_FLOATING_LYRIC_COLOR, 0xFFFF0000.toInt()) ?: 0xFFFF0000.toInt()

    // ── 悬浮歌词字体大小(sp) ──
    fun saveFloatingLyricSize(size: Int) { safePrefs()?.edit()?.putInt(KEY_FLOATING_LYRIC_SIZE, size)?.apply() }
    fun loadFloatingLyricSize(): Int = safePrefs()?.getInt(KEY_FLOATING_LYRIC_SIZE, 18) ?: 18

    // ── 歌词高亮颜色 ──
    fun saveLyricCurrentColor(color: Int) { safePrefs()?.edit()?.putInt(KEY_LYRIC_CURRENT_COLOR, color)?.apply() }
    fun loadLyricCurrentColor(): Int = safePrefs()?.getInt(KEY_LYRIC_CURRENT_COLOR, 0) ?: 0  // 0 = 自动

    fun saveLyricNormalColor(color: Int) { safePrefs()?.edit()?.putInt(KEY_LYRIC_NORMAL_COLOR, color)?.apply() }
    fun loadLyricNormalColor(): Int = safePrefs()?.getInt(KEY_LYRIC_NORMAL_COLOR, 0) ?: 0  // 0 = 自动

    // ── 歌词字体大小 (0 = 默认) ──
    fun saveLyricFontSize(size: Int) { safePrefs()?.edit()?.putInt(KEY_LYRIC_FONT_SIZE, size)?.apply() }
    fun loadLyricFontSize(): Int = safePrefs()?.getInt(KEY_LYRIC_FONT_SIZE, 0) ?: 0

    // ── 收藏歌曲 ──

    fun saveFavorites(songs: List<Song>) {
        val p = safePrefs() ?: return
        val arr = JSONArray()
        songs.forEach { arr.put(songToJson(it)) }
        p.edit().putString(KEY_FAVORITES, arr.toString()).apply()
    }

    fun loadFavorites(): List<Song> {
        val str = safePrefs()?.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<Song>()
            for (i in 0 until arr.length()) { jsonToSong(arr.getJSONObject(i))?.let { list.add(it) } }
            list
        } catch (_: Exception) { emptyList() }
    }

    // ── 用户歌单 ──

    fun savePlaylists(playlists: List<Playlist>) {
        val p = safePrefs() ?: return
        val arr = JSONArray()
        playlists.forEach { pl ->
            val obj = JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("creator", pl.creator)
                put("description", pl.description)
                val songsArr = JSONArray()
                pl.songs.forEach { songsArr.put(songToJson(it)) }
                put("songs", songsArr)
            }
            arr.put(obj)
        }
        p.edit().putString(KEY_PLAYLISTS, arr.toString()).apply()
    }

    fun loadPlaylists(): List<Playlist> {
        val str = safePrefs()?.getString(KEY_PLAYLISTS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(str)
            val list = mutableListOf<Playlist>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val songsArr = obj.optJSONArray("songs")
                val songs = mutableListOf<Song>()
                if (songsArr != null) {
                    for (j in 0 until songsArr.length()) {
                        jsonToSong(songsArr.getJSONObject(j))?.let { songs.add(it) }
                    }
                }
                list.add(
                    Playlist(
                        id = obj.optLong("id", 0),
                        name = obj.optString("name", ""),
                        creator = "我",
                        description = obj.optString("description", ""),
                        songCount = songs.size,
                        songs = songs,
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── 数据导出/导入 ──

    /**
     * 导出所有数据为 JSON 字符串
     */
    fun exportData(): String {
        val data = JSONObject().apply {
            put("version", EXPORT_FILE_VERSION)
            put("exportTime", System.currentTimeMillis())
            put("favorites", JSONArray(loadFavorites().map { songToJson(it) }))
            put("playlists", JSONArray(loadPlaylists().map { playlistToJson(it) }))
            put("playHistory", JSONArray(loadPlayHistory().map { songToJson(it) }))
            put("searchHistory", JSONArray(loadSearchHistory()))
            put("playCount", loadPlayCount())
            put("searchCount", loadSearchCount())
            put("quality", loadQuality())
            put("darkMode", loadDarkMode())
            put("playMode", loadPlayMode())
        }
        return data.toString(2) // 格式化缩进
    }

    /**
     * 从 JSON 字符串导入数据
     * @return 成功导入的歌曲数和歌单数
     */
    fun importData(jsonString: String): ImportResult {
        return try {
            val data = JSONObject(jsonString)
            
            // 导入收藏
            val favoritesArr = data.optJSONArray("favorites")
            if (favoritesArr != null) {
                val favorites = mutableListOf<Song>()
                for (i in 0 until favoritesArr.length()) {
                    jsonToSong(favoritesArr.getJSONObject(i))?.let { favorites.add(it) }
                }
                saveFavorites(favorites)
            }
            
            // 导入歌单
            val playlistsArr = data.optJSONArray("playlists")
            if (playlistsArr != null) {
                val playlists = mutableListOf<Playlist>()
                for (i in 0 until playlistsArr.length()) {
                    jsonToPlaylist(playlistsArr.getJSONObject(i))?.let { playlists.add(it) }
                }
                savePlaylists(playlists)
            }
            
            // 导入播放历史
            val historyArr = data.optJSONArray("playHistory")
            if (historyArr != null) {
                val history = mutableListOf<Song>()
                for (i in 0 until historyArr.length()) {
                    jsonToSong(historyArr.getJSONObject(i))?.let { history.add(it) }
                }
                savePlayHistory(history)
            }
            
            // 导入搜索历史
            val searchArr = data.optJSONArray("searchHistory")
            if (searchArr != null) {
                val searches = mutableListOf<String>()
                for (i in 0 until searchArr.length()) {
                    searches.add(searchArr.getString(i))
                }
                saveSearchHistory(searches)
            }
            
            // 导入设置
            data.optInt("playCount", -1).takeIf { it >= 0 }?.let { savePlayCount(it) }
            data.optInt("searchCount", -1).takeIf { it >= 0 }?.let { saveSearchCount(it) }
            data.optString("quality", "").takeIf { it.isNotEmpty() }?.let { saveQuality(it) }
            // 仅在 JSON 中明确包含 darkMode 字段时才覆盖，避免默认值 false 覆盖用户当前设置
            if (data.has("darkMode")) { saveDarkMode(data.optBoolean("darkMode", false)) }
            data.optString("playMode", "").takeIf { it.isNotEmpty() }?.let { savePlayMode(it) }
            
            val favoriteCount = favoritesArr?.length() ?: 0
            val playlistCount = playlistsArr?.length() ?: 0
            
            ImportResult.Success(favoriteCount, playlistCount)
        } catch (e: Exception) {
            ImportResult.Error(e.message ?: "导入失败")
        }
    }

    data class ImportResult(val success: Boolean, val favorites: Int, val playlists: Int, val error: String?) {
        companion object {
            fun Success(favorites: Int, playlists: Int) = ImportResult(true, favorites, playlists, null)
            fun Error(message: String) = ImportResult(false, 0, 0, message)
        }
    }

    // ── Playlist JSON 序列化 ──

    private fun playlistToJson(playlist: Playlist): JSONObject {
        return JSONObject().apply {
            put("id", playlist.id)
            put("name", playlist.name)
            put("coverUrl", playlist.coverUrl)
            put("creator", playlist.creator)
            put("playCount", playlist.playCount)
            put("songCount", playlist.songCount)
            put("description", playlist.description)
            put("tags", JSONArray(playlist.tags))
            val songsArr = JSONArray()
            playlist.songs.forEach { songsArr.put(songToJson(it)) }
            put("songs", songsArr)
        }
    }

    private fun jsonToPlaylist(json: JSONObject): Playlist? {
        return try {
            val songsArr = json.optJSONArray("songs")
            val songs = mutableListOf<Song>()
            if (songsArr != null) {
                for (i in 0 until songsArr.length()) {
                    jsonToSong(songsArr.getJSONObject(i))?.let { songs.add(it) }
                }
            }
            Playlist(
                id = json.optLong("id", System.currentTimeMillis()),
                name = json.optString("name", "未命名歌单"),
                coverUrl = firstStoredImageUrl(
                    json.optString("coverUrl", ""),
                    json.optString("cover", ""),
                    json.optString("picUrl", ""),
                    songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                ),
                creator = json.optString("creator", "我"),
                playCount = json.optLong("playCount", 0),
                description = json.optString("description", ""),
                tags = runCatching {
                    val arr = json.optJSONArray("tags") ?: JSONArray()
                    buildList {
                        for (i in 0 until arr.length()) {
                            val t = arr.optString(i, "").trim()
                            if (t.isNotEmpty()) add(t)
                        }
                    }
                }.getOrDefault(emptyList()),
                songCount = songs.size,
                songs = songs
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun normalizeStoredImageUrl(raw: String?): String {
        var value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ""
        if (value.equals("null", ignoreCase = true) || value.equals("undefined", ignoreCase = true)) return ""
        value = value.replace("\\/", "/").replace("&amp;", "&").trim()
        if (value.startsWith("//")) value = "https:$value"
        if (value.startsWith("/")) {
            val server = loadServerUrl().trim().trimEnd('/')
            if (server.startsWith("http://") || server.startsWith("https://")) return "$server$value"
        }
        return if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("data:image", ignoreCase = true)
        ) value else ""
    }

    private fun firstStoredImageUrl(vararg candidates: String?): String {
        for (candidate in candidates) {
            val normalized = normalizeStoredImageUrl(candidate)
            if (normalized.isNotEmpty()) return normalized
        }
        return ""
    }

    // ── Song JSON 序列化 ──

    private fun songToJson(song: Song): JSONObject {
        return JSONObject().apply {
            put("id", song.id)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("duration", song.duration)
            put("coverUrl", song.coverUrl)
            put("artistPicUrl", song.artistPicUrl)
            put("isLiked", song.isLiked)
            put("platform", song.platform)
            put("platformId", song.platformId)
            put("directUrl", song.directUrl)
        }
    }

    private fun jsonToSong(json: JSONObject): Song? {
        return try {
            val artistPic = firstStoredImageUrl(
                json.optString("artistPicUrl", ""),
                json.optString("artistPic", ""),
                json.optString("singerPic", ""),
            )
            val cover = firstStoredImageUrl(
                json.optString("coverUrl", ""),
                json.optString("cover", ""),
                json.optString("picUrl", ""),
                json.optString("pic", ""),
                json.optString("imgUrl", ""),
                json.optString("img", ""),
                artistPic,
            )
            Song(
                id = json.optLong("id", 0),
                title = json.optString("title", ""),
                artist = json.optString("artist", ""),
                album = json.optString("album", ""),
                duration = json.optLong("duration", 0),
                coverUrl = cover,
                artistPicUrl = artistPic,
                isLiked = json.optBoolean("isLiked", false),
                platform = json.optString("platform", ""),
                platformId = json.optString("platformId", ""),
                directUrl = json.optString("directUrl", ""),
            )
        } catch (_: Exception) {
            null
        }
    }

    // ═══════════════════════════════════════════
    //  用户登录状态
    // ═══════════════════════════════════════════

    fun saveUserAuth(token: String, username: String, userId: Int) {
        saveSecure(KEY_USER_TOKEN, token)
        saveSecure(KEY_USER_NAME, username)
        saveSecureInt(KEY_USER_ID, userId)
    }

    fun loadUserToken(): String = loadSecure(KEY_USER_TOKEN)
    fun loadUserName(): String = loadSecure(KEY_USER_NAME)
    fun loadUserId(): Int = loadSecureInt(KEY_USER_ID)
    fun isLoggedIn(): Boolean = loadUserToken().isNotEmpty()

    fun clearUserAuth() {
        removeSecure(KEY_USER_TOKEN)
        removeSecure(KEY_USER_NAME)
        removeSecure(KEY_USER_ID)
    }

    // ═══════════════════════════════════════════
    //  设备标识（用于严格鉴权）
    // ═══════════════════════════════════════════

    fun loadDeviceId(): String = loadSecure(KEY_DEVICE_ID)

    fun loadOrCreateDeviceId(): String {
        val existing = loadSecure(KEY_DEVICE_ID).trim()
        if (existing.isNotEmpty()) return existing
        val generated = UUID.randomUUID().toString().replace("-", "")
        saveSecure(KEY_DEVICE_ID, generated)
        return generated
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ 新增设置功能的持久化：自动更新、音源管理 ═══
    // ═══════════════════════════════════════════════════════════



    // ── 音源启用状态 ──
    fun saveSourceEnabled(source: String, enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_SOURCE_ENABLED_PREFIX + source, enabled)?.apply()
    }

    fun loadSourceEnabled(source: String): Boolean {
        // 默认全部启用
        return prefs?.getBoolean(KEY_SOURCE_ENABLED_PREFIX + source, true) ?: true
    }

    // ═══════════════════════════════════════════════════════════

    // ── 通用字符串存取 ──
    fun saveString(key: String, value: String) {
        safePrefs()?.edit()?.putString(key, value)?.apply()
    }
    fun loadString(key: String): String {
        return safePrefs()?.getString(key, "") ?: ""
    }
}
