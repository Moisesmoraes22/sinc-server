package com.apksinc.monitor.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.domain.ServerEvent
import com.apksinc.monitor.domain.ServerInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerDetailsUiState(
    val server: ServerInfo? = null,
    val events: List<ServerEvent> = emptyList(),
    val isLoading: Boolean = false,
)

class ServerDetailsViewModel(
    private val repository: ServerRepository,
    private val serverId: String,
) : ViewModel() {

    private val serverFlow = MutableStateFlow<ServerInfo?>(null)
    private val loading = MutableStateFlow(false)

    val uiState: StateFlow<ServerDetailsUiState> = combine(
        serverFlow, repository.observeEventsForServer(serverId), loading,
    ) { server, events, isLoading ->
        ServerDetailsUiState(server = server, events = events, isLoading = isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerDetailsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching {
                repository.refreshServers()
                repository.refreshEvents()
                serverFlow.value = repository.getServer(serverId)
            }
            loading.value = false
        }
    }
}
