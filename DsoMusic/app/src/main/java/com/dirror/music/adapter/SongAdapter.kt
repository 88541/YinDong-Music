package com.dirror.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.size.ViewSizeResolver
import coil.transform.RoundedCornersTransformation
import android.util.Log
import com.dirror.music.App.Companion.mmkv
import com.dirror.music.R
import com.dirror.music.music.standard.data.*
import com.dirror.music.service.playMusic
import com.dirror.music.util.*
import com.dirror.music.util.parse
import com.dso.ext.toArrayList

/**
 * 歌曲适配器
 * @author Moriafly
 */
class SongAdapter(
    private val itemMenuClickedListener: (StandardSongData) -> Unit
) : ListAdapter<StandardSongData, RecyclerView.ViewHolder>(DiffCallback) {

    companion object {
        private const val TYPE_SONG = 0
        private const val TYPE_LOAD_MORE = 1
    }

    private var showLoadMore = false
    private var onLoadMoreClickListener: (() -> Unit)? = null

    inner class ViewHolder(view: View, itemMenuClickedListener: (StandardSongData) -> Unit) : RecyclerView.ViewHolder(view) {
        val clSong: ConstraintLayout = view.findViewById(R.id.clSong)
        val ivCover: ImageView = view.findViewById(R.id.ivCover)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvSub: TextView = view.findViewById(R.id.tvSub)
        private val ivMenu: ImageView = view.findViewById(R.id.ivMenu)
        val ivTag: ImageView = view.findViewById(R.id.ivTag)

        val isAnimation = mmkv.decodeBool(Config.PLAYLIST_SCROLL_ANIMATION, true)

        var songData: StandardSongData? = null

        init {
            ivMenu.setOnClickListener {
                songData?.let { it1 -> itemMenuClickedListener(it1) }
            }
            clSong.setOnLongClickListener {
                songData?.let { it1 -> itemMenuClickedListener(it1) }
                return@setOnLongClickListener true
            }
        }

        fun cancelAnim() {
            itemView.clearAnimation()
        }

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
        return if (showLoadMore && position == currentList.size) {
            TYPE_LOAD_MORE
        } else {
            TYPE_SONG
        }
    }

    override fun getItemCount(): Int {
        return if (showLoadMore) {
            currentList.size + 1
        } else {
            currentList.size
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_LOAD_MORE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_load_more, parent, false)
                LoadMoreViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_song, parent, false)
                ViewHolder(view, itemMenuClickedListener)
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        super.onViewDetachedFromWindow(holder)
        if (holder is ViewHolder) {
            holder.cancelAnim()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ViewHolder -> {
                val song = getItem(position)
                with(holder) {
                    songData = song

                    if (song.neteaseInfo?.pl == 0) {
                        holder.tvTitle.alpha = 0.25f
                        holder.tvSub.alpha = 0.25f
                    } else {
                        holder.tvTitle.alpha = 1f
                        holder.tvSub.alpha = 1f
                    }

                    if (song.quality() == SONG_QUALITY_HQ) {
                        holder.ivTag.visibility = View.VISIBLE
                    } else {
                        holder.ivTag.visibility = View.GONE
                    }

                    val imageUrl = when (song.source) {
                        SOURCE_NETEASE -> {
                            if (song.imageUrl == "https://p2.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"
                                || song.imageUrl == "https://p1.music.126.net/6y-UleORITEDbvrOLV0Q8A==/5639395138885805.jpg"
                            ) {
                                ""
                                // "$API_FCZBL_VIP/?type=cover&id=${song.id}"
                            } else {
                                val neteaseUrl = "${song.imageUrl}?param=${100}y${100}"
                                // loge(neteaseUrl, "NeteaseUrl")
                                neteaseUrl
                            }
                        }
                        SOURCE_QQ -> {
                            "https://y.gtimg.cn/music/photo_new/T002R300x300M000${song.imageUrl}.jpg?max_age=2592000"
                        }
                        else -> song.imageUrl
                    }

                    ivCover.load(imageUrl) {
                        transformations(RoundedCornersTransformation(dp2px(6f)))
                        size(ViewSizeResolver(ivCover))
                        error(R.drawable.ic_song_cover)
                        crossfade(300)
                    }

                    tvTitle.text = song.name
                    val artist = song.artists?.parse()
                    tvSub.text = if (artist.isNullOrEmpty()) {
                        "未知"
                    } else {
                        artist
                    }
                    // 点击项目 - 直接播放，不检查 pl 值（让播放服务处理）
                    clSong.setOnClickListener {
                        Log.d("SongAdapter", "点击歌曲: ${song.name}, id=${song.id}")
                        playMusic(it.context, song, currentList.toArrayList())
                    }
                    if (isAnimation) {
                        setAnimation(holder.itemView, position)
                    }
                }
            }
            is LoadMoreViewHolder -> {
                // 加载更多按钮不需要绑定数据
            }
        }
    }

    /**
     * 设置是否显示加载更多按钮
     */
    fun setShowLoadMore(show: Boolean) {
        if (showLoadMore != show) {
            showLoadMore = show
            if (show) {
                notifyItemInserted(currentList.size)
            } else {
                notifyItemRemoved(currentList.size)
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
     * 播放第一首歌
     */
    fun playFirst() {
        if (currentList.isNotEmpty()) {
            playMusic(null, getItem(0), currentList.toArrayList(), true)
        }
    }

    /**
     * Here is the key method to apply the animation
     */
    private fun setAnimation(viewToAnimate: View, position: Int) {
        // If the bound view wasn't previously displayed on screen, it's animated
        val animation: Animation =
            AnimationUtils.loadAnimation(viewToAnimate.context, R.anim.anim_recycle_item)
        viewToAnimate.startAnimation(animation)
    }

    object DiffCallback : DiffUtil.ItemCallback<StandardSongData>() {
        override fun areItemsTheSame(oldItem: StandardSongData, newItem: StandardSongData): Boolean {
            return oldItem.source == newItem.source && oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StandardSongData, newItem: StandardSongData): Boolean {
            return oldItem == newItem
        }
    }

}
