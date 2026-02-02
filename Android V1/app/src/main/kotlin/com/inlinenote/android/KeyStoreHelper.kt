package com.inlinenote.android

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_NAME = "inlinenote_secure"
private const val KEY_API_KEY = "openai_api_key"

object KeyStoreHelper {

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(context: Context): String? =
        prefs(context).getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() }

    fun setApiKey(context: Context, apiKey: String) {
        prefs(context).edit().putString(KEY_API_KEY, apiKey.trim()).apply()
    }

    fun clearApiKey(context: Context) {
        prefs(context).edit().remove(KEY_API_KEY).apply()
    }
}
