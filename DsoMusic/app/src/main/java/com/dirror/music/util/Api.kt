package com.dirror.music.util

import android.net.Uri
import android.util.Log
import com.dirror.music.data.*
import com.dirror.music.manager.User
import com.dirror.music.music.compat.CompatSearchData
import com.dirror.music.music.compat.compatSearchDataToStandardPlaylistData
import com.dirror.music.music.netease.Playlist
import com.dirror.music.music.qq.SearchSong
import com.dirror.music.music.standard.data.*
import com.dso.ext.averageAssignFixLength
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object Api {

    private const val TAG = "API"

    private const val SPLIT_PLAYLIST_NUMBER = 1000 // 切割歌单
    private const val CHEATING_CODE = -460 // Cheating 错误

    suspend fun getPlayListInfo(id: Long): DetailPlaylistInnerData? {
        val url = "${getDefaultApi()}/playlist/detail?id=${id}"
        return HttpUtils.get(url, DetailPlaylistData::class.java, true)?.playlist
    }

    suspend fun getPlayList(id: Long, useCache: Boolean): PackedSongList {
        // 使用新的 API 地址获取歌单详情
        val newApiUrl = "http://156.225.18.78:3000/playlist/detail?id=$id"
        Log.d(TAG, "使用新API获取歌单: $newApiUrl")

        return try {
            val response = HttpUtils.get(newApiUrl, NewPlaylistResponse::class.java, useCache)
            if (response?.code == 200 && response.playlist != null) {
                val songs = parseNewPlaylistSongs(response.playlist)
                Log.d(TAG, "获取歌单成功, id=$id, size=${songs.size}")
                PackedSongList(songs, response.isCache ?: false)
            } else {
                Log.e(TAG, "获取歌单失败: code=${response?.code}")
                PackedSongList(ArrayList(), false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取歌单异常", e)
            PackedSongList(ArrayList(), false)
        }
    }

    // 新的歌单响应数据类
    data class NewPlaylistResponse(
        val code: Int,
        val playlist: NewPlaylistData?,
        val isCache: Boolean? = false
    )

    data class NewPlaylistData(
        val id: Long,
        val name: String,
        val coverImgUrl: String?,
        val description: String?,
        val playCount: Long,
        val trackCount: Int,
        val tracks: List<NewTrack>?,
        val privileges: List<NewPrivilege>?  // privileges 是单独的数组
    )

    data class NewTrack(
        val id: Long,
        val name: String,
        val al: NewAlbum?,
        val ar: List<NewArtist>?,
        val fee: Int?
        // privilege 不在 track 内，而是在单独的 privileges 数组中
    )

    data class NewAlbum(
        val id: Long,
        val name: String,
        val picUrl: String?
    )

    data class NewArtist(
        val id: Long?,
        val name: String?
    )

    data class NewPrivilege(
        val id: Long?,  // 歌曲ID，用于匹配
        val pl: Long?,  // API返回的是Long类型
        val flag: Int?,
        val maxbr: Long?  // API返回的是Long类型
    )

    // 解析新歌单歌曲
    private fun parseNewPlaylistSongs(playlist: NewPlaylistData): ArrayList<StandardSongData> {
        val songs = ArrayList<StandardSongData>()

        // 创建 privilege 映射表，以歌曲ID为key
        val privilegeMap = HashMap<Long, NewPrivilege>()
        playlist.privileges?.forEach { privilege ->
            privilege.id?.let { id ->
                privilegeMap[id] = privilege
            }
        }

        Log.d(TAG, "解析歌单歌曲，共 ${playlist.tracks?.size} 首，privileges ${playlist.privileges?.size} 个")

        playlist.tracks?.forEach { track ->
            val artists = ArrayList<StandardSongData.StandardArtistData>()
            track.ar?.forEach { artist ->
                artists.add(StandardSongData.StandardArtistData(artist.id, artist.name))
            }

            // 从映射表中获取对应的 privilege
            val privilege = privilegeMap[track.id]

            val pl = privilege?.pl?.toInt() ?: 0
            Log.d(TAG, "歌曲: ${track.name}, id=${track.id}, pl=$pl, privilege=${privilege != null}")

            val song = StandardSongData(
                source = SOURCE_NETEASE,
                id = track.id.toString(),
                name = track.name,
                imageUrl = track.al?.picUrl,
                artists = artists,
                neteaseInfo = StandardSongData.NeteaseInfo(
                    fee = track.fee ?: 0,
                    pl = pl,
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

    suspend fun searchMusic(keyword:String, type:SearchType): StandardSearchResult? {
        val url = "${getDefaultApi()}/cloudsearch?keywords=$keyword&limit=100&type=${SearchType.getSearchTypeInt(type)}"
        val result = HttpUtils.get(url, NeteaseSearchResult::class.java)
        return result?.result?.toStandardResult()
    }

    suspend fun getAlbumSongs(id:Long): StandardAlbumPackage? {
        val url = "${getDefaultApi()}/album?id=${id}"
        HttpUtils.get(url, NeteaseAlbumResult::class.java)?.let {
            return StandardAlbumPackage(it.album.switchToStandard(), it.switchToStandardSongs())
        }
        return null
    }

    suspend fun getSingerSongs(id: Long): StandardSingerPackage? {
        val songs = ArrayList<StandardSongData>()
        var result: ArtistsSongs?
        do {
            val url = "${getDefaultApi()}/artist/songs?id=$id&offset=${songs.size}"
            result = HttpUtils.get(url, ArtistsSongs::class.java, true)
            result?.let {
//                Log.d(TAG, "getSingerSongs result${result.songs.size} ")
                songs.addAll(it.switchToStandardSongs())
            }
        } while (result?.more == true && result.songs.isNotEmpty())

        HttpUtils.get("${getDefaultApi()}/artist/detail?id=$id", ArtistInfoResult::class.java, true)?.data?.artist?.let {
            return StandardSingerPackage(it.switchToStandardSinger(), songs)
        }
        return null
    }

    suspend fun getOtherCPSong(song: StandardSongData): StandardSongData? {
        val r = getFromKuWo(song)
        if (r != null) {
            return r
        }
        return getFromQQ(song)
    }

    suspend fun getFromKuWo(song: StandardSongData): StandardSongData? {
        val songName = song.name?.replace(Regex("（.*）"), "")?.trim()?:""
        val artistName = song.artists?.first()?.name
        searchFromKuwo("$songName $artistName")?.forEach { res ->
            if (res.name == song.name ||  (res.name != null && res.name?.contains(songName) == true && res.name?.contains("伴奏") == false)) {
                val artName = res.artists?.first()?.name ?: ""
                song.artists?.let { artists ->
                    var checkSingerCount = 0
                    for (singer in artists) {
                        if (singer.name == artName || singer.name != null && artName.contains(singer.name)) {
                            checkSingerCount++
                        } else {
                            break
                        }
                    }
                    if (checkSingerCount == song.artists?.size) return res
                }

            }
        }
        return null
    }

    suspend fun getFromQQ(song: StandardSongData): StandardSongData? {
        val songName = song.name?.replace(Regex("（.*）"), "")?.trim()?:""
        val artistName = song.artists?.first()?.name
        searchFromQQ("$songName $artistName")?.data?.song?.list?.let {
            for (res in it) {
                if (res.songname == song.name || res.songname.contains(songName)) {
                    val nameBuffer = StringBuffer()
                    for (singer in res.singer) {
                        singer.name?.let { singerName -> nameBuffer.append(singerName) }
                    }
                    val names = nameBuffer.toString()
                    var checkSingerCount = 0
                    song.artists?.forEach forArtists@ { artist->
                        artist.name?.let { name ->
                            if (names.contains(name)) {
                                checkSingerCount++
                            } else {
                                return@forArtists
                            }
                        }
                    }
                    if (checkSingerCount == song.artists?.size) return res.switchToStandard()
                }
            }
        }
        return null
    }

    private suspend fun searchFromQQ(keywords: String): SearchSong.QQSearch? {
        val url = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?aggr=1&cr=1&flag_qc=0&p=1&n=20&w=${keywords}"
        HttpUtils.get(url, String::class.java)?.let {
            var response = it.replace("callback(", "")
            if (response.endsWith(")")) {
                response = response.substring(0, response.lastIndex)
            }
            try {
                return Gson().fromJson(response, SearchSong.QQSearch::class.java)
            } catch (e: JsonSyntaxException) {
                e.printStackTrace()
            }
        }
        return null
    }

    private suspend fun searchFromKuwo(keywords: String): List<StandardSongData>? {
        val url =
            "http://kuwo.cn/api/www/search/searchMusicBykeyWord?key=$keywords&pn=1&rn=50&httpsStatus=1&reqId=24020ad0-3ab4-11eb-8b50-cf8a98bef531"
        val header = mapOf(
            "Referer" to Uri.encode("http://kuwo.cn/search/list?key=$keywords"),
            "Cookie" to "kw_token=EUOH79P2LLK",
            "csrf" to "EUOH79P2LLK",
            "User-Agent" to "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"
        )
        HttpUtils.get(url, String::class.java,false , header)?.let {
            try {
                val resp = JSONObject(it)
                val songList = resp
                    .getJSONObject("data")
                    .getJSONArray("list")

                val standardSongDataList = ArrayList<StandardSongData>()
                // 每首歌适配
                (0 until songList.length()).forEach {
                    val songInfo = songList[it] as JSONObject
                    standardSongDataList.add(
                        com.dirror.music.music.kuwo.SearchSong.KuwoSearchData.SongData(
                            songInfo.getIntOrNull("rid").toString(),
                            songInfo.getStr("name", ""),
                            songInfo.getStr("artist", ""),
                            songInfo.getStr("pic", "")
                        ).switchToStandard()
                    )
                }
                return standardSongDataList
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    suspend fun getLoginKey(): NeteaseGetKey? {
        return HttpUtils.get("${getLoginUrl()}/login/qr/key?timestamp=${Date().time}", NeteaseGetKey::class.java)
    }

    suspend fun getLoginQRCode(key: String): NeteaseQRCodeResult? {
        return HttpUtils.get("${getLoginUrl()}/login/qr/create?key=$key&qrimg=1&timestamp=${Date().time}", NeteaseQRCodeResult::class.java)
    }

    suspend fun checkLoginResult(key: String): NeteaseLoginResult? {
        return HttpUtils.get("${getLoginUrl()}/login/qr/check?key=$key&timestamp=${Date().time}", NeteaseLoginResult::class.java)
    }

    suspend fun getUserInfo(cookie: String): NeteaseUserInfo? {
        return HttpUtils.post("${getLoginUrl()}/user/account", Utils.toMap("cookie", cookie) , NeteaseUserInfo::class.java)
    }

    private fun getLoginUrl() :String {
        return User.neteaseCloudMusicApi
    }

    private fun getDefaultApi() :String {
        var api = User.neteaseCloudMusicApi
        if (api.isEmpty()) {
            api = "https://olbb.vercel.app"
        }
        return api
    }

}