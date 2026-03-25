package com.infantgeofence.data.model

import com.google.gson.annotations.SerializedName

// ── Device Location ──────────────────────────────────────────
data class DeviceLocation(
    @SerializedName("device_id")  val deviceId: String = "",
    @SerializedName("child_name") val childName: String = "Unknown",
    @SerializedName("latitude")   val latitude: Double = 0.0,
    @SerializedName("longitude")  val longitude: Double = 0.0,
    @SerializedName("speed_kmph") val speedKmph: Double = 0.0,
    @SerializedName("satellites") val satellites: Int = 0,
    @SerializedName("hdop")       val hdop: Double = 99.9,
    @SerializedName("battery_pct") val batteryPct: Int? = null,
    @SerializedName("timestamp")  val timestamp: String = "",
    @SerializedName("online")     val online: Boolean = false,
    @SerializedName("success")    val success: Boolean = false
)

// ── Trail point ───────────────────────────────────────────────
data class TrailPoint(
    @SerializedName("latitude")  val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("timestamp") val timestamp: String = ""
)

data class TrailResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("trail")   val trail: List<TrailPoint> = emptyList()
)

// ── Geofence ──────────────────────────────────────────────────
data class GeoPoint(
    val lat: Double,
    val lng: Double
)

data class GeofenceRequest(
    @SerializedName("device_id")   val deviceId: String,
    @SerializedName("name")        val name: String,
    @SerializedName("coordinates") val coordinates: List<GeoPoint>
)

data class GeofenceResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String = "",
    @SerializedName("id")      val id: Int = 0
)

// ── Alert ─────────────────────────────────────────────────────
data class GeoAlert(
    @SerializedName("id")          val id: Int,
    @SerializedName("device_id")   val deviceId: String,
    @SerializedName("child_name")  val childName: String = "",
    @SerializedName("alert_type")  val alertType: String,
    @SerializedName("latitude")    val latitude: Double? = null,
    @SerializedName("longitude")   val longitude: Double? = null,
    @SerializedName("is_read")     val isRead: Int = 0,
    @SerializedName("timestamp")   val timestamp: String = ""
) {
    val typeLabel: String get() = when (alertType) {
        "outside"      -> "⚠️ Left Safe Zone"
        "inside"       -> "✅ Entered Safe Zone"
        "sos"          -> "🆘 SOS Button Pressed"
        "low_battery"  -> "🔋 Low Battery"
        else           -> alertType
    }
    val isCritical: Boolean get() = alertType == "outside" || alertType == "sos"
    val isReadBool: Boolean get() = isRead == 1
}

data class AlertsResponse(
    @SerializedName("success")      val success: Boolean,
    @SerializedName("alerts")       val alerts: List<GeoAlert> = emptyList(),
    @SerializedName("unread_count") val unreadCount: Int = 0
)

// ── Generic API response ──────────────────────────────────────
data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String = "",
    @SerializedName("status")  val status: String = ""
)

// ── Fence status ──────────────────────────────────────────────
enum class FenceStatus { UNKNOWN, INSIDE, OUTSIDE }
