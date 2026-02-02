package com.inlinenote.android.llm

/**
 * V1: 单轮解释结果。大白话 + 要点拆解，可选潜台词。
 */
data class ExplainResult(
    val plainExplanation: String,
    val bulletPoints: List<String> = emptyList(),
    val subtext: String? = null
) {
    fun toDisplayText(): String {
        val sb = StringBuilder(plainExplanation)
        if (bulletPoints.isNotEmpty()) {
            sb.append("\n\n")
            bulletPoints.forEach { sb.append("• ").append(it).append("\n") }
        }
        subtext?.let { sb.append("\n\n").append(it) }
        return sb.toString().trim()
    }
}
