package com.apksinc.monitor.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.R
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.util.formatTimeOnly

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val viewModel: HistoryViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_history)) }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter == HistoryFilter.ALL,
                    onClick = { viewModel.setFilter(HistoryFilter.ALL) },
                    label = { Text(stringResource(R.string.history_filter_all)) },
                )
                FilterChip(
                    selected = state.filter == HistoryFilter.ONLINE,
                    onClick = { viewModel.setFilter(HistoryFilter.ONLINE) },
                    label = { Text(stringResource(R.string.history_filter_online)) },
                )
                FilterChip(
                    selected = state.filter == HistoryFilter.OFFLINE,
                    onClick = { viewModel.setFilter(HistoryFilter.OFFLINE) },
                    label = { Text(stringResource(R.string.history_filter_offline)) },
                )
            }

            LazyColumn(modifier = Modifier.padding(horizontal = 16.dp)) {
                items(state.filteredEvents, key = { it.id }) { event ->
                    val emoji = if (event.toStatus == "OFFLINE") "🔴" else if (event.toStatus == "ONLINE") "🟢" else "🟡"
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("$emoji ${formatTimeOnly(event.occurredAt)}", fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "${event.serverName}: ${event.fromStatus} → ${event.toStatus}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
