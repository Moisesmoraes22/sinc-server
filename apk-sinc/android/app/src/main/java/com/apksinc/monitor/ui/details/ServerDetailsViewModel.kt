package com.apksinc.monitor.ui.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.remote.UptimeDto
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.domain.ServerEvent
import com.apksinc.monitor.domain.ServerInfo
import com.apksinc.monitor.util.friendlyErrorMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Tela de detalhe de uma unidade especifica - intervalo curto e fixo (nao
// usa o intervalo configuravel em Ajustes, que e pensado pro Dashboard
// inteiro) porque aqui a pessoa normalmente esta olhando de perto uma
// unidade por um motivo (ex.: conferindo se "revisoes a enviar" esta
// baixando), entao vale a pena atualizar com mais frequencia.
private const val AUTO_REFRESH_INTERVAL_MS = 10_000L

data class ServerDetailsUiState(
    val server: ServerInfo? = null,
    val events: List<ServerEvent> = emptyList(),
    val isLoading: Boolean = false,
    val uptime: UptimeDto? = null,
    val error: String? = null,
)

class ServerDetailsViewModel(
    private val repository: ServerRepository,
    private val serverId: String,
) : ViewModel() {

    private val serverFlow = MutableStateFlow<ServerInfo?>(null)
    private val loading = MutableStateFlow(false)
    private val uptimeFlow = MutableStateFlow<UptimeDto?>(null)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ServerDetailsUiState> = combine(
        serverFlow, repository.observeEventsForServer(serverId), loading, uptimeFlow, error,
    ) { server, events, isLoading, uptime, err ->
        ServerDetailsUiState(server = server, events = events, isLoading = isLoading, uptime = uptime, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServerDetailsUiState())

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                refresh()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching {
                repository.refreshServers()
                repository.refreshEvents()
                serverFlow.value = repository.getServer(serverId)
            }.onFailure {
                // Nao apaga o que ja estava carregado (cache local continua
                // visivel) mas avisa que pode estar desatualizado - antes
                // essa falha era engolida em silencio e a tela parecia "ao
                // vivo" mesmo mostrando dados de horas atras.
                error.value = friendlyErrorMessage(it)
            }
            loading.value = false
        }
        viewModelScope.launch {
            runCatching { uptimeFlow.value = repository.getUptime(serverId, days = 7) }
        }
    }
}
