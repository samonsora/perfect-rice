package com.example.team1application

import android.content.Context
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

class FontSizeRepository(private val context: Context) {

    companion object {
        val FONT_SIZE_KEY = floatPreferencesKey("font_size")
    }

    val fontSizeFlow = context.dataStore.data.map { preferences ->
        preferences[FONT_SIZE_KEY] ?: 20f
    }

    suspend fun saveFontSize(size: Float) {
        context.dataStore.edit { settings ->
            settings[FONT_SIZE_KEY] = size
        }
    }
}
