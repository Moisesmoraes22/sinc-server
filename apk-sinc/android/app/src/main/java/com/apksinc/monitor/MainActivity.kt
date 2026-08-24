package com.apksinc.monitor

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.apksinc.monitor.data.local.ThemeMode
import com.apksinc.monitor.navigation.SincNavHost
import com.apksinc.monitor.ui.theme.ApkSincTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SERVER_ID = "extra_server_id"
        private const val PREFS_NAME = "sinc_first_run"
        private const val KEY_NOTIFICATION_ASKED = "notification_permission_asked"
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfFirstRun()

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

    private fun requestNotificationPermissionIfFirstRun() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_NOTIFICATION_ASKED, false)) return

        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        prefs.edit { putBoolean(KEY_NOTIFICATION_ASKED, true) }

        if (!alreadyGranted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
