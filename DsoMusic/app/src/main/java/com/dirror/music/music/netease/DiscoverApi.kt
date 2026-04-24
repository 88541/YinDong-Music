package com.dirror.music.music.netease

import android.content.Context
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.util.MagicHttp
import com.dirror.music.util.runOnMainThread
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * 发现页面 API（使用新API）
 */
object DiscoverApi {

    private const val API_BASE_URL = "http://156.225.18.78:3000"

    /**
     * 获取推荐歌单
     */
    fun getRecommendPlaylists(context: Context, limit: Int = 16, success: (List<PlaylistItem>) -> Unit, failure: () -> Unit) {
        val url = "$API_BASE_URL/personalized?limit=$limit"
        MagicHttp.OkHttpManager().getByCache(context, url, { json ->
            try {
                val data = Gson().fromJson(json, RecommendPlaylistResponse::class.java)
                if (data.code == 200) {
                    runOnMainThread {
                        success.invoke(data.result)
                    }
                } else {
                    failure.invoke()
                }
            } catch (e: Exception) {
                failure.invoke()
            }
        }, {
            failure.invoke()
        })
    }

    /**
     * 获取新歌速递
     */
    fun getNewSongs(context: Context, limit: Int = 12, success: (List<StandardSongData>) -> Unit, failure: () -> Unit) {
        val url = "$API_BASE_URL/personalized/newsong?limit=$limit"
        MagicHttp.OkHttpManager().getByCache(context, url, { json ->
            try {
                val data = Gson().fromJson(json, NewSongResponse::class.java)
                if (data.code == 200) {
                    val songs = data.result.map { item ->
                        val artists = ArrayList(item.song.artists.map { artist ->
                            StandardSongData.StandardArtistData(artist.id, artist.name)
                        })
                        StandardSongData(
                            source = SOURCE_NETEASE,
                            id = item.id.toString(),
                            name = item.name,
                            imageUrl = item.picUrl,
                            artists = artists,
                            neteaseInfo = StandardSongData.NeteaseInfo(
                                item.song.fee,
                                item.song.privilege.pl.toInt(),
                                item.song.privilege.flag,
                                item.song.privilege.maxbr.toInt()
                            ),
                            localInfo = null,
                            dirrorInfo = null
                        )
                    }
                    runOnMainThread {
                        success.invoke(songs)
                    }
                } else {
                    failure.invoke()
                }
            } catch (e: Exception) {
                failure.invoke()
            }
        }, {
            failure.invoke()
        })
    }

    /**
     * 获取排行榜列表
     */
    fun getToplists(context: Context, success: (List<TopListItem>) -> Unit, failure: () -> Unit) {
        val url = "$API_BASE_URL/toplist/detail"
        MagicHttp.OkHttpManager().getByCache(context, url, { json ->
            try {
                val data = Gson().fromJson(json, TopListResponse::class.java)
                if (data.code == 200) {
                    runOnMainThread {
                        success.invoke(data.list)
                    }
                } else {
                    failure.invoke()
                }
            } catch (e: Exception) {
                failure.invoke()
            }
        }, {
            failure.invoke()
        })
    }

    /**
     * 获取精品歌单
     */
    fun getHighQualityPlaylists(context: Context, limit: Int = 10, success: (List<HighQualityPlaylist>) -> Unit, failure: () -> Unit) {
        val url = "$API_BASE_URL/top/playlist/highquality?limit=$limit"
        MagicHttp.OkHttpManager().getByCache(context, url, { json ->
            try {
                val data = Gson().fromJson(json, HighQualityPlaylistResponse::class.java)
                if (data.code == 200) {
                    runOnMainThread {
                        success.invoke(data.playlists)
                    }
                } else {
                    failure.invoke()
                }
            } catch (e: Exception) {
                failure.invoke()
            }
        }, {
            failure.invoke()
        })
    }

    /**
     * 获取热门歌手
     */
    fun getTopArtists(context: Context, limit: Int = 10, success: (List<ArtistItem>) -> Unit, failure: () -> Unit) {
        val url = "$API_BASE_URL/top/artists?limit=$limit"
        MagicHttp.OkHttpManager().getByCache(context, url, { json ->
            try {
                val data = Gson().fromJson(json, TopArtistsResponse::class.java)
                if (data.code == 200) {
                    runOnMainThread {
                        success.invoke(data.artists)
                    }
                } else {
                    failure.invoke()
                }
            } catch (e: Exception) {
                failure.invoke()
            }
        }, {
            failure.invoke()
        })
    }

    // 数据类定义
    data class RecommendPlaylistResponse(
        val code: Int,
        val result: List<PlaylistItem>
    )

    data class PlaylistItem(
        val id: Long,
        val name: String,
        val picUrl: String,
        val playCount: Long
    )

    data class NewSongResponse(
        val code: Int,
        val result: List<NewSongItem>
    )

    data class NewSongItem(
        val id: Long,
        val name: String,
        val picUrl: String,
        val song: SongDetail
    )

    data class SongDetail(
        val artists: List<Artist>,
        val fee: Int,
        val privilege: Privilege
    )

    data class Artist(
        val id: Long,
        val name: String
    )

    data class Privilege(
        val pl: Long,
        val flag: Int,
        val maxbr: Long
    )

    data class TopListResponse(
        val code: Int,
        val list: List<TopListItem>
    )

    data class TopListItem(
        val id: Long,
        val name: String,
        val coverImgUrl: String,
        val description: String,
        val tracks: List<TopListTrack>?
    )

    data class TopListTrack(
        val first: String,
        val second: String
    )

    data class HighQualityPlaylistResponse(
        val code: Int,
        val playlists: List<HighQualityPlaylist>
    )

    data class HighQualityPlaylist(
        val id: Long,
        val name: String,
        val coverImgUrl: String,
        val playCount: Long,
        val description: String
    )

    data class TopArtistsResponse(
        val code: Int,
        val artists: List<ArtistItem>
    )

    data class ArtistItem(
        val id: Long,
        val name: String,
        val picUrl: String
    )
}
