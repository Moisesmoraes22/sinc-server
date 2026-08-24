package com.apksinc.monitor.ui.apistatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.remote.HealthDto
import com.apksinc.monitor.data.repository.ServerRepository
import com.apksinc.monitor.util.friendlyErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ApiStatusUiState(
    val health: HealthDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

class ApiStatusViewModel(private val repository: ServerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ApiStatusUiState())
    val uiState: StateFlow<ApiStatusUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            runCatching { repository.getHealth() }
                .onSuccess { health -> _uiState.value = ApiStatusUiState(health = health, isLoading = false) }
                .onFailure { t -> _uiState.value = _uiState.value.copy(isLoading = false, error = friendlyErrorMessage(t)) }
        }
    }
}
