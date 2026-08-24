package com.apksinc.monitor.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.domain.ServerEvent
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.theme.ApkSincColors
import com.apksinc.monitor.util.formatTimeOnly
import androidx.compose.material.icons.filled.History as HistoryIcon

@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val state by viewModel.uiState.collectAsState()
    val colors = ApkSincColors.colors

    Scaffold(containerColor = colors.background) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp)) {
                Text("Histórico", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
                Text(
                    "Todas as unidades",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChipItem("Todos", state.filter == HistoryFilter.ALL) { viewModel.setFilter(HistoryFilter.ALL) }
                FilterChipItem("Normal", state.filter == HistoryFilter.ONLINE) { viewModel.setFilter(HistoryFilter.ONLINE) }
                FilterChipItem("Atenção", state.filter == HistoryFilter.ATENCAO) { viewModel.setFilter(HistoryFilter.ATENCAO) }
                FilterChipItem("Crítico", state.filter == HistoryFilter.OFFLINE) { viewModel.setFilter(HistoryFilter.OFFLINE) }
            }

            if (state.filteredEvents.isEmpty()) {
                EmptyState(
                    icon = Icons.Filled.HistoryIcon,
                    title = "Nenhum evento ainda",
                    message = "As mudanças de status das unidades aparecem aqui.",
                )
            } else {
                LazyColumn(contentPadding = PaddingValues(20.dp, 8.dp, 20.dp, 32.dp)) {
                    item { SectionLabel("Eventos") }
                    items(state.filteredEvents, key = { it.id }) { event -> EventRow(event) }
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ApkSincColors.colors
    val bg = if (selected) colors.accentSoft else colors.elevated
    val textColor = if (selected) colors.accent else colors.textSecondary
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}

private data class EventVisual(val icon: ImageVector, val color: Color, val soft: Color)

@Composable
private fun eventVisualFor(status: String): EventVisual {
    val colors = ApkSincColors.colors
    return when (status) {
        "OFFLINE" -> EventVisual(Icons.Filled.ReportProblem, colors.danger, colors.dangerSoft)
        "ATENCAO" -> EventVisual(Icons.Filled.WarningAmber, colors.warning, colors.warningSoft)
        else -> EventVisual(Icons.Filled.CheckCircle, colors.success, colors.successSoft)
    }
}

@Composable
private fun EventRow(event: ServerEvent) {
    val colors = ApkSincColors.colors
    val visual = eventVisualFor(event.toStatus)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .background(colors.card, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(visual.soft, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(16.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                "${event.serverName} — ${statusLabel(event.fromStatus)} → ${statusLabel(event.toStatus)}",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!event.reason.isNullOrBlank()) {
                Text(
                    event.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(formatTimeOnly(event.occurredAt), style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
    }
}

private fun statusLabel(raw: String): String = when (raw) {
    "OFFLINE" -> "Crítico"
    "ATENCAO" -> "Atenção"
    else -> "Normal"
}
