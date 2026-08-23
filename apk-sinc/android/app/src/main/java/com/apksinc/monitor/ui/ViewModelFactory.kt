package com.apksinc.monitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.repository.HabitRepository
import com.apksinc.monitor.ui.habits.HabitsViewModel
import com.apksinc.monitor.ui.health.HealthViewModel
import com.apksinc.monitor.ui.home.HomeViewModel
import com.apksinc.monitor.ui.profile.ProfileViewModel

class SincViewModelFactory(
    private val repository: HabitRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(repository) as T
        HabitsViewModel::class.java -> HabitsViewModel(repository) as T
        HealthViewModel::class.java -> HealthViewModel(repository) as T
        ProfileViewModel::class.java -> ProfileViewModel(repository, settingsDataStore) as T
        else -> throw IllegalArgumentException("ViewModel desconhecido: $modelClass")
    }
}
