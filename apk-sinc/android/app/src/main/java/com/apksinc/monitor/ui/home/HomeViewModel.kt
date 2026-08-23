package com.apksinc.monitor.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.HabitRepository
import com.apksinc.monitor.domain.HomeSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val summary: HomeSummary) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(private val repository: HabitRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repository.getHomeSummary() }
                .onSuccess { _uiState.value = HomeUiState.Success(it) }
                .onFailure { _uiState.value = HomeUiState.Error(it.message ?: "Falha ao carregar seu resumo") }
        }
    }

    fun toggleHabit(habitId: String, currentlyCompleted: Boolean) {
        viewModelScope.launch {
            runCatching { repository.logHabit(habitId, value = null, completed = !currentlyCompleted) }
            refresh()
        }
    }
}
