package com.apksinc.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.local.SincDatabase
import com.apksinc.monitor.data.remote.RetrofitFactory
import com.apksinc.monitor.data.repository.ServerRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SincApplication : Application() {

    lateinit var repository: ServerRepository
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    private val appScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val database = SincDatabase.getInstance(this)
        val api = RetrofitFactory.create()
        repository = ServerRepository(api, database.sincDao())
        settingsDataStore = SettingsDataStore(this)

        registerCurrentFcmToken()
    }

    /**
     * SincFirebaseMessagingService.onNewToken() so dispara quando o token
     * MUDA (primeira instalacao, token expirado/rotacionado) - se o app for
     * reinstalado/recompilado sem gerar um token novo, ou se o POST de
     * registro falhar silenciosamente naquele momento, o dispositivo nunca
     * mais e re-registrado e as notificacoes reais simplesmente param de
     * chegar, sem nenhum erro visivel. Buscar e reenviar o token atual toda
     * vez que o app abre e barato (o backend so faz um upsert por token) e
     * torna o registro auto-recuperavel.
     */
    private fun registerCurrentFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) return@addOnCompleteListener
            val token = task.result ?: return@addOnCompleteListener
            appScope.launch {
                runCatching { repository.registerDeviceToken(token) }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            getString(R.string.notification_channel_id),
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = getString(R.string.notification_channel_description)
            enableLights(true)
            enableVibration(true)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }
}
