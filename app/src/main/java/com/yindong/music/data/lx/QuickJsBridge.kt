package com.yindong.music.data.lx

import org.json.JSONArray
import org.json.JSONObject

interface QuickJsBridge : AutoCloseable {
    fun evaluate(script: String, fileName: String = "<eval>")
    fun evaluateModule(script: String, fileName: String = "<eval>") { evaluate(script, fileName) }
    fun registerFunction(name: String, fn: (JSONArray) -> Any?)
    fun callFunction(name: String, args: JSONArray = JSONArray()): Any?
    /** evaluate() but returns the result as a String. Used for JSON.stringify() calls. */
    fun evaluateForResult(script: String, fileName: String = "<eval>"): String? { evaluate(script, fileName); return null }
    fun executePendingJobs() {}
    override fun close()
}

class NoopQuickJsBridge : QuickJsBridge {
    override fun evaluate(script: String, fileName: String) = Unit
    override fun evaluateModule(script: String, fileName: String) = Unit
    override fun registerFunction(name: String, fn: (JSONArray) -> Any?) = Unit
    override fun callFunction(name: String, args: JSONArray): Any? = JSONObject()
    override fun evaluateForResult(script: String, fileName: String): String? = null
    override fun executePendingJobs() = Unit
    override fun close() = Unit
}
