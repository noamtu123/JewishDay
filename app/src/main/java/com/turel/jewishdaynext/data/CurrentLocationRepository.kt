package com.turel.jewishdaynext.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import com.turel.jewishdaynext.model.JewishLocation
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class CurrentLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val _currentLocation = MutableStateFlow<JewishLocation?>(null)
    val currentLocation: StateFlow<JewishLocation?> = _currentLocation.asStateFlow()

    fun refreshCurrentLocation() {
        val latest = getLastKnownJewishLocation()
        if (latest != null) {
            _currentLocation.value = latest
        }
        requestSingleUpdate()
    }

    fun currentLocationOrDefault(): JewishLocation =
        currentLocation.value ?: getLastKnownJewishLocation() ?: defaultJerusalemLocation

    @SuppressLint("MissingPermission")
    fun getLastKnownJewishLocation(): JewishLocation? {
        if (!context.hasLocationPermission()) return null

        return enabledProviders()
            .mapNotNull(locationManager::getLastKnownLocation)
            .maxByOrNull(Location::getTime)
            ?.toJewishLocation()
    }

    @SuppressLint("MissingPermission")
    private fun requestSingleUpdate() {
        if (!context.hasLocationPermission()) return

        val providers = enabledProviders()
        if (providers.isEmpty()) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _currentLocation.value = location.toJewishLocation()
                locationManager.removeUpdates(this)
            }
        }
        providers.forEach { provider ->
            locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    private fun enabledProviders(): List<String> =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(locationManager::isProviderEnabled)
}

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun Location.toJewishLocation(name: String = "Current location"): JewishLocation = JewishLocation(
    name = name,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = if (hasAltitude()) altitude else 0.0,
    zoneId = ZoneId.systemDefault(),
)
