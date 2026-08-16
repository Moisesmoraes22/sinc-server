package com.apksinc.monitor

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.apksinc.monitor.data.local.SettingsDataStore
import com.apksinc.monitor.data.local.SincDatabase
import com.apksinc.monitor.data.remote.RetrofitFactory
import com.apksinc.monitor.data.repository.ServerRepository

class SincApplication : Application() {

    lateinit var repository: ServerRepository
        private set

    lateinit var settingsDataStore: SettingsDataStore
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val database = SincDatabase.getInstance(this)
        val api = RetrofitFactory.create()
        repository = ServerRepository(api, database.sincDao())
        settingsDataStore = SettingsDataStore(this)
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
