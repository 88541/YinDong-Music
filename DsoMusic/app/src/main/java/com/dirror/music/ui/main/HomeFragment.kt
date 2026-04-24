package com.dirror.music.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.dirror.music.App
import com.dirror.music.adapter.NewSongAdapter
import com.dirror.music.adapter.PlaylistRecommendAdapter
import com.dirror.music.adapter.TopListAdapter
import com.dirror.music.databinding.FragmentHomeBinding
import com.dirror.music.foyou.sentence.Sentence
import com.dirror.music.manager.User
import com.dirror.music.music.netease.DiscoverApi
import com.dirror.music.music.netease.PlaylistRecommend
import com.dirror.music.music.netease.TopList
import com.dirror.music.music.netease.data.TopListData
import com.dirror.music.ui.activity.MVListActivity
import com.dirror.music.ui.activity.PlaylistRecommendActivity
import com.dirror.music.ui.activity.RecommendActivity
import com.dirror.music.ui.activity.TopListActivity
import com.dirror.music.ui.base.BaseFragment
import com.dirror.music.ui.main.viewmodel.MainViewModel
import com.dirror.music.ui.playlist.TAG_NETEASE
import com.dirror.music.util.*

class HomeFragment : BaseFragment(){

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun initView() {
        update()
    }

    /**
     * 刷新整个页面
     */
    private fun update() {
        // Banner
        // initBanner()
        // 推荐歌单
        refreshPlaylistRecommend()
        // 新歌速递
        refreshNewSongs()
        // 更改句子
        changeSentence()
    }

    override fun initListener() {
        binding.includeFoyou.root.setOnClickListener {
            changeSentence()
        }

        // 歌单推荐更多按钮
        binding.tvPlaylistRecommendMore.setOnClickListener {
            // 跳转到歌单推荐页面
            val intent = Intent(this.context, PlaylistRecommendActivity::class.java)
            startActivity(intent)
        }

        // MV入口点击
        binding.clMV.setOnClickListener {
            val intent = Intent(this.context, MVListActivity::class.java)
            startActivity(intent)
        }

        // 排行榜入口点击
        binding.clTopListEntry.setOnClickListener {
            val intent = Intent(this.context, TopListActivity::class.java)
            startActivity(intent)
        }
    }

    override fun initObserver() {
        with(mainViewModel) {
            statusBarHeight.observe(viewLifecycleOwner) {
//                (binding.llMain.layoutParams as FrameLayout.LayoutParams).apply {
//                    topMargin = it
//                }
            }
            sentenceVisibility.observe(viewLifecycleOwner) {
                if (it) {
                    binding.includeFoyou.root.visibility = View.VISIBLE
                    binding.tvFoyou.visibility = View.VISIBLE
                } else {
                    binding.includeFoyou.root.visibility = View.GONE
                    binding.tvFoyou.visibility = View.GONE
                }
            }
        }

    }

    private fun changeSentence() {
        binding.includeFoyou.tvText.alpha = 0f
        binding.includeFoyou.tvAuthor.alpha = 0f
        binding.includeFoyou.tvSource.alpha = 0f
        Sentence.getSentence {
            runOnMainThread {
                binding.includeFoyou.tvText.text = it.text
                binding.includeFoyou.tvAuthor.text = it.author
                binding.includeFoyou.tvSource.text = it.source
                AnimationUtil.fadeIn(binding.includeFoyou.tvText, 1000, false)
                AnimationUtil.fadeIn(binding.includeFoyou.tvAuthor, 1000, false)
                AnimationUtil.fadeIn(binding.includeFoyou.tvSource, 1000, false)
            }
        }
    }

    private fun refreshPlaylistRecommend() {
        DiscoverApi.getRecommendPlaylists(requireContext(), 16, { playlists ->
            binding.rvPlaylistRecommend.layoutManager = GridLayoutManager(context, 2, GridLayoutManager.HORIZONTAL, false)
            val playlistData = ArrayList<PlaylistRecommend.PlaylistRecommendDataResult>()
            playlists.forEach {
                playlistData.add(PlaylistRecommend.PlaylistRecommendDataResult(it.id, it.picUrl, it.name, it.playCount))
            }
            binding.rvPlaylistRecommend.adapter = PlaylistRecommendAdapter(playlistData)
        }, {
            // 失败处理
        })
    }

    private fun refreshNewSongs() {
        DiscoverApi.getNewSongs(requireContext(), 12, { songs ->
            binding.rvNewSong.layoutManager = GridLayoutManager(this.context, 2)
            binding.rvNewSong.adapter = NewSongAdapter(songs as ArrayList)
        }, {
            // 失败处理
        })
    }

}
