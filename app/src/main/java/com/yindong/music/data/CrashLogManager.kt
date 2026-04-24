package com.yindong.music.data

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 崩溃日志管理器
 * - 捕获所有未处理异常并写入本地文件
 * - 提供日志读取/清除接口给开发者模式 UI
 * - 每条日志包含: 时间、设备信息、异常堆栈
 */
object CrashLogManager {

    private const val LOG_DIR = "crash_logs"
    private const val MAX_LOG_FILES = 50
    private var logDir: File? = null

    /** 初始化 (在 Application.onCreate 调用) */
    fun init(context: Context) {
        logDir = File(context.filesDir, LOG_DIR).also { it.mkdirs() }
    }

    /**
     * 安装全局异常捕获器
     * 捕获崩溃 → 写入日志文件 → 交给默认处理器
     */
    fun installCrashHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // 写入崩溃日志
            try {
                saveCrashLog(throwable, thread.name)
            } catch (_: Exception) {}

            // 前台服务相关的非致命异常可以吞掉
            val msg = throwable.message ?: ""
            val isFgsStartNotAllowed =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    throwable is android.app.ForegroundServiceStartNotAllowedException
            val isForegroundServiceCrash = msg.contains("ForegroundService") ||
                msg.contains("startForeground") ||
                msg.contains("Not allowed to start service") ||
                isFgsStartNotAllowed
            if (isForegroundServiceCrash) {
                return@setDefaultUncaughtExceptionHandler
            }

            // 其他异常交给默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /** 手动记录一条异常日志 (非崩溃级别) */
    fun logException(tag: String, throwable: Throwable) {
        try {
            saveCrashLog(throwable, "[$tag]", isCrash = false)
        } catch (_: Exception) {}
    }

    /** 保存崩溃/异常日志到文件 */
    private fun saveCrashLog(throwable: Throwable, threadName: String, isCrash: Boolean = true) {
        val dir = logDir ?: return
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.getDefault())
        val timeStr = dateFormat.format(Date())
        val fileName = "${if (isCrash) "CRASH" else "ERROR"}_$timeStr.log"

        val sb = StringBuilder()
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine(if (isCrash) "💥 CRASH REPORT" else "⚠️ ERROR REPORT")
        sb.appendLine("═══════════════════════════════════════")
        sb.appendLine()
        sb.appendLine("时间: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())}")
        sb.appendLine("线程: $threadName")
        sb.appendLine()

        // 设备信息
        sb.appendLine("── 设备信息 ──")
        sb.appendLine("品牌: ${Build.BRAND}")
        sb.appendLine("型号: ${Build.MODEL}")
        sb.appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        sb.appendLine("CPU: ${Build.SUPPORTED_ABIS.joinToString()}")
        sb.appendLine()

        // 内存信息
        try {
            val runtime = Runtime.getRuntime()
            val maxMem = runtime.maxMemory() / 1024 / 1024
            val totalMem = runtime.totalMemory() / 1024 / 1024
            val freeMem = runtime.freeMemory() / 1024 / 1024
            sb.appendLine("── 内存 ──")
            sb.appendLine("最大: ${maxMem}MB | 已用: ${totalMem - freeMem}MB | 空闲: ${freeMem}MB")
            sb.appendLine()
        } catch (_: Exception) {}

        // 异常堆栈
        sb.appendLine("── 异常信息 ──")
        sb.appendLine("类型: ${throwable.javaClass.name}")
        sb.appendLine("消息: ${throwable.message}")
        sb.appendLine()
        sb.appendLine("── 完整堆栈 ──")
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        sb.appendLine(sw.toString())

        // 写入文件
        File(dir, fileName).writeText(sb.toString())

        // 清理旧日志
        cleanOldLogs()
    }

    /** 获取所有日志文件，按时间倒序 */
    fun getLogFiles(): List<CrashLogEntry> {
        val dir = logDir ?: return emptyList()
        return dir.listFiles()
            ?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() }
            ?.take(MAX_LOG_FILES)
            ?.map { file ->
                CrashLogEntry(
                    fileName = file.name,
                    time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(Date(file.lastModified())),
                    isCrash = file.name.startsWith("CRASH"),
                    sizeKB = (file.length() / 1024.0).let { "%.1f".format(it) },
                    content = try { file.readText() } catch (_: Exception) { "读取失败" },
                )
            }
            ?: emptyList()
    }

    /** 获取日志数量 */
    fun getLogCount(): Int {
        return logDir?.listFiles()?.count { it.extension == "log" } ?: 0
    }

    /** 清除所有日志 */
    fun clearAllLogs() {
        logDir?.listFiles()?.forEach { it.delete() }
    }

    /** 清理旧日志，只保留最新 MAX_LOG_FILES 个 */
    private fun cleanOldLogs() {
        val files = logDir?.listFiles()?.filter { it.extension == "log" }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.size > MAX_LOG_FILES) {
            files.drop(MAX_LOG_FILES).forEach { it.delete() }
        }
    }
}

/** 崩溃日志条目 */
data class CrashLogEntry(
    val fileName: String,
    val time: String,
    val isCrash: Boolean,
    val sizeKB: String,
    val content: String,
)
