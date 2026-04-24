# 任务列表：LX 插件播放器适配

## 相关文件
- `app/src/main/java/com/cloudmusic/app/viewmodel/MusicViewModel.kt`

## 任务

- [ ] 1. playSong() 插件回退逻辑
  - [ ] 1.1 在 playSong() 的 lx_plugin 分支中用 try-catch 包裹插件 musicUrl 调用，catch 中回退到 MusicApiService.fetchMusicUrl() 并重置 HTTP headers
  - [ ] 1.2 回退时记录日志到 lxDebugLogs

- [ ] 2. 进度条时长回退
  - [ ] 2.1 在 initPlayer() 的 onPlaybackStateChanged(STATE_READY) 中，当 ExoPlayer duration <= 0 时使用 currentSong?.duration 作为 totalDuration

- [ ] 3. 歌词 source 路由
  - [ ] 3.1 添加 platformToSourceKey() 辅助函数，将平台显示名映射到插件 source key
  - [ ] 3.2 在 loadLyrics() 的插件歌词分支中使用 platformToSourceKey() 推断 source，而非固定使用 lxSelectedSource
