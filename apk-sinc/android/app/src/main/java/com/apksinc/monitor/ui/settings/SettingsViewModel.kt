package com.apksinc.monitor.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.local.AppSettings
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.local.ThemeMode
import com.apksinc.monitor.data.repository.ServerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TestNotificationResult { SENT, NO_DEVICES, ERROR }

class SettingsViewModel(
    private val dataStore: SettingsDataStore,
    private val repository: ServerRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = dataStore.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _testNotificationState = MutableStateFlow<TestNotificationResult?>(null)
    val testNotificationState: StateFlow<TestNotificationResult?> = _testNotificationState.asStateFlow()
    private val _testNotificationInFlight = MutableStateFlow(false)
    val testNotificationInFlight: StateFlow<Boolean> = _testNotificationInFlight.asStateFlow()

    fun sendTestNotification() = viewModelScope.launch {
        _testNotificationInFlight.value = true
        _testNotificationState.value = try {
            val response = repository.sendTestNotification()
            if (response.status == "ok" && response.devicesNotified > 0) {
                TestNotificationResult.SENT
            } else if (response.devicesNotified == 0) {
                TestNotificationResult.NO_DEVICES
            } else {
                TestNotificationResult.ERROR
            }
        } catch (e: Exception) {
            TestNotificationResult.ERROR
        } finally {
            _testNotificationInFlight.value = false
        }
    }

    fun clearTestNotificationState() {
        _testNotificationState.value = null
    }

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
