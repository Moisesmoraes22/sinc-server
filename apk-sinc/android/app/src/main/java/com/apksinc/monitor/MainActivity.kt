package com.apksinc.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.apksinc.monitor.data.local.ThemeMode
import com.apksinc.monitor.navigation.SincNavHost
import com.apksinc.monitor.ui.theme.ApkSincTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SERVER_ID = "extra_server_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val serverId = intent?.getStringExtra(EXTRA_SERVER_ID)
        val app = application as SincApplication

        setContent {
            val settings by app.settingsDataStore.settingsFlow.collectAsState(initial = null)
            val darkTheme = when (settings?.themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM, null -> isSystemInDarkTheme()
            }
            ApkSincTheme(darkTheme = darkTheme) {
                SincNavHost(startServerId = serverId)
            }
        }
    }
}
