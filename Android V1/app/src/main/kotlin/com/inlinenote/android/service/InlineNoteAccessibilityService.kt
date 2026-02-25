package com.inlinenote.android.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ClipboardManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.inlinenote.android.KeyStoreHelper
import com.inlinenote.android.overlay.ExplanationOverlay
import com.inlinenote.android.trigger.FloatingTriggerView
import com.inlinenote.android.llm.LLMClient
import com.inlinenote.android.llm.OpenAIClientDirect
import com.inlinenote.android.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 双触发：① 监听 TYPE_VIEW_TEXT_SELECTION_CHANGED；② 监听剪贴板变化（复制后出解释按钮）。
 * 显示浮动「解释」按钮，用户点击后调用 LLM 并展示浮窗。
 */
class InlineNoteAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var triggerView: FloatingTriggerView
    private lateinit var overlay: ExplanationOverlay
    private var llmClient: LLMClient? = null
    private var lastSelectedText: String = ""

    private var clipboardManager: ClipboardManager? = null
    private val clipListener = ClipboardManager.OnPrimaryClipChangedListener { onPrimaryClipChanged() }
    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastClipboardTriggerTime = 0L
    private val clipboardDebounceMs = 800L

    override fun onCreate() {
        super.onCreate()
        triggerView = FloatingTriggerView(this)
        overlay = ExplanationOverlay(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
        }
        serviceInfo = info
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as? ClipboardManager
        clipboardManager?.addPrimaryClipChangedListener(clipListener)
    }

    private fun onPrimaryClipChanged() {
        mainHandler.post {
            val now = System.currentTimeMillis()
            if (now - lastClipboardTriggerTime < clipboardDebounceMs) return@post
            val clip = clipboardManager?.primaryClip ?: return@post
            if (clip.itemCount == 0) return@post
            val text = clip.getItemAt(0).text?.toString()?.trim() ?: return@post
            if (!isReasonableSelection(text)) return@post
            lastClipboardTriggerTime = now
            lastSelectedText = text
            triggerView.dismiss()
            triggerView.show { onExplainClicked() }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) return
        val source = event.source
        var text = source?.let { getSelectedText(it) }
        if (source != null) source.recycle()
        // 部分应用（如 Chrome）不在节点里暴露选中内容，但会放在 event.text 里
        if (text.isNullOrBlank() && event.text != null && event.text.isNotEmpty()) {
            text = event.text.firstOrNull()?.toString()?.trim()
        }
        if (text.isNullOrBlank()) {
            triggerView.dismiss()
            return
        }
        if (!isReasonableSelection(text)) {
            triggerView.dismiss()
            return
        }
        lastSelectedText = text.trim()
        triggerView.dismiss()
        triggerView.show { onExplainClicked() }
    }

    override fun onInterrupt() {
        triggerView.dismiss()
        overlay.dismiss()
    }

    override fun onDestroy() {
        clipboardManager?.removePrimaryClipChangedListener(clipListener)
        clipboardManager = null
        scope.cancel()
        triggerView.dismiss()
        overlay.dismiss()
        super.onDestroy()
    }

    private fun getSelectedText(node: AccessibilityNodeInfo): String? {
        if (node.text != null && node.text.isNotEmpty()) {
            val start = node.textSelectionStart.coerceAtLeast(0)
            val end = node.textSelectionEnd.coerceAtLeast(0)
            if (end > start) {
                val seq = node.text
                return seq.subSequence(start, end).toString()
            }
        }
        for (i in 0 until node.childCount) {
            getSelectedText(node.getChild(i) ?: continue)?.let { return it }
        }
        return null
    }

    private fun isReasonableSelection(text: String): Boolean {
        val t = text.trim()
        if (t.length < 2 || t.length > 2000) return false
        if (t.isBlank()) return false
        return true
    }

    private fun onExplainClicked() {
        triggerView.dismiss()
        val apiKey = KeyStoreHelper.getApiKey(this)
        if (apiKey.isNullOrBlank()) {
            overlay.showError(getString(R.string.error_generic)) {}
            openSettingsForApiKey()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            overlay.showError("需要先开启悬浮窗权限") {}
            openSettingsForApiKey()
            return
        }
        val baseUrl = KeyStoreHelper.getBaseUrl(this) ?: KeyStoreHelper.DEFAULT_BASE_URL
        val model = KeyStoreHelper.getModel(this) ?: KeyStoreHelper.DEFAULT_MODEL
        llmClient = OpenAIClientDirect(apiKey, baseUrl, model)
        val client = llmClient ?: return
        val selected = lastSelectedText
        scope.launch {
            val result = withContext(Dispatchers.IO) { client.explain(selected) }
            if (result != null) {
                overlay.showExplanation(result.toDisplayText()) {}
            } else {
                overlay.showError(getString(R.string.error_network)) {}
            }
        }
    }

    private fun openSettingsForApiKey() {
        val intent = Intent(this, com.inlinenote.android.MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }
}
