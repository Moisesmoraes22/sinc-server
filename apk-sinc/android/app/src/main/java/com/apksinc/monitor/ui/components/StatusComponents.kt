package com.apksinc.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.ui.theme.ApkSincColors

data class StatusColors(val accent: Color, val soft: Color, val label: String)

@Composable
fun statusColorsFor(status: ServerStatus): StatusColors {
    val colors = ApkSincColors.colors
    return when (status) {
        ServerStatus.ONLINE -> StatusColors(colors.success, colors.successSoft, "Normal")
        ServerStatus.ATENCAO -> StatusColors(colors.warning, colors.warningSoft, "Atenção")
        ServerStatus.OFFLINE -> StatusColors(colors.danger, colors.dangerSoft, "Crítico")
    }
}

/** Pontinho de status isolado - usado como marcador (ex.: dentro de uma pill). */
@Composable
fun StatusDot(status: ServerStatus, modifier: Modifier = Modifier) {
    val colors = statusColorsFor(status)
    Box(
        modifier = modifier
            .size(6.dp)
            .background(colors.accent, CircleShape),
    )
}

/** Pill de status: ponto + rotulo, com fundo "soft" da cor funcional. */
@Composable
fun StatusBadge(status: ServerStatus) {
    val colors = statusColorsFor(status)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(colors.soft, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).background(colors.accent, CircleShape))
        Text(
            text = colors.label,
            style = MaterialTheme.typography.labelMedium,
            color = colors.accent,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
