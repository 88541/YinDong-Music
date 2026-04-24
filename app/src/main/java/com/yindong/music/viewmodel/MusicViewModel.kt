package com.yindong.music.viewmodel

import android.app.Application
import com.yindong.music.data.model.Banner
import com.yindong.music.data.CrashLogEntry
import com.yindong.music.data.CrashLogManager
import android.content.Context
import android.content.Intent
import android.media.audiofx.BassBoost
import android.media.audiofx.EnvironmentalReverb
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import android.os.PowerManager
import com.yindong.music.FloatingLyricsService
import com.yindong.music.MediaSessionHolder
import com.yindong.music.MusicPlaybackService
import com.yindong.music.data.BluetoothHeadsetManager
import com.yindong.music.data.LocalStorage
import com.yindong.music.data.MockData
import com.yindong.music.data.RemoteConfig
import com.yindong.music.data.api.MusicApiConfig
import com.yindong.music.data.api.MusicApiService
import com.yindong.music.data.api.RecommendPlaylist
import com.yindong.music.data.lx.BuiltinPluginManager
import com.yindong.music.data.lx.LxPluginEntry
import com.yindong.music.data.lx.LxPluginInfo
import com.yindong.music.data.lx.LxPluginManager
import com.yindong.music.data.lx.LxRuntimeOptions
import com.yindong.music.data.lx.RealQuickJsBridge
import com.yindong.music.security.MusicPlaybackGate
import com.yindong.music.security.SecurityGuard
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import com.yindong.music.data.model.LyricLine
import com.yindong.music.data.model.Playlist
import com.yindong.music.data.model.Song
import java.net.URLEncoder
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(UnstableApi::class)
class MusicViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "MusicVM"
        /** Special source value meaning "search all sources and merge results". */
        const val LX_SOURCE_ALL = "__all__"
    }

    // ── HTTP DataSource (supports per-play custom headers for LX plugins) ──
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(10_000)
        .setReadTimeoutMs(15_000)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            30_000,   // minBufferMs
            60_000,   // maxBufferMs
            2_500,    // bufferForPlaybackMs
            5_000     // bufferForPlaybackAfterRebufferMs
        )
        .build()

    private val musicAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .build()

    // ── 实时音频振幅处理器（必须在player之前初始化）──
    private val amplitudeProcessor = com.yindong.music.audio.AmplitudeAudioProcessor()

    private val player: ExoPlayer = ExoPlayer.Builder(
        application,
        com.yindong.music.audio.AmplitudeRenderersFactory(application, amplitudeProcessor)
    )
        .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
        .setLoadControl(loadControl)
        .setWakeMode(PowerManager.PARTIAL_WAKE_LOCK)
        .setAudioAttributes(musicAudioAttributes, /* handleAudioFocus = */ true)
        .build()

    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var equalizerEffect: Equalizer? = null
    private var reverbEffect: EnvironmentalReverb? = null
    private var loudnessEnhancerEffect: LoudnessEnhancer? = null
    // private var dynamicsEffect: DynamicsProcessing? = null

    // ── MediaSession (通知栏/灵动岛/锁屏媒体控制) ──
    private val sessionPlayer = object : ForwardingPlayer(player) {
        override fun seekToNext() { this@MusicViewModel.playNext() }
        override fun seekToPrevious() { this@MusicViewModel.playPrevious() }
        override fun seekToNextMediaItem() { this@MusicViewModel.playNext() }
        override fun seekToPreviousMediaItem() { this@MusicViewModel.playPrevious() }
        override fun hasNextMediaItem(): Boolean = true
        override fun hasPreviousMediaItem(): Boolean = true
        override fun getAvailableCommands(): Player.Commands {
            return super.getAvailableCommands().buildUpon()
                .add(Player.COMMAND_SEEK_TO_NEXT)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                .build()
        }
    }

    private val mediaSession: MediaSession? = try {
        MediaSession.Builder(application, sessionPlayer).build().also {
            MediaSessionHolder.session = it
        }
    } catch (e: Exception) {
        Log.e(TAG, "MediaSession创建失败", e)
        null
    }

    private var serviceStarted = false
    private var playerReleased = false
    private var playRequestToken = 0L
    private var playJob: Job? = null
    private var lyricsRequestToken = 0L
    private var intendedPlatformId: String? = null
    /** 上次自动重试的 platformId，防止无限重试 */
    private var lastRetryPlatformId: String? = null

    // ── 播放状态 ──
    var currentSong by mutableStateOf<Song?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set
    var progress by mutableFloatStateOf(0f)
        private set
    var isSeeking by mutableStateOf(false)
    var totalDuration by mutableLongStateOf(0L)
        private set
    var isBuffering by mutableStateOf(false)
        private set
    var playError by mutableStateOf<String?>(null)
        private set

    // ── 音质 (持久化) ──
    var selectedQuality by mutableStateOf(MusicApiConfig.Quality.STANDARD)
        private set

    // ── 主题 (持久化) ──
    var isDarkMode by mutableStateOf(false)
        private set

    // ── 开发者模式 (持久化) ──
    var isDevMode by mutableStateOf(false)
        private set

    // ── 播放模式 (持久化) ──
    enum class PlayMode { LOOP, SINGLE, SHUFFLE }
    var playMode by mutableStateOf(PlayMode.LOOP)
        private set

    // ── 播放器样式 (持久化) ──
    enum class PlayerStyle(val displayName: String, val description: String) {
        VINYL("黑胶唱片", "经典3D旋转唱片+唱针"),
        IMMERSIVE("沉浸歌词", "网易云风格，歌词占满全屏"),
        MINIMAL("极简模式", "大字歌名+精简控件"),
        COVER("封面大图", "全屏封面展示"),
        WAVE("波形可视化", "动态条形波形动画"),
        DISC_LYRICS("唱片歌词", "圆形封面+歌词，纯色背景随封面变化")
    }
    var playerStyle by mutableStateOf(PlayerStyle.VINYL)
        private set

    fun changePlayerStyle(style: PlayerStyle) {
        playerStyle = style
        LocalStorage.savePlayerStyle(style.name)
    }

    // ── 歌词 ──
    var lyrics by mutableStateOf<List<LyricLine>>(emptyList())
        private set
    var currentLyricIndex by mutableIntStateOf(0)
        private set
    var floatingLyricsEnabled by mutableStateOf(false)
        private set
    var floatingLyricsText by mutableStateOf("")
        private set
    var floatingLyricsColor by mutableStateOf(android.graphics.Color.WHITE)
        private set
    var highlightLyricColor by mutableStateOf(android.graphics.Color.GREEN)
        private set
    var normalLyricColor by mutableStateOf(android.graphics.Color.WHITE)
        private set
    var playerLyricSize by mutableIntStateOf(18)
        private set
    var floatingLyricSize by mutableIntStateOf(16)
        private set

    // ── 播放列表 ──
    var playlist by mutableStateOf<List<Song>>(emptyList())
        private set
    var currentIndex by mutableIntStateOf(0)
        private set

    // ── 歌单 ──
    var myPlaylists by mutableStateOf<List<Playlist>>(emptyList())
        private set
    var favoriteSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var recentSongs by mutableStateOf<List<Song>>(emptyList())
        private set

    // ── 发现页 ──
    var banners by mutableStateOf<List<Banner>>(emptyList())
        private set
    var recommendPlaylists by mutableStateOf<List<RecommendPlaylist>>(emptyList())
        private set

    // ── 搜索 ──
    var searchHistory by mutableStateOf<List<String>>(emptyList())
        private set
    var hotSearches by mutableStateOf<List<String>>(emptyList())
        private set
    var searchResults by mutableStateOf<List<Song>>(emptyList())
        private set
    var isSearching by mutableStateOf(false)
        private set
    var hasSearched by mutableStateOf(false)
        private set
    var hasMoreResults by mutableStateOf(true)
        private set
    private var currentSearchPage = 1
    private val searchPageSize = 20

    // ── 排行榜 ──
    var hotChart by mutableStateOf<List<Song>>(emptyList())
        private set
    var risingChart by mutableStateOf<List<Song>>(emptyList())
        private set
    var newChart by mutableStateOf<List<Song>>(emptyList())
        private set
    var originalChart by mutableStateOf<List<Song>>(emptyList())
        private set

    // ── 歌单广场 ──
    var playlistSquare by mutableStateOf<List<Playlist>>(emptyList())
        private set

    // ── 导入外部歌单 ──
    sealed class ImportState {
        object Idle : ImportState()
        object Loading : ImportState()
        data class Success(val playlistId: Long, val playlistName: String = "", val songCount: Int = 0) : ImportState()
        data class Error(val message: String) : ImportState()
    }
    var importState by mutableStateOf<ImportState>(ImportState.Idle)
        private set
    var isRecommendLoading by mutableStateOf(false)
        private set
    private var recommendLoadToken = 0L

    // ── 热歌榜（UI 用） ──
    var hotChartSongs by mutableStateOf<List<Song>>(emptyList())
        private set
    var isHotChartLoading by mutableStateOf(false)
        private set

    // ── 在线搜索结果（UI 可写） ──
    var onlineResults by mutableStateOf<List<Song>>(emptyList())
    var searchQuery by mutableStateOf("")
        private set
    var isParsing by mutableStateOf(false)
        private set
    var parseError by mutableStateOf<String?>(null)
        private set
    var searchError by mutableStateOf<String?>(null)
        private set

    // ── 搜索建议 ──
    var searchSuggestions by mutableStateOf<List<com.yindong.music.data.api.NeteaseApi.SearchSuggestion>>(emptyList())
        private set
    private var searchSuggestionJob: kotlinx.coroutines.Job? = null

    // ── 搜索分页相关（用于在线搜索） ──
    var isLoadingMore by mutableStateOf(false)
        private set
    private var currentSearchOffset = 0
    private val searchLimit = 30

    // ── 分类/播客搜索 ──
    var isCategoryLoading by mutableStateOf(false)
        private set
    var categorySongs by mutableStateOf<List<Song>>(emptyList())
        private set

    // ── 均衡器预设 ──
    enum class EqPreset(val displayName: String) {
        FLAT("纯净"),
        BASS("低音"),
        VOCAL("人声"),
        POP("流行"),
        ROCK("摇滚"),
        JAZZ("爵士"),
        CLASSICAL("古典"),
        ELECTRONIC("电音"),
        HIPHOP("说唱"),
        LIVE("现场"),
        NIGHT("夜间"),
        ACG("次元"),
    }
    var currentPreset by mutableStateOf(EqPreset.FLAT)
        private set

    // ── 混响参数 ──
    var reverbRoomSize by mutableIntStateOf(0)
        private set
    var reverbDamping by mutableIntStateOf(0)
        private set
    var reverbLevel by mutableIntStateOf(0)
        private set
    var loudnessGain by mutableIntStateOf(0)
        private set

    // ── 登录状态 ──
    var isLoggedIn by mutableStateOf(false)
        private set
    var userName by mutableStateOf("")
        private set
    var userId by mutableStateOf("")
        private set
    var userToken by mutableStateOf("")
        private set

    // ── 下载地址 ──
    var downloadPageUrl by mutableStateOf("")
        private set

    // ── API配置 ──
    var apiMode by mutableStateOf("official")
        private set
    var apiHost by mutableStateOf("")
        private set
    var qqMusicApi by mutableStateOf("")
        private set
    var neteaseApi by mutableStateOf("")
        private set
    var kuwoApi by mutableStateOf("")
        private set
    var miguApi by mutableStateOf("")
        private set
    var kugouApi by mutableStateOf("")
        private set
    var douyinApi by mutableStateOf("")
        private set
    var qqCookie by mutableStateOf("")
        private set
    var lxPluginUri by mutableStateOf("")
        private set
    var lxPluginHash by mutableStateOf("")
        private set
    var lxPluginInfo by mutableStateOf(LxPluginInfo())
        private set
    var lxSelectedSource by mutableStateOf("")
        private set
    var lxSources by mutableStateOf<List<String>>(emptyList())
        private set
    var lxAllowHttp by mutableStateOf(true)
        private set
    var lxTimeoutMs by mutableLongStateOf(15000L)
        private set
    var lxDebugLogs by mutableStateOf<List<String>>(emptyList())
        private set

    // ── 多插件状态 ──
    var lxPlugins by mutableStateOf<List<LxPluginEntry>>(emptyList())
        private set
    var lxSelectedPluginId by mutableStateOf("")
        private set

    // ── 插件/音源启用控制 ──
    var disabledPluginIds by mutableStateOf<Set<String>>(emptySet())
        private set
    /** key 格式: "pluginId:sourceKey" */
    var disabledSourceKeys by mutableStateOf<Set<String>>(emptySet())
        private set

    fun isPluginEnabled(pluginId: String): Boolean = pluginId !in disabledPluginIds
    fun isSourceEnabled(pluginId: String, source: String): Boolean = "$pluginId:$source" !in disabledSourceKeys

    fun togglePluginEnabled(pluginId: String) {
        disabledPluginIds = if (pluginId in disabledPluginIds) {
            disabledPluginIds - pluginId
        } else {
            disabledPluginIds + pluginId
        }
        LocalStorage.saveDisabledPlugins(disabledPluginIds)
    }

    fun toggleSourceEnabled(pluginId: String, source: String) {
        val key = "$pluginId:$source"
        disabledSourceKeys = if (key in disabledSourceKeys) {
            disabledSourceKeys - key
        } else {
            disabledSourceKeys + key
        }
        LocalStorage.saveDisabledSources(disabledSourceKeys)
    }

    fun clearLxLogs() { lxDebugLogs = emptyList() }
    fun addLxLog(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).format(java.util.Date())
        lxDebugLogs = (lxDebugLogs + "[$ts] $msg").takeLast(200)
    }

    private val lxPluginManager by lazy {
        val app = getApplication<Application>()
        LxPluginManager(
            context = app,
            contentResolver = app.contentResolver,
            quickJsFactory = { RealQuickJsBridge() },
            logCallback = { msg -> addLxLog(msg) },
        )
    }

    // ── 崩溃日志 ──
    var crashLogs by mutableStateOf<List<CrashLogEntry>>(emptyList())
        private set

    // ── 均衡器 ──
    var equalizerEnabled by mutableStateOf(true)
        private set
    var equalizerBands by mutableStateOf<List<Pair<Int, Int>>>(emptyList())
        private set
    var bassBoostEnabled by mutableStateOf(false)
        private set
    var bassBoostStrength by mutableIntStateOf(0)
        private set
    var virtualizerEnabled by mutableStateOf(false)
        private set
    var virtualizerStrength by mutableIntStateOf(0)
        private set

    // ── 定时停止 ──
    var sleepTimerMinutes by mutableIntStateOf(0)
        private set
    var sleepTimerEnabled by mutableStateOf(false)
        private set
    private var sleepTimerJob: Job? = null

    // ── 下载状态 (必须在 init 之前声明, 否则 loadDownloadedSongs() 会 NPE) ──
    var downloadState by mutableStateOf<DownloadState>(DownloadState.Idle)
        private set
    var downloadedSongs by mutableStateOf<List<DownloadedSong>>(emptyList())
        private set

    // ── 蓝牙耳机状态 ──
    var isHeadsetConnected by mutableStateOf(false)
        private set
    var connectedHeadsetName by mutableStateOf<String?>(null)
        private set
    var headsetType by mutableStateOf(BluetoothHeadsetManager.HeadsetType.UNKNOWN)
        private set
    var showHeadsetBanner by mutableStateOf(false)
        private set
    private var headsetBannerDismissed by mutableStateOf(false)

    var showBluetoothWelcomeAnimation by mutableStateOf(false)
        private set
    private var bluetoothWelcomeAnimationShown by mutableStateOf(false)

    // ── 实时音频振幅（用于封面跳动等视觉效果）──
    var audioAmplitude by mutableFloatStateOf(0f)
        private set

    private val headsetManager: BluetoothHeadsetManager by lazy {
        BluetoothHeadsetManager(getApplication())
    }

    init {
        // 设置振幅处理器回调（音频处理线程调用，不访问player）
        amplitudeProcessor.onAmplitudeUpdate = { amplitude ->
            audioAmplitude = amplitude
        }
        
        // 监听播放状态变化，同步给振幅处理器（主线程）
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                amplitudeProcessor.isPlayerPlaying = isPlaying
                if (!isPlaying) {
                    audioAmplitude = 0f
                }
            }
        })

        // 1. 立即预热TCP连接 (异步，不阻塞)
        MusicApiService.warmup()
        // 2. 本地数据加载 (无网络, 合并到单个协程减少开销)
        viewModelScope.launch {
            loadSettingsSync()
            loadUserAuthSync()
            loadLocalDataSync()
        }
        loadApiConfig() // 单独协程: 可能触发插件加载
        initPlayer()
        initEqualizer()
        // 3. 网络数据: 批量接口 + 推荐歌单并行加载
        loadInitData()
        loadRecommendPlaylists()
        // 4. 静默加载内置远程 JS 插件 (用户无感知)
        loadBuiltinPlugin()
        // 5. 初始化蓝牙耳机状态监测
        initHeadsetMonitoring()
    }

    /**
     * 初始化蓝牙耳机状态监测
     */
    private fun initHeadsetMonitoring() {
        headsetManager.startMonitoring()
        // 收集耳机状态变化
        viewModelScope.launch {
            headsetManager.headsetState.collect { state ->
                val wasConnected = isHeadsetConnected
                isHeadsetConnected = state.isConnected
                connectedHeadsetName = state.deviceName
                headsetType = state.type

                // 当耳机新连接时显示提示（且用户未手动关闭）
                if (state.isConnected && !wasConnected && !headsetBannerDismissed) {
                    showHeadsetBanner = true
                }
                
                // 应用启动时，如果蓝牙耳机已连接且未显示过欢迎动画，则显示
                if (state.isConnected && 
                    state.type == BluetoothHeadsetManager.HeadsetType.BLUETOOTH && 
                    !bluetoothWelcomeAnimationShown) {
                    showBluetoothWelcomeAnimation = true
                    bluetoothWelcomeAnimationShown = true
                }
            }
        }
    }

    /**
     * 关闭耳机连接提示横幅
     */
    fun dismissHeadsetBanner() {
        showHeadsetBanner = false
        headsetBannerDismissed = true
    }
    
    /**
     * 欢迎动画完成回调
     */
    fun onBluetoothWelcomeAnimationComplete() {
        showBluetoothWelcomeAnimation = false
    }

    /**
     * 重新显示耳机连接提示（用于测试或用户主动触发）
     */
    fun showHeadsetBannerAgain() {
        if (isHeadsetConnected) {
            showHeadsetBanner = true
            headsetBannerDismissed = false
        }
    }

    private fun initPlayer() {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_READY) {
                    totalDuration = player.duration.coerceAtLeast(0L)
                    playError = null
                }
                if (state == Player.STATE_ENDED) {
                    autoPlayNext()
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updateMediaSessionPlaybackState()
            }
            override fun onPositionDiscontinuity(reason: Int) {
                val mediaId = player.currentMediaItem?.mediaId
                // 只在 mediaId 与当前意图播放的歌曲匹配时才更新，防止旧歌曲事件覆盖新歌曲
                if (!mediaId.isNullOrEmpty() && mediaId == intendedPlatformId) {
                    val idx = playlist.indexOfFirst { it.platformId == mediaId }
                    if (idx >= 0) {
                        currentIndex = idx
                        currentSong = playlist[idx]
                        Log.d(TAG, "onPositionDiscontinuity: resolved by mediaId=$mediaId, idx=$idx")
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "播放错误: ${error.errorCodeName}", error)
                // IO 类错误自动重试一次 (清除缓存重新获取URL，解决QQ音乐等CDN链接过期问题)
                val song = currentSong
                if (song != null && song.platformId != lastRetryPlatformId && isRetryableError(error)) {
                    lastRetryPlatformId = song.platformId
                    Log.d(TAG, "播放失败, 自动重试: ${song.title}")
                    MusicApiService.invalidatePlayUrlCache(song.platform, song.platformId)
                    playSong(song)
                    return
                }
                playError = "播放失败: ${getErrorMessage(error.errorCode)}"
                isBuffering = false
            }
        })

        viewModelScope.launch {
            while (true) {
                delay(500)
                // 不播放时跳过更新，减少 CPU 开销
                if (!player.isPlaying && player.playbackState != Player.STATE_BUFFERING) continue
                if (player.duration > 0 && !isSeeking) {
                    progress = player.currentPosition.toFloat() / player.duration.toFloat()
                }
                updateLyricIndex()
                updateFloatingLyrics()
            }
        }
    }

    /** 判断是否为可重试的播放错误 (网络/IO相关) */
    private fun isRetryableError(error: androidx.media3.common.PlaybackException): Boolean {
        return error.errorCode in setOf(
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
        )
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> "网络连接失败"
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "网络连接超时"
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> "音频链接已失效"
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> "无效的音频格式"
            androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED -> "解码器初始化失败"
            else -> "未知错误 (代码: $errorCode)"
        }
    }

    private fun updateMediaSessionPlaybackState() {
        // MediaSession 通过 Player 自动同步元数据
    }

    /** 同步加载设置 (在已有协程内调用, 避免额外协程开销) */
    private fun loadSettingsSync() {
        selectedQuality = MusicApiConfig.Quality.fromStoredValue(LocalStorage.loadQuality())
        isDarkMode = LocalStorage.loadDarkMode()
        isDevMode = LocalStorage.loadDevMode()
        playMode = try { PlayMode.valueOf(LocalStorage.loadPlayMode()) } catch (_: Exception) { PlayMode.LOOP }
        playerStyle = try { PlayerStyle.valueOf(LocalStorage.loadPlayerStyle()) } catch (_: Exception) { PlayerStyle.VINYL }
        currentPreset = try { EqPreset.valueOf(LocalStorage.loadEqPreset()) } catch (_: Exception) { EqPreset.FLAT }
        floatingLyricsColor = LocalStorage.loadFloatingLyricColor()
        highlightLyricColor = LocalStorage.loadLyricCurrentColor()
        normalLyricColor = LocalStorage.loadLyricNormalColor()
        playerLyricSize = LocalStorage.loadLyricFontSize()
        floatingLyricSize = LocalStorage.loadFloatingLyricSize()
    }

    /** 同步加载用户认证 */
    private fun loadUserAuthSync() {
        userToken = LocalStorage.loadUserToken()
        userName = LocalStorage.loadUserName()
        userId = LocalStorage.loadUserId().toString()
        isLoggedIn = userToken.isNotEmpty()
    }

    /** 同步加载所有本地数据 (歌单/收藏/历史等) */
    private fun loadLocalDataSync() {
        myPlaylists = LocalStorage.loadPlaylists()
        favoriteSongs = LocalStorage.loadFavorites()
        recentSongs = LocalStorage.loadPlayHistory()
        searchHistory = LocalStorage.loadSearchHistory()
        crashLogs = CrashLogManager.getLogFiles()
        banners = MockData.banners
        hotSearches = listOf("周杰伦", "林俊杰", "薛之谦", "陈奕迅", "邓紫棋")
        downloadPageUrl = LocalStorage.loadServerUrl()
        loadDownloadedSongs()
    }

    private fun loadApiConfig() {
        viewModelScope.launch {
            apiMode = LocalStorage.loadApiMode()
            apiHost = LocalStorage.loadApiHost()
            qqMusicApi = LocalStorage.loadQQMusicApiKey()
            neteaseApi = LocalStorage.loadNeteaseApiKey()
            kuwoApi = LocalStorage.loadKuwoApiKey()
            miguApi = LocalStorage.loadMiguApiKey()
            kugouApi = LocalStorage.loadKugouApiKey()
            douyinApi = LocalStorage.loadDouyinApiKey()
            qqCookie = LocalStorage.loadQQCookie()
            lxPluginUri = LocalStorage.loadLxPluginUri()
            lxPluginHash = LocalStorage.loadLxPluginHash()
            lxPluginInfo = LocalStorage.loadLxPluginInfo()
            lxSelectedSource = LocalStorage.loadLxSelectedSource()
            lxSelectedPluginId = LocalStorage.loadLxSelectedPluginId()
            lxAllowHttp = LocalStorage.loadLxAllowHttp()
            lxTimeoutMs = LocalStorage.loadLxTimeoutMs()
            disabledPluginIds = LocalStorage.loadDisabledPlugins()
            disabledSourceKeys = LocalStorage.loadDisabledSources()
            if (apiMode == "lx_plugin") {
                ensurePluginPolicyLoaded()
                val savedPlugins = LocalStorage.loadLxPlugins()
                if (savedPlugins.isNotEmpty()) {
                    reloadAllPlugins(savedPlugins)
                } else if (lxPluginUri.isNotBlank()) {
                    // Legacy single-plugin fallback
                    val result = reloadLxPlugin()
                    if (result.isFailure) {
                        Log.e(TAG, "reloadLxPlugin failed on startup", result.exceptionOrNull())
                    } else {
                        Log.d(TAG, "reloadLxPlugin ok: sources=${lxSources}, selectedSource=$lxSelectedSource")
                    }
                }
            }
        }
    }


    /**
     * 静默加载内置远程 JS 插件
     * 从服务器拉取最新 JS 音源脚本，用户完全无感知。
     * 管理员替换服务器上的 JS 文件即可热更新。
     */
    private fun loadBuiltinPlugin() {
        viewModelScope.launch {
            // 静默加载，不产生任何用户可见日志
            BuiltinPluginManager.syncAndLoad(
                context = getApplication(),
                pluginManager = lxPluginManager,
                logCallback = null,
            )
            if (BuiltinPluginManager.isLoaded) {
                // 内置插件加载后，重建 source 列表（包含内置源）
                rebuildSources()
                // 插件模式下，若当前选中的source不属于当前插件，则自动切到内置插件
                if (apiMode == "lx_plugin" && BuiltinPluginManager.pluginId.isNotBlank()) {
                    val curSources = lxPluginManager.getPluginEntry(lxSelectedPluginId)?.sources.orEmpty()
                    if (lxSelectedSource.isBlank() || lxSelectedSource !in curSources) {
                        lxSelectedPluginId = BuiltinPluginManager.pluginId
                        LocalStorage.saveLxSelectedPluginId(lxSelectedPluginId)
                        if (lxSelectedSource.isBlank() || lxSelectedSource !in BuiltinPluginManager.sources) {
                            lxSelectedSource = BuiltinPluginManager.sources.firstOrNull().orEmpty()
                            LocalStorage.saveLxSelectedSource(lxSelectedSource)
                        }
                    }
                }
                Log.d(TAG, "内置插件就绪: sources=${BuiltinPluginManager.sources}")
            }
        }
    }

    /** 内置插件的 pluginId，用于从用户可见列表中过滤 */
    private val builtinPluginId: String get() = BuiltinPluginManager.pluginId

    private fun mergeRecommendPlaylists(vararg groups: List<RecommendPlaylist>): List<RecommendPlaylist> {
        val merged = mutableListOf<RecommendPlaylist>()
        val seen = LinkedHashSet<String>()
        groups.forEach { group ->
            group.forEach { item ->
                val platform = item.platform.trim()
                val playlistId = item.playlistId.trim()
                val name = item.name.trim()
                if (platform.isBlank() || playlistId.isBlank() || name.isBlank()) return@forEach
                val key = "$platform::$playlistId"
                if (!seen.add(key)) return@forEach
                merged.add(
                    item.copy(
                        name = name,
                        platform = platform,
                        playlistId = playlistId,
                        coverUrl = item.coverUrl.trim(),
                    )
                )
            }
        }
        return merged
    }

    private fun preferRecommendPlaylistsWithCover(playlists: List<RecommendPlaylist>, minCoveredOnly: Int = 24): List<RecommendPlaylist> {
        if (playlists.isEmpty()) return playlists
        val withCover = playlists.filter { it.coverUrl.isNotBlank() }
        if (withCover.isEmpty()) return playlists
        return if (withCover.size >= minCoveredOnly) withCover else withCover + playlists.filter { it.coverUrl.isBlank() }
    }

    fun loadRecommendPlaylists() {
        val requestToken = ++recommendLoadToken
        viewModelScope.launch {
            isRecommendLoading = true
            val serverPlaylists = try {
                MusicApiService.fetchRecommendPlaylists()
            } catch (e: Exception) {
                emptyList()
            }
            val quickBase = mergeRecommendPlaylists(serverPlaylists, builtInPlaylists)
            recommendPlaylists = quickBase
            // 用前4个推荐歌单的真实封面构建 Banner
            buildBannersFromRecommend(recommendPlaylists)
            // 异步从服务器拉取 Banner 封面
            loadBannerCovers()
            try {
                var working = quickBase
                val officialPlaylists = try {
                    MusicApiService.fetchOfficialFeaturedPlaylists(
                        totalLimit = 160,
                        perPlatformLimit = 48,
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "官方精选歌单拉取失败", e)
                    emptyList()
                }
                if (requestToken != recommendLoadToken) return@launch
                if (officialPlaylists.isNotEmpty()) {
                    val mergedOfficial = mergeRecommendPlaylists(officialPlaylists, working)
                    if (mergedOfficial != recommendPlaylists) {
                        recommendPlaylists = mergedOfficial
                        buildBannersFromRecommend(recommendPlaylists)
                    }
                    working = mergedOfficial
                }

                val enriched = MusicApiService.enrichRecommendPlaylistCovers(
                    playlists = working,
                    maxLookup = 480,
                    parallelism = 16,
                )
                if (requestToken != recommendLoadToken) return@launch
                val preferred = preferRecommendPlaylistsWithCover(enriched, minCoveredOnly = 24)
                if (preferred != recommendPlaylists) {
                    recommendPlaylists = preferred
                    buildBannersFromRecommend(recommendPlaylists)
                }
            } catch (e: Exception) {
                Log.w(TAG, "推荐歌单加载失败", e)
            } finally {
                if (requestToken == recommendLoadToken) {
                    isRecommendLoading = false
                }
            }
        }
    }

    /** Banner 固定 4 张卡片，从推荐列表中查找封面 */
    private data class BannerSpec(
        val title: String,
        val subtitle: String,
        val platform: String,
        val playlistId: String,
    )

    private val fixedBannerSpecs = listOf(
        BannerSpec("每日30首", "QQ音乐 \u00b7 每日推荐", "QQ音乐", "6117630062"),
        BannerSpec("热歌排行榜", "QQ音乐 \u00b7 飙升榜", "QQ音乐", "62"),
        BannerSpec("新歌推荐", "QQ音乐 \u00b7 新歌榜", "QQ音乐", "27"),
        BannerSpec("猜你喜欢", "QQ音乐 \u00b7 精选推荐", "QQ音乐", "8995383783"),
        BannerSpec("粤语老歌", "网易云 \u00b7 经典粤语", "网易云", "13938849799"),
        BannerSpec("好听的音乐", "网易云 \u00b7 精选好歌", "网易云", "12449928929"),
        BannerSpec("抖音热歌", "QQ音乐 \u00b7 热门抖音", "QQ音乐", "7993862959"),
    )

    private fun buildBannersFromRecommend(playlists: List<RecommendPlaylist>) {
        val current = banners
        banners = fixedBannerSpecs.mapIndexed { i, spec ->
            val matched = playlists.find { it.platform == spec.platform && it.playlistId == spec.playlistId }
            val existingCover = current.getOrNull(i)?.imageUrl.orEmpty()
            val cover = matched?.coverUrl?.takeIf { it.isNotBlank() } ?: existingCover
            Banner(
                id = i.toLong() + 1,
                title = spec.title,
                subtitle = spec.subtitle,
                imageUrl = cover,
                platform = spec.platform,
                playlistId = spec.playlistId,
            )
        }
    }

    /** 异步获取 Banner 封面（从服务器 API 拉取歌单详情提取 coverUrl） */
    private fun loadBannerCovers() {
        viewModelScope.launch {
            fixedBannerSpecs.forEachIndexed { i, spec ->
                launch {
                    try {
                        val result = MusicApiService.fetchPlaylistImport(spec.platform, spec.playlistId)
                        val coverUrl = result?.coverUrl.orEmpty()
                        if (coverUrl.isNotBlank()) {
                            val current = banners.toMutableList()
                            if (i < current.size && current[i].imageUrl.isBlank()) {
                                current[i] = current[i].copy(imageUrl = coverUrl)
                                banners = current
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Banner封面加载失败[${spec.title}]: ${e.message}")
                    }
                }
            }
        }
    }

    /** 内置精选歌单——服务器无数据时兜底 (均为各平台真实歌单ID) */
    private val builtInPlaylists = listOf(
        // ── 网易云音乐 (官方榜单 + 热门用户歌单) ──
        RecommendPlaylist("云音乐热歌榜",       "", 3200_0000, "网易云", "3778678"),
        RecommendPlaylist("云音乐飙升榜",       "", 2100_0000, "网易云", "19723756"),
        RecommendPlaylist("云音乐新歌榜",       "", 1800_0000, "网易云", "3779629"),
        RecommendPlaylist("云音乐原创榜",       "", 680_0000,  "网易云", "2884035"),
        // ── QQ音乐 (热门歌单) ──
        RecommendPlaylist("抖音最热DJ合集",     "", 860_0000,  "QQ音乐", "7526065605"),
        RecommendPlaylist("经典老歌500首",     "", 1500_0000, "QQ音乐", "7446068798"),
        RecommendPlaylist("华语流行热歌",       "", 920_0000,  "QQ音乐", "8690683167"),
        RecommendPlaylist("抖音热歌精选",       "", 1100_0000, "QQ音乐", "7603617752"),
        // ── 酷我音乐 ──
        RecommendPlaylist("酷我热歌榜",         "", 780_0000,  "酷我音乐", "236682355"),
        RecommendPlaylist("酷我飙升榜",         "", 560_0000,  "酷我音乐", "244441035"),
        RecommendPlaylist("酷我新歌榜",         "", 430_0000,  "酷我音乐", "279521540"),
        // ── 酷狗音乐 ──
        RecommendPlaylist("酷狗TOP500",       "", 2500_0000, "酷狗音乐", "320496"),
        RecommendPlaylist("酷狗飙升榜",         "", 520_0000,  "酷狗音乐", "31308"),
        RecommendPlaylist("酷狗新歌榜",         "", 390_0000,  "酷狗音乐", "279203"),
        // ── 跨平台热门 ──
        RecommendPlaylist("2025热搜热歌",      "", 1600_0000, "网易云", "2892128872"),
        RecommendPlaylist("2025流行音乐集锦",   "", 890_0000,  "网易云", "13834199288"),
    )


    /**
     * 启动时用批量接口一次获取所有热歌榜，失败则回退到并发单独请求
     */
    private fun loadInitData() {
        viewModelScope.launch {
            try {
                val initData = MusicApiService.fetchInitData()
                if (initData != null) {
                    hotChart = initData.hotChart
                    risingChart = initData.risingChart
                    newChart = initData.newChart
                    originalChart = initData.originalChart
                    Log.d(TAG, "批量初始化成功: hot=${initData.hotChart.size}, rising=${initData.risingChart.size}")
                    return@launch
                }
            } catch (e: Exception) {
                Log.w(TAG, "批量初始化失败，回退到并发请求", e)
            }
            // 回退: 并发加载各平台热歌榜
            loadChartsConcurrently()
        }
    }

    /**
     * 并发4个热歌榜请求 (以前是串行的，现在并发)
     */
    private fun loadChartsConcurrently() {
        viewModelScope.launch {
            try {
                coroutineScope {
                    val hotDeferred = async { MusicApiService.fetchAllHotChart() }
                    val risingDeferred = async { MusicApiService.fetchNeteaseHotChart() }
                    val newDeferred = async { MusicApiService.fetchKuwoHotChart() }
                    val originalDeferred = async { MusicApiService.fetchKugouHotChart() }
                    hotChart = hotDeferred.await()
                    risingChart = risingDeferred.await()
                    newChart = newDeferred.await()
                    originalChart = originalDeferred.await()
                }
            } catch (e: Exception) {
                Log.e(TAG, "并发加载排行榜失败", e)
            }
        }
    }

    // ── 浏览外部歌单（不保存到本地） ──
    var externalViewPlaylist by mutableStateOf<Playlist?>(null)
        private set
    var isExternalViewLoading by mutableStateOf(false)
        private set
    private var externalViewRequestToken = 0L

    fun loadExternalPlaylistForView(platform: String, playlistId: String) {
        viewModelScope.launch {
            val requestToken = ++externalViewRequestToken
            isExternalViewLoading = true
            externalViewPlaylist = null
            try {
                val result = MusicApiService.fetchPlaylistImport(platform, playlistId)
                if (result != null) {
                    val initialSongs = result.songs
                    val initialCover = if (result.coverUrl.isNotBlank()) {
                        result.coverUrl
                    } else {
                        initialSongs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty()
                    }
                    externalViewPlaylist = Playlist(
                        id = -1L,
                        name = result.name,
                        coverUrl = initialCover,
                        songCount = initialSongs.size,
                        playCount = 0,
                        creator = platform,
                        songs = initialSongs,
                    )
                    val needsEnrich = initialSongs.any {
                        it.coverUrl.isBlank() &&
                            it.artistPicUrl.isBlank() &&
                            it.platform.isNotBlank() &&
                            it.platformId.isNotBlank()
                    }
                    if (needsEnrich) {
                        launch {
                            fun publishIfLatest(enrichedSongs: List<Song>) {
                                if (requestToken != externalViewRequestToken) return
                                val current = externalViewPlaylist ?: return
                                val mergedCover = if (current.coverUrl.isNotBlank()) {
                                    current.coverUrl
                                } else {
                                    enrichedSongs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty()
                                }
                                val updated = current.copy(
                                    coverUrl = mergedCover,
                                    songCount = enrichedSongs.size,
                                    songs = enrichedSongs,
                                )
                                if (updated != current) {
                                    externalViewPlaylist = updated
                                }
                            }
                            try {
                                val fast = MusicApiService.enrichMissingSongCovers(
                                    songs = initialSongs,
                                    maxLookup = 24,
                                    parallelism = 12,
                                )
                                publishIfLatest(fast)
                                val full = MusicApiService.enrichMissingSongCovers(
                                    songs = fast,
                                    maxLookup = 160,
                                    parallelism = 10,
                                )
                                publishIfLatest(full)
                            } catch (e: Exception) {
                                Log.w(TAG, "外部歌单歌曲补图失败", e)
                            }
                        }
                    }
                } else {
                    showToast("获取歌单失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载外部歌单失败", e)
                showToast("加载失败: ${e.message}")
            } finally {
                isExternalViewLoading = false
            }
        }
    }

    fun saveExternalPlaylistToMine(): Long? {
        val ext = externalViewPlaylist ?: return null
        val newId = System.currentTimeMillis()
        val newPlaylist = ext.copy(id = newId, creator = userName)
        myPlaylists = myPlaylists + newPlaylist
        LocalStorage.savePlaylists(myPlaylists)
        showToast("已保存到我的歌单")
        return newId
    }

    // ── 导入外部歌单 ──
    private var importJob: Job? = null

    fun importExternalPlaylist(platform: String, playlistId: String) {
        importJob?.cancel()
        importState = ImportState.Loading
        importJob = viewModelScope.launch {
            try {
                val result = MusicApiService.fetchPlaylistImport(platform, playlistId)
                if (result == null) {
                    importState = ImportState.Error("获取歌单失败，请检查链接是否正确")
                    return@launch
                }
                val enrichedSongs = try {
                    MusicApiService.enrichMissingSongCovers(
                        songs = result.songs,
                        maxLookup = 160,
                        parallelism = 10,
                    )
                } catch (_: Exception) {
                    result.songs
                }
                val mergedCover = if (result.coverUrl.isNotBlank()) {
                    result.coverUrl
                } else {
                    enrichedSongs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty()
                }
                val newPlaylist = Playlist(
                    id = System.currentTimeMillis(),
                    name = result.name,
                    coverUrl = mergedCover,
                    songCount = enrichedSongs.size,
                    playCount = 0,
                    creator = userName,
                    songs = enrichedSongs,
                )
                myPlaylists = myPlaylists + newPlaylist
                LocalStorage.savePlaylists(myPlaylists)
                importState = ImportState.Success(newPlaylist.id, result.name, enrichedSongs.size)
            } catch (e: Exception) {
                Log.e(TAG, "导入外部歌单失败", e)
                importState = ImportState.Error("导入失败: ${e.message}")
            }
        }
    }

    fun resetImportState() {
        importState = ImportState.Idle
    }

    /** 浏览外部歌单：获取歌曲并直接播放，不保存到本地歌单 */
    fun viewExternalPlaylist(platform: String, playlistId: String) {
        viewModelScope.launch {
            isBuffering = true
            try {
                val result = MusicApiService.fetchPlaylistImport(platform, playlistId)
                if (result != null && result.songs.isNotEmpty()) {
                    playPlaylist(result.songs, 0)
                } else {
                    showToast("获取歌单失败")
                }
            } catch (e: Exception) {
                Log.e(TAG, "浏览外部歌单失败", e)
                showToast("加载失败: ${e.message}")
            } finally {
                isBuffering = false
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        importState = ImportState.Idle
    }


    /**
     * Try to get a music URL from plugins.
     * Priority: 1) origin plugin (found the song, pluginRawJson is its format)
     *           2) selected plugin (only if it supports source)
     *           3) fallback: other plugins supporting source (up to 3)
     */
    /** Reject plugin URLs that contain unresolved template variables or are clearly invalid. */
    private val trustedCleartextPlaybackHosts = setOf(
        "127.0.0.1",
        "localhost",
    )
    private fun isValidPluginUrl(url: String): Boolean {
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        val scheme = parsed.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = parsed.host?.trim()?.lowercase().orEmpty()
        if (host.isBlank()) return false
        if (scheme == "http" && host !in trustedCleartextPlaybackHosts) {
            Log.w(TAG, "isValidPluginUrl: rejected cleartext host=$host")
            return false
        }
        if (url.contains("=undefined") || url.contains("=null") || url.contains("=NaN")) return false
        if (url.contains("/undefined") || url.contains("/null")) return false
        return true
    }

    private suspend fun tryPluginMusicUrl(
        source: String,
        song: Song,
        quality: String,
    ): com.yindong.music.data.lx.LxMusicUrlResult? {
        val perPluginTimeout = 6000L

        suspend fun tryPlugin(pluginId: String, label: String): com.yindong.music.data.lx.LxMusicUrlResult? {
            return try {
                val r = withTimeoutOrNull(perPluginTimeout) {
                    lxPluginManager.musicUrl(pluginId, source, song, perPluginTimeout, quality)
                }
                if (r != null && r.url.isNotBlank() && isValidPluginUrl(r.url)) {
                    Log.d(TAG, "tryPluginMusicUrl: $label ${pluginId.take(8)} ok")
                    r
                } else {
                    if (r != null && r.url.isNotBlank()) {
                        Log.w(TAG, "tryPluginMusicUrl: $label ${pluginId.take(8)} rejected invalid url: ${r.url.take(120)}")
                    }
                    null
                }
            } catch (e: Exception) {
                Log.w(TAG, "tryPluginMusicUrl: $label ${pluginId.take(8)} failed: ${e.message}")
                null
            }
        }

        // 1. 原始插件: 搜索该歌曲的插件，最能理解其 pluginRawJson 格式
        val originId = song.lxPluginId
        if (originId.isNotBlank() && lxPluginManager.isPluginLoaded(originId)) {
            tryPlugin(originId, "origin")?.let { return it }
        }

        // 2. 选中插件: 仅当其支持该 source 时才尝试
        if (lxSelectedPluginId.isNotBlank() && lxSelectedPluginId != originId) {
            val entry = lxPlugins.find { it.id == lxSelectedPluginId }
            val supportsSource = entry == null || source in entry.sources
            if (supportsSource) {
                tryPlugin(lxSelectedPluginId, "selected")?.let { return it }
            } else {
                Log.d(TAG, "tryPluginMusicUrl: selected plugin ${lxSelectedPluginId.take(8)} does not support source=$source, skip")
            }
        }

        // 3. Fallback: 其他支持该 source 的插件，最多尝试3次
        var fallbackAttempts = 0
        for (plugin in lxPlugins) {
            if (fallbackAttempts >= 3) break
            if (plugin.id == lxSelectedPluginId || plugin.id == originId) continue
            if (!isPluginEnabled(plugin.id)) continue
            if (source !in plugin.sources) continue
            fallbackAttempts++
            tryPlugin(plugin.id, "fallback")?.let { return it }
        }
        return null
    }

    private fun passSecurityGate(
        action: MusicPlaybackGate.Action,
        defaultMessage: String,
        parseScope: Boolean = false,
    ): Boolean {
        val decision = MusicPlaybackGate.evaluate(action)
        if (decision.allowed) return true
        val message = decision.reason ?: defaultMessage
        Log.w(TAG, "安全策略拦截[$action]: $message")
        if (parseScope) parseError = message else playError = message
        showToast(message)
        if (decision.shouldKillProcess) {
            SecurityGuard.killProcess()
        }
        return false
    }

    // ── 播放控制 ──
    fun playSong(song: Song) {
        if (!passSecurityGate(MusicPlaybackGate.Action.START_PLAYBACK, "当前环境存在风险，已阻止播放")) return
        if (playerReleased) return
        val requestToken = ++playRequestToken
        // 用户主动切歌时重置重试标记
        if (song.platformId != currentSong?.platformId) lastRetryPlatformId = null
        playJob?.cancel()
        Log.d(TAG, "playSong start: song=${song.title}, platform=${song.platform}, pid=${song.platformId}, queueSize=${playlist.size}, currentIndex=$currentIndex")
        currentSong = song
        intendedPlatformId = song.platformId
        // 立即清除旧歌词，避免切歌时显示上一首的歌词
        lyrics = emptyList()
        currentLyricIndex = 0
        // 立即停止旧播放，防止旧歌曲的回调事件覆盖新歌曲状态
        player.stop()
        val existingIndex = playlist.indexOfFirst { it.platformId == song.platformId && it.platform == song.platform }
        if (existingIndex >= 0) {
            currentIndex = existingIndex
        } else {
            if (playlist.isEmpty()) {
                playlist = listOf(song)
                currentIndex = 0
            } else {
                playlist = playlist + song
                currentIndex = playlist.lastIndex
            }
        }
        Log.d(TAG, "playSong resolved: queueSize=${playlist.size}, currentIndex=$currentIndex")
        // 歌词与播放并行加载——不等URL拿到就开始获取歌词
        loadLyrics(song, requestToken)

        playJob = viewModelScope.launch {
            isBuffering = true
            playError = null
            try {
                val result = if (apiMode == "lx_plugin" && lxSelectedSource.isNotBlank()) {
                    val lxQuality = when (selectedQuality) {
                        MusicApiConfig.Quality.STANDARD -> "128k"
                        MusicApiConfig.Quality.EXHIGH -> "320k"
                        MusicApiConfig.Quality.LOSSLESS -> "flac"
                    }
                    val effectiveSource = effectiveLxSource(song)
                    // Try selected plugin first, then fall back to other plugins
                    val pluginResult = tryPluginMusicUrl(effectiveSource, song, lxQuality)
                    if (pluginResult != null) {
                        // Apply plugin headers to ExoPlayer
                        val headers = mutableMapOf(
                            "User-Agent" to "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        )
                        if (pluginResult.headers.isNotEmpty()) {
                            headers.putAll(pluginResult.headers)
                        }
                        httpDataSourceFactory.setDefaultRequestProperties(headers)
                        Log.d(TAG, "playSong plugin url=${pluginResult.url.take(120)}, headers=${pluginResult.headers.keys}")
                        com.yindong.music.data.api.MusicUrlResult(url = pluginResult.url)
                    } else {
                        // All plugins failed, try server API as last resort
                        Log.w(TAG, "playSong: all plugins failed, falling back to server API")
                        httpDataSourceFactory.setDefaultRequestProperties(mapOf("User-Agent" to "CloudMusic/2.3.7"))
                        try {
                            MusicApiService.fetchMusicUrl(song.platform, song.platformId, selectedQuality.name.lowercase())
                        } catch (e: Exception) {
                            com.yindong.music.data.api.MusicUrlResult(error = "所有插件均无法获取播放链接")
                        }
                    }
                } else {
                    // Reset to default headers for non-plugin playback
                    httpDataSourceFactory.setDefaultRequestProperties(mapOf(
                        "User-Agent" to "CloudMusic/2.3.7"
                    ))
                    // 官方模式: 服务端API → 失败时自动 fallback 到内置插件
                    val serverResult = MusicApiService.fetchMusicUrl(song.platform, song.platformId, selectedQuality.name.lowercase())
                    if (!serverResult.url.isNullOrEmpty()) {
                        serverResult
                    } else if (BuiltinPluginManager.isLoaded) {
                        // 服务端API失败，尝试内置插件兜底
                        Log.d(TAG, "playSong: 服务端API失败，尝试内置插件")
                        val builtinQuality = when (selectedQuality) {
                            MusicApiConfig.Quality.STANDARD -> "128k"
                            MusicApiConfig.Quality.EXHIGH -> "320k"
                            MusicApiConfig.Quality.LOSSLESS -> "flac"
                        }
                        val builtinResult = BuiltinPluginManager.musicUrl(lxPluginManager, song, builtinQuality)
                        if (builtinResult != null && builtinResult.url.isNotBlank()) {
                            // 应用插件 headers
                            val headers = mutableMapOf(
                                "User-Agent" to "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            )
                            if (builtinResult.headers.isNotEmpty()) {
                                headers.putAll(builtinResult.headers)
                            }
                            httpDataSourceFactory.setDefaultRequestProperties(headers)
                            Log.d(TAG, "playSong: 内置插件兜底成功: ${builtinResult.url.take(80)}")
                            com.yindong.music.data.api.MusicUrlResult(url = builtinResult.url)
                        } else {
                            serverResult // 返回原始错误
                        }
                    } else {
                        serverResult
                    }
                }
                if (requestToken != playRequestToken) {
                    Log.d(TAG, "playSong ignored stale url result: token=$requestToken latest=$playRequestToken")
                    return@launch
                }
                if (result.url.isNullOrEmpty()) {
                    Log.w(TAG, "playSong url empty: song=${song.title}, err=${result.error}")
                    playError = result.error ?: "获取播放链接失败"
                    isBuffering = false
                    return@launch
                }
                val mediaItem = MediaItem.Builder()
                    .setUri(result.url)
                    .setMediaId(song.platformId)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(song.title)
                            .setArtist(song.artist)
                            .setAlbumTitle(song.album)
                            .setArtworkUri(song.coverUrl.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) })
                            .build()
                    )
                    .build()
                player.setMediaItem(mediaItem)
                player.prepare()
                player.play()
                Log.d(TAG, "playSong player started: title=${song.title}, idx=$currentIndex, queueSize=${playlist.size}")

                addToRecent(song)
                prefetchNextSongUrl()
                if (!serviceStarted) {
                    startPlaybackService()
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放失败", e)
                playError = "播放失败: ${e.message}"
                isBuffering = false
            }
        }
    }

    /** 预取下一首歌的播放URL (后台预热缓存, 切歌时零等待) */
    private fun prefetchNextSongUrl() {
        if (playlist.size <= 1) return
        val nextIdx = (currentIndex + 1) % playlist.size
        val nextSong = playlist.getOrNull(nextIdx) ?: return
        viewModelScope.launch {
            try {
                MusicApiService.fetchMusicUrl(
                    nextSong.platform, nextSong.platformId,
                    selectedQuality.name.lowercase()
                )
                Log.d(TAG, "prefetch ok: ${nextSong.title}")
            } catch (_: Exception) { /* 预取失败不影响当前播放 */ }
        }
    }

    private fun startPlaybackService() {
        try {
            val intent = Intent(getApplication(), MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplication<Application>().startForegroundService(intent)
            } else {
                getApplication<Application>().startService(intent)
            }
            serviceStarted = true
        } catch (e: Exception) {
            Log.e(TAG, "启动播放服务失败", e)
        }
    }

    /** 歌曲播放完毕后自动切下一首（根据播放模式） */
    private fun autoPlayNext() {
        if (playlist.isEmpty()) return
        when (playMode) {
            PlayMode.SINGLE -> {
                // 单曲循环：重新播放当前歌曲
                currentSong?.let { playSong(it) }
            }
            PlayMode.LOOP -> {
                currentIndex = (currentIndex + 1) % playlist.size
                playlist.getOrNull(currentIndex)?.let { playSong(it) }
            }
            PlayMode.SHUFFLE -> {
                if (playlist.size <= 1) {
                    currentSong?.let { playSong(it) }
                } else {
                    var next = currentIndex
                    while (next == currentIndex) {
                        next = (0 until playlist.size).random()
                    }
                    currentIndex = next
                    playlist.getOrNull(currentIndex)?.let { playSong(it) }
                }
            }
        }
    }

    /** 将歌曲插入到当前播放位置的下一首 (不立即播放) */
    fun playNextInQueue(song: Song) {
        // 如果已在队列中，先移除再插入
        val existing = playlist.indexOfFirst { it.platformId == song.platformId && it.platform == song.platform }
        val mutable = playlist.toMutableList()
        if (existing >= 0) {
            mutable.removeAt(existing)
            if (existing <= currentIndex && currentIndex > 0) currentIndex--
        }
        val insertAt = (currentIndex + 1).coerceAtMost(mutable.size)
        mutable.add(insertAt, song)
        playlist = mutable
        showToast("\u5df2\u6dfb\u52a0\u5230\u4e0b\u4e00\u9996\u64ad\u653e")
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0) {
        playlist = songs
        currentIndex = startIndex
        songs.getOrNull(startIndex)?.let { playSong(it) }
    }

    fun togglePlayPause() {
        if (playerReleased) return
        if (player.isPlaying) {
            player.pause()
        } else {
            when (player.playbackState) {
                Player.STATE_ENDED -> {
                    // 播放完毕，回到起点重新播放
                    player.seekTo(0)
                    player.play()
                }
                Player.STATE_IDLE -> {
                    // 播放器已停止/未准备，重新播放当前歌曲
                    currentSong?.let { playSong(it) }
                }
                else -> {
                    player.play()
                }
            }
        }
    }

    fun playNext() {
        if (playlist.isEmpty()) return
        if (playlist.size == 1) {
            showToast("当前只有一首歌")
            return
        }
        val oldIndex = currentIndex
        currentIndex = when (playMode) {
            // 手动点击“下一首”时，单曲循环也应切换到下一首
            PlayMode.LOOP, PlayMode.SINGLE -> (currentIndex + 1) % playlist.size
            PlayMode.SHUFFLE -> {
                var next = currentIndex
                while (next == currentIndex && playlist.size > 1) {
                    next = (0 until playlist.size).random()
                }
                next
            }
        }
        Log.d(TAG, "playNext: mode=$playMode, oldIndex=$oldIndex, newIndex=$currentIndex, queueSize=${playlist.size}")
        playlist.getOrNull(currentIndex)?.let { playSong(it) }
    }

    fun playPrevious() {
        if (playlist.isEmpty()) return
        if (playlist.size == 1) {
            showToast("当前只有一首歌")
            return
        }
        val oldIndex = currentIndex
        currentIndex = when (playMode) {
            // 手动点击“上一首”时，单曲循环也应切换到上一首
            PlayMode.LOOP, PlayMode.SINGLE -> (currentIndex - 1 + playlist.size) % playlist.size
            PlayMode.SHUFFLE -> {
                var prev = currentIndex
                while (prev == currentIndex && playlist.size > 1) {
                    prev = (0 until playlist.size).random()
                }
                prev
            }
        }
        Log.d(TAG, "playPrevious: mode=$playMode, oldIndex=$oldIndex, newIndex=$currentIndex, queueSize=${playlist.size}")
        playlist.getOrNull(currentIndex)?.let { playSong(it) }
    }

    fun seekTo(progress: Float) {
        if (playerReleased) return
        this.progress = progress
        val position = (progress * player.duration).toLong()
        player.seekTo(position)
    }

    fun changePlayMode() {
        playMode = when (playMode) {
            PlayMode.LOOP -> PlayMode.SINGLE
            PlayMode.SINGLE -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.LOOP
        }
        LocalStorage.savePlayMode(playMode.name)
    }

    // ── 音质 ──
    fun changeQuality(quality: MusicApiConfig.Quality) {
        selectedQuality = quality
        LocalStorage.saveQuality(quality.name)
        currentSong?.let { playSong(it) }
    }

    // ── 主题 ──
    fun toggleDarkMode() {
        isDarkMode = !isDarkMode
        LocalStorage.saveDarkMode(isDarkMode)
    }

    // ── 开发者模式 ──
    fun toggleDevMode() {
        isDevMode = !isDevMode
        LocalStorage.saveDevMode(isDevMode)
    }

    // ── 歌词 ──
    private fun loadLyrics(song: Song, playToken: Long? = null) {
        val token = playToken ?: ++lyricsRequestToken
        if (playToken != null) {
            lyricsRequestToken = playToken
        }
        val platform = song.platform
        val songId = song.platformId

        viewModelScope.launch {
            Log.d(TAG, "loadLyrics start: song=${song.title}, platform=$platform, songId=$songId, token=$token")
            var fetchedLyrics: List<LyricLine> = emptyList()
            var tlyricRaw: String = ""

            fun isStale() = token != lyricsRequestToken

            try {
                // ① 本地最快: 搜索结果自带的 lrcText (零网络)
                if (song.lrcText.isNotBlank()) {
                    try {
                        val parsed = MusicApiService.parseLrc(song.lrcText)
                        if (parsed.isNotEmpty()) {
                            fetchedLyrics = parsed
                            lyrics = fetchedLyrics
                            currentLyricIndex = 0
                            Log.d(TAG, "loadLyrics ① lrcText: ${parsed.size} lines")
                        }
                    } catch (_: Exception) { /* ignore */ }
                }
                if (isStale()) return@launch

                // ② 插件模式: 调用插件获取歌词+翻试 (8s超时)
                if (apiMode == "lx_plugin" && lxSelectedSource.isNotBlank()) {
                    try {
                        val pluginResult = withTimeoutOrNull(8000L) {
                            val effectiveSource = effectiveLxSource(song)
                            // 优先用原始插件，保证 musicInfo 格式匹配
                            val pluginIdForLyric = song.lxPluginId.ifBlank { lxSelectedPluginId }
                            lxPluginManager.lyric(pluginIdForLyric, effectiveSource, song, lxTimeoutMs)
                        }
                        if (isStale()) return@launch
                        if (pluginResult != null && pluginResult.lyric.isNotBlank()) {
                            val parsed = MusicApiService.parseLrc(pluginResult.lyric)
                            if (parsed.isNotEmpty()) {
                                fetchedLyrics = parsed
                                tlyricRaw = pluginResult.tlyric
                                lyrics = fetchedLyrics
                                currentLyricIndex = 0
                                Log.d(TAG, "loadLyrics ② plugin: ${parsed.size} lines")
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "loadLyrics ② plugin failed: ${e.message}")
                    }
                }
                if (isStale()) return@launch

                // ③ 直接调用平台API获取歌词 (不经服务器, 更快更稳, 8s超时)
                if (fetchedLyrics.isEmpty() && songId.isNotBlank()) {
                    try {
                        Log.d(TAG, "loadLyrics ③ direct API: platform=$platform, songId=$songId")
                        val directLyrics = withTimeoutOrNull(8000L) {
                            MusicApiService.fetchLyricsDirect(platform, songId)
                        } ?: emptyList()
                        if (isStale()) return@launch
                        if (directLyrics.isNotEmpty()) {
                            fetchedLyrics = directLyrics
                            lyrics = fetchedLyrics
                            currentLyricIndex = 0
                            Log.d(TAG, "loadLyrics ③ direct: ${directLyrics.size} lines")
                        } else {
                            Log.d(TAG, "loadLyrics ③ direct returned empty")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "loadLyrics ③ direct failed: ${e.message}")
                    }
                }
                if (isStale()) return@launch

                // ④ 服务器歌词API备用 (5s超时, 服务器可能慢)
                if (fetchedLyrics.isEmpty() && songId.isNotBlank()) {
                    try {
                        Log.d(TAG, "loadLyrics ④ server API: platform=$platform")
                        val serverLyrics = withTimeoutOrNull(5000L) {
                            MusicApiService.fetchLyrics(platform, songId)
                        } ?: emptyList()
                        if (isStale()) return@launch
                        if (serverLyrics.isNotEmpty()) {
                            fetchedLyrics = serverLyrics
                            lyrics = fetchedLyrics
                            currentLyricIndex = 0
                            Log.d(TAG, "loadLyrics ④ server: ${serverLyrics.size} lines")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "loadLyrics ④ server failed: ${e.message}")
                    }
                }
                if (isStale()) return@launch

                // ⑤ 最后兵器: 搜索+直接拉取歌词 (10s总超时)
                if (fetchedLyrics.isEmpty()) {
                    try {
                        withTimeoutOrNull(10000L) {
                            val query = "${song.title} ${song.artist}".trim()
                            Log.d(TAG, "loadLyrics ⑤ search fallback: query=$query")
                            val searchResults = MusicApiService.searchByKeyword(query)
                            if (isStale()) return@withTimeoutOrNull
                            for (sr in searchResults.take(3)) {
                                val titleMatch = sr.title.contains(song.title, true) ||
                                        song.title.contains(sr.title, true)
                                if (!titleMatch) continue
                                if (sr.lrcText.isNotBlank()) {
                                    val parsed = MusicApiService.parseLrc(sr.lrcText)
                                    if (parsed.isNotEmpty()) { fetchedLyrics = parsed; break }
                                }
                                // 直接拉取而不走服务器
                                if (sr.platformId.isNotBlank() && sr.platform.isNotBlank()) {
                                    val srLyrics = MusicApiService.fetchLyricsDirect(sr.platform, sr.platformId)
                                    if (srLyrics.isNotEmpty()) { fetchedLyrics = srLyrics; break }
                                }
                                if (isStale()) return@withTimeoutOrNull
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "loadLyrics ⑤ search fallback failed: ${e.message}")
                    }
                }
                if (isStale()) return@launch

                // 合并翻译歌词
                if (tlyricRaw.isNotBlank() && fetchedLyrics.isNotEmpty()) {
                    try {
                        val tmap = MusicApiService.parseLrcToMap(tlyricRaw)
                        if (tmap.isNotEmpty()) {
                            fetchedLyrics = MusicApiService.mergeLyricTranslation(fetchedLyrics, tmap)
                            Log.d(TAG, "合并翻译歌词: ${tmap.size} 行")
                        }
                    } catch (_: Exception) { /* ignore */ }
                }

                if (isStale()) {
                    Log.d(TAG, "loadLyrics stale at end: token=$token latest=$lyricsRequestToken")
                    return@launch
                }
                lyrics = fetchedLyrics
                currentLyricIndex = 0
                Log.d(TAG, "loadLyrics done: song=${song.title}, lines=${fetchedLyrics.size}")

            } catch (e: Exception) {
                Log.e(TAG, "loadLyrics unexpected error: ${e.message}", e)
            }
        }
    }

    private fun updateLyricIndex() {
        if (lyrics.isEmpty()) return
        val currentTime = player.currentPosition
        currentLyricIndex = lyrics.indexOfLast { it.timeMs <= currentTime }.coerceAtLeast(0)
    }

    private fun updateFloatingLyrics() {
        if (!floatingLyricsEnabled) return
        val curText = lyrics.getOrNull(currentLyricIndex)?.text ?: ""
        val nxtText = lyrics.getOrNull(currentLyricIndex + 1)?.text ?: ""
        floatingLyricsText = curText
        // 同步到悬浮歌词服务
        FloatingLyricsService.currentLyricText = curText
        FloatingLyricsService.nextLyricText = nxtText
        FloatingLyricsService.isPlaying = isPlaying
        FloatingLyricsService.lyricColor = floatingLyricsColor
        FloatingLyricsService.lyricSize = floatingLyricSize.toFloat()
    }

    fun toggleFloatingLyrics() {
        floatingLyricsEnabled = !floatingLyricsEnabled
        if (floatingLyricsEnabled) {
            if (!canDrawOverlays()) {
                floatingLyricsEnabled = false
                showToast("请先授予悬浮窗权限")
                openOverlayPermissionSettings()
                return
            }
            // 设置播放控制回调
            FloatingLyricsService.onPlayPause = { togglePlayPause() }
            FloatingLyricsService.onPrevious = { playPrevious() }
            FloatingLyricsService.onNext = { playNext() }
            try {
                val intent = Intent(getApplication(), FloatingLyricsService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplication<Application>().startForegroundService(intent)
                } else {
                    getApplication<Application>().startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "启动悬浮歌词失败", e)
                floatingLyricsEnabled = false
                showToast("悬浮歌词启动失败")
            }
        } else {
            FloatingLyricsService.onPlayPause = null
            FloatingLyricsService.onPrevious = null
            FloatingLyricsService.onNext = null
            try {
                getApplication<Application>().stopService(
                    Intent(getApplication(), FloatingLyricsService::class.java)
                )
            } catch (e: Exception) {
                Log.e(TAG, "停止悬浮歌词失败", e)
            }
        }
    }

    private fun canDrawOverlays(): Boolean {
        val app = getApplication<Application>()
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(app)
    }

    private fun openOverlayPermissionSettings() {
        val app = getApplication<Application>()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${app.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            app.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "打开悬浮窗权限设置失败", e)
        }
    }

    fun changeFloatingLyricsColor(color: Int) {
        floatingLyricsColor = color
        LocalStorage.saveFloatingLyricColor(color)
    }

    fun changeHighlightLyricColor(color: Int) {
        highlightLyricColor = color
        LocalStorage.saveLyricCurrentColor(color)
    }

    fun changeNormalLyricColor(color: Int) {
        normalLyricColor = color
        LocalStorage.saveLyricNormalColor(color)
    }

    fun changePlayerLyricSize(size: Int) {
        playerLyricSize = size
        LocalStorage.saveLyricFontSize(size)
    }

    fun changeFloatingLyricSize(size: Int) {
        floatingLyricSize = size
        LocalStorage.saveFloatingLyricSize(size)
    }

    /** Check if the currently selected plugin+source supports the given action. */
    private fun lxSupportsAction(action: String): Boolean {
        if (lxSelectedSource.isBlank()) return false
        if (lxSelectedSource == LX_SOURCE_ALL) {
            // "全部" mode: supported if ANY enabled plugin's enabled source supports the action
            return lxPlugins.any { plugin ->
                isPluginEnabled(plugin.id) && plugin.sources.any { src ->
                    isSourceEnabled(plugin.id, src) &&
                        lxPluginManager.sourceSupportsAction(plugin.id, src, action)
                }
            }
        }
        if (lxSelectedPluginId.isBlank()) return false
        return lxPluginManager.sourceSupportsAction(lxSelectedPluginId, lxSelectedSource, action)
    }

    /**
     * Map a display-name platform (e.g. "QQ音乐") back to a server API platform key (e.g. "qq").
     * If the value is already an API key it is returned as-is.
     */
    private fun resolveApiPlatform(platform: String): String {
        return when (platform) {
            "QQ音乐" -> "qq"
            "网易云", "网易云音乐" -> "netease"
            "酷我音乐" -> "kuwo"
            "酷狗音乐" -> "kugou"
            "咪咕音乐" -> "migu"
            "抖音", "汽水音乐" -> "douyin"
            else -> platform // already an API key or unknown
        }
    }

    /**
     * Map a display-name platform (e.g. "QQ音乐") to a LX plugin source key (e.g. "tx").
     * Returns empty string if no mapping found.
     */
    private fun platformToLxSource(platform: String): String {
        return when (platform) {
            "QQ音乐" -> "tx"
            "网易云", "网易云音乐" -> "wy"
            "酷我音乐" -> "kw"
            "酷狗音乐" -> "kg"
            "咪咕音乐" -> "mg"
            else -> ""
        }
    }

    /** Resolve the effective LX source key for a song: prefer stored key, then infer from platform name, finally fallback to global selection. */
    private fun effectiveLxSource(song: Song): String {
        return song.lxSourceKey.ifBlank { platformToLxSource(song.platform).ifBlank { lxSelectedSource } }
    }

    // ── 搜索 ──
    fun search(keyword: String, page: Int = 1) {
        if (keyword.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            try {
                if (page == 1) {
                    searchResults = emptyList()
                    currentSearchPage = 1
                }
                val results = if (apiMode == "lx_plugin" && lxSelectedSource.isNotBlank() && lxSupportsAction("search")) {
                    if (lxSelectedSource == LX_SOURCE_ALL) {
                        // Search ALL sources in parallel and merge results
                        searchAllSources(keyword)
                    } else {
                        lxPluginManager.search(lxSelectedPluginId, lxSelectedSource, keyword, lxTimeoutMs)
                    }
                } else {
                    if (apiMode == "lx_plugin" && !lxSupportsAction("search")) {
                        Log.d(TAG, "当前插件不支持search，回退到官方API")
                    }
                    MusicApiService.searchByKeyword(keyword)
                }
                searchResults = if (page == 1) results else searchResults + results
                hasMoreResults = results.size >= searchPageSize
                currentSearchPage = page
                hasSearched = true
                addSearchHistory(keyword)
            } catch (e: Exception) {
                Log.e(TAG, "搜索失败", e)
            } finally {
                isSearching = false
            }
        }
    }

    /**
     * 搜索音乐（供UI调用）
     */
    fun searchMusic(keyword: String) {
        search(keyword, page = 1)
    }

    /**
     * Search all enabled plugins' enabled sources in parallel and merge/interleave results.
     * Each song retains its own lxSourceKey so playback uses the correct source.
     * Uses per-source timeout (8s) so one slow source doesn't block everything.
     */
    private suspend fun searchAllSources(keyword: String): List<Song> = coroutineScope {
        val perSourceTimeout = 8000L
        // Collect (pluginId, source) pairs from all enabled plugins & sources
        val pluginSourcePairs = lxPlugins
            .filter { isPluginEnabled(it.id) }
            .flatMap { plugin -> plugin.sources.filter { isSourceEnabled(plugin.id, it) }.map { plugin.id to it } }
        Log.d(TAG, "searchAllSources: keyword=$keyword, pairs=${pluginSourcePairs.map { "${it.first.take(8)}:${it.second}" }}")
        val jobs = pluginSourcePairs.map { (pluginId, source) ->
            async(Dispatchers.IO) {
                try {
                    if (!lxPluginManager.sourceSupportsAction(pluginId, source, "search")) {
                        return@async emptyList<Song>()
                    }
                    withTimeoutOrNull(perSourceTimeout) {
                        lxPluginManager.search(pluginId, source, keyword, perSourceTimeout)
                    } ?: run {
                        Log.w(TAG, "searchAllSources: plugin=${pluginId.take(8)} source=$source TIMEOUT")
                        emptyList<Song>()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "searchAllSources: plugin=${pluginId.take(8)} source=$source failed: ${e.message}")
                    emptyList<Song>()
                }
            }
        }
        val allResults = jobs.awaitAll()
        // Interleave results: take turns from each source for variety
        val iterators = allResults.filter { it.isNotEmpty() }.map { it.iterator() }.toMutableList()
        val merged = mutableListOf<Song>()
        while (iterators.isNotEmpty()) {
            val it = iterators.iterator()
            while (it.hasNext()) {
                val iter = it.next()
                if (iter.hasNext()) {
                    merged.add(iter.next())
                } else {
                    it.remove()
                }
            }
        }
        Log.d(TAG, "searchAllSources: merged ${merged.size} results from ${pluginSourcePairs.size} plugin-source pairs")
        merged
    }

    fun loadMoreSearchResults(keyword: String) {
        if (!isSearching && hasMoreResults) {
            search(keyword, currentSearchPage + 1)
        }
    }

    fun clearSearchResults() {
        searchResults = emptyList()
        currentSearchPage = 1
        hasMoreResults = true
        hasSearched = false
    }

    private fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        searchHistory = listOf(keyword) + searchHistory.filter { it != keyword }.take(19)
        LocalStorage.saveSearchHistory(searchHistory)
    }

    fun clearSearchHistory() {
        searchHistory = emptyList()
        LocalStorage.saveSearchHistory(emptyList())
    }

    fun getSearchSuggestionsFromHistory(keyword: String): List<String> {
        if (keyword.isBlank()) return emptyList()
        // 使用本地搜索历史做简单联想
        return searchHistory.filter {
            it.contains(keyword, ignoreCase = true)
        }.take(10)
    }

    // ── 歌单 ──
    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        val newPlaylist = Playlist(
            id = System.currentTimeMillis(),
            name = name,
            coverUrl = "",
            songCount = 0,
            playCount = 0,
            creator = userName
        )
        myPlaylists = myPlaylists + newPlaylist
        LocalStorage.savePlaylists(myPlaylists)
    }

    fun deletePlaylist(playlistId: Long) {
        myPlaylists = myPlaylists.filter { it.id != playlistId }
        LocalStorage.savePlaylists(myPlaylists)
    }

    fun addToPlaylist(playlistId: Long, song: Song) {
        val playlist = myPlaylists.find { it.id == playlistId } ?: return
        val mergedSongs = (playlist.songs + song).distinctBy { "${it.platformId}:${it.platform}" }
        val updatedPlaylist = playlist.copy(
            songs = mergedSongs,
            songCount = mergedSongs.size,
            coverUrl = playlist.coverUrl.ifBlank { mergedSongs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty() },
        )
        myPlaylists = myPlaylists.map { if (it.id == playlistId) updatedPlaylist else it }
        LocalStorage.savePlaylists(myPlaylists)
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        val playlist = myPlaylists.find { it.id == playlistId } ?: return
        val remainingSongs = playlist.songs.filter { it.id != songId }
        val updatedPlaylist = playlist.copy(
            songs = remainingSongs,
            songCount = remainingSongs.size,
            coverUrl = remainingSongs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl.orEmpty(),
        )
        myPlaylists = myPlaylists.map { if (it.id == playlistId) updatedPlaylist else it }
        LocalStorage.savePlaylists(myPlaylists)
    }
    private fun normalizeCoverUrl(raw: String?): String {
        var value = raw?.trim().orEmpty()
        if (value.isEmpty()) return ""
        if (value.equals("null", ignoreCase = true) || value.equals("undefined", ignoreCase = true)) return ""
        value = value.replace("\\/", "/").replace("&amp;", "&").trim()
        if (value.startsWith("//")) value = "https:$value"
        return if (
            value.startsWith("http://", ignoreCase = true) ||
            value.startsWith("https://", ignoreCase = true) ||
            value.startsWith("data:image", ignoreCase = true)
        ) value else ""
    }

    private fun firstCoverUrl(vararg candidates: String?): String {
        for (candidate in candidates) {
            val normalized = normalizeCoverUrl(candidate)
            if (normalized.isNotEmpty()) return normalized
        }
        return ""
    }

    fun importPlaylist(json: String): Boolean {
        return try {
            val jsonObj = org.json.JSONObject(json)
            val songsArr = jsonObj.optJSONArray("songs")
            val songs = mutableListOf<Song>()
            if (songsArr != null) {
                for (i in 0 until songsArr.length()) {
                    val s = songsArr.getJSONObject(i)
                    songs.add(Song(
                        id = s.optLong("id", System.currentTimeMillis() + i),
                        title = s.optString("title", ""),
                        artist = s.optString("artist", "未知"),
                        album = s.optString("album", ""),
                        coverUrl = firstCoverUrl(
                            s.optString("coverUrl", ""),
                            s.optString("cover", ""),
                            s.optString("picUrl", ""),
                            s.optString("pic", ""),
                            s.optString("imgUrl", ""),
                            s.optString("img", ""),
                        ),
                        platform = s.optString("platform", ""),
                        platformId = s.optString("platformId", ""),
                    ))
                }
            }
            val playlist = Playlist(
                id = jsonObj.optLong("id", System.currentTimeMillis()),
                name = jsonObj.optString("name", "未知歌单"),
                coverUrl = firstCoverUrl(
                    jsonObj.optString("coverUrl", ""),
                    jsonObj.optString("cover", ""),
                    jsonObj.optString("picUrl", ""),
                    songs.firstOrNull { it.coverUrl.isNotBlank() }?.coverUrl,
                ),
                songCount = songs.size,
                playCount = jsonObj.optLong("playCount", 0),
                creator = jsonObj.optString("creator", ""),
                songs = songs,
            )
            myPlaylists = myPlaylists + playlist
            LocalStorage.savePlaylists(myPlaylists)
            true
        } catch (e: Exception) {
            Log.e(TAG, "导入歌单失败", e)
            false
        }
    }

    fun exportAllPlaylists(): String {
        return try {
            val arr = org.json.JSONArray()
            myPlaylists.forEach { pl ->
                arr.put(org.json.JSONObject().apply {
                    put("id", pl.id)
                    put("name", pl.name)
                    put("coverUrl", pl.coverUrl)
                    put("songCount", pl.songCount)
                    val songsArr = org.json.JSONArray()
                    pl.songs.forEach { s ->
                        songsArr.put(org.json.JSONObject().apply {
                            put("id", s.id)
                            put("title", s.title)
                            put("artist", s.artist)
                            put("album", s.album)
                            put("coverUrl", s.coverUrl)
                            put("platform", s.platform)
                            put("platformId", s.platformId)
                        })
                    }
                    put("songs", songsArr)
                })
            }
            arr.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "导出歌单失败", e)
            "[]"
        }
    }

    // ── 收藏 ──
    fun toggleFavorite(song: Song) {
        if (favoriteSongs.any { it.id == song.id }) {
            favoriteSongs = favoriteSongs.filter { it.id != song.id }
        } else {
            favoriteSongs = favoriteSongs + song
        }
        LocalStorage.saveFavorites(favoriteSongs)
    }

    fun isFavorite(songId: Long): Boolean {
        return favoriteSongs.any { it.id == songId }
    }

    private fun addToRecent(song: Song) {
        recentSongs = listOf(song) + recentSongs.filter { it.id != song.id }.take(99)
        LocalStorage.savePlayHistory(recentSongs)
    }

    fun clearRecentSongs() {
        recentSongs = emptyList()
        LocalStorage.savePlayHistory(recentSongs)
    }

    fun exportFavorites(): String {
        return try {
            val arr = org.json.JSONArray()
            favoriteSongs.forEach { s ->
                arr.put(org.json.JSONObject().apply {
                    put("id", s.id); put("title", s.title); put("artist", s.artist)
                    put("album", s.album); put("coverUrl", s.coverUrl)
                })
            }
            arr.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "导出收藏失败", e)
            "[]"
        }
    }

    fun importFavorites(json: String): Boolean {
        return try {
            val arr = org.json.JSONArray(json)
            val songs = (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                Song(
                    id = obj.optLong("id", System.currentTimeMillis() + i),
                    title = obj.optString("title", ""),
                    artist = obj.optString("artist", ""),
                    album = obj.optString("album", ""),
                    coverUrl = firstCoverUrl(
                        obj.optString("coverUrl", ""),
                        obj.optString("cover", ""),
                        obj.optString("picUrl", ""),
                        obj.optString("pic", ""),
                    ),
                )
            }
            favoriteSongs = (favoriteSongs + songs).distinctBy { it.id }
            LocalStorage.saveFavorites(favoriteSongs)
            true
        } catch (e: Exception) {
            Log.e(TAG, "导入收藏失败", e)
            false
        }
    }

    // ── 登录 ──
    fun login(token: String, name: String, id: String) {
        userToken = token
        userName = name
        userId = id
        isLoggedIn = true
        LocalStorage.saveUserAuth(token, name, id.toIntOrNull() ?: 0)
    }

    fun logout() {
        userToken = ""
        userName = ""
        userId = ""
        isLoggedIn = false
        LocalStorage.clearUserAuth()
    }

    // ── API配置 ──
    fun updateApiMode(mode: String) {
        apiMode = mode
        LocalStorage.saveApiMode(mode)
    }

    fun updateApiHost(host: String) {
        apiHost = host
        LocalStorage.saveApiHost(host)
    }

    fun updateApiKey(platform: String, key: String) {
        when (platform) {
            "qq_music" -> { qqMusicApi = key; LocalStorage.saveQQMusicApiKey(key) }
            "netease" -> { neteaseApi = key; LocalStorage.saveNeteaseApiKey(key) }
            "kuwo" -> { kuwoApi = key; LocalStorage.saveKuwoApiKey(key) }
            "migu" -> { miguApi = key; LocalStorage.saveMiguApiKey(key) }
            "kugou" -> { kugouApi = key; LocalStorage.saveKugouApiKey(key) }
            "douyin" -> { douyinApi = key; LocalStorage.saveDouyinApiKey(key) }
        }
    }

    fun updateQQCookie(cookie: String) {
        qqCookie = cookie
        LocalStorage.saveQQCookie(cookie)
    }



    // ── 崩溃日志 ──
    fun addCrashLog(tag: String, throwable: Throwable) {
        CrashLogManager.logException(tag, throwable)
        crashLogs = CrashLogManager.getLogFiles()
    }

    // ── 均衡器 ──
    private fun initEqualizer() {
        try {
            equalizerEffect = Equalizer(0, player.audioSessionId)
            bassBoostEffect = BassBoost(0, player.audioSessionId)
            virtualizerEffect = Virtualizer(0, player.audioSessionId)
            reverbEffect = EnvironmentalReverb(0, player.audioSessionId)
            loudnessEnhancerEffect = LoudnessEnhancer(player.audioSessionId)

            equalizerBands = (0 until (equalizerEffect?.numberOfBands ?: 0)).map { i ->
                val freq = equalizerEffect?.getCenterFreq(i.toShort())?.div(1000) ?: 0
                freq to 0
            }
            applyPreset(currentPreset)
            toggleEqualizer(equalizerEnabled)

        } catch (e: Exception) {
            Log.e(TAG, "初始化均衡器失败", e)
        }
    }

    fun setEqualizerBand(band: Int, level: Int) {
        try {
            val clamped = level.coerceIn(-15, 15)
            equalizerEffect?.setBandLevel(band.toShort(), (clamped * 100).toShort())
            equalizerBands = equalizerBands.mapIndexed { index, pair ->
                if (index == band) pair.first to clamped else pair
            }
        } catch (e: Exception) {
            Log.e(TAG, "设置均衡器失败", e)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        equalizerEnabled = enabled
        try {
            equalizerEffect?.enabled = enabled
            bassBoostEffect?.enabled = enabled && bassBoostStrength > 0
            virtualizerEffect?.enabled = enabled && virtualizerStrength > 0
            reverbEffect?.enabled = enabled && reverbLevel > 0
            loudnessEnhancerEffect?.enabled = enabled && loudnessGain > 0
        } catch (e: Exception) {
            Log.e(TAG, "切换均衡器失败", e)
        }
    }

    fun toggleBassBoost(enabled: Boolean) {
        bassBoostEnabled = enabled
        if (!enabled) updateBassBoost(0)
        else updateBassBoost(bassBoostStrength.coerceAtLeast(1))
    }

    fun applyBassBoostStrength(strength: Int) {
        val normalized = if (strength > 100) strength / 10 else strength
        updateBassBoost(normalized)
    }

    fun toggleVirtualizer(enabled: Boolean) {
        virtualizerEnabled = enabled
        if (!enabled) updateVirtualizer(0)
        else updateVirtualizer(virtualizerStrength.coerceAtLeast(1))
    }

    fun applyVirtualizerStrength(strength: Int) {
        val normalized = if (strength > 100) strength / 10 else strength
        updateVirtualizer(normalized)
    }

    // ── 定时停止 ──
    fun setSleepTimer(minutes: Int) {
        sleepTimerMinutes = minutes
        sleepTimerEnabled = minutes > 0
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                player.pause()
                sleepTimerEnabled = false
                sleepTimerMinutes = 0
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerEnabled = false
        sleepTimerMinutes = 0
    }

    // ── 播放队列管理 ──
    fun getQueue(): List<Song> = playlist

    fun removeFromQueue(song: Song) {
        val removedIndex = playlist.indexOfFirst { it.platformId == song.platformId && it.platform == song.platform }
        if (removedIndex < 0) return
        playlist = playlist.filterIndexed { idx, _ -> idx != removedIndex }
        // 调整 currentIndex 保持指向正确的歌曲
        if (playlist.isEmpty()) {
            currentIndex = 0
        } else if (removedIndex < currentIndex) {
            currentIndex = (currentIndex - 1).coerceAtLeast(0)
        } else if (removedIndex == currentIndex) {
            currentIndex = currentIndex.coerceAtMost(playlist.lastIndex)
        }
    }

    fun clearQueue() {
        player.stop()
        playlist = emptyList()
        currentIndex = 0
        currentSong = null
        isPlaying = false
    }

    // ── 播放历史 ──
    // playHistory 直接引用 recentSongs，避免两个字段不同步
    val playHistory: List<Song> get() = recentSongs

    fun removeFromHistory(song: Song) {
        recentSongs = recentSongs.filter { it.id != song.id }
        LocalStorage.savePlayHistory(recentSongs)
    }

    fun playAllFromList(songs: List<Song>) {
        if (songs.isNotEmpty()) {
            playPlaylist(songs, 0)
        }
    }

    // ── 收藏/喜欢 ──
    fun toggleLike(song: Song) {
        toggleFavorite(song)
    }

    // ── 定时停止（兼容旧接口）──
    val isSleepTimerRunning: Boolean
        get() = sleepTimerEnabled

    val sleepTimerMs: Long
        get() = if (sleepTimerEnabled) sleepTimerMinutes * 60 * 1000L else 0L

    fun startSleepTimer(minutes: Int) {
        setSleepTimer(minutes)
    }

    // ── 下载页面 ──
    fun openDownloadPage() {
        val url = downloadPageUrl.trim()
        if (url.isEmpty()) { showToast("暂未配置下载地址"); return }
        if (!url.startsWith("http://", ignoreCase = true) && !url.startsWith("https://", ignoreCase = true)) {
            showToast("下载地址格式不正确")
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            getApplication<Application>().startActivity(intent)
        } catch (e: Exception) {
            Log.e("MusicViewModel", "打开下载页面失败", e)
            showToast("打开链接失败: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ 插件音质定义 ═══
    // ═══════════════════════════════════════════════════════════

    /** LX plugin quality levels — ordered from lowest to highest. */
    enum class LxQuality(val key: String, val displayName: String) {
        Q128K("128k", "128K"),
        Q320K("320k", "320K"),
        FLAC("flac", "FLAC"),
        FLAC24BIT("flac24bit", "Hires 无损24-Bit"),
        WAV("wav", "臻品音质"),
        FLAC32BIT("flac32bit", "臻品音质 2.0"),
        MASTER("master", "臻品母带"),
        ;
        companion object {
            fun fromKey(key: String): LxQuality? = entries.firstOrNull { it.key.equals(key, ignoreCase = true) }
            /** For unknown keys from the plugin, create a display name from the key itself. */
            fun displayFor(key: String): String = fromKey(key)?.displayName ?: key.uppercase()
        }
    }

    /**
     * Get available download qualities for a song, extracted from its pluginRawJson types array.
     * Returns a list of (key, displayName) pairs.
     */
    fun getAvailableQualities(song: Song): List<Pair<String, String>> {
        if (song.pluginRawJson.isBlank()) {
            // No raw JSON — return default qualities
            return listOf(
                "128k" to "128K",
                "320k" to "320K",
                "flac" to "FLAC",
            )
        }
        return try {
            val raw = org.json.JSONObject(song.pluginRawJson)
            val typesArr = raw.optJSONArray("types") ?: return listOf(
                "128k" to "128K",
                "320k" to "320K",
                "flac" to "FLAC",
            )
            val result = mutableListOf<Pair<String, String>>()
            for (i in 0 until typesArr.length()) {
                val typeObj = typesArr.optJSONObject(i)
                val key = typeObj?.optString("type")?.takeIf { it.isNotBlank() }
                    ?: typesArr.optString(i).takeIf { it.isNotBlank() }
                    ?: continue
                result.add(key to LxQuality.displayFor(key))
            }
            // Sort by our defined order
            val order = LxQuality.entries.map { it.key }
            result.sortedBy { (k, _) -> order.indexOf(k).let { if (it < 0) 999 else it } }
        } catch (e: Exception) {
            listOf("128k" to "128K", "320k" to "320K", "flac" to "FLAC")
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ 插件音乐下载 ═══
    // ═══════════════════════════════════════════════════════════

    data class DownloadedSong(
        val song: Song,
        val filePath: String,
        val fileSize: Long,
        val platform: String,
        val downloadTime: Long = System.currentTimeMillis(),
    )

    sealed class DownloadState {
        object Idle : DownloadState()
        data class Downloading(val songId: String, val progress: Float = 0f) : DownloadState()
        data class Success(val songTitle: String) : DownloadState()
        data class Error(val message: String) : DownloadState()
    }

    /** Whether a specific song has been downloaded. */
    fun isDownloaded(song: Song): Boolean {
        return downloadedSongs.any { it.song.platformId == song.platformId && it.song.platform == song.platform }
    }

    /**
     * Download a song using the LX plugin.
     * @param song The song to download (defaults to currently playing song)
     * @param qualityKey The LX quality key to download (e.g. "128k", "320k", "flac", "flac24bit").
     *                   If null, uses the currently selected global quality.
     */
    fun downloadSong(song: Song? = null, qualityKey: String? = null) {
        val target = song ?: currentSong
        if (target == null) { showToast("没有可下载的歌曲"); return }
        if (apiMode != "lx_plugin" || lxSelectedSource.isBlank()) {
            showToast("仅支持下载插件音乐，请切换到JS插件模式")
            return
        }
        // Allow download as long as we can determine a valid LX source for this song
        val dlSource = effectiveLxSource(target)
        if (dlSource.isBlank()) {
            showToast("无法确定该歌曲的插件音源，无法下载")
            return
        }
        if (isDownloaded(target)) {
            showToast("已下载过该歌曲")
            return
        }
        if (downloadState is DownloadState.Downloading) {
            showToast("正在下载中，请稍候")
            return
        }
        val downloadDecision = MusicPlaybackGate.evaluate(MusicPlaybackGate.Action.DOWNLOAD)
        if (!downloadDecision.allowed) {
            val message = downloadDecision.reason ?: "当前环境存在风险，已阻止下载"
            Log.w(TAG, "安全策略拦截[DOWNLOAD]: $message")
            downloadState = DownloadState.Error(message)
            showToast(message)
            if (downloadDecision.shouldKillProcess) {
                SecurityGuard.killProcess()
            }
            return
        }

        downloadState = DownloadState.Downloading(target.platformId)
        viewModelScope.launch {
            try {
                // 1. Get the music URL (multi-plugin fallback + quality fallback + server API fallback)
                val lxQuality = qualityKey ?: when (selectedQuality) {
                    MusicApiConfig.Quality.STANDARD -> "128k"
                    MusicApiConfig.Quality.EXHIGH -> "320k"
                    MusicApiConfig.Quality.LOSSLESS -> "flac"
                }
                val effectiveSource = effectiveLxSource(target)
                var downloadUrl = ""
                var downloadHeaders = mapOf<String, String>()

                // ① 多插件尝试当前品质
                val pluginResult = try {
                    tryPluginMusicUrl(effectiveSource, target, lxQuality)
                } catch (_: Exception) { null }
                if (pluginResult != null && pluginResult.url.isNotBlank()) {
                    downloadUrl = pluginResult.url
                    downloadHeaders = pluginResult.headers
                }

                // ② 当前品质失败 → 降级重试
                if (downloadUrl.isBlank()) {
                    val fallbackQualities = listOf("flac", "320k", "128k").filter { it != lxQuality }
                    for (q in fallbackQualities) {
                        val result = try { tryPluginMusicUrl(effectiveSource, target, q) } catch (_: Exception) { null }
                        if (result != null && result.url.isNotBlank()) {
                            downloadUrl = result.url
                            downloadHeaders = result.headers
                            Log.d(TAG, "下载品质降级: $lxQuality → $q")
                            break
                        }
                    }
                }

                // ③ 插件全部失败 → 服务端API兆底
                if (downloadUrl.isBlank()) {
                    val serverQ = when (selectedQuality) {
                        MusicApiConfig.Quality.STANDARD -> "standard"
                        MusicApiConfig.Quality.EXHIGH -> "exhigh"
                        MusicApiConfig.Quality.LOSSLESS -> "lossless"
                    }
                    val serverResult = try {
                        MusicApiService.fetchMusicUrl(target.platform, target.platformId, serverQ)
                    } catch (_: Exception) { null }
                    if (serverResult != null && !serverResult.url.isNullOrEmpty()) {
                        downloadUrl = serverResult.url!!
                        Log.d(TAG, "下载回退服务端API")
                    }
                }

                if (downloadUrl.isBlank()) {
                    downloadState = DownloadState.Error("获取下载链接失败")
                    showToast("获取下载链接失败，请稍后重试")
                    return@launch
                }
                val pluginUrl = downloadUrl
                val pluginHeaders = downloadHeaders

                // 2. Create platform subfolder under download dir
                val platformDir = target.platform.ifBlank { "其他" }
                val baseDir = java.io.File(downloadDir)
                val dir = java.io.File(baseDir, platformDir)
                if (!dir.exists()) dir.mkdirs()

                // 3. Determine file extension from URL or quality
                val ext = when {
                    pluginUrl.contains(".flac", true) -> "flac"
                    pluginUrl.contains(".m4a", true) -> "m4a"
                    pluginUrl.contains(".wav", true) -> "wav"
                    lxQuality == "flac" -> "flac"
                    else -> "mp3"
                }
                val safeTitle = target.title.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
                val safeArtist = target.artist.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(40)
                val fileName = "${safeArtist} - ${safeTitle}.$ext"
                val outputFile = java.io.File(dir, fileName)

                // 4. Download with OkHttp
                withContext(Dispatchers.IO) {
                    val reqBuilder = Request.Builder().url(pluginUrl)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                    pluginHeaders.forEach { (k, v) -> reqBuilder.header(k, v) }
                    val downloadClient = MusicApiService.sharedClient().newBuilder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                        .followRedirects(true)
                        .build()
                    val response = downloadClient.newCall(reqBuilder.build()).execute()
                    if (!response.isSuccessful) throw IllegalStateException("下载失败: HTTP ${response.code}")
                    val body = response.body ?: throw IllegalStateException("下载内容为空")
                    val totalBytes = body.contentLength()
                    outputFile.outputStream().buffered().use { out ->
                        body.byteStream().buffered().use { input ->
                            val buf = ByteArray(8192)
                            var downloaded = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n == -1) break
                                out.write(buf, 0, n)
                                downloaded += n
                                if (totalBytes > 0) {
                                    val prog = (downloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                                    withContext(Dispatchers.Main) {
                                        downloadState = DownloadState.Downloading(target.platformId, prog)
                                    }
                                }
                            }
                        }
                    }
                    response.close()
                }

                // 5. Register in downloaded list
                val entry = DownloadedSong(
                    song = target,
                    filePath = outputFile.absolutePath,
                    fileSize = outputFile.length(),
                    platform = platformDir,
                )
                downloadedSongs = downloadedSongs + entry
                saveDownloadedSongs()
                downloadState = DownloadState.Success(target.title)
                showToast("下载完成: ${target.title}")

            } catch (e: Exception) {
                Log.e(TAG, "下载歌曲失败", e)
                downloadState = DownloadState.Error("下载失败: ${e.message}")
                showToast("下载失败: ${e.message}")
            }
        }
    }

    /** Play a downloaded song directly from its local file. */
    fun playDownloadedSong(entry: DownloadedSong) {
        val file = java.io.File(entry.filePath)
        if (!file.exists()) {
            showToast("文件已被删除")
            downloadedSongs = downloadedSongs.filter { it.filePath != entry.filePath }
            saveDownloadedSongs()
            return
        }
        val localSong = entry.song.copy(directUrl = entry.filePath)
        playSong(localSong)
    }

    /** Delete a downloaded song file and remove from list. */
    fun deleteDownloadedSong(entry: DownloadedSong) {
        try { java.io.File(entry.filePath).delete() } catch (_: Exception) {}
        downloadedSongs = downloadedSongs.filter { it.filePath != entry.filePath }
        saveDownloadedSongs()
        showToast("已删除")
    }

    /** Get downloaded songs grouped by platform for the UI. */
    fun getDownloadsByPlatform(): Map<String, List<DownloadedSong>> {
        return downloadedSongs.groupBy { it.platform }
    }

    /** Get total download size. */
    fun getDownloadTotalSize(): String {
        val total = downloadedSongs.sumOf { it.fileSize }
        return formatFileSize(total)
    }

    fun resetDownloadState() { downloadState = DownloadState.Idle }

    private fun saveDownloadedSongs() {
        try {
            val arr = org.json.JSONArray()
            downloadedSongs.forEach { d ->
                arr.put(org.json.JSONObject().apply {
                    put("title", d.song.title)
                    put("artist", d.song.artist)
                    put("album", d.song.album)
                    put("coverUrl", d.song.coverUrl)
                    put("platform", d.song.platform)
                    put("platformId", d.song.platformId)
                    put("pluginRawJson", d.song.pluginRawJson)
                    put("lxSourceKey", d.song.lxSourceKey)
                    put("filePath", d.filePath)
                    put("fileSize", d.fileSize)
                    put("platformDir", d.platform)
                    put("downloadTime", d.downloadTime)
                })
            }
            LocalStorage.saveString("downloaded_songs", arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "保存下载列表失败", e)
        }
    }

    private fun loadDownloadedSongs() {
        try {
            val json = LocalStorage.loadString("downloaded_songs")
            if (json.isBlank()) return
            val arr = org.json.JSONArray(json)
            val list = mutableListOf<DownloadedSong>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val filePath = obj.optString("filePath", "")
                if (filePath.isBlank() || !java.io.File(filePath).exists()) continue
                list.add(DownloadedSong(
                    song = Song(
                        id = obj.optString("platformId", "").hashCode().toLong(),
                        title = obj.optString("title", ""),
                        artist = obj.optString("artist", ""),
                        album = obj.optString("album", ""),
                        coverUrl = firstCoverUrl(
                            obj.optString("coverUrl", ""),
                            obj.optString("cover", ""),
                            obj.optString("picUrl", ""),
                            obj.optString("pic", ""),
                            obj.optString("imgUrl", ""),
                            obj.optString("img", ""),
                        ),
                        platform = obj.optString("platform", ""),
                        platformId = obj.optString("platformId", ""),
                        pluginRawJson = obj.optString("pluginRawJson", ""),
                        lxSourceKey = obj.optString("lxSourceKey", ""),
                    ),
                    filePath = filePath,
                    fileSize = obj.optLong("fileSize", java.io.File(filePath).length()),
                    platform = obj.optString("platformDir", obj.optString("platform", "其他")),
                    downloadTime = obj.optLong("downloadTime", 0L),
                ))
            }
            downloadedSongs = list
        } catch (e: Exception) {
            Log.e(TAG, "加载下载列表失败", e)
        }
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ 新增设置功能：缓存管理、下载目录、自动更新、音源管理 ═══
    // ═══════════════════════════════════════════════════════════

    // ── 音乐缓存管理 ──
    fun getCacheSize(): String {
        return try {
            val cacheDir = getApplication<Application>().cacheDir
            val externalCacheDir = getApplication<Application>().externalCacheDir
            var size = cacheDir?.let { calculateDirSize(it) } ?: 0L
            size += externalCacheDir?.let { calculateDirSize(it) } ?: 0L
            formatFileSize(size)
        } catch (e: Exception) {
            "未知"
        }
    }

    private fun calculateDirSize(dir: java.io.File): Long {
        var size = 0L
        try {
            dir.listFiles()?.forEach { file ->
                size += if (file.isDirectory) calculateDirSize(file) else file.length()
            }
        } catch (_: Exception) {}
        return size
    }

    private fun formatFileSize(size: Long): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.2f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.2f KB", size / 1024.0)
            else -> "$size B"
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                getApplication<Application>().cacheDir?.let { deleteDir(it) }
                getApplication<Application>().externalCacheDir?.let { deleteDir(it) }
                withContext(Dispatchers.Main) {
                    showToast("缓存已清理")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("清理缓存失败: ${e.message}")
                }
            }
        }
    }

    private fun deleteDir(dir: java.io.File) {
        try {
            dir.listFiles()?.forEach { file ->
                if (file.isDirectory) deleteDir(file)
                else file.delete()
            }
        } catch (_: Exception) {}
    }

    // ── 下载目录 ──
    var customDownloadDir by mutableStateOf(LocalStorage.loadString("custom_download_dir"))
        private set

    val downloadDir: String
        get() {
            val custom = customDownloadDir
            if (custom.isNotBlank()) return custom
            return try {
                val musicDir = getApplication<Application>().getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC)
                musicDir?.absolutePath ?: getApplication<Application>().filesDir.absolutePath + "/Music"
            } catch (_: Exception) {
                "/storage/emulated/0/Music/云音乐"
            }
        }

    fun setDownloadDir(path: String) {
        val trimmed = path.trim()
        customDownloadDir = trimmed
        LocalStorage.saveString("custom_download_dir", trimmed)
        if (trimmed.isNotBlank()) {
            try { java.io.File(trimmed).mkdirs() } catch (_: Exception) {}
            showToast("下载路径已更新")
        } else {
            showToast("已恢复默认下载路径")
        }
    }



    // ── 音源插件管理 ──
    var qqMusicEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("qq_music"))
        private set
    var neteaseEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("netease"))
        private set
    var kuwoEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("kuwo"))
        private set
    var miguEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("migu"))
        private set
    var kugouEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("kugou"))
        private set
    var douyinEnabled by mutableStateOf(LocalStorage.loadSourceEnabled("douyin"))
        private set

    fun toggleQqMusic() {
        qqMusicEnabled = !qqMusicEnabled
        LocalStorage.saveSourceEnabled("qq_music", qqMusicEnabled)
        showToast("QQ音乐 ${if (qqMusicEnabled) "已启用" else "已禁用"}")
    }

    fun toggleNetease() {
        neteaseEnabled = !neteaseEnabled
        LocalStorage.saveSourceEnabled("netease", neteaseEnabled)
        showToast("网易云音乐 ${if (neteaseEnabled) "已启用" else "已禁用"}")
    }

    fun toggleKuwo() {
        kuwoEnabled = !kuwoEnabled
        LocalStorage.saveSourceEnabled("kuwo", kuwoEnabled)
        showToast("酷我音乐 ${if (kuwoEnabled) "已启用" else "已禁用"}")
    }

    fun toggleMigu() {
        miguEnabled = !miguEnabled
        LocalStorage.saveSourceEnabled("migu", miguEnabled)
        showToast("咪咕音乐 ${if (miguEnabled) "已启用" else "已禁用"}")
    }

    fun toggleKugou() {
        kugouEnabled = !kugouEnabled
        LocalStorage.saveSourceEnabled("kugou", kugouEnabled)
        showToast("酷狗音乐 ${if (kugouEnabled) "已启用" else "已禁用"}")
    }

    fun toggleDouyin() {
        douyinEnabled = !douyinEnabled
        LocalStorage.saveSourceEnabled("douyin", douyinEnabled)
        showToast("抖音 ${if (douyinEnabled) "已启用" else "已禁用"}")
    }

    // ═══════════════════════════════════════════════════════════

    // ═══════════════════════════════════════════════════════════
    // ═══ UI 屏幕所需的别名属性和辅助方法 ═══
    // ═══════════════════════════════════════════════════════════

    // ── 属性别名 ──
    val favorites: List<Song> get() = favoriteSongs
    val userPlaylists: List<Playlist> get() = myPlaylists
    val isFloatingLyricsEnabled: Boolean get() = floatingLyricsEnabled
    val lyricCurrentColor: Int get() = highlightLyricColor
    val lyricNormalColor: Int get() = normalLyricColor
    val lyricFontSize: Int get() = playerLyricSize
    val bassBoost: Int get() = bassBoostStrength
    val virtualizer: Int get() = virtualizerStrength
    val reverbWet: Int get() = reverbLevel
    val currentVersion: String get() = "2.5.1"
    var qqPlayApiValue by mutableStateOf(LocalStorage.loadQQPlayApi())
        private set
    var douyinParseApiValue by mutableStateOf(LocalStorage.loadDouyinParseApi())
        private set
    val qqPlayApi: String get() = qqPlayApiValue
    val douyinParseApi: String get() = douyinParseApiValue
    val douyinApiKey: String get() = douyinApi
    val qqMusicApiKey: String get() = qqMusicApi
    val neteaseApiKey: String get() = neteaseApi
    val kuwoApiKey: String get() = kuwoApi
    val miguApiKey: String get() = miguApi
    val kugouApiKey: String get() = kugouApi
    val equalizerFreqs: List<Int>
        get() = equalizerBands.map { it.first }

    // ── 热歌榜 ──
    fun loadHotChart() {
        viewModelScope.launch {
            isHotChartLoading = true
            try {
                val fetched = MusicApiService.fetchAllHotChart()
                hotChartSongs = fetched
                if (fetched.isNotEmpty()) {
                    val snapshotKey = fetched.joinToString("|") { "${it.platform}:${it.platformId}:${it.id}" }
                    launch {
                        try {
                            // 第一阶段：优先补全首屏可见歌曲，目标是尽快看到封面
                            val fastPass = MusicApiService.enrichMissingSongCovers(
                                songs = fetched,
                                maxLookup = 24,
                                parallelism = 12,
                            )
                            val keyAfterFastPass = hotChartSongs.joinToString("|") { "${it.platform}:${it.platformId}:${it.id}" }
                            if (fastPass != fetched && keyAfterFastPass == snapshotKey) {
                                hotChartSongs = fastPass
                            }

                            // 第二阶段：后台补全剩余歌曲，不阻塞首屏体验
                            val fullPass = MusicApiService.enrichMissingSongCovers(
                                songs = if (fastPass.isNotEmpty()) fastPass else fetched,
                                maxLookup = 120,
                                parallelism = 8,
                            )
                            val keyBeforeFullPass = hotChartSongs.joinToString("|") { "${it.platform}:${it.platformId}:${it.id}" }
                            if (fullPass != hotChartSongs && keyBeforeFullPass == snapshotKey) {
                                hotChartSongs = fullPass
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "热歌榜封面补全失败", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载热歌榜失败", e)
            }
            isHotChartLoading = false
        }
    }

    // ── 搜索相关 ──
    fun quickSearch(keyword: String) {
        searchQuery = keyword
        performOnlineSearch(keyword)
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        if (query.isEmpty()) {
            onlineResults = emptyList()
            searchSuggestions = emptyList()
            searchSuggestionJob?.cancel()
        } else {
            // 获取搜索建议
            fetchSearchSuggestions(query)
        }
    }

    private fun fetchSearchSuggestions(query: String) {
        searchSuggestionJob?.cancel()
        searchSuggestionJob = viewModelScope.launch {
            // 延迟 300ms 避免频繁请求
            delay(300)
            try {
                val suggestions = com.yindong.music.data.api.NeteaseApi.getSearchSuggestions(query)
                searchSuggestions = suggestions
            } catch (e: Exception) {
                // 忽略错误，不显示建议
                searchSuggestions = emptyList()
            }
        }
    }

    fun performOnlineSearch(query: String, isNewSearch: Boolean = true) {
        if (query.isBlank()) return
        // 清空搜索建议
        searchSuggestions = emptyList()
        searchSuggestionJob?.cancel()
        viewModelScope.launch {
            if (isNewSearch) {
                isSearching = true
                currentSearchOffset = 0
                hasMoreResults = true
            }
            searchError = null
            try {
                val usePlugin = apiMode == "lx_plugin" && lxSelectedSource.isNotBlank() && lxSupportsAction("search")
                val results = if (usePlugin) {
                    try {
                        if (lxSelectedSource == LX_SOURCE_ALL) {
                            searchAllSources(query)
                        } else {
                            withTimeoutOrNull(lxTimeoutMs) {
                                lxPluginManager.search(lxSelectedPluginId, lxSelectedSource, query, lxTimeoutMs)
                            } ?: run {
                                searchError = "插件搜索超时，已回退到默认搜索"
                                try { MusicApiService.searchByKeyword(query, currentSearchOffset, searchLimit) } catch (_: Exception) { emptyList() }
                            }
                        }
                    } catch (pluginEx: Exception) {
                        Log.e(TAG, "LX插件搜索失败，回退到官方API", pluginEx)
                        searchError = "插件搜索失败，已回退到默认搜索"
                        try { MusicApiService.searchByKeyword(query, currentSearchOffset, searchLimit) } catch (_: Exception) { emptyList() }
                    }
                } else {
                    if (apiMode == "lx_plugin" && !lxSupportsAction("search")) {
                        searchError = "当前插件不支持搜索，已使用官方API搜索（播放仍走插件）"
                    }
                    // 15s总超时保护，避免无限转圈
                    withTimeoutOrNull(15_000L) {
                        MusicApiService.searchByKeyword(query, currentSearchOffset, searchLimit, isLoadMore = !isNewSearch)
                    } ?: run {
                        searchError = "搜索超时，请稍后重试"
                        emptyList()
                    }
                }

                if (isNewSearch) {
                    onlineResults = results
                } else {
                    // 加载更多：追加结果并去重
                    val existingIds = onlineResults.map { "${it.platform}:${it.platformId}" }.toSet()
                    val newSongs = results.filter { "${it.platform}:${it.platformId}" !in existingIds }
                    onlineResults = onlineResults + newSongs
                }

                // 更新分页状态
                currentSearchOffset += results.size
                hasMoreResults = results.size >= searchLimit

                if (onlineResults.isNotEmpty() && isNewSearch) {
                    addSearchHistory(query)
                    // 延迟刷新列表，让异步获取的网易云封面能够显示
                    viewModelScope.launch {
                        delay(1500)
                        refreshSongCovers()
                        delay(1500)
                        refreshSongCovers()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "在线搜索失败", e)
                searchError = "搜索失败: ${e.message}"
            } finally {
                isSearching = false
                isLoadingMore = false
            }
        }
    }

    /**
     * 单平台搜索
     * @param query 搜索关键词
     * @param platform 平台名称（网易云、酷我音乐、酷狗音乐、QQ音乐）
     */
    fun performSinglePlatformSearch(query: String, platform: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            isSearching = true
            currentSearchOffset = 0
            hasMoreResults = true
            searchError = null
            onlineResults = emptyList()

            try {
                val results = withTimeoutOrNull(15_000L) {
                    when (platform) {
                        "网易云" -> MusicApiService.searchNeteaseOfficial(query, currentSearchOffset, searchLimit)
                        "酷我音乐" -> MusicApiService.searchKuwoOfficial(query, currentSearchOffset, searchLimit)
                        "酷狗音乐" -> MusicApiService.searchKugouOfficial(query, currentSearchOffset, searchLimit)
                        "QQ音乐" -> MusicApiService.searchQQCeruMusic(query, currentSearchOffset, searchLimit)
                        else -> emptyList()
                    }
                } ?: run {
                    searchError = "搜索超时，请稍后重试"
                    emptyList()
                }

                onlineResults = results
                currentSearchOffset += results.size
                hasMoreResults = results.size >= searchLimit

                if (onlineResults.isNotEmpty()) {
                    addSearchHistory(query)
                }
            } catch (e: Exception) {
                Log.e(TAG, "单平台搜索失败", e)
                searchError = "搜索失败: ${e.message}"
            } finally {
                isSearching = false
            }
        }
    }

    /**
     * 加载更多搜索结果
     */
    fun loadMoreResults() {
        if (!hasMoreResults || isLoadingMore || isSearching) return
        val query = searchQuery.trim()
        if (query.isEmpty()) return
        isLoadingMore = true
        performOnlineSearch(query, isNewSearch = false)
    }

    /** 刷新歌曲封面列表（异步获取后调用） */
    fun refreshSongCovers() {
        // 创建新列表触发 Compose 重组
        onlineResults = onlineResults.toList()
    }

    fun isShareLink(url: String): Boolean {
        return url.contains("douyin.com") || url.contains("iesdouyin.com") ||
               url.contains("music.163.com") || url.contains("y.qq.com") ||
               url.contains("kuwo.cn") || url.contains("kugou.com")
    }

    fun parseAndPlay(url: String) {
        viewModelScope.launch {
            isParsing = true
            parseError = null
            if (!passSecurityGate(
                    action = MusicPlaybackGate.Action.PARSE_LINK,
                    defaultMessage = "当前环境存在风险，已阻止解析",
                    parseScope = true,
                )
            ) {
                isParsing = false
                return@launch
            }
            try {
                val song = MusicApiService.parseDouyinLink(url)
                if (song != null) {
                    playSong(song)
                } else {
                    parseError = "解析失败，请检查链接"
                }
            } catch (e: Exception) {
                parseError = "解析失败: ${e.message}"
            } finally {
                isParsing = false
            }
        }
    }

    // ── 分类搜索 ──
    fun searchCategory(category: String) {
        viewModelScope.launch {
            isCategoryLoading = true
            try {
                categorySongs = MusicApiService.searchByKeyword(category)
            } catch (e: Exception) {
                Log.e(TAG, "分类搜索失败", e)
            } finally {
                isCategoryLoading = false
            }
        }
    }

    // ── 歌单辅助 ──
    fun getUserPlaylistById(id: Long): Playlist? = myPlaylists.find { it.id == id }

    fun addSongToPlaylist(playlistId: Long, song: Song) = addToPlaylist(playlistId, song)

    fun removeSongFromPlaylist(playlistId: Long, song: Song) {
        removeFromPlaylist(playlistId, song.id)
    }

    fun exportPlaylistCopy(playlistId: Long) {
        val pl = myPlaylists.find { it.id == playlistId } ?: return
        showToast("歌单「${pl.name}」已导出")
    }

    // ── 收藏辅助 ──
    fun isFavorite(song: Song): Boolean = favoriteSongs.any { it.id == song.id }

    fun removeFavorite(song: Song) {
        favoriteSongs = favoriteSongs.filter { it.id != song.id }
        LocalStorage.saveFavorites(favoriteSongs)
    }

    fun addFavoriteManual(title: String, artist: String) {
        if (title.isBlank()) return
        val song = Song(
            id = System.currentTimeMillis(),
            title = title,
            artist = artist.ifBlank { "未知" },
            album = "",
        )
        favoriteSongs = favoriteSongs + song
        LocalStorage.saveFavorites(favoriteSongs)
    }

    fun importFavoritesFromText(text: String) {
        importFavorites(text)
    }

    fun exportFavoritesAsText(): String {
        return favoriteSongs.joinToString("\n") { "${it.title} - ${it.artist}" }
    }

    fun fetchMissingCovers(playlistId: Long) {
        // 异步为歌单中缺失封面的歌曲搜索封面
        viewModelScope.launch {
            // Stub: 未来可对接搜索 API 补全封面
        }
    }

    fun fetchMissingFavCovers() {
        viewModelScope.launch {
            // Stub: 未来可对接搜索 API 补全收藏封面
        }
    }

    // ── 播放控制别名 ──
    fun togglePlay() = togglePlayPause()
    fun togglePlayMode() = changePlayMode()

    // ── 音质别名 ──
    fun setQuality(quality: MusicApiConfig.Quality) = changeQuality(quality)

    // ── 歌词颜色别名 ──
    fun changeLyricCurrentColor(color: Int) = changeHighlightLyricColor(color)
    fun changeLyricNormalColor(color: Int) = changeNormalLyricColor(color)
    fun changeLyricFontSize(size: Int) = changePlayerLyricSize(size)

    // ── QQ Cookie 别名 ──
    fun setQQCookie(cookie: String) = updateQQCookie(cookie)

    // ── 均衡器预设 ──
    fun getCurrentPresetName(): String = currentPreset.displayName
    private data class SceneParams(
        val eq: List<Int>,
        val bass: Int = 0,
        val virt: Int = 0,
        val reverbRoom: Int = 0,
        val reverbDamp: Int = 0,
        val reverbWet: Int = 0,
        val loud: Int = 0,
    )

    private data class ReverbProfile(
        val decayTime: Int,
        val decayHFRatio: Int,
        val roomLevel: Int,
        val reverbLevel: Int,
        val reflectionsLevel: Int,
        val reflectionsDelay: Int,
        val reverbDelay: Int,
        val diffusion: Int,
        val density: Int,
    )

    private fun getSceneParams(preset: EqPreset): SceneParams = when (preset) {
        EqPreset.FLAT -> SceneParams(listOf(0, 0, 0, 0, 0))
        EqPreset.BASS -> SceneParams(listOf(4, 3, 1, 0, 0), bass = 45, virt = 5, reverbRoom = 10, reverbDamp = 50, reverbWet = 8)
        EqPreset.VOCAL -> SceneParams(listOf(-1, 2, 4, 3, 1), bass = 0, virt = 18, reverbRoom = 22, reverbDamp = 55, reverbWet = 15)
        EqPreset.POP -> SceneParams(listOf(1, 3, 3, 2, 2), bass = 18, virt = 16, reverbRoom = 25, reverbDamp = 45, reverbWet = 18)
        EqPreset.ROCK -> SceneParams(listOf(4, 1, 0, 2, 4), bass = 32, virt = 22, reverbRoom = 28, reverbDamp = 42, reverbWet = 18)
        EqPreset.JAZZ -> SceneParams(listOf(2, 0, 1, 3, 2), bass = 12, virt = 50, reverbRoom = 52, reverbDamp = 50, reverbWet = 35)
        EqPreset.CLASSICAL -> SceneParams(listOf(0, 0, 0, 1, 3), bass = 0, virt = 40, reverbRoom = 70, reverbDamp = 40, reverbWet = 35)
        EqPreset.ELECTRONIC -> SceneParams(listOf(4, 1, -1, 1, 4), bass = 35, virt = 55, reverbRoom = 30, reverbDamp = 32, reverbWet = 20)
        EqPreset.HIPHOP -> SceneParams(listOf(4, 3, 1, 2, 3), bass = 38, virt = 12, reverbRoom = 18, reverbDamp = 50, reverbWet = 12)
        EqPreset.LIVE -> SceneParams(listOf(0, 1, 3, 2, 0), bass = 10, virt = 65, reverbRoom = 62, reverbDamp = 38, reverbWet = 42)
        EqPreset.NIGHT -> SceneParams(listOf(-2, 0, 1, 1, -2), bass = 0, virt = 10, reverbRoom = 20, reverbDamp = 65, reverbWet = 12)
        EqPreset.ACG -> SceneParams(listOf(-1, 1, 3, 4, 3), bass = 10, virt = 20, reverbRoom = 22, reverbDamp = 48, reverbWet = 15)
    }

    private fun getReverbProfile(preset: EqPreset): ReverbProfile? = when (preset) {
        EqPreset.FLAT -> null
        EqPreset.BASS -> ReverbProfile(300, 900, -1800, -2400, -2000, 5, 8, 800, 900)
        EqPreset.VOCAL -> ReverbProfile(800, 650, -1500, -1800, -1600, 8, 12, 850, 800)
        EqPreset.POP -> ReverbProfile(1000, 850, -1200, -1500, -1400, 10, 15, 880, 850)
        EqPreset.ROCK -> ReverbProfile(1500, 750, -1000, -1200, -1200, 15, 20, 900, 880)
        EqPreset.JAZZ -> ReverbProfile(2600, 580, -600, -800, -800, 18, 25, 950, 940)
        EqPreset.CLASSICAL -> ReverbProfile(3500, 480, -600, -800, -800, 22, 32, 970, 960)
        EqPreset.ELECTRONIC -> ReverbProfile(800, 1000, -1000, -1400, -1200, 8, 12, 820, 880)
        EqPreset.HIPHOP -> ReverbProfile(450, 800, -1800, -2200, -2000, 5, 10, 800, 850)
        EqPreset.LIVE -> ReverbProfile(3200, 620, -500, -600, -800, 20, 30, 985, 980)
        EqPreset.NIGHT -> ReverbProfile(600, 380, -2000, -2500, -2200, 8, 12, 700, 750)
        EqPreset.ACG -> ReverbProfile(900, 750, -1500, -1800, -1500, 10, 14, 860, 820)
    }

    fun applyPreset(preset: EqPreset) {
        currentPreset = preset
        LocalStorage.saveEqPreset(preset.name)
        val scene = getSceneParams(preset)
        val eqTargets = scene.eq
        val bandCount = equalizerBands.size
        for (i in 0 until bandCount) {
            setEqualizerBand(i, eqTargets.getOrElse(i) { 0 })
        }
        updateBassBoost(scene.bass)
        updateVirtualizer(scene.virt)
        val profile = getReverbProfile(preset)
        if (profile != null) {
            reverbRoomSize = scene.reverbRoom.coerceIn(0, 100)
            reverbDamping = scene.reverbDamp.coerceIn(0, 100)
            reverbLevel = scene.reverbWet.coerceIn(0, 100)
            setReverbDirect(profile)
        } else {
            updateReverbRoomSize(scene.reverbRoom)
            updateReverbDamping(scene.reverbDamp)
            updateReverbWet(scene.reverbWet)
        }
        updateLoudnessGain(scene.loud)
    }

    fun updateBassBoost(value: Int) {
        val clamped = value.coerceIn(0, 100)
        bassBoostStrength = clamped
        bassBoostEnabled = clamped > 0
        try {
            bassBoostEffect?.enabled = equalizerEnabled && clamped > 0
            bassBoostEffect?.setStrength((clamped * 10).coerceIn(0, 1000).toShort())
        } catch (_: Exception) {}
    }

    fun updateVirtualizer(value: Int) {
        val clamped = value.coerceIn(0, 100)
        virtualizerStrength = clamped
        virtualizerEnabled = clamped > 0
        try {
            virtualizerEffect?.enabled = equalizerEnabled && clamped > 0
            virtualizerEffect?.setStrength((clamped * 10).coerceIn(0, 1000).toShort())
        } catch (_: Exception) {}
    }

    fun updateReverbRoomSize(value: Int) {
        reverbRoomSize = value.coerceIn(0, 100)
        applyMappedReverb()
    }

    fun updateReverbDamping(value: Int) {
        reverbDamping = value.coerceIn(0, 100)
        applyMappedReverb()
    }

    fun updateReverbWet(value: Int) {
        reverbLevel = value.coerceIn(0, 100)
        applyMappedReverb()
    }

    fun updateReverbLevel(value: Int) {
        updateReverbWet(value)
    }

    fun updateLoudnessGain(value: Int) {
        val clamped = value.coerceIn(0, 100)
        loudnessGain = clamped
        try {
            if (!equalizerEnabled || clamped <= 0) {
                loudnessEnhancerEffect?.enabled = false
            } else {
                loudnessEnhancerEffect?.enabled = true
                loudnessEnhancerEffect?.setTargetGain((clamped * 20).coerceAtMost(2000))
            }
        } catch (_: Exception) {}
    }

    private fun setReverbDirect(profile: ReverbProfile) {
        try {
            if (!equalizerEnabled || reverbLevel <= 0) {
                reverbEffect?.enabled = false
                return
            }
            reverbEffect?.enabled = true
            reverbEffect?.decayTime = profile.decayTime.coerceIn(100, 20000)
            reverbEffect?.decayHFRatio = profile.decayHFRatio.coerceIn(100, 2000).toShort()
            reverbEffect?.roomLevel = profile.roomLevel.coerceIn(-9000, 0).toShort()
            reverbEffect?.reverbLevel = profile.reverbLevel.coerceIn(-9000, 2000).toShort()
            reverbEffect?.reflectionsLevel = profile.reflectionsLevel.coerceIn(-9000, 1000).toShort()
            reverbEffect?.reflectionsDelay = profile.reflectionsDelay.coerceIn(0, 300)
            reverbEffect?.reverbDelay = profile.reverbDelay.coerceIn(0, 100)
            reverbEffect?.diffusion = profile.diffusion.coerceIn(0, 1000).toShort()
            reverbEffect?.density = profile.density.coerceIn(0, 1000).toShort()
        } catch (_: Exception) {}
    }

    private fun applyMappedReverb() {
        val wet = reverbLevel.coerceIn(0, 100)
        val room = reverbRoomSize.coerceIn(0, 100)
        val damping = reverbDamping.coerceIn(0, 100)
        try {
            if (!equalizerEnabled || wet <= 0) {
                reverbEffect?.enabled = false
                return
            }
            reverbEffect?.enabled = true
            reverbEffect?.decayTime = (100 + room * 49).coerceIn(100, 5000)
            reverbEffect?.decayHFRatio = (1900 - damping * 18).coerceIn(100, 1900).toShort()
            reverbEffect?.roomLevel = (-3000 + wet * 30).coerceIn(-3000, 0).toShort()
            reverbEffect?.reverbLevel = (-6000 + wet * 60).coerceIn(-6000, 0).toShort()
            reverbEffect?.reflectionsDelay = (2 + room * 28 / 100).coerceIn(0, 30)
            reverbEffect?.reflectionsLevel = (-4000 + wet * 30).coerceIn(-4000, -1000).toShort()
            reverbEffect?.reverbDelay = (5 + room * 35 / 100).coerceIn(0, 40)
            reverbEffect?.diffusion = (600 + room * 4).coerceIn(600, 1000).toShort()
            reverbEffect?.density = (600 + room * 4).coerceIn(600, 1000).toShort()
        } catch (_: Exception) {}
    }

    fun formatFrequency(hz: Int): String {
        return if (hz >= 1000) "${hz / 1000}k" else "${hz}Hz"
    }

    // ── API 更新方法（MineScreen 使用）──
    fun updateQQPlayApi(url: String) {
        qqPlayApiValue = url
        LocalStorage.saveQQPlayApi(url)
    }

    fun updateDouyinParseApi(url: String) {
        douyinParseApiValue = url
        LocalStorage.saveDouyinParseApi(url)
    }

    fun updateQQMusicApiKey(key: String) = updateApiKey("qq_music", key)
    fun updateNeteaseApiKey(key: String) = updateApiKey("netease", key)
    fun updateKuwoApiKey(key: String) = updateApiKey("kuwo", key)
    fun updateMiguApiKey(key: String) = updateApiKey("migu", key)
    fun updateKugouApiKey(key: String) = updateApiKey("kugou", key)
    fun updateDouyinApiKey(key: String) = updateApiKey("douyin", key)

    /**
     * Import a JS plugin (add to the multi-plugin list).
     * If the same plugin (by hash) already exists, it is replaced.
     */
    private fun sha256Hex(bytes: ByteArray): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") { "%02x".format(it) }
    }
    private fun sha256Hex(text: String): String {
        return sha256Hex(text.toByteArray(Charsets.UTF_8))
    }

    private fun normalizePluginScriptForHash(script: String): String {
        return script.replace("\uFEFF", "").trim()
    }

    private suspend fun ensurePluginPolicyLoaded() {
        if (RemoteConfig.isLoaded) return
        val loaded = try {
            RemoteConfig.fetch()
        } catch (e: Exception) {
            Log.w(TAG, "加载插件策略失败: ${e.message}")
            false
        }
        if (!loaded) {
            Log.w(TAG, "插件策略未加载成功，继续使用本地默认策略: ${RemoteConfig.lastError}")
        }
    }

    private suspend fun ensurePluginHashAllowed(pluginHash: String, script: String? = null) {
        // 允许所有插件导入，跳过白名单检查
        return
    }
    suspend fun importLxPlugin(uri: Uri): Result<Unit> {
        return try {
            try {
                getApplication<Application>().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // ignore: fallback to transient read permission
            }

            // ── 将插件复制到内部存储 (解决 content:// URI 过期导致重启后无法重新加载) ──
            val app = getApplication<Application>()
            val maxPluginSize = 2 * 1024 * 1024
            val scriptBytes = app.contentResolver.openInputStream(uri)?.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (true) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    total += n
                    if (total > maxPluginSize) throw IllegalArgumentException("插件大小不能超过2MB")
                    output.write(buffer, 0, n)
                }
                output.toByteArray()
            } ?: throw IllegalStateException("读取插件文件失败")
            val script = scriptBytes.toString(Charsets.UTF_8)
            val pluginHash = sha256Hex(scriptBytes)
            ensurePluginHashAllowed(pluginHash, script)

            val pluginsDir = java.io.File(app.filesDir, "lx_plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdirs()
            val rawName = uri.lastPathSegment?.substringAfterLast('/')?.substringBefore('?') ?: ""
            val safeRawName = rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = safeRawName.takeIf { it.endsWith(".js") && it.isNotBlank() }
                ?: "plugin_${System.currentTimeMillis()}.js"
            val localFile = java.io.File(pluginsDir, fileName)
            localFile.writeText(script, Charsets.UTF_8)
            val fileUri = android.net.Uri.fromFile(localFile).toString()

            val opts = LxRuntimeOptions(callTimeoutMs = lxTimeoutMs, allowHttp = lxAllowHttp)
            val entry = lxPluginManager.loadPluginFromScript(script, fileUri, opts).getOrThrow()

            // Update multi-plugin list (排除内置插件)
            lxPlugins = (lxPlugins.filter { it.id != entry.id && it.id != builtinPluginId } + entry)
            persistPlugins()

            // Legacy compat: keep single-plugin fields in sync with first plugin
            lxPluginUri = fileUri
            lxPluginHash = entry.id
            lxPluginInfo = entry.info
            LocalStorage.saveLxPluginUri(fileUri)
            LocalStorage.saveLxPluginHash(entry.id)
            LocalStorage.saveLxPluginInfo(entry.info)

            // Rebuild combined sources
            rebuildSources()

            // Auto-select if nothing selected
            if (lxSelectedPluginId.isBlank() || !lxPluginManager.isPluginLoaded(lxSelectedPluginId)) {
                lxSelectedPluginId = entry.id
                LocalStorage.saveLxSelectedPluginId(lxSelectedPluginId)
            }
            if (lxSelectedSource.isBlank() || lxSelectedSource !in lxSources) {
                lxSelectedSource = entry.sources.firstOrNull().orEmpty()
                LocalStorage.saveLxSelectedSource(lxSelectedSource)
            }

            apiMode = "lx_plugin"
            LocalStorage.saveApiMode(apiMode)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import a JS plugin from a remote URL.
     * Downloads the script, saves it to internal storage, then loads it.
     */
    suspend fun importLxPluginFromUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                throw IllegalArgumentException("仅支持HTTP/HTTPS插件URL")
            }
            val maxPluginBytes = 2L * 1024 * 1024
            val request = Request.Builder().url(url)
                .header("User-Agent", "Mozilla/5.0")
                .build()
            val scriptBytes = OkHttpClient().newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("下载失败: HTTP ${response.code}")
                }
                val body = response.body ?: throw IllegalStateException("下载内容为空")
                val contentLength = body.contentLength()
                if (contentLength > maxPluginBytes) {
                    throw IllegalArgumentException("插件大小不能超过2MB")
                }
                body.byteStream().use { input ->
                    val output = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n <= 0) break
                        total += n
                        if (total > maxPluginBytes) {
                            throw IllegalArgumentException("插件大小不能超过2MB")
                        }
                        output.write(buffer, 0, n)
                    }
                    output.toByteArray()
                }
            }
            val script = scriptBytes.toString(Charsets.UTF_8)
            val pluginHash = sha256Hex(scriptBytes)
            ensurePluginHashAllowed(pluginHash, script)

            // Save to internal storage for persistence & reload on startup
            val pluginsDir = java.io.File(getApplication<Application>().filesDir, "lx_plugins")
            if (!pluginsDir.exists()) pluginsDir.mkdirs()
            val rawFileName = url.substringAfterLast('/').substringBefore('?')
            val safeFileName = rawFileName.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = safeFileName.takeIf { it.endsWith(".js") && it.isNotBlank() }
                ?: "plugin_${System.currentTimeMillis()}.js"
            val localFile = java.io.File(pluginsDir, fileName)
            localFile.writeText(script, Charsets.UTF_8)
            val fileUri = android.net.Uri.fromFile(localFile).toString()

            val opts = LxRuntimeOptions(callTimeoutMs = lxTimeoutMs, allowHttp = lxAllowHttp)
            val entry = lxPluginManager.loadPluginFromScript(script, fileUri, opts).getOrThrow()

            withContext(Dispatchers.Main) {
                lxPlugins = (lxPlugins.filter { it.id != entry.id && it.id != builtinPluginId } + entry)
                persistPlugins()

                lxPluginUri = fileUri
                lxPluginHash = entry.id
                lxPluginInfo = entry.info
                LocalStorage.saveLxPluginUri(fileUri)
                LocalStorage.saveLxPluginHash(entry.id)
                LocalStorage.saveLxPluginInfo(entry.info)

                rebuildSources()

                if (lxSelectedPluginId.isBlank() || !lxPluginManager.isPluginLoaded(lxSelectedPluginId)) {
                    lxSelectedPluginId = entry.id
                    LocalStorage.saveLxSelectedPluginId(lxSelectedPluginId)
                }
                if (lxSelectedSource.isBlank() || lxSelectedSource !in lxSources) {
                    lxSelectedSource = entry.sources.firstOrNull().orEmpty()
                    LocalStorage.saveLxSelectedSource(lxSelectedSource)
                }

                apiMode = "lx_plugin"
                LocalStorage.saveApiMode(apiMode)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Remove a specific plugin by ID (内置插件不可移除). */
    fun removeLxPluginById(pluginId: String) {
        if (pluginId == builtinPluginId) return  // 内置插件不可移除
        lxPluginManager.removePlugin(pluginId)
        lxPlugins = lxPlugins.filter { it.id != pluginId }
        persistPlugins()
        rebuildSources()

        // If the removed plugin was selected, reset selection
        if (lxSelectedPluginId == pluginId) {
            val first = lxPlugins.firstOrNull()
            lxSelectedPluginId = first?.id.orEmpty()
            lxSelectedSource = first?.sources?.firstOrNull().orEmpty()
            LocalStorage.saveLxSelectedPluginId(lxSelectedPluginId)
            LocalStorage.saveLxSelectedSource(lxSelectedSource)
        }

        // Legacy compat
        if (lxPlugins.isEmpty()) {
            lxPluginUri = ""
            lxPluginHash = ""
            lxPluginInfo = LxPluginInfo()
            LocalStorage.saveLxPluginUri("")
            LocalStorage.saveLxPluginHash("")
            LocalStorage.saveLxPluginInfo(LxPluginInfo())
        }
    }

    /** Remove all user plugins (不影响内置插件). */
    fun removeLxPlugin() {
        // 只移除用户插件，保留内置插件
        lxPlugins.forEach { if (it.id != builtinPluginId) lxPluginManager.removePlugin(it.id) }
        lxPlugins = emptyList()
        lxPluginUri = ""
        lxPluginHash = ""
        lxPluginInfo = LxPluginInfo()
        lxSelectedSource = ""
        lxSelectedPluginId = ""
        lxSources = emptyList()
        LocalStorage.saveLxPlugins(emptyList())
        LocalStorage.saveLxPluginUri("")
        LocalStorage.saveLxPluginHash("")
        LocalStorage.saveLxPluginInfo(LxPluginInfo())
        LocalStorage.saveLxSelectedSource("")
        LocalStorage.saveLxSelectedPluginId("")
    }

    fun updateLxSelectedSource(source: String) {
        lxSelectedSource = source
        // 如果当前插件不支持该source，则自动切换到支持该source的插件
        val currentSources = lxPluginManager.getPluginEntry(lxSelectedPluginId)?.sources.orEmpty()
        if (source !in currentSources) {
            val matchedPluginId = lxPluginManager.getAllPluginEntries()
                .firstOrNull { source in it.sources }
                ?.id
                .orEmpty()
            if (matchedPluginId.isNotBlank()) {
                lxSelectedPluginId = matchedPluginId
                LocalStorage.saveLxSelectedPluginId(matchedPluginId)
            }
        }
        LocalStorage.saveLxSelectedSource(source)
    }

    /** Select a specific plugin + source combination. */
    fun updateLxSelection(pluginId: String, source: String) {
        lxSelectedPluginId = pluginId
        lxSelectedSource = source
        LocalStorage.saveLxSelectedPluginId(pluginId)
        LocalStorage.saveLxSelectedSource(source)
        // Update legacy fields to match
        val entry = lxPlugins.find { it.id == pluginId }
        if (entry != null) {
            lxPluginInfo = entry.info
            lxPluginUri = entry.uri
            lxPluginHash = entry.id
        }
    }

    fun updateLxTimeoutMs(timeoutMs: Long) {
        lxTimeoutMs = timeoutMs.coerceIn(5000L, 30000L)
        LocalStorage.saveLxTimeoutMs(lxTimeoutMs)
    }
    fun updateLxAllowHttp(allow: Boolean) {
        lxAllowHttp = allow
        LocalStorage.saveLxAllowHttp(allow)
    }

    /** Reload all plugins from a list of saved entries. */
    private fun reloadAllPlugins(entries: List<LxPluginEntry>) {
        val opts = LxRuntimeOptions(callTimeoutMs = lxTimeoutMs, allowHttp = lxAllowHttp)
        val loaded = mutableListOf<LxPluginEntry>()
        for (entry in entries) {
            if (!RemoteConfig.isPluginHashAllowed(entry.id)) {
                Log.w(TAG, "reloadAllPlugins: plugin blocked by allowlist, id=${entry.id.take(12)}")
                continue
            }
            try {
                val result = lxPluginManager.loadPlugin(entry.uri, opts).getOrThrow()
                loaded.add(result)
                Log.d(TAG, "reloadAllPlugins: loaded ${result.info.name}, sources=${result.sources}")
            } catch (e: Exception) {
                Log.e(TAG, "reloadAllPlugins: failed to load ${entry.info.name}", e)
                // Keep the entry in the list with saved sources (offline)
                loaded.add(entry)
            }
        }
        lxPlugins = loaded.filter { it.id != builtinPluginId }
        persistPlugins()
        rebuildSources()

        // Validate selection
        val available = lxPluginManager.getAllPluginEntries()
        if (lxSelectedPluginId.isBlank() || available.none { it.id == lxSelectedPluginId }) {
            lxSelectedPluginId = available.firstOrNull()?.id.orEmpty()
            LocalStorage.saveLxSelectedPluginId(lxSelectedPluginId)
        }
        if (lxSelectedSource.isBlank() || lxSelectedSource !in lxSources) {
            lxSelectedSource = lxSources.firstOrNull().orEmpty()
            LocalStorage.saveLxSelectedSource(lxSelectedSource)
        }

        // Legacy compat
        val firstPlugin = loaded.firstOrNull()
        lxPluginInfo = firstPlugin?.info ?: LxPluginInfo()
        lxPluginUri = firstPlugin?.uri.orEmpty()
        lxPluginHash = firstPlugin?.id.orEmpty()
    }

    /** Legacy: reload the single selected plugin. */
    suspend fun reloadLxPlugin(): Result<Unit> {
        return try {
            ensurePluginPolicyLoaded()
            if (lxPluginUri.isBlank()) error("请先导入JS插件")
            // 允许所有插件加载，跳过白名单检查
            val loaded = lxPluginManager.load(
                pluginUri = lxPluginUri,
                options = LxRuntimeOptions(
                    callTimeoutMs = lxTimeoutMs,
                    allowHttp = lxAllowHttp,
                ),
            ).getOrThrow()
            lxPluginInfo = loaded.first
            rebuildSources()
            if (lxSelectedSource.isBlank() || lxSelectedSource !in lxSources) {
                lxSelectedSource = lxSources.firstOrNull().orEmpty()
                LocalStorage.saveLxSelectedSource(lxSelectedSource)
            }
            LocalStorage.saveLxPluginInfo(lxPluginInfo)
            // Sync multi-plugin list (过滤掉内置插件)
            lxPlugins = lxPluginManager.getAllPluginEntries().filter { it.id != builtinPluginId }
            persistPlugins()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Rebuild the combined source list from all loaded plugins (不含内置插件). */
    private fun rebuildSources() {
        lxSources = lxPluginManager.getAllPluginEntries().flatMap { it.sources }.distinct()
    }

    /** Persist the current plugin list. */
    private fun persistPlugins() {
        // 只持久化用户插件，内置插件由服务器下发，不走本地URI持久化
        LocalStorage.saveLxPlugins(
            lxPlugins.filter { it.id != builtinPluginId && !it.uri.startsWith("builtin://") }
        )
    }

    suspend fun testLxSearch(keyword: String): Result<List<Song>> = runCatching {
        if (lxSelectedSource.isBlank()) error("未选择source")
        if (!lxSupportsAction("search")) error("当前插件source不支持search操作")
        // Ensure plugin is loaded
        if (lxPluginManager.pluginCount() == 0 && lxPluginUri.isNotBlank()) {
            Log.d(TAG, "testLxSearch: plugin not loaded, reloading...")
            reloadLxPlugin().getOrThrow()
        }
        lxPluginManager.search(lxSelectedPluginId, lxSelectedSource, keyword, lxTimeoutMs)
    }

    suspend fun testLxMusicUrl(song: Song): Result<String> = runCatching {
        if (lxSelectedSource.isBlank()) error("未选择source")
        val lxQuality = when (selectedQuality) {
            MusicApiConfig.Quality.STANDARD -> "128k"
            MusicApiConfig.Quality.EXHIGH -> "320k"
            MusicApiConfig.Quality.LOSSLESS -> "flac"
        }
        lxPluginManager.musicUrl(lxSelectedPluginId, lxSelectedSource, song, lxTimeoutMs, lxQuality).url
    }

    fun testApiConnection(host: String): Boolean {
        // 简单连通性检测
        return try {
            val url = if (host.endsWith("/")) "${host}api/ping" else "$host/api/ping"
            val request = okhttp3.Request.Builder().url(url).build()
            val response = okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .build().newCall(request).execute()
            response.isSuccessful.also { response.close() }
        } catch (_: Exception) { false }
    }

    // ── 歌单文本导入/导出（MineScreen 使用）──
    fun importPlaylistFromText(text: String) {
        if (text.isBlank()) return
        val lines = text.lines().filter { it.isNotBlank() }
        val name = lines.firstOrNull() ?: "导入歌单"
        val songs = lines.drop(1).mapIndexed { i, line ->
            val parts = line.split(" - ", "—", limit = 2)
            Song(
                id = System.currentTimeMillis() + i,
                title = parts.getOrElse(0) { line }.trim(),
                artist = parts.getOrElse(1) { "" }.trim(),
                album = "",
            )
        }
        val newPlaylist = Playlist(
            id = System.currentTimeMillis(),
            name = name.trim(),
            coverUrl = "",
            songCount = songs.size,
            playCount = 0,
            creator = userName,
            songs = songs,
        )
        myPlaylists = myPlaylists + newPlaylist
        LocalStorage.savePlaylists(myPlaylists)
        showToast("已导入歌单「${name.trim()}」，共 ${songs.size} 首")
    }

    fun exportAllPlaylistsAsText(): String {
        if (myPlaylists.isEmpty()) return ""
        return myPlaylists.joinToString("\n\n") { pl ->
            val header = "歌单: ${pl.name} (${pl.songs.size}首)"
            val body = pl.songs.joinToString("\n") { "  ${it.title} - ${it.artist}" }
            "$header\n$body"
        }
    }

    // ── 崩溃日志（MineScreen 使用）──
    fun getCrashLogCount(): Int = CrashLogManager.getLogCount()

    fun fetchCrashLogs(): List<CrashLogEntry> = CrashLogManager.getLogFiles()

    fun clearCrashLogs() {
        CrashLogManager.clearAllLogs()
        crashLogs = emptyList()
    }

    fun showToast(msg: String) {
        Toast.makeText(getApplication(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onCleared() {
        super.onCleared()
        if (!playerReleased) {
            playerReleased = true
            player.release()
            mediaSession?.release()
            equalizerEffect?.release()
            bassBoostEffect?.release()
            virtualizerEffect?.release()
            reverbEffect?.release()
            loudnessEnhancerEffect?.release()
            try {
                getApplication<Application>().stopService(
                    Intent(getApplication(), MusicPlaybackService::class.java)
                )
            } catch (_: Exception) {}
        }
        headsetManager.stopMonitoring()
    }
}