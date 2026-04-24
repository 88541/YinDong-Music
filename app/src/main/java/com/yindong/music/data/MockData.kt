package com.yindong.music.data

import com.yindong.music.data.model.Banner

/**
 * 静态 UI 数据 (仅 Banner 和热搜词，不含任何假歌曲/歌单)
 */
object MockData {

    val banners = listOf(
        Banner(1, title = "猜你喜欢", subtitle = "千万曲库 · 免费畅听", color = 0xFFD4963C, colorEnd = 0xFFBE7B28),
        Banner(2, title = "每日30首", subtitle = "每日更新 · 不重复推荐", color = 0xFF4A7FB5, colorEnd = 0xFF34608E),
        Banner(3, title = "无损品质", subtitle = "Hi-Res · 极致音质体验", color = 0xFF3D9E6E, colorEnd = 0xFF2B7A52),
        Banner(4, title = "新歌速递", subtitle = "全网新歌 · 抢先收听", color = 0xFF8B62B8, colorEnd = 0xFF6E4A96),
    )

    val hotSearches = listOf(
        "周杰伦", "起风了", "薛之谦", "林俊杰", "邓紫棋",
        "陈奕迅", "孤勇者", "告白气球", "晴天", "平凡之路",
        "七里香", "稻香", "青花瓷", "光年之外", "演员",
    )
}
