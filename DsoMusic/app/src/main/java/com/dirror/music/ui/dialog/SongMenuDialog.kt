package com.dirror.music.ui.dialog

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.dirror.music.App
import com.dirror.music.databinding.DialogSongMenuBinding
import com.dirror.music.manager.User
import com.dirror.music.music.local.MyFavorite
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.plugin.PluginConstants
import com.dirror.music.plugin.PluginSupport
import com.dirror.music.ui.base.BaseBottomSheetDialog
import com.dirror.music.util.AppConfig
import com.dirror.music.util.DownloadManager
import com.dirror.music.util.toast

/**
 * 每个歌曲右边三个点点击后显示
 */
class SongMenuDialog
constructor(
    context: Context,
    private val activity: Activity,
    private val songData: StandardSongData,
    private val itemDeleteListener: () -> Unit
) : BaseBottomSheetDialog(context) {

    private var binding = DialogSongMenuBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
    }

    override fun initListener() {
        super.initListener()
        binding.apply {
            // 下载歌曲
            itemDownloadSong.setOnClickListener {
                checkAndDownload()
            }

            // 下一首播放
            itemNextPlay.setOnClickListener {
                App.musicController.value?.addToNextPlay(songData)
                toast("成功添加到下一首播放")
                dismiss()
            }
            // 添加到本地我喜欢
            itemAddLocalMyFavorite.setOnClickListener {
                MyFavorite.addSong(songData)
                dismiss()
            }
            // 添加到网易云我喜欢
            itemAddNeteaseFavorite.setOnClickListener {
                if (AppConfig.cookie.isEmpty()) {
                    toast("离线模式无法收藏到在线我喜欢~")
                } else {
                    when (songData.source) {
                        SOURCE_NETEASE -> {
                            App.cloudMusicManager.likeSong(songData.id?:"", {
                                toast("添加到我喜欢成功")
                            }, {
                                toast("添加到我喜欢失败")
                            })
                        }
                        else -> {
                            toast("暂不支持此音源")
                            dismiss()
                        }
                    }
                }
            }
            // 歌曲信息
            itemSongInfo.setOnClickListener {
                // toast("歌曲信息 ${ songData.id }")
                SongInfoDialog(context, songData).show()
                // 自己消失
                dismiss()
            }
            // 歌曲评论
            itemSongComment.setOnClickListener {
                App.activityManager.startCommentActivity(activity, songData.source?: SOURCE_NETEASE, songData.id?:"")
                dismiss()
            }
            // 歌曲删除
            itemDeleteSong.setOnClickListener {
                itemDeleteListener()
                dismiss()
            }

            PluginSupport.setSong(songData);
            PluginSupport.setActivity(activity)
            PluginSupport.setDialog(this@SongMenuDialog)
            PluginSupport.setMenu(songMenuParent)
            PluginSupport.apply(PluginConstants.POINT_SONG_MENU_DIALOG_INIT)
        }
    }

    /**
     * 检查权限并下载
     */
    private fun checkAndDownload() {
        when {
            // Android 10+ 不需要存储权限
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                startDownload()
            }
            // 检查是否已有权限
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                    PackageManager.PERMISSION_GRANTED -> {
                startDownload()
            }
            // 请求权限
            else -> {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    REQUEST_CODE_DOWNLOAD_PERMISSION
                )
                // 保存状态，权限回调后使用
                pendingDownload = true
            }
        }
    }

    /**
     * 开始下载
     */
    fun startDownload() {
        pendingDownload = false
        // 显示音质选择对话框
        DownloadQualityDialog(context, songData).show()
        dismiss()
    }

    companion object {
        const val REQUEST_CODE_DOWNLOAD_PERMISSION = 1001
        var pendingDownload = false
    }

}