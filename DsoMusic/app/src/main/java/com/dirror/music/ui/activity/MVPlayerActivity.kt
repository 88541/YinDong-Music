package com.dirror.music.ui.activity

import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.MediaController
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.dirror.music.adapter.MVAdapter
import com.dirror.music.databinding.ActivityMvPlayerBinding
import com.dirror.music.music.netease.MVManager
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.util.runOnMainThread

/**
 * MV播放页面
 */
class MVPlayerActivity : BaseActivity() {

    private lateinit var binding: ActivityMvPlayerBinding
    private var mvid: Long = 0
    private lateinit var mvName: String
    private lateinit var artistName: String
    private lateinit var coverUrl: String
    private var currentResolution = 1080
    private lateinit var relatedMVAdapter: MVAdapter

    override fun initBinding() {
        binding = ActivityMvPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initView() {
        // 获取传递的参数
        mvid = intent.getLongExtra("mvid", 0)
        mvName = intent.getStringExtra("mv_name") ?: "未知MV"
        artistName = intent.getStringExtra("artist_name") ?: "未知歌手"
        coverUrl = intent.getStringExtra("cover") ?: ""

        if (mvid == 0L) {
            Toast.makeText(this, "MV ID无效", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 设置标题栏
        binding.titleBarLayout.setTitleBarText(mvName)

        // 设置MV信息
        binding.tvMVName.text = mvName
        binding.tvArtistName.text = artistName

        // 初始化视频播放器
        setupVideoPlayer()

        // 初始化相关MV列表
        setupRelatedMVList()

        // 加载MV详情和播放链接
        loadMVDetail()
        loadMVUrl()

        // 加载相关MV
        loadRelatedMV()
    }

    private fun setupVideoPlayer() {
        val mediaController = MediaController(this)
        mediaController.setAnchorView(binding.videoView)
        binding.videoView.setMediaController(mediaController)

        // 监听播放完成
        binding.videoView.setOnCompletionListener {
            // 播放完成
        }

        // 监听错误
        binding.videoView.setOnErrorListener { _, what, extra ->
            runOnMainThread {
                binding.progressBar.visibility = View.GONE
                binding.tvError.visibility = View.VISIBLE
                binding.tvError.text = "播放错误: $what, $extra"
            }
            true
        }

        // 监听准备完成
        binding.videoView.setOnPreparedListener {
            runOnMainThread {
                binding.progressBar.visibility = View.GONE
                binding.videoView.start()
            }
        }
    }

    private fun setupRelatedMVList() {
        relatedMVAdapter = MVAdapter { mv ->
            // 点击相关MV，重新加载当前页面
            mvid = mv.id
            mvName = mv.name
            artistName = mv.artistName
            coverUrl = mv.cover

            binding.titleBarLayout.setTitleBarText(mvName)
            binding.tvMVName.text = mvName
            binding.tvArtistName.text = artistName

            // 重新加载
            loadMVDetail()
            loadMVUrl()
        }

        binding.rvRelatedMV.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRelatedMV.adapter = relatedMVAdapter
    }

    private fun loadMVDetail() {
        MVManager.getMVDetail(mvid) { detail ->
            runOnMainThread {
                if (detail != null) {
                    binding.tvMVDesc.text = detail.desc ?: detail.briefDesc ?: "暂无描述"
                    binding.tvMVDesc.visibility = View.VISIBLE
                } else {
                    binding.tvMVDesc.visibility = View.GONE
                }
            }
        }
    }

    private fun loadMVUrl() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvError.visibility = View.GONE

        MVManager.getMVUrl(mvid, currentResolution) { url ->
            runOnMainThread {
                if (!url.isNullOrEmpty()) {
                    try {
                        binding.videoView.setVideoURI(Uri.parse(url))
                        binding.videoView.requestFocus()
                    } catch (e: Exception) {
                        binding.progressBar.visibility = View.GONE
                        binding.tvError.visibility = View.VISIBLE
                        binding.tvError.text = "视频加载失败"
                    }
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.tvError.visibility = View.VISIBLE
                    binding.tvError.text = "无法获取播放链接"

                    // 尝试降低分辨率
                    if (currentResolution > 480) {
                        currentResolution = 480
                        loadMVUrl()
                    }
                }
            }
        }
    }

    private fun loadRelatedMV() {
        // 获取歌手的其他MV作为相关推荐
        MVManager.getAllMV(
            area = "",
            order = "最热",
            limit = 10,
            offset = 0
        ) { mvList ->
            runOnMainThread {
                // 过滤掉当前播放的MV
                val filteredList = mvList.filter { it.id != mvid }
                relatedMVAdapter.setData(filteredList)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (binding.videoView.isPlaying) {
            binding.videoView.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.videoView.stopPlayback()
    }

    override fun onBackPressed() {
        // 如果当前是全屏，先退出全屏
        if (resources.configuration.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            super.onBackPressed()
        }
    }
}
