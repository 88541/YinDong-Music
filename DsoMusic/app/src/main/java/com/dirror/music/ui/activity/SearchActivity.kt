package com.dirror.music.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.dirror.music.App
import com.dirror.music.App.Companion.mmkv
import com.dirror.music.R
import com.dirror.music.adapter.*
import com.dirror.music.data.SearchType
import com.dirror.music.databinding.ActivitySearchBinding
import com.dirror.music.music.netease.NewSearchSong
import com.dirror.music.music.netease.SearchFeatures
import com.dirror.music.music.standard.data.StandardAlbum
import com.dirror.music.music.standard.data.StandardPlaylist
import com.dirror.music.music.standard.data.StandardSinger
import com.dirror.music.music.standard.data.StandardSongData
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.ui.dialog.SongMenuDialog
import com.dirror.music.ui.playlist.SongPlaylistActivity
import com.dirror.music.ui.playlist.TAG_KUWO
import com.dirror.music.ui.playlist.TAG_NETEASE
import com.dirror.music.ui.viewmodel.SearchViewModel
import com.dirror.music.util.*
import com.dirror.music.util.asDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 搜索界面
 */
class SearchActivity : BaseActivity() {

    companion object {
        private val TAG = "SearchActivity"
    }

    private lateinit var binding: ActivitySearchBinding

    private val searchViewModel: SearchViewModel by viewModels()

    private var realKeyWord = ""

    private var searchType: SearchType

    // 分页相关
    private var currentOffset = 0
    private val limit = 30
    private var hasMore = true
    private var isLoading = false
    private var currentSongList = ArrayList<StandardSongData>()

    private lateinit var searchHistoryManager: SearchHistoryManager
    private lateinit var searchHistoryAdapter: SearchHistoryAdapter

    init {
        val typeStr = mmkv.decodeString(Config.SEARCH_TYPE, SearchType.SINGLE.toString())!!
        searchType = SearchType.valueOf(typeStr)
    }

    override fun initData() {
        val typeStr = mmkv.decodeString(Config.SEARCH_TYPE, SearchType.SINGLE.toString())!!
        searchType = SearchType.valueOf(typeStr)

        // 初始化搜索历史管理器
        searchHistoryManager = SearchHistoryManager(this)

        // 初始化搜索建议适配器
        initSearchSuggestAdapter()

        // 初始化搜索历史适配器
        initSearchHistoryAdapter()
    }

    /**
     * 初始化搜索建议适配器
     */
    private fun initSearchSuggestAdapter() {
        val suggestAdapter = SearchSuggestAdapter()
        suggestAdapter.setOnItemClick(object : SearchSuggestAdapter.OnItemClick {
            override fun onItemClick(keyword: String) {
                binding.etSearch.setText(keyword)
                binding.etSearch.setSelection(keyword.length)
                hideSearchSuggest()
                search()
            }
        })

        binding.rvSearchSuggest.layoutManager = LinearLayoutManager(this)
        binding.rvSearchSuggest.adapter = suggestAdapter
    }

    /**
     * 显示搜索建议
     */
    private fun showSearchSuggest(suggestions: List<String>) {
        val adapter = binding.rvSearchSuggest.adapter as? SearchSuggestAdapter
        adapter?.updateSuggestions(suggestions)
        binding.rvSearchSuggest.visibility = View.VISIBLE
        binding.clPanel.visibility = View.GONE
    }

    /**
     * 隐藏搜索建议
     */
    private fun hideSearchSuggest() {
        binding.rvSearchSuggest.visibility = View.GONE
    }

    /**
     * 初始化搜索历史适配器
     */
    private fun initSearchHistoryAdapter() {
        searchHistoryAdapter = SearchHistoryAdapter()
        searchHistoryAdapter.setOnItemClick(object : SearchHistoryAdapter.OnItemClick {
            override fun onItemClick(keyword: String, position: Int) {
                // 点击历史记录，填充搜索框并搜索
                binding.etSearch.setText(keyword)
                binding.etSearch.setSelection(keyword.length)
                search()
            }

            override fun onDeleteClick(keyword: String, position: Int) {
                // 删除单条历史记录
                searchHistoryManager.removeHistory(keyword)
                refreshSearchHistory()
            }
        })

        binding.rvSearchHistory.layoutManager = LinearLayoutManager(this)
        binding.rvSearchHistory.adapter = searchHistoryAdapter

        // 加载搜索历史
        refreshSearchHistory()
    }

    /**
     * 刷新搜索历史显示
     */
    private fun refreshSearchHistory() {
        val history = searchHistoryManager.getHistory()
        searchHistoryAdapter.updateHistory(history)

        // 根据是否有历史记录显示或隐藏历史区域
        if (history.isEmpty()) {
            binding.clSearchHistoryHeader.visibility = View.GONE
            binding.rvSearchHistory.visibility = View.GONE
        } else {
            binding.clSearchHistoryHeader.visibility = View.VISIBLE
            binding.rvSearchHistory.visibility = View.VISIBLE
        }
    }

    override fun initBinding() {
        binding = ActivitySearchBinding.inflate(layoutInflater)
        miniPlayer = binding.miniPlayer
        setContentView(binding.root)
    }

    override fun initView() {
        // 获取焦点
        binding.etSearch.apply {
            isFocusable = true
            isFocusableInTouchMode = true
            requestFocus()
        }

        // 加载搜索相关数据
        loadSearchFeatures()
    }

    /**
     * 加载搜索功能数据（默认关键词、热门搜索等）
     */
    private fun loadSearchFeatures() {
        // 获取默认搜索关键词
        SearchFeatures.getDefaultKeyword { keyword ->
            runOnMainThread {
                if (!keyword.isNullOrEmpty()) {
                    binding.etSearch.hint = keyword
                    realKeyWord = keyword
                }
            }
        }

        // 获取热搜列表（详细）
        SearchFeatures.getHotSearchDetail { hotList ->
            runOnMainThread {
                if (hotList.isNotEmpty()) {
                    binding.rvSearchHot.layoutManager = LinearLayoutManager(this)
                    val searchHotAdapter = SearchHotDetailAdapter(hotList)
                    searchHotAdapter.setOnItemClick(object : SearchHotDetailAdapter.OnItemClick {
                        override fun onItemClick(view: View?, position: Int) {
                            val searchWord = hotList[position].searchWord
                            binding.etSearch.setText(searchWord)
                            binding.etSearch.setSelection(searchWord.length)
                            search()
                        }
                    })
                    binding.rvSearchHot.adapter = searchHotAdapter
                }
            }
        }

        // 添加搜索框文本变化监听，实现搜索建议
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val keywords = s?.toString()
                if (!keywords.isNullOrEmpty() && keywords.length >= 1) {
                    // 获取搜索建议
                    SearchFeatures.getSearchSuggest(keywords) { suggestions ->
                        runOnMainThread {
                            if (suggestions.isNotEmpty()) {
                                showSearchSuggest(suggestions)
                            } else {
                                hideSearchSuggest()
                            }
                        }
                    }
                } else {
                    hideSearchSuggest()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun initListener() {
        binding.apply {
            // ivBack
            ivBack.setOnClickListener {
                if (clPanel.visibility == View.VISIBLE) {
                    finish()
                } else {
                    clPanel.visibility = View.VISIBLE
                }
            }
            // 搜索
            btnSearch.setOnClickListener { search() }

            // 清空所有搜索历史
            ivClearHistory.setOnClickListener {
                searchHistoryManager.clearHistory()
                refreshSearchHistory()
                toast("已清空搜索历史")
            }

            // 网易云
            clNetease.setOnClickListener {
                changeSearchEngine(SearchViewModel.ENGINE_NETEASE)
            }
            // 酷我
            clKuwo.setOnClickListener {
                changeSearchEngine(SearchViewModel.ENGINE_KUWO)
//                toast("酷我音源暂只支持精确搜索，需要填入完整歌曲名")
            }
        }

        // 搜索框
        binding.etSearch.apply {
            setOnEditorActionListener { _, p1, _ ->
                if (p1 == EditorInfo.IME_ACTION_SEARCH) { // 软键盘点击了搜索
                    search()
                }
                false
            }

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
                override fun afterTextChanged(s: Editable) {
                    if (binding.etSearch.text.toString() != "") {
                        binding.ivClear.visibility = View.VISIBLE // 有文字，显示清楚按钮
                    } else {
                        binding.ivClear.visibility = View.INVISIBLE // 隐藏
                        hideSearchSuggest() // 清空时也隐藏搜索建议
                    }
                }
            })
        }


        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
        }

    }

    override fun initObserver() {
        searchViewModel.searchEngine.observe(this) {
            binding.apply {
                clNetease.background =
                    R.drawable.background_transparency.asDrawable(this@SearchActivity)
                clKuwo.background =
                    R.drawable.background_transparency.asDrawable(this@SearchActivity)
            }
            when (it) {
                SearchViewModel.ENGINE_NETEASE -> {
                    binding.clNetease.background =
                        ContextCompat.getDrawable(this@SearchActivity, R.drawable.bg_edit_text)
                }
                SearchViewModel.ENGINE_KUWO -> {
                    binding.clKuwo.background =
                        ContextCompat.getDrawable(this@SearchActivity, R.drawable.bg_edit_text)
                }
            }
        }
    }

    /**
     * 搜索音乐
     */
    private fun search() {
        // 关闭软键盘
        val inputMethodManager: InputMethodManager =
            this.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(this.window?.decorView?.windowToken, 0)

        var keywords = binding.etSearch.text.toString()
        // 内部酷我
        if (keywords.startsWith("。")) {
            keywords.replace("。", "")
            searchViewModel.searchEngine.value = SearchViewModel.ENGINE_KUWO
        }
        if (keywords == "") {
            keywords = realKeyWord
            binding.etSearch.setText(keywords)
            binding.etSearch.setSelection(keywords.length)
        }
        if (keywords != "") {
            // 保存搜索历史
            searchHistoryManager.addHistory(keywords)
            refreshSearchHistory()

            // 重置分页状态
            currentOffset = 0
            hasMore = true
            currentSongList.clear()
            
            performSearch(keywords, true)
        }
    }

    /**
     * 执行搜索
     * @param keywords 关键词
     * @param isNewSearch 是否是新搜索（true=新搜索，false=加载更多）
     */
    private fun performSearch(keywords: String, isNewSearch: Boolean) {
        if (isLoading) return
        isLoading = true

        if (isNewSearch) {
            // 先隐藏搜索面板、搜索建议和结果列表，显示加载状态
            binding.clPanel.visibility = View.GONE
            binding.rvSearchSuggest.visibility = View.GONE
            binding.rvPlaylist.visibility = View.GONE
            binding.loadingContainer.visibility = View.VISIBLE
        } else {
            // 加载更多时，按钮状态由适配器内部处理
        }

        when (searchViewModel.searchEngine.value) {
            SearchViewModel.ENGINE_NETEASE -> {
                // 使用新的网易云搜索API
                NewSearchSong.search(
                    keywords = keywords,
                    offset = currentOffset,
                    limit = limit,
                    success = { songs ->
                        runOnMainThread {
                            isLoading = false
                            binding.loadingContainer.visibility = View.GONE

                            if (songs.isNotEmpty()) {
                                if (isNewSearch) {
                                    // 新搜索，初始化列表
                                    currentSongList = songs
                                    initRecycleView(currentSongList)
                                } else {
                                    // 加载更多，追加到列表
                                    val oldSize = currentSongList.size
                                    currentSongList.addAll(songs)
                                    (binding.rvPlaylist.adapter as? SongAdapter)?.submitList(currentSongList)
                                }

                                // 更新分页状态
                                currentOffset += songs.size
                                hasMore = songs.size >= limit

                                // 显示或隐藏加载更多按钮（通过适配器）
                                (binding.rvPlaylist.adapter as? SongAdapter)?.setShowLoadMore(hasMore)
                            } else {
                                hasMore = false
                                (binding.rvPlaylist.adapter as? SongAdapter)?.setShowLoadMore(false)
                                if (isNewSearch) {
                                    toast("未找到相关歌曲")
                                } else {
                                    toast("没有更多歌曲了")
                                }
                            }
                        }
                    },
                    onCoverUpdated = { index ->
                        // 封面更新时刷新对应位置
                        runOnMainThread {
                            binding.rvPlaylist.adapter?.notifyItemChanged(index)
                        }
                    }
                )
            }
            SearchViewModel.ENGINE_KUWO -> {
                com.dirror.music.music.kuwo.SearchSong.search(keywords, searchType) {
                    runOnMainThread {
                        isLoading = false
                        binding.loadingContainer.visibility = View.GONE
                        when (searchType) {
                            SearchType.SINGLE -> initRecycleView(it.songs)
                            SearchType.PLAYLIST -> initPlaylist(it.playlist, TAG_KUWO)
                            else -> {
                                toast("酷我仅支持搜索单曲与歌单")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 加载更多歌曲
     */
    private fun loadMore() {
        if (!hasMore || isLoading) return
        val keywords = binding.etSearch.text.toString()
        if (keywords.isNotEmpty()) {
            performSearch(keywords, false)
        }
    }

    private fun initSingers(singers: List<StandardSinger>) {
        binding.loadingContainer.visibility = View.GONE
        binding.rvSearchSuggest.visibility = View.GONE
        binding.clPanel.visibility = View.GONE
        binding.rvPlaylist.layoutManager = LinearLayoutManager(this)
        binding.rvPlaylist.adapter = SingerAdapter {
            val intent = Intent(this@SearchActivity, SongPlaylistActivity::class.java)
            intent.putExtra(SongPlaylistActivity.EXTRA_TAG, TAG_NETEASE)
            intent.putExtra(SongPlaylistActivity.EXTRA_ID, it.id.toString())
            intent.putExtra(SongPlaylistActivity.EXTRA_TYPE, SearchType.SINGER)
            startActivity(intent)
        }.apply {
            submitList(singers)
        }
    }

    private fun initRecycleView(songList: List<StandardSongData>) {
        binding.loadingContainer.visibility = View.GONE
        binding.rvSearchSuggest.visibility = View.GONE
        binding.rvPlaylist.visibility = View.VISIBLE
        binding.clPanel.visibility = View.GONE
        binding.rvPlaylist.layoutManager = LinearLayoutManager(this)
        
        val adapter = SongAdapter() {
            SongMenuDialog(this, this, it) {
                toast("不支持删除")
            }.show()
        }
        
        // 设置加载更多点击监听器
        adapter.setOnLoadMoreClickListener {
            loadMore()
        }
        
        binding.rvPlaylist.adapter = adapter
        adapter.submitList(songList)
    }

    private fun initPlaylist(playlists: List<StandardPlaylist>,tag:Int) {
        binding.loadingContainer.visibility = View.GONE
        binding.rvSearchSuggest.visibility = View.GONE
        binding.rvPlaylist.visibility = View.VISIBLE
        binding.clPanel.visibility = View.GONE
        binding.rvPlaylist.layoutManager = LinearLayoutManager(this)
        binding.rvPlaylist.adapter = PlaylistAdapter {
            val intent = Intent(this@SearchActivity, SongPlaylistActivity::class.java)
            intent.putExtra(SongPlaylistActivity.EXTRA_TAG, tag)
            intent.putExtra(SongPlaylistActivity.EXTRA_ID, it.id.toString())
            startActivity(intent)
        }.apply {
            submitList(playlists)
        }
    }

    private fun initAlbums(albums: List<StandardAlbum>) {
        binding.loadingContainer.visibility = View.GONE
        binding.rvSearchSuggest.visibility = View.GONE
        binding.clPanel.visibility = View.GONE
        binding.rvPlaylist.layoutManager = LinearLayoutManager(this)
        binding.rvPlaylist.adapter = AlbumAdapter {
            val intent = Intent(this@SearchActivity, SongPlaylistActivity::class.java)
            intent.putExtra(SongPlaylistActivity.EXTRA_TAG, TAG_NETEASE)
            intent.putExtra(SongPlaylistActivity.EXTRA_ID, it.id.toString())
            intent.putExtra(SongPlaylistActivity.EXTRA_TYPE, SearchType.ALBUM)
            startActivity(intent)
        }.apply {
            submitList(albums)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 保存搜索引擎
        mmkv.encode(
            Config.SEARCH_ENGINE,
            searchViewModel.searchEngine.value ?: SearchViewModel.ENGINE_NETEASE
        )

    }

    override fun finish() {
        super.finish()
        overridePendingTransition(
            R.anim.anim_no_anim,
            R.anim.anim_alpha_exit
        )
    }


    override fun onBackPressed() {
        if (binding.clPanel.visibility == View.VISIBLE) {
            super.onBackPressed()
        } else {
            binding.clPanel.visibility = View.VISIBLE
        }
    }

    /**
     * 改变搜索引擎
     */
    private fun changeSearchEngine(engineCode: Int) {
        searchViewModel.searchEngine.value = engineCode
        if (binding.clPanel.visibility != View.VISIBLE) {
            search()
        }
    }

}