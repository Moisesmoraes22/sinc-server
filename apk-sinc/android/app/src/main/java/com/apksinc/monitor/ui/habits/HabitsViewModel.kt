package com.apksinc.monitor.ui.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apksinc.monitor.data.repository.HabitRepository
import com.apksinc.monitor.domain.ColorTag
import com.apksinc.monitor.domain.Habit
import com.apksinc.monitor.domain.HabitCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HabitsUiState(
    val habits: List<Habit> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

class HabitsViewModel(private val repository: HabitRepository) : ViewModel() {

    private val loading = MutableStateFlow(true)
    private val error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HabitsUiState> = combine(
        repository.observeHabits(), loading, error,
    ) { habits, isLoading, err ->
        HabitsUiState(habits = habits, isLoading = isLoading, error = err)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            loading.value = true
            error.value = null
            runCatching { repository.refreshHabits() }
                .onFailure { error.value = it.message ?: "Falha ao atualizar seus habitos" }
            loading.value = false
        }
    }

    fun toggleHabit(habit: Habit) {
        viewModelScope.launch {
            runCatching { repository.logHabit(habit.id, value = null, completed = !habit.todayCompleted) }
        }
    }

    fun addHabit(title: String, category: HabitCategory, targetValue: Double?, targetUnit: String?) {
        viewModelScope.launch {
            runCatching { repository.createHabit(title, category, targetValue, targetUnit, ColorTag.ACCENT) }
                .onFailure { error.value = it.message ?: "Falha ao criar habito" }
        }
    }
}
