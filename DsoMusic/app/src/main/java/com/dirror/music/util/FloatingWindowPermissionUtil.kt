package com.dirror.music.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

/**
 * 悬浮窗权限工具类
 */
object FloatingWindowPermissionUtil {

    /**
     * 检查是否有悬浮窗权限
     */
    fun canDrawOverlays(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }

    /**
     * 请求悬浮窗权限
     */
    fun requestFloatingWindowPermission(activity: Activity, requestCode: Int = 1001) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(activity)) {
                AlertDialog.Builder(activity)
                    .setTitle("需要悬浮窗权限")
                    .setMessage("歌词悬浮窗功能需要悬浮窗权限才能正常使用，是否前往设置开启？")
                    .setPositiveButton("去开启") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${activity.packageName}")
                        )
                        activity.startActivityForResult(intent, requestCode)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }
    }

    /**
     * 检查并请求悬浮窗权限（带回调）
     */
    fun checkAndRequestPermission(
        activity: Activity,
        onGranted: () -> Unit,
        onDenied: () -> Unit = {}
    ) {
        if (canDrawOverlays(activity)) {
            onGranted()
        } else {
            AlertDialog.Builder(activity)
                .setTitle("需要悬浮窗权限")
                .setMessage("歌词悬浮窗功能需要悬浮窗权限才能正常使用，是否前往设置开启？")
                .setPositiveButton("去开启") { _, _ ->
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:${activity.packageName}")
                    )
                    activity.startActivity(intent)
                }
                .setNegativeButton("取消") { _, _ ->
                    onDenied()
                }
                .setCancelable(false)
                .show()
        }
    }

    /**
     * 处理权限申请结果
     */
    fun onActivityResult(context: Context, requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return if (requestCode == 1001) {
            if (canDrawOverlays(context)) {
                Toast.makeText(context, "悬浮窗权限已开启", Toast.LENGTH_SHORT).show()
                true
            } else {
                Toast.makeText(context, "悬浮窗权限未开启", Toast.LENGTH_SHORT).show()
                false
            }
        } else {
            false
        }
    }
}
