// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.hardware.GeomagneticField
import android.location.LocationManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.MizrachInfo
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.hasFineLocationPermission
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.openAppSettings

@Composable
fun MizrachScreen(
    modifier: Modifier = Modifier,
    viewModel: MizrachViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    var hasPreciseLocationPermission by remember { mutableStateOf(context.hasFineLocationPermission()) }
    var preciseUpgradeRequested by remember { mutableStateOf(false) }
    var locationServicesEnabled by remember { mutableStateOf(context.isLocationServicesEnabled()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compassLocationTrust = when {
        uiState.compassLocationTrust == CompassLocationTrust.DeveloperOverride ->
            CompassLocationTrust.DeveloperOverride
        uiState.hasCurrentLocation && !hasPreciseLocationPermission ->
            CompassLocationTrust.NeedsPrecisePermission
        uiState.compassLocationTrust == CompassLocationTrust.NeedsPrecisePermission ->
            CompassLocationTrust.NeedsBetterFix
        else -> uiState.compassLocationTrust
    }
    val mizrach = uiState.mizrachInfo
    val magneticDeclinationDegrees = remember(mizrach.fromLatitude, mizrach.fromLongitude, mizrach.fromElevationMeters) {
        GeomagneticField(
            mizrach.fromLatitude.toFloat(),
            mizrach.fromLongitude.toFloat(),
            mizrach.fromElevationMeters.toFloat(),
            System.currentTimeMillis(),
        ).declination
    }
    val compassSensorState = rememberCompassSensorState(
        enabled = hasLocationPermission && uiState.hasCurrentLocation,
        magneticDeclinationDegrees = magneticDeclinationDegrees,
        diagnosticsEnabled = uiState.compassMonitoringEnabled,
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasPreciseLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val wasPreciseUpgrade = preciseUpgradeRequested
        preciseUpgradeRequested = false
        if (hasLocationPermission) {
            viewModel.refreshCurrentLocation()
            if (wasPreciseUpgrade && !hasPreciseLocationPermission && !context.canAskLocationPermission()) {
                context.openAppSettings()
            }
        } else if (!context.canAskLocationPermission()) {
            // Permission is permanently denied ("Don't allow" chosen before), so the system dialog
            // won't appear again — the only way to grant it is from the app's settings page.
            context.openAppSettings()
        }
    }

    // Try for a fix whenever we have both permission and the system location toggle on; without
    // either, a fix can never arrive, so we prompt instead of waiting.
    LaunchedEffect(
        hasLocationPermission,
        locationServicesEnabled,
        uiState.hasCurrentLocation,
        compassLocationTrust,
    ) {
        if (hasLocationPermission && locationServicesEnabled &&
            (!uiState.hasCurrentLocation ||
                compassLocationTrust == CompassLocationTrust.NeedsBetterFix)
        ) {
            viewModel.refreshCurrentLocation()
        }
    }

    // Re-check on every resume so a change made in system Settings (granting permission or turning
    // location on) takes effect without recreating the process.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context, viewModel, compassLocationTrust) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasLocationPermission()
                hasPreciseLocationPermission = context.hasFineLocationPermission()
                locationServicesEnabled = context.isLocationServicesEnabled()
                if (hasLocationPermission && locationServicesEnabled &&
                    compassLocationTrust == CompassLocationTrust.NeedsBetterFix
                ) {
                    viewModel.refreshCurrentLocation()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Listen for the system location switch flipping while we're on screen (e.g. toggled from the
    // Quick Settings shade, which doesn't pause the activity). This updates the state live so the
    // compass appears the moment location is turned on, without a manual refresh — and drops the
    // old fix the moment it turns off (the app-wide "never remember a past location" policy is
    // otherwise only enforced on the next ON_START).
    DisposableEffect(context, viewModel) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                hasLocationPermission = context.hasLocationPermission()
                locationServicesEnabled = context.isLocationServicesEnabled()
                if (!locationServicesEnabled) viewModel.useJerusalemFallback()
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    MizrachContent(
        mizrach = mizrach,
        hasLocationPermission = hasLocationPermission,
        locationServicesEnabled = locationServicesEnabled,
        hasCurrentLocation = uiState.hasCurrentLocation,
        compassLocationTrust = compassLocationTrust,
        compassMonitoringEnabled = uiState.compassMonitoringEnabled,
        compassSensorState = compassSensorState,
        onRequestLocation = {
            preciseUpgradeRequested = false
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onRequestPreciseLocation = {
            preciseUpgradeRequested = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onOpenLocationSettings = {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } catch (_: ActivityNotFoundException) {
                // Some devices lack the location-settings activity; fall back to app settings.
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        },
        onRetryLocation = viewModel::refreshCurrentLocation,
        modifier = modifier,
    )
}

private fun Context.findActivity(): Activity? {
    var context: Context? = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

/**
 * Whether the system permission dialog can still be shown. False means the user permanently denied
 * location (chose "Don't allow"), so a launch() no-ops and we must route to app settings instead.
 */
private fun Context.canAskLocationPermission(): Boolean {
    val activity = findActivity() ?: return true
    return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION) ||
        ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION)
}

@Composable
private fun MizrachContent(
    mizrach: MizrachInfo,
    hasLocationPermission: Boolean,
    locationServicesEnabled: Boolean,
    hasCurrentLocation: Boolean,
    compassLocationTrust: CompassLocationTrust,
    compassMonitoringEnabled: Boolean,
    compassSensorState: State<CompassSensorState>,
    onRequestLocation: () -> Unit,
    onRequestPreciseLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
    onRetryLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!hasLocationPermission) {
                item {
                    LocationPermissionCard(onRequestLocation = onRequestLocation)
                }
            } else if (!hasCurrentLocation) {
                item {
                    if (!locationServicesEnabled) {
                        // Permission is granted but the system location toggle is off, so no fix
                        // will ever arrive — send the user to turn it on instead of waiting.
                        EnableLocationServicesCard(onOpenLocationSettings = onOpenLocationSettings)
                    } else {
                        Text(
                            text = stringResource(R.string.mizrach_waiting_location),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (hasCurrentLocation) {
                item {
                    MizrachHeader(mizrach = mizrach)
                }
                item {
                    CompassCard(
                        mizrach = mizrach,
                        locationTrust = compassLocationTrust,
                        compassSensorState = compassSensorState,
                        onRequestPreciseLocation = onRequestPreciseLocation,
                        onRetryLocation = onRetryLocation,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (compassMonitoringEnabled) {
                    item {
                        CompassDiagnosticsCard(
                            compassSensorState = compassSensorState,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationPermissionCard(
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mizrach_need_location),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRequestLocation) {
            Text(stringResource(R.string.mizrach_allow_location))
        }
    }
}

@Composable
private fun EnableLocationServicesCard(
    onOpenLocationSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.mizrach_location_off),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onOpenLocationSettings) {
            Text(stringResource(R.string.mizrach_turn_on_location))
        }
    }
}

@Composable
private fun MizrachHeader(mizrach: MizrachInfo, modifier: Modifier = Modifier) {
    // Label the source honestly: a fresh device fix, Jerusalem (the fallback), or a named place
    // (dev preset). The Jerusalem fallback is coloured red so it's obvious it isn't where you are.
    val source = locationSourceForName(mizrach.fromLocationName)
    val fromText = when (source) {
        LocationSource.CurrentFix -> stringResource(R.string.mizrach_from_current)
        LocationSource.Jerusalem -> stringResource(R.string.mizrach_from_jerusalem)
        LocationSource.Named -> stringResource(R.string.mizrach_from, mizrach.fromLocationName)
    }
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = fromText,
            style = MaterialTheme.typography.bodyLarge,
            color = if (source == LocationSource.Jerusalem) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}
