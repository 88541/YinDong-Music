package com.dirror.music.ui.dialog

import android.content.Context
import com.dirror.music.App
import com.dirror.music.databinding.DialogSoundQualityBinding
import com.dirror.music.ui.base.BaseBottomSheetDialog
import com.dirror.music.util.AppConfig
import com.dirror.music.util.toast

/**
 * 音质选择对话框
 */
class SoundQualityDialog(context: Context) : BaseBottomSheetDialog(context) {

    private val binding: DialogSoundQualityBinding = DialogSoundQualityBinding.inflate(layoutInflater)

    init {
        setContentView(binding.root)
    }

    override fun initView() {
        // 获取当前音质设置
        val currentQuality = AppConfig.soundQuality
        updateSelectedState(currentQuality)
    }

    override fun initListener() {
        binding.apply {
            // 标准音质
            itemStandard.setOnClickListener {
                setSoundQuality("standard", "标准音质")
            }

            // 较高音质
            itemExhigh.setOnClickListener {
                setSoundQuality("exhigh", "较高音质")
            }

            // 无损音质
            itemLossless.setOnClickListener {
                setSoundQuality("lossless", "无损音质")
            }

            // Hi-Res音质
            itemHires.setOnClickListener {
                setSoundQuality("hires", "Hi-Res音质")
            }

            // 鲸云臻音
            itemJymaster.setOnClickListener {
                setSoundQuality("jymaster", "鲸云臻音")
            }
        }
    }

    /**
     * 设置音质
     */
    private fun setSoundQuality(quality: String, qualityName: String) {
        AppConfig.soundQuality = quality
        updateSelectedState(quality)
        toast("已切换到$qualityName，切换歌曲后生效")
        dismiss()
    }

    /**
     * 更新选中状态
     */
    private fun updateSelectedState(currentQuality: String) {
        binding.apply {
            ivStandardSelected.visibility = if (currentQuality == "standard") android.view.View.VISIBLE else android.view.View.GONE
            ivExhighSelected.visibility = if (currentQuality == "exhigh") android.view.View.VISIBLE else android.view.View.GONE
            ivLosslessSelected.visibility = if (currentQuality == "lossless") android.view.View.VISIBLE else android.view.View.GONE
            ivHiresSelected.visibility = if (currentQuality == "hires") android.view.View.VISIBLE else android.view.View.GONE
            ivJymasterSelected.visibility = if (currentQuality == "jymaster") android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
