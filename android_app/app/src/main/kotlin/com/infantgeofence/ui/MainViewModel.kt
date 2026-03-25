package com.infantgeofence.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.infantgeofence.InfantGeoFenceApp
import com.infantgeofence.data.model.*
import com.infantgeofence.data.repository.GeofenceEngine
import com.infantgeofence.data.repository.LocationRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MainViewModel : ViewModel() {

    private val repo = LocationRepository()
    private var pollJob: Job? = null

    // ── Location ──────────────────────────────────────────────
    private val _location = MutableLiveData<DeviceLocation?>()
    val location: LiveData<DeviceLocation?> = _location

    private val _trail = MutableLiveData<List<GeoPoint>>(emptyList())
    val trail: LiveData<List<GeoPoint>> = _trail

    // ── Geofence ──────────────────────────────────────────────
    private val _polygon = MutableLiveData<List<GeoPoint>>(emptyList())
    val polygon: LiveData<List<GeoPoint>> = _polygon

    private val _fenceStatus = MutableLiveData(FenceStatus.UNKNOWN)
    val fenceStatus: LiveData<FenceStatus> = _fenceStatus

    private val _fenceName = MutableLiveData("Safe Zone")
    val fenceName: LiveData<String> = _fenceName

    // ── Alerts ────────────────────────────────────────────────
    private val _alerts = MutableLiveData<List<GeoAlert>>(emptyList())
    val alerts: LiveData<List<GeoAlert>> = _alerts

    private val _unreadCount = MutableLiveData(0)
    val unreadCount: LiveData<Int> = _unreadCount

    // ── Save geofence polygon ─────────────────────────────────
    fun setPolygon(points: List<GeoPoint>, name: String = "Safe Zone") {
        _polygon.value = points
        _fenceName.value = name
        // Re-evaluate fence status with current location
        val loc = _location.value ?: return
        val pt  = GeoPoint(loc.latitude, loc.longitude)
        val inside = GeofenceEngine.isInside(pt, points)
        _fenceStatus.value = GeofenceEngine.toFenceStatus(inside)
    }

    fun clearPolygon() {
        _polygon.value = emptyList()
        _fenceStatus.value = FenceStatus.UNKNOWN
    }

    // ── Polling ───────────────────────────────────────────────
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                fetchLocation()
                fetchAlerts()
                delay(5_000)
            }
        }
    }

    fun stopPolling() { pollJob?.cancel() }

    // ── Single fetch ──────────────────────────────────────────
    fun fetchLocation() {
        viewModelScope.launch {
            val loc = repo.getLocation(InfantGeoFenceApp.DEVICE_ID)
            _location.postValue(loc)

            if (loc != null && loc.success) {
                // Update trail
                val trail = repo.getTrail(InfantGeoFenceApp.DEVICE_ID, 60)
                _trail.postValue(trail.map { GeoPoint(it.latitude, it.longitude) })

                // Update fence status
                val poly = _polygon.value ?: emptyList()
                if (poly.size >= 3) {
                    val pt = GeoPoint(loc.latitude, loc.longitude)
                    val inside = GeofenceEngine.isInside(pt, poly)
                    _fenceStatus.postValue(GeofenceEngine.toFenceStatus(inside))
                }
            }
        }
    }

    fun fetchAlerts() {
        viewModelScope.launch {
            val resp = repo.getAlerts(InfantGeoFenceApp.DEVICE_ID)
            _alerts.postValue(resp.alerts)
            _unreadCount.postValue(resp.unreadCount)
        }
    }

    fun markAlertsRead() {
        viewModelScope.launch {
            repo.markAlertsRead(InfantGeoFenceApp.DEVICE_ID)
            fetchAlerts()
        }
    }

    suspend fun saveGeofenceToServer(points: List<GeoPoint>, name: String): Boolean {
        val geoPoints = points.map {
            com.infantgeofence.data.model.GeoPoint(it.latitude, it.longitude)
        }
        return repo.setGeofence(InfantGeoFenceApp.DEVICE_ID, name, geoPoints)
    }

    override fun onCleared() {
        super.onCleared()
        pollJob?.cancel()
    }
}
