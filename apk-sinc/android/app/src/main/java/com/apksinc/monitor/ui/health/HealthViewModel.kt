package com.apksinc.monitor.ui.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.HabitRepository
import com.apksinc.monitor.domain.MetricSnapshot
import com.apksinc.monitor.domain.MetricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HealthUiState {
    data object Loading : HealthUiState
    data class Success(val metrics: List<MetricSnapshot>) : HealthUiState
    data class Error(val message: String) : HealthUiState
}

class HealthViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HealthUiState>(HealthUiState.Loading)
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.getHomeSummary() }
                .onSuccess { _uiState.value = HealthUiState.Success(it.metrics) }
                .onFailure { _uiState.value = HealthUiState.Error(it.message ?: "Falha ao carregar suas metricas") }
        }
    }

    fun addReading(type: MetricType, value: Double) {
        viewModelScope.launch {
            runCatching { repository.addMetric(type, value) }
            refresh()
        }
    }
}
