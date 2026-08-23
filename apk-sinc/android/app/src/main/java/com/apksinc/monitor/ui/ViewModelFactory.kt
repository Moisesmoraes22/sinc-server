package com.apksinc.monitor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.ui.dashboard.DashboardViewModel
import com.apksinc.monitor.ui.details.ServerDetailsViewModel
import com.apksinc.monitor.ui.history.HistoryViewModel
import com.apksinc.monitor.ui.settings.SettingsViewModel

class SincViewModelFactory(
    private val repository: ServerRepository,
    private val settingsDataStore: SettingsDataStore,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        DashboardViewModel::class.java -> DashboardViewModel(repository) as T
        HistoryViewModel::class.java -> HistoryViewModel(repository) as T
        SettingsViewModel::class.java -> SettingsViewModel(settingsDataStore) as T
        else -> throw IllegalArgumentException("ViewModel desconhecido: $modelClass")
    }
}

class ServerDetailsViewModelFactory(
    private val repository: ServerRepository,
    private val serverId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass == ServerDetailsViewModel::class.java)
        return ServerDetailsViewModel(repository, serverId) as T
    }
}
