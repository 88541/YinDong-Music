package com.dirror.music.ui.activity

import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.dirror.music.R
import com.dirror.music.adapter.CommentAdapter
import com.dirror.music.databinding.ActivityCommentBinding
import com.dirror.music.music.netease.CommentManager
import com.dirror.music.music.standard.data.SOURCE_NETEASE
import com.dirror.music.ui.base.SlideBackActivity
import com.dirror.music.util.runOnMainThread
import com.dirror.music.util.toast

class CommentActivity : SlideBackActivity() {

    companion object {
        const val EXTRA_INT_SOURCE = "extra_int_source"
        const val EXTRA_STRING_ID = "extra_string_id"
    }

    private lateinit var binding: ActivityCommentBinding
    private lateinit var adapter: CommentAdapter

    private lateinit var songId: String
    private var source: Int = SOURCE_NETEASE

    // 分页相关
    private var currentOffset = 0
    private val limit = 20
    private var hasMore = true
    private var isLoading = false
    private var totalComments: Long = 0

    override fun initBinding() {
        binding = ActivityCommentBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initData() {
        songId = intent.getStringExtra(EXTRA_STRING_ID) ?: ""
        source = intent.getIntExtra(EXTRA_INT_SOURCE, SOURCE_NETEASE)

        if (songId.isEmpty()) {
            toast("歌曲ID无效")
            finish()
            return
        }

        // 初始化适配器
        adapter = CommentAdapter(this)
        adapter.setOnLoadMoreClickListener {
            loadMoreComments()
        }

        binding.rvComment.layoutManager = LinearLayoutManager(this)
        binding.rvComment.adapter = adapter

        // 加载评论
        loadComments(true)
    }

    override fun initView() {
        bindSlide(this, binding.clBase)

        // 设置标题
        binding.tvTitle.text = getString(R.string.comment)

        var rvPlaylistScrollY = 0
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            binding.rvComment.setOnScrollChangeListener { _, _, _, _, oldScrollY ->
                rvPlaylistScrollY += oldScrollY
                slideBackEnabled = rvPlaylistScrollY == 0
            }
        }
    }

    override fun initListener() {
        binding.btnSendComment.setOnClickListener {
            val content = binding.etCommentContent.text.toString()
            if (content.isNotEmpty()) {
                sendComment(content)
            } else {
                toast("请输入评论内容")
            }
        }
    }

    /**
     * 加载评论
     * @param isRefresh 是否是刷新（true=刷新，false=加载更多）
     */
    private fun loadComments(isRefresh: Boolean) {
        if (isLoading) return
        isLoading = true

        if (isRefresh) {
            currentOffset = 0
            hasMore = true
            binding.progressBar.visibility = View.VISIBLE
        }

        when (source) {
            SOURCE_NETEASE -> {
                CommentManager.getComments(
                    id = songId,
                    limit = limit,
                    offset = currentOffset
                ) { result ->
                    runOnMainThread {
                        isLoading = false
                        binding.progressBar.visibility = View.GONE

                        if (result.code == 200) {
                            totalComments = result.total

                            if (isRefresh) {
                                // 刷新，设置新数据
                                adapter.setComments(result.comments)
                                if (result.comments.isEmpty()) {
                                    toast("暂无评论")
                                }
                            } else {
                                // 加载更多，追加数据
                                adapter.addComments(result.comments)
                            }

                            // 更新分页状态
                            currentOffset += result.comments.size
                            hasMore = result.comments.size >= limit && currentOffset < totalComments

                            // 显示或隐藏加载更多按钮
                            adapter.setShowLoadMore(hasMore)

                            // 更新标题显示评论数量
                            binding.tvTitle.text = "${getString(R.string.comment)} (${totalComments})"
                        } else {
                            toast("获取评论失败")
                        }
                    }
                }
            }
            else -> {
                isLoading = false
                binding.progressBar.visibility = View.GONE
                toast("暂不支持该音源的评论")
            }
        }
    }

    /**
     * 加载更多评论
     */
    private fun loadMoreComments() {
        if (!hasMore || isLoading) return
        loadComments(false)
    }

    /**
     * 发送评论
     */
    private fun sendComment(content: String) {
        // 暂时使用原有的发送评论方式
        // 后续可以迁移到新的API
        toast("评论功能开发中")
        binding.etCommentContent.setText("")
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.anim_no_anim,
            R.anim.anim_slide_exit_bottom
        )
    }
}
