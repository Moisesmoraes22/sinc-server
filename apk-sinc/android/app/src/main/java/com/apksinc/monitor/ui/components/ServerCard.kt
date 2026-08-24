package com.apksinc.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.R
import com.apksinc.monitor.domain.ServerInfo
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.ui.theme.ApkSincColors
import com.apksinc.monitor.util.formatDuration
import com.apksinc.monitor.util.formatTimeOnly

/**
 * Card de servidor no padrao v2: barra lateral de 2.5dp com a cor do status
 * (severidade em vez de decoracao), nome + endereco, pill de status, e uma
 * linha de metadados (tempo de resposta / ultima checagem). Normal nao ganha
 * marcacao lateral de proposito - so o que precisa de atencao chama atencao.
 */
@Composable
fun ServerCard(server: ServerInfo, onClick: () -> Unit) {
    val colors = ApkSincColors.colors
    val barColor = when (server.status) {
        ServerStatus.OFFLINE -> colors.danger
        ServerStatus.ATENCAO -> colors.warning
        ServerStatus.ONLINE -> colors.card
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .background(barColor),
            )

            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = server.id,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    StatusBadge(status = server.status)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (server.status == ServerStatus.OFFLINE && server.lastDownAt != null) {
                        MetaItem(
                            label = "OFFLINE HÁ",
                            value = formatDuration(server.lastDownAt),
                            valueColor = colors.danger,
                        )
                    } else {
                        MetaItem(
                            label = stringRes(R.string.label_synced_ago),
                            value = formatDuration(server.lastCheck),
                        )
                    }
                    MetaItem(
                        label = stringRes(R.string.label_last_check),
                        value = formatTimeOnly(server.lastCheck),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color = ApkSincColors.colors.textPrimary) {
    Column {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = ApkSincColors.colors.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
        )
    }
}

@Composable
private fun stringRes(id: Int): String = androidx.compose.ui.res.stringResource(id)
