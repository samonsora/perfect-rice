package com.example.team1application

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val FONT_SIZE_KEY = floatPreferencesKey("font_size")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    // ▼ 文字サイズの読み込み
    val fontSizeFlow: Flow<Float> = context.dataStore.data.map { pref ->
        pref[FONT_SIZE_KEY] ?: 20f
    }

    // ▼ ダークモードの読み込み
    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { pref ->
        pref[DARK_MODE_KEY] ?: false
    }

    // ▼ 文字サイズの保存
    suspend fun saveFontSize(size: Float) {
        context.dataStore.edit { pref ->
            pref[FONT_SIZE_KEY] = size
        }
    }

    // ▼ ダークモードの保存
    suspend fun saveDarkMode(enabled: Boolean) {
        context.dataStore.edit { pref ->
            pref[DARK_MODE_KEY] = enabled
        }
    }

}
