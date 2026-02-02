package com.inlinenote.android.llm

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * V1：App 直连 OpenAI。API Key 由调用方传入（从 Keystore/EncryptedSharedPreferences 读取）。
 */
class OpenAIClientDirect(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : LLMClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    override suspend fun explain(selectedText: String): ExplainResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null
        val body = buildRequestJson(selectedText)
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val raw = response.body?.string() ?: return@withContext null
                parseResponse(raw)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildRequestJson(selectedText: String): String {
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
        val payload = mapOf(
            "model" to model,
            "messages" to messages,
            "max_tokens" to 500,
            "temperature" to 0.3
        )
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
