package com.inlinenote.android

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 首页：API Key 输入 + 最小无障碍引导 + 悬浮窗权限引导。
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
        }

        val apiHint = TextView(this).apply {
            text = getString(R.string.api_key_hint)
            setPadding(0, 0, 0, 8)
        }
        val apiInput = EditText(this).apply {
            hint = "sk-..."
            setPadding(16, 16, 16, 16)
            setSingleLine(true)
        }
        KeyStoreHelper.getApiKey(this)?.let { apiInput.setText(it) }

        val saveBtn = Button(this).apply {
            text = getString(R.string.api_key_save)
            setOnClickListener {
                val key = apiInput.text?.toString()?.trim()
                if (key.isNullOrBlank()) {
                    Toast.makeText(this@MainActivity, getString(R.string.api_key_hint), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                KeyStoreHelper.setApiKey(this@MainActivity, key)
                Toast.makeText(this@MainActivity, getString(R.string.api_key_saved), Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(apiHint)
        layout.addView(apiInput)
        layout.addView(saveBtn)

        val guideTitle = TextView(this).apply {
            text = getString(R.string.accessibility_guide_title)
            setPadding(0, 32, 0, 8)
            textSize = 18f
        }
        val guideBody = TextView(this).apply {
            text = getString(R.string.accessibility_guide_body)
            setPadding(0, 0, 0, 16)
        }
        val goSettings = Button(this).apply {
            text = getString(R.string.accessibility_go_settings)
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }

        layout.addView(guideTitle)
        layout.addView(guideBody)
        layout.addView(goSettings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val overlayHint = TextView(this).apply {
                text = "需要「悬浮窗」权限才能在其他应用上显示解释浮窗。"
                setPadding(0, 24, 0, 8)
            }
            val goOverlay = Button(this).apply {
                text = "去开启悬浮窗权限"
                setOnClickListener {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
            layout.addView(overlayHint)
            layout.addView(goOverlay)
        }

        setContentView(layout)
    }
}
