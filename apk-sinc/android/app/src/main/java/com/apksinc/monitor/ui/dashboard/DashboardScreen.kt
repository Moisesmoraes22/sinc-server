package com.apksinc.monitor.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.ServerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onServerClick: (String) -> Unit) {
    val viewModel: DashboardViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringRes(R.string.app_name), fontWeight = FontWeight.Bold)
                        Text(
                            stringRes(R.string.app_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            OverallStatusBanner(hasOffline = state.hasOffline, hasAttention = state.attentionCount > 0)
            SummaryRow(state)

            if (state.isLoading && state.servers.isEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }
            }

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.servers, key = { it.id }) { server ->
                    ServerCard(server = server, onClick = { onServerClick(server.id) })
                }
            }
        }
    }
}

@Composable
private fun OverallStatusBanner(hasOffline: Boolean, hasAttention: Boolean) {
    val (bg, text) = when {
        hasOffline -> MaterialTheme.colorScheme.errorContainer to stringRes(R.string.status_some_offline)
        hasAttention -> MaterialTheme.colorScheme.tertiaryContainer to stringRes(R.string.status_some_attention)
        else -> MaterialTheme.colorScheme.primaryContainer to stringRes(R.string.status_all_operational)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SummaryRow(state: DashboardUiState) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SummaryStat(stringRes(R.string.status_online), state.onlineCount)
        SummaryStat(stringRes(R.string.status_attention), state.attentionCount)
        SummaryStat(stringRes(R.string.status_offline), state.offlineCount)
    }
}

@Composable
private fun SummaryStat(label: String, count: Int) {
    Column {
        Text(count.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
