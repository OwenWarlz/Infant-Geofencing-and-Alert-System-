package com.infantgeofence.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.infantgeofence.InfantGeoFenceApp
import com.infantgeofence.data.model.FenceStatus
import com.infantgeofence.data.repository.GeofenceEngine
import com.infantgeofence.data.repository.LocationRepository
import com.infantgeofence.util.GeofencePrefs
import com.infantgeofence.util.NotificationHelper
import kotlinx.coroutines.*
import org.osmdroid.util.GeoPoint

class GeofenceMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val repo  = LocationRepository()
    private var lastStatus = FenceStatus.UNKNOWN

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, NotificationHelper.buildServiceNotification(this))
        startPolling()
        return START_STICKY
    }

    private fun startPolling() {
        scope.launch {
            while (true) {
                try {
                    val loc = repo.getLocation(InfantGeoFenceApp.DEVICE_ID)
                    if (loc != null && loc.success) {
                        val polygon = GeofencePrefs.loadPolygon(
                            this@GeofenceMonitorService, InfantGeoFenceApp.DEVICE_ID
                        )
                        if (polygon.size >= 3) {
                            val pt     = GeoPoint(loc.latitude, loc.longitude)
                            val inside = GeofenceEngine.isInside(pt, polygon)
                            val status = GeofenceEngine.toFenceStatus(inside)

                            if (status != lastStatus && lastStatus != FenceStatus.UNKNOWN) {
                                if (status == FenceStatus.OUTSIDE) {
                                    NotificationHelper.sendGeofenceAlert(
                                        this@GeofenceMonitorService,
                                        "⚠️ Geofence Alert!",
                                        "${loc.childName} has LEFT the safe zone!"
                                    )
                                } else {
                                    NotificationHelper.sendGeofenceAlert(
                                        this@GeofenceMonitorService,
                                        "✅ Safe Zone Entered",
                                        "${loc.childName} has returned to the safe zone."
                                    )
                                }
                            }
                            lastStatus = status
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(5_000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
