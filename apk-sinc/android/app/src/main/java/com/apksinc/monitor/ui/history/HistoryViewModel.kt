package com.apksinc.monitor.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.domain.ServerEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HistoryFilter { ALL, ONLINE, ATENCAO, OFFLINE }

data class HistoryUiState(
    val events: List<ServerEvent> = emptyList(),
    val filter: HistoryFilter = HistoryFilter.ALL,
    val isLoading: Boolean = false,
) {
    val filteredEvents: List<ServerEvent>
        get() = when (filter) {
            HistoryFilter.ALL -> events
            HistoryFilter.ONLINE -> events.filter { it.toStatus == "ONLINE" }
            HistoryFilter.ATENCAO -> events.filter { it.toStatus == "ATENCAO" }
            HistoryFilter.OFFLINE -> events.filter { it.toStatus == "OFFLINE" }
        }
}

class HistoryViewModel(private val repository: ServerRepository) : ViewModel() {

    private val filter = MutableStateFlow(HistoryFilter.ALL)
    private val loading = MutableStateFlow(false)

    val uiState: StateFlow<HistoryUiState> = combine(
        repository.observeEvents(), filter, loading,
    ) { events, currentFilter, isLoading ->
        HistoryUiState(events = events, filter = currentFilter, isLoading = isLoading)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())

    init {
        refresh()
    }

    fun setFilter(newFilter: HistoryFilter) {
        filter.value = newFilter
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            runCatching { repository.refreshEvents() }
            loading.value = false
        }
    }
}
