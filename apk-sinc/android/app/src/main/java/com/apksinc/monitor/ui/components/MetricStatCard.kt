package com.apksinc.monitor.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.MetricSnapshot
import com.apksinc.monitor.ui.theme.OmgSincTheme

/** Card compacto de uma metrica na Home/Saude: icone, valor mais recente e
 * rotulo. Mostra "--" quando ainda nao ha leitura (estado vazio inline, sem
 * poluir a tela com um EmptyState por metrica). */
@Composable
fun MetricStatCard(snapshot: MetricSnapshot, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.elevated),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = snapshot.type.icon(),
                    contentDescription = null,
                    tint = OmgSincTheme.colors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = snapshot.value?.let { formatMetricValue(it) } ?: "--",
                style = MaterialTheme.typography.titleLarge,
                color = OmgSincTheme.colors.textPrimary,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = snapshot.type.label(),
                style = MaterialTheme.typography.bodySmall,
                color = OmgSincTheme.colors.textMuted,
            )
        }
    }
}

private fun formatMetricValue(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)
