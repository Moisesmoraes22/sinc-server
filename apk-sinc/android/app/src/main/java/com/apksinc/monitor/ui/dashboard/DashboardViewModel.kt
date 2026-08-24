package com.apksinc.monitor.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.domain.ServerInfo
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.util.friendlyErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val servers: List<ServerInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val onlineCount get() = servers.count { it.status == ServerStatus.ONLINE }
    val attentionCount get() = servers.count { it.status == ServerStatus.ATENCAO }
    val offlineCount get() = servers.count { it.status == ServerStatus.OFFLINE }
    val allOperational get() = servers.isNotEmpty() && offlineCount == 0 && attentionCount == 0
    val hasOffline get() = offlineCount > 0
}

class DashboardViewModel(private val repository: ServerRepository) : ViewModel() {

    private val loading = MutableStateFlow(false)
    private val error = MutableStateFlow<String?>(null)

    companion object {
        // Criticas primeiro, depois atencao, normais por ultimo - dentro de
        // cada grupo, ordem alfabetica pelo nome.
        private val SEVERITY_ORDER = compareBy<ServerInfo>(
            { server ->
                when (server.status) {
                    ServerStatus.OFFLINE -> 0
                    ServerStatus.ATENCAO -> 1
                    ServerStatus.ONLINE -> 2
                }
            },
            { it.name },
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.observeServers(), loading, error,
    ) { servers, isLoading, err ->
        DashboardUiState(servers = servers.sortedWith(SEVERITY_ORDER), isLoading = isLoading, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.refreshServers() }
                .onFailure { error.value = friendlyErrorMessage(it) }
            loading.value = false
        }
    }
}
