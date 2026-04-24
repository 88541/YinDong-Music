package com.dirror.music.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.ViewSizeResolver
import coil.transform.CircleCropTransformation
import com.dirror.music.App
import com.dirror.music.R
import com.dirror.music.music.netease.CommentManager
import com.dirror.music.util.msTimeToFormatDate

/**
 * 评论 Adapter（支持加载更多）
 */
class CommentAdapter(
    private val activity: Activity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_COMMENT = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private val comments = ArrayList<CommentManager.Comment>()
    private var showLoadMore = false
    private var onLoadMoreClickListener: (() -> Unit)? = null

    inner class CommentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvLikedCount: TextView = view.findViewById(R.id.tvLikedCount)
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val tvTime: TextView = view.findViewById(R.id.tvTime)
    }

    inner class LoadMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvLoadMore: TextView = view.findViewById(R.id.tvLoadMore)

        init {
            tvLoadMore.setOnClickListener {
                onLoadMoreClickListener?.invoke()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (showLoadMore && position == comments.size) {
            TYPE_LOAD_MORE
        } else {
            TYPE_COMMENT
        }
    }

    override fun getItemCount(): Int {
        return if (showLoadMore) {
            comments.size + 1
        } else {
            comments.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_comment, parent, false)
                CommentViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CommentViewHolder -> {
                val comment = comments[position]
                holder.apply {
                    tvName.text = comment.user.nickname
                    tvContent.text = comment.content
                    tvLikedCount.text = comment.likedCount.toString()
                    tvTime.text = msTimeToFormatDate(comment.time)
                    ivCover.load(comment.user.avatarUrl) {
                        transformations(CircleCropTransformation())
                        size(ViewSizeResolver(ivCover))
                        crossfade(300)
                    }

                    ivCover.setOnClickListener {
                        App.activityManager.startUserActivity(activity, comment.user.userId)
                    }

                    tvName.setOnClickListener {
                        App.activityManager.startUserActivity(activity, comment.user.userId)
                    }
                }
            }
            is LoadMoreViewHolder -> {
                // 加载更多按钮不需要绑定数据
            }
        }
    }

    /**
     * 设置评论数据
     */
    fun setComments(newComments: List<CommentManager.Comment>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
    }

    /**
     * 添加评论数据（用于加载更多）
     */
    fun addComments(newComments: List<CommentManager.Comment>) {
        val oldSize = comments.size
        comments.addAll(newComments)
        notifyItemRangeInserted(oldSize, newComments.size)
    }

    /**
     * 设置是否显示加载更多按钮
     */
    fun setShowLoadMore(show: Boolean) {
        if (showLoadMore != show) {
            showLoadMore = show
            if (show) {
                notifyItemInserted(comments.size)
            } else {
                notifyItemRemoved(comments.size)
            }
        }
    }

    /**
     * 设置加载更多点击监听器
     */
    fun setOnLoadMoreClickListener(listener: () -> Unit) {
        onLoadMoreClickListener = listener
    }

    /**
     * 获取当前评论列表
     */
    fun getComments(): List<CommentManager.Comment> {
        return comments
    }
}
