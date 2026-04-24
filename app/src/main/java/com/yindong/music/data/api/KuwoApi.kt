package com.yindong.music.data.api

import android.util.Log
import com.yindong.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 酷我音乐API（完全按照CeruMusic方式）
 */
object KuwoApi {

    private const val TAG = "KuwoApi"
    private const val LIMIT = 30

    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // 正则表达式匹配音质信息
    private val mInfoRegex = Regex("""level:(\w+),bitrate:(\d+),format:(\w+),size:([\w.]+)""")

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
        if (retryNum > 2) {
            Log.e(TAG, "酷我搜索重试次数超过限制")
            return@withContext emptyList()
        }

        val url = "http://search.kuwo.cn/r.s?client=kt&all=${URLEncoder.encode(str, "UTF-8")}&pn=${page - 1}&rn=$limit&uid=794762570&ver=kwplayer_ar_9.2.2.1&vipver=1&show_copyright_off=1&newver=1&ft=music&cluster=0&strategy=2012&encoding=utf8&rformat=json&vermerge=1&mobi=1&issubtitle=1"

        Log.d(TAG, "酷我搜索URL: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "酷我搜索响应: ${body.take(500)}")

            if (body.isEmpty()) {
                Log.w(TAG, "酷我搜索返回空body，准备重试...")
                return@withContext search(str, page, limit, retryNum + 1)
            }

            val result = JSONObject(body)

            // 检查是否需要重试
            val total = result.optString("TOTAL", "0")
            val show = result.optString("SHOW", "0")
            if (total != "0" && show == "0") {
                Log.w(TAG, "酷我搜索需要重试: TOTAL=$total, SHOW=$show")
                return@withContext search(str, page, limit, retryNum + 1)
            }

            val abslist = result.optJSONArray("abslist")
            if (abslist == null) {
                Log.w(TAG, "酷我搜索abslist为空，准备重试...")
                return@withContext search(str, page, limit, retryNum + 1)
            }

            val list = handleResult(abslist)
            if (list == null) {
                Log.w(TAG, "酷我搜索结果处理失败，准备重试...")
                return@withContext search(str, page, limit, retryNum + 1)
            }

            Log.d(TAG, "酷我搜索成功，返回 ${list.size} 首歌曲")
            return@withContext list

        } catch (e: Exception) {
            Log.e(TAG, "酷我搜索异常: ${e.message}")
            return@withContext search(str, page, limit, retryNum + 1)
        }
    }

    /**
     * 处理搜索结果
     */
    private fun handleResult(abslist: org.json.JSONArray): List<Song>? {
        val result = mutableListOf<Song>()

        for (i in 0 until abslist.length()) {
            try {
                val info = abslist.getJSONObject(i)

                // 获取歌曲ID
                val musicrid = info.optString("MUSICRID", "")
                val songId = musicrid.replace("MUSIC_", "")
                if (songId.isEmpty()) continue

                // 检查N_MINFO是否存在
                val nMinfo = info.optString("N_MINFO", "")
                if (nMinfo.isEmpty()) {
                    Log.w(TAG, "N_MINFO is empty")
                    return null
                }

                // 解析音质信息
                val types = parseTypes(nMinfo)

                // 获取基本信息
                val songName = info.optString("SONGNAME", "")
                val artist = info.optString("ARTIST", "")
                val album = info.optString("ALBUM", "")
                val albumId = info.optString("ALBUMID", "")
                val webAlbumPic = info.optString("web_albumpic_short", "")
                val duration = info.optInt("DURATION", 0)

                // 构建封面URL（使用酷我专辑封面API）
                // 优先使用web_albumpic_short，其次是专辑封面，最后是歌手封面
                val coverUrl = when {
                    webAlbumPic.isNotBlank() -> "https://img1.kuwo.cn/star/albumcover/$webAlbumPic"
                    albumId.isNotBlank() && albumId != "0" -> "https://img1.kuwo.cn/star/albumcover/500/$albumId.jpg"
                    else -> "http://artistpicserver.kuwo.cn/pic.web?corp=kuwo&type=rid_pic&pictype=500&size=500&rid=$songId"
                }

                result.add(Song(
                    id = songId.hashCode().toLong(),
                    title = decodeName(songName),
                    artist = formatSinger(decodeName(artist)),
                    album = decodeName(album),
                    duration = duration * 1000L,
                    coverUrl = coverUrl,
                    artistPicUrl = "",
                    platform = "酷我音乐",
                    platformId = songId
                ))

            } catch (e: Exception) {
                Log.e(TAG, "处理酷我歌曲异常: ${e.message}")
                continue
            }
        }

        return result
    }

    /**
     * 解析音质信息
     */
    private fun parseTypes(nMinfo: String): Map<String, String> {
        val types = mutableMapOf<String, String>()
        if (nMinfo.isEmpty()) return types

        val infoArr = nMinfo.split(";")
        for (info in infoArr) {
            val match = mInfoRegex.find(info)
            if (match != null) {
                val bitrate = match.groupValues[2]
                val size = match.groupValues[4]
                when (bitrate) {
                    "20900" -> types["master"] = size
                    "4000" -> types["hires"] = size
                    "2000" -> types["flac"] = size
                    "320" -> types["320k"] = size
                    "128" -> types["128k"] = size
                }
            }
        }
        return types
    }

    /**
     * 格式化歌手名
     */
    private fun formatSinger(rawData: String): String {
        return rawData.replace("&", "、")
    }

    /**
     * 解码名称（处理HTML实体）
     */
    private fun decodeName(name: String): String {
        return name
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
    }
}
