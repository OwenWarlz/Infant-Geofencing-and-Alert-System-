package com.infantgeofence.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.infantgeofence.R
import com.infantgeofence.data.model.FenceStatus
import com.infantgeofence.databinding.FragmentDashboardBinding
import com.infantgeofence.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchLocation()
            viewModel.fetchAlerts()
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.location.observe(viewLifecycleOwner) { loc ->
            if (loc == null || !loc.success) {
                binding.tvChildName.text = "No Data"
                binding.tvOnlineStatus.text = "Offline"
                binding.tvSatellites.text = "--"
                binding.tvSpeed.text = "--"
                binding.tvLatitude.text = "--"
                binding.tvLongitude.text = "--"
                binding.tvLastUpdate.text = "No data yet"
                return@observe
            }

            binding.tvChildName.text = loc.childName
            binding.tvOnlineStatus.text = if (loc.online) "● Online" else "● Offline"
            binding.tvOnlineStatus.setTextColor(
                requireContext().getColor(if (loc.online) R.color.green else R.color.red)
            )
            binding.tvSatellites.text = "${loc.satellites}"
            binding.tvSpeed.text = "%.1f km/h".format(loc.speedKmph)
            binding.tvLatitude.text = "%.6f".format(loc.latitude)
            binding.tvLongitude.text = "%.6f".format(loc.longitude)

            // Format timestamp
            runCatching {
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())
                sdf.timeZone = TimeZone.getTimeZone("UTC")
                val date = sdf.parse(loc.timestamp)
                val local = SimpleDateFormat("MMM d, yyyy  hh:mm:ss a", Locale.getDefault())
                binding.tvLastUpdate.text = date?.let { local.format(it) } ?: loc.timestamp
            }.onFailure { binding.tvLastUpdate.text = loc.timestamp }
        }

        viewModel.fenceStatus.observe(viewLifecycleOwner) { status ->
            val (iconRes, bgRes, title, subtitle) = when (status) {
                FenceStatus.INSIDE -> Quadruple(
                    R.drawable.ic_check_circle, R.drawable.bg_fence_inside,
                    "Inside Safe Zone ✅", "Child is within the geofenced area"
                )
                FenceStatus.OUTSIDE -> Quadruple(
                    R.drawable.ic_warning, R.drawable.bg_fence_outside,
                    "OUTSIDE Safe Zone ⚠️", "Child has left the safe boundary!"
                )
                FenceStatus.UNKNOWN -> Quadruple(
                    R.drawable.ic_fence, R.drawable.bg_fence_unknown,
                    "Geofence Not Set", "Go to Geofence tab to draw a safe zone"
                )
            }
            binding.ivFenceIcon.setImageResource(iconRes)
            binding.cardFenceStatus.setCardBackgroundColor(
                requireContext().getColor(
                    when (status) {
                        FenceStatus.INSIDE  -> R.color.fence_inside_bg
                        FenceStatus.OUTSIDE -> R.color.fence_outside_bg
                        FenceStatus.UNKNOWN -> R.color.fence_unknown_bg
                    }
                )
            )
            binding.tvFenceTitle.text = title
            binding.tvFenceSubtitle.text = subtitle

            binding.chipAlert.visibility =
                if (status == FenceStatus.OUTSIDE) View.VISIBLE else View.GONE
        }

        viewModel.polygon.observe(viewLifecycleOwner) { poly ->
            if (poly.isEmpty()) {
                binding.tvFenceTitle.text = "No Geofence Set"
                binding.tvFenceSubtitle.text = "Go to Geofence tab to draw a safe zone"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
