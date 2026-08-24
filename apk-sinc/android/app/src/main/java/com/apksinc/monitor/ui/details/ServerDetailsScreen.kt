package com.apksinc.monitor.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.StatusBadge
import com.apksinc.monitor.ui.theme.ApkSincColors
import com.apksinc.monitor.util.formatDuration
import com.apksinc.monitor.util.formatTimeOnly

@Composable
fun ServerDetailsScreen(serverId: String, onBack: () -> Unit) {
    val viewModel: ServerDetailsViewModel = viewModel(factory = ViewModelFactoryProvider.detailsFactory(serverId))
    val state by viewModel.uiState.collectAsState()
    val server = state.server
    val colors = ApkSincColors.colors

    Scaffold(containerColor = colors.background) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textSecondary)
                }
            }

            if (server == null) {
                Text(
                    text = "Carregando...",
                    color = colors.textSecondary,
                    modifier = Modifier.padding(24.dp),
                )
                return@Column
            }

            LazyColumn(contentPadding = PaddingValues(20.dp, 0.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Column {
                        Text(server.name, style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                        Text(
                            server.id,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            modifier = Modifier.padding(top = 2.dp, bottom = 10.dp),
                        )
                        StatusBadge(status = server.status)
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.card, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        KvRow("Sincronizado há", formatDuration(server.lastCheck))
                        KvRow("Última checagem", formatTimeOnly(server.lastCheck))
                        KvRow("Última queda", formatTimeOnly(server.lastDownAt))
                        KvRow("Última normalização", formatTimeOnly(server.lastUpAt))
                        KvRow("Quedas registradas", server.downCount.toString(), isLast = true)
                    }
                }

                item { com.apksinc.monitor.ui.components.SectionLabel("Histórico recente") }

                if (state.events.isEmpty()) {
                    item {
                        Text(
                            "Nenhum evento registrado ainda.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                        )
                    }
                } else {
                    items(state.events, key = { it.id }) { event -> TimelineRow(event) }
                }
            }
        }
    }
}

@Composable
private fun KvRow(label: String, value: String, isLast: Boolean = false) {
    val colors = ApkSincColors.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
    if (!isLast) {
        androidx.compose.material3.HorizontalDivider(color = colors.hairline)
    }
}

@Composable
private fun TimelineRow(event: com.apksinc.monitor.domain.ServerEvent) {
    val colors = ApkSincColors.colors
    val dotColor = when (event.toStatus) {
        "OFFLINE" -> colors.danger
        "ATENCAO" -> colors.warning
        else -> colors.success
    }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .padding(top = 6.dp, end = 10.dp)
                .background(dotColor, CircleShape)
                .padding(3.5.dp),
        )
        Column {
            Text(
                "${statusLabel(event.fromStatus)} → ${statusLabel(event.toStatus)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
            Text(
                formatTimeOnly(event.occurredAt),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun statusLabel(raw: String): String = when (raw) {
    "OFFLINE" -> "Crítico"
    "ATENCAO" -> "Atenção"
    else -> "Normal"
}
