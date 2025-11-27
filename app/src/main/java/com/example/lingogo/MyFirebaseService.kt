package com.example.lingogo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseService : FirebaseMessagingService() {

    // Esta función se dispara cuando llega una notificación y la app está abierta
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            mostrarNotificacion(it.title, it.body)
        }
    }

    // Esta función se dispara si el token cambia (mantenimiento de Firebase)
    override fun onNewToken(token: String) {
        // Aquí podrías volver a enviarlo a tu base de datos si quieres ser muy estricto
    }

    private fun mostrarNotificacion(title: String?, body: String?) {
        val channelId = "lingogo_chat_channel"
        val intent = Intent(this, MainActivity::class.java) // A dónde va si tocan la notificación
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // CAMBIA ESTO por tu icono de app (R.drawable.tu_icono)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Crear canal de notificaciones (Obligatorio para Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Mensajes de Chat", NotificationManager.IMPORTANCE_DEFAULT)
            manager.createNotificationChannel(channel)
        }

        manager.notify(0, builder.build())
    }
}