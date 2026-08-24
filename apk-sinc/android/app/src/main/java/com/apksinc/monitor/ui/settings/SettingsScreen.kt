package com.apksinc.monitor.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.BuildConfig
import com.apksinc.monitor.R
import com.apksinc.monitor.data.local.ThemeMode
import com.apksinc.monitor.ui.ViewModelFactoryProvider
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.theme.ApkSincColors

@Composable
fun SettingsScreen(onAboutClick: () -> Unit = {}, onApiStatusClick: () -> Unit = {}) {
    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val settings by viewModel.settings.collectAsState()
    val colors = ApkSincColors.colors
    val context = LocalContext.current

    fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // Pede a permissao do sistema so quando a pessoa esta de fato olhando
    // pra secao de notificacoes (nao no primeiro abrir do app) - segue o
    // toque na chave e tambem sincroniza se a chave ja estava "ligada" por
    // padrao mas a permissao do SO nunca foi concedida.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.setNotificationsEnabled(granted) }

    fun requestOrEnableNotifications(wantsEnabled: Boolean) {
        if (!wantsEnabled) {
            viewModel.setNotificationsEnabled(false)
        } else if (hasNotificationPermission()) {
            viewModel.setNotificationsEnabled(true)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(settings.notificationsEnabled) {
        if (settings.notificationsEnabled && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(containerColor = colors.background) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item { Text("Configurações", style = MaterialTheme.typography.headlineMedium, color = colors.textPrimary) }

            item { SectionLabel("Notificações") }
            item {
                SettingsCard {
                    SwitchRow(
                        label = "Notificações",
                        subtitle = "Receber alertas e atualizações",
                        checked = settings.notificationsEnabled,
                        onCheckedChange = ::requestOrEnableNotifications,
                    )
                    SwitchRow(
                        label = "Som",
                        subtitle = "Ativar sons de alerta",
                        checked = settings.soundEnabled,
                        onCheckedChange = viewModel::setSoundEnabled,
                    )
                    SwitchRow(
                        label = "Vibração",
                        subtitle = "Vibrar em notificações",
                        checked = settings.vibrationEnabled,
                        onCheckedChange = viewModel::setVibrationEnabled,
                        isLast = true,
                    )
                }
            }

            item { SectionLabel("Aparência") }
            item {
                SettingsCard {
                    Text("Tema", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeChip(
                                label = themeModeLabel(mode),
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.setThemeMode(mode) },
                            )
                        }
                    }
                }
            }

            item { SectionLabel("Monitoramento") }
            item {
                SettingsCard {
                    IntervalStepper(
                        seconds = settings.refreshIntervalSeconds,
                        onChange = viewModel::setRefreshIntervalSeconds,
                    )
                }
            }

            item { SectionLabel("Diagnóstico") }
            item {
                SettingsCard(onClick = onApiStatusClick) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Dns, contentDescription = null, tint = colors.textSecondary)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text("Status da API", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                                Text(
                                    "Conexão, banco de dados e monitor",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textMuted,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textMuted)
                    }
                }
            }

            item { SectionLabel("Sobre") }
            item {
                SettingsCard(onClick = onAboutClick) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                            Text(
                                "Versão ${BuildConfig.VERSION_NAME}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = colors.textMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ApkSincColors.colors.card)
            .then(clickModifier)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast: Boolean = false,
) {
    val colors = ApkSincColors.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent))
    }
    if (!isLast) androidx.compose.material3.HorizontalDivider(color = colors.hairline)
}

private val REFRESH_INTERVAL_OPTIONS = listOf(15, 30, 60, 120, 300)

@Composable
private fun IntervalStepper(seconds: Int, onChange: (Int) -> Unit) {
    val colors = ApkSincColors.colors
    val currentIndex = REFRESH_INTERVAL_OPTIONS.indexOf(seconds).let { if (it == -1) 1 else it }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text("Intervalo de atualização", style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
            Text(
                "Com que frequência o Dashboard busca dados novos",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onChange(REFRESH_INTERVAL_OPTIONS[(currentIndex - 1).coerceAtLeast(0)]) },
                enabled = currentIndex > 0,
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Diminuir intervalo", tint = colors.textSecondary)
            }
            Text(
                formatIntervalLabel(REFRESH_INTERVAL_OPTIONS[currentIndex]),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            IconButton(
                onClick = { onChange(REFRESH_INTERVAL_OPTIONS[(currentIndex + 1).coerceAtMost(REFRESH_INTERVAL_OPTIONS.lastIndex)]) },
                enabled = currentIndex < REFRESH_INTERVAL_OPTIONS.lastIndex,
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Aumentar intervalo", tint = colors.textSecondary)
            }
        }
    }
}

private fun formatIntervalLabel(seconds: Int): String =
    if (seconds < 60) "${seconds}s" else "${seconds / 60}min"

private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Sistema"
    ThemeMode.LIGHT -> "Claro"
    ThemeMode.DARK -> "Escuro"
}

@Composable
private fun ThemeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = ApkSincColors.colors
    val bg = if (selected) colors.accentSoft else colors.elevated
    val textColor = if (selected) colors.accent else colors.textSecondary
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = textColor)
    }
}
