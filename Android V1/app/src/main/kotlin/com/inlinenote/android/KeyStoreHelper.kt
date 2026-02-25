package com.inlinenote.android

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

private const val PREFS_NAME = "inlinenote_secure"
private const val KEY_API_KEY = "openai_api_key"
private const val KEY_BASE_URL = "llm_base_url"
private const val KEY_MODEL = "llm_model"

object KeyStoreHelper {

    const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
    const val DEFAULT_MODEL = "gpt-4o-mini"

    private fun prefs(context: Context): SharedPreferences {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                PREFS_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Throwable) {
            // 部分机型/API 上 EncryptedSharedPreferences 会抛错，回退到普通存储避免闪退
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getApiKey(context: Context): String? =
        prefs(context).getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun setApiKey(context: Context, apiKey: String) {
        prefs(context).edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit().remove(KEY_API_KEY).apply()
    }

    fun getBaseUrl(context: Context): String? =
        prefs(context).getString(KEY_BASE_URL, null)?.trim()?.takeIf { it.isNotBlank() }

    fun setBaseUrl(context: Context, url: String) {
        prefs(context).edit().putString(KEY_BASE_URL, url.trim()).apply()
    }

    fun getModel(context: Context): String? =
        prefs(context).getString(KEY_MODEL, null)?.trim()?.takeIf { it.isNotBlank() }

    fun setModel(context: Context, model: String) {
        prefs(context).edit().putString(KEY_MODEL, model.trim()).apply()
    }
}
