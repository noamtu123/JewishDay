// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import com.noamtu.jewishday.di.ApplicationScope
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.defaultJerusalemLocation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class CurrentLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val developerOverrides: DeveloperOverridesRepository,
) {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _currentLocation = MutableStateFlow<JewishLocation?>(null)
    private var bestPublishedDeviceLocation: Location? = null
    private var activeLocationListener: LocationListener? = null
    private var activeTimeout: Runnable? = null

    // Written from the location callback (main looper) and read under the
    // requestSingleUpdate monitor; @Volatile guarantees cross-thread visibility.
    @Volatile
    private var lastSuccessfulFreshLocationElapsed = 0L

    // The location seen by the rest of the app: the developer-pinned location when one is set,
    // otherwise the real device location. Centralizing it here means both ViewModels and the
    // status-icon service honor the override without changes.
    val currentLocation: StateFlow<JewishLocation?> =
        combine(developerOverrides.state, _currentLocation) { overrides, deviceLocation ->
            overrides.overrideLocation ?: deviceLocation
        }.stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = developerOverrides.overrideLocation ?: _currentLocation.value,
        )

    /**
     * Pull a location now. Called only when we have permission and location services are on, so the
     * OS's cached fix is published immediately (zmanim compute without waiting — a coarse, slightly
     * old position is fine for zmanim) and a fresh fix is then requested to refine it. Both are your
     * current location, so there's no visible label flip.
     */
    fun refreshCurrentLocation() {
        bestLastKnownLocation()?.let(::publishDeviceLocationIfBetter)
        requestSingleUpdate()
    }

    /**
     * Drop any device fix so the app falls back to Jerusalem. Called when we can't get a location
     * (permission denied, or the location switch is off) — the app never remembers a past location.
     */
    @Synchronized
    fun useJerusalemFallback() {
        activeLocationListener?.let(::finishLocationRequest)
        bestPublishedDeviceLocation = null
        lastSuccessfulFreshLocationElapsed = 0L
        _currentLocation.value = null
    }

    fun currentLocationOrDefault(): JewishLocation =
        developerOverrides.overrideLocation
            ?: currentLocation.value
            ?: getLastKnownJewishLocation()
            ?: defaultJerusalemLocation

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

    /** The OS's best cached fix, for background callers that need a location before a refresh. */
    fun getLastKnownJewishLocation(): JewishLocation? =
        bestLastKnownLocation()?.toJewishLocation()

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(): Location? {
        if (!context.hasLocationPermission()) return null

        return enabledProviders()
            .mapNotNull(locationManager::getLastKnownLocation)
            .reduceOrNull { best, candidate ->
                if (isBetterLocationFix(candidate, best)) {
                    candidate
                } else {
                    best
                }
            }
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
                acceptLocationUpdate(this, location)
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
    private fun publishDeviceLocationIfBetter(candidate: Location): Boolean {
        val current = bestPublishedDeviceLocation
        if (current != null && !isBetterLocationFix(candidate, current)) return false

        bestPublishedDeviceLocation = Location(candidate)
        _currentLocation.value = candidate.toJewishLocation()
        return true
    }

    @Synchronized
    private fun acceptLocationUpdate(listener: LocationListener, location: Location) {
        // removeUpdates() cannot retract a callback that is already queued. Check identity while
        // holding the same monitor as cancellation so an obsolete request cannot restore a fix.
        if (activeLocationListener !== listener || !publishDeviceLocationIfBetter(location)) return
        if (location.hasAccuracy() &&
            location.accuracy.isFinite() &&
            location.accuracy in 0f..AcceptableLocationAccuracyMeters
        ) {
            lastSuccessfulFreshLocationElapsed = SystemClock.elapsedRealtime()
            finishLocationRequest(listener)
        }
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
        const val AcceptableLocationAccuracyMeters = 5_000f
    }
}

private fun isBetterLocationFix(candidate: Location, current: Location): Boolean {
    // Location.time follows the user-adjustable wall clock. Prefer the elapsed-realtime stamps so
    // a clock correction cannot make every new live fix appear older than a cached one.
    val candidateElapsedNanos = candidate.elapsedRealtimeNanos
    val currentElapsedNanos = current.elapsedRealtimeNanos
    val useElapsedTime = candidateElapsedNanos > 0L && currentElapsedNanos > 0L
    return isBetterLocationFix(
        candidateTimeMillis = if (useElapsedTime) candidateElapsedNanos / 1_000_000L else candidate.time,
        candidateAccuracyMeters = candidate.accuracy.takeIf { candidate.hasAccuracy() },
        currentTimeMillis = if (useElapsedTime) currentElapsedNanos / 1_000_000L else current.time,
        currentAccuracyMeters = current.accuracy.takeIf { current.hasAccuracy() },
    )
}

/**
 * Accuracy-aware one-shot fix comparison adapted from Android's location guidance. A fix more than
 * two minutes newer wins; one more than two minutes older loses. Within that window, prefer better
 * accuracy, or a newer fix that is not dramatically less accurate.
 */
fun isBetterLocationFix(
    candidateTimeMillis: Long,
    candidateAccuracyMeters: Float?,
    currentTimeMillis: Long,
    currentAccuracyMeters: Float?,
): Boolean {
    val timeDelta = candidateTimeMillis - currentTimeMillis
    if (timeDelta > SignificantLocationTimeDeltaMillis) return true
    if (timeDelta < -SignificantLocationTimeDeltaMillis) return false

    val candidateAccuracy = candidateAccuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.POSITIVE_INFINITY
    val currentAccuracy = currentAccuracyMeters
        ?.takeIf { it.isFinite() && it >= 0f }
        ?: Float.POSITIVE_INFINITY
    if (candidateAccuracy < currentAccuracy) return true
    return timeDelta > 0L && candidateAccuracy <= currentAccuracy + MaximumAccuracyRegressionMeters
}

private const val SignificantLocationTimeDeltaMillis = 2 * 60 * 1_000L
private const val MaximumAccuracyRegressionMeters = 200f

fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

/**
 * Whether the device's location toggle is on. Permission can be granted while the system location
 * switch is off, in which case no fix will ever arrive and the caller should prompt the user to
 * enable location rather than wait forever.
 */
fun Context.isLocationServicesEnabled(): Boolean {
    val locationManager = getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        locationManager.isLocationEnabled
    } else {
        @Suppress("DEPRECATION")
        locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}

/**
 * Sentinel display name for a device-derived fix. The UI recognizes it to show a *localized*
 * "current location" label instead of this raw English string.
 */
const val CurrentLocationName = "Current location"

/** Where the location the app is using came from, so the UI can label it honestly. */
enum class LocationSource { CurrentFix, Jerusalem, Named }

/** Classifies a [JewishLocation] name into its [LocationSource] using the sentinel above. */
fun locationSourceForName(name: String): LocationSource = when (name) {
    CurrentLocationName -> LocationSource.CurrentFix
    defaultJerusalemLocation.name -> LocationSource.Jerusalem
    else -> LocationSource.Named
}

fun Location.toJewishLocation(name: String = CurrentLocationName): JewishLocation = JewishLocation(
    name = name,
    latitude = latitude,
    longitude = longitude,
    elevationMeters = if (hasAltitude()) altitude else 0.0,
    zoneId = ZoneId.systemDefault(),
)
