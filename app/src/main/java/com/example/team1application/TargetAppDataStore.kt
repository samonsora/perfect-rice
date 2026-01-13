package com.example.team1application

import android.content.Context

object TargetAppDataStore {
    private const val PREF_NAME = "target_apps_prefs"
    private const val KEY_PACKAGES = "target_packages"

    // 選択されたアプリのパッケージ名リストを保存
    fun saveTargetApps(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_PACKAGES, packages).apply()
    }

    // 保存されたパッケージ名リストを取得
    fun loadTargetApps(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PACKAGES, emptySet()) ?: emptySet()
    }
}