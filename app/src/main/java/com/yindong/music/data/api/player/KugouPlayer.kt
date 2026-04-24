package com.yindong.music.data.api.player

import android.util.Log
import com.yindong.music.data.api.MusicUrlResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 酷狗音乐播放API（TuneHub V3方式）
 * 注意：TuneHub不支持酷狗，使用QQ平台作为替代
 */
object KugouPlayer {

    private const val TAG = "KugouPlayer"
    private const val API_KEY = "th_71949110b28b6c830b5e70b184153a59a845935b957ff126"
    private const val BASE_URL = "https://tunehub.sayqz.com/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * 获取播放链接
     * @param hash 歌曲hash
     * @param type 音质类型: 128k, 320k, flac
     * @return 播放链接结果
     */
    suspend fun getMusicUrl(hash: String, type: String): MusicUrlResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "获取酷狗播放链接: hash=$hash, type=$type")

                // 音质映射
                val quality = when (type.lowercase()) {
                    "128k" -> "128k"
                    "320k" -> "320k"
                    "flac", "flac24bit", "hires", "master" -> "flac"
                    else -> "320k"
                }

                // 构建请求体 - 酷狗使用hash作为id，但TuneHub不支持酷狗，这里用QQ平台尝试
                val requestBody = JSONObject().apply {
                    put("platform", "qq")
                    put("ids", hash)
                    put("quality", quality)
                }.toString()

                Log.d(TAG, "请求体: $requestBody")

                val request = Request.Builder()
                    .url("$BASE_URL/v1/parse")
                    .header("X-API-Key", API_KEY)
                    .header("Content-Type", "application/json")
                    .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
                    .build()

                val response = client.newCall(request).execute()
                val body = response.use { it.body?.string() }

                Log.d(TAG, "响应: code=${response.code}, body=${body?.take(500)}")

                if (body.isNullOrEmpty()) {
                    return@withContext MusicUrlResult(error = "API无响应")
                }

                // 关键修复：判断是否是HTML错误页
                if (!body.trim().startsWith("{")) {
                    Log.e(TAG, "API返回非JSON内容: ${body.take(200)}")
                    return@withContext MusicUrlResult(error = "服务器访问受限，无法播放")
                }

                val json = JSONObject(body)
                val code = json.optInt("code", -1)

                if (code != 0) {
                    val msg = json.optString("message", "获取播放链接失败")
                    Log.w(TAG, "API返回错误: code=$code, msg=$msg")
                    return@withContext MusicUrlResult(error = msg)
                }

                val dataObj = json.optJSONObject("data")
                if (dataObj == null) {
                    return@withContext MusicUrlResult(error = "API返回数据格式错误")
                }

                val data = dataObj.optJSONArray("data")
                if (data == null || data.length() == 0) {
                    return@withContext MusicUrlResult(error = "歌曲暂无版权或需要VIP")
                }

                val songData = data.getJSONObject(0)

                // 检查歌曲解析是否成功
                val success = songData.optBoolean("success", false)
                if (!success) {
                    val errorMsg = songData.optString("error", "歌曲暂无版权或需要VIP")
                    return@withContext MusicUrlResult(error = errorMsg)
                }

                val playUrl = songData.optString("url", "")

                if (playUrl.isBlank()) {
                    return@withContext MusicUrlResult(error = "歌曲暂无版权或需要VIP")
                }

                MusicUrlResult(
                    url = playUrl,
                    directUrl = playUrl,
                    isStream = false,
                    error = null
                )
            } catch (e: Exception) {
                Log.e(TAG, "获取播放链接失败: ${e.message}", e)
                MusicUrlResult(error = e.message ?: "网络请求失败")
            }
        }
    }
}
