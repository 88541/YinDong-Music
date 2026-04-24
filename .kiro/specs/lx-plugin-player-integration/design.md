# 设计文档：LX 插件播放器适配

## 概述

在现有 MusicViewModel 的 `playSong()` 中增加插件失败回退逻辑，修复进度条时长为0的边界情况，以及歌词加载时的 source 路由问题。改动量极小，不引入新类或新架构。

## 改动点

### 1. playSong() 插件回退 (需求 1)

在现有 `playSong()` 的 `lx_plugin` 分支中，用 try-catch 包裹插件 musicUrl 调用，失败时回退到 `MusicApiService.fetchMusicUrl()`：

```kotlin
// 现有逻辑：
val result = if (apiMode == "lx_plugin" && lxSelectedSource.isNotBlank()) {
    // 插件获取 URL ...
} else {
    // 服务端获取 URL ...
}

// 改为：
val result = if (apiMode == "lx_plugin" && lxSelectedSource.isNotBlank()) {
    try {
        // 插件获取 URL（现有逻辑不变）
        ...
    } catch (e: Exception) {
        Log.w(TAG, "插件获取URL失败，回退服务端: ${e.message}")
        // 重置 headers 并回退到服务端
        httpDataSourceFactory.setDefaultRequestProperties(mapOf("User-Agent" to "CloudMusic/2.3.7"))
        MusicApiService.fetchMusicUrl(song.platform, song.platformId, selectedQuality.name.lowercase())
    }
} else {
    // 服务端获取（不变）
}
```

### 2. 进度条时长回退 (需求 2)

在 `initPlayer()` 的 `onPlaybackStateChanged(STATE_READY)` 中：

```kotlin
if (state == Player.STATE_READY) {
    val exoDuration = player.duration.coerceAtLeast(0L)
    totalDuration = if (exoDuration > 0) exoDuration else (currentSong?.duration ?: 0L)
}
```

### 3. 歌词 source 路由 (需求 3)

在 `loadLyrics()` 的插件歌词分支中，根据歌曲 platform 推断 source key：

```kotlin
// 现有：固定使用 lxSelectedSource
val result = lxPluginManager.lyric(lxSelectedPluginId, lxSelectedSource, song, lxTimeoutMs)

// 改为：优先使用歌曲对应的 source
val sourceKey = platformToSourceKey(song.platform) ?: lxSelectedSource
val result = lxPluginManager.lyric(lxSelectedPluginId, sourceKey, song, lxTimeoutMs)
```

`platformToSourceKey` 是一个简单的映射函数：
```kotlin
private fun platformToSourceKey(platform: String): String? = when {
    platform.contains("QQ") -> "tx"
    platform.contains("网易") -> "wy"
    platform.contains("酷我") -> "kw"
    platform.contains("酷狗") -> "kg"
    platform.contains("咪咕") -> "mg"
    platform.contains("抖音") -> "dy"
    else -> null
}
```
