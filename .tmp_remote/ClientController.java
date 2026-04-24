package com.cheang.cloudmusic.controller;

import com.cheang.cloudmusic.config.AppProperties;
import com.cheang.cloudmusic.dto.R;
import com.cheang.cloudmusic.service.PluginService;
import com.cheang.cloudmusic.util.HttpUtil;
import com.cheang.cloudmusic.util.SignUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 综合客户端接口
 *
 * 插件:
 *   GET  /api/builtin-plugin  — 内置插件信息
 *   GET  /api/pk              — 获取动态 Plugin Key
 *   GET  /api/pp              — 播放代理 (需 Plugin Token)
 *
 * 流代理:
 *   GET  /api/stream          — 透传音频流
 *
 * 外部解析 (兼容旧接口):
 *   GET  /kgqq/tx.php         — QQ外部解析
 *   GET  /kgqq/kg.php         — 酷狗外部解析
 *   GET  /wy.php              — 网易外部解析
 *   GET  /kw.php              — 酷我外部解析
 *   GET  /mg.php              — 咪咕外部解析
 *
 * 赞助:
 *   POST /api/sponsor         — 赞助验证
 *
 * 客户端:
 *   GET  /api/client/config   — 客户端配置 (公告/更新/版本等)
 *   GET  /api/ping            — 简单 ping
 */
@RestController
public class ClientController {

    private final PluginService pluginService;
    private final AppProperties props;

    public ClientController(PluginService pluginService, AppProperties props) {
        this.pluginService = pluginService;
        this.props = props;
    }

    // ==================== 插件 ====================

    @GetMapping("/api/builtin-plugin")
    public R builtinPlugin() {
        return R.ok(pluginService.builtinPlugin());
    }

    @GetMapping("/api/pk")
    public R pluginKey(@RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        if (deviceId == null || deviceId.isBlank()) return R.fail(400, "缺少设备ID");
        return R.ok(pluginService.generatePluginKey(deviceId));
    }

    @GetMapping("/api/pp")
    public R pluginPlay(@RequestParam String platform,
                        @RequestParam String songId,
                        @RequestParam(required = false) String quality,
                        @RequestHeader(value = "X-Plugin-Token", required = false) String token) {
        String err = pluginService.verifyPluginToken(token);
        if (err != null) return R.fail(403, err);
        return R.ok(pluginService.proxyPlay(platform, songId, quality));
    }

    // ==================== 流代理 ====================

    // 允许流代理的域名白名单
    private static final String[] STREAM_ALLOWED_HOSTS = {
            "music.163.com", "m801.music.126.net", "m701.music.126.net",
            "dl.stream.qqmusic.qq.com", "ws.stream.qqmusic.qq.com", "isure.stream.qqmusic.qq.com",
            "antiserver.kuwo.cn", "other.web.nf01.sycdn.kuwo.cn", "sy.kuwo.cn",
            "webfs.tx.kugou.com", "webfs.cloud.kugou.com",
            "music.nxinxz.com",
            "p1.music.126.net", "p2.music.126.net",
    };

    private static final long MAX_STREAM_BYTES = 50L * 1024 * 1024; // 50MB

    @GetMapping("/api/stream")
    public void stream(@RequestParam String url, HttpServletResponse response) throws IOException {
        // SSRF 防护: 只允许音乐 CDN 域名
        if (!isStreamUrlAllowed(url)) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"\u57df\u540d\u4e0d\u5728\u767d\u540d\u5355\"}");
            return;
        }
        // 流式转发，不将整个文件加载到内存
        try {
            var uri = java.net.URI.create(url);
            var req = java.net.http.HttpRequest.newBuilder(uri)
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                    .GET().build();
            var upstream = java.net.http.HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(10))
                    .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
                    .build()
                    .send(req, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

            // 检查 Content-Length 防 OOM
            long contentLength = upstream.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (contentLength > MAX_STREAM_BYTES) {
                upstream.body().close();
                response.setStatus(413);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":413,\"msg\":\"\u6587\u4ef6\u8fc7\u5927\"}");
                return;
            }

            response.setContentType(upstream.headers().firstValue("Content-Type").orElse("audio/mpeg"));
            if (contentLength > 0) response.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
            response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=3600");

            // 流式写入，并限制总字节数
            try (var in = upstream.body(); var out = response.getOutputStream()) {
                byte[] buf = new byte[8192];
                long total = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    total += n;
                    if (total > MAX_STREAM_BYTES) break; // 截断超大流
                    out.write(buf, 0, n);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            response.setStatus(500);
        } catch (Exception e) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"msg\":\"\u6d41\u83b7\u53d6\u5931\u8d25\"}");
        }
    }

    private boolean isStreamUrlAllowed(String url) {
        if (url == null || !url.startsWith("http")) return false;
        try {
            String host = java.net.URI.create(url).getHost();
            if (host == null) return false;
            for (String allowed : STREAM_ALLOWED_HOSTS) {
                if (host.equals(allowed) || host.endsWith("." + allowed)) return true;
            }
        } catch (Exception e) { return false; }
        return false;
    }

    // ==================== 外部解析代理 ====================

    /** id 只允许字母数字和常见分隔符，防止参数注入 */
    private static final java.util.regex.Pattern SAFE_ID = java.util.regex.Pattern.compile("^[a-zA-Z0-9_\\-]{1,128}$");

    @GetMapping("/kgqq/tx.php")
    public R qqProxy(@RequestParam String id) {
        if (!SAFE_ID.matcher(id).matches()) return R.fail(400, "参数非法");
        return proxyExternal("/kgqq/tx.php?id=" + id);
    }

    @GetMapping("/kgqq/kg.php")
    public R kugouProxy(@RequestParam String id) {
        if (!SAFE_ID.matcher(id).matches()) return R.fail(400, "参数非法");
        return proxyExternal("/kgqq/kg.php?id=" + id);
    }

    @GetMapping("/wy.php")
    public R neteaseProxy(@RequestParam String id) {
        if (!SAFE_ID.matcher(id).matches()) return R.fail(400, "参数非法");
        return proxyExternal("/wy.php?id=" + id);
    }

    @GetMapping("/kw.php")
    public R kuwoProxy(@RequestParam String id) {
        if (!SAFE_ID.matcher(id).matches()) return R.fail(400, "参数非法");
        return proxyExternal("/kw.php?id=" + id);
    }

    @GetMapping("/mg.php")
    public R miguProxy(@RequestParam String id) {
        if (!SAFE_ID.matcher(id).matches()) return R.fail(400, "参数非法");
        return proxyExternal("/mg.php?id=" + id);
    }

    private R proxyExternal(String path) {
        try {
            String body = HttpUtil.get(props.getExternalApiHost() + path);
            if (body != null) {
                return R.ok(new com.fasterxml.jackson.databind.ObjectMapper().readTree(body));
            }
        } catch (Exception ignored) {}
        return R.fail(500, "解析失败");
    }

    // ==================== 赞助 ====================

    @PostMapping("/api/sponsor")
    public R sponsor(@RequestBody Map<String, String> body) {
        String key = body.get("key");
        if (key == null) return R.fail(400, "缺少赞助码");
        String expected = props.getSponsorKey();
        // 恒定时间比较，防时序攻击
        boolean valid = expected != null && java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if (valid) {
            return R.ok(Map.of("valid", true, "msg", "感谢赞助！"));
        }
        return R.ok(Map.of("valid", false, "msg", "赞助码无效"));
    }

    // ==================== 客户端配置 ====================

    @GetMapping("/api/client/config")
    public R clientConfig(@RequestHeader(value = "X-App-Id", required = false) String appId) {
        Map<String, Object> officialContact = buildOfficialContactConfig();
        Map<String, Object> customData = new LinkedHashMap<>();
        customData.put("official_contact", officialContact);
        customData.put("force_show_official_contact", officialContact.get("force_show"));
        AppProperties.AppConfig cfg = null;
        if (appId != null && props.getApps() != null) {
            cfg = props.getApps().get(appId);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("official_contact", officialContact);
        result.put("custom_data", customData);
        if (cfg == null) {
            result.put("announcement", "");
            result.put("latestVersion", "1.0.0");
            result.put("forceUpdate", false);
            return R.ok(result);
        }
        result.put("appName", cfg.getName() != null ? cfg.getName() : appId);
        result.put("latestVersion", cfg.getLatestVersion() != null ? cfg.getLatestVersion() : "");
        result.put("minVersion", cfg.getMinVersion() != null ? cfg.getMinVersion() : "");
        result.put("updateUrl", cfg.getUpdateUrl() != null ? cfg.getUpdateUrl() : "");
        result.put("forceUpdate", cfg.isForceUpdate());
        result.put("announcement", cfg.getAnnouncement() != null ? cfg.getAnnouncement() : "");
        result.put("enabled", cfg.isAppEnabled());
        return R.ok(result);
    }

    private Map<String, Object> buildOfficialContactConfig() {
        String groupQq = digitsOnly(firstNonBlank(
                env("OFFICIAL_GROUP_QQ"),
                env("GROUP_QQ")
        ));
        String authorQq = digitsOnly(firstNonBlank(
                env("OFFICIAL_AUTHOR_QQ"),
                env("AUTHOR_QQ"),
                groupQq
        ));
        String joinUrl = firstNonBlank(
                sanitizeJoinUrl(env("OFFICIAL_JOIN_URL")),
                sanitizeJoinUrl(env("GROUP_JOIN_URL"))
        );
        String title = firstNonBlank(env("OFFICIAL_CONTACT_TITLE"), "QQ官方群");
        String subtitle = firstNonBlank(env("OFFICIAL_CONTACT_SUBTITLE"), "官方群与作者联系方式");
        boolean forceShow = toBool(env("FORCE_SHOW_OFFICIAL_CONTACT"));

        Map<String, Object> contact = new LinkedHashMap<>();
        contact.put("group_qq", groupQq);
        contact.put("author_qq", authorQq);
        contact.put("join_url", joinUrl);
        contact.put("title", title);
        contact.put("subtitle", subtitle);
        contact.put("force_show", forceShow);
        return contact;
    }

    private String env(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v.trim();
    }

    private String firstNonBlank(String... values) {
        if (values == null) return "";
        for (String v : values) {
            if (v != null) {
                String t = v.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return "";
    }

    private String digitsOnly(String value) {
        if (value == null) return "";
        return value.replaceAll("\\D", "");
    }

    private String sanitizeJoinUrl(String value) {
        if (value == null) return "";
        String v = value.trim();
        if (v.isEmpty()) return "";
        if (v.startsWith("https://") || v.startsWith("mqqapi://")) return v;
        return "";
    }

    private boolean toBool(String value) {
        if (value == null) return false;
        String v = value.trim().toLowerCase();
        return "1".equals(v) || "true".equals(v) || "yes".equals(v);
    }

    // ==================== Ping ====================

    @GetMapping("/api/ping")
    public R ping() {
        return R.ok(Map.of("pong", true, "ts", System.currentTimeMillis()));
    }
}
