package com.example.link.data

import android.content.Context
import com.example.link.BuildConfig
import java.util.UUID

class ConnectionStore(context: Context) {
    private val prefs = context.getSharedPreferences("guardian_connection", Context.MODE_PRIVATE)

    val installId: String
        get() = prefs.getString(KEY_INSTALL_ID, null)
            ?: UUID.randomUUID().toString().also { prefs.edit().putString(KEY_INSTALL_ID, it).apply() }

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, null) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim()).apply()

    // TEMPORAL (P2): se elimina cuando el Gateway verifique tokens de Firebase.
    val appSecret: String
        get() = BuildConfig.APP_SECRET

    companion object {
        private const val KEY_INSTALL_ID = "install_id"
        private const val KEY_BASE_URL = "base_url"
        const val DEFAULT_BASE_URL = "http://127.0.0.1:8000"
    }
}