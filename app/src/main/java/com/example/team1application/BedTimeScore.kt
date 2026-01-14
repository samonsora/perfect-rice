package com.example.team1application

import android.content.Context

class BedtimeStore(context: Context) {
    private val prefs = context.getSharedPreferences("bedtime_store", Context.MODE_PRIVATE)

    fun saveBedtime(time: String) {
        prefs.edit().putString("last_bedtime", time).apply()
    }

    fun getLastBedtime(): String? {
        return prefs.getString("last_bedtime", null)
    }

    fun clear() {
        prefs.edit().remove("last_bedtime").apply()
    }
}
