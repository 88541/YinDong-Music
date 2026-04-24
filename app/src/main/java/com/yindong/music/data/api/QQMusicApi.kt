package com.yindong.music.data.api

import android.util.Log
import com.yindong.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * QQ音乐API（完全按照CeruMusic方式）
 */
object QQMusicApi {

    private const val TAG = "QQMusicApi"
    private const val SUCCESS_CODE = 0
    private const val LIMIT = 50

    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // 签名相关常量
    private val PART_1_INDEXES = intArrayOf(23, 14, 6, 36, 16, 40, 7, 19)
    private val PART_2_INDEXES = intArrayOf(16, 1, 32, 12, 19, 27, 8, 5)
    private val SCRAMBLE_VALUES = intArrayOf(
        89, 39, 179, 150, 218, 82, 58, 252, 177, 52, 186, 123, 120, 64, 242, 133, 143, 161, 121, 179
    )

    /**
     * 搜索歌曲
     * @param str 关键词
     * @param page 页码（从1开始）
     * @param limit 每页数量
     * @param retryNum 重试次数
     * @return 歌曲列表
     */
    suspend fun search(
        str: String,
        page: Int = 1,
        limit: Int = LIMIT,
        retryNum: Int = 0
    ): List<Song> = withContext(Dispatchers.IO) {
        if (retryNum > 5) {
            Log.e(TAG, "QQ音乐搜索重试次数超过限制")
            return@withContext emptyList()
        }

        try {
            val data = musicSearch(str, page, limit)

            val json = JSONObject(data)
            val code = json.optInt("code", -1)
            val reqCode = json.optJSONObject("req")?.optInt("code", -1) ?: -1

            if (code != SUCCESS_CODE || reqCode != SUCCESS_CODE) {
                Log.w(TAG, "QQ音乐搜索返回错误，准备重试: code=$code, req.code=$reqCode")
                return@withContext search(str, page, limit, retryNum + 1)
            }

            val reqData = json.optJSONObject("req")?.optJSONObject("data") ?: return@withContext emptyList()
            val itemSong = reqData.optJSONArray("item_song") ?: return@withContext emptyList()

            val result = handleResult(itemSong)

            Log.d(TAG, "QQ音乐搜索成功，返回 ${result.size} 首歌曲")
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "QQ音乐搜索异常: ${e.message}")
            return@withContext search(str, page, limit, retryNum + 1)
        }
    }

    /**
     * 执行搜索请求
     */
    private fun musicSearch(str: String, page: Int, limit: Int): String {
        val requestData = JSONObject().apply {
            put("comm", JSONObject().apply {
                put("ct", "11")
                put("cv", "14090508")
                put("v", "14090508")
                put("tmeAppID", "qqmusic")
                put("phonetype", "EBG-AN10")
                put("deviceScore", "553.47")
                put("devicelevel", "50")
                put("newdevicelevel", "20")
                put("rom", "HuaWei/EMOTION/EmotionUI_14.2.0")
                put("os_ver", "12")
                put("OpenUDID", "0")
                put("OpenUDID2", "0")
                put("QIMEI36", "0")
                put("udid", "0")
                put("chid", "0")
                put("aid", "0")
                put("oaid", "0")
                put("taid", "0")
                put("tid", "0")
                put("wid", "0")
                put("uid", "0")
                put("sid", "0")
                put("modeSwitch", "6")
                put("teenMode", "0")
                put("ui_mode", "2")
                put("nettype", "1020")
                put("v4ip", "")
            })
            put("req", JSONObject().apply {
                put("module", "music.search.SearchCgiService")
                put("method", "DoSearchForQQMusicMobile")
                put("param", JSONObject().apply {
                    put("search_type", 0)
                    put("searchid", getSearchId())
                    put("query", str)
                    put("page_num", page)
                    put("num_per_page", limit)
                    put("highlight", 0)
                    put("nqc_flag", 0)
                    put("multi_zhida", 0)
                    put("cat", 2)
                    put("grp", 1)
                    put("sin", 0)
                    put("sem", 0)
                })
            })
        }

        val jsonString = requestData.toString()
        val sign = zzcSign(jsonString)
        val url = "https://u.y.qq.com/cgi-bin/musics.fcg?sign=$sign"

        Log.d(TAG, "QQ音乐搜索URL: $url")
        Log.d(TAG, "QQ音乐搜索Sign: $sign")

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonString.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "QQMusic 14090508(android 12)")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: ""
    }

    /**
     * 处理搜索结果
     */
    private fun handleResult(itemSong: org.json.JSONArray): List<Song> {
        val result = mutableListOf<Song>()

        for (i in 0 until itemSong.length()) {
            try {
                val item = itemSong.getJSONObject(i)

                val file = item.optJSONObject("file")
                val mediaMid = file?.optString("media_mid", "")
                if (mediaMid.isNullOrEmpty()) continue

                val songId = item.optLong("id", 0)
                val songMid = item.optString("mid", "")
                val songName = item.optString("name", "")
                val titleExtra = item.optString("title_extra", "")
                val interval = item.optInt("interval", 0)

                // 解析歌手
                val singers = item.optJSONArray("singer")
                val singerNames = mutableListOf<String>()
                if (singers != null) {
                    for (j in 0 until singers.length()) {
                        val singer = singers.getJSONObject(j)
                        singerNames.add(singer.optString("name", ""))
                    }
                }

                // 解析专辑
                val album = item.optJSONObject("album")
                val albumName = album?.optString("name", "") ?: ""
                val albumMid = album?.optString("mid", "") ?: ""

                // 构建封面URL
                val coverUrl = if (albumMid.isEmpty() || albumMid == "空") {
                    if (singers != null && singers.length() > 0) {
                        val singerMid = singers.getJSONObject(0).optString("mid", "")
                        "https://y.gtimg.cn/music/photo_new/T001R500x500M000$singerMid.jpg"
                    } else ""
                } else {
                    "https://y.gtimg.cn/music/photo_new/T002R500x500M000$albumMid.jpg"
                }

                result.add(Song(
                    id = songId,
                    title = songName + titleExtra,
                    artist = singerNames.joinToString("、"),
                    album = albumName,
                    duration = interval * 1000L,
                    coverUrl = coverUrl,
                    artistPicUrl = "",
                    platform = "QQ音乐",
                    platformId = songMid
                ))

            } catch (e: Exception) {
                Log.e(TAG, "处理QQ音乐歌曲异常: ${e.message}")
                continue
            }
        }

        return result
    }

    /**
     * 生成搜索ID
     */
    private fun getSearchId(): String {
        val e = randomInt(1, 20)
        val t = e * 18014398509481984L
        val n = randomInt(0, 4194304).toLong() * 4294967296L
        val a = System.currentTimeMillis()
        val r = (a * 1000) % (24 * 60 * 60 * 1000)
        return (t + n + r).toString()
    }

    /**
     * 生成随机整数
     */
    private fun randomInt(min: Int, max: Int): Int {
        return (Math.random() * (max - min + 1)).toInt() + min
    }

    /**
     * 生成ZZC签名
     */
    private fun zzcSign(text: String): String {
        val hash = hashSHA1(text).uppercase()

        // Part 1
        val part1 = StringBuilder()
        for (idx in PART_1_INDEXES) {
            if (idx < hash.length) {
                part1.append(hash[idx])
            }
        }

        // Part 2
        val part2 = StringBuilder()
        for (idx in PART_2_INDEXES) {
            if (idx < hash.length) {
                part2.append(hash[idx])
            }
        }

        // Part 3 (scramble)
        val part3 = ByteArray(SCRAMBLE_VALUES.size)
        for (i in SCRAMBLE_VALUES.indices) {
            val hexByte = hash.substring(i * 2, i * 2 + 2).toInt(16)
            part3[i] = (SCRAMBLE_VALUES[i] xor hexByte).toByte()
        }

        val b64Part = base64Encode(part3).replace(Regex("[\\/+=]"), "")

        return "zzc${part1}${b64Part}${part2}".lowercase()
    }

    /**
     * SHA1哈希
     */
    private fun hashSHA1(text: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Base64编码
     */
    private fun base64Encode(data: ByteArray): String {
        return java.util.Base64.getEncoder().encodeToString(data)
    }
}
