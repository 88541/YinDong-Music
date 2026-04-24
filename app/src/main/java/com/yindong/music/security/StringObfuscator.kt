package com.yindong.music.security

/**
 * 字符串混淆工具 (运行时解码)
 * 防止 APK 反编译后直接看到明文敏感常量
 */
object StringObfuscator {

    // 旋转XOR密钥 (6字节)
    private val _k = intArrayOf(0x4B, 0x2D, 0x6F, 0x1A, 0x53, 0x38)

    /** 解码混淆数组 → 明文字符串 */
    fun decode(encoded: IntArray): String {
        val c = CharArray(encoded.size)
        for (i in encoded.indices) c[i] = (encoded[i] xor _k[i % _k.size]).toChar()
        return String(c)
    }

    @Deprecated("Do not store long-lived auth secrets in client code")
    val APP_ID: String
        get() = ""

    @Deprecated("Do not store long-lived auth secrets in client code")
    val APP_KEY: String
        get() = ""
}
