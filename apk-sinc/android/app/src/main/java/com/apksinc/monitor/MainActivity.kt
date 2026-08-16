package com.apksinc.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

        setContent {
            ApkSincTheme {
                SincNavHost(startServerId = serverId)
            }
        }
    }
}
