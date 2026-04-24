package com.yindong.music.data.lx

object LxPluginHeaderParser {
    private val tagRegex = Regex("@(name|version|author|description|homepage)\\s+(.+)")

    fun parse(script: String): LxPluginInfo {
        val header = script.lineSequence()
            .take(120)
            .takeWhile {
                val t = it.trim()
                t.startsWith("//") || t.startsWith("/*") || t.startsWith("*") || t.startsWith("*/") || t.isBlank()
            }
            .toList()
            .joinToString("\n")

        val map = mutableMapOf<String, String>()
        tagRegex.findAll(header).forEach { m ->
            map[m.groupValues[1].lowercase()] = m.groupValues[2].trim()
        }
        return LxPluginInfo(
            name = map["name"].orEmpty(),
            version = map["version"].orEmpty(),
            author = map["author"].orEmpty(),
            description = map["description"].orEmpty(),
            homepage = map["homepage"].orEmpty(),
        )
    }
}
