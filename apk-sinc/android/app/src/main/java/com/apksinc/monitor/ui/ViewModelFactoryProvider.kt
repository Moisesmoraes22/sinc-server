package com.apksinc.monitor.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.apksinc.monitor.SincApplication

/** Facilita a criacao de ViewModels dentro de Composables a partir do Application. */
object ViewModelFactoryProvider {

    @Composable
    fun factory(): SincViewModelFactory {
        val app = LocalContext.current.applicationContext as SincApplication
        return SincViewModelFactory(app.repository, app.settingsDataStore)
    }
}
