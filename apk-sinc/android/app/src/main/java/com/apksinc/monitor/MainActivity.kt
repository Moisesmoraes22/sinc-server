package com.apksinc.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apksinc.monitor.navigation.OmgSincNavHost
import com.apksinc.monitor.ui.theme.OmgSincAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            OmgSincAppTheme {
                OmgSincNavHost()
            }
        }
    }
}
