package com.dirror.music.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dirror.music.databinding.ActivityMvListBinding
import com.dirror.music.music.netease.MVManager
import com.dirror.music.ui.base.BaseActivity
import com.dirror.music.adapter.MVAdapter
import com.dirror.music.util.runOnMainThread
import android.util.Log
import android.view.inputmethod.EditorInfo

/**
 * MV列表页面
 */
class MVListActivity : BaseActivity() {

    private lateinit var binding: ActivityMvListBinding
    private lateinit var mvAdapter: MVAdapter
    
    private var currentArea = ""
    private var currentOrder = "最热"
    private var currentOffset = 0
    private val limit = 30
    private var isLoading = false
    private var hasMore = true
    private var isSearchMode = false
    private var searchKeyword = ""

    // 地区选项
    private val areaOptions = listOf("全部地区", "内地", "港台", "欧美", "日本", "韩国")
    private val areaValues = listOf("", "内地", "港台", "欧美", "日本", "韩国")
    
    // 排序选项
    private val orderOptions = listOf("最热", "最新", "上升最快")
    private val orderValues = listOf("最热", "最新", "上升最快")

    override fun initBinding() {
        binding = ActivityMvListBinding.inflate(layoutInflater)
        miniPlayer = binding.miniPlayer
        setContentView(binding.root)
    }

    override fun initView() {
        setupSpinners()
        setupRecyclerView()
        setupSearch()
        loadMVList()
    }

    private fun setupSpinners() {
        // 地区筛选
        val areaAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, areaOptions)
        areaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerArea.adapter = areaAdapter
        binding.spinnerArea.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentArea = areaValues[position]
                if (!isSearchMode) {
                    refreshList()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 排序筛选
        val orderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, orderOptions)
        orderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerOrder.adapter = orderAdapter
        binding.spinnerOrder.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentOrder = orderValues[position]
                if (!isSearchMode) {
                    refreshList()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupRecyclerView() {
        mvAdapter = MVAdapter { mv ->
            // 点击MV项，跳转到播放页面
            val intent = Intent(this, MVPlayerActivity::class.java).apply {
                putExtra("mvid", mv.id)
                putExtra("mv_name", mv.name)
                putExtra("artist_name", mv.artistName)
                putExtra("cover", mv.cover)
            }
            startActivity(intent)
        }

        // 使用网格布局，每行2列
        val layoutManager = GridLayoutManager(this, 2)
        binding.rvMVList.layoutManager = layoutManager
        binding.rvMVList.adapter = mvAdapter

        // 添加滚动监听，实现加载更多
        binding.rvMVList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                if (dy > 0 && !isLoading && hasMore) { // 向下滚动
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMoreMV()
                    }
                }
            }
        })
    }

    private fun setupSearch() {
        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }

        binding.btnSearch.setOnClickListener {
            performSearch()
        }
    }

    private fun performSearch() {
        val keyword = binding.etSearch.text.toString().trim()
        if (keyword.isEmpty()) {
            // 如果搜索框为空，退出搜索模式
            isSearchMode = false
            searchKeyword = ""
            refreshList()
            return
        }

        isSearchMode = true
        searchKeyword = keyword
        currentOffset = 0
        hasMore = true
        showLoading(true)

        MVManager.searchMV(keyword, limit, currentOffset) { mvList ->
            runOnMainThread {
                showLoading(false)
                mvAdapter.setData(mvList)
                hasMore = mvList.size >= limit
                currentOffset = mvList.size
            }
        }
    }

    private fun refreshList() {
        currentOffset = 0
        hasMore = true
        loadMVList()
    }

    private fun loadMVList() {
        if (isLoading) return
        isLoading = true
        showLoading(true)

        MVManager.getAllMV(
            area = currentArea,
            order = currentOrder,
            limit = limit,
            offset = currentOffset
        ) { mvList ->
            runOnMainThread {
                showLoading(false)
                isLoading = false
                mvAdapter.setData(mvList)
                hasMore = mvList.size >= limit
                currentOffset = mvList.size
            }
        }
    }

    private fun loadMoreMV() {
        if (isLoading || !hasMore) return
        isLoading = true

        if (isSearchMode) {
            MVManager.searchMV(searchKeyword, limit, currentOffset) { mvList ->
                runOnMainThread {
                    isLoading = false
                    if (mvList.isNotEmpty()) {
                        mvAdapter.addData(mvList)
                        currentOffset += mvList.size
                    }
                    hasMore = mvList.size >= limit
                }
            }
        } else {
            MVManager.getAllMV(
                area = currentArea,
                order = currentOrder,
                limit = limit,
                offset = currentOffset
            ) { mvList ->
                runOnMainThread {
                    isLoading = false
                    if (mvList.isNotEmpty()) {
                        mvAdapter.addData(mvList)
                        currentOffset += mvList.size
                    }
                    hasMore = mvList.size >= limit
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
}
