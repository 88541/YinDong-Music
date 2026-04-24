package com.dirror.music.ui.main.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.R
import com.dirror.music.startLocalMusicActivity
import com.dirror.music.ui.activity.DownloadActivity
import com.dirror.music.ui.activity.FloatingLyricsSettingsActivity
import com.dirror.music.ui.activity.PlayHistoryActivity
import com.dirror.music.ui.playlist.SongPlaylistActivity
import com.dirror.music.ui.playlist.TAG_LOCAL_MY_FAVORITE
import com.dirror.music.util.AnimationUtil

class MyFragmentIconAdapter(val context: Context): RecyclerView.Adapter<MyFragmentIconAdapter.ViewHolder>() {

    inner class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val clLocal: ConstraintLayout = view.findViewById(R.id.clLocal)
        val clFavorite: ConstraintLayout = view.findViewById(R.id.clFavorite)
        val clLatest: ConstraintLayout = view.findViewById(R.id.clLatest)
        val clDownload: ConstraintLayout = view.findViewById(R.id.clDownload)
        val clFloatingLyrics: ConstraintLayout = view.findViewById(R.id.clFloatingLyrics)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        LayoutInflater.from(parent.context).inflate(R.layout.recycler_fragment_my_icon, parent, false).apply {
            return ViewHolder(this)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.apply {
            clLocal.setOnClickListener {
                AnimationUtil.click(it)
                startLocalMusicActivity(context)
            }
            // 我喜欢的音乐
            clFavorite.setOnClickListener {
                AnimationUtil.click(it)
                val intent = Intent(context, SongPlaylistActivity::class.java).apply {
                    putExtra(SongPlaylistActivity.EXTRA_TAG, TAG_LOCAL_MY_FAVORITE)
//                    putExtra(PlaylistActivity2.EXTRA_LONG_PLAYLIST_ID, 0L)
//                    putExtra(PlaylistActivity2.EXTRA_INT_TAG, PLAYLIST_TAG_MY_FAVORITE)
                }
                context.startActivity(intent)
            }
            // 播放历史
            clLatest.setOnClickListener {
                AnimationUtil.click(it)
                val intent = Intent(context, PlayHistoryActivity::class.java)
                context.startActivity(intent)
            }
            // 下载管理
            clDownload.setOnClickListener {
                AnimationUtil.click(it)
                val intent = Intent(context, DownloadActivity::class.java)
                context.startActivity(intent)
            }
            // 悬浮歌词设置
            clFloatingLyrics.setOnClickListener {
                AnimationUtil.click(it)
                val intent = Intent(context, FloatingLyricsSettingsActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    override fun getItemCount(): Int {
        return 1
    }

}
