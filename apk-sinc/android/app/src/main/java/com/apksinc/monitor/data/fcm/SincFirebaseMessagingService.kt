package com.apksinc.monitor.data.fcm

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.apksinc.monitor.MainActivity
import com.apksinc.monitor.R
import com.apksinc.monitor.SincApplication
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Recebe pushes do backend (via FCM). O app nunca monitora sozinho: apenas
 * exibe o que o backend detectou. Funciona em foreground e background porque
 * o sistema operacional entrega a notificacao mesmo com o app fechado,
 * atraves do canal de notificacao de alta prioridade criado em SincApplication.
 */
class SincFirebaseMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as SincApplication
        scope.launch {
            runCatching { app.repository.registerDeviceToken(token) }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        val serverId = message.data["serverId"]

        showNotification(title, body, serverId)
    }

    private fun showNotification(title: String, body: String, serverId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (serverId != null) {
                putExtra(MainActivity.EXTRA_SERVER_ID, serverId)
            }
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        val pendingIntent = PendingIntent.getActivity(this, serverId.hashCode(), intent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(this).notify(serverId?.hashCode() ?: title.hashCode(), notification)
    }
}
