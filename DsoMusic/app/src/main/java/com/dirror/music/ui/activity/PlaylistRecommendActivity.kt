package com.dirror.music.ui.activity

import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import com.dirror.music.adapter.PlaylistRecommendAdapter
import com.dirror.music.databinding.ActivityPlaylistRecommendBinding
import com.dirror.music.music.netease.DiscoverApi
import com.dirror.music.music.netease.PlaylistRecommend
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.util.runOnMainThread

/**
 * 歌单推荐页面
 */
class PlaylistRecommendActivity : BaseActivity() {

    private lateinit var binding: ActivityPlaylistRecommendBinding

    override fun initBinding() {
        binding = ActivityPlaylistRecommendBinding.inflate(layoutInflater)
        miniPlayer = binding.miniPlayer
        setContentView(binding.root)
    }

    override fun initView() {
        binding.titleBarLayout.setTitleBarText("歌单推荐")
        loadPlaylistRecommend()
    }

    private fun loadPlaylistRecommend() {
        DiscoverApi.getRecommendPlaylists(this, 100, { playlists ->
            runOnMainThread {
                binding.rvPlaylist.layoutManager = GridLayoutManager(this, 2)
                val playlistData = ArrayList<PlaylistRecommend.PlaylistRecommendDataResult>()
                playlists.forEach {
                    playlistData.add(PlaylistRecommend.PlaylistRecommendDataResult(it.id, it.picUrl, it.name, it.playCount))
                }
                binding.rvPlaylist.adapter = PlaylistRecommendAdapter(playlistData)
            }
        }, {
            // 失败处理
        })
    }
}
