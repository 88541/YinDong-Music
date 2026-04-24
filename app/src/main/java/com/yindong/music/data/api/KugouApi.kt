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
 * 酷狗音乐API（完全按照CeruMusic方式）
 */
object KugouApi {

    private const val TAG = "KugouApi"

    // OkHttp客户端
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 搜索歌曲
     * @param str 关键词
     * @param page 页码（从1开始）
     * @param limit 每页数量
     * @return 歌曲列表
     */
    suspend fun search(
        str: String,
        page: Int = 1,
        limit: Int = 30
    ): List<Song> = withContext(Dispatchers.IO) {
        val url = "https://songsearch.kugou.com/song_search_v2?keyword=${URLEncoder.encode(str, "UTF-8")}&page=$page&pagesize=$limit&showtype=1&filter=2&iscorrection=1&privilege_filter=0&area_code=1"

        Log.d(TAG, "酷狗搜索URL: $url")

        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""

            Log.d(TAG, "酷狗搜索响应: ${body.take(500)}")

            if (body.isEmpty()) {
                return@withContext emptyList()
            }

            val json = JSONObject(body)

            // 检查错误码
            val errorCode = json.optInt("error_code", -1)
            if (errorCode != 0) {
                Log.e(TAG, "酷狗搜索返回错误: error_code=$errorCode")
                return@withContext emptyList()
            }

            val data = json.optJSONObject("data") ?: return@withContext emptyList()
            val lists = data.optJSONArray("lists") ?: return@withContext emptyList()

            val result = handleResult(lists)

            Log.d(TAG, "酷狗搜索成功，返回 ${result.size} 首歌曲")
            return@withContext result

        } catch (e: Exception) {
            Log.e(TAG, "酷狗搜索异常: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * 处理搜索结果
     */
    private fun handleResult(lists: org.json.JSONArray): List<Song> {
        val result = mutableListOf<Song>()
        val ids = mutableSetOf<String>()

        for (i in 0 until lists.length()) {
            try {
                val item = lists.getJSONObject(i)

                val audioId = item.optString("Audioid", "")
                val fileHash = item.optString("FileHash", "")
                val key = audioId + fileHash

                // 去重
                if (ids.contains(key)) continue
                ids.add(key)

                val songName = item.optString("SongName", "")
                val singersJson = item.optString("Singers", "")
                val albumName = item.optString("AlbumName", "")
                val albumId = item.optString("AlbumID", "")
                val duration = item.optInt("Duration", 0)
                val imgUrl = item.optString("ImgUrl", "")

                // 解析歌手名
                val singerName = parseKugouSingers(singersJson)

                // 构建封面URL（优先使用ImgUrl，否则使用专辑封面）
                val coverUrl = when {
                    imgUrl.isNotBlank() -> imgUrl.replace("{size}", "240")
                    albumId.isNotBlank() -> "http://imge.kugou.com/stdmusic/240/$albumId.jpg"
                    else -> ""
                }

                result.add(Song(
                    id = key.hashCode().toLong(),
                    title = decodeName(songName),
                    artist = singerName,
                    album = decodeName(albumName),
                    duration = duration * 1000L,
                    coverUrl = coverUrl,
                    artistPicUrl = "",
                    platform = "酷狗音乐",
                    platformId = fileHash  // 使用hash作为platformId，播放时需要
                ))

                // 处理Grp中的歌曲（相关歌曲）
                val grp = item.optJSONArray("Grp")
                if (grp != null) {
                    for (j in 0 until grp.length()) {
                        try {
                            val childItem = grp.getJSONObject(j)
                            val childAudioId = childItem.optString("Audioid", "")
                            val childFileHash = childItem.optString("FileHash", "")
                            val childKey = childAudioId + childFileHash

                            if (ids.contains(childKey)) continue
                            ids.add(childKey)

                            val childSongName = childItem.optString("SongName", "")
                            val childSingersJson = childItem.optString("Singers", "")
                            val childAlbumName = childItem.optString("AlbumName", "")
                            val childAlbumId = childItem.optString("AlbumID", "")
                            val childDuration = childItem.optInt("Duration", 0)
                            val childImgUrl = childItem.optString("ImgUrl", "")

                            // 解析歌手名
                            val childSingerName = parseKugouSingers(childSingersJson)

                            // 构建封面URL
                            val childCoverUrl = when {
                                childImgUrl.isNotBlank() -> childImgUrl.replace("{size}", "240")
                                childAlbumId.isNotBlank() -> "http://imge.kugou.com/stdmusic/240/$childAlbumId.jpg"
                                else -> ""
                            }

                            result.add(Song(
                                id = childKey.hashCode().toLong(),
                                title = decodeName(childSongName),
                                artist = childSingerName,
                                album = decodeName(childAlbumName),
                                duration = childDuration * 1000L,
                                coverUrl = childCoverUrl,
                                artistPicUrl = "",
                                platform = "酷狗音乐",
                                platformId = childFileHash  // 使用hash作为platformId，播放时需要
                            ))
                        } catch (e: Exception) {
                            continue
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "处理酷狗歌曲异常: ${e.message}")
                continue
            }
        }

        return result
    }

    /**
     * 解析酷狗歌手名（Singers是JSON数组字符串）
     */
    private fun parseKugouSingers(singersJson: String): String {
        if (singersJson.isEmpty()) return "未知"

        // 如果不是JSON格式（旧格式），直接返回
        if (!singersJson.startsWith("[")) {
            return singersJson.replace("&", "、")
        }

        return try {
            // Singers格式: [{"name":"林俊杰","ip_id":"0"},...]
            val jsonArray = org.json.JSONArray(singersJson)
            val singerNames = mutableListOf<String>()

            for (i in 0 until jsonArray.length()) {
                val singer = jsonArray.getJSONObject(i)
                val name = singer.optString("name", "")
                if (name.isNotEmpty()) {
                    singerNames.add(name)
                }
            }

            if (singerNames.isEmpty()) "未知"
            else singerNames.joinToString("、")
        } catch (e: Exception) {
            // 如果解析失败，直接返回原字符串
            singersJson.replace("&", "、")
        }
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
