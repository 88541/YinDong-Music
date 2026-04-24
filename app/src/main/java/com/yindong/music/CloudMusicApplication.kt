package com.yindong.music

import android.app.Application
import android.util.Log
import com.yindong.music.data.CrashLogManager
import com.yindong.music.data.LocalStorage
import com.yindong.music.security.CriticalUiProtector
import com.yindong.music.security.SecurityGuard

/**
 * 自定义 Application — 确保全局初始化在所有组件之前完成
 */
class CloudMusicApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 1. 最先初始化本地存储
        LocalStorage.init(this)
        LocalStorage.loadOrCreateDeviceId()
        // 2. 初始化崩溃日志管理器
        CrashLogManager.init(this)
        // 3. 安装全局异常捕获 (写入日志 + 前台服务异常兖底)
        CrashLogManager.installCrashHandler(this)
        // 4. 初始化安全加固模块
        SecurityGuard.init(this, BuildConfig.EXPECTED_SIGNATURE)
        CriticalUiProtector.init(this)

        // 5. 在 Application 层做早期快速检测（调试器 / Hook）
        if (BuildConfig.SECURITY_KILL_ON_RISK) {
            if (SecurityGuard.checkDebugger() || SecurityGuard.checkHook() || CriticalUiProtector.isStaticPolicyTampered()) {
                SecurityGuard.killProcess()
            }
        }

        // Debug 模式下输出当前签名哈希，方便首次配置
        if (BuildConfig.DEBUG) {
            Log.d("SecurityGuard", "APK Signature SHA-256: ${SecurityGuard.getCurrentSignatureHash()}")
        }
    }
}
