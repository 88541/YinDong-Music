package com.dirror.music.music.netease

import android.util.Log
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.util.MagicHttp
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 新网易云音乐搜索 (使用新API)
 * API地址: http://156.225.18.78:3000
 */
object NewSearchSong {

    private const val API_BASE_URL = "http://156.225.18.78:3000"

    /**
     * 搜索歌曲
     * @param keywords 关键词
     * @param offset 偏移量（分页）
     * @param limit 返回数量
     * @param success 成功回调
     * @param onCoverUpdated 封面更新回调（用于刷新UI）
     */
    fun search(
        keywords: String,
        offset: Int = 0,
        limit: Int = 30,
        success: (ArrayList<StandardSongData>) -> Unit,
        onCoverUpdated: ((Int) -> Unit)? = null
    ) {
        val encodedKeywords = java.net.URLEncoder.encode(keywords, "UTF-8")
        val url = "$API_BASE_URL/search?keywords=$encodedKeywords&limit=$limit&offset=$offset"
        
        Log.d("NewSearchSong", "搜索URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("NewSearchSong", "搜索响应: ${response.take(500)}")
            try {
                val searchResult = Gson().fromJson(response, SearchResponse::class.java)
                if (searchResult.code == 200 && searchResult.result?.songs != null) {
                    val songList = ArrayList<StandardSongData>()
                    searchResult.result.songs.forEach { song ->
                        // 搜索接口返回的album中没有picUrl，需要使用默认封面
                        // 然后通过song/detail接口异步获取真实封面
                        val defaultCover = "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"
                        Log.d("NewSearchSong", "歌曲: ${song.name}, 使用默认封面")
                        val standardSong = StandardSongData(
                            source = SOURCE_NETEASE,
                            id = song.id.toString(),
                            name = song.name,
                            imageUrl = defaultCover,  // 先使用默认封面
                            artists = ArrayList(song.artists?.map { 
                                StandardSongData.StandardArtistData(
                                    artistId = it.id?.toLong(),
                                    name = it.name
                                )
                            } ?: emptyList()),
                            neteaseInfo = StandardSongData.NeteaseInfo(
                                fee = song.fee ?: 0,
                                pl = 320000,
                                flag = 0,
                                maxbr = 320000
                            ),
                            localInfo = null,
                            dirrorInfo = null
                        )
                        songList.add(standardSong)
                    }
                    Log.d("NewSearchSong", "转换结果: ${songList.size} 首歌曲")
                    success.invoke(songList)
                    
                    // 异步获取真实封面
                    songList.forEachIndexed { index, song ->
                        val songId = song.id
                        if (!songId.isNullOrEmpty()) {
                            fetchRealCover(songId) { realCoverUrl ->
                                if (!realCoverUrl.isNullOrEmpty()) {
                                    song.imageUrl = realCoverUrl
                                    Log.d("NewSearchSong", "更新歌曲 ${song.name} 封面: $realCoverUrl")
                                    // 通知UI更新
                                    onCoverUpdated?.invoke(index)
                                }
                            }
                        }
                    }
                } else {
                    Log.e("NewSearchSong", "搜索失败或结果为空")
                    success.invoke(ArrayList())
                }
            } catch (e: Exception) {
                Log.e("NewSearchSong", "解析异常", e)
                e.printStackTrace()
                success.invoke(ArrayList())
            }
        }, { errorMsg ->
            Log.e("NewSearchSong", "请求失败: $errorMsg")
            success.invoke(ArrayList())
        })
    }

    /**
     * 获取歌曲真实封面URL
     * @param songId 歌曲ID
     * @param success 成功回调
     */
    private fun fetchRealCover(songId: String, success: (String?) -> Unit) {
        getSongDetail(songId) { detail ->
            if (detail != null) {
                val coverUrl = detail.al?.picUrl
                if (!coverUrl.isNullOrEmpty()) {
                    success.invoke(coverUrl)
                } else {
                    success.invoke(null)
                }
            } else {
                success.invoke(null)
            }
        }
    }

    /**
     * 获取歌曲详情（包含封面）
     * @param id 歌曲ID
     * @param success 成功回调
     */
    fun getSongDetail(id: String, success: (SongDetail?) -> Unit) {
        val url = "$API_BASE_URL/song/detail?ids=$id"
        
        Log.d("NewSearchSong", "获取歌曲详情URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("NewSearchSong", "歌曲详情响应: ${response.take(300)}")
            try {
                val detailResult = Gson().fromJson(response, SongDetailResponse::class.java)
                if (detailResult.code == 200 && detailResult.songs != null && detailResult.songs.isNotEmpty()) {
                    Log.d("NewSearchSong", "获取到歌曲详情: ${detailResult.songs[0].name}")
                    success.invoke(detailResult.songs[0])
                } else {
                    Log.e("NewSearchSong", "获取歌曲详情失败: ${detailResult.code}")
                    success.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("NewSearchSong", "解析歌曲详情异常", e)
                e.printStackTrace()
                success.invoke(null)
            }
        }, { errorMsg ->
            Log.e("NewSearchSong", "获取歌曲详情请求失败: $errorMsg")
            success.invoke(null)
        })
    }

    /**
     * 获取歌曲播放URL
     * @param id 歌曲ID
     * @param level 音质等级 (standard, exhigh, lossless, hires, jymaster)
     * @param success 成功回调
     */
    fun getSongUrl(id: String, level: String = "exhigh", success: (String?) -> Unit) {
        val url = "$API_BASE_URL/song/url/v1?id=$id&level=$level"
        
        Log.d("NewSearchSong", "获取播放链接URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("NewSearchSong", "播放链接响应: ${response.take(300)}")
            try {
                val urlResult = Gson().fromJson(response, SongUrlResponse::class.java)
                if (urlResult.code == 200 && urlResult.data != null && urlResult.data.isNotEmpty()) {
                    val playUrl = urlResult.data[0].url
                    Log.d("NewSearchSong", "获取到播放链接: ${playUrl?.take(50)}")
                    success.invoke(playUrl)
                } else {
                    Log.e("NewSearchSong", "获取播放链接失败: ${urlResult.code}")
                    success.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("NewSearchSong", "解析播放链接异常", e)
                e.printStackTrace()
                success.invoke(null)
            }
        }, { errorMsg ->
            Log.e("NewSearchSong", "获取播放链接请求失败: $errorMsg")
            success.invoke(null)
        })
    }

    /**
     * 获取歌词
     * @param id 歌曲ID
     * @param success 成功回调 (原歌词, 翻译歌词)
     */
    fun getLyric(id: String, success: (String?, String?) -> Unit) {
        val url = "$API_BASE_URL/lyric?id=$id"
        
        Log.d("NewSearchSong", "获取歌词URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("NewSearchSong", "歌词响应: ${response.take(300)}")
            try {
                val lyricResult = Gson().fromJson(response, LyricResponse::class.java)
                if (lyricResult.code == 200) {
                    val originalLyric = lyricResult.lrc?.lyric
                    val translatedLyric = lyricResult.tlyric?.lyric
                    Log.d("NewSearchSong", "获取到歌词 - 原歌词长度: ${originalLyric?.length ?: 0}, 翻译歌词长度: ${translatedLyric?.length ?: 0}")
                    success.invoke(originalLyric, translatedLyric)
                } else {
                    Log.e("NewSearchSong", "获取歌词失败: ${lyricResult.code}")
                    success.invoke(null, null)
                }
            } catch (e: Exception) {
                Log.e("NewSearchSong", "解析歌词异常", e)
                e.printStackTrace()
                success.invoke(null, null)
            }
        }, { errorMsg ->
            Log.e("NewSearchSong", "获取歌词请求失败: $errorMsg")
            success.invoke(null, null)
        })
    }

    // 数据模型类
    data class SearchResponse(
        val code: Int,
        val result: SearchResult?
    )

    data class SearchResult(
        val songs: List<Song>?,
        val songCount: Int?
    )

    data class Song(
        val id: Long,
        val name: String,
        val artists: List<Artist>?,
        val album: Album?,
        val duration: Long?,
        val fee: Int?
    )

    data class Artist(
        val id: Long,
        val name: String,
        val picUrl: String?
    )

    data class Album(
        val id: Long,
        val name: String,
        val picUrl: String?,
        val picId: Long?,
        val publishTime: Long?
    )

    // 歌曲详情响应
    data class SongDetailResponse(
        val code: Int,
        val songs: List<SongDetail>?
    )

    // 歌曲详情（用于song/detail接口）
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

    data class SongUrlResponse(
        val code: Int,
        val data: List<SongUrlData>?
    )

    data class SongUrlData(
        val id: Long,
        val url: String?,
        val br: Int?,
        val size: Long?,
        val type: String?,
        val level: String?
    )

    data class LyricResponse(
        val code: Int,
        val lrc: LyricData?,
        val tlyric: LyricData?,
        val klyric: LyricData?
    )

    data class LyricData(
        val version: Int?,
        val lyric: String?
    )
}
