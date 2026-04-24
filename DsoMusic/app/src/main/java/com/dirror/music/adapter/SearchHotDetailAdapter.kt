package com.dirror.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.R
import com.dirror.music.music.netease.SearchFeatures

/**
 * 热搜详情 Adapter
 */
class SearchHotDetailAdapter(
    private val hotList: List<SearchFeatures.HotSearchDetail>
) : RecyclerView.Adapter<SearchHotDetailAdapter.ViewHolder>() {

    private var onItemClick: OnItemClick? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView = view.findViewById(R.id.tvRank)
        val tvSearchWord: TextView = view.findViewById(R.id.tvSearchWord)
        val tvScore: TextView = view.findViewById(R.id.tvScore)
        val tvContent: TextView = view.findViewById(R.id.tvContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_hot_detail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = hotList[position]
        holder.apply {
            tvRank.text = (position + 1).toString()
            tvSearchWord.text = item.searchWord
            tvScore.text = "${item.score}"
            tvContent.text = item.content ?: ""
            
            // 前三名使用不同颜色
            when (position) {
                0 -> tvRank.setTextColor(0xFFFF0000.toInt()) // 红色
                1 -> tvRank.setTextColor(0xFFFF6600.toInt()) // 橙色
                2 -> tvRank.setTextColor(0xFFFFCC00.toInt()) // 黄色
                else -> tvRank.setTextColor(0xFF666666.toInt()) // 灰色
            }
            
            itemView.setOnClickListener {
                onItemClick?.onItemClick(it, position)
            }
        }
    }

    override fun getItemCount(): Int = hotList.size

    fun setOnItemClick(listener: OnItemClick) {
        onItemClick = listener
    }

    interface OnItemClick {
        fun onItemClick(view: View?, position: Int)
    }
}
