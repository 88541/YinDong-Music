package com.yindong.music.data.lx

import android.content.Context
import android.util.Log
import com.yindong.music.R
import com.yindong.music.data.LocalStorage
import com.yindong.music.data.RemoteConfig
import com.yindong.music.data.api.MusicApiService
import com.yindong.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 内置远程 JS 音源插件管理器
 *
 * 对用户完全透明——自动从服务器拉取 JS 脚本并通过 QuickJS 运行。
 * 管理员只需替换服务器上的 JS 文件即可热更新。
 *
 * 工作流程:
 * 1. App 启动 → syncAndLoad()
 * 2. 对比本地缓存 hash → 有变化才下载
 * 3. 加载到 LxPluginManager → 可用于 musicUrl / search / lyric
 */
object BuiltinPluginManager {

    private const val TAG = "BuiltinPlugin"

    /** 内置插件在 LxPluginManager 中的 ID (加载后获得) */
    var pluginId: String = ""
        private set

    /** 内置插件提供的音源 key 列表 */
    var sources: List<String> = emptyList()
        private set

    /** 是否已成功加载 */
    var isLoaded: Boolean = false
        private set

    // 复用 MusicApiService 的连接池
    private val client: OkHttpClient by lazy {
        MusicApiService.sharedClient().newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 同步远程插件并加载 (IO 线程)
     *
     * @param context      Application Context（用于读取内置资产）
     * @param pluginManager 已有的 LxPluginManager 实例
     * @param logCallback   可选日志回调 (用于调试)
     */
    suspend fun syncAndLoad(
        context: Context,
        pluginManager: LxPluginManager,
        logCallback: ((String) -> Unit)? = null,
    ) = withContext(Dispatchers.IO) {
        try {
            if (!RemoteConfig.runtimeBuiltinPluginEnabled) {
                isLoaded = false
                pluginId = ""
                sources = emptyList()
                logCallback?.invoke("内置插件已被后端禁用")
                Log.d(TAG, "后端已禁用内置插件")
                return@withContext
            }
            val localHash = LocalStorage.loadBuiltinPluginHash()
            val localScript = LocalStorage.loadBuiltinPluginScript()

            // 1. 尝试从服务器拉取 (带 ETag 条件请求)
            val remoteResult = if (RemoteConfig.runtimeBuiltinPluginAutoUpdate) {
                fetchRemotePlugin(localHash)
            } else {
                Log.d(TAG, "后端关闭自动更新，使用本地内置插件缓存")
                null
            }

            val script: String
            val hash: String

            when {
                // 服务器返回新版本
                remoteResult != null -> {
                    script = remoteResult.first
                    hash = remoteResult.second
                    // 缓存到本地
                    LocalStorage.saveBuiltinPluginScript(script)
                    LocalStorage.saveBuiltinPluginHash(hash)
                    Log.d(TAG, "已更新内置插件: hash=${hash.take(12)}...")
                    logCallback?.invoke("内置插件已更新: ${hash.take(12)}...")
                }
                // 304 未变化，用本地缓存
                localScript.isNotBlank() -> {
                    script = localScript
                    hash = localHash
                    Log.d(TAG, "内置插件未变化，使用本地缓存")
                }
                // 无远程也无本地 → 使用内置资产文件作为兜底
                else -> {
                    val builtinScript = loadBuiltinAsset(context)
                    if (builtinScript.isNotBlank()) {
                        script = builtinScript
                        hash = "builtin_asset_qq"
                        Log.d(TAG, "使用内置资产音源 (QQ)")
                        logCallback?.invoke("已加载内置音源 (QQ音乐)")
                    } else {
                        Log.d(TAG, "无可用内置插件")
                        return@withContext
                    }
                }
            }

            // 2. 加载到 QuickJS
            loadScript(pluginManager, script, hash, logCallback)

        } catch (e: Exception) {
            Log.e(TAG, "syncAndLoad 失败: ${e.message}")
            logCallback?.invoke("内置插件加载失败: ${e.message}")
            // 尝试用本地缓存兜底
            if (!tryLoadFromCache(pluginManager, logCallback)) {
                // 最后尝试内置资产
                tryLoadFromAsset(context, pluginManager, logCallback)
            }
        }
    }

    /**
     * 仅从本地缓存加载 (无网络时兜底)
     * @return 是否加载成功
     */
    private fun tryLoadFromCache(
        pluginManager: LxPluginManager,
        logCallback: ((String) -> Unit)? = null,
    ): Boolean {
        return try {
            val script = LocalStorage.loadBuiltinPluginScript()
            val hash = LocalStorage.loadBuiltinPluginHash()
            if (script.isNotBlank() && hash.isNotBlank()) {
                loadScript(pluginManager, script, hash, logCallback)
                Log.d(TAG, "已从本地缓存加载内置插件")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "本地缓存加载也失败: ${e.message}")
            false
        }
    }

    /**
     * 从内置资产文件加载 (最终兜底)
     */
    private fun tryLoadFromAsset(
        context: Context,
        pluginManager: LxPluginManager,
        logCallback: ((String) -> Unit)? = null,
    ) {
        try {
            val script = loadBuiltinAsset(context)
            if (script.isNotBlank()) {
                loadScript(pluginManager, script, "builtin_asset_qq", logCallback)
                Log.d(TAG, "已从内置资产加载音源")
                logCallback?.invoke("已加载内置音源 (QQ音乐)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "内置资产加载失败: ${e.message}")
        }
    }

    /**
     * 读取 assets 中的内置音源 JS 文件
     */
    private fun loadBuiltinAsset(context: Context): String {
        return try {
            context.assets.open("builtin_qq.js").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.w(TAG, "读取内置资产 builtin_qq.js 失败: ${e.message}")
            ""
        }
    }

    /**
     * 从服务器拉取插件脚本
     * @return (script, hash) 或 null (304/失败)
     */
    private fun fetchRemotePlugin(localHash: String): Pair<String, String>? {
        for (baseUrl in MusicApiService.serverBaseUrls()) {
            try {
                val url = "${baseUrl.trimEnd('/')}/api/builtin-plugin"
                val reqBuilder = MusicApiService.attachServerAuthHeaders(
                    Request.Builder().url(url),
                    includeChallengeToken = true
                )
                if (localHash.isNotBlank()) {
                    reqBuilder.header("If-None-Match", localHash)
                }
                val response = client.newCall(reqBuilder.build()).execute()
                try {
                    if (response.code == 304) {
                        MusicApiService.markServerHealthy(baseUrl)
                        Log.d(TAG, "服务器返回 304，插件未变化")
                        return null
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "拉取内置插件失败($baseUrl): HTTP ${response.code}")
                        continue
                    }
                    val body = response.body?.string() ?: continue
                    val json = JSONObject(body)
                    if (json.optInt("code") != 200) continue
                    val data = json.optJSONObject("data") ?: continue
                    val script = data.optString("script", "")
                    val hash = data.optString("hash", "")
                    if (script.isBlank() || hash.isBlank()) continue
                    // 独立校验: 本地重新计算 hash，防止服务器被入侵后篡改插件内容而保留旧 hash
                    val localComputedHash = java.security.MessageDigest.getInstance("SHA-256")
                        .digest(script.toByteArray(Charsets.UTF_8))
                        .joinToString("") { "%02x".format(it) }
                    if (localComputedHash != hash) {
                        Log.e(TAG, "内置插件 hash 不匹配! 服务器=$hash, 本地计算=$localComputedHash")
                        continue
                    }
                    MusicApiService.markServerHealthy(baseUrl)
                    return script to hash
                } finally {
                    response.close()
                }
            } catch (e: Exception) {
                Log.w(TAG, "网络请求内置插件失败($baseUrl): ${e.message}")
            }
        }
        return null
    }

    /**
     * 将 JS 脚本加载到 LxPluginManager
     */
    private fun loadScript(
        pluginManager: LxPluginManager,
        script: String,
        hash: String,
        logCallback: ((String) -> Unit)? = null,
    ) {
        val options = LxRuntimeOptions(
            callTimeoutMs = 10_000L,
            allowHttp = true,  // 内置插件允许 HTTP/HTTPS
        )
        val result = pluginManager.loadPluginFromScript(
            script = script,
            virtualUri = "builtin://internal",
            options = options,
        )
        result.onSuccess { entry ->
            pluginId = entry.id
            sources = entry.sources
            isLoaded = true
            Log.d(TAG, "内置插件加载成功: id=${entry.id.take(12)}, sources=$sources, initialized=${entry.initialized}")
            logCallback?.invoke("内置插件已加载: ${entry.info.name.ifBlank { "builtin" }}, 音源=${sources}")
        }
        result.onFailure { e ->
            isLoaded = false
            Log.e(TAG, "内置插件加载失败: ${e.message}")
            logCallback?.invoke("内置插件加载失败: ${e.message}")
        }
    }

    /**
     * 通过内置插件获取播放 URL
     * @return LxMusicUrlResult 或 null (未加载/失败)
     */
    suspend fun musicUrl(
        pluginManager: LxPluginManager,
        song: Song,
        quality: String = "320k",
    ): LxMusicUrlResult? {
        if (!isLoaded || pluginId.isBlank() || sources.isEmpty()) return null
        val source = findBestSource(song.platform) ?: return null
        return try {
            pluginManager.musicUrl(pluginId, source, song, 8000L, quality)
        } catch (e: Exception) {
            Log.w(TAG, "内置插件 musicUrl 失败: ${e.message}")
            null
        }
    }

    /**
     * 通过内置插件搜索歌曲
     */
    suspend fun search(
        pluginManager: LxPluginManager,
        source: String,
        keyword: String,
    ): List<Song> {
        if (!isLoaded || pluginId.isBlank()) return emptyList()
        return try {
            pluginManager.search(pluginId, source, keyword, 8000L)
        } catch (e: Exception) {
            Log.w(TAG, "内置插件搜索失败: ${e.message}")
            emptyList()
        }
    }

    /**
     * 根据歌曲平台名匹配内置插件的音源 key
     */
    private fun findBestSource(platform: String): String? {
        if (sources.isEmpty()) return null
        val lower = platform.lowercase()
        // 优先精确匹配
        for (src in sources) {
            val sl = src.lowercase()
            when {
                (lower.contains("qq") || lower == "qq音乐") && (sl == "tx" || sl.contains("qq")) -> return src
                (lower.contains("网易") || lower == "网易云") && (sl == "wy" || sl.contains("netease") || sl.contains("163")) -> return src
                (lower.contains("酷我")) && (sl == "kw" || sl.contains("kuwo")) -> return src
                (lower.contains("酷狗")) && (sl == "kg" || sl.contains("kugou")) -> return src
                (lower.contains("咪咕")) && (sl == "mg" || sl.contains("migu")) -> return src
            }
        }
        // 没匹配到，返回第一个
        return sources.firstOrNull()
    }
}
