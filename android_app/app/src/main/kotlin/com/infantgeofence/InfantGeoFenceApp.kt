package com.infantgeofence

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.osmdroid.config.Configuration

class InfantGeoFenceApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // OSMDroid configuration — required before any map use
        Configuration.getInstance().apply {
            userAgentValue = packageName
            load(this@InfantGeoFenceApp,
                getSharedPreferences("osm_prefs", MODE_PRIVATE))
        }

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Geofence alert channel — HIGH importance
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_GEOFENCE,
                "Geofence Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when child crosses the safe zone boundary"
                enableVibration(true)
                enableLights(true)
            })

            // Background service channel — LOW importance
            nm.createNotificationChannel(NotificationChannel(
                CHANNEL_SERVICE,
                "Monitoring Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps geofence monitoring running in background"
            })
        }
    }

    companion object {
        const val CHANNEL_GEOFENCE = "geofence_alerts"
        const val CHANNEL_SERVICE  = "monitoring_service"
        const val DEVICE_ID        = "ESP32_001"
        // ↓ Change this to your WAMP server IP
        const val BASE_URL         = "http://192.168.1.2/infant_geofence/api/"
    }
}
