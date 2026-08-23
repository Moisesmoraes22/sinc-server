package com.apksinc.monitor.ui.health

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.MetricSnapshot
import com.apksinc.monitor.domain.MetricType
import com.apksinc.monitor.ui.components.ErrorState
import com.apksinc.monitor.ui.components.LoadingState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.components.icon
import com.apksinc.monitor.ui.components.label
import com.apksinc.monitor.ui.components.unit
import com.apksinc.monitor.ui.theme.OmgSincTheme

@Composable
fun HealthScreen(viewModel: HealthViewModel) {
    val state by viewModel.uiState.collectAsState()
    var editingType by remember { mutableStateOf<MetricType?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val current = state) {
            is HealthUiState.Loading -> LoadingState()
            is HealthUiState.Error -> ErrorState(current.message, onRetry = viewModel::refresh)
            is HealthUiState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Column {
                        Text(
                            text = "Saude",
                            style = MaterialTheme.typography.headlineMedium,
                            color = OmgSincTheme.colors.textPrimary,
                        )
                        Text(
                            text = "Registre como voce esta se sentindo hoje",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OmgSincTheme.colors.textSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
                item { SectionLabel("Ultimas leituras", modifier = Modifier.padding(top = 8.dp)) }
                items(current.metrics, key = { it.type.name }) { snapshot ->
                    MetricRow(snapshot = snapshot, onRegister = { editingType = snapshot.type })
                }
            }
        }
    }

    val type = editingType
    if (type != null) {
        RegisterMetricDialog(
            type = type,
            onDismiss = { editingType = null },
            onConfirm = { value ->
                viewModel.addReading(type, value)
                editingType = null
            },
        )
    }
}

@Composable
private fun MetricRow(snapshot: MetricSnapshot, onRegister: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = snapshot.type.icon(),
                contentDescription = null,
                tint = OmgSincTheme.colors.accent,
            )
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = snapshot.type.label(),
                    style = MaterialTheme.typography.titleSmall,
                    color = OmgSincTheme.colors.textPrimary,
                )
                Text(
                    text = snapshot.value?.let { "${formatValue(it)} ${snapshot.type.unit()}" } ?: "Sem registro ainda",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmgSincTheme.colors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            TextButton(onClick = onRegister) {
                Text("Registrar", color = OmgSincTheme.colors.accent)
            }
        }
    }
}

@Composable
private fun RegisterMetricDialog(
    type: MetricType,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit,
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar ${type.label().lowercase()}") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Valor (${type.unit()})") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.toDoubleOrNull() != null,
                onClick = { value.toDoubleOrNull()?.let(onConfirm) },
            ) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

private fun formatValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
