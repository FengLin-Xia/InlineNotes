package com.inlinenote.android.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import com.inlinenote.android.R

/**
 * 系统级浮窗，展示解释内容。可拖拽、可关闭，不持久保存。
 * V1：先可用，不设计；失败时在浮窗内显示人话错误提示 + 关闭按钮。
 */
class ExplanationOverlay(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private val layoutParams: WindowManager.LayoutParams
        get() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
            width = (context.resources.displayMetrics.widthPixels * 0.9f).toInt()
            height = WindowManager.LayoutParams.WRAP_CONTENT
        }

    fun showExplanation(text: String, onClose: () -> Unit) {
        dismiss()
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.overlay_explanation, null)
        root.findViewById<TextView>(R.id.overlay_text).text = text
        root.findViewById<Button>(R.id.overlay_close).setOnClickListener {
            dismiss()
            onClose()
        }
        params = layoutParams
        overlayView = root
        windowManager.addView(root, params)
    }

    fun showError(message: String, onClose: () -> Unit) {
        dismiss()
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.overlay_explanation, null)
        root.findViewById<TextView>(R.id.overlay_text).text = message
        root.findViewById<Button>(R.id.overlay_close).setOnClickListener {
            dismiss()
            onClose()
        }
        params = layoutParams
        overlayView = root
        windowManager.addView(root, params)
    }

    fun dismiss() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { }
        }
        overlayView = null
        params = null
    }

    fun isShowing(): Boolean = overlayView != null
}
