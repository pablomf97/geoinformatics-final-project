package com.figueroa.geofinalprgoject.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.figueroa.geofinalprgoject.R
import com.figueroa.geofinalprgoject.db.FirebaseAuth
import com.figueroa.geofinalprgoject.auth.login.LoginActivity
import com.figueroa.geofinalprgoject.auth.registration.RegisterActivity
import com.figueroa.geofinalprgoject.db.FirebaseDB
import com.figueroa.geofinalprgoject.user.GeoMarkerListActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, NavigationView.OnNavigationItemSelectedListener {

    // Firebase Auth
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    // Drawer
    private lateinit var drawer: DrawerLayout
    private lateinit var navView: NavigationView

    // FAB
    private lateinit var fab: FloatingActionButton

    // To handle permissions
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val tag: String = MapsActivity::class.java.simpleName
    private lateinit var grantPermissions: MaterialButton

    // Location
    private val requestCheckSettings: Int = 0x1
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest

    /**
     * This variable handles the the data
     * received by the fused location provider.
     */
    private val locationCallback = object : LocationCallback() {
        @SuppressLint("MissingPermission")
        override fun onLocationResult(locationResult: LocationResult?) {
            locationResult ?: return
            for (location in locationResult.locations) {
                Log.d(tag, location.toString())

                if (!map.isMyLocationEnabled) map.isMyLocationEnabled = true

                val center = LatLng(location.latitude, location.longitude)
                val cameraPosition = CameraPosition.Builder().target(center).zoom(17f).build()
                val cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition)
                map.animateCamera(cameraUpdate)
            }
        }
    }

    /**
     * Overriding Android's onCreate function.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // ...and Firebase Auth
        auth = FirebaseAuth(applicationContext)

        // Drawer & Navigation View
        drawer = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navView.setNavigationItemSelectedListener(this)

        // FAB
        fab = findViewById(R.id.fab)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Overriding Android's onStart function.
     */
    override fun onStart() {
        super.onStart()

        // Handling location permission
        checkPermissionsAndInit()
    }

    /**
     * Overriding Android's onStop function.
     */
    override fun onStop() {
        super.onStop()
        stopLocationUpdates()
    }

    /**
     * Overriding Android's onResume function.
     */
    override fun onResume() {
        super.onResume()
        setUpDrawer()
    }
    /**
     * Checks that all permissions were granted
     * and ask for them in case they were not.
     */
    private fun checkPermissionsAndInit() {
        when (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            /**
             * If permissions were already granted,
             * we can proceed to get the user location.
             */
            PackageManager.PERMISSION_GRANTED -> {
                initFusedLocationClient()
                createLocationRequest()
                checkSettingsAndStartLocationUpdates()

                // Making drawer visible
                drawer.visibility = View.VISIBLE

                // Adding a listener to the FAB and enabling it
                fab.setOnClickListener {
                    when(drawer.isDrawerOpen(GravityCompat.END)) {
                        true -> drawer.closeDrawer(GravityCompat.END, true)
                        false -> drawer.openDrawer(GravityCompat.END, true)
                    }
                }
                fab.show()
            }
            /**
             * If permissions are not granted, initialise the permission launcher
             * and the button to ask for it
             */
            PackageManager.PERMISSION_DENIED -> {
                // Making drawer invisible
                drawer.visibility = View.GONE
                // Disabling FAB
                fab.hide()

                // Init the permissions launcher
                initRequestPermissionLauncher()

                // Init the button and show it
                grantPermissions = findViewById(R.id.activity_maps_allow_location)
                grantPermissions.visibility = View.VISIBLE
                grantPermissions.setOnClickListener {
                    launchPermissionRequest()
                }
            }
        }
    }

    /**
     * Checks that location is enabled and then starts the location updates.
     */
    private fun checkSettingsAndStartLocationUpdates() {
        val locationSettingsRequest = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest).build()
        val settingsClient = LocationServices.getSettingsClient(this)

        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(
            locationSettingsRequest
        )

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MapsActivity,
                        requestCheckSettings
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    /**
     * Starts the location updates.
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        initFusedLocationClient()
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Stops the location updates.
     */
    private fun stopLocationUpdates() {
        if (this::fusedLocationClient.isInitialized)
            fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * If the fused location provider was not initialised
     * already, this function initialises it.
     */
    private fun initFusedLocationClient() {
        if (!this::fusedLocationClient.isInitialized)
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    /**
     * Creates a location request with the desired options.
     */
    private fun createLocationRequest() {
        if (!this::locationRequest.isInitialized) {
            locationRequest = LocationRequest.create().apply {
                interval = 4000
                fastestInterval = 2500
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            }
        }
    }

    /**
     * Initialises the ActivityResultLauncher that will ask for X permission. However,
     * in this project this will only ask for fine location permissions.
     */
    private fun initRequestPermissionLauncher() {
        if (!this::requestPermissionLauncher.isInitialized) {
            requestPermissionLauncher =
                registerForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted: Boolean ->
                    if (isGranted) {
                        // Hide button
                        grantPermissions.visibility = View.GONE
                        showSnackbar("Permission granted!")

                        checkPermissionsAndInit()
                    } else {
                        showSnackbar(
                            "The app needs to be granted location " +
                                    "permissions in order to work properly"
                        )
                    }
                }
        }
    }

    /**
     * Prompts the dialog that will ask the user to grant location permissions.
     *
     * Note that if the user denied twice, on devices using Android 11, the OS will block
     * asking for this permission again and the user will have to grant it from the
     * permissions window in the App Info.
     */
    private fun launchPermissionRequest() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    /**
     * Shows a simple snackbar
     *
     * @
     * @param message   Message to show in the snackbar
     */
    private fun showSnackbar(message: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.isBuildingsEnabled = true

        map.setOnCameraMoveStartedListener {
            // TODO: Get the geomarkers
        }

        val uiSettings = map.uiSettings

        uiSettings.setAllGesturesEnabled(false)
        uiSettings.isZoomControlsEnabled = false
        uiSettings.isMyLocationButtonEnabled = false
    }

    /**
     * Configures the drawer.
     */
    private fun setUpDrawer() {
        val currentUser = auth.currentUser()

        val menu = navView.menu

        val drawerLoginButton: MenuItem = menu.findItem(R.id.drawer_login)
        val drawerRegisterButton: MenuItem = menu.findItem(R.id.drawer_register)
        val drawerLogoutButton: MenuItem = menu.findItem(R.id.drawer_logout)

        val drawerPlacesGroup: MenuItem = menu.findItem(R.id.drawer_places_group)

        when (currentUser) {
            null -> {
                drawerLoginButton.isVisible = true
                drawerRegisterButton.isVisible = true

                drawerLogoutButton.isVisible = false
                drawerPlacesGroup.isVisible = false
            }
            else -> {
                userId = currentUser.uid

                drawerLoginButton.isVisible = false
                drawerRegisterButton.isVisible = false

                drawerLogoutButton.isVisible = true
                drawerPlacesGroup.isVisible = true
            }
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.drawer_login -> {
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
            }
            R.id.drawer_logout -> {
                auth.logout()
                if (auth.currentUser() == null) {
                    Toast.makeText(applicationContext,
                        "Logged out!",
                        Toast.LENGTH_SHORT).show()

                    // Restarting the activity
                    finish()
                    overridePendingTransition(0, 0);
                    startActivity(intent)
                    overridePendingTransition(0, 0);
                } else
                    Toast.makeText(applicationContext,
                        "Sorry, could not perform logout...",
                        Toast.LENGTH_SHORT).show()
            }
            R.id.drawer_register -> {
                val intent = Intent(applicationContext, RegisterActivity::class.java)
                startActivity(intent)
            }
            R.id.drawer_my_places -> {
                val intent = Intent(applicationContext, GeoMarkerListActivity::class.java).apply {
                    putExtra("userId", userId)
                }
                startActivity(intent)
            }
            R.id.drawer_create_place -> {
                // TODO: Create Place
            }
        }

        return false
    }
}