// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Singleton
class AndroidCurrentLocationRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val appScope: CoroutineScope,
    private val developerOverrides: DeveloperOverridesRepository,
) : CurrentLocationRepository {
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())
    // Plain preferences, deliberately: only the DataStore is backed up (see backup_rules.xml), so the
    // remembered fix never leaves the device.
    private val rememberedFixPrefs = context.getSharedPreferences(RememberedFixPrefs, Context.MODE_PRIVATE)
    private val _currentLocationFix = MutableStateFlow<CurrentLocationFix?>(null)
    private var bestPublishedDeviceLocation: Location? = null
    private var activeLocationListener: LocationListener? = null
    private var activeTimeout: Runnable? = null

    // Written from the location callback (main looper) and read under the
    // requestSingleUpdate monitor; @Volatile guarantees cross-thread visibility.
    @Volatile
    private var lastSuccessfulFreshLocationElapsed = 0L

    /**
     * A fix carries the zone it was taken in ([toJewishLocation] stamps `ZoneId.systemDefault()`),
     * and every zman is computed and displayed in that zone. Changing the device's time zone or
     * clock therefore has to invalidate the stamp — otherwise a screen left open, or a stationary
     * phone whose next fix is not "better" than the published one, keeps calculating in the old
     * zone indefinitely.
     */
    private val timeSettingsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // The framework clears its own TimeZone.getDefault() cache from this same broadcast and
            // the order against app receivers is not defined, so re-stamp on the next main-looper
            // pass — by then the new default is in place.
            mainHandler.post(::republishWithCurrentZone)
            refreshCurrentLocation(force = true)
        }
    }

    init {
        ContextCompat.registerReceiver(
            context,
            timeSettingsReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    /**
     * Re-publishes the current fix so it picks up the device's zone as it is now. Unconditional on
     * purpose: [publishDeviceLocationIfBetter] would reject the same coordinates as "not better",
     * which is exactly the case this exists to handle.
     */
    @Synchronized
    private fun republishWithCurrentZone() {
        val published = bestPublishedDeviceLocation ?: return
        _currentLocationFix.value = published.toCurrentLocationFix(context.hasFineLocationPermission())
    }

    // The location seen by the rest of the app: the developer-pinned location when one is set,
    // otherwise the real device location. Centralizing it here means both ViewModels and the
    // status-icon service honor the override without changes.
    override val currentLocationState: StateFlow<CurrentLocationState> =
        combine(developerOverrides.state, _currentLocationFix) { overrides, deviceFix ->
            val override = overrides.overrideLocation
            if (override != null) {
                CurrentLocationState(location = override, isDeveloperOverride = true)
            } else {
                CurrentLocationState(location = deviceFix?.location, compassFix = deviceFix)
            }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = developerOverrides.overrideLocation?.let { CurrentLocationState(location = it) }
                ?: CurrentLocationState(),
        )

    override val currentLocation: StateFlow<JewishLocation?> = currentLocationState
        .map { it.location }
        .stateIn(
            scope = appScope,
            started = SharingStarted.Eagerly,
            initialValue = currentLocationState.value.location,
        )

    /**
     * Pull a location now. Called only when we have permission and location services are on, so the
     * OS's cached fix is published immediately (zmanim compute without waiting — a coarse, slightly
     * old position is fine for zmanim) and a fresh fix is then requested to refine it. Both are your
     * current location, so there's no visible label flip.
     */
    override fun refreshCurrentLocation(force: Boolean) {
        bestLastKnownLocation()?.let(::publishDeviceLocationIfBetter)
        requestSingleUpdate(force)
    }

    /**
     * Drop any device fix so the app falls back to Jerusalem, and forget the one remembered for
     * background refreshes. Called when we can't get a location (permission denied, or the location
     * switch is off) — the app keeps no location it is no longer allowed to use.
     */
    @Synchronized
    override fun useJerusalemFallback() {
        activeLocationListener?.let(::finishLocationRequest)
        bestPublishedDeviceLocation = null
        lastSuccessfulFreshLocationElapsed = 0L
        _currentLocationFix.value = null
        forgetRememberedFix()
    }

    override fun currentLocationOrDefault(): JewishLocation =
        developerOverrides.overrideLocation
            ?: currentLocation.value
            ?: getLastKnownJewishLocation()
            ?: rememberedLocation()
            ?: defaultJerusalemLocation

    /**
     * The last device fix the app was allowed to take, kept across process death. Location is
     * foreground-only for this app, so with no screen showing Android hands the process neither a
     * fresh fix nor its cached one — a widget or the date icon refreshing in the background would
     * otherwise fall back to Jerusalem and compute a day that is not the user's. Zoned to the device's
     * current zone like every fix, and forgotten the moment the app is told to stop using location
     * ([useJerusalemFallback]), so it never outlives the permission it was taken under.
     */
    private fun rememberedLocation(): JewishLocation? {
        if (!rememberedFixPrefs.contains(RememberedLatitude)) return null
        return JewishLocation(
            name = CurrentLocationName,
            latitude = Double.fromBits(rememberedFixPrefs.getLong(RememberedLatitude, 0L)),
            longitude = Double.fromBits(rememberedFixPrefs.getLong(RememberedLongitude, 0L)),
            elevationMeters = Double.fromBits(rememberedFixPrefs.getLong(RememberedElevation, 0L)),
            zoneId = ZoneId.systemDefault(),
        )
    }

    private fun rememberFix(location: Location) {
        rememberedFixPrefs.edit {
            putLong(RememberedLatitude, location.latitude.toBits())
            putLong(RememberedLongitude, location.longitude.toBits())
            putLong(RememberedElevation, (if (location.hasAltitude()) location.altitude else 0.0).toBits())
        }
    }

    private fun forgetRememberedFix() {
        rememberedFixPrefs.edit { clear() }
    }

    /** The OS's best cached fix, for background callers that need a location before a refresh. */
    private fun getLastKnownJewishLocation(): JewishLocation? =
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
    private fun requestSingleUpdate(force: Boolean) {
        if (!context.hasLocationPermission()) return
        val now = SystemClock.elapsedRealtime()
        if (activeLocationListener != null) return
        if (!force &&
            _currentLocationFix.value != null &&
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
        _currentLocationFix.value = candidate.toCurrentLocationFix(context.hasFineLocationPermission())
        rememberFix(candidate)
        return true
    }

    @Synchronized
    private fun acceptLocationUpdate(listener: LocationListener, location: Location) {
        // removeUpdates() cannot retract a callback that is already queued. Check identity while
        // holding the same monitor as cancellation so an obsolete request cannot restore a fix.
        if (activeLocationListener !== listener || !publishDeviceLocationIfBetter(location)) return
        val completionAccuracyMeters = if (context.hasFineLocationPermission()) {
            AcceptablePreciseLocationAccuracyMeters
        } else {
            AcceptableCoarseLocationAccuracyMeters
        }
        if (location.hasAccuracy() &&
            location.accuracy.isFinite() &&
            location.accuracy in 0f..completionAccuracyMeters
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
        const val AcceptablePreciseLocationAccuracyMeters = 100f
        const val AcceptableCoarseLocationAccuracyMeters = 5_000f
        const val RememberedFixPrefs = "remembered_location_fix"
        const val RememberedLatitude = "latitude"
        const val RememberedLongitude = "longitude"
        const val RememberedElevation = "elevation_meters"
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

private fun Location.toCurrentLocationFix(hasPrecisePermission: Boolean): CurrentLocationFix = CurrentLocationFix(
    location = toJewishLocation(),
    horizontalAccuracyMeters = accuracy.takeIf { hasAccuracy() && it.isFinite() && it >= 0f },
    elapsedRealtimeNanos = elapsedRealtimeNanos,
    wallTimeMillis = time,
    hasPrecisePermission = hasPrecisePermission,
)
