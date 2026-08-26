package com.apksinc.monitor.ui.details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.data.remote.UptimeDto
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.components.StatusBadge
import com.apksinc.monitor.ui.components.statusColorsFor
import com.apksinc.monitor.ui.theme.ApkSincColors
import com.apksinc.monitor.util.formatElapsedMinutesShort
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = colors.textSecondary)
                }
                IconButton(onClick = viewModel::refresh, enabled = !state.isLoading) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Atualizar", tint = colors.textSecondary)
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
                state.error?.let { message ->
                    item { StaleDataBanner(message = message, onRetry = viewModel::refresh) }
                }

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
                    SyncDirectionCard(
                        title = "Envio",
                        status = server.sendStatus,
                        elapsedMinutes = server.sendElapsedMinutes,
                        lastAt = server.lastSendAt,
                        lastLabel = "Último envio às",
                        warningThresholdMinutes = server.warningThresholdMinutes,
                        criticalThresholdMinutes = server.criticalThresholdMinutes,
                        revisionsToSend = server.revisionsToSend,
                    )
                }

                item {
                    SyncDirectionCard(
                        title = "Recebimento",
                        status = server.receiveStatus,
                        elapsedMinutes = server.receiveElapsedMinutes,
                        lastAt = server.lastReceiveAt,
                        lastLabel = "Último recebimento às",
                        warningThresholdMinutes = server.warningThresholdMinutes,
                        criticalThresholdMinutes = server.criticalThresholdMinutes,
                    )
                }

                state.uptime?.let { uptime ->
                    item { UptimeCard(uptime) }
                }

                item { SectionLabel("Histórico recente") }

                if (state.events.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.History,
                            title = "Nenhum evento registrado ainda",
                            message = "Mudanças de status desta unidade vão aparecer aqui.",
                        )
                    }
                } else {
                    items(state.events, key = { it.id }) { event -> TimelineRow(event) }
                }
            }
        }
    }
}

/**
 * Avisa quando a ultima tentativa de atualizar falhou, para que os dados
 * exibidos (que continuam sendo os do cache local) nao pareçam "ao vivo"
 * quando na verdade podem estar horas desatualizados.
 */
@Composable
private fun StaleDataBanner(message: String, onRetry: () -> Unit) {
    val colors = ApkSincColors.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.warningSoft, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onRetry,
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = colors.warning, modifier = Modifier.padding(end = 10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("Dados podem estar desatualizados", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(message, style = MaterialTheme.typography.bodySmall, color = colors.textMuted, modifier = Modifier.padding(top = 2.dp))
        }
        Text("Tentar de novo", style = MaterialTheme.typography.labelMedium, color = colors.accent)
    }
}

/**
 * Cartao de uma direcao de sincronizacao (envio ou recebimento): numero
 * grande com o tempo decorrido (cor = severidade), pill de status e o
 * horario do ultimo evento, com o limite configurado quando o estado nao
 * e normal - da pra pessoa entender o "porque" sem abrir outra tela.
 */
@Composable
private fun SyncDirectionCard(
    title: String,
    status: ServerStatus?,
    elapsedMinutes: Double?,
    lastAt: String?,
    lastLabel: String,
    warningThresholdMinutes: Int?,
    criticalThresholdMinutes: Int?,
    revisionsToSend: Int? = null,
) {
    val colors = ApkSincColors.colors
    val effectiveStatus = status ?: ServerStatus.ONLINE
    val valueColor = statusColorsFor(effectiveStatus).accent

    val thresholdSuffix = when (effectiveStatus) {
        ServerStatus.OFFLINE -> criticalThresholdMinutes?.let { " — limite crítico é $it min" } ?: ""
        ServerStatus.ATENCAO -> warningThresholdMinutes?.let { " — limite de atenção é $it min" } ?: ""
        ServerStatus.ONLINE -> ""
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colors.hairline), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted,
            )
            if (status != null) StatusBadge(status = status)
        }
        Text(
            formatElapsedMinutesShort(elapsedMinutes),
            style = MaterialTheme.typography.headlineLarge,
            color = valueColor,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "$lastLabel ${formatTimeOnly(lastAt)}$thresholdSuffix",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (revisionsToSend != null) {
            Text(
                "$revisionsToSend revisão(ões) a enviar",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

/**
 * Percentual de disponibilidade calculado no backend a partir do historico
 * de eventos (nao ha coleta nova envolvida) - da uma nocao rapida de "essa
 * unidade e estavel ou instavel" sem precisar rolar a linha do tempo toda.
 */
@Composable
private fun UptimeCard(uptime: UptimeDto) {
    val colors = ApkSincColors.colors
    val valueColor = when {
        uptime.uptimePercent >= 99.5 -> colors.success
        uptime.uptimePercent >= 95.0 -> colors.warning
        else -> colors.danger
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.card, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, colors.hairline), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(
            "DISPONIBILIDADE (${uptime.periodDays} DIAS)".uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
        )
        Text(
            "${uptime.uptimePercent}%",
            style = MaterialTheme.typography.headlineLarge,
            color = valueColor,
            modifier = Modifier.padding(top = 6.dp),
        )
        Text(
            "Fora do normal por ${formatDurationSecondsShort(uptime.downtimeSeconds)} nesse período",
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted,
            modifier = Modifier.padding(top = 4.dp),
        )
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
                formatTimeOnly(event.occurredAt) + (event.durationSeconds?.let { " · durou ${formatDurationSecondsShort(it)}" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

private fun formatDurationSecondsShort(seconds: Int): String {
    val minutes = seconds / 60
    if (minutes < 60) return "$minutes min"
    val hours = minutes / 60
    val remainder = minutes % 60
    return if (remainder == 0) "${hours}h" else "${hours}h ${remainder}min"
}

private fun statusLabel(raw: String): String = when (raw) {
    "OFFLINE" -> "Crítico"
    "ATENCAO" -> "Atenção"
    else -> "Normal"
}
