package com.yindong.music.data.api

/**
 * 歌单链接解析结果
 */
data class ParsedLink(
    val platform: String,   // "QQ音乐" | "网易云" | "酷我音乐" | "酷狗音乐"
    val playlistId: String
)

/**
 * 外部歌单链接解析器
 *
 * 支持从用户输入文本中识别 QQ音乐、网易云、酷我音乐、酷狗音乐 的歌单链接，
 * 也支持从分享口令文本中提取嵌入的 URL。
 */
object LinkParser {

    // ── 网易云 ──
    private val NETEASE_PATTERNS = listOf(
        Regex("""music\.163\.com/playlist\?id=(\d+)"""),
        Regex("""music\.163\.com/#/playlist\?id=(\d+)"""),
        Regex("""music\.163\.com/m/playlist\?id=(\d+)"""),
        Regex("""music\.163\.com.*[?&]id=(\d+)"""),
        Regex("""163cn\.tv/([a-zA-Z0-9]+)"""),
        Regex("""y\.music\.163\.com.*[?&]id=(\d+)"""),
    )

    // ── QQ音乐 ──
    private val QQ_PATTERNS = listOf(
        Regex("""y\.qq\.com/n/ryqq/playlist/(\d+)"""),
        Regex("""i\.y\.qq\.com/n2/m/share/details/taoge\.html\?.*id=(\d+)"""),
        Regex("""c\.y\.qq\.com/base/fcgi-bin/u\?__=(\w+)"""),
        Regex("""qq\.com.*[?&]id=(\d{7,})"""),
        Regex("""_qq.*[&?]id=(\d{7,})"""),
        Regex("""[&?]id=(\d{7,}).*ADTAG"""),
    )

    // ── 酷我 ──
    private val KUWO_PATTERNS = listOf(
        Regex("""kuwo\.cn/playlist_detail/(\d+)"""),
        Regex("""m\.kuwo\.cn/newh5app/playlist_detail/(\d+)"""),
        Regex("""kuwo\.cn.*playlist.*?(\d{5,})"""),
        Regex("""m\.kuwo\.cn.*?(\d{5,})"""),
    )

    // ── 酷狗 ──
    private val KUGOU_PATTERNS = listOf(
        Regex("""kugou\.com/songlist/(\d+)"""),
        Regex("""kugou\.com.*special.*?(\d{5,})"""),
        Regex("""m\.kugou\.com/.*?listid=(\d+)"""),
        Regex("""m\.kugou\.com.*?(\d{5,})"""),
        Regex("""t1\.kugou\.com/([a-zA-Z0-9]+)"""),
    )

    // URL 提取正则（从分享口令文本中提取链接）
    private val URL_EXTRACTOR = Regex("""https?://[^\s<>"{}|\\^`\[\]]+""")

    /**
     * 从用户输入文本中解析歌单链接
     * 支持直接 URL 和分享口令（含短链接的文本）
     * @return ParsedLink 或 null（无法识别时）
     */
    fun parse(input: String): ParsedLink? {
        if (input.isBlank()) return null

        // 先尝试直接匹配整段文本
        matchPlatform(input)?.let { return it }

        // 从文本中提取所有 URL，逐个尝试匹配
        val urls = URL_EXTRACTOR.findAll(input).map { it.value }.toList()
        for (url in urls) {
            matchPlatform(url)?.let { return it }
        }

        return null
    }

    private fun matchPlatform(text: String): ParsedLink? {
        // 网易云
        for (pattern in NETEASE_PATTERNS) {
            pattern.find(text)?.let { match ->
                return ParsedLink("网易云", match.groupValues[1])
            }
        }
        // QQ音乐
        for (pattern in QQ_PATTERNS) {
            pattern.find(text)?.let { match ->
                return ParsedLink("QQ音乐", match.groupValues[1])
            }
        }
        // 酷我
        for (pattern in KUWO_PATTERNS) {
            pattern.find(text)?.let { match ->
                return ParsedLink("酷我音乐", match.groupValues[1])
            }
        }
        // 酷狗
        for (pattern in KUGOU_PATTERNS) {
            pattern.find(text)?.let { match ->
                return ParsedLink("酷狗音乐", match.groupValues[1])
            }
        }
        return null
    }
}
