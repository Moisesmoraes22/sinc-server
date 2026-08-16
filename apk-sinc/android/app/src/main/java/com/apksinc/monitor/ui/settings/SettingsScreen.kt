package com.apksinc.monitor.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.apksinc.monitor.BuildConfig
import com.apksinc.monitor.R
import com.apksinc.monitor.data.local.ThemeMode
import com.apksinc.monitor.ui.ViewModelFactoryProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactoryProvider.factory())
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(horizontal = 8.dp)) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_notifications)) },
                trailingContent = {
                    Switch(checked = settings.notificationsEnabled, onCheckedChange = viewModel::setNotificationsEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_sound)) },
                trailingContent = {
                    Switch(checked = settings.soundEnabled, onCheckedChange = viewModel::setSoundEnabled)
                },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_vibration)) },
                trailingContent = {
                    Switch(checked = settings.vibrationEnabled, onCheckedChange = viewModel::setVibrationEnabled)
                },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_theme)) },
                supportingContent = { Text(settings.themeMode.name) },
            )
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                ThemeMode.entries.forEach { mode ->
                    androidx.compose.material3.FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_refresh_interval)) },
                supportingContent = { Text("${settings.refreshIntervalSeconds}s") },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_test_notification)) },
                modifier = Modifier,
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_about)) },
                supportingContent = { Text("APK SINC v${BuildConfig.VERSION_NAME}") },
            )
        }
    }
}
