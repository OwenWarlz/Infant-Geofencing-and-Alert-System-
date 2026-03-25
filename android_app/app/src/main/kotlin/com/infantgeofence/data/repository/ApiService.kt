package com.infantgeofence.data.repository

import com.infantgeofence.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("get_location.php")
    suspend fun getLocation(
        @Query("device_id") deviceId: String
    ): Response<DeviceLocation>

    @GET("get_location.php")
    suspend fun getTrail(
        @Query("device_id") deviceId: String,
        @Query("history")   count: Int
    ): Response<TrailResponse>

    @POST("set_geofence.php")
    suspend fun setGeofence(
        @Body request: GeofenceRequest
    ): Response<GeofenceResponse>

    @POST("check_geofence.php")
    suspend fun checkGeofence(
        @Body body: Map<String, String>
    ): Response<ApiResponse>

    @GET("get_alerts.php")
    suspend fun getAlerts(
        @Query("device_id") deviceId: String,
        @Query("limit")     limit: Int = 50
    ): Response<AlertsResponse>

    @POST("get_alerts.php")
    suspend fun markAlertsRead(
        @Body body: Map<String, String>
    ): Response<ApiResponse>
}
