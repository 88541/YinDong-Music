package com.yindong.music.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.provider.Settings
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Proxy

/**
 * ═══════════════════════════════════════════
 *  安全加固核心模块 — SecurityGuard
 * ═══════════════════════════════════════════
 *
 * 功能一览:
 *  1. 反调试检测 (Debugger / TracerPid / Debug Flag)
 *  2. 反Root检测 (su / Magisk / 可写系统分区)
 *  3. 反模拟器检测 (指纹/硬件/传感器/电话特征)
 *  4. 签名校验 (SHA-256 签名摘要比对)
 *  5. 完整性校验 (Installer 来源 / debuggable flag)
 *  6. 反Hook检测 (Xposed / Frida / LSPosed / 内存特征)
 *  7. 反抓包检测 (代理/VPN 检测)
 *
 * 使用方式:
 *  SecurityGuard.init(context, "你的签名SHA256")
 *  SecurityGuard.performAllChecks() → SecurityReport
 */
object SecurityGuard {

    private lateinit var appContext: Context

    /**
     * 正确的签名 SHA-256 摘要。
     * 第一次打包签名后，通过 Gradle signingReport 或 logcat 获取，填入此处。
     * 如果留空，则跳过签名校验。
     */
    private var expectedSignature: String = ""

    /** 初始化，在 Application.onCreate 中调用 */
    fun init(context: Context, expectedSig: String = "") {
        appContext = context.applicationContext
        expectedSignature = expectedSig.uppercase().replace(":", "")
    }

    // ═══════════════════════════════════════
    //  统一检测入口
    // ═══════════════════════════════════════

    data class SecurityReport(
        val isDebuggerAttached: Boolean = false,
        val isDebuggable: Boolean = false,
        val isRooted: Boolean = false,
        val isEmulator: Boolean = false,
        val isSignatureTampered: Boolean = false,
        val isHooked: Boolean = false,
        val isProxyActive: Boolean = false,
        val isVpnActive: Boolean = false,
        val isUnofficialInstaller: Boolean = false,
    ) {
        /** 是否存在任何高危风险 */
        val hasHighRisk: Boolean
            get() = isDebuggerAttached || isSignatureTampered || isHooked

        /** 是否存在中等风险 */
        val hasMediumRisk: Boolean
            get() = isRooted || isEmulator || isDebuggable || isUnofficialInstaller

        /** 是否完全安全 */
        val isSafe: Boolean
            get() = !hasHighRisk && !hasMediumRisk
    }

    /**
     * 执行所有安全检查，返回报告。
     * 可根据业务需要决定对每项风险的处置策略。
     */
    fun performAllChecks(): SecurityReport {
        return SecurityReport(
            isDebuggerAttached = checkDebugger(),
            isDebuggable = checkDebuggable(),
            isRooted = checkRoot(),
            isEmulator = checkEmulator(),
            isSignatureTampered = checkSignature(),
            isHooked = checkHook(),
            isProxyActive = checkProxy(),
            isVpnActive = checkVPN(),
            isUnofficialInstaller = checkInstaller(),
        )
    }

    // ═══════════════════════════════════════
    //  1. 反调试检测
    // ═══════════════════════════════════════

    /** 检测是否有调试器连接 */
    fun checkDebugger(): Boolean {
        // 方式1: Java 层调试器检测
        if (Debug.isDebuggerConnected()) return true

        // 方式2: 等待调试器标记
        if (Debug.waitingForDebugger()) return true

        // 方式3: 检查 TracerPid (ptrace 附加检测)
        // 正常进程 TracerPid 应为 0，非 0 表示有调试器 attach
        try {
            val statusFile = File("/proc/self/status")
            if (statusFile.exists()) {
                statusFile.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("TracerPid:")) {
                            val pid = line.substringAfter(":").trim().toIntOrNull() ?: 0
                            if (pid != 0) return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return false
    }

    /** 检查 APK 是否为可调试版本 */
    fun checkDebuggable(): Boolean {
        return try {
            (appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════
    //  2. 反Root检测
    // ═══════════════════════════════════════

    /** 综合检测设备是否 Root */
    fun checkRoot(): Boolean {
        return checkRootBinaries() || checkRootPackages() || checkRootProperties() || checkSuAccess()
    }

    /** 检查常见 Root 二进制文件 */
    private fun checkRootBinaries(): Boolean {
        val paths = arrayOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/local/su", "/data/local/bin/su", "/data/local/xbin/su",
            "/system/app/Superuser.apk", "/system/app/SuperSU.apk",
            // Magisk
            "/sbin/.magisk", "/data/adb/magisk", "/data/adb/modules",
            "/system/bin/magisk", "/system/xbin/magisk",
            // KernelSU
            "/data/adb/ksu", "/data/adb/ksud",
        )
        return paths.any { File(it).exists() }
    }

    /** 检查 Root 相关应用包名 */
    private fun checkRootPackages(): Boolean {
        val packages = arrayOf(
            "com.topjohnwu.magisk",        // Magisk Manager
            "me.weishu.kernelsu",           // KernelSU
            "eu.chainfire.supersu",         // SuperSU
            "com.koushikdutta.superuser",   // Superuser
            "com.noshufou.android.su",      // Superuser
            "com.thirdparty.superuser",     // 第三方 Root
            "com.yellowes.su",              // SU
        )
        val pm = appContext.packageManager
        return packages.any { pkg ->
            try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /** 检查危险系统属性 */
    private fun checkRootProperties(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.build.tags"))
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val tags = reader.readLine()?.trim() ?: ""
                reader.close()
                tags.contains("test-keys")
            } finally {
                process.destroy()
            }
        } catch (_: Exception) {
            false
        }
    }

    /** 尝试执行 su 命令 */
    private fun checkSuAccess(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val result = reader.readLine()
                reader.close()
                !result.isNullOrBlank()
            } finally {
                process.destroy()
            }
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════
    //  3. 反模拟器检测
    // ═══════════════════════════════════════

    /** 综合检测是否运行在模拟器上 */
    fun checkEmulator(): Boolean {
        var score = 0

        // 硬件指纹特征
        if (Build.FINGERPRINT.contains("generic", true)) score += 2
        if (Build.FINGERPRINT.contains("vbox", true)) score += 2
        if (Build.FINGERPRINT.contains("test-keys")) score += 1
        if (Build.MODEL.contains("google_sdk", true)) score += 2
        if (Build.MODEL.contains("Emulator", true)) score += 2
        if (Build.MODEL.contains("Android SDK", true)) score += 2
        if (Build.MANUFACTURER.contains("Genymotion", true)) score += 2
        if (Build.BRAND.startsWith("generic", true)) score += 1
        if (Build.DEVICE.startsWith("generic", true)) score += 1
        if (Build.PRODUCT.contains("sdk", true)) score += 1
        if (Build.PRODUCT.contains("vbox", true)) score += 1
        if (Build.HARDWARE.contains("goldfish", true)) score += 2
        if (Build.HARDWARE.contains("ranchu", true)) score += 2

        // 运营商名称
        if (Build.HOST.contains("Build", true)) score += 1

        // IMEI / 电话号码 特征 (模拟器常见固定值)
        // 注: Android 10+ 已限制获取 IMEI，此处不调用 TelephonyManager

        // 模拟器特有文件
        val emulatorFiles = arrayOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/goldfish_pipe",
            "/dev/vport1p1",          // QEMU virtio
            "/dev/hax",               // Intel HAXM
        )
        emulatorFiles.forEach { if (File(it).exists()) score += 2 }

        // 电池特征：模拟器通常没有真实电池 (通过属性判断)
        try {
            val process = Runtime.getRuntime().exec(arrayOf("getprop", "ro.kernel.qemu"))
            try {
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val isQemu = reader.readLine()?.trim() ?: "0"
                reader.close()
                if (isQemu == "1") score += 3
            } finally {
                process.destroy()
            }
        } catch (_: Exception) {}

        // 评分 >= 3 判定为模拟器
        return score >= 3
    }

    // ═══════════════════════════════════════
    //  4. 签名校验 (防二次打包)
    // ═══════════════════════════════════════

    /**
     * 校验 APK 签名 SHA-256 是否匹配。
     * 返回 true 表示签名被篡改。
     */
    @SuppressLint("PackageManagerGetSignatures")
    fun checkSignature(): Boolean {
        if (expectedSignature.isBlank()) return false  // 未配置则跳过

        return try {
            val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                val info = appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()
            }

            if (sig == null) return true // 获取不到签名 = 异常

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            val currentSig = digest.joinToString("") { "%02X".format(it) }

            currentSig != expectedSignature
        } catch (_: Exception) {
            true // 异常情况视为篡改
        }
    }

    /**
     * 获取当前 APK 的签名 SHA-256（用于首次获取正确签名值）。
     * 在 debug 模式下调用此方法，将返回值填入 expectedSignature。
     */
    @SuppressLint("PackageManagerGetSignatures")
    fun getCurrentSignatureHash(): String {
        return try {
            val sig = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                info.signingInfo?.apkContentsSigners?.firstOrNull()
            } else {
                @Suppress("DEPRECATION")
                val info = appContext.packageManager.getPackageInfo(
                    appContext.packageName,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()
            }
            if (sig == null) return "UNKNOWN"
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val digest = md.digest(sig.toByteArray())
            digest.joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            "ERROR"
        }
    }

    // ═══════════════════════════════════════
    //  5. 安装来源校验
    // ═══════════════════════════════════════

    /** 检查安装来源是否非官方渠道 */
    fun checkInstaller(): Boolean {
        return try {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appContext.packageManager.getInstallSourceInfo(appContext.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getInstallerPackageName(appContext.packageName)
            }
            // null = adb install或手动安装, 部分设备应用商店也返回null, 不拦截避免误报
            if (installer == null) return false
            // 合法来源: Google Play, 华为, 小米, OPPO, VIVO 等应用商店
            val trustedInstallers = setOf(
                "com.android.vending",           // Google Play
                "com.huawei.appmarket",          // 华为应用市场
                "com.xiaomi.market",             // 小米应用商店
                "com.heytap.market",             // OPPO 应用商店
                "com.bbk.appstore",              // VIVO 应用商店
                "com.samsung.android.vending",   // Samsung Galaxy Store
                "com.tencent.android.qqdownloader", // 应用宝
            )
            installer !in trustedInstallers
        } catch (_: Exception) {
            false // 无法获取时不误报
        }
    }

    // ═══════════════════════════════════════
    //  6. 反Hook检测
    // ═══════════════════════════════════════

    /** 综合检测是否被 Hook 框架注入 */
    fun checkHook(): Boolean {
        return checkXposed() || checkFrida() || checkHookByStack() || checkHookByMaps()
    }

    /** 检测 Xposed / LSPosed / EdXposed */
    private fun checkXposed(): Boolean {
        // 方式1: 检查已安装的 Xposed 相关包
        val xposedPackages = arrayOf(
            "de.robv.android.xposed.installer",  // Xposed Installer
            "org.lsposed.manager",               // LSPosed
            "com.solohsu.android.edxp.manager",  // EdXposed Manager
            "org.meowcat.edxposed.manager",      // EdXposed
            "top.canyie.dreamland.manager",      // Dreamland
        )
        val pm = appContext.packageManager
        if (xposedPackages.any { pkg ->
                try { pm.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }
            }) return true

        // 方式2: 检查 Xposed 类是否被加载到内存
        try {
            Class.forName("de.robv.android.xposed.XposedBridge")
            return true
        } catch (_: ClassNotFoundException) {}

        try {
            Class.forName("de.robv.android.xposed.XC_MethodHook")
            return true
        } catch (_: ClassNotFoundException) {}

        return false
    }

    /** 检测 Frida 注入 */
    private fun checkFrida(): Boolean {
        // 方式1: Frida 默认监听端口 27042
        try {
            val socket = java.net.Socket()
            socket.connect(InetSocketAddress("127.0.0.1", 27042), 50)
            socket.close()
            return true  // 端口可连接 = Frida 运行中
        } catch (_: Exception) {}

        // 方式2: 检查 /proc/self/maps 中的 Frida 特征
        try {
            File("/proc/self/maps").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    if (line.contains("frida", true) ||
                        line.contains("gadget", true)) {
                        return true
                    }
                }
            }
        } catch (_: Exception) {}

        // 方式3: 检查 Frida 特征文件
        val fridaPaths = arrayOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
        )
        if (fridaPaths.any { File(it).exists() }) return true

        return false
    }

    /** 通过调用栈检测 Hook (Xposed hook 会在栈中留下痕迹) */
    private fun checkHookByStack(): Boolean {
        return try {
            val stackTrace = Thread.currentThread().stackTrace
            stackTrace.any { element ->
                val className = element.className ?: ""
                className.contains("xposed", true) ||
                className.contains("lsposed", true) ||
                className.contains("edxposed", true) ||
                className.contains("substrate", true) ||
                className.contains("frida", true)
            }
        } catch (_: Exception) {
            false
        }
    }

    /** 通过 /proc/self/maps 检测注入的 SO 库 (逐行扫描避免 OOM) */
    private fun checkHookByMaps(): Boolean {
        return try {
            val suspiciousLibs = arrayOf(
                "XposedBridge",
                "libsubstrate",
                "libfrida",
                "liblsp",         // LSPosed
                "libsandhook",    // SandHook
                "libwhale",       // Whale Hook
                "libriru",        // Riru (Magisk 模块)
                "libzygisk",      // Zygisk
            )
            File("/proc/self/maps").bufferedReader().useLines { lines ->
                lines.any { line ->
                    suspiciousLibs.any { lib -> line.contains(lib, true) }
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════
    //  7. 反抓包检测
    // ═══════════════════════════════════════

    /** 检测是否设置了 HTTP 代理 (抓包工具常用手段) */
    fun checkProxy(): Boolean {
        // 方式1: 系统属性检测
        val proxyHost = System.getProperty("http.proxyHost")
        val proxyPort = System.getProperty("http.proxyPort")
        if (!proxyHost.isNullOrBlank() && !proxyPort.isNullOrBlank()) return true

        // 方式2: Android 系统代理设置
        try {
            val globalProxy = Settings.Global.getString(
                appContext.contentResolver,
                Settings.Global.HTTP_PROXY
            )
            if (!globalProxy.isNullOrBlank() && globalProxy != ":0") return true
        } catch (_: Exception) {}

        return false
    }

    /** 检测是否正在使用 VPN */
    fun checkVPN(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return false
            interfaces.asSequence().any { networkInterface ->
                networkInterface.isUp &&
                (networkInterface.name.contains("tun", true) ||
                 networkInterface.name.contains("ppp", true) ||
                 networkInterface.name.contains("pptp", true))
            }
        } catch (_: Exception) {
            false
        }
    }

    // ═══════════════════════════════════════
    //  辅助方法：安全处置
    // ═══════════════════════════════════════

    /** 终止进程 (用于高危风险) */
    fun killProcess() {
        android.os.Process.killProcess(android.os.Process.myPid())
        Runtime.getRuntime().exit(0)
    }
}
