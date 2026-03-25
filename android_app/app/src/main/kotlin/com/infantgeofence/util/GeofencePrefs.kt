package com.infantgeofence.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.infantgeofence.data.model.GeoPoint
import org.osmdroid.util.GeoPoint as OsmGeoPoint

object GeofencePrefs {
    private const val PREFS_NAME = "geofence_prefs"
    private const val KEY_POLYGON = "polygon_"
    private const val KEY_FENCE_NAME = "fence_name_"
    private val gson = Gson()

    fun savePolygon(ctx: Context, deviceId: String, points: List<OsmGeoPoint>, name: String = "Safe Zone") {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val list  = points.map { GeoPoint(it.latitude, it.longitude) }
        prefs.edit()
            .putString(KEY_POLYGON + deviceId, gson.toJson(list))
            .putString(KEY_FENCE_NAME + deviceId, name)
            .apply()
    }

    fun loadPolygon(ctx: Context, deviceId: String): List<OsmGeoPoint> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json  = prefs.getString(KEY_POLYGON + deviceId, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<GeoPoint>>() {}.type
            val list: List<GeoPoint> = gson.fromJson(json, type)
            list.map { OsmGeoPoint(it.lat, it.lng) }
        }.getOrElse { emptyList() }
    }

    fun loadFenceName(ctx: Context, deviceId: String): String {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FENCE_NAME + deviceId, "Safe Zone") ?: "Safe Zone"
    }

    fun clearPolygon(ctx: Context, deviceId: String) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_POLYGON + deviceId).remove(KEY_FENCE_NAME + deviceId).apply()
    }

    fun toGeoPoints(points: List<OsmGeoPoint>): List<GeoPoint> =
        points.map { GeoPoint(it.latitude, it.longitude) }
}
