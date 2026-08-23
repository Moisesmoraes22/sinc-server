package com.apksinc.monitor.ui.habits

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.HabitCategory
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.ErrorState
import com.apksinc.monitor.ui.components.HabitRow
import com.apksinc.monitor.ui.components.LoadingState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.components.icon
import com.apksinc.monitor.ui.theme.OmgSincTheme

@Composable
fun HabitsScreen(viewModel: HabitsViewModel) {
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = OmgSincTheme.colors.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = OmgSincTheme.colors.accent,
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Adicionar habito")
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when {
                state.isLoading && state.habits.isEmpty() -> LoadingState()
                state.error != null && state.habits.isEmpty() -> ErrorState(state.error!!, onRetry = viewModel::refresh)
                state.habits.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Checklist,
                    title = "Nenhum habito ainda",
                    message = "Toque no + para adicionar seu primeiro habito.",
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    val grouped = state.habits.groupBy { it.category }
                    HabitCategory.entries.forEach { category ->
                        val habits = grouped[category].orEmpty()
                        if (habits.isNotEmpty()) {
                            item(key = "header-${category.name}") {
                                SectionLabel(categoryLabel(category), modifier = Modifier.padding(top = 4.dp))
                            }
                            items(habits, key = { it.id }) { habit ->
                                HabitRow(habit = habit, onToggle = { viewModel.toggleHabit(habit) })
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddHabitDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, category, target, unit ->
                viewModel.addHabit(title, category, target, unit)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun AddHabitDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, HabitCategory, Double?, String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(HabitCategory.OUTRO) }
    var targetValue by remember { mutableStateOf("") }
    var targetUnit by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo habito") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "Categoria",
                    style = MaterialTheme.typography.labelMedium,
                    color = OmgSincTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                CategoryChips(selected = category, onSelect = { category = it })

                OutlinedTextField(
                    value = targetValue,
                    onValueChange = { targetValue = it },
                    label = { Text("Meta (opcional)") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                )
                OutlinedTextField(
                    value = targetUnit,
                    onValueChange = { targetUnit = it },
                    label = { Text("Unidade (ex.: L, min, x)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank(),
                onClick = {
                    onConfirm(title.trim(), category, targetValue.toDoubleOrNull(), targetUnit.ifBlank { null })
                },
            ) { Text("Adicionar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(selected: HabitCategory, onSelect: (HabitCategory) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        HabitCategory.entries.forEach { category ->
            FilterChip(
                selected = category == selected,
                onClick = { onSelect(category) },
                label = { Text(categoryLabel(category)) },
                leadingIcon = { Icon(category.icon(), contentDescription = null) },
            )
        }
    }
}

private fun categoryLabel(category: HabitCategory): String = when (category) {
    HabitCategory.AGUA -> "Agua"
    HabitCategory.SONO -> "Sono"
    HabitCategory.EXERCICIO -> "Exercicio"
    HabitCategory.SKINCARE -> "Skincare"
    HabitCategory.HUMOR -> "Humor"
    HabitCategory.OUTRO -> "Outro"
}
