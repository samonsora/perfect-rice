package com.example.team1application

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val fontSize = repo.fontSizeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 20f)

    val darkMode = repo.darkModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun saveFontSize(size: Float) {
        viewModelScope.launch {
            repo.saveFontSize(size)
        }
    }

    fun saveDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            repo.saveDarkMode(enabled)
        }
    }
}
