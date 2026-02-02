package com.inlinenote.android.trigger

import android.content.Context
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.inlinenote.android.R

/**
 * 选中文本后显示的轻量浮动按钮「解释」。用户点击后才进入解释流程。
 * V1：看得清、点得到；可拖拽（可选，先实现固定位置或简单跟随）。
 */
class FloatingTriggerView(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var triggerView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var lastX = 0f
    private var lastY = 0f
    private var initialX = 0
    private var initialY = 0
    private var dragged = false
    private val dragThresholdPx = 10

    private val layoutParams: WindowManager.LayoutParams
        get() = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 200
            y = 400
        }

    fun show(onClick: () -> Unit) {
        dismiss()
        val inflater = LayoutInflater.from(context)
        val root = inflater.inflate(R.layout.trigger_floating_button, null)
        val button = root.findViewById<Button>(R.id.trigger_explain)
        button.text = context.getString(R.string.trigger_explain)
        button.setOnClickListener {
            if (!dragged) onClick()
        }
        button.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragged = false
                    lastX = event.rawX
                    lastY = event.rawY
                    params?.let { initialX = it.x; initialY = it.y }
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    if (dx * dx + dy * dy > dragThresholdPx * dragThresholdPx) dragged = true
                    params?.let {
                        it.x = (initialX + dx).toInt()
                        it.y = (initialY + dy).toInt()
                        windowManager.updateViewLayout(triggerView, it)
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                }
            }
            dragged
        }
        params = layoutParams
        triggerView = root
        windowManager.addView(root, params)
    }

    fun dismiss() {
        triggerView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) { }
        }
        triggerView = null
        params = null
    }

    fun isShowing(): Boolean = triggerView != null
}
