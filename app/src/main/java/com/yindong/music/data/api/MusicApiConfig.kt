package com.yindong.music.data.api

import com.yindong.music.data.LocalStorage
import java.net.URLEncoder

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              云音乐 · 本地音乐API配置中心                      ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║                                                              ║
 * ║  本文件管理所有音乐平台的API端点、音质、密钥配置。                ║
 * ║  支持5大平台: QQ音乐、网易云、酷我、咪咕、酷狗                   ║
 * ║                                                              ║
 * ║  【小白看这里】                                                ║
 * ║  你只需要在App设置里填写API地址和API Key就行，                   ║
 * ║  不需要修改这个文件！这个文件是给开发者看的。                     ║
 * ║                                                              ║
 * ║  【开发者看这里】                                              ║
 * ║  如果要新增平台或修改端点，改下面的 ENDPOINTS 和 PLATFORMS。     ║
 * ║                                                              ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * ── 工作原理 ──
 *
 * 1. 用户在App设置中填写:
 *    - 自己的API服务地址
 *    - 各平台的API Key (可选，由用户自行获取)
 *
 * 2. 播放歌曲时，App调用 buildUrl() 拼接完整请求地址:
 *    API地址 + 端点 + 歌曲ID + 音质 + API Key
 *
 * 3. 拼接示例:
 *    {用户API地址}/{端点}?id={歌曲ID}&level={音质}&type=mp3&apikey={Key}
 *    ├── API地址(用户填写) ──┘     │         │          │           │
 *    ├── 端点(按平台自动选择) ─────┘         │          │           │
 *    ├── 歌曲ID ────────────────────────────┘          │           │
 *    ├── 音质(128K/320K/FLAC) ────────────────────────┘           │
 *    └── API Key(用户填写) ───────────────────────────────────────┘
 */
object MusicApiConfig {

    // ═══════════════════════════════════════════
    //  API 地址（用户在App设置中配置）
    // ═══════════════════════════════════════════
    //
    //  用户可在 App → 我的 → 设置 → 音乐API地址 中修改
    //  由用户自行填写自己的API服务地址
    //
    val API_HOST: String
        get() = LocalStorage.loadApiHost()

    // ═══════════════════════════════════════════
    //  各平台 API 端点
    // ═══════════════════════════════════════════
    //
    //  端点 = API地址后面的路径部分
    //
    //  完整URL = API_HOST + 端点 + 参数
    //
    //  示例:
    //  ┌─────────┬──────────────┐
    //  │ 平台    │ 端点          │
    //  ├─────────┼──────────────┤
    //  │ 酷狗    │ /kgqq/kg.php │
    //  │ QQ音乐  │ /kgqq/tx.php │
    //  │ 网易云  │ /wy.php      │
    //  │ 酷我    │ /kw.php      │
    //  │ 咪咕    │ /mg.php      │
    //  └─────────┴──────────────┘
    //
    private val ENDPOINTS = mapOf(
        "kg" to "/kgqq/kg.php",   // 酷狗音乐端点
        "tx" to "/kgqq/tx.php",   // QQ音乐端点
        "wy" to "/wy.php",        // 网易云音乐端点
        "kw" to "/kw.php",        // 酷我音乐端点
        "mg" to "/mg.php",        // 咪咕音乐端点
    )

    // ═══════════════════════════════════════════
    //  音质配置
    // ═══════════════════════════════════════════
    //
    //  三种音质可选:
    //  ┌──────────┬────────┬──────────┬──────┬────────────┐
    //  │ 名称     │ 参数值  │ 酷狗参数  │ 码率  │ 说明       │
    //  ├──────────┼────────┼──────────┼──────┼────────────┤
    //  │ 标准     │standard│ 128k     │128K  │ 省流量     │
    //  │ 高品质   │exhigh  │ 320      │320K  │ 推荐，均衡  │
    //  │ 无损     │lossless│ flac     │FLAC  │ 最高音质   │
    //  └──────────┴────────┴──────────┴──────┴────────────┘
    //
    //  注意: 酷狗音乐的音质参数格式和其他平台不同！
    //  - 其他平台: level=standard / exhigh / lossless
    //  - 酷狗:     quality=128k / 320 / flac
    //
    enum class Quality(
        val key: String,       // 通用音质参数 (QQ/网易云/酷我/咪咕用)
        val kgKey: String,     // 酷狗专用音质参数
        val label: String,     // 界面显示名称
        val bitrate: Int,      // 码率 (kbps)
    ) {
        STANDARD("standard", "128k", "128K", 128),
        EXHIGH("exhigh", "320", "320K", 320),
        LOSSLESS("lossless", "flac", "FLAC无损", 900),
        ;

        companion object {
            fun fromStoredValue(value: String?): Quality {
                if (value.isNullOrBlank()) return STANDARD
                return entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                    ?: entries.firstOrNull { it.key.equals(value, ignoreCase = true) }
                    ?: STANDARD
            }
        }
    }

    // ═══════════════════════════════════════════
    //  平台配置
    // ═══════════════════════════════════════════
    //
    //  每个平台有3个属性:
    //  - name:    平台显示名称 (如 "QQ音乐")
    //  - key:     内部标识符 (如 "tx")，用于匹配端点和API Key
    //  - idField: 该平台歌曲ID的字段名 (如 QQ音乐用"songmid", 网易云用"id")
    //
    data class Platform(
        val name: String,      // 显示名称
        val key: String,       // 内部标识 (对应 ENDPOINTS 的 key)
        val idField: String,   // 歌曲ID字段名
    )

    //  平台列表:
    //  ┌──────────┬─────┬──────────┬────────────────────────────┐
    //  │ 平台名称 │ key │ ID字段    │ ID示例                      │
    //  ├──────────┼─────┼──────────┼────────────────────────────┤
    //  │ QQ音乐   │ tx  │ songmid  │ 001yS0N33rHHHP (字母数字)   │
    //  │ 网易云   │ wy  │ id       │ 1974443814 (纯数字)         │
    //  │ 酷我音乐 │ kw  │ id       │ 228908 (纯数字)             │
    //  │ 咪咕音乐 │ mg  │ id       │ 60054701923 (纯数字)        │
    //  │ 酷狗音乐 │ kg  │ hash     │ ABC123DEF456 (大写哈希值)   │
    //  └──────────┴─────┴──────────┴────────────────────────────┘
    //
    val PLATFORMS = mapOf(
        "QQ音乐" to Platform("QQ音乐", "tx", "songmid"),
        "网易云" to Platform("网易云", "wy", "id"),
        "酷我音乐" to Platform("酷我音乐", "kw", "id"),
        "咪咕音乐" to Platform("咪咕音乐", "mg", "id"),
        "酷狗音乐" to Platform("酷狗音乐", "kg", "hash"),
    )

    // 平台显示顺序（搜索结果排序用）
    val PLATFORM_ORDER = listOf("QQ音乐", "网易云", "酷我音乐", "咪咕音乐", "酷狗音乐")

    // ═══════════════════════════════════════════
    //  构建播放请求URL（核心方法）
    // ═══════════════════════════════════════════
    //
    //  【调用流程】
    //  1. 用户点击播放 → MusicApiService.fetchMusicUrl()
    //  2. fetchMusicUrl() 调用本方法拼接URL
    //  3. 请求该URL获取音频播放地址
    //
    //  【参数说明】
    //  @param platformName  平台名称 → "QQ音乐" / "网易云" / "酷我音乐" / "咪咕音乐" / "酷狗音乐"
    //  @param songId        歌曲ID   → 不同平台格式不同，见上方表格
    //  @param quality       音质     → "standard"(128K) / "exhigh"(320K) / "lossless"(FLAC)
    //
    //  【返回值示例】
    //  通用:    {API地址}/wy.php?id=123&level=exhigh&type=mp3
    //  酷狗:    {API地址}/kgqq/kg.php?id=ABC123&quality=320
    //  带Key:   {API地址}/wy.php?id=123&level=exhigh&type=mp3&apikey=xxx
    //
    fun buildUrl(
        platformName: String,
        songId: String,
        quality: String = Quality.EXHIGH.key,
    ): String {
        // 第1步: 查找平台配置
        val platform = PLATFORMS[platformName]
            ?: throw IllegalArgumentException("未知平台: $platformName")

        // 第2步: 查找对应端点
        val endpoint = ENDPOINTS[platform.key]
            ?: throw IllegalArgumentException("无端点: ${platform.key}")

        // 第3步: 获取API Key（仅本地模式下使用）
        //
        //  官方模式 → 不带apikey参数
        //  本地模式 → 带apikey参数（如果用户填写了的话）
        //
        val apiMode = LocalStorage.loadApiMode()
        val apiKey = if (apiMode == "local") {
            when (platform.key) {
                "tx" -> LocalStorage.loadQQMusicApiKey()    // QQ音乐 API Key
                "wy" -> LocalStorage.loadNeteaseApiKey()    // 网易云 API Key
                "kw" -> LocalStorage.loadKuwoApiKey()       // 酷我 API Key
                "mg" -> LocalStorage.loadMiguApiKey()       // 咪咕 API Key
                "kg" -> LocalStorage.loadKugouApiKey()      // 酷狗 API Key
                else -> ""
            }
        } else ""

        // 第4步: 拼接完整URL
        //
        //  注意: 酷狗用 quality 参数，其他平台用 level 参数！
        //
        //  酷狗:   ?id=xxx&quality=320
        //  其他:   ?id=xxx&level=exhigh&type=mp3
        //
        // 防止 URL 注入: 对 songId 和 apiKey 进行 URL 编码
        val safeId = URLEncoder.encode(songId, "UTF-8")
        val safeKey = if (apiKey.isNotEmpty()) URLEncoder.encode(apiKey, "UTF-8") else ""

        return if (platform.key == "kg") {
            // ── 酷狗音乐（特殊参数格式）──
            val kgQuality = Quality.entries.find { it.key == quality }?.kgKey ?: "320"
            if (safeKey.isNotEmpty()) {
                "$API_HOST$endpoint?id=$safeId&quality=$kgQuality&apikey=$safeKey"
            } else {
                "$API_HOST$endpoint?id=$safeId&quality=$kgQuality"
            }
        } else {
            // ── 其他平台（统一参数格式）──
            if (safeKey.isNotEmpty()) {
                "$API_HOST$endpoint?id=$safeId&level=$quality&type=mp3&apikey=$safeKey"
            } else {
                "$API_HOST$endpoint?id=$safeId&level=$quality&type=mp3"
            }
        }
    }
}
