package com.apksinc.monitor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.local.AppSettings
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.repository.HabitRepository
import com.apksinc.monitor.domain.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profile: Profile? = null,
    val settings: AppSettings = AppSettings(),
    val isLoading: Boolean = true,
)

class ProfileViewModel(
    private val repository: HabitRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val profileFlow = MutableStateFlow<Profile?>(null)
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<ProfileUiState> = combine(
        profileFlow, settingsDataStore.settingsFlow, loading,
    ) { profile, settings, isLoading ->
        ProfileUiState(profile = profile, settings = settings, isLoading = isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProfileUiState())

    init {
        viewModelScope.launch {
            runCatching { repository.getProfile() }.onSuccess { profileFlow.value = it }
            loading.value = false
        }
    }

    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            runCatching { repository.updateProfile(name) }.onSuccess { profileFlow.value = it }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setNotificationsEnabled(enabled) }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setSoundEnabled(enabled) }
    }

    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setVibrationEnabled(enabled) }
    }
}
