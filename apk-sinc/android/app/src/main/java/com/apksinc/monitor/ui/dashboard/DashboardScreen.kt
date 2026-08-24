package com.apksinc.monitor.ui.dashboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.components.ServerCard
import com.apksinc.monitor.ui.theme.ApkSincColors
import androidx.compose.material.icons.filled.Checklist

@Composable
fun DashboardScreen(onServerClick: (String) -> Unit) {
    val viewModel: DashboardViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val state by viewModel.uiState.collectAsState()
    val colors = ApkSincColors.colors

    Scaffold(containerColor = colors.background) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { DashboardHeader(isLoading = state.isLoading, onRefresh = viewModel::refresh) }

                if (!state.allOperational) {
                    item { AlertBanner(offlineCount = state.offlineCount, attentionCount = state.attentionCount) }
                }

                item { StatRow(state) }

                item { SectionLabel("Unidades — críticas primeiro") }

                if (state.isLoading && state.servers.isEmpty()) {
                    items(4) {
                        com.apksinc.monitor.ui.components.SkeletonServerCard()
                    }
                } else if (state.error != null) {
                    item {
                        com.apksinc.monitor.ui.components.ErrorState(
                            message = state.error!!,
                            onRetry = viewModel::refresh,
                        )
                    }
                } else if (state.servers.isEmpty()) {
                    item {
                        EmptyState(
                            icon = Icons.Filled.Checklist,
                            title = "Nenhuma unidade monitorada",
                            message = "Assim que houver unidades sincronizando, elas aparecem aqui.",
                        )
                    }
                } else {
                    items(state.servers, key = { it.id }) { server ->
                        ServerCard(server = server, onClick = { onServerClick(server.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(isLoading: Boolean, onRefresh: () -> Unit) {
    val colors = ApkSincColors.colors
    val rotation = remember { androidx.compose.animation.core.Animatable(0f) }
    androidx.compose.runtime.LaunchedEffect(isLoading) {
        if (isLoading) {
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                    animation = androidx.compose.animation.core.tween(700, easing = androidx.compose.animation.core.LinearEasing),
                ),
            )
        } else {
            rotation.snapTo(0f)
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column {
            Text(
                text = stringRes(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Text(
                text = stringRes(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(
            onClick = onRefresh,
            enabled = !isLoading,
            modifier = Modifier.background(colors.elevated, RoundedCornerShape(10.dp)),
        ) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = "Atualizar",
                tint = colors.textSecondary,
                modifier = Modifier.graphicsLayer { rotationZ = rotation.value },
            )
        }
    }
}

@Composable
private fun AlertBanner(offlineCount: Int, attentionCount: Int) {
    val colors = ApkSincColors.colors
    val message = when {
        offlineCount > 0 -> "$offlineCount unidade${if (offlineCount > 1) "s" else ""} crítica${if (offlineCount > 1) "s" else ""} agora"
        else -> "$attentionCount unidade${if (attentionCount > 1) "s" else ""} precisa${if (attentionCount == 1) "" else "m"} de atenção"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dangerSoft.takeIf { offlineCount > 0 } ?: colors.warningSoft, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = if (offlineCount > 0) colors.danger else colors.warning,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
}

@Composable
private fun StatRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatCard(state.onlineCount, "Normal", ApkSincColors.colors.success, Modifier.weight(1f))
        StatCard(state.attentionCount, "Atenção", ApkSincColors.colors.warning, Modifier.weight(1f))
        StatCard(state.offlineCount, "Crítico", ApkSincColors.colors.danger, Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(count: Int, label: String, dotColor: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    val colors = ApkSincColors.colors
    Column(
        modifier = modifier
            .background(colors.elevated, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(modifier = Modifier.background(dotColor, RoundedCornerShape(50)).padding(3.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = colors.textMuted, modifier = Modifier.padding(start = 6.dp))
        }
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
