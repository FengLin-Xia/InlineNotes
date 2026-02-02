package com.inlinenote.android.llm

/**
 * 解释来源抽象。业务层只依赖此接口；直连 OpenAI 或经后端由实现决定。
 */
interface LLMClient {
    /**
     * 对选中的一段文本请求大白话解释。V1 仅传选中文本，不传前后文。
     * @param selectedText 用户选中的原文
     * @return 成功时 [ExplainResult]，失败时 null（调用方在浮窗内提示错误）
     */
    suspend fun explain(selectedText: String): ExplainResult?
}
