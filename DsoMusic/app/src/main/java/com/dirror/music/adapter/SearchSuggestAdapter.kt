package com.dirror.music.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.R

/**
 * 搜索建议 Adapter
 */
class SearchSuggestAdapter : RecyclerView.Adapter<SearchSuggestAdapter.ViewHolder>() {

    private var suggestions = ArrayList<String>()
    private var onItemClick: OnItemClick? = null

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvSuggest: TextView = view.findViewById(R.id.tvSuggest)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_suggest, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val suggest = suggestions[position]
        holder.tvSuggest.text = suggest
        holder.itemView.setOnClickListener {
            onItemClick?.onItemClick(suggest)
        }
    }

    override fun getItemCount(): Int = suggestions.size

    /**
     * 更新搜索建议列表
     */
    fun updateSuggestions(newSuggestions: List<String>) {
        suggestions.clear()
        suggestions.addAll(newSuggestions)
        notifyDataSetChanged()
    }

    /**
     * 清空搜索建议
     */
    fun clearSuggestions() {
        suggestions.clear()
        notifyDataSetChanged()
    }

    fun setOnItemClick(listener: OnItemClick) {
        onItemClick = listener
    }

    interface OnItemClick {
        fun onItemClick(keyword: String)
    }
}
