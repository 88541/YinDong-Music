package com.yindong.music.data

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import com.yindong.music.data.api.MusicApiService
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 远程配置管理器 — 服务器完全控制APP一切行为
 *
 * 服务器后台 → /api/app_config → APP读取 → 应用所有控制
 */
object RemoteConfig {

    private const val TAG = "RemoteConfig"
    private val SHA256_REGEX = Regex("^[a-f0-9]{64}$")
    private val officialJoinHosts = setOf("qm.qq.com", "qun.qq.com")

    // 复用 MusicApiService 的连接池和DNS缓存，缩短超时避免启动阻塞
    private val client: OkHttpClient by lazy {
        MusicApiService.sharedClient().newBuilder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    // ═══════ 总开关 ═══════
    var appEnabled: Boolean = true; private set
    var announcement: String = ""; private set
    var announcementType: String = "info"; private set
    var showAnnouncement: Boolean = false; private set

    // ═══════ 版本控制 ═══════
    var minVersion: String = "1.0.0"; private set
    var latestVersion: String = ""; private set
    var updateUrl: String = ""; private set
    var updateApkSha256: String = ""; private set
    var updateLog: String = ""; private set
    var forceUpdate: Boolean = false; private set

    // ═══════ 音乐平台开关 ═══════
    var platformEnabled: MutableMap<String, Boolean> = mutableMapOf(
        "QQ音乐" to true, "网易云" to true, "酷我音乐" to true,
        "酷狗音乐" to true, "汽水音乐" to true
    ); private set

    // ═══════ 功能开关 ═══════
    var featureSearch: Boolean = true; private set
    var featurePlay: Boolean = true; private set
    var featureLyrics: Boolean = true; private set
    var featureHotChart: Boolean = true; private set
    var featureDownload: Boolean = true; private set
    var featureDouyinParse: Boolean = true; private set
    var featureShare: Boolean = true; private set

    // ═══════ UI控制 ═══════
    var theme: String = "default"; private set
    var splashImageUrl: String = ""; private set
    var splashDuration: Int = 2; private set
    var noticeBarText: String = ""; private set
    var noticeBarColor: String = "#FF6B6B"; private set
    var homeBannerUrl: String = ""; private set
    var homeBannerClickUrl: String = ""; private set
    var adEnabled: Boolean = false; private set
    var adImageUrl: String = ""; private set
    var adClickUrl: String = ""; private set
    var adDuration: Int = 5; private set
    var officialCommunityQq: String = ""; private set
    var officialAuthorQq: String = ""; private set
    var officialCommunityJoinUrl: String = ""; private set
    var officialCommunityTitle: String = ""; private set
    var officialCommunitySubtitle: String = ""; private set
    var forceShowOfficialContact: Boolean = false; private set

    // ═══════ 频率限制 ═══════
    var searchPerMinute: Int = 30; private set
    var playPerMinute: Int = 60; private set

    // ═══════ API地址 (服务器下发) ═══════
    var apiHost: String = ""; private set
    var serverUrl: String = ""; private set
    var runtimeClientMode: String = "official"; private set
    var runtimePlaybackStreamMode: String = "stream"; private set
    var runtimeBuiltinPluginEnabled: Boolean = true; private set
    var runtimeBuiltinPluginAutoUpdate: Boolean = true; private set
    var runtimePortPool: String = ""; private set
    var runtimeRequireHttpsOnly: Boolean = false; private set
    var runtimeEnforcePluginHashAllowlist: Boolean = true; private set
    var runtimePluginHashAllowlist: Set<String> = emptySet(); private set
    var runtimePluginHashBanned: Set<String> = emptySet(); private set

    // ═══════ 自定义数据 ═══════
    var customData: JSONObject = JSONObject(); private set

    // ═══════ 状态 ═══════
    var isLoaded: Boolean = false; private set
    var lastError: String? = null; private set

    /**
     * 从服务器拉取远程配置 (IO线程)
     */
    suspend fun fetch(): Boolean = withContext(Dispatchers.IO) {
        var finalErr: String? = null
        for (baseUrl in MusicApiService.serverBaseUrls()) {
            val configPaths = listOf("/api/client/config", "/api/app_config")
            for (path in configPaths) {
                try {
                    val request = MusicApiService.attachServerAuthHeaders(
                        Request.Builder().url("${baseUrl.trimEnd('/')}$path"),
                        includeChallengeToken = false
                    ).build()
                    val (body, httpCode) = client.newCall(request).execute().use { response ->
                        Pair(response.body?.string(), response.code)
                    }

                    if (httpCode !in 200..299 || body.isNullOrEmpty()) {
                        finalErr = "HTTP $httpCode"
                        Log.w(TAG, "拉取远程配置失败($baseUrl$path): HTTP $httpCode")
                        continue
                    }

                    val root = JSONObject(body)
                    if (root.optInt("code") != 200) {
                        finalErr = root.optString("msg", "未知错误")
                        continue
                    }

                    val data = root.optJSONObject("data") ?: continue
                    parseConfig(data)
                    isLoaded = true
                    lastError = null
                    MusicApiService.markServerHealthy(baseUrl)
                    Log.d(TAG, "远程配置已加载: appEnabled=$appEnabled, base=$baseUrl, path=$path")
                    return@withContext true
                } catch (e: Exception) {
                    finalErr = e.message
                    Log.w(TAG, "拉取远程配置异常($baseUrl$path): ${e.message}")
                }
            }
        }
        lastError = finalErr
        false
    }

    private fun parseConfig(data: JSONObject) {
        // 总开关
        appEnabled = data.optBoolean("app_enabled", true)
        announcement = data.optString("announcement", "")
        announcementType = data.optString("announcement_type", "info")
        showAnnouncement = data.optBoolean("show_announcement", false)

        // 版本
        minVersion = data.optString("min_version", "1.0.0")
        latestVersion = data.optString("latest_version", "")
        updateUrl = data.optString("update_url", "")
        updateApkSha256 = data.optString("update_apk_sha256", "").trim().lowercase()
        updateLog = data.optString("update_log", "")
        forceUpdate = data.optBoolean("force_update", false)

        // 音乐平台开关
        val mp = data.optJSONObject("music_platforms")
        if (mp != null) {
            val iter = mp.keys()
            while (iter.hasNext()) {
                val key = iter.next()
                val obj = mp.optJSONObject(key)
                platformEnabled[key] = obj?.optBoolean("enabled", true) ?: true
            }
        }

        // 功能开关
        val ft = data.optJSONObject("features")
        if (ft != null) {
            featureSearch = ft.optBoolean("music_search", true)
            featurePlay = ft.optBoolean("music_play", true)
            featureLyrics = ft.optBoolean("lyrics_display", true)
            featureHotChart = ft.optBoolean("hot_chart", true)
            featureDownload = ft.optBoolean("download", true)
            featureDouyinParse = ft.optBoolean("douyin_parse", true)
            featureShare = ft.optBoolean("share", true)
        }

        // UI控制
        val ui = data.optJSONObject("ui_config")
        if (ui != null) {
            theme = ui.optString("theme", "default")
            splashImageUrl = ui.optString("splash_image_url", "")
            splashDuration = ui.optInt("splash_duration", 2)
            noticeBarText = ui.optString("notice_bar_text", "")
            noticeBarColor = ui.optString("notice_bar_color", "#FF6B6B")
            homeBannerUrl = ui.optString("home_banner_url", "")
            homeBannerClickUrl = ui.optString("home_banner_click_url", "")
            adEnabled = ui.optBoolean("ad_enabled", false)
            adImageUrl = ui.optString("ad_image_url", "")
            adClickUrl = ui.optString("ad_click_url", "")
            adDuration = ui.optInt("ad_duration", 5)
        }
        val custom = data.optJSONObject("custom_data") ?: JSONObject()
        val official = data.optJSONObject("official_contact")
            ?: custom.optJSONObject("official_contact")
            ?: JSONObject()
        officialCommunityQq = sanitizeQq(
            firstNonBlank(
                official.optString("group_qq", ""),
                official.optString("qq_group", ""),
                official.optString("community_qq", ""),
                custom.optString("group_qq", ""),
                custom.optString("qq_group", ""),
            )
        )
        officialAuthorQq = sanitizeQq(
            firstNonBlank(
                official.optString("author_qq", ""),
                official.optString("contact_qq", ""),
                official.optString("author_contact_qq", ""),
                custom.optString("author_qq", ""),
                custom.optString("contact_qq", ""),
            )
        )
        officialCommunityJoinUrl = sanitizeJoinUrl(
            firstNonBlank(
                official.optString("join_url", ""),
                official.optString("qq_group_url", ""),
                official.optString("community_join_url", ""),
                custom.optString("join_url", ""),
                custom.optString("qq_group_url", ""),
            )
        )
        officialCommunityTitle = sanitizeDisplayText(firstNonBlank(
            official.optString("title", ""),
            official.optString("community_title", ""),
            custom.optString("community_title", ""),
        ), maxLength = 32)
        officialCommunitySubtitle = sanitizeDisplayText(firstNonBlank(
            official.optString("subtitle", ""),
            official.optString("community_subtitle", ""),
            custom.optString("community_subtitle", ""),
        ), maxLength = 64)
        forceShowOfficialContact =
            official.optBoolean("force_show", false) ||
                custom.optBoolean("force_show_official_contact", false)
        runtimeEnforcePluginHashAllowlist =
            custom.optBoolean(
                "enforce_plugin_hash_allowlist",
                custom.optBoolean("lx_plugin_allowlist_strict", true)
            )
        runtimePluginHashAllowlist = parseSha256Allowlist(
            custom.optJSONArray("plugin_hash_allowlist")
                ?: custom.optJSONArray("lx_plugin_hash_allowlist")
                ?: data.optJSONArray("plugin_hash_allowlist")
                ?: data.optJSONArray("lx_plugin_hash_allowlist")
        )
        runtimePluginHashBanned = parseSha256Allowlist(
            custom.optJSONArray("plugin_hash_banned")
                ?: custom.optJSONArray("lx_plugin_hash_banned")
                ?: data.optJSONArray("plugin_hash_banned")
                ?: data.optJSONArray("lx_plugin_hash_banned")
        )
        runtimeRequireHttpsOnly =
            custom.optBoolean(
                "require_https_only",
                custom.optBoolean(
                    "runtime_require_https_only",
                    custom.optBoolean("enforce_https_only", false)
                )
            )

        // 频率限制
        val rl = data.optJSONObject("rate_limits")
        if (rl != null) {
            searchPerMinute = rl.optInt("search_per_minute", 30)
            playPerMinute = rl.optInt("play_per_minute", 60)
        }

        // API地址
        apiHost = data.optString("api_host", "")
        serverUrl = data.optString("server_url", "")

        // 运行控制 (后端统一管理)
        val rt = data.optJSONObject("runtime_controls")
        if (rt != null) {
            runtimeClientMode = rt.optString("client_mode", "official")
            runtimePlaybackStreamMode = rt.optString("playback_stream_mode", "auto")
            runtimeBuiltinPluginEnabled = rt.optBoolean("builtin_plugin_enabled", true)
            runtimeBuiltinPluginAutoUpdate = rt.optBoolean("builtin_plugin_auto_update", true)
            runtimeRequireHttpsOnly = rt.optBoolean("require_https_only", runtimeRequireHttpsOnly)
            val ports = rt.optJSONArray("port_pool")
            runtimePortPool = if (ports != null && ports.length() > 0) {
                (0 until ports.length()).joinToString(",") { ports.optString(it, "") }
            } else ""
        }

        // 服务端口池 (后端下发, 覆盖内置端口池)
        val baseUrls = data.optJSONArray("server_base_urls")
        if (baseUrls != null && baseUrls.length() > 0) {
            val urls = mutableListOf<String>()
            for (i in 0 until baseUrls.length()) {
                val u = baseUrls.optString(i, "").trim()
                if (MusicApiService.isAllowedServerBaseUrl(u)) {
                    urls.add(u)
                }
            }
            if (urls.isNotEmpty()) {
                MusicApiService.applyServerBaseUrls(urls)
            }
        }

        // 自定义数据
        customData = custom

        // 同步api_host到本地 (仅允许HTTPS地址)
        if (apiHost.isNotEmpty()) {
            if (apiHost.startsWith("https://")) {
                LocalStorage.saveApiHost(apiHost)
            } else {
                Log.w(TAG, "忽略不安全的api_host下发: $apiHost")
            }
        }
    }

    /**
     * 检查平台是否启用
     */
    fun isPlatformEnabled(platform: String): Boolean {
        return platformEnabled[platform] ?: true
    }

    /**
     * 获取启用的平台列表
     */
    fun getEnabledPlatforms(): List<String> {
        return platformEnabled.filter { it.value }.keys.toList()
    }

    private fun firstNonBlank(vararg values: String): String {
        for (value in values) {
            val v = value.trim()
            if (v.isNotEmpty()) return v
        }
        return ""
    }

    private fun sanitizeQq(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        val digits = value.filter { it.isDigit() }
        return if (digits.length in 5..12) digits else ""
    }

    private fun sanitizeJoinUrl(raw: String): String {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        return runCatching {
            val uri = Uri.parse(value)
            val scheme = uri.scheme?.lowercase() ?: return@runCatching ""
            when (scheme) {
                "https" -> {
                    val host = uri.host?.lowercase() ?: return@runCatching ""
                    if (host in officialJoinHosts) value else ""
                }
                "mqqapi" -> {
                    val authority = uri.authority?.lowercase() ?: ""
                    val path = uri.path?.lowercase().orEmpty()
                    if (authority == "card" && path.startsWith("/show_pslcard")) value else ""
                }
                else -> ""
            }
        }.getOrDefault("")
    }

    private fun sanitizeDisplayText(raw: String, maxLength: Int): String {
        val value = raw.trim()
        if (value.isEmpty()) return ""
        return value.take(maxLength)
    }

    private fun parseSha256Allowlist(arr: org.json.JSONArray?): Set<String> {
        if (arr == null || arr.length() <= 0) return emptySet()
        val values = mutableSetOf<String>()
        for (i in 0 until arr.length()) {
            val value = arr.optString(i, "").trim().lowercase()
            if (SHA256_REGEX.matches(value)) {
                values.add(value)
            }
        }
        return values
    }

    fun isPluginHashAllowed(hash: String): Boolean {
        val normalized = hash.trim().lowercase()
        if (!SHA256_REGEX.matches(normalized)) return false
        if (normalized in runtimePluginHashBanned) return false
        if (!runtimeEnforcePluginHashAllowlist) return true
        return normalized in runtimePluginHashAllowlist
    }
}
