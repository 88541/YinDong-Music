package com.dirror.music.music.netease

import android.util.Log
import com.dirror.music.util.MagicHttp
import com.google.gson.Gson

/**
 * 网易云评论管理类（使用新API）
 * API地址: http://156.225.18.78:3000
 */
object CommentManager {

    private const val API_BASE_URL = "http://156.225.18.78:3000"

    /**
     * 获取歌曲评论
     * @param id 歌曲ID
     * @param limit 返回数量
     * @param offset 偏移量（分页）
     * @param success 成功回调
     */
    fun getComments(
        id: String,
        limit: Int = 20,
        offset: Int = 0,
        success: (CommentResult) -> Unit
    ) {
        val url = "$API_BASE_URL/comment/music?id=$id&limit=$limit&offset=$offset"
        
        Log.d("CommentManager", "获取评论URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("CommentManager", "评论响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, CommentResult::class.java)
                if (result.code == 200) {
                    success.invoke(result)
                } else {
                    Log.e("CommentManager", "获取评论失败: ${result.code}")
                    success.invoke(CommentResult(200, 0, emptyList()))
                }
            } catch (e: Exception) {
                Log.e("CommentManager", "解析评论异常", e)
                e.printStackTrace()
                success.invoke(CommentResult(200, 0, emptyList()))
            }
        }, { errorMsg ->
            Log.e("CommentManager", "获取评论请求失败: $errorMsg")
            success.invoke(CommentResult(200, 0, emptyList()))
        })
    }

    /**
     * 获取热门评论
     * @param id 歌曲ID
     * @param limit 返回数量
     * @param offset 偏移量（分页）
     * @param success 成功回调
     */
    fun getHotComments(
        id: String,
        limit: Int = 15,
        offset: Int = 0,
        success: (HotCommentResult) -> Unit
    ) {
        val url = "$API_BASE_URL/comment/hot?id=$id&type=0&limit=$limit&offset=$offset"
        
        Log.d("CommentManager", "获取热门评论URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("CommentManager", "热门评论响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, HotCommentResult::class.java)
                if (result.code == 200) {
                    success.invoke(result)
                } else {
                    Log.e("CommentManager", "获取热门评论失败: ${result.code}")
                    success.invoke(HotCommentResult(200, emptyList()))
                }
            } catch (e: Exception) {
                Log.e("CommentManager", "解析热门评论异常", e)
                e.printStackTrace()
                success.invoke(HotCommentResult(200, emptyList()))
            }
        }, { errorMsg ->
            Log.e("CommentManager", "获取热门评论请求失败: $errorMsg")
            success.invoke(HotCommentResult(200, emptyList()))
        })
    }

    // 数据模型
    data class CommentResult(
        val code: Int,
        val total: Long,
        val comments: List<Comment>
    )

    data class HotCommentResult(
        val code: Int,
        val hotComments: List<Comment>
    )

    data class Comment(
        val commentId: Long,
        val user: CommentUser,
        val content: String,
        val time: Long,
        val likedCount: Long,
        val replyCount: Int? = 0
    )

    data class CommentUser(
        val userId: Long,
        val nickname: String,
        val avatarUrl: String
    )
}
