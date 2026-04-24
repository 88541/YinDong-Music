package com.yindong.music.security

import android.os.SystemClock
import com.yindong.music.BuildConfig

/**
 * 播放链路安全闸门：
 * - 统一复用 SecurityGuard 检测结果
 * - 在解析 / 取播放链接 / 播放前给出拦截决策
 */
object MusicPlaybackGate {

    enum class Action {
        PARSE_LINK,
        FETCH_PLAY_URL,
        START_PLAYBACK,
        DOWNLOAD,
    }

    data class Decision(
        val allowed: Boolean,
        val reason: String? = null,
        val shouldKillProcess: Boolean = false,
    )

    private const val REPORT_CACHE_TTL_MS = 8_000L
    @Volatile private var cachedReport: SecurityGuard.SecurityReport? = null
    @Volatile private var cachedAtElapsedMs: Long = 0L

    fun evaluate(action: Action, forceRefresh: Boolean = false): Decision {
        // Debug 构建不拦截，保持开发调试体验
        if (BuildConfig.DEBUG) return Decision(allowed = true)

        val report = getReport(forceRefresh) ?: return Decision(allowed = true)
        val highRisk = report.isDebuggerAttached || report.isHooked || report.isSignatureTampered
        val networkRisk = report.isProxyActive || report.isVpnActive
        if (!highRisk && !networkRisk) return Decision(allowed = true)

        val reasons = mutableListOf<String>()
        if (report.isDebuggerAttached) reasons += "检测到调试器"
        if (report.isHooked) reasons += "检测到Hook环境"
        if (report.isSignatureTampered) reasons += "签名校验异常"
        if (report.isProxyActive) reasons += "检测到代理抓包环境"
        if (report.isVpnActive) reasons += "检测到VPN环境"

        val actionName = when (action) {
            Action.PARSE_LINK -> "解析"
            Action.FETCH_PLAY_URL -> "取播放链接"
            Action.START_PLAYBACK -> "播放"
            Action.DOWNLOAD -> "下载"
        }
        val reasonText = if (reasons.isEmpty()) "环境风险" else reasons.joinToString("、")
        val shouldKill = BuildConfig.SECURITY_KILL_ON_RISK && highRisk
        return Decision(
            allowed = false,
            reason = "安全策略阻止$actionName：$reasonText",
            shouldKillProcess = shouldKill,
        )
    }

    private fun getReport(forceRefresh: Boolean): SecurityGuard.SecurityReport? {
        if (!forceRefresh) {
            val cached = cachedReport
            val now = SystemClock.elapsedRealtime()
            if (cached != null && now - cachedAtElapsedMs <= REPORT_CACHE_TTL_MS) {
                return cached
            }
        }
        return try {
            SecurityGuard.performAllChecks().also { report ->
                cachedReport = report
                cachedAtElapsedMs = SystemClock.elapsedRealtime()
            }
        } catch (_: Exception) {
            null
        }
    }
}
