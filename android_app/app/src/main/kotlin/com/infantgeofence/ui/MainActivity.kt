package com.infantgeofence.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.infantgeofence.R
import com.infantgeofence.databinding.ActivityMainBinding
import com.infantgeofence.data.model.FenceStatus
import com.infantgeofence.service.GeofenceMonitorService
import com.infantgeofence.util.GeofencePrefs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handle results if needed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Navigation
        val host = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = host.navController
        binding.bottomNav.setupWithNavController(navController)

        // Load saved geofence
        val savedPolygon = GeofencePrefs.loadPolygon(this, com.infantgeofence.InfantGeoFenceApp.DEVICE_ID)
        val savedName    = GeofencePrefs.loadFenceName(this, com.infantgeofence.InfantGeoFenceApp.DEVICE_ID)
        if (savedPolygon.isNotEmpty()) {
            viewModel.setPolygon(savedPolygon, savedName)
        }

        // Badge on alerts tab
        viewModel.unreadCount.observe(this) { count ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.alertsFragment)
            if (count > 0) { badge.isVisible = true; badge.number = count }
            else badge.isVisible = false
        }

        // Fence status color on bottom nav
        viewModel.fenceStatus.observe(this) { status ->
            val badge = binding.bottomNav.getOrCreateBadge(R.id.mapFragment)
            badge.isVisible = status == FenceStatus.OUTSIDE
        }

        // Start polling
        viewModel.startPolling()

        // Request permissions
        requestPermissions()

        // Handle navigation intent
        intent?.getStringExtra("nav_to")?.let { dest ->
            if (dest == "alerts") navController.navigate(R.id.alertsFragment)
        }
    }

    private fun requestPermissions() {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray())
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopPolling()
    }
}
