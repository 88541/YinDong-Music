package com.yindong.music.security

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * ═══════════════════════════════════════════
 *  字符串加密工具 — StringEncryptor
 * ═══════════════════════════════════════════
 *
 * 使用 AES-128-CBC 对敏感字符串进行加解密。
 * 目的：防止逆向人员在反编译后的 smali/dex 中直接搜索到
 * API 地址、密钥、关键字符串等敏感信息。
 *
 * 使用方式:
 *  1. 开发阶段：调用 StringEncryptor.encrypt("你的敏感字符串")
 *     得到加密后的 Base64 字符串
 *  2. 代码中：用 StringEncryptor.decrypt("加密后的字符串")
 *     在运行时解密
 *
 * 注意:
 *  - 密钥被拆分存储，增加静态分析难度
 *  - 配合 ProGuard 混淆后，攻击者难以定位此类
 *  - 不要在日志中打印解密后的值！
 */
object StringEncryptor {

    /**
     * AES 密钥 (16 字节 / 128 位)
     * 拆分为多段拼接，防止直接在二进制中搜索到完整密钥
     */
    private val keyParts = arrayOf(
        "cM\u0075s", // 4 chars
        "1c\u0042ey", // 4 chars  
        "S\u0065cr",  // 4 chars
        "3t\u0021X",  // 4 chars
    )

    /**
     * IV 初始向量 (16 字节)
     * 同样拆分存储
     */
    private val ivParts = arrayOf(
        "Rn\u0064m",  // 4 chars
        "Iv\u0056ec", // 4 chars
        "t0\u0072S",  // 4 chars
        "3c\u0075R",  // 4 chars
    )

    /** 懒加载拼接密钥，避免类加载时就暴露 */
    private val secretKey: SecretKeySpec by lazy {
        val key = keyParts.joinToString("").toByteArray(Charsets.UTF_8)
        SecretKeySpec(key, "AES")
    }

    /** 懒加载拼接 IV */
    private val iv: IvParameterSpec by lazy {
        val ivBytes = ivParts.joinToString("").toByteArray(Charsets.UTF_8)
        IvParameterSpec(ivBytes)
    }

    /**
     * 加密字符串 → Base64 编码的密文
     * 在开发阶段使用此方法预生成加密字符串
     */
    fun encrypt(plainText: String): String {
        // 加密失败时抛出异常，不静默回退明文（防止敏感数据意外以明文存储）
        val randomIv = ByteArray(16)
        SecureRandom().nextBytes(randomIv)
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(randomIv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        // IV + 密文拼接后 Base64 编码
        return Base64.encodeToString(randomIv + encrypted, Base64.NO_WRAP)
    }

    /**
     * 解密 Base64 编码的密文 → 明文
     * 支持新格式 (IV+密文) 和旧格式 (静态IV)
     */
    fun decrypt(encryptedBase64: String): String {
        return try {
            val data = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (data.size <= 16) return encryptedBase64

            // 尝试新格式: 前16字节为IV
            try {
                val extractedIv = data.copyOfRange(0, 16)
                val ciphertext = data.copyOfRange(16, data.size)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(extractedIv))
                return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            } catch (_: Exception) {
                // 回退旧格式: 使用静态IV
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
                String(cipher.doFinal(data), Charsets.UTF_8)
            }
        } catch (_: Exception) {
            encryptedBase64
        }
    }

    /**
     * 便捷方法：解密字符串，别名
     * 用法: S("加密后的字符串")
     */
    fun S(enc: String): String = decrypt(enc)
}
