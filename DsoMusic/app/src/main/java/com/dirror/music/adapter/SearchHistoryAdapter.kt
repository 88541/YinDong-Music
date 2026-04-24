package com.dirror.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.R

/**
 * 搜索历史 Adapter
 */
class SearchHistoryAdapter : RecyclerView.Adapter<SearchHistoryAdapter.ViewHolder>() {

    private var historyList = ArrayList<String>()
    private var onItemClick: OnItemClick? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvHistory: TextView = view.findViewById(R.id.tvHistory)
        val ivDelete: ImageView = view.findViewById(R.id.ivDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val history = historyList[position]
        holder.tvHistory.text = history

        // 点击历史记录进行搜索
        holder.itemView.setOnClickListener {
            onItemClick?.onItemClick(history, position)
        }

        // 点击删除按钮删除单条记录
        holder.ivDelete.setOnClickListener {
            onItemClick?.onDeleteClick(history, position)
        }
    }

    override fun getItemCount(): Int = historyList.size

    /**
     * 更新搜索历史列表
     */
    fun updateHistory(newHistory: List<String>) {
        historyList.clear()
        historyList.addAll(newHistory)
        notifyDataSetChanged()
    }

    /**
     * 获取当前历史列表
     */
    fun getHistoryList(): List<String> = historyList.toList()

    fun setOnItemClick(listener: OnItemClick) {
        onItemClick = listener
    }

    interface OnItemClick {
        fun onItemClick(keyword: String, position: Int)
        fun onDeleteClick(keyword: String, position: Int)
    }
}
