package com.dirror.music.music.netease

import android.util.Log
import com.dirror.music.util.MagicHttp
import com.google.gson.Gson

/**
 * MV管理类 (使用新API)
 * API地址: http://156.225.18.78:3000
 */
object MVManager {

    private const val API_BASE_URL = "http://156.225.18.78:3000"

    /**
     * 获取全部MV列表
     * @param area 地区：内地、港台、欧美、日本、韩国
     * @param type 类型：官方版、原生、现场版、网易出品
     * @param order 排序：上升最快、最热、最新
     * @param limit 返回数量
     * @param offset 偏移量
     * @param success 成功回调
     */
    fun getAllMV(
        area: String = "",
        type: String = "",
        order: String = "最热",
        limit: Int = 30,
        offset: Int = 0,
        success: (List<MVData>) -> Unit
    ) {
        val params = StringBuilder()
        params.append("limit=$limit")
        params.append("&offset=$offset")
        if (area.isNotEmpty()) {
            params.append("&area=${java.net.URLEncoder.encode(area, "UTF-8")}")
        }
        if (type.isNotEmpty()) {
            params.append("&type=${java.net.URLEncoder.encode(type, "UTF-8")}")
        }
        if (order.isNotEmpty()) {
            params.append("&order=${java.net.URLEncoder.encode(order, "UTF-8")}")
        }

        val url = "$API_BASE_URL/mv/all?$params"
        Log.d("MVManager", "获取MV列表URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "MV列表响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, MVListResponse::class.java)
                if (result.code == 200 && result.data != null) {
                    success.invoke(result.data)
                } else {
                    Log.e("MVManager", "获取MV列表失败: ${result.code}")
                    success.invoke(emptyList())
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析MV列表异常", e)
                e.printStackTrace()
                success.invoke(emptyList())
            }
        }, { errorMsg ->
            Log.e("MVManager", "获取MV列表请求失败: $errorMsg")
            success.invoke(emptyList())
        })
    }

    /**
     * 获取MV详情
     * @param mvid MV ID
     * @param success 成功回调
     */
    fun getMVDetail(mvid: Long, success: (MVDetailData?) -> Unit) {
        val url = "$API_BASE_URL/mv/detail?mvid=$mvid"
        Log.d("MVManager", "获取MV详情URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "MV详情响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, MVDetailResponse::class.java)
                if (result.code == 200 && result.data != null) {
                    success.invoke(result.data)
                } else {
                    Log.e("MVManager", "获取MV详情失败: ${result.code}")
                    success.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析MV详情异常", e)
                e.printStackTrace()
                success.invoke(null)
            }
        }, { errorMsg ->
            Log.e("MVManager", "获取MV详情请求失败: $errorMsg")
            success.invoke(null)
        })
    }

    /**
     * 获取MV播放链接
     * @param mvid MV ID
     * @param resolution 分辨率：1080、720、480
     * @param success 成功回调
     */
    fun getMVUrl(mvid: Long, resolution: Int = 1080, success: (String?) -> Unit) {
        val url = "$API_BASE_URL/mv/url?id=$mvid&r=$resolution"
        Log.d("MVManager", "获取MV播放链接URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "MV播放链接响应: ${response.take(300)}")
            try {
                val result = Gson().fromJson(response, MVUrlResponse::class.java)
                if (result.code == 200 && result.data?.url != null) {
                    success.invoke(result.data.url)
                } else {
                    Log.e("MVManager", "获取MV播放链接失败: ${result.code}")
                    success.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析MV播放链接异常", e)
                e.printStackTrace()
                success.invoke(null)
            }
        }, { errorMsg ->
            Log.e("MVManager", "获取MV播放链接请求失败: $errorMsg")
            success.invoke(null)
        })
    }

    /**
     * 获取歌手MV列表
     * @param artistId 歌手ID
     * @param limit 返回数量
     * @param offset 偏移量
     * @param success 成功回调
     */
    fun getArtistMV(
        artistId: Long,
        limit: Int = 30,
        offset: Int = 0,
        success: (List<MVData>) -> Unit
    ) {
        val url = "$API_BASE_URL/artist/mv?id=$artistId&limit=$limit&offset=$offset"
        Log.d("MVManager", "获取歌手MV列表URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "歌手MV列表响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, ArtistMVResponse::class.java)
                if (result.code == 200 && result.mvs != null) {
                    // 转换ArtistMVData为MVData
                    val mvList = result.mvs.map { it.toMVData() }
                    success.invoke(mvList)
                } else {
                    Log.e("MVManager", "获取歌手MV列表失败: ${result.code}")
                    success.invoke(emptyList())
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析歌手MV列表异常", e)
                e.printStackTrace()
                success.invoke(emptyList())
            }
        }, { errorMsg ->
            Log.e("MVManager", "获取歌手MV列表请求失败: $errorMsg")
            success.invoke(emptyList())
        })
    }

    /**
     * 搜索MV
     * @param keywords 搜索关键词
     * @param limit 返回数量
     * @param offset 偏移量
     * @param success 成功回调
     */
    fun searchMV(
        keywords: String,
        limit: Int = 30,
        offset: Int = 0,
        success: (List<MVData>) -> Unit
    ) {
        val encodedKeywords = java.net.URLEncoder.encode(keywords, "UTF-8")
        val url = "$API_BASE_URL/search?keywords=$encodedKeywords&type=1004&limit=$limit&offset=$offset"
        Log.d("MVManager", "搜索MV URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "搜索MV响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, MVSearchResponse::class.java)
                if (result.code == 200 && result.result?.mvs != null) {
                    success.invoke(result.result.mvs)
                } else {
                    Log.e("MVManager", "搜索MV失败: ${result.code}")
                    success.invoke(emptyList())
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析搜索MV结果异常", e)
                e.printStackTrace()
                success.invoke(emptyList())
            }
        }, { errorMsg ->
            Log.e("MVManager", "搜索MV请求失败: $errorMsg")
            success.invoke(emptyList())
        })
    }

    /**
     * 获取MV评论
     * @param mvid MV ID
     * @param limit 返回数量
     * @param offset 偏移量
     * @param success 成功回调
     */
    fun getMVComments(
        mvid: Long,
        limit: Int = 20,
        offset: Int = 0,
        success: (List<MVComment>, Int) -> Unit
    ) {
        val url = "$API_BASE_URL/comment/mv?id=$mvid&limit=$limit&offset=$offset"
        Log.d("MVManager", "获取MV评论URL: $url")

        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("MVManager", "MV评论响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, MVCommentResponse::class.java)
                if (result.code == 200 && result.comments != null) {
                    success.invoke(result.comments, result.total ?: 0)
                } else {
                    Log.e("MVManager", "获取MV评论失败: ${result.code}")
                    success.invoke(emptyList(), 0)
                }
            } catch (e: Exception) {
                Log.e("MVManager", "解析MV评论异常", e)
                e.printStackTrace()
                success.invoke(emptyList(), 0)
            }
        }, { errorMsg ->
            Log.e("MVManager", "获取MV评论请求失败: $errorMsg")
            success.invoke(emptyList(), 0)
        })
    }

    // 数据模型类

    // MV列表响应
    data class MVListResponse(
        val code: Int,
        val data: List<MVData>?
    )

    // MV数据
    data class MVData(
        val id: Long = 0,
        val name: String = "",
        val artistId: Long = 0,
        val artistName: String = "",
        val cover: String = "",
        val duration: Long = 0,
        val playCount: Long = 0,
        val publishTime: String = "",
        val briefDesc: String? = null,
        val desc: String? = null
    )

    // MV详情响应
    data class MVDetailResponse(
        val code: Int,
        val data: MVDetailData?
    )

    // MV详情数据
    data class MVDetailData(
        val id: Long = 0,
        val name: String = "",
        val artistId: Long = 0,
        val artistName: String = "",
        val cover: String = "",
        val duration: Long = 0,
        val playCount: Long = 0,
        val publishTime: String = "",
        val desc: String? = null,
        val briefDesc: String? = null,
        val brs: Map<String, String>? = null  // 不同分辨率的视频链接
    )

    // MV播放链接响应
    data class MVUrlResponse(
        val code: Int,
        val data: MVUrlData?
    )

    data class MVUrlData(
        val id: Long,
        val url: String?,
        val r: Int?
    )

    // 歌手MV响应
    data class ArtistMVResponse(
        val code: Int,
        val mvs: List<ArtistMVData>?
    )

    // 歌手MV数据
    data class ArtistMVData(
        val id: Long = 0,
        val name: String = "",
        val imgurl: String = "",
        val imgurl16v9: String = "",
        val duration: Long = 0,
        val playCount: Long = 0,
        val publishTime: String = ""
    ) {
        fun toMVData(): MVData {
            return MVData(
                id = id,
                name = name,
                artistId = 0,
                artistName = "",
                cover = imgurl,
                duration = duration,
                playCount = playCount,
                publishTime = publishTime
            )
        }
    }

    // MV搜索响应
    data class MVSearchResponse(
        val code: Int,
        val result: MVSearchResult?
    )

    data class MVSearchResult(
        val mvCount: Int,
        val mvs: List<MVData>?
    )

    // MV评论响应
    data class MVCommentResponse(
        val code: Int,
        val comments: List<MVComment>?,
        val total: Int?
    )

    // MV评论数据
    data class MVComment(
        val user: MVCommentUser,
        val content: String,
        val time: Long,
        val likedCount: Int,
        val commentId: Long
    )

    data class MVCommentUser(
        val userId: Long,
        val nickname: String,
        val avatarUrl: String
    )
}
