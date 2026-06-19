package com.noamtu.jewishday.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.defaultJerusalemLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class CurrentLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _currentLocation = MutableStateFlow<JewishLocation?>(null)
    private var activeLocationListener: LocationListener? = null
    private var activeTimeout: Runnable? = null

    // Written from the location callback (main looper) and read under the
    // requestSingleUpdate monitor; @Volatile guarantees cross-thread visibility.
    @Volatile
    private var lastSuccessfulFreshLocationElapsed = 0L
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

    /**
     * Requests a fresh fix and suspends until one (or any cached value) is available,
     * falling back to last-known/Jerusalem after [timeoutMillis]. Intended for
     * background workers, which must not compute zmanim against a default location
     * just because the in-memory state was empty after process restart.
     */
    suspend fun awaitCurrentLocation(timeoutMillis: Long = AwaitLocationTimeoutMillis): JewishLocation {
        // Without location permission a fresh fix can never arrive, so don't block for the full
        // timeout — fall back immediately to last-known/Jerusalem so callers (e.g. the date-icon
        // service) stay responsive and still compute a correct date.
        if (!context.hasLocationPermission()) return currentLocationOrDefault()
        refreshCurrentLocation()
        return withTimeoutOrNull(timeoutMillis) { currentLocation.filterNotNull().first() }
            ?: currentLocationOrDefault()
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownJewishLocation(): JewishLocation? {
        if (!context.hasLocationPermission()) return null

        return enabledProviders()
            .mapNotNull(locationManager::getLastKnownLocation)
            .maxByOrNull(Location::getTime)
            ?.toJewishLocation()
    }

    @SuppressLint("MissingPermission")
    @Synchronized
    private fun requestSingleUpdate() {
        if (!context.hasLocationPermission()) return
        val now = SystemClock.elapsedRealtime()
        if (activeLocationListener != null) return
        if (
            _currentLocation.value != null &&
            lastSuccessfulFreshLocationElapsed > 0L &&
            now - lastSuccessfulFreshLocationElapsed < FreshLocationRequestThrottleMillis
        ) return

        val providers = enabledProviders()
        if (providers.isEmpty()) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                _currentLocation.value = location.toJewishLocation()
                lastSuccessfulFreshLocationElapsed = SystemClock.elapsedRealtime()
                finishLocationRequest(this)
            }
        }
        val timeout = Runnable { finishLocationRequest(listener) }
        activeLocationListener = listener
        activeTimeout = timeout
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
        }
        mainHandler.postDelayed(timeout, SingleUpdateTimeoutMillis)
    }

    @Synchronized
    private fun finishLocationRequest(listener: LocationListener) {
        if (activeLocationListener !== listener) return
        locationManager.removeUpdates(listener)
        activeTimeout?.let(mainHandler::removeCallbacks)
        activeLocationListener = null
        activeTimeout = null
    }

    private fun enabledProviders(): List<String> =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter(locationManager::isProviderEnabled)

    private companion object {
        const val SingleUpdateTimeoutMillis = 30_000L
        const val FreshLocationRequestThrottleMillis = 5 * 60 * 1_000L
        const val AwaitLocationTimeoutMillis = 10_000L
    }
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
