package com.dirror.music.music.netease

import android.content.Context
import android.util.Log
import com.dirror.music.music.netease.data.TopListData
import com.dirror.music.util.MagicHttp
import com.dirror.music.util.runOnMainThread
import com.google.gson.Gson

/**
 * 排行榜（使用新API）
 */
object TopList {

    private const val API_BASE_URL = "http://156.225.18.78:3000"
    private const val API = "$API_BASE_URL/toplist/detail"
    private const val TAG = "TopList"

    fun getTopList(context: Context, success: (TopListData) -> Unit, failure: () -> Unit) {
        Log.d(TAG, "获取排行榜: $API")
        MagicHttp.OkHttpManager().getByCache(context, API, { response ->
            try {
                val topListData = Gson().fromJson(response, TopListData::class.java)
                if (topListData.code == 200) {
                    Log.d(TAG, "获取排行榜成功，共 ${topListData.list?.size} 个")
                    runOnMainThread {
                        success.invoke(topListData)
                    }
                } else {
                    Log.e(TAG, "获取排行榜失败: code=${topListData.code}")
                    runOnMainThread { failure.invoke() }
                }
            } catch (e: Exception) {
                Log.e(TAG, "解析排行榜失败", e)
                runOnMainThread { failure.invoke() }
            }
        }, {
            Log.e(TAG, "获取排行榜请求失败")
            runOnMainThread { failure.invoke() }
        })
    }

}