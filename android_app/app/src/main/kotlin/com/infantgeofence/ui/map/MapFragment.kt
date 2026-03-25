package com.infantgeofence.ui.map

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infantgeofence.R
import com.infantgeofence.data.model.FenceStatus
import com.infantgeofence.databinding.FragmentMapBinding
import com.infantgeofence.ui.MainViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import org.osmdroid.views.overlay.Polyline

class MapFragment : Fragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    private var childMarker: Marker? = null
    private var trailPolyline: Polyline? = null
    private var fencePolygon: Polygon? = null
    private var followDevice = true
    private var showTrail = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
        setupControls()
        observeData()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK) // OpenStreetMap
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(26.1433, 91.7362)) // Default: Guwahati
        }
    }

    private fun setupControls() {
        binding.fabFollow.setOnClickListener {
            followDevice = !followDevice
            binding.fabFollow.setImageResource(
                if (followDevice) R.drawable.ic_my_location else R.drawable.ic_location_searching
            )
            if (followDevice) centerOnChild()
        }

        binding.fabTrail.setOnClickListener {
            showTrail = !showTrail
            trailPolyline?.isEnabled = showTrail
            binding.mapView.invalidate()
            binding.fabTrail.alpha = if (showTrail) 1.0f else 0.5f
        }
    }

    private fun observeData() {
        viewModel.location.observe(viewLifecycleOwner) { loc ->
            if (loc == null || !loc.success) return@observe
            updateChildMarker(GeoPoint(loc.latitude, loc.longitude), loc.childName, loc.online)
            updateStatusBar(loc.speedKmph, loc.satellites, loc.online)
            if (followDevice) centerOnChild()
        }

        viewModel.trail.observe(viewLifecycleOwner) { trail ->
            updateTrail(trail)
        }

        viewModel.polygon.observe(viewLifecycleOwner) { poly ->
            updateFenceOverlay(poly)
        }

        viewModel.fenceStatus.observe(viewLifecycleOwner) { status ->
            updateFenceColor(status)
            updateStatusBanner(status)
        }
    }

    private fun updateChildMarker(point: GeoPoint, name: String, online: Boolean) {
        if (childMarker == null) {
            childMarker = Marker(binding.mapView).apply {
                title = name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                binding.mapView.overlays.add(this)
            }
        }
        childMarker?.apply {
            position = point
            icon = ContextCompat.getDrawable(
                requireContext(),
                if (online) R.drawable.ic_child_marker else R.drawable.ic_child_marker_offline
            )
            snippet = if (online) "Online" else "Offline"
        }
        binding.mapView.invalidate()
    }

    private fun updateTrail(trail: List<GeoPoint>) {
        if (trail.size < 2) return
        binding.mapView.overlays.remove(trailPolyline)
        trailPolyline = Polyline(binding.mapView).apply {
            setPoints(trail)
            outlinePaint.color = Color.argb(180, 108, 99, 255)
            outlinePaint.strokeWidth = 8f
            isEnabled = showTrail
        }
        binding.mapView.overlays.add(0, trailPolyline)
        binding.mapView.invalidate()
    }

    private fun updateFenceOverlay(poly: List<GeoPoint>) {
        binding.mapView.overlays.remove(fencePolygon)
        if (poly.size < 3) { binding.mapView.invalidate(); return }

        fencePolygon = Polygon(binding.mapView).apply {
            points = poly + poly.first() // Close the polygon
            fillColor  = Color.argb(40, 108, 99, 255)
            strokeColor = Color.argb(220, 108, 99, 255)
            strokeWidth = 6f
        }
        binding.mapView.overlays.add(0, fencePolygon)
        binding.mapView.invalidate()
    }

    private fun updateFenceColor(status: FenceStatus) {
        fencePolygon ?: return
        val (fill, stroke) = when (status) {
            FenceStatus.INSIDE  -> Color.argb(40, 0, 180, 80) to Color.argb(220, 0, 150, 60)
            FenceStatus.OUTSIDE -> Color.argb(50, 220, 50, 50) to Color.argb(220, 200, 30, 30)
            FenceStatus.UNKNOWN -> Color.argb(40, 108, 99, 255) to Color.argb(220, 108, 99, 255)
        }
        fencePolygon?.fillColor  = fill
        fencePolygon?.strokeColor = stroke
        binding.mapView.invalidate()
    }

    private fun updateStatusBar(speed: Double, sats: Int, online: Boolean) {
        binding.tvSpeed.text = "%.1f km/h".format(speed)
        binding.tvSats.text  = "$sats sats"
        binding.tvOnline.text = if (online) "● Live" else "● Offline"
        binding.tvOnline.setTextColor(
            requireContext().getColor(if (online) R.color.green else R.color.red)
        )
    }

    private fun updateStatusBanner(status: FenceStatus) {
        when (status) {
            FenceStatus.OUTSIDE -> {
                binding.bannerAlert.visibility = View.VISIBLE
                binding.bannerAlert.text = "⚠️ CHILD IS OUTSIDE SAFE ZONE"
                binding.bannerAlert.setBackgroundColor(
                    requireContext().getColor(R.color.red)
                )
            }
            FenceStatus.INSIDE -> {
                binding.bannerAlert.visibility = View.VISIBLE
                binding.bannerAlert.text = "✅ Child is inside safe zone"
                binding.bannerAlert.setBackgroundColor(
                    requireContext().getColor(R.color.green)
                )
            }
            FenceStatus.UNKNOWN -> binding.bannerAlert.visibility = View.GONE
        }
    }

    private fun centerOnChild() {
        val loc = viewModel.location.value ?: return
        binding.mapView.controller.animateTo(GeoPoint(loc.latitude, loc.longitude))
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
