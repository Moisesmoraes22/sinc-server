package com.apksinc.monitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.R
import com.apksinc.monitor.domain.ServerInfo
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.util.formatDuration
import com.apksinc.monitor.util.formatTimeOnly

@Composable
fun ServerCard(server: ServerInfo, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = server.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 10.dp))
            StatusBadge(status = server.status)
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(top = 14.dp))

            if (server.status == ServerStatus.OFFLINE && server.lastDownAt != null) {
                Text(
                    text = stringResourceOfflineSince(server.lastDownAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.label_response_time) + "  " +
                            (server.responseTimeMs?.let { "$it ms" } ?: "--"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = stringResource(R.string.label_last_check) + "  " + formatTimeOnly(server.lastCheck),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun stringResourceOfflineSince(lastDownAtIso: String): String {
    val duration = formatDuration(lastDownAtIso)
    return stringResource(R.string.label_offline_since) + " " + duration
}

@Composable
private fun stringResource(id: Int): String = androidx.compose.ui.res.stringResource(id)
