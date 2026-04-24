package com.dirror.music.util

import com.drake.serialize.serialize.serialLazy

object AppConfig {

    var cookie by serialLazy("", Config.CLOUD_MUSIC_COOKIE)

    // 音质设置: standard, exhigh, lossless, hires, jymaster
    var soundQuality by serialLazy("exhigh", Config.SOUND_QUALITY)

}