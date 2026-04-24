package com.yindong.music.data.lx

import android.util.Log
import com.whl.quickjs.android.QuickJSLoader
import com.whl.quickjs.wrapper.QuickJSContext
import com.whl.quickjs.wrapper.JSObject
import com.whl.quickjs.wrapper.JSArray
import com.whl.quickjs.wrapper.JSFunction
import com.whl.quickjs.wrapper.JSCallFunction
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONException
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class RealQuickJsBridge : QuickJsBridge {
    companion object {
        private const val TAG = "RealQuickJsBridge"
        private var nativeLoaded = false

        @Synchronized
        fun ensureNativeLoaded() {
            if (!nativeLoaded) {
                QuickJSLoader.init()
                nativeLoaded = true
                Log.d(TAG, "QuickJSLoader.init() OK")
            }
        }
    }

    /**
     * Dedicated single-thread executor for ALL QuickJS operations.
     * QuickJSContext enforces same-thread access (checkSameThread), so the context
     * must be created AND used exclusively on this thread. Without this, calling
     * evaluate() from Dispatchers.IO while the context was created on Main would
     * throw QuickJSException, causing silent failures and request timeouts.
     */
    private val jsExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "QuickJS-${System.nanoTime() % 100000}").apply { isDaemon = true }
    }

    private var context: QuickJSContext? = null
    var initError: String? = null
        private set

    init {
        try {
            ensureNativeLoaded()
            context = runOnJsThread { QuickJSContext.create() }
            Log.d(TAG, "QuickJS init ok")
        } catch (e: Exception) {
            Log.e(TAG, "QuickJS init failed", e)
            initError = "QuickJS init failed: ${e.javaClass.simpleName}: ${e.message}"
        } catch (e: Error) {
            Log.e(TAG, "QuickJS init error (native)", e)
            initError = "QuickJS native error: ${e.javaClass.simpleName}: ${e.message}"
        }
    }

    /**
     * Execute a block on the dedicated QuickJS thread and return the result.
     * Blocks the calling thread until completion.
     */
    private fun <T> runOnJsThread(block: () -> T): T {
        val future = jsExecutor.submit(Callable { block() })
        return try {
            future.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    override fun evaluate(script: String, fileName: String) {
        val ctx = context ?: throw IllegalStateException("QuickJS context is null")
        try {
            runOnJsThread { ctx.evaluate(script) }
        } catch (e: Exception) {
            Log.e(TAG, "evaluate failed: $fileName", e)
            throw e
        }
    }

    override fun evaluateForResult(script: String, fileName: String): String? {
        val ctx = context ?: return null
        return try {
            runOnJsThread { ctx.evaluate(script, fileName)?.toString() }
        } catch (e: Exception) {
            Log.e(TAG, "evaluateForResult failed: $fileName", e)
            null
        }
    }

    override fun evaluateModule(script: String, fileName: String) {
        try {
            runOnJsThread { context?.evaluateModule(script, fileName) }
        } catch (e: Exception) {
            Log.e(TAG, "evaluateModule failed: $fileName", e)
            throw e
        }
    }

    override fun registerFunction(name: String, fn: (JSONArray) -> Any?) {
        try {
            val ctx = context
            if (ctx == null) {
                Log.e(TAG, "registerFunction[$name]: context is null!")
                return
            }
            runOnJsThread {
                val globalObj = ctx.getGlobalObject()
                if (globalObj == null) {
                    Log.e(TAG, "registerFunction[$name]: globalObject is null!")
                    return@runOnJsThread
                }
                Log.d(TAG, "registerFunction[$name]: setting property")
                globalObj.setProperty(name, JSCallFunction { args ->
                    // This callback runs on the JS thread (triggered during evaluate),
                    // so QuickJS operations inside are safe without runOnJsThread.
                    try {
                        val jsonArray = JSONArray()
                        if (args != null) {
                            for ((idx, arg) in args.withIndex()) {
                                jsonArray.put(jsValueToJson(arg))
                            }
                        }
                        val result = fn(jsonArray)
                        val converted = convertFromJson(result)
                        converted ?: ""
                    } catch (e: Exception) {
                        Log.e(TAG, "registerFunction[$name] callback error", e)
                        ""
                    }
                })
            }
        } catch (e: Exception) {
            Log.e(TAG, "registerFunction failed: $name", e)
        }
    }

    override fun callFunction(name: String, args: JSONArray): Any? {
        return try {
            runOnJsThread {
                val globalObj = context?.getGlobalObject() ?: return@runOnJsThread JSONObject()
                val func = globalObj.getJSFunction(name) ?: return@runOnJsThread JSONObject()
                val jsArgs = Array(args.length()) { i -> convertFromJson(args.opt(i)) }
                val result = func.call(*jsArgs)
                convertToJson(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "callFunction failed: $name", e)
            JSONObject().put("error", e.message ?: "call failed")
        }
    }

    override fun executePendingJobs() {
        // The native evaluate() internally calls JS_ExecutePendingJob() in a loop,
        // which flushes all pending microtasks (Promise callbacks, etc.).
        try {
            runOnJsThread { context?.evaluate("void 0;") }
        } catch (_: Exception) {}
    }

    override fun close() {
        try {
            runOnJsThread {
                context?.destroy()
                context = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "close failed", e)
        }
        jsExecutor.shutdown()
    }

    /** Convert JS callback argument to JSON-compatible value.
     *  Unlike convertToJson, this does NOT try to parse strings as JSON. */
    private fun jsValueToJson(value: Any?): Any? {
        return when (value) {
            null -> JSONObject.NULL
            is String -> value
            is Int -> value
            is Long -> value
            is Double -> value
            is Boolean -> value
            is JSArray -> {
                val jsonArr = JSONArray()
                for (i in 0 until value.length()) {
                    jsonArr.put(jsValueToJson(value.get(i)))
                }
                value.release()
                jsonArr
            }
            is JSFunction -> {
                value.release()
                null
            }
            is JSObject -> {
                // Could be a wrapped primitive or a real object
                val jsonObj = JSONObject()
                val names = value.getNames()
                if (names != null) {
                    for (i in 0 until names.length()) {
                        val key = names.get(i)?.toString() ?: continue
                        jsonObj.put(key, jsValueToJson(value.getProperty(key)))
                    }
                    names.release()
                }
                value.release()
                jsonObj
            }
            else -> value.toString()
        }
    }

    /** Convert Kotlin/JSON value to QuickJS value */
    private fun convertFromJson(value: Any?): Any? {
        return when (value) {
            null -> null
            is String -> value
            is Int -> value
            is Long -> value
            is Double -> value
            is Boolean -> value
            is JSONObject -> {
                val jsObj = context?.createNewJSObject() ?: return null
                val keys = value.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    setJsProperty(jsObj, key, convertFromJson(value.opt(key)))
                }
                jsObj
            }
            is JSONArray -> {
                val jsArr = context?.createNewJSArray() ?: return null
                for (i in 0 until value.length()) {
                    val converted = convertFromJson(value.opt(i))
                    setJsArrayElement(jsArr, i, converted)
                }
                jsArr
            }
            else -> value.toString()
        }
    }

    /** Convert QuickJS value to Kotlin/JSON value */
    private fun convertToJson(value: Any?): Any? {
        return when (value) {
            null -> null
            is String -> {
                // Try parsing as JSON object
                try { JSONObject(value) } catch (_: JSONException) { value }
            }
            is Int -> value
            is Long -> value
            is Double -> value
            is Boolean -> value
            is JSArray -> {
                val jsonArr = JSONArray()
                for (i in 0 until value.length()) {
                    jsonArr.put(convertToJson(value.get(i)))
                }
                value.release()
                jsonArr
            }
            is JSFunction -> {
                value.release()
                null
            }
            is JSObject -> {
                val jsonObj = JSONObject()
                val names = value.getNames()
                if (names != null) {
                    for (i in 0 until names.length()) {
                        val key = names.get(i)?.toString() ?: continue
                        jsonObj.put(key, convertToJson(value.getProperty(key)))
                    }
                    names.release()
                }
                value.release()
                jsonObj
            }
            else -> value
        }
    }

    /** Type-safe setProperty - JSObject.setProperty has typed overloads */
    private fun setJsProperty(obj: JSObject, key: String, value: Any?) {
        when (value) {
            null -> {
                // Set undefined/null by evaluating inline - no direct null setter in wrapper
                // Skip null properties to avoid issues
            }
            is String -> obj.setProperty(key, value)
            is Int -> obj.setProperty(key, value)
            is Long -> obj.setProperty(key, value)
            is Double -> obj.setProperty(key, value)
            is Boolean -> obj.setProperty(key, value)
            is JSObject -> obj.setProperty(key, value)
            else -> obj.setProperty(key, value.toString())
        }
    }

    /** Type-safe set for JSArray - JSArray.set(value, index) */
    private fun setJsArrayElement(arr: JSArray, index: Int, value: Any?) {
        when (value) {
            null -> arr.set("", index)
            is String -> arr.set(value, index)
            is Int -> arr.set(value, index)
            is Long -> arr.set(value, index)
            is Double -> arr.set(value, index)
            is Boolean -> arr.set(value, index)
            is JSObject -> arr.set(value, index)
            else -> arr.set(value.toString(), index)
        }
    }
}
