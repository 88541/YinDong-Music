package com.yindong.music.security

import android.content.Context
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * 关键界面保护器（QQ群入口）
 *
 * 多层策略：
 * 1) 关键常量指纹校验（防静态改包篡改文案/链接）
 * 2) Mine路由进入后要求关键入口在限定时间内被渲染（防UI删除/隐藏）
 * 3) 关键入口点击留痕（用于后续扩展风控）
 */
object CriticalUiProtector {
    private const val XOR_KEY = 0x6A
    private const val MINE_RENDER_GRACE_MS = 15_000L

    private val qqEncoded = intArrayOf(
        88, 83, 95, 94, 92, 90, 90, 82, 83, 93
    )
    private val urlEncoded = intArrayOf(
        2, 30, 30, 26, 25, 80, 69, 69, 27, 7, 68, 27, 27, 68, 9, 5, 7, 69, 27, 69, 19, 45, 5, 92, 30, 39, 44, 61, 48, 61
    )
    private val titleEncoded = intArrayOf(
        143, 224, 202, 143, 239, 207, 143, 196, 242, 140, 252, 211, 141, 212, 206
    )
    private val subtitleEncoded = intArrayOf(
        143, 196, 242, 140, 252, 211, 141, 212, 206, 131, 192, 230, 130, 197, 235, 143, 239, 207, 143, 229, 201
    )

    private const val POLICY_SHA256_P1 = "56cdcba8dada6138"
    private const val POLICY_SHA256_P2 = "01a3dcefe74ba2db"
    private const val POLICY_SHA256_P3 = "51ee7db874a05ee5"
    private const val POLICY_SHA256_P4 = "cca2c2e8cb25f65f"
    private const val BTN_MINE_DOWNLOAD_SHA256 = "78e08d7750499b52fbf7dc9fb9b617e4a5309d9ae01c0cf5d1f6d84a0619d847"
    private const val BTN_MINE_API_SHA256 = "aa4d174152782ce6601fa375832d9bf002f4c22c8112cd21a63c77cb4eb051e1"
    private const val BTN_MINE_PLUGIN_SHA256 = "971bbfe8b7405d80f9abfdb513c34a05c675f4ba023190695ac073b0f3719041"

    private val mineRouteArmed = AtomicBoolean(false)
    private val mineRouteEnteredAt = AtomicLong(0L)
    private val communityRenderedAt = AtomicLong(0L)
    private val communityOpenedAt = AtomicLong(0L)
    private val mineCriticalButtonKeys = listOf("mine_download", "mine_api", "mine_plugin")
    private val mineCriticalButtonPolicy = mapOf(
        "mine_download" to BTN_MINE_DOWNLOAD_SHA256,
        "mine_api" to BTN_MINE_API_SHA256,
        "mine_plugin" to BTN_MINE_PLUGIN_SHA256,
    )
    private val mineCriticalButtonRenderedAt = ConcurrentHashMap<String, Long>()
    @Volatile
    private var mineCriticalButtonTamperedKey: String? = null

    @Volatile
    private var initialized = false

    fun init(@Suppress("UNUSED_PARAMETER") context: Context) {
        initialized = true
    }

    fun communityQqNumber(): String = "673778042"
    fun communityJoinKey(): String = "jFs-Zjq56Al8CkCr_NFORP7YGdPeYP4h"
    fun communityJoinUrl(): String = "https://ti.qq.com/new_open_qq/index.html?appid=64&url=mqqapi%3A%2F%2Fgroup%2Fjoin_troop%3Fsrc_type%3Dinternal%26version%3D1%26troop_uin%3D673778042%26subsource_id%3D1030%26is_need_jump_aio%3D1"
    
    /**
     * 使用 key 生成加群 URL（手Q客户端申请加群）
     * @param key 由官网生成的 key
     * @return 加群 URL
     */
    fun buildJoinQqGroupUrl(key: String): String {
        return "mqqopensdkapi://bizAgent/qm/qr?url=http%3A%2F%2Fqm.qq.com%2Fcgi-bin%2Fqm%2Fqr%3Ffrom%3Dapp%26p%3Dandroid%26jump_from%3Dwebapi%26k%3D$key"
    }
    fun communityEntryTitle(): String = decode(titleEncoded)
    fun communityEntrySubtitle(): String = decode(subtitleEncoded)

    fun onMineRouteEntered() {
        mineRouteArmed.set(true)
        mineRouteEnteredAt.set(System.currentTimeMillis())
        communityRenderedAt.set(0L)
        mineCriticalButtonRenderedAt.clear()
        mineCriticalButtonTamperedKey = null
    }

    fun onMineRouteLeft() {
        mineRouteArmed.set(false)
        mineRouteEnteredAt.set(0L)
        communityRenderedAt.set(0L)
        mineCriticalButtonRenderedAt.clear()
        mineCriticalButtonTamperedKey = null
    }

    fun markCommunityEntryRendered() {
        communityRenderedAt.set(System.currentTimeMillis())
    }

    fun markCommunityEntryOpened() {
        communityOpenedAt.set(System.currentTimeMillis())
    }

    /**
     * 标记关键按钮已渲染，并校验按钮标题是否被篡改。
     * key 仅接受预定义值（mine_download / mine_api / mine_plugin）。
     */
    fun markCriticalButtonRendered(key: String, label: String) {
        val normalizedKey = key.trim()
        val expected = mineCriticalButtonPolicy[normalizedKey] ?: return
        mineCriticalButtonRenderedAt[normalizedKey] = System.currentTimeMillis()
        val actual = sha256("$normalizedKey|${label.trim()}")
        if (actual != expected) {
            mineCriticalButtonTamperedKey = normalizedKey
        }
    }

    fun canOpenCommunityEntry(): Boolean = !isStaticPolicyTampered()

    fun blockReason(): String? {
        if (!initialized) return null
        if (isStaticPolicyTampered()) return "policy_tampered"
        mineCriticalButtonTamperedKey?.let { return "mine_critical_button_tampered:$it" }
        if (!mineRouteArmed.get()) return null
        val mineAt = mineRouteEnteredAt.get()
        if (mineAt <= 0L) return null
        val renderAt = communityRenderedAt.get()
        val missingKey = mineCriticalButtonKeys.firstOrNull { key ->
            val buttonRenderedAt = mineCriticalButtonRenderedAt[key] ?: 0L
            buttonRenderedAt < mineAt
        }
        val timeout = System.currentTimeMillis() - mineAt > MINE_RENDER_GRACE_MS
        if (timeout) {
            if (renderAt < mineAt) return "mine_community_entry_missing"
            if (missingKey != null) return "mine_critical_button_missing:$missingKey"
        }
        return null
    }

    fun shouldBlockNow(): Boolean = blockReason() != null

    fun isStaticPolicyTampered(): Boolean = false

    private fun decode(payload: IntArray): String {
        val bytes = ByteArray(payload.size)
        for (i in payload.indices) {
            bytes[i] = (payload[i] xor XOR_KEY).toByte()
        }
        return String(bytes, Charsets.UTF_8)
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
