package com.dirror.music.ui.activity

import android.app.DownloadManager
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.databinding.ActivityDownloadBinding
import com.dirror.music.ui.base.BaseActivity
import java.io.File

/**
 * 下载管理页面
 */
class DownloadActivity : BaseActivity() {

    private lateinit var binding: ActivityDownloadBinding
    private lateinit var downloadAdapter: DownloadAdapter
    private val downloadList = mutableListOf<DownloadItem>()

    override fun initBinding() {
        binding = ActivityDownloadBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun initView() {
        // 设置标题栏
        binding.titleBarLayout.setTitleBarText("下载管理")

        // 设置RecyclerView
        downloadAdapter = DownloadAdapter(downloadList)
        binding.rvDownloadList.layoutManager = LinearLayoutManager(this)
        binding.rvDownloadList.adapter = downloadAdapter

        // 加载下载记录
        loadDownloadHistory()
    }

    private fun loadDownloadHistory() {
        downloadList.clear()
        
        // 从系统下载管理器获取下载记录
        val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query()
        query.setFilterByStatus(
            DownloadManager.STATUS_SUCCESSFUL or 
            DownloadManager.STATUS_PENDING or 
            DownloadManager.STATUS_RUNNING or 
            DownloadManager.STATUS_PAUSED
        )
        
        val cursor = downloadManager.query(query)
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
            val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
            val descriptionIndex = cursor.getColumnIndex(DownloadManager.COLUMN_DESCRIPTION)
            val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
            val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
            
            do {
                val id = cursor.getLong(idIndex)
                val title = cursor.getString(titleIndex) ?: "未知文件"
                val description = cursor.getString(descriptionIndex) ?: ""
                val status = cursor.getInt(statusIndex)
                val localUri = cursor.getString(localUriIndex) ?: ""
                
                val statusText = when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> "已完成"
                    DownloadManager.STATUS_PENDING -> "等待中"
                    DownloadManager.STATUS_RUNNING -> "下载中"
                    DownloadManager.STATUS_PAUSED -> "已暂停"
                    DownloadManager.STATUS_FAILED -> "下载失败"
                    else -> "未知状态"
                }
                
                downloadList.add(DownloadItem(id, title, description, statusText, localUri))
            } while (cursor.moveToNext())
            cursor.close()
        }
        
        // 同时扫描音乐文件夹中的下载文件
        scanMusicFolder()
        
        // 更新UI
        if (downloadList.isEmpty()) {
            showEmptyState()
        } else {
            showDownloadList()
            downloadAdapter.notifyDataSetChanged()
        }
    }

    private fun scanMusicFolder() {
        // 扫描标准音乐目录
        val musicDir = File(android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_MUSIC), "")
        
        if (musicDir.exists() && musicDir.isDirectory) {
            val files = musicDir.listFiles { file ->
                file.isFile && (file.name.endsWith(".mp3", true) || 
                               file.name.endsWith(".flac", true) ||
                               file.name.endsWith(".m4a", true) ||
                               file.name.endsWith(".wav", true))
            }
            
            files?.forEach { file ->
                // 检查是否已经在列表中
                val exists = downloadList.any { it.localUri.contains(file.name) }
                if (!exists) {
                    downloadList.add(DownloadItem(
                        id = System.currentTimeMillis(),
                        title = file.nameWithoutExtension,
                        description = "",
                        status = "已下载",
                        localUri = file.absolutePath
                    ))
                }
            }
        }
    }

    private fun showEmptyState() {
        binding.rvDownloadList.visibility = View.GONE
        binding.llEmpty.visibility = View.VISIBLE
    }

    private fun showDownloadList() {
        binding.rvDownloadList.visibility = View.VISIBLE
        binding.llEmpty.visibility = View.GONE
    }

    // 数据类
    data class DownloadItem(
        val id: Long,
        val title: String,
        val description: String,
        val status: String,
        val localUri: String
    )

    // 适配器
    inner class DownloadAdapter(
        private val items: List<DownloadItem>
    ) : RecyclerView.Adapter<DownloadAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: ImageView = itemView.findViewById(com.dirror.music.R.id.ivIcon)
            val tvTitle: TextView = itemView.findViewById(com.dirror.music.R.id.tvTitle)
            val tvStatus: TextView = itemView.findViewById(com.dirror.music.R.id.tvStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(com.dirror.music.R.layout.item_download, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvTitle.text = item.title
            holder.tvStatus.text = item.status
            holder.ivIcon.setImageResource(com.dirror.music.R.drawable.ic_download)
        }

        override fun getItemCount(): Int = items.size
    }
}
