package com.dirror.music.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.dirror.music.R
import com.dirror.music.databinding.ActivityFloatingLyricsSettingsBinding
import com.dirror.music.service.FloatingLyricsService
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.util.FloatingLyricsSettings

/**
 * 悬浮歌词设置页面
 */
class FloatingLyricsSettingsActivity : BaseActivity() {

    private lateinit var binding: ActivityFloatingLyricsSettingsBinding

    private lateinit var tvPreviewCurrent: TextView
    private lateinit var tvPreviewNext: TextView
    private lateinit var previewContainer: android.widget.FrameLayout
    private lateinit var seekBarTextSize: SeekBar
    private lateinit var seekBarAlpha: SeekBar
    private lateinit var seekBarWidth: SeekBar
    private lateinit var seekBarHeight: SeekBar
    private lateinit var radioGroupTextColor: RadioGroup
    private lateinit var rbWhite: RadioButton
    private lateinit var rbGreen: RadioButton
    private lateinit var rbCyan: RadioButton
    private lateinit var rbYellow: RadioButton
    private lateinit var switchShowNextLine: Switch
    private lateinit var etCustomText: EditText
    private lateinit var btnReset: Button

    override fun initBinding() {
        binding = ActivityFloatingLyricsSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initView() {
        // 设置标题栏
        binding.titleBarLayout.setTitleBarText("悬浮歌词设置")

        // 初始化视图
        tvPreviewCurrent = binding.tvPreviewCurrent
        tvPreviewNext = binding.tvPreviewNext
        previewContainer = binding.previewContainer
        seekBarTextSize = binding.seekBarTextSize
        seekBarAlpha = binding.seekBarAlpha
        seekBarWidth = binding.seekBarWidth
        seekBarHeight = binding.seekBarHeight
        radioGroupTextColor = binding.radioGroupTextColor
        rbWhite = binding.rbWhite
        rbGreen = binding.rbGreen
        rbCyan = binding.rbCyan
        rbYellow = binding.rbYellow
        switchShowNextLine = binding.switchShowNextLine
        etCustomText = binding.etCustomText
        btnReset = binding.btnReset

        // 加载当前设置
        loadSettings()

        // 设置监听器
        setupListeners()
    }

    private fun loadSettings() {
        // 文字大小
        val textSize = FloatingLyricsSettings.getTextSize(this)
        seekBarTextSize.progress = textSize.toInt()
        tvPreviewCurrent.textSize = textSize
        tvPreviewNext.textSize = textSize - 4

        // 背景透明度
        val alpha = FloatingLyricsSettings.getBackgroundAlpha(this)
        seekBarAlpha.progress = alpha
        updatePreviewBackground(alpha)

        // 文字颜色
        val textColor = FloatingLyricsSettings.getTextColor(this)
        when (textColor) {
            Color.WHITE -> rbWhite.isChecked = true
            Color.GREEN -> rbGreen.isChecked = true
            Color.CYAN -> rbCyan.isChecked = true
            Color.YELLOW -> rbYellow.isChecked = true
            else -> rbWhite.isChecked = true
        }
        tvPreviewCurrent.setTextColor(textColor)

        // 显示下一句
        val showNextLine = FloatingLyricsSettings.getShowNextLine(this)
        switchShowNextLine.isChecked = showNextLine
        tvPreviewNext.visibility = if (showNextLine) android.view.View.VISIBLE else android.view.View.GONE

        // 窗口宽度
        val windowWidth = FloatingLyricsSettings.getWindowWidth(this)
        seekBarWidth.progress = if (windowWidth > 0) windowWidth else 400

        // 窗口高度
        val windowHeight = FloatingLyricsSettings.getWindowHeight(this)
        seekBarHeight.progress = if (windowHeight > 0) windowHeight else 100

        // 自定义提示文字
        val customText = FloatingLyricsSettings.getCustomText(this)
        etCustomText.setText(customText)
    }

    private fun setupListeners() {
        // 文字大小
        seekBarTextSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val size = progress.coerceAtLeast(12).toFloat()
                tvPreviewCurrent.textSize = size
                tvPreviewNext.textSize = size - 4
                FloatingLyricsSettings.setTextSize(this@FloatingLyricsSettingsActivity, size)
                refreshFloatingLyrics()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 背景透明度
        seekBarAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updatePreviewBackground(progress)
                FloatingLyricsSettings.setBackgroundAlpha(this@FloatingLyricsSettingsActivity, progress)
                refreshFloatingLyrics()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 文字颜色
        radioGroupTextColor.setOnCheckedChangeListener { _, checkedId ->
            val color = when (checkedId) {
                R.id.rbWhite -> Color.WHITE
                R.id.rbGreen -> Color.GREEN
                R.id.rbCyan -> Color.CYAN
                R.id.rbYellow -> Color.YELLOW
                else -> Color.WHITE
            }
            tvPreviewCurrent.setTextColor(color)
            FloatingLyricsSettings.setTextColor(this, color)
            refreshFloatingLyrics()
        }

        // 显示下一句
        switchShowNextLine.setOnCheckedChangeListener { _, isChecked ->
            tvPreviewNext.visibility = if (isChecked) android.view.View.VISIBLE else android.view.View.GONE
            FloatingLyricsSettings.setShowNextLine(this, isChecked)
            refreshFloatingLyrics()
        }

        // 窗口宽度
        seekBarWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val width = progress.coerceAtLeast(200)
                FloatingLyricsSettings.setWindowWidth(this@FloatingLyricsSettingsActivity, width)
                refreshFloatingLyrics()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 窗口高度
        seekBarHeight.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val height = progress.coerceAtLeast(60)
                FloatingLyricsSettings.setWindowHeight(this@FloatingLyricsSettingsActivity, height)
                refreshFloatingLyrics()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 自定义提示文字
        etCustomText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val text = s?.toString() ?: ""
                FloatingLyricsSettings.setCustomText(this@FloatingLyricsSettingsActivity, text)
                refreshFloatingLyrics()
            }
        })

        // 重置按钮
        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("恢复默认设置")
                .setMessage("确定要恢复默认设置吗？")
                .setPositiveButton("确定") { _, _ ->
                    FloatingLyricsSettings.resetToDefault(this)
                    loadSettings()
                    refreshFloatingLyrics()
                }
                .setNegativeButton("取消", null)
                .show()
        }
    }

    private fun refreshFloatingLyrics() {
        FloatingLyricsService.refreshSettings(this)
    }

    private fun updatePreviewBackground(alpha: Int) {
        val backgroundColor = FloatingLyricsSettings.getBackgroundColor(this)
        val colorWithAlpha = Color.argb(
            alpha,
            Color.red(backgroundColor),
            Color.green(backgroundColor),
            Color.blue(backgroundColor)
        )
        previewContainer.setBackgroundColor(colorWithAlpha)
    }
}
