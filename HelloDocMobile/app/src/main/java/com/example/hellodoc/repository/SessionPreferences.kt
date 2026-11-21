package com.example.hellodoc.repository

import android.content.Context

class SessionPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("hello_doc_session", Context.MODE_PRIVATE)
    var hasLoggedIn: Boolean
        get() = prefs.getBoolean("was_authenticated", false)
        set(value) { prefs.edit().putBoolean("was_authenticated", value).apply() }
    fun clear() { prefs.edit().clear().apply() }
}
