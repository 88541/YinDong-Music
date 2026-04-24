package com.dirror.music.ui.dialog

import android.content.Context
import com.dirror.music.databinding.DialogDownloadQualityBinding
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.ui.base.BaseBottomSheetDialog
import com.dirror.music.util.DownloadManager
import com.dirror.music.util.toast

/**
 * 下载音质选择对话框
 */
class DownloadQualityDialog(
    context: Context,
    private val songData: StandardSongData
) : BaseBottomSheetDialog(context) {

    private val binding: DialogDownloadQualityBinding = DialogDownloadQualityBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
    }

    override fun initListener() {
        binding.apply {
            // 标准音质
            itemStandard.setOnClickListener {
                startDownload("standard", "标准音质")
            }

            // 较高音质
            itemExhigh.setOnClickListener {
                startDownload("exhigh", "较高音质")
            }

            // 无损音质
            itemLossless.setOnClickListener {
                startDownload("lossless", "无损音质")
            }

            // Hi-Res音质
            itemHires.setOnClickListener {
                startDownload("hires", "Hi-Res音质")
            }

            // 鲸云臻音
            itemJymaster.setOnClickListener {
                startDownload("jymaster", "鲸云臻音")
            }
        }
    }

    /**
     * 开始下载
     */
    private fun startDownload(quality: String, qualityName: String) {
        DownloadManager.downloadSong(context, songData, quality)
        toast("开始下载 - $qualityName")
        dismiss()
    }
}
