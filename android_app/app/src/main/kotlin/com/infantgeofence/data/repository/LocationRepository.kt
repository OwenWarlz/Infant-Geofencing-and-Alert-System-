package com.infantgeofence.data.repository

import com.infantgeofence.InfantGeoFenceApp
import com.infantgeofence.data.model.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.osmdroid.util.GeoPoint as OsmGeoPoint
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// ── Network singleton ─────────────────────────────────────────
object NetworkClient {
    private val okhttp = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: ApiService = Retrofit.Builder()
        .baseUrl(InfantGeoFenceApp.BASE_URL)
        .client(okhttp)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

// ── Main data repository ──────────────────────────────────────
class LocationRepository {
    private val api = NetworkClient.api

    suspend fun getLocation(deviceId: String): DeviceLocation? = runCatching {
        api.getLocation(deviceId).body()
    }.getOrNull()

    suspend fun getTrail(deviceId: String, count: Int = 60): List<TrailPoint> = runCatching {
        api.getTrail(deviceId, count).body()?.trail ?: emptyList()
    }.getOrElse { emptyList() }

    suspend fun setGeofence(deviceId: String, name: String, points: List<GeoPoint>): Boolean =
        runCatching {
            api.setGeofence(GeofenceRequest(deviceId, name, points)).body()?.success == true
        }.getOrElse { false }

    suspend fun checkGeofence(deviceId: String, lat: Double, lng: Double): String =
        runCatching {
            api.checkGeofence(mapOf(
                "device_id" to deviceId,
                "latitude"  to lat.toString(),
                "longitude" to lng.toString()
            )).body()?.status ?: "no_fence"
        }.getOrElse { "error" }

    suspend fun getAlerts(deviceId: String, limit: Int = 50): AlertsResponse =
        runCatching {
            api.getAlerts(deviceId, limit).body() ?: AlertsResponse(false)
        }.getOrElse { AlertsResponse(false) }

    suspend fun markAlertsRead(deviceId: String) = runCatching {
        api.markAlertsRead(mapOf("device_id" to deviceId, "mark_read" to "true"))
    }
}

// ── In-app geofence engine (Ray-Casting) ─────────────────────
object GeofenceEngine {

    /**
     * Ray-casting point-in-polygon algorithm.
     * @param point  the GPS point to test
     * @param polygon list of vertices (OsmGeoPoint)
     * @return true if point is inside the polygon
     */
    fun isInside(point: OsmGeoPoint, polygon: List<OsmGeoPoint>): Boolean {
        val n = polygon.size
        if (n < 3) return false
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xj = polygon[j].longitude
            val yj = polygon[j].latitude
            val intersect = (yi > point.latitude) != (yj > point.latitude) &&
                    point.longitude < (xj - xi) * (point.latitude - yi) / (yj - yi) + xi
            if (intersect) inside = !inside
            j = i
        }
        return inside
    }

    fun toFenceStatus(inside: Boolean) =
        if (inside) FenceStatus.INSIDE else FenceStatus.OUTSIDE
}
