package com.example.myapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapp.verification.UpdateManager
import com.example.myapp.verification.VerificationManager
import kotlinx.coroutines.launch

/**
 * ======================================================
 *  MainActivity 示例 - 展示如何接入网络验证
 * ======================================================
 *
 *  核心逻辑:
 *    onCreate → 显示加载画面 → 调用 verify()
 *      → 成功: 隐藏加载, 显示主界面
 *      → 失败: 弹窗提示, 点击退出
 *
 *  你只需要把下面的验证逻辑复制到你自己的 Activity 中即可
 */
class MainActivity : AppCompatActivity() {

    private lateinit var verifier: VerificationManager

    // ---- UI 组件 (你换成自己的) ----
    private lateinit var loadingView: ProgressBar
    private lateinit var loadingText: TextView
    private lateinit var mainContent: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化 UI (根据你自己的布局修改)
        loadingView = findViewById(R.id.loading_progress)
        loadingText = findViewById(R.id.loading_text)
        mainContent = findViewById(R.id.main_content)

        // 初始状态: 显示加载, 隐藏主界面
        loadingView.visibility = View.VISIBLE
        loadingText.visibility = View.VISIBLE
        loadingText.text = "正在连接服务器..."
        mainContent.visibility = View.GONE

        // ============================================================
        //  ★ 初始化验证管理器
        // ============================================================
        verifier = VerificationManager(this)

        // 心跳失败回调 (比如服务器突然关了, 或者被管理员踢了)
        verifier.onHeartbeatFailed = { msg ->
            runOnUiThread {
                mainContent.visibility = View.GONE
                showExitDialog("连接已断开", msg)
            }
        }

        // 收到服务器公告
        verifier.onAnnouncement = { msg ->
            runOnUiThread {
                if (msg.isNotEmpty()) {
                    Toast.makeText(this, "公告: $msg", Toast.LENGTH_LONG).show()
                }
            }
        }

        // ============================================================
        //  ★ 启动验证
        // ============================================================
        startVerification()
    }

    /**
     * 执行网络验证
     */
    private fun startVerification() {
        lifecycleScope.launch {
            try {
                val result = verifier.verify()

                if (result.success) {
                    // ✅ 验证通过 → 显示主界面
                    loadingView.visibility = View.GONE
                    loadingText.visibility = View.GONE
                    mainContent.visibility = View.VISIBLE

                    // 显示公告 (如果有)
                    if (result.announcement.isNotEmpty()) {
                        Toast.makeText(
                            this@MainActivity,
                            result.announcement,
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    // ============================
                    // 在这里加载你的应用正常内容
                    // ============================

                    // ★ 验证通过后自动检查更新
                    UpdateManager(this@MainActivity).checkUpdate()

                } else {
                    // ❌ 验证失败 → 弹窗退出
                    loadingView.visibility = View.GONE
                    loadingText.visibility = View.GONE
                    showExitDialog("无法启动", result.message)
                }
            } catch (e: Exception) {
                // 网络超时、崩溃等都会到这里，保证用户能看到提示
                loadingView.visibility = View.GONE
                loadingText.visibility = View.GONE
                showExitDialog("验证失败", e.message ?: "请检查网络后重试")
            }
        }
    }

    /**
     * 弹出对话框, 用户只能点退出
     */
    private fun showExitDialog(title: String, message: String) {
        if (isFinishing || isDestroyed) return

        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false) // 不允许点外面关闭
            .setPositiveButton("退出应用") { _, _ ->
                finish()
            }
            .setNegativeButton("重试") { dialog, _ ->
                dialog.dismiss()
                // 重新显示加载
                loadingView.visibility = View.VISIBLE
                loadingText.visibility = View.VISIBLE
                loadingText.text = "正在重新连接..."
                mainContent.visibility = View.GONE
                // 重新验证
                startVerification()
            }
            .show()
    }

    override fun onDestroy() {
        // ★ 退出时注销会话
        verifier.logout()
        super.onDestroy()
    }
}
