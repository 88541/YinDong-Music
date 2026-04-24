package com.dirror.music.music.netease

import android.util.Log
import com.dirror.music.util.MagicHttp
import com.google.gson.Gson

/**
 * 网易云搜索功能（使用新API）
 * API地址: http://156.225.18.78:3000
 */
object SearchFeatures {

    private const val API_BASE_URL = "http://156.225.18.78:3000"

    /**
     * 获取默认搜索关键词
     * @param success 成功回调 (showKeyword)
     */
    fun getDefaultKeyword(success: (String?) -> Unit) {
        val url = "$API_BASE_URL/search/default"
        
        Log.d("SearchFeatures", "获取默认搜索关键词URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("SearchFeatures", "默认关键词响应: $response")
            try {
                val result = Gson().fromJson(response, DefaultKeywordResult::class.java)
                if (result.code == 200 && result.data != null) {
                    success.invoke(result.data.showKeyword)
                } else {
                    Log.e("SearchFeatures", "获取默认关键词失败: ${result.code}")
                    success.invoke(null)
                }
            } catch (e: Exception) {
                Log.e("SearchFeatures", "解析默认关键词异常", e)
                e.printStackTrace()
                success.invoke(null)
            }
        }, { errorMsg ->
            Log.e("SearchFeatures", "获取默认关键词请求失败: $errorMsg")
            success.invoke(null)
        })
    }

    /**
     * 获取热门搜索列表（简单）
     * @param success 成功回调 (热门词列表)
     */
    fun getHotSearch(success: (List<String>) -> Unit) {
        val url = "$API_BASE_URL/search/hot"
        
        Log.d("SearchFeatures", "获取热门搜索URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("SearchFeatures", "热门搜索响应: ${response.take(300)}")
            try {
                val result = Gson().fromJson(response, HotSearchResult::class.java)
                if (result.code == 200 && result.result?.hots != null) {
                    val keywords = result.result.hots.map { it.first }
                    success.invoke(keywords)
                } else {
                    Log.e("SearchFeatures", "获取热门搜索失败: ${result.code}")
                    success.invoke(emptyList())
                }
            } catch (e: Exception) {
                Log.e("SearchFeatures", "解析热门搜索异常", e)
                e.printStackTrace()
                success.invoke(emptyList())
            }
        }, { errorMsg ->
            Log.e("SearchFeatures", "获取热门搜索请求失败: $errorMsg")
            success.invoke(emptyList())
        })
    }

    /**
     * 获取热搜列表（详细）
     * @param success 成功回调 (热搜列表)
     */
    fun getHotSearchDetail(success: (List<HotSearchDetail>) -> Unit) {
        val url = "$API_BASE_URL/search/hot/detail"
        
        Log.d("SearchFeatures", "获取热搜详情URL: $url")
        
        MagicHttp.OkHttpManager().newGet(url, { response ->
            Log.d("SearchFeatures", "热搜详情响应: ${response.take(500)}")
            try {
                val result = Gson().fromJson(response, HotSearchDetailResult::class.java)
                if (result.code == 200 && result.data != null) {
                    success.invoke(result.data)
                } else {
                    Log.e("SearchFeatures", "获取热搜详情失败: ${result.code}")
                    success.invoke(emptyList())
                }
            } catch (e: Exception) {
                Log.e("SearchFeatures", "解析热搜详情异常", e)
                e.printStackTrace()
                success.invoke(emptyList())
            }
        }, { errorMsg ->
            Log.e("SearchFeatures", "获取热搜详情请求失败: $errorMsg")
            success.invoke(emptyList())
        })
    }

    /**
     * 获取搜索建议（增强版）
     * 结合搜索建议API和搜索结果，提供更多建议
     * @param keywords 输入的关键词
     * @param success 成功回调 (建议词列表)
     */
    fun getSearchSuggest(keywords: String, success: (List<String>) -> Unit) {
        val encodedKeywords = java.net.URLEncoder.encode(keywords, "UTF-8")
        val suggestUrl = "$API_BASE_URL/search/suggest?keywords=$encodedKeywords&type=mobile"
        val searchUrl = "$API_BASE_URL/search?keywords=$encodedKeywords&limit=10"
        
        val allSuggestions = ArrayList<String>()
        var completedRequests = 0
        
        // 检查是否两个请求都完成
        fun checkComplete() {
            completedRequests++
            if (completedRequests >= 2) {
                // 去重并限制数量
                val uniqueSuggestions = allSuggestions.distinct().take(15)
                success.invoke(uniqueSuggestions)
            }
        }
        
        // 1. 获取搜索建议
        Log.d("SearchFeatures", "获取搜索建议URL: $suggestUrl")
        MagicHttp.OkHttpManager().newGet(suggestUrl, { response ->
            Log.d("SearchFeatures", "搜索建议响应: ${response.take(300)}")
            try {
                val result = Gson().fromJson(response, SearchSuggestResult::class.java)
                if (result.code == 200 && result.result?.allMatch != null) {
                    val suggestions = result.result.allMatch.map { it.keyword }
                    allSuggestions.addAll(suggestions)
                }
            } catch (e: Exception) {
                Log.e("SearchFeatures", "解析搜索建议异常", e)
            }
            checkComplete()
        }, { errorMsg ->
            Log.e("SearchFeatures", "获取搜索建议请求失败: $errorMsg")
            checkComplete()
        })
        
        // 2. 获取搜索结果中的歌曲名和歌手名作为补充建议
        Log.d("SearchFeatures", "获取搜索结果URL: $searchUrl")
        MagicHttp.OkHttpManager().newGet(searchUrl, { response ->
            try {
                val result = Gson().fromJson(response, NewSearchSong.SearchResponse::class.java)
                if (result.code == 200 && result.result?.songs != null) {
                    // 添加歌曲名
                    result.result.songs.forEach { song ->
                        allSuggestions.add(song.name)
                        // 添加歌手名
                        song.artists?.forEach { artist ->
                            if (!allSuggestions.contains(artist.name)) {
                                allSuggestions.add(artist.name)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SearchFeatures", "解析搜索结果异常", e)
            }
            checkComplete()
        }, { errorMsg ->
            Log.e("SearchFeatures", "获取搜索结果请求失败: $errorMsg")
            checkComplete()
        })
    }

    // 数据模型
    data class DefaultKeywordResult(
        val code: Int,
        val data: DefaultKeywordData?
    )

    data class DefaultKeywordData(
        val showKeyword: String,
        val realkeyword: String,
        val searchType: Int,
        val action: Int
    )

    data class HotSearchResult(
        val code: Int,
        val result: HotSearchList?
    )

    data class HotSearchList(
        val hots: List<HotSearchItem>?
    )

    data class HotSearchItem(
        val first: String,
        val second: Int,
        val third: Any?
    )

    data class HotSearchDetailResult(
        val code: Int,
        val data: List<HotSearchDetail>?
    )

    data class HotSearchDetail(
        val searchWord: String,
        val score: Int,
        val content: String?,
        val source: String?,
        val iconType: Int,
        val iconUrl: String?,
        val url: String?,
        val alg: String?
    )

    data class SearchSuggestResult(
        val code: Int,
        val result: SearchSuggestData?
    )

    data class SearchSuggestData(
        val allMatch: List<SearchSuggestItem>?
    )

    data class SearchSuggestItem(
        val keyword: String,
        val type: Int,
        val alg: String,
        val lastKeyword: String?
    )
}
