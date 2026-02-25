package com.inlinenote.android.llm

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.Inet4Address
import java.net.InetAddress
import java.net.SocketException
import java.util.concurrent.TimeUnit

/**
 * 直连 OpenAI 兼容 API（可配置 base_url 与 model，用于代理或其它兼容服务）。
 */
class OpenAIClientDirect(
    private val apiKey: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val model: String = "gpt-4o-mini"
) : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .protocols(listOf(Protocol.HTTP_1_1))
        .dns(IPv4PreferDns)
        .build()

    private val gson = Gson()

    override suspend fun explain(selectedText: String): ExplainResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val url = buildRequestUrl()
        Log.d(TAG, "request url: $url")
        val body = buildRequestJson(selectedText, url)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        runWithRetry(maxAttempts = 3) { attempt ->
            try {
                httpClient.newCall(request).execute().use { response ->
                    val raw = response.body?.string() ?: ""
                    if (!response.isSuccessful) {
                        Log.e(TAG, "API failed: ${response.code} ${response.message} url=$url")
                        Log.e(TAG, "body: ${raw.take(500)}")
                        return@runWithRetry null
                    }
                    parseResponse(raw)
                }
            } catch (e: Exception) {
                val isConnectionAbort = e is SocketException ||
                    (e.message?.contains("connection abort", ignoreCase = true) == true) ||
                    (e.message?.contains("Connection reset", ignoreCase = true) == true)
                if (isConnectionAbort && attempt < 2) {
                    Log.w(TAG, "Connection abort (attempt ${attempt + 1}/3), retry in 2s")
                    null
                } else {
                    Log.e(TAG, "API request error: ${e.message}", e)
                    throw e
                }
            }
        }
    }

    private inline fun <T> runWithRetry(maxAttempts: Int, block: (attempt: Int) -> T?): T? {
        for (attempt in 0 until maxAttempts) {
            try {
                val result = block(attempt)
                if (result != null) return result
                if (attempt < maxAttempts - 1) Thread.sleep(2000)
            } catch (e: Exception) {
                if (attempt == maxAttempts - 1) {
                    Log.e(TAG, "API request error: ${e.message}", e)
                    return null
                }
                Thread.sleep(2000)
            }
        }
        return null
    }

    private companion object {
        const val TAG = "InlineNote/LLM"
        /** 同 WiFi 下电脑能连、手机 TLS 被 abort 时，常是手机走了 IPv6 而路径不通；优先 IPv4 可规避。 */
        private val IPv4PreferDns = object : Dns {
            override fun lookup(hostname: String): List<InetAddress> {
                val all = Dns.SYSTEM.lookup(hostname)
                val v4 = all.filterIsInstance<Inet4Address>()
                return if (v4.isNotEmpty()) v4 else all
            }
        }
    }

    /** 若 Base URL 已是完整 endpoint（如 MiniMax /v1/text/chatcompletion_v2）则直接用，否则拼 /chat/completions。 */
    private fun buildRequestUrl(): String {
        val base = baseUrl.trimEnd('/')
        return when {
            base.contains("chatcompletion") || base.endsWith("chat/completions") -> base
            else -> "$base/chat/completions"
        }
    }

    private fun buildRequestJson(selectedText: String, requestUrl: String): String {
        val system = """
            你是一个「大白话解释」助手。用户会选中一段难以理解的句子（可能是术语、黑话、套话）。
            请用一两段大白话解释这段话在说什么；再用 3 条以内的要点拆解；如有潜台词或弦外之音可简短提示。
            不要复述原文，不要加「根据您选中的内容」之类的前缀。直接给解释。
        """.trimIndent()
        val user = "请解释下面这段话：\n\n$selectedText"
        val messages = listOf(
            mapOf("role" to "system", "content" to system),
            mapOf("role" to "user", "content" to user)
        )
        val useMinimaxParams = requestUrl.contains("minimax")
        val payload = buildMap<String, Any> {
            put("model", model)
            put("messages", messages)
            put(if (useMinimaxParams) "max_completion_tokens" else "max_tokens", 500)
            put("temperature", 0.3)
        }
        return gson.toJson(payload)
    }

    private fun parseResponse(raw: String): ExplainResult? {
        val parsed = gson.fromJson(raw, OpenAIResponse::class.java)
        val content = parsed.choices?.firstOrNull()?.message?.content ?: return null
        return parseContentToResult(content)
    }

    private fun parseContentToResult(content: String): ExplainResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val plain = mutableListOf<String>()
        val bullets = mutableListOf<String>()
        var subtext: String? = null
        var inBullets = false
        for (line in lines) {
            when {
                line.startsWith("•") || line.startsWith("-") || line.matches(Regex("^\\d+\\.\\s.*")) -> {
                    inBullets = true
                    bullets.add(line.replace(Regex("^[•\\-]\\s*|^\\d+\\.\\s*"), ""))
                }
                line.lowercase().startsWith("潜台词") || line.lowercase().startsWith("弦外之音") -> {
                    subtext = line.replace(Regex("^(潜台词|弦外之音)[：:]?\\s*", RegexOption.IGNORE_CASE), "")
                }
                inBullets -> bullets.add(line)
                else -> plain.add(line)
            }
        }
        return ExplainResult(
            plainExplanation = plain.joinToString("\n").ifEmpty { content },
            bulletPoints = bullets,
            subtext = subtext?.takeIf { it.isNotBlank() }
        )
    }

    private data class OpenAIResponse(
        val choices: List<Choice>?
    )

    private data class Choice(
        val message: Message?
    )

    private data class Message(
        val content: String?
    )
}
