package com.apksinc.monitor.ui.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.apksinc.monitor.ui.components.StatusBadge
import com.apksinc.monitor.util.formatTimeOnly

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDetailsScreen(serverId: String, onBack: () -> Unit) {
    val viewModel: ServerDetailsViewModel = viewModel(factory = ViewModelFactoryProvider.detailsFactory(serverId))
    val state by viewModel.uiState.collectAsState()
    val server = state.server

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(server?.name ?: androidx.compose.ui.res.stringResource(R.string.details_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        if (server == null) {
            Column(modifier = Modifier.padding(padding).padding(24.dp)) {
                Text("Carregando...")
            }
            return@Scaffold
        }

        LazyColumn(modifier = Modifier.padding(padding).padding(horizontal = 20.dp)) {
            item {
                Column(modifier = Modifier.padding(vertical = 16.dp)) {
                    StatusBadge(status = server.status)
                    DetailRow(androidx.compose.ui.res.stringResource(R.string.details_address), server.id)
                    DetailRow(
                        androidx.compose.ui.res.stringResource(R.string.label_last_check),
                        formatTimeOnly(server.lastCheck),
                    )
                    DetailRow(
                        androidx.compose.ui.res.stringResource(R.string.label_response_time),
                        server.responseTimeMs?.let { "$it ms" } ?: "--",
                    )
                    DetailRow(
                        androidx.compose.ui.res.stringResource(R.string.details_last_down),
                        formatTimeOnly(server.lastDownAt),
                    )
                    DetailRow(
                        androidx.compose.ui.res.stringResource(R.string.details_last_up),
                        formatTimeOnly(server.lastUpAt),
                    )
                    DetailRow(
                        androidx.compose.ui.res.stringResource(R.string.details_down_count),
                        server.downCount.toString(),
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.details_history),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            items(state.events, key = { it.id }) { event ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("${event.fromStatus} → ${event.toStatus}", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        formatTimeOnly(event.occurredAt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}
