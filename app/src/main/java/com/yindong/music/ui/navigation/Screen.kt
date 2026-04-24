package com.yindong.music.ui.navigation

sealed class Screen(val route: String) {
    data object Discover : Screen("discover")
    data object Playlist : Screen("playlist")
    data object Mine : Screen("mine")
    data object Search : Screen("search")
    data object Player : Screen("player")
    data object HotChart : Screen("hot_chart")
    data object PlaylistDetail : Screen("playlist_detail/{playlistId}") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
    data object ImportPlaylist : Screen("import_playlist")
    data object PlaylistSquare : Screen("playlist_square")
    data object ExternalPlaylistDetail : Screen("external_playlist_detail")
    data object Downloads : Screen("downloads")
    data object AiAudioEffect : Screen("ai_audio_effect")
}
