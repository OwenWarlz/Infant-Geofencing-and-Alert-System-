package com.infantgeofence.ui.alerts

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.infantgeofence.R
import com.infantgeofence.data.model.GeoAlert
import com.infantgeofence.databinding.FragmentAlertsBinding
import com.infantgeofence.databinding.ItemAlertBinding
import com.infantgeofence.ui.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

// ── AlertsFragment ────────────────────────────────────────────
class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: MainViewModel by activityViewModels()
    private val adapter = AlertsAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerAlerts.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerAlerts.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.fetchAlerts()
            binding.swipeRefresh.isRefreshing = false
        }

        binding.btnMarkRead.setOnClickListener {
            viewModel.markAlertsRead()
        }

        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            adapter.submitList(alerts)
            binding.emptyState.visibility = if (alerts.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerAlerts.visibility = if (alerts.isEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.unreadCount.observe(viewLifecycleOwner) { count ->
            binding.btnMarkRead.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.tvUnreadBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
            binding.tvUnreadBadge.text = "$count unread"
        }

        viewModel.fetchAlerts()
    }

    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}

// ── AlertsAdapter ─────────────────────────────────────────────
class AlertsAdapter : ListAdapter<GeoAlert, AlertsAdapter.ViewHolder>(DIFF) {

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<GeoAlert>() {
            override fun areItemsTheSame(a: GeoAlert, b: GeoAlert) = a.id == b.id
            override fun areContentsTheSame(a: GeoAlert, b: GeoAlert) = a == b
        }
    }

    inner class ViewHolder(val binding: ItemAlertBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        ItemAlertBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val alert = getItem(position)
        val ctx   = holder.itemView.context

        holder.binding.tvAlertType.text = alert.typeLabel

        val isCritical = alert.isCritical
        holder.binding.tvAlertType.setTextColor(
            ctx.getColor(if (isCritical) R.color.red else R.color.green)
        )
        holder.binding.ivAlertIcon.setImageResource(
            when (alert.alertType) {
                "outside"     -> R.drawable.ic_warning
                "inside"      -> R.drawable.ic_check_circle
                "sos"         -> R.drawable.ic_sos
                "low_battery" -> R.drawable.ic_battery_alert
                else          -> R.drawable.ic_info
            }
        )
        holder.binding.ivAlertIcon.setColorFilter(
            ctx.getColor(if (isCritical) R.color.red else R.color.green)
        )

        // Subtitle: child name + time
        val timeStr = runCatching {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val date = sdf.parse(alert.timestamp)
            SimpleDateFormat("MMM d  hh:mm a", Locale.getDefault()).format(date!!)
        }.getOrElse { alert.timestamp }
        holder.binding.tvAlertMeta.text = "${alert.childName} • $timeStr"

        // Unread highlight
        holder.binding.root.alpha = if (alert.isReadBool) 0.65f else 1.0f
        holder.binding.unreadDot.visibility =
            if (!alert.isReadBool) View.VISIBLE else View.GONE
    }
}
