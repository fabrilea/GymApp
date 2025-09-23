// ExcelLinkStore.kt
package com.example.gymcheckin.data

import android.content.Context
import android.net.Uri

object ExcelLinkStore {
    private const val PREFS = "gym_prefs"
    private const val KEY_EXCEL_URI = "excel_uri"

    fun setUri(ctx: Context, uri: Uri) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_EXCEL_URI, uri.toString()).apply()
    }

    fun getUri(ctx: Context): Uri? {
        val s = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_EXCEL_URI, null) ?: return null
        return Uri.parse(s)
    }

    fun clear(ctx: Context) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(KEY_EXCEL_URI).apply()
    }
}
