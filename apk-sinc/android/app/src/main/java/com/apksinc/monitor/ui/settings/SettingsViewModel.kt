package com.apksinc.monitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.local.AppSettings
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.local.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val dataStore: SettingsDataStore) : ViewModel() {

    val settings: StateFlow<AppSettings> = dataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    fun setNotificationsEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.setNotificationsEnabled(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.setSoundEnabled(enabled)
    }

    fun setVibrationEnabled(enabled: Boolean) = viewModelScope.launch {
        dataStore.setVibrationEnabled(enabled)
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        dataStore.setThemeMode(mode)
    }

    fun setRefreshIntervalSeconds(seconds: Int) = viewModelScope.launch {
        dataStore.setRefreshIntervalSeconds(seconds)
    }
}
