// PinStore.kt
package com.example.gymcheckin.data

import android.content.Context
import android.content.SharedPreferences

object PinStore {
    private const val PREFS = "gym_prefs"
    private const val KEY_PIN = "admin_pin"
    private const val DEFAULT_PIN = "1234"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getPin(ctx: Context): String =
        prefs(ctx).getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN

    fun setPin(ctx: Context, newPin: String) {
        require(newPin.length in 4..8 && newPin.all { it.isDigit() }) {
            "El PIN debe tener 4 a 8 dígitos."
        }
        prefs(ctx).edit().putString(KEY_PIN, newPin).apply()
    }
}
