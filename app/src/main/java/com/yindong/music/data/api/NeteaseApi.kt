package com.yindong.music.data.api

import android.util.Log
import com.yindong.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * 网易云音乐API（完全按照DsoMusic的NewSearchSong实现）
 * API地址: http://156.225.18.78:3000
 */
object NeteaseApi {

    private const val TAG = "NeteaseApi"
    private const val API_BASE_URL = "http://156.225.18.78:3000"

    // 默认封面（网易云默认图）
    private const val DEFAULT_COVER = "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395135138885805.jpg"

    // OkHttp客户端 - 使用DsoMusic的配置
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 搜索歌曲（带自动重试机制）
     * @param keywords 关键词
     * @param offset 偏移量（分页）
     * @param limit 返回数量
     * @param maxRetries 最大重试次数（默认3次）
     * @return 歌曲列表
     */
    suspend fun search(
        keywords: String,
        offset: Int = 0,
        limit: Int = 30,
        maxRetries: Int = 3
    ): List<Song> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                Log.d(TAG, "搜索尝试 $attempt/$maxRetries: $keywords")
                val result = searchInternal(keywords, offset, limit)
                
                // 如果搜索成功且有结果，直接返回
                if (result.isNotEmpty()) {
                    Log.d(TAG, "搜索成功，返回 ${result.size} 首歌曲")
                    return@withContext result
                }
                
                // 如果结果为空但不是最后一次尝试，等待后重试
                if (attempt < maxRetries) {
                    Log.w(TAG, "搜索返回空结果，准备重试...")
                    kotlinx.coroutines.delay(1000L * attempt) // 递增延迟：1s, 2s, 3s
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "搜索尝试 $attempt 失败: ${e.message}")
                
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(1000L * attempt)
                }
            }
        }
        
        Log.e(TAG, "搜索失败，已重试 $maxRetries 次")
        emptyList()
    }

    /**
     * 内部搜索方法（实际执行搜索）
     */
    private suspend fun searchInternal(
        keywords: String,
        offset: Int = 0,
        limit: Int = 30
    ): List<Song> = suspendCancellableCoroutine { continuation ->
        val encodedKeywords = URLEncoder.encode(keywords, "UTF-8")
        val url = "$API_BASE_URL/search?keywords=$encodedKeywords&limit=$limit&offset=$offset"

        Log.d(TAG, "搜索URL: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Referer", "https://music.163.com/")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "搜索请求失败: ${e.message}")
                continuation.resume(emptyList())
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    Log.d(TAG, "搜索响应: ${body.take(800)}")

                    if (body.isEmpty()) {
                        Log.e(TAG, "搜索返回空body")
                        continuation.resume(emptyList())
                        return
                    }

                    val json = JSONObject(body)
                    val code = json.optInt("code", 0)
                    val result = json.optJSONObject("result")
                    val songs = result?.optJSONArray("songs")

                    Log.d(TAG, "搜索解析: code=$code, result=${result != null}, songs=${songs?.length()}")

                    if (code != 200 || songs == null) {
                        Log.e(TAG, "搜索失败或结果为空: code=$code, songs=${songs != null}")
                        continuation.resume(emptyList())
                        return
                    }

                    val songList = mutableListOf<Song>()

                    for (i in 0 until songs.length()) {
                        val song = songs.getJSONObject(i)
                        val songId = song.optLong("id", 0)
                        val name = song.optString("name", "")

                        // 解析歌手
                        val artists = song.optJSONArray("artists")
                        val artistName = if (artists != null && artists.length() > 0) {
                            (0 until artists.length()).joinToString("/") {
                                artists.getJSONObject(it).optString("name", "")
                            }
                        } else "未知"

                        // 解析专辑
                        val album = song.optJSONObject("album")
                        val albumName = album?.optString("name", "") ?: ""

                        // 时长（毫秒）
                        val duration = song.optLong("duration", 0)

                        // 搜索接口返回的album中没有picUrl，直接使用默认封面
                        // 然后通过song/detail接口异步获取真实封面
                        if (i < 3) {
                            Log.d(TAG, "歌曲[$i]: $name, id=$songId, 使用默认封面")
                        }

                        songList.add(Song(
                            id = songId,
                            title = name,
                            artist = artistName,
                            album = albumName,
                            duration = duration,
                            coverUrl = DEFAULT_COVER,
                            artistPicUrl = "",
                            platform = "网易云",
                            platformId = songId.toString()
                        ))
                    }

                    Log.d(TAG, "转换结果: ${songList.size} 首歌曲")

                    // 异步获取真实封面
                    songList.forEachIndexed { index, song ->
                        if (song.platformId.isNotEmpty()) {
                            fetchRealCover(song.platformId) { realCoverUrl ->
                                if (!realCoverUrl.isNullOrEmpty()) {
                                    song.coverUrl = realCoverUrl
                                    Log.d(TAG, "更新歌曲 ${song.title} 封面: $realCoverUrl")
                                }
                            }
                        }
                    }

                    continuation.resume(songList)
                } catch (e: Exception) {
                    Log.e(TAG, "解析搜索响应异常", e)
                    e.printStackTrace()
                    continuation.resume(emptyList())
                }
            }
        })
    }

    /**
     * 获取歌曲真实封面URL
     * @param songId 歌曲ID
     * @param callback 成功回调
     */
    private fun fetchRealCover(songId: String, callback: (String?) -> Unit) {
        getSongDetail(songId) { detail ->
            if (detail != null) {
                val coverUrl = detail.al?.picUrl
                if (!coverUrl.isNullOrEmpty()) {
                    callback.invoke(coverUrl)
                } else {
                    callback.invoke(null)
                }
            } else {
                callback.invoke(null)
            }
        }
    }

    /**
     * 获取歌曲详情（包含封面）
     * @param id 歌曲ID
     * @param callback 成功回调
     */
    fun getSongDetail(id: String, callback: (SongDetail?) -> Unit) {
        val url = "$API_BASE_URL/song/detail?ids=$id"

        Log.d(TAG, "获取歌曲详情URL: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Referer", "https://music.163.com/")
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "获取歌曲详情失败: ${e.message}")
                callback.invoke(null)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val body = response.body?.string() ?: ""
                    Log.d(TAG, "歌曲详情响应: ${body.take(300)}")

                    if (body.isEmpty()) {
                        callback.invoke(null)
                        return
                    }

                    val json = JSONObject(body)
                    val code = json.optInt("code", 0)
                    val songs = json.optJSONArray("songs")

                    if (code == 200 && songs != null && songs.length() > 0) {
                        val song = songs.getJSONObject(0)
                        val al = song.optJSONObject("al")
                        val ar = song.optJSONArray("ar")

                        val songDetail = SongDetail(
                            id = song.optLong("id", 0),
                            name = song.optString("name", ""),
                            al = if (al != null) AlbumDetail(
                                id = al.optLong("id", 0),
                                name = al.optString("name", ""),
                                picUrl = al.optString("picUrl", null),
                                pic_str = al.optString("pic_str", null)
                            ) else null,
                            ar = (0 until (ar?.length() ?: 0)).map {
                                val artist = ar!!.getJSONObject(it)
                                ArtistDetail(
                                    id = artist.optLong("id", 0),
                                    name = artist.optString("name", "")
                                )
                            },
                            dt = song.optLong("dt", 0)
                        )

                        Log.d(TAG, "获取到歌曲详情: ${songDetail.name}, 封面: ${songDetail.al?.picUrl}")
                        callback.invoke(songDetail)
                    } else {
                        Log.e(TAG, "获取歌曲详情失败: code=$code")
                        callback.invoke(null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析歌曲详情异常", e)
                    e.printStackTrace()
                    callback.invoke(null)
                }
            }
        })
    }

    /**
     * 获取歌曲播放URL（使用DsoMusic的回调式方式）
     * @param id 歌曲ID
     * @param level 音质等级 (standard, exhigh, lossless, hires, jymaster)
     * @return 播放URL或null
     */
    suspend fun getSongUrl(id: String, level: String = "exhigh"): String? = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val url = "$API_BASE_URL/song/url/v1?id=$id&level=$level"

            Log.d(TAG, "获取播放链接URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://music.163.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "获取播放链接失败: ${e.message}")
                    continuation.resume(null)
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string() ?: ""
                        Log.d(TAG, "播放链接响应: ${body.take(300)}")

                        if (body.isEmpty()) {
                            continuation.resume(null)
                            return
                        }

                        val json = JSONObject(body)
                        val code = json.optInt("code", 0)
                        val data = json.optJSONArray("data")

                        if (code == 200 && data != null && data.length() > 0) {
                            val songData = data.getJSONObject(0)
                            val playUrl = songData.optString("url", null)
                            Log.d(TAG, "获取到播放链接: ${playUrl?.take(50)}")
                            continuation.resume(playUrl)
                        } else {
                            Log.e(TAG, "获取播放链接失败: code=$code")
                            continuation.resume(null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析播放链接异常", e)
                        e.printStackTrace()
                        continuation.resume(null)
                    }
                }
            })
        }
    }

    /**
     * 获取歌词（使用DsoMusic的回调式方式）
     * @param id 歌曲ID
     * @return 歌词数据 (原歌词, 翻译歌词)
     */
    suspend fun getLyric(id: String): Pair<String?, String?> = withContext(Dispatchers.IO) {
        return@withContext suspendCancellableCoroutine { continuation ->
            val url = "$API_BASE_URL/lyric?id=$id"

            Log.d(TAG, "获取歌词URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://music.163.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "获取歌词失败: ${e.message}")
                    continuation.resume(Pair(null, null))
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string() ?: ""
                        Log.d(TAG, "歌词响应: ${body.take(300)}")

                        if (body.isEmpty()) {
                            continuation.resume(Pair(null, null))
                            return
                        }

                        val json = JSONObject(body)
                        val code = json.optInt("code", 0)

                        if (code == 200) {
                            val lrc = json.optJSONObject("lrc")
                            val tlyric = json.optJSONObject("tlyric")
                            val originalLyric = lrc?.optString("lyric", null)
                            val translatedLyric = tlyric?.optString("lyric", null)
                            Log.d(TAG, "获取到歌词 - 原歌词长度: ${originalLyric?.length ?: 0}, 翻译歌词长度: ${translatedLyric?.length ?: 0}")
                            continuation.resume(Pair(originalLyric, translatedLyric))
                        } else {
                            Log.e(TAG, "获取歌词失败: code=$code")
                            continuation.resume(Pair(null, null))
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "解析歌词异常", e)
                        e.printStackTrace()
                        continuation.resume(Pair(null, null))
                    }
                }
            })
        }
    }

    /**
     * 获取搜索建议
     * @param keywords 搜索关键词
     * @return 搜索建议列表
     */
    suspend fun getSearchSuggestions(keywords: String): List<SearchSuggestion> = withContext(Dispatchers.IO) {
        if (keywords.isBlank()) return@withContext emptyList()
        
        return@withContext suspendCancellableCoroutine { continuation ->
            val encodedKeywords = URLEncoder.encode(keywords, "UTF-8")
            val url = "$API_BASE_URL/search/suggest?keywords=$encodedKeywords&type=mobile"

            Log.d(TAG, "获取搜索建议URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Referer", "https://music.163.com/")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .get()
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "获取搜索建议失败: ${e.message}")
                    continuation.resume(emptyList())
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val body = response.body?.string() ?: ""
                        Log.d(TAG, "搜索建议响应: ${body.take(500)}")

                        if (body.isEmpty()) {
                            continuation.resume(emptyList())
                            return
                        }

                        val json = JSONObject(body)
                        val code = json.optInt("code", 0)
                        val result = json.optJSONObject("result")
                        val allMatch = result?.optJSONArray("allMatch")

                        if (code != 200 || allMatch == null) {
                            Log.e(TAG, "搜索建议失败或结果为空: code=$code")
                            continuation.resume(emptyList())
                            return
                        }

                        val suggestions = mutableListOf<SearchSuggestion>()

                        for (i in 0 until allMatch.length()) {
                            val item = allMatch.getJSONObject(i)
                            val keyword = item.optString("keyword", "")
                            val type = item.optInt("type", 1)
                            
                            if (keyword.isNotEmpty()) {
                                suggestions.add(SearchSuggestion(
                                    keyword = keyword,
                                    type = type
                                ))
                            }
                        }

                        Log.d(TAG, "获取到 ${suggestions.size} 条搜索建议")
                        continuation.resume(suggestions)
                    } catch (e: Exception) {
                        Log.e(TAG, "解析搜索建议异常", e)
                        e.printStackTrace()
                        continuation.resume(emptyList())
                    }
                }
            })
        }
    }

    // 数据模型类
    data class SongDetail(
        val id: Long,
        val name: String,
        val al: AlbumDetail?,  // 专辑信息（song/detail接口用al而不是album）
        val ar: List<ArtistDetail>?,  // 歌手信息（song/detail接口用ar而不是artists）
        val dt: Long?  // 时长
    )

    data class AlbumDetail(
        val id: Long,
        val name: String,
        val picUrl: String?,  // 封面URL
        val pic_str: String?  // 封面ID字符串
    )

    data class ArtistDetail(
        val id: Long,
        val name: String
    )

    data class SearchSuggestion(
        val keyword: String,
        val type: Int
    )
}
