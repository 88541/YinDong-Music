# 任务列表：导入外部歌单（仅客户端）

## 任务

- [x] 1. 实现 LinkParser 链接解析工具类
  - [x] 1.1 创建 `LinkParser.kt`，包含 `ParsedLink` 数据类和 `LinkParser` object
  - [x] 1.2 实现网易云歌单链接解析
  - [x] 1.3 实现QQ音乐歌单链接解析
  - [x] 1.4 实现酷我音乐歌单链接解析
  - [x] 1.5 实现酷狗音乐歌单链接解析
  - [x] 1.6 实现分享口令文本中 URL 提取逻辑
- [x] 2. 实现 MusicApiService.fetchPlaylistImport 方法
  - [x] 2.1 在 MusicApiService.kt 中新增 ImportedPlaylistData 数据类
  - [x] 2.2 实现 fetchPlaylistImport 方法
- [x] 3. 实现 MusicViewModel 导入逻辑
  - [x] 3.1 新增 ImportState sealed class 和 importState 状态变量
  - [x] 3.2 实现 importExternalPlaylist 方法
  - [x] 3.3 实现 cancelImport 和 resetImportState 方法
- [x] 4. 新增 Screen 路由和导航集成
  - [x] 4.1 在 Screen.kt 中新增 ImportPlaylist 路由
  - [x] 4.2 在 AppNavigation.kt 中注册 ImportPlaylistScreen composable 路由
- [x] 5. 实现 ImportPlaylistScreen Compose 界面
  - [x] 5.1 创建 ImportPlaylistScreen.kt 基础布局
  - [x] 5.2 实现剪贴板粘贴按钮功能
  - [x] 5.3 实现开始导入按钮逻辑
  - [x] 5.4 实现导入状态展示
  - [x] 5.5 实现可展开的 FAQ 常见问题区域
  - [x] 5.6 实现取消导入按钮和返回导航逻辑
- [x] 6. 在我的页面添加导入入口
  - [x] 6.1 在 MineScreen.kt 中添加导入外部歌单入口按钮
