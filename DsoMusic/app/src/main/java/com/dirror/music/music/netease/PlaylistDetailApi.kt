package com.dirror.music.music.netease

import android.content.Context
import android.util.Log
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.util.MagicHttp
import com.dirror.music.util.runOnMainThread
import com.google.gson.Gson

/**
 * 歌单详情 API（使用新API）
 */
object PlaylistDetailApi {

    private const val API_BASE_URL = "http://156.225.18.78:3000"
    private const val TAG = "PlaylistDetailApi"

    /**
     * 获取歌单详情
     * @param context 上下文
     * @param playlistId 歌单ID
     * @param success 成功回调
     * @param failure 失败回调
     */
    fun getPlaylistDetail(
        context: Context,
        playlistId: Long,
        success: (ArrayList<StandardSongData>) -> Unit,
        failure: () -> Unit
    ) {
        val url = "$API_BASE_URL/playlist/detail?id=$playlistId"
        Log.d(TAG, "获取歌单详情: $url")

        MagicHttp.OkHttpManager().getByCache(context, url, { response ->
            try {
                val data = Gson().fromJson(response, PlaylistDetailResponse::class.java)
                if (data.code == 200 && data.playlist != null) {
                    val songs = parsePlaylistSongs(data.playlist)
                    runOnMainThread {
                        success.invoke(songs)
                    }
                } else {
                    Log.e(TAG, "获取歌单详情失败: code=${data.code}")
                    runOnMainThread { failure.invoke() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析歌单详情失败", e)
                runOnMainThread { failure.invoke() }
            }
        }, {
            Log.e(TAG, "网络请求失败")
            runOnMainThread { failure.invoke() }
        })
    }

    /**
     * 解析歌单歌曲
     */
    private fun parsePlaylistSongs(playlist: PlaylistData): ArrayList<StandardSongData> {
        val songs = ArrayList<StandardSongData>()

        // 创建 privilege 映射表，以歌曲ID为key
        val privilegeMap = HashMap<Long, Privilege>()
        playlist.privileges?.forEach { privilege ->
            privilege.id?.let { id ->
                privilegeMap[id] = privilege
            }
        }

        playlist.tracks?.forEach { track ->
            val artists = ArrayList<StandardSongData.StandardArtistData>()
            track.ar?.forEach { artist ->
                artists.add(StandardSongData.StandardArtistData(artist.id, artist.name))
            }

            // 从映射表中获取对应的 privilege
            val privilege = privilegeMap[track.id]

            val song = StandardSongData(
                source = SOURCE_NETEASE,
                id = track.id.toString(),
                name = track.name,
                imageUrl = track.al?.picUrl,
                artists = artists,
                neteaseInfo = StandardSongData.NeteaseInfo(
                    fee = track.fee ?: 0,
                    pl = privilege?.pl?.toInt() ?: 0,
                    flag = privilege?.flag ?: 0,
                    maxbr = privilege?.maxbr?.toInt() ?: 0
                ),
                localInfo = null,
                dirrorInfo = null
            )
            songs.add(song)
        }

        return songs
    }

    // 数据类定义
    data class PlaylistDetailResponse(
        val code: Int,
        val playlist: PlaylistData?
    )

    data class PlaylistData(
        val id: Long,
        val name: String,
        val coverImgUrl: String,
        val description: String,
        val playCount: Long,
        val trackCount: Int,
        val tracks: List<Track>?,
        val privileges: List<Privilege>?  // privileges 是单独的数组
    )

    data class Track(
        val id: Long,
        val name: String,
        val al: Album?,
        val ar: List<Artist>?,
        val fee: Int?
        // privilege 不在 track 内，而是在单独的 privileges 数组中
    )

    data class Album(
        val id: Long,
        val name: String,
        val picUrl: String?
    )

    data class Artist(
        val id: Long?,
        val name: String?
    )

    data class Privilege(
        val id: Long?,  // 歌曲ID，用于匹配
        val pl: Long?,  // API返回的是Long类型
        val flag: Int?,
        val maxbr: Long?  // API返回的是Long类型
    )
}
