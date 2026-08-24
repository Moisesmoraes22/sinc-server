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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
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
fun SettingsScreen(onAboutClick: () -> Unit = {}) {
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
                    SwitchRow("Notificações", settings.notificationsEnabled, ::requestOrEnableNotifications)
                    SwitchRow("Som", settings.soundEnabled, viewModel::setSoundEnabled)
                    SwitchRow("Vibração", settings.vibrationEnabled, viewModel::setVibrationEnabled)
                }
            }

            item { SectionLabel("Aparência") }
            item {
                SettingsCard {
                    Text("Tema", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemeMode.entries.forEach { mode ->
                            ThemeChip(
                                label = mode.name,
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
                    InfoRow("Intervalo de atualização", "${settings.refreshIntervalSeconds}s")
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
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val colors = ApkSincColors.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = colors.accent))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = ApkSincColors.colors
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = colors.textPrimary)
    }
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
