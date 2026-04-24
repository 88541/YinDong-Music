package com.dirror.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.dirror.music.R
import com.dirror.music.music.netease.MVManager

/**
 * MV列表适配器
 */
class MVAdapter(
    private val mvList: MutableList<MVManager.MVData> = mutableListOf(),
    private val onItemClick: (MVManager.MVData) -> Unit
) : RecyclerView.Adapter<MVAdapter.MVViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MVViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mv, parent, false)
        return MVViewHolder(view)
    }

    override fun onBindViewHolder(holder: MVViewHolder, position: Int) {
        val mv = mvList[position]
        holder.bind(mv)
        holder.itemView.setOnClickListener {
            onItemClick(mv)
        }
    }

    override fun getItemCount(): Int = mvList.size

    fun setData(newList: List<MVManager.MVData>) {
        mvList.clear()
        mvList.addAll(newList)
        notifyDataSetChanged()
    }

    fun addData(moreList: List<MVManager.MVData>) {
        val startPosition = mvList.size
        mvList.addAll(moreList)
        notifyItemRangeInserted(startPosition, moreList.size)
    }

    inner class MVViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMVCover: ImageView = itemView.findViewById(R.id.ivMVCover)
        private val tvMVName: TextView = itemView.findViewById(R.id.tvMVName)
        private val tvArtistName: TextView = itemView.findViewById(R.id.tvArtistName)
        private val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        private val tvPlayCount: TextView = itemView.findViewById(R.id.tvPlayCount)
        private val tvPublishTime: TextView = itemView.findViewById(R.id.tvPublishTime)

        fun bind(mv: MVManager.MVData) {
            tvMVName.text = mv.name
            tvArtistName.text = mv.artistName
            tvPublishTime.text = mv.publishTime
            tvDuration.text = formatDuration(mv.duration)
            tvPlayCount.text = formatPlayCount(mv.playCount)

            // 加载封面
            ivMVCover.load(mv.cover) {
                placeholder(R.drawable.ic_launcher_foreground)
                error(R.drawable.ic_launcher_foreground)
            }
        }

        private fun formatDuration(durationMs: Long): String {
            val minutes = durationMs / 60000
            val seconds = (durationMs % 60000) / 1000
            return String.format("%02d:%02d", minutes, seconds)
        }

        private fun formatPlayCount(count: Long): String {
            return when {
                count >= 100000000 -> String.format("%.1f亿", count / 100000000.0)
                count >= 10000 -> String.format("%.1f万", count / 10000.0)
                else -> count.toString()
            }
        }
    }
}
