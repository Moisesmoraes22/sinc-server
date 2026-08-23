package com.apksinc.monitor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.domain.ServerStatus
import com.apksinc.monitor.ui.theme.StatusAttention
import com.apksinc.monitor.ui.theme.StatusAttentionContainer
import com.apksinc.monitor.ui.theme.StatusOffline
import com.apksinc.monitor.ui.theme.StatusOfflineContainer
import com.apksinc.monitor.ui.theme.StatusOnline
import com.apksinc.monitor.ui.theme.StatusOnlineContainer

data class StatusColors(val accent: Color, val container: Color, val emoji: String, val label: String)

fun statusColorsFor(status: ServerStatus): StatusColors = when (status) {
    ServerStatus.ONLINE -> StatusColors(StatusOnline, StatusOnlineContainer, "🟢", "ONLINE")
    ServerStatus.ATENCAO -> StatusColors(StatusAttention, StatusAttentionContainer, "🟡", "ATENÇÃO")
    ServerStatus.OFFLINE -> StatusColors(StatusOffline, StatusOfflineContainer, "🔴", "OFFLINE")
}

@Composable
fun StatusDot(status: ServerStatus, modifier: Modifier = Modifier) {
    val colors = statusColorsFor(status)
    Box(
        modifier = modifier
            .padding(2.dp)
            .background(colors.accent, CircleShape)
    )
}

@Composable
fun StatusBadge(status: ServerStatus) {
    val colors = statusColorsFor(status)
    Box(
        modifier = Modifier
            .background(colors.container, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "${colors.emoji} ${colors.label}",
            style = MaterialTheme.typography.labelLarge,
            color = colors.accent,
        )
    }
}
