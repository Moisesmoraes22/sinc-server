package com.apksinc.monitor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.ui.theme.OmgSincTheme

/** Estado vazio - usado sempre que uma lista nao tem itens ainda (nunca uma
 * tela em branco sem explicacao). */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = OmgSincTheme.colors.textMuted,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OmgSincTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = OmgSincTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, modifier = Modifier.padding(top = 8.dp)) {
                Text(actionLabel, color = OmgSincTheme.colors.accent)
            }
        }
    }
}

/** Estado de erro - sempre com uma acao de tentar novamente. */
@Composable
fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Filled.ErrorOutline,
        title = "Nao foi possivel carregar",
        message = message,
        modifier = modifier,
        actionLabel = "Tentar novamente",
        onAction = onRetry,
    )
}

/** Estado de carregamento inicial (tela cheia). Uso pontual: preferimos
 * skeleton/conteudo parcial quando ja ha cache local para mostrar. */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = OmgSincTheme.colors.accent)
    }
}
