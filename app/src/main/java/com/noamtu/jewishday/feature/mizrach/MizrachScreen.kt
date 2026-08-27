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
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.hasFineLocationPermission
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import com.noamtu.jewishday.model.AlignmentDirection
import com.noamtu.jewishday.model.AlignmentGate
import com.noamtu.jewishday.model.CompassDiagnostics
import com.noamtu.jewishday.model.MizrachInfo
import com.noamtu.jewishday.model.sensorStatusLabel
import com.noamtu.jewishday.model.targetDirectionOnScreen
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedString
import kotlin.math.cos
import kotlin.math.sin

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

private fun Context.openAppSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
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
                            text = localizedString(R.string.mizrach_waiting_location, R.string.mizrach_waiting_location_hebrew),
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
            text = localizedString(R.string.mizrach_need_location, R.string.mizrach_need_location_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onRequestLocation) {
            Text(localizedString(R.string.mizrach_allow_location, R.string.mizrach_allow_location_hebrew))
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
            text = localizedString(R.string.mizrach_location_off, R.string.mizrach_location_off_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(14.dp))
        Button(onClick = onOpenLocationSettings) {
            Text(localizedString(R.string.mizrach_turn_on_location, R.string.mizrach_turn_on_location_hebrew))
        }
    }
}

@Composable
private fun MizrachHeader(mizrach: MizrachInfo, modifier: Modifier = Modifier) {
    // Label the source honestly: a fresh device fix, Jerusalem (the fallback), or a named place
    // (dev preset). The Jerusalem fallback is coloured red so it's obvious it isn't where you are.
    val source = locationSourceForName(mizrach.fromLocationName)
    val fromText = when (source) {
        LocationSource.CurrentFix -> localizedString(R.string.mizrach_from_current, R.string.mizrach_from_current_hebrew)
        LocationSource.Jerusalem -> localizedString(R.string.mizrach_from_jerusalem, R.string.mizrach_from_jerusalem_hebrew)
        LocationSource.Named -> localizedString(R.string.mizrach_from, R.string.mizrach_from_hebrew, mizrach.fromLocationName)
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

@Composable
private fun CompassCard(
    mizrach: MizrachInfo,
    locationTrust: CompassLocationTrust,
    compassSensorState: State<CompassSensorState>,
    onRequestPreciseLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf: heading updates arrive up to 50×/s, but these only change
    // composition when the derived value actually flips.
    val locationTrusted = locationTrust == CompassLocationTrust.Trusted
    val hint by remember(compassSensorState, locationTrust) {
        derivedStateOf { compassSensorState.value.accuracyHint(locationTrust) }
    }
    val alignment = rememberAlignmentState(
        bearingDegrees = mizrach.bearingDegreesExact,
        locationTrusted = locationTrusted,
        compassSensorState = compassSensorState,
    )
    VibrateWhenAligned(aligned = alignment?.isAligned == true)

    InfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
    ) {
        CompassFace(
            bearingDegrees = mizrach.bearingDegrees,
            bearingDegreesExact = mizrach.bearingDegreesExact,
            locationTrusted = locationTrusted,
            sensorStateProvider = { compassSensorState.value },
            alignment = alignment,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = localizedString(R.string.mizrach_compass_caption, R.string.mizrach_compass_caption_hebrew),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
        hint?.let { (english, hebrew) ->
            Spacer(Modifier.height(8.dp))
            val message = localizedString(english, hebrew)
            // The unreliable warning leads with "Compass is unreliable right now"; emphasize that
            // clause (bold + error colour) so it reads as a stronger caution than low accuracy.
            val text = if (english == R.string.mizrach_compass_unreliable) {
                emphasizeLeadClause(message, MaterialTheme.colorScheme.error)
            } else {
                AnnotatedString(message)
            }
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        val locationAction = when (locationTrust) {
            CompassLocationTrust.NeedsPrecisePermission -> onRequestPreciseLocation
            CompassLocationTrust.NeedsBetterFix -> onRetryLocation
            CompassLocationTrust.Trusted,
            CompassLocationTrust.DeveloperOverride,
            -> null
        }
        locationAction?.let { action ->
            Spacer(Modifier.height(10.dp))
            Button(onClick = action, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                val label = if (locationTrust == CompassLocationTrust.NeedsPrecisePermission) {
                    localizedString(R.string.mizrach_allow_precise_location, R.string.mizrach_allow_precise_location_hebrew)
                } else {
                    localizedString(R.string.mizrach_retry_location, R.string.mizrach_retry_location_hebrew)
                }
                Text(label)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ValuePill(text = localizedString(R.string.mizrach_distance_km, R.string.mizrach_distance_km_hebrew, mizrach.distanceKm))
        }
    }
}

/**
 * Developer-only live overlay of the compass pipeline internals, shown when the hidden "Monitor
 * compass" switch is on. English-only, matching the rest of the developer tooling — it exposes the
 * active sensor source, each sensor's Android accuracy status and delivery rate, the fused
 * heading-error estimate, the raw vs. declination-corrected heading, and the derived quality
 * verdict, so magnetic problems can be diagnosed on-device from the platform's own signals.
 */
@Composable
private fun CompassDiagnosticsCard(
    compassSensorState: State<CompassSensorState>,
    modifier: Modifier = Modifier,
) {
    val diagnostics by remember(compassSensorState) {
        derivedStateOf { compassSensorState.value.diagnostics }
    }
    InfoCard(modifier = modifier) {
        Text(
            text = "Compass monitor (developer)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        val current = diagnostics
        if (current == null) {
            Text(
                text = "Waiting for the first sensor reading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@InfoCard
        }
        DiagnosticsRow("Active source", current.activeSource)
        DiagnosticsRow("Quality verdict", current.qualityVerdict())
        DiagnosticsRow("Heading (true)", current.trueHeadingDegrees.formatDegrees())
        DiagnosticsRow("Heading (magnetic)", current.magneticHeadingDegrees.formatDegrees())
        DiagnosticsRow("Declination", current.declinationDegrees.formatDegrees())
        DiagnosticsRow("Heading error (values[4])", current.headingErrorDegrees.formatDegreesOrUnknown())
        DiagnosticsRow("Rotation-vector status", sensorStatusLabel(current.fusedStatus))
        DiagnosticsRow("Accelerometer status", sensorStatusLabel(current.accelerometerStatus))
        DiagnosticsRow("Magnetometer status", sensorStatusLabel(current.magnetometerStatus))
        DiagnosticsRow("Rotation-vector rate", current.fusedRateHz.formatHz())
        DiagnosticsRow("Accelerometer rate", current.accelerometerRateHz.formatHz())
        DiagnosticsRow("Magnetometer rate", current.magnetometerRateHz.formatHz())
    }
}

private fun CompassDiagnostics.qualityVerdict(): String {
    val flags = buildList {
        if (unreliable) add("unreliable")
        if (needsCalibration) add("needs calibration")
        if (lowConfidence) add("low confidence")
        if (qualityPending) add("awaiting status")
    }
    return if (flags.isEmpty()) "OK (live)" else flags.joinToString(", ")
}

private fun Float?.formatDegrees(): String =
    if (this == null || !this.isFinite()) "—" else String.format(java.util.Locale.US, "%.1f°", this)

private fun Float.formatDegreesOrUnknown(): String =
    if (!isFinite()) "unknown" else String.format(java.util.Locale.US, "%.1f°", this)

private fun Float.formatHz(): String = String.format(java.util.Locale.US, "%.1f Hz", this)

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(150.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CompassFace(
    bearingDegrees: Int,
    bearingDegreesExact: Float,
    locationTrusted: Boolean,
    sensorStateProvider: () -> CompassSensorState,
    alignment: CompassAlignment?,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val alignedColor = MaterialTheme.colorScheme.tertiary
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    val isAligned = alignment?.isAligned == true

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val dialDescription = localizedString(
            R.string.mizrach_compass_dial_description,
            R.string.mizrach_compass_dial_description_hebrew,
            bearingDegrees,
        )
        // A Canvas carries no text, so without this the needle is invisible to TalkBack. The
        // description is keyed on the bearing only — the live heading is read in the draw phase on
        // purpose, and pulling it up here would recompose the screen at the sensor rate.
        Canvas(
            modifier = Modifier
                .size(240.dp)
                .semantics { contentDescription = dialDescription },
        ) {
            // Sensor state is read here, in the draw phase, so each sensor tick only
            // redraws the needle instead of recomposing the whole screen.
            val sensorState = sensorStateProvider()
            val heading = sensorState.headingDegrees
            // Do not draw an assumed-north needle while waiting for the first sensor reading: it
            // would point incorrectly and then jump. A device with no compass sensor keeps the
            // explicitly documented static-bearing fallback.
            val showNeedle = heading != null || !sensorState.sensorsAvailable
            val live = heading != null && !sensorState.hasLowAccuracy && locationTrusted
            val relativeDirectionDegrees = heading?.let { targetDirectionOnScreen(bearingDegreesExact, it) }
                ?: bearingDegreesExact
            val needleColor = if (isAligned) alignedColor else primary
            // Faded when the reading cannot be trusted. Kept apart from the color so it can be
            // applied to the needle as a whole rather than to each of its shapes.
            val needleAlpha = if (isAligned || live) 1f else FadedNeedleAlpha
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val needleRadius = radius - 48.dp.toPx()
            val bearingRadians = Math.toRadians((relativeDirectionDegrees - 90f).toDouble())
            val needleEnd = Offset(
                x = center.x + cos(bearingRadians).toFloat() * needleRadius,
                y = center.y + sin(bearingRadians).toFloat() * needleRadius,
            )

            drawCircle(color = surface, radius = radius, center = center)
            drawCircle(
                color = outline,
                radius = radius - 1.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx()),
            )
            if (showNeedle) {
                drawNeedle(
                    color = needleColor,
                    alpha = needleAlpha,
                    center = center,
                    tip = needleEnd,
                    strokeWidth = 8.dp.toPx(),
                    tipRadius = 9.dp.toPx(),
                    hubRadius = 5.dp.toPx(),
                )
            }
            drawTempleMarker(
                center = Offset(center.x, center.y - radius + 32.dp.toPx()),
                color = if (isAligned) alignedColor else primary,
                cutoutColor = surface,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.mizrach_degrees, bearingDegrees),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
            Text(
                text = localizedString(R.string.mizrach_toward_jerusalem, R.string.mizrach_toward_jerusalem_hebrew),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (alignment != null) {
                Spacer(Modifier.height(8.dp))
                // "Turn left" becoming "Facing Kodesh HaKodashim" is the whole point of the screen
                // for someone who cannot see the needle, so it is announced rather than merely
                // readable. Polite: it should not cut across what the user is already hearing.
                ValuePill(
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    text = alignment.label,
                    containerColor = if (isAligned) alignedColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isAligned) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun DrawScope.drawTempleMarker(
    center: Offset,
    color: Color,
    cutoutColor: Color,
) {
    val scale = 0.5.dp.toPx() * 0.94f
    val left = center.x - 72f * scale / 2f
    val top = center.y - 64f * scale / 2f

    fun rect(x: Float, y: Float, width: Float, height: Float): Rect = Rect(
        left = left + x * scale,
        top = top + y * scale,
        right = left + (x + width) * scale,
        bottom = top + (y + height) * scale,
    )

    fun offset(x: Float, y: Float): Offset = Offset(left + x * scale, top + y * scale)

    fun size(width: Float, height: Float): Size = Size(width * scale, height * scale)

    val gate = Path().apply {
        addRect(rect(x = 16f, y = 16f, width = 40f, height = 38f))
        addRect(rect(x = 2f, y = 28f, width = 12f, height = 26f))
        addRect(rect(x = 58f, y = 28f, width = 12f, height = 26f))
        addRect(rect(x = 16f, y = 4f, width = 9f, height = 14f))
        addRect(rect(x = 31.5f, y = 4f, width = 9f, height = 14f))
        addRect(rect(x = 47f, y = 4f, width = 9f, height = 14f))
    }
    drawPath(gate, color = color)

    drawRoundRect(
        color = cutoutColor,
        topLeft = offset(x = 31f, y = 34f),
        size = size(width = 10f, height = 20f),
        cornerRadius = CornerRadius(2f * scale, 2f * scale),
    )
    drawRoundRect(
        color = color,
        topLeft = offset(x = 0f, y = 56f),
        size = size(width = 72f, height = 8f),
        cornerRadius = CornerRadius(4f * scale, 4f * scale),
    )
}

/**
 * The needle: a shaft, a tip and a hub, drawn as one shape.
 *
 * A faded needle has to fade as a whole. Drawing the three parts each at [alpha] lets the shaft
 * show through the circles it runs under, and the overlaps read darker than the rest — so when it
 * is translucent they are composited together in one layer first, and that layer is faded.
 */
private fun DrawScope.drawNeedle(
    color: Color,
    alpha: Float,
    center: Offset,
    tip: Offset,
    strokeWidth: Float,
    tipRadius: Float,
    hubRadius: Float,
) {
    fun drawParts() {
        drawLine(color = color, start = center, end = tip, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawCircle(color = color, radius = tipRadius, center = tip)
        drawCircle(color = color, radius = hubRadius, center = center)
    }

    if (alpha >= 1f) {
        drawParts()
        return
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
        drawParts()
        canvas.restore()
    }
}

private const val FadedNeedleAlpha = 0.35f

private data class CompassAlignment(
    val isAligned: Boolean,
    val label: String,
)

/**
 * The accuracy message to show under the compass, as an English/Hebrew string-resource pair, or
 * null when the reading is healthy. A degraded reading always fades the needle and suppresses
 * guidance; the message distinguishes an *unreliable* reading (Android says the data can't be
 * trusted — a magnet/metal is interfering) from ordinary low accuracy that a figure-eight fixes.
 */
/**
 * Emphasizes the leading clause (everything before the "—") of a warning: bold and coloured, so
 * the headline caution stands out from the calmer instructions that follow. Falls back to a plain
 * annotated string when no separator is present.
 */
private fun emphasizeLeadClause(message: String, highlightColor: Color): AnnotatedString {
    val separatorIndex = message.indexOf('—')
    if (separatorIndex <= 0) return AnnotatedString(message)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
            append(message.substring(0, separatorIndex).trim())
        }
        append(' ')
        append(message.substring(separatorIndex))
    }
}

private fun CompassSensorState.accuracyHint(locationTrust: CompassLocationTrust): Pair<Int, Int>? = when {
    !sensorsAvailable ->
        R.string.mizrach_no_compass_sensor to R.string.mizrach_no_compass_sensor_hebrew
    // Checked before needsCalibration: an unreliable status also trips needsCalibration
    // (UNRELIABLE ≤ LOW), but it warrants the stronger "can't be trusted" wording.
    unreliableStatus ->
        R.string.mizrach_compass_unreliable to R.string.mizrach_compass_unreliable_hebrew
    needsCalibration || lowConfidence ->
        R.string.mizrach_compass_accuracy_low to R.string.mizrach_compass_accuracy_low_hebrew
    headingDegrees == null ->
        R.string.mizrach_heading_unavailable to R.string.mizrach_heading_unavailable_hebrew
    locationTrust == CompassLocationTrust.NeedsPrecisePermission ->
        R.string.mizrach_precise_location_required to R.string.mizrach_precise_location_required_hebrew
    locationTrust == CompassLocationTrust.DeveloperOverride ->
        R.string.mizrach_developer_location_compass to R.string.mizrach_developer_location_compass_hebrew
    locationTrust == CompassLocationTrust.NeedsBetterFix ->
        R.string.mizrach_location_accuracy_low to R.string.mizrach_location_accuracy_low_hebrew
    else -> null
}

/**
 * Live guidance toward the target, or null while there is no usable heading (waiting, stale, or
 * missing sensors) — the UI must not claim a direction it doesn't have. Hysteresis and the
 * degraded-reading suppression live in [AlignmentGate].
 */
@Composable
private fun rememberAlignmentState(
    bearingDegrees: Float,
    locationTrusted: Boolean,
    compassSensorState: State<CompassSensorState>,
): CompassAlignment? {
    // Keyed on the bearing: a location refinement resets the hysteresis, so a stale aligned
    // state can't ride the wider exit threshold against a new target direction.
    val gate = remember(bearingDegrees) { AlignmentGate() }
    // Derived: recomposes only when the coarse direction changes, not on every heading tick.
    // Mutating the gate inside derivedStateOf is safe because update() is idempotent for
    // identical inputs — a same-input re-evaluation returns the same result and end state.
    val direction by remember(bearingDegrees, locationTrusted, compassSensorState, gate) {
        derivedStateOf {
            val state = compassSensorState.value
            val degraded = state.hasLowAccuracy || !locationTrusted
            gate.update(
                // Android-reported degraded data gives no guidance at all: even a coarse turn
                // direction can be wrong when a quality signal is degraded.
                relativeDirectionDegrees = state.headingDegrees
                    ?.takeUnless { degraded }
                    ?.let { heading -> targetDirectionOnScreen(bearingDegrees, heading) },
                degraded = degraded,
            )
        }
    }
    val label = when (direction) {
        AlignmentDirection.Aligned -> localizedString(R.string.mizrach_aligned, R.string.mizrach_aligned_hebrew)
        AlignmentDirection.TurnRight -> localizedString(R.string.mizrach_turn_right, R.string.mizrach_turn_right_hebrew)
        AlignmentDirection.TurnLeft -> localizedString(R.string.mizrach_turn_left, R.string.mizrach_turn_left_hebrew)
        null -> return null
    }
    val isAligned = direction == AlignmentDirection.Aligned
    return remember(isAligned, label) { CompassAlignment(isAligned = isAligned, label = label) }
}

@Composable
private fun VibrateWhenAligned(aligned: Boolean) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    // Buzz on every genuine alignment entry. Angular hysteresis prevents sensor noise from
    // repeatedly crossing the boundary.
    DisposableEffect(vibrator, aligned) {
        if (aligned && vibrator?.hasVibrator() == true) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 90L, 70L, 120L),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1,
                ),
            )
        }
        onDispose {
            if (aligned) vibrator?.cancel()
        }
    }
}
