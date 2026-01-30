package com.example.simplelocationtracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.os.Looper
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.simplelocationtracker.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var currentMarkerItem: OverlayItem? = null
    private var currentGeoPoint: GeoPoint? = null
    private var itemizedOverlay: ItemizedIconOverlay<OverlayItem>? = null
    
    // Location request configuration
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        1000 // Update interval: 1 second
    ).apply {
        setMinUpdateIntervalMillis(500) // Fastest update interval
        setMaxUpdateAgeMillis(0) // No older updates
    }.build()
    
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results: Map<String, Boolean> ->
        val fine = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = results[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fine || coarse) {
            Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
            // Center quickly on last known location (if any)
            getLastLocationAndCenter()
            startLocationUpdates()
        } else {
            // Permission denied
            handlePermissionDenied()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Osmdroid configuration (required)
        Configuration.getInstance().apply {
            load(this@MainActivity, android.preference.PreferenceManager.getDefaultSharedPreferences(this@MainActivity))
            userAgentValue = "SimpleLocationTracker/1.0"
            // Set cache directory
            osmdroidBasePath = cacheDir
        }
        
        // Initialize View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Get MapView from layout
        mapView = binding.map
        try {
            mapView.setTileSource(TileSourceFactory.MAPNIK) // Use OpenStreetMap tiles
            mapView.setMultiTouchControls(true)
            mapView.zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
            
            // Set initial map center (San Francisco)
            val mapController = mapView.controller
            mapController.setZoom(17)
            val startPoint = GeoPoint(37.7749, -122.4194)
            mapController.setCenter(startPoint)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing map: ${e.message}", Toast.LENGTH_LONG).show()
        }
        
        // Initialize FusedLocationProviderClient
        fusedLocationClient = com.google.android.gms.location.LocationServices
            .getFusedLocationProviderClient(this)
        
        // Check permission and start location updates
        checkLocationPermission()
    }
    
    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
                getLastLocationAndCenter()
                startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                // Show explanation and request permission
                Toast.makeText(
                    this,
                    "Location permission is required to display your location on the map",
                    Toast.LENGTH_LONG
                ).show()
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            else -> {
                // Request permission (both fine and coarse)
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocationAndCenter() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    Log.d("MainActivity", "onLocationResult: ${location.latitude}, ${location.longitude} provider=${location.provider}")
                    // show one quick toast for the first fix
                    if (currentGeoPoint == null) {
                        Toast.makeText(this@MainActivity, "First location: ${String.format("%.4f", location.latitude)}, ${String.format("%.4f", location.longitude)}", Toast.LENGTH_SHORT).show()
                    }
                    mapView.controller.setCenter(geoPoint)

                    // Update marker quickly
                    mapView.overlays.remove(itemizedOverlay)
                    val overlayItems = ArrayList<OverlayItem>()
                    currentMarkerItem = OverlayItem(
                        "Your Location",
                        "Lat: ${String.format("%.4f", location.latitude)}\n" +
                                "Lng: ${String.format("%.4f", location.longitude)}",
                        geoPoint
                    )
                    currentGeoPoint = geoPoint
                    overlayItems.add(currentMarkerItem!!)
                    itemizedOverlay = ItemizedIconOverlay(overlayItems, null, this)
                    mapView.overlays.add(itemizedOverlay)
                    mapView.invalidate()
                } else {
                    Log.d("MainActivity", "No last known location available")
                }
            }.addOnFailureListener { e ->
                Log.w("MainActivity", "getLastLocation failed: ${e.message}")
            }
        } catch (e: SecurityException) {
            Log.w("MainActivity", "getLastLocation: security exception")
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
        }
        startActivity(intent)
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Create location callback for receiving location updates
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                
                val location = locationResult.lastLocation
                if (location != null) {
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    
                    // Update marker or create new one
                    if (currentMarkerItem == null) {
                        // First location - add marker
                        val overlayItems = ArrayList<OverlayItem>()
                        currentMarkerItem = OverlayItem(
                            "Your Location",
                            "Lat: ${String.format("%.4f", location.latitude)}\n" +
                            "Lng: ${String.format("%.4f", location.longitude)}",
                            geoPoint
                        )
                        currentGeoPoint = geoPoint
                        overlayItems.add(currentMarkerItem!!)
                        
                        // Create and add overlay
                        itemizedOverlay = ItemizedIconOverlay(
                            overlayItems,
                            null,
                            this@MainActivity
                        )
                        mapView.overlays.add(itemizedOverlay)
                        
                        // Center camera on initial location
                        mapView.controller.setCenter(geoPoint)
                    } else if (currentGeoPoint?.latitude != geoPoint.latitude || currentGeoPoint?.longitude != geoPoint.longitude) {
                        // Location changed - update marker position
                        mapView.overlays.remove(itemizedOverlay)
                        
                        val overlayItems = ArrayList<OverlayItem>()
                        currentMarkerItem = OverlayItem(
                            "Your Location",
                            "Lat: ${String.format("%.4f", location.latitude)}\n" +
                            "Lng: ${String.format("%.4f", location.longitude)}",
                            geoPoint
                        )
                        currentGeoPoint = geoPoint
                        overlayItems.add(currentMarkerItem!!)
                        
                        itemizedOverlay = ItemizedIconOverlay(
                            overlayItems,
                            null,
                            this@MainActivity
                        )
                        mapView.overlays.add(itemizedOverlay)
                        
                        // Move map to new location
                        mapView.controller.setCenter(geoPoint)
                    }
                    
                    mapView.invalidate() // Refresh map display
                }
            }
        }
        
        // Request location updates
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper() // ensure callback on main looper
            )
        } catch (e: SecurityException) {
            Log.w("MainActivity", "requestLocationUpdates: security exception: ${e.message}")
            handlePermissionDenied()
        }
    }
    
    private fun handlePermissionDenied() {
        Toast.makeText(
            this,
            "Location permission is denied. Cannot display your location.",
            Toast.LENGTH_LONG
        ).show()
        
        // Set a default location on the map (San Francisco)
        val defaultLocation = GeoPoint(37.7749, -122.4194)
        mapView.controller.setCenter(defaultLocation)
        
        // Add marker at default location
        val overlayItems = ArrayList<OverlayItem>()
        overlayItems.add(
            OverlayItem(
                "Location permission denied",
                "Enable location permission to see your live location",
                defaultLocation
            )
        )
        itemizedOverlay = ItemizedIconOverlay(overlayItems, null, this)
        mapView.overlays.add(itemizedOverlay)
        mapView.invalidate()
    }
    
    override fun onPause() {
        super.onPause()
        // Stop location updates when activity is paused to save battery
        stopLocationUpdates()
        mapView.onPause()
    }
    
    override fun onResume() {
        super.onResume()
        mapView.onResume()
        
        // Resume location updates when activity is resumed
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationUpdates()
        }
    }
    
    private fun stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback!!)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        mapView.onDetach()
    }
}