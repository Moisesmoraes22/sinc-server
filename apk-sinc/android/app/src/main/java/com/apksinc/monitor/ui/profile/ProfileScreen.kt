package com.apksinc.monitor.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.item
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import com.apksinc.monitor.BuildConfig
import com.apksinc.monitor.ui.components.SectionLabel
import com.apksinc.monitor.ui.theme.OmgSincTheme

@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val state by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var hasFocus by remember { mutableStateOf(false) }

    LaunchedEffect(state.profile?.displayName) {
        if (!hasFocus) name = state.profile?.displayName.orEmpty()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.headlineMedium,
                color = OmgSincTheme.colors.textPrimary,
            )
        }

        item {
            SettingsCard {
                Text(
                    text = "Como podemos te chamar?",
                    style = MaterialTheme.typography.titleSmall,
                    color = OmgSincTheme.colors.textPrimary,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .onFocusChanged { focus ->
                            hasFocus = focus.isFocused
                            if (!focus.isFocused && name.isNotBlank() && name != state.profile?.displayName) {
                                viewModel.updateDisplayName(name)
                            }
                        },
                )
            }
        }

        item { SectionLabel("Notificacoes") }
        item {
            SettingsCard {
                SettingsSwitchRow(
                    label = "Lembretes de habito",
                    checked = state.settings.notificationsEnabled,
                    onCheckedChange = viewModel::setNotificationsEnabled,
                )
                SettingsSwitchRow(
                    label = "Som",
                    checked = state.settings.soundEnabled,
                    onCheckedChange = viewModel::setSoundEnabled,
                )
                SettingsSwitchRow(
                    label = "Vibracao",
                    checked = state.settings.vibrationEnabled,
                    onCheckedChange = viewModel::setVibrationEnabled,
                )
            }
        }

        item { SectionLabel("Sobre") }
        item {
            SettingsCard {
                Text(
                    text = "OMG SINC",
                    style = MaterialTheme.typography.titleSmall,
                    color = OmgSincTheme.colors.textPrimary,
                )
                Text(
                    text = "Versao ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmgSincTheme.colors.textMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = OmgSincTheme.colors.card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SettingsSwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = OmgSincTheme.colors.textPrimary,
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = OmgSincTheme.colors.accent),
        )
    }
}
