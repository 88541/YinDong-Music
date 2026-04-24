# 🎵 音动音乐 (YinDong Music)

一个基于 Jetpack Compose 的 Android 音乐播放器，支持多平台音乐搜索和播放。

[![GitHub stars](https://img.shields.io/github/stars/88541/YinDong-Music?style=flat-square)](https://github.com/88541/YinDong-Music/stargazers)
[![GitHub forks](https://img.shields.io/github/forks/88541/YinDong-Music?style=flat-square)](https://github.com/88541/YinDong-Music/network)
[![GitHub license](https://img.shields.io/github/license/88541/YinDong-Music?style=flat-square)](https://github.com/88541/YinDong-Music/blob/main/LICENSE)

## 📱 应用截图

![播放器界面](C:\Users\WJH\Downloads\Collage_20260424_194122.jpg)
*精美的圆形封面 + 环形频谱可视化效果*

## ✨ 功能特性

### 🎧 核心播放功能

- **多平台音乐搜索** - 支持网易云、QQ音乐、酷我音乐、酷狗音乐四大平台
- **在线播放** - 支持在线试听，无需下载
- **多种音质选择** - 标准(128kbps)、极高(320kbps)、无损(FLAC)、Hi-Res、超清母带
- **歌词显示** - 支持同步歌词、翻译歌词
- **播放模式** - 顺序播放、随机播放、单曲循环、列表循环
- **音频可视化** - 128条频谱条环绕封面，随音乐实时律动

### 🔍 搜索与发现

- **实时搜索建议** - 输入时自动提示相关搜索词
- **热搜榜单** - 显示热门搜索关键词
- **链接解析** - 支持抖音/汽水音乐分享链接自动解析播放
- **搜索历史** - 保存搜索记录，方便快速搜索
- **分类发现** - 抖音热歌、伤感情歌、怀旧金曲等分类

### 📂 歌单管理

- **创建歌单** - 自定义创建个人歌单
- **收藏歌曲** - 收藏喜欢的歌曲到歌单
- **添加到歌单** - 将歌曲添加到指定歌单
- **播放全部** - 一键播放歌单所有歌曲

### ⬇️ 下载功能

- **多音质下载** - 支持多种音质下载
- **下载管理** - 查看和管理已下载的歌曲

### 🔌 插件系统（高级功能）

- **LX Plugin 支持** - 支持洛雪音乐插件扩展
- **多源切换** - 可在不同音乐源之间切换
- **自定义API** - 支持自定义API地址

## 🎨 UI 特性

- **暗色主题** - 精美的深色界面设计
- **频谱可视化** - 128条紫色频谱条环绕封面，随音乐律动
- **封面旋转** - 播放时封面自动旋转
- **沉浸式体验** - 全屏歌词、沉浸式播放界面
- **底部导航** - 首页、发现、我的 三大模块

## 🛠️ 技术栈

- **Kotlin** - 现代 Android 开发语言
- **Jetpack Compose** - 声明式 UI 框架
- **Kotlin Coroutines** - 异步编程
- **OkHttp** - 网络请求
- **ExoPlayer** - 音频播放
- **Coil** - 图片加载
- **Material Design 3** - 设计规范

## 📦 安装方式

### 方式一：下载 APK

从 [Releases](https://github.com/88541/YinDong-Music/releases) 页面下载最新版本的 APK 文件安装。

### 方式二：自行编译

1. **克隆仓库**
   
   ```bash
   git clone https://github.com/88541/YinDong-Music.git
   ```

2. **打开项目**
   使用 Android Studio 打开项目目录

3. **编译运行**
   点击 Run 按钮编译并安装到设备

## ⚙️ 配置说明

### API 配置

项目使用第三方音乐 API，如需自定义 API 地址，请在设置中配置。

### 插件配置

支持 LX Music 插件格式，可在设置中导入插件扩展音乐源。

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

## 💬 联系我们

- **官方 QQ 群**: [673778042](https://github.com/88541/YinDong-Music#) - 音动音乐×众和夜雨科
- **GitHub Issues**: [提交问题](https://github.com/88541/YinDong-Music/issues)

## 🙏 致谢

感谢以下开源项目：

- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [ExoPlayer](https://github.com/google/ExoPlayer)
- [OkHttp](https://github.com/square/okhttp)
- [Coil](https://github.com/coil-kt/coil)

## ⚠️ 免责声明

本项目仅供学习交流使用，请勿用于商业用途。音乐版权归原平台所有。

---

<p align="center">
  Made with ❤️ by 音动音乐团队
</p>
