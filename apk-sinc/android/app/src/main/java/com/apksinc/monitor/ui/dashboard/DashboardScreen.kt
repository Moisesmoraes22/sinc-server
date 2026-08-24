package com.apksinc.monitor.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.EmptyState
import com.apksinc.monitor.ui.components.ErrorState
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.components.ServerCard
import com.apksinc.monitor.ui.components.SkeletonServerCard
import com.apksinc.monitor.ui.theme.ApkSincColors

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
                item {
                    DashboardHeader(
                        isLoading = state.isLoading,
                        isConnected = state.error == null,
                        onRefresh = viewModel::refresh,
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = state.servers.isNotEmpty() && !state.allOperational,
                        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                        exit = fadeOut(tween(150)) + shrinkVertically(tween(150)),
                    ) {
                        AlertBanner(offlineCount = state.offlineCount, attentionCount = state.attentionCount)
                    }
                }

                item { StatRow(state) }

                item { SectionLabel("Unidades — críticas primeiro") }

                if (state.isLoading && state.servers.isEmpty()) {
                    items(4) { SkeletonServerCard() }
                } else if (state.error != null) {
                    item { ErrorState(message = state.error!!, onRetry = viewModel::refresh) }
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
                        ServerCard(
                            server = server,
                            onClick = { onServerClick(server.id) },
                            modifier = Modifier.animateItemPlacement(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(isLoading: Boolean, isConnected: Boolean, onRefresh: () -> Unit) {
    val colors = ApkSincColors.colors
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(isLoading) {
        if (isLoading) {
            rotation.snapTo(0f)
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = infiniteRepeatable(animation = tween(700, easing = LinearEasing)),
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                )
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp, end = 5.dp)
                        .size(5.dp)
                        .background(if (isConnected) colors.success else colors.danger, CircleShape),
                )
                Text(
                    text = if (isConnected) "Conectado" else "Sem conexão",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isConnected) colors.success else colors.danger,
                )
            }
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
    val isCritical = offlineCount > 0
    val message = when {
        isCritical -> "$offlineCount unidade${if (offlineCount > 1) "s" else ""} crítica${if (offlineCount > 1) "s" else ""} agora"
        else -> "$attentionCount unidade${if (attentionCount > 1) "s" else ""} precisa${if (attentionCount == 1) "" else "m"} de atenção"
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dangerSoft.takeIf { isCritical } ?: colors.warningSoft, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
            )
            .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.WarningAmber,
            contentDescription = null,
            tint = if (isCritical) colors.danger else colors.warning,
            modifier = Modifier.padding(end = 10.dp),
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textMuted)
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
private fun StatCard(count: Int, label: String, dotColor: Color, modifier: Modifier = Modifier) {
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
