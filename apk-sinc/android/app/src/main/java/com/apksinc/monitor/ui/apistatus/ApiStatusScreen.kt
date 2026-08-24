package com.apksinc.monitor.ui.apistatus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.data.remote.HealthDto
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.ErrorState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.theme.ApkSincColors
import com.apksinc.monitor.util.formatTimeOnly

@Composable
fun ApiStatusScreen(onBack: () -> Unit) {
    val viewModel: ApiStatusViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val state by viewModel.uiState.collectAsState()
    val colors = ApkSincColors.colors
    val isConnected = state.health?.status == "ok"

    Scaffold(containerColor = colors.background) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textSecondary)
                }
                Text("Status da API", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = colors.textSecondary)
                }
            }

            LazyColumn(contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { ServerRackHero(isConnected = isConnected) }

                if (state.isLoading && state.health == null) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator(color = colors.accent)
                        }
                    }
                } else if (state.error != null) {
                    item { ErrorState(message = state.error!!, onRetry = viewModel::refresh) }
                } else {
                    val health = state.health
                    if (health != null) {
                        item { StatusHeadline(isConnected = isConnected) }
                        item { SectionLabel("Informações da conexão") }
                        item { ConnectionInfoCard(health) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusHeadline(isConnected: Boolean) {
    val colors = ApkSincColors.colors
    val bg = if (isConnected) colors.successSoft else colors.dangerSoft
    val fg = if (isConnected) colors.success else colors.danger
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isConnected) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = fg,
            modifier = Modifier.padding(end = 12.dp),
        )
        Column {
            Text(
                if (isConnected) "API conectada" else "Não foi possível conectar",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
            )
            Text(
                if (isConnected) "Tudo funcionando normalmente" else "Verifique a conexão com o servidor",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun ConnectionInfoCard(health: HealthDto) {
    val colors = ApkSincColors.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        InfoRow("Status", statusLabel(health.status), isFirst = true)
        InfoRow("Database", databaseLabel(health.database))
        InfoRow("Monitor", monitorLabel(health.monitor))
        InfoRow("Firebase", firebaseLabel(health.firebase))
        InfoRow("Fonte dos dados", sourceModeLabel(health.sourceMode))
        InfoRow("Banco de dados", health.dbBackend.replaceFirstChar { it.uppercase() })
        InfoRow("Última verificação", health.lastPollAt?.let { formatTimeOnly(it) } ?: "--")
        InfoRow("Unidades monitoradas", health.unitsCount.toString(), isLast = true)
    }
}

@Composable
private fun InfoRow(label: String, value: String, isFirst: Boolean = false, isLast: Boolean = false) {
    val colors = ApkSincColors.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = if (isFirst) 0.dp else 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
    if (!isLast) {
        androidx.compose.material3.HorizontalDivider(color = colors.hairline, modifier = Modifier.padding(top = 10.dp))
    }
}

private fun statusLabel(raw: String) = if (raw == "ok") "Operacional" else "Degradado"

private fun databaseLabel(raw: String) = when (raw) {
    "connected" -> "Conectado"
    "error" -> "Erro"
    else -> raw
}

private fun monitorLabel(raw: String) = when (raw) {
    "running" -> "Rodando"
    "starting" -> "Iniciando"
    "stopped" -> "Parado"
    else -> raw
}

private fun firebaseLabel(raw: String) = when (raw) {
    "configured" -> "Configurado"
    "not-configured" -> "Não configurado"
    "dry-run" -> "Modo de teste"
    else -> raw
}

private fun sourceModeLabel(raw: String) = when (raw) {
    "real" -> "Real"
    "mock" -> "Simulado"
    else -> raw
}
