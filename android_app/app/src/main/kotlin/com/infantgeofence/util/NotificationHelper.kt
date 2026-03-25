package com.infantgeofence.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.infantgeofence.InfantGeoFenceApp
import com.infantgeofence.R
import com.infantgeofence.ui.MainActivity

object NotificationHelper {

    fun sendGeofenceAlert(ctx: Context, title: String, message: String) {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("nav_to", "alerts")
        }
        val pi = PendingIntent.getActivity(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notif = NotificationCompat.Builder(ctx, InfantGeoFenceApp.CHANNEL_GEOFENCE)
            .setSmallIcon(R.drawable.ic_fence)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setContentIntent(pi)
            .build()

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notif)
    }

    fun buildServiceNotification(ctx: Context) =
        NotificationCompat.Builder(ctx, InfantGeoFenceApp.CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_fence)
            .setContentTitle("Infant GeoFence Active")
            .setContentText("Monitoring child's location…")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
}
