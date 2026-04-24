package com.yindong.music.data.lx

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class LxJsRuntime(
    private val context: Context,
    private val quickJsBridge: QuickJsBridge,
    private val options: LxRuntimeOptions,
    private val logCallback: ((String) -> Unit)? = null,
) : AutoCloseable {

    companion object {
        private const val TAG = "LxJsRuntime"
    }

    private var initedPayload: JSONObject? = null
    private var currentRequestHandlerId: String? = null

    private fun log(msg: String) {
        Log.d(TAG, msg)
        try { logCallback?.invoke(msg) } catch (_: Exception) {}
    }

    fun loadPluginScript(jsCode: String) {
        registerNativeFunctions()
        log("loading plugin (${jsCode.length} chars)...")

        if (quickJsBridge is RealQuickJsBridge) {
            val initErr = (quickJsBridge as RealQuickJsBridge).initError
            if (initErr != null) {
                log("FATAL: $initErr")
                return
            }
        }

        // Step 1: evaluate minimal prelude (MusicFree style)
        quickJsBridge.evaluate(buildPrelude(), "prelude.js")

        // Step 2: wrap and evaluate plugin (EXACTLY like MusicFree)
        val wrapped = buildMusicFreeWrapper(jsCode)
        log("wrapped plugin length: ${wrapped.length}")
        
        try {
            quickJsBridge.evaluate(wrapped, "plugin.js")
            log("plugin evaluated OK")
        } catch (e: Exception) {
            log("plugin eval error: ${e.message}")
            throw e
        }

        // Step 3: extract instance from module.exports
        try {
            quickJsBridge.evaluate("""
                var pluginInstance = globalThis.__lxModule.exports;
                if (pluginInstance && pluginInstance.default) {
                    pluginInstance = pluginInstance.default;
                }
                if (pluginInstance && typeof pluginInstance === 'object') {
                    globalThis.__lxPluginInstance = pluginInstance;
                    __lxNativeLog('Plugin instance extracted OK, platform=' + (pluginInstance.platform || 'unknown'));
                } else {
                    __lxNativeLog('WARN: no valid plugin instance in exports');
                }
            """.trimIndent(), "extract_instance.js")
        } catch (e: Exception) {
            log("instance extraction error: ${e.message}")
        }
    }

    /**
     * MusicFree-style prelude - minimal, clean, ES5 compatible
     * Includes common packages that plugins may require
     */
    private fun buildPrelude(): String {
        return """
// ── Module system (CommonJS) ──
globalThis.__lxModule = { exports: {} };

// ── Console ──
globalThis.console = {
    log: function() { __lxNativeLog('[LOG] ' + Array.prototype.slice.call(arguments).join(' ')); },
    warn: function() { __lxNativeLog('[WARN] ' + Array.prototype.slice.call(arguments).join(' ')); },
    error: function() { __lxNativeLog('[ERR] ' + Array.prototype.slice.call(arguments).join(' ')); },
    info: function() { __lxNativeLog('[INFO] ' + Array.prototype.slice.call(arguments).join(' ')); }
};

// ── Environment 
globalThis.env = {
    getUserVariables: function() { return {}; },
    userVariables: {},
    appVersion: '1.0',
    os: 'android',
    lang: 'zh-CN'
};

globalThis.process = {
    platform: 'android',
    version: '1.0',
    env: globalThis.env
};

// ── URL 
globalThis.URL = function(url) { 
    this.href = url;
    this.toString = function() { return url; };
    this.protocol = url.split(':')[0] || '';
    this.hostname = '';
    this.pathname = '';
    try {
        var parts = url.split('://');
        if (parts.length > 1) {
            var rest = parts[1];
            var pathIdx = rest.indexOf('/');
            if (pathIdx !== -1) {
                this.hostname = rest.substring(0, pathIdx);
                this.pathname = rest.substring(pathIdx);
            } else {
                this.hostname = rest;
            }
        }
    } catch(e) {}
};

// ── LX API (placeholder for plugin compatibility) ──
globalThis.lx = {
    request: function() { 
        __lxNativeLog('lx.request not implemented');
        return Promise.reject(new Error('lx.request not implemented'));
    },
    utils: {
        crypto: {
            aesEncrypt: function() { throw new Error('lx.utils.crypto.aesEncrypt not implemented'); },
            aesDecrypt: function() { throw new Error('lx.utils.crypto.aesDecrypt not implemented'); },
            md5: function() { throw new Error('lx.utils.crypto.md5 not implemented'); },
            sha256: function() { throw new Error('lx.utils.crypto.sha256 not implemented'); }
        }
    }
};

// ── Request handling ──
globalThis.__lxHandlers = {};

// ── Package registry (MusicFree compatible) ──
globalThis.__lxPackages = {};

// he - HTML entity encoder/decoder (stub implementation)
globalThis.__lxPackages['he'] = {
    encode: function(str) { 
        return String(str).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    },
    decode: function(str) { 
        return String(str).replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&amp;/g, '&');
    },
    default: function() { return this; }
};

// cheerio - HTML parser (stub - very basic)
globalThis.__lxPackages['cheerio'] = {
    load: function(html) {
        return function(selector) {
            return {
                text: function() { return ''; },
                attr: function() { return ''; },
                html: function() { return html || ''; },
                length: 0
            };
        };
    },
    default: function() { return this; }
};

// crypto-js - cryptography (stub)
globalThis.__lxPackages['crypto-js'] = {
    MD5: function() { return { toString: function() { return ''; } }; },
    SHA256: function() { return { toString: function() { return ''; } }; },
    enc: { Hex: {} },
    default: function() { return this; }
};

// axios - HTTP client (stub - will use native bridge)
globalThis.__lxPackages['axios'] = {
    get: function() { return Promise.reject(new Error('axios.get not implemented')); },
    post: function() { return Promise.reject(new Error('axios.post not implemented')); },
    default: function() { return this; }
};

// dayjs - date library (stub)
globalThis.__lxPackages['dayjs'] = function() { 
    return { format: function() { return ''; }, unix: function() { return 0; } }; 
};
globalThis.__lxPackages['dayjs'].default = globalThis.__lxPackages['dayjs'];

// qs - query string parser (stub)
globalThis.__lxPackages['qs'] = {
    parse: function() { return {}; },
    stringify: function() { return ''; },
    default: function() { return this; }
};

// big-integer - big number support (stub)
globalThis.__lxPackages['big-integer'] = function(v) { 
    return { 
        value: v, 
        toString: function() { return String(v); },
        add: function(n) { return globalThis.__lxPackages['big-integer'](parseInt(v||0) + parseInt(n||0)); }
    }; 
};
globalThis.__lxPackages['big-integer'].default = globalThis.__lxPackages['big-integer'];

// pako - zlib compression (stub)
globalThis.__lxPackages['pako'] = {
    inflate: function() { throw new Error('pako.inflate not implemented'); },
    deflate: function() { throw new Error('pako.deflate not implemented'); },
    default: function() { return this; }
};

// buffer - Node.js Buffer (stub)
globalThis.__lxPackages['buffer'] = {
    Buffer: {
        from: function() { return { toString: function() { return ''; } }; }
    },
    default: function() { return this; }
};

// webdav (stub)
globalThis.__lxPackages['webdav'] = {
    createClient: function() { throw new Error('webdav not implemented'); },
    default: function() { return this; }
};

// ── Native bridge functions ──
globalThis.require = function(moduleName) {
    // __lxNativeLog('require: ' + moduleName);
    
    // Check registered packages first
    if (globalThis.__lxPackages && globalThis.__lxPackages[moduleName]) {
        return globalThis.__lxPackages[moduleName];
    }
    
    // For missing modules, return a dummy object instead of throwing
    // This prevents "Module not found" from crashing the entire plugin import
    return {
        default: {},
        toString: function() { return '[Stub: ' + moduleName + ']'; }
    };
};

// ── Promise utilities ──
globalThis.__lxNativeResolvePromise = function(handlerId, result) {
    if (globalThis.__lxHandlers[handlerId]) {
        globalThis.__lxHandlers[handlerId].done = true;
        globalThis.__lxHandlers[handlerId].result = result;
    }
};

globalThis.__lxNativeRejectPromise = function(handlerId, error) {
    if (globalThis.__lxHandlers[handlerId]) {
        globalThis.__lxHandlers[handlerId].done = true;
        globalThis.__lxHandlers[handlerId].result = { error: error };
    }
};
""".trimIndent()
    }

    /**
     * Pre-process plugin code to be more compatible with QuickJS (ES5)
     */
    private fun preprocessPluginCode(code: String): String {
        var processed = code
            // 1. Replace const/let with var
            .replace(Regex("(^|\\n|;|\\s)(const|let)\\s+", RegexOption.MULTILINE), "$1var ")
            // 2. Remove BOM
            .replace("\uFEFF", "")
            // 3. Convert arrow functions to standard functions
            .replace(Regex("\\(\\s*\\)\\s*=>\\s*\\{"), "function() {")
            .replace(Regex("\\(([^)]+)\\)\\s*=>\\s*\\{"), "function($1) {")
            // 4. Handle object method shorthand: name() {} -> name: function() {}
            .replace(Regex("(,|\\{|^)\\s*([a-zA-Z0-9_]+)\\s*\\(\\s*\\)\\s*\\{"), "$1 $2: function() {")
            .replace(Regex("(,|\\{|^)\\s*([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)\\s*\\{"), "$1 $2: function($3) {")

        return processed
    }

    /**
     * MusicFree-style wrapper - Final robust version for QuickJS
     */
    private fun buildMusicFreeWrapper(rawCode: String): String {
        val processedCode = preprocessPluginCode(rawCode)
        
        return """
(function() {
    'use strict';
    
    // Ensure all required globals are present on globalThis
    if (!globalThis.__lxModule) globalThis.__lxModule = { exports: {} };
    if (!globalThis.console) globalThis.console = { log: function(){}, error: function(){} };
    if (!globalThis.env) globalThis.env = {};
    if (!globalThis.process) globalThis.process = { env: {} };
    if (!globalThis.URL) globalThis.URL = function(u){ this.href = u; };
    
    // Critical: Ensure lx.request is a callable function
    if (!globalThis.lx) globalThis.lx = {};
    if (typeof globalThis.lx.request !== 'function') {
        globalThis.lx.request = function(params) {
            __lxNativeLog('lx.request called: ' + (params.url || 'unknown'));
            var handlerId = 'req_' + Math.random().toString(36).substr(2, 9);
            globalThis.__lxHandlers[handlerId] = { done: false, result: null };
            
            try {
                __lxNativeRequest(
                    params.method || 'get',
                    params.url,
                    JSON.stringify(params.headers || {}),
                    handlerId,
                    params.body ? JSON.stringify(params.body) : ''
                );
            } catch(e) {
                globalThis.__lxHandlers[handlerId].done = true;
                globalThis.__lxHandlers[handlerId].result = { error: e.message };
            }

            return {
                then: function(onSuccess, onError) {
                    var timer = setInterval(function() {
                        if (globalThis.__lxHandlers[handlerId].done) {
                            clearInterval(timer);
                            var res = globalThis.__lxHandlers[handlerId].result;
                            if (res && res.error) {
                                if (onError) onError(new Error(res.error));
                            } else {
                                if (onSuccess) onSuccess(res);
                            }
                        }
                    }, 50);
                    return this;
                }
            };
        };
    }

    // Map to local scope to mimic CommonJS environment
    var module = globalThis.__lxModule;
    var exports = module.exports;
    var require = globalThis.require;
    var console = globalThis.console;
    var env = globalThis.env;
    var URL = globalThis.URL;
    var process = globalThis.process;
    var lx = globalThis.lx;
    
    try {
        // Execute the pre-processed plugin code
$processedCode
        
        if (module.exports && typeof module.exports === 'object') {
            globalThis.__lxPluginInstance = module.exports;
            __lxNativeLog('Plugin instance captured from module.exports');
        }
        
    } catch (e) {
        __lxNativeLog('FATAL Plugin Error: ' + e.message);
        throw e;
    }
})();
""".trimIndent()
    }

    private fun registerNativeFunctions() {
        quickJsBridge.registerFunction("__lxNativeLog") { args ->
            val msg = if (args.length() > 0) args.opt(0)?.toString() else ""
            log("[JS] $msg")
        }
        quickJsBridge.registerFunction("__lxNativeOn") { args ->
            val event = if (args.length() > 0) args.opt(0)?.toString() else ""
            val id = if (args.length() > 1) args.opt(1)?.toString() else ""
            log("on: event=$event, id=$id")
        }
        quickJsBridge.registerFunction("__lxNativeSend") { args ->
            log("send: ${(if (args.length() > 0) args.opt(0) else "")}")
            emptyMap<String, Any?>()
        }
        quickJsBridge.registerFunction("__lxNativeRequest") { args ->
            try {
                val method = if (args.length() > 0) args.optString(0) else "get"
                val url = if (args.length() > 1) args.optString(1) else ""
                val configStr = if (args.length() > 2) args.optString(2) else "{}"
                val handlerId = if (args.length() > 3) args.optString(3) else ""
                val dataStr = if (args.length() > 4) args.optString(4) else null
                
                log("request: method=$method, url=$url, handler=$handlerId")
                
                // Execute request asynchronously
                Thread {
                    try {
                        val client = OkHttpClient.Builder()
                            .connectTimeout(options.callTimeoutMs, TimeUnit.MILLISECONDS)
                            .readTimeout(options.callTimeoutMs, TimeUnit.MILLISECONDS)
                            .build()
                        
                        val requestBuilder = Request.Builder().url(url)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
                        
                        if (method.equals("post", ignoreCase = true) && dataStr != null) {
                            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                            val body = RequestBody.create(mediaType, dataStr)
                            requestBuilder.post(body)
                        } else {
                            requestBuilder.get()
                        }
                        
                        val response = client.newCall(requestBuilder.build()).execute()
                        val responseBody = response.body?.string() ?: ""
                        
                        val result = JSONObject().apply {
                            put("statusCode", response.code)
                            put("body", responseBody)
                            put("headers", JSONObject(response.headers.toMap()))
                        }
                        
                        // Resolve the promise
                        quickJsBridge.evaluate(
                            "__lxNativeResolvePromise('$handlerId', ${result.toString()})",
                            "resolve.js"
                        )
                    } catch (e: Exception) {
                        log("request error: ${e.message}")
                        quickJsBridge.evaluate(
                            "__lxNativeRejectPromise('$handlerId', '${e.message?.replace("'", "\\'")}')",
                            "reject.js"
                        )
                    }
                }.start()
                
                emptyMap<String, Any?>()
            } catch (e: Exception) {
                log("native request setup error: ${e.message}")
                emptyMap<String, Any?>()
            }
        }
        quickJsBridge.registerFunction("__lxNativeCancelRequest") { args ->
            val handlerId = if (args.length() > 0) args.optString(0) else ""
            log("cancel: handler=$handlerId")
        }
        quickJsBridge.registerFunction("__lxNativeResolvePromise") { args ->
            val handlerId = if (args.length() > 0) args.optString(0) else ""
            val resultStr = if (args.length() > 1) args.optString(1) else "{}"
            log("resolve: handler=$handlerId")
            // Store result for JS polling
            try {
                quickJsBridge.evaluate(
                    "if(globalThis.__lxHandlers['$handlerId']){globalThis.__lxHandlers['$handlerId'].done=true;globalThis.__lxHandlers['$handlerId'].result=$resultStr;}",
                    "store_resolve.js"
                )
            } catch (e: Exception) {
                log("store resolve error: ${e.message}")
            }
        }
        quickJsBridge.registerFunction("__lxNativeRejectPromise") { args ->
            val handlerId = if (args.length() > 0) args.optString(0) else ""
            val errorStr = if (args.length() > 1) args.optString(1) else "unknown error"
            log("reject: handler=$handlerId, error=$errorStr")
            try {
                quickJsBridge.evaluate(
                    "if(globalThis.__lxHandlers['$handlerId']){globalThis.__lxHandlers['$handlerId'].done=true;globalThis.__lxHandlers['$handlerId'].result={error:'${errorStr.replace("'", "\\'")}'};}",
                    "store_reject.js"
                )
            } catch (e: Exception) {
                log("store reject error: ${e.message}")
            }
        }
        quickJsBridge.registerFunction("__lxNativeCallRequest") { args ->
            mapOf("done" to true, "value" to mapOf("error" to "not implemented"))
        }
    }

    fun hasHandler(): Boolean {
        return try {
            quickJsBridge.evaluate("Object.keys(globalThis.__lxHandlers || {}).length > 0", "check.js")
            true
        } catch (e: Exception) { false }
    }

    fun callRequest(payload: JSONObject): JSONObject {
        return try {
            val handlerId = currentRequestHandlerId ?: return JSONObject().put("error", "no handler")
            val args = JSONArray().put(handlerId).put("req_${System.currentTimeMillis()}").put(payload)
            val result = quickJsBridge.callFunction("__lxNativeCallRequest", args)
            when (result) {
                is Map<*, *> -> {
                    val value = result["value"]
                    when (value) {
                        is Map<*, *> -> JSONObject(value.mapKeys { it.key.toString() })
                        else -> JSONObject().put("result", value?.toString())
                    }
                }
                else -> JSONObject()
            }
        } catch (e: Exception) {
            log("callRequest error: ${e.message}")
            JSONObject().put("error", e.message)
        }
    }

    fun getInitedPayload(): JSONObject? = initedPayload

    override fun close() { quickJsBridge.close() }
}
