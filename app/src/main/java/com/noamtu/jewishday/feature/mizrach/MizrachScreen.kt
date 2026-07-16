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
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.GeomagneticField
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import com.noamtu.jewishday.model.AlignmentDirection
import com.noamtu.jewishday.model.AlignmentGate
import com.noamtu.jewishday.model.MizrachInfo
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
    var locationServicesEnabled by remember { mutableStateOf(context.isLocationServicesEnabled()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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
    )
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            viewModel.refreshCurrentLocation()
        } else if (!context.canAskLocationPermission()) {
            // Permission is permanently denied ("Don't allow" chosen before), so the system dialog
            // won't appear again — the only way to grant it is from the app's settings page.
            context.openAppSettings()
        }
    }

    // Try for a fix whenever we have both permission and the system location toggle on; without
    // either, a fix can never arrive, so we prompt instead of waiting.
    LaunchedEffect(hasLocationPermission, locationServicesEnabled) {
        if (hasLocationPermission && locationServicesEnabled) viewModel.refreshCurrentLocation()
    }

    // Re-check on every resume so a change made in system Settings (granting permission or turning
    // location on) takes effect without recreating the process.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasLocationPermission()
                locationServicesEnabled = context.isLocationServicesEnabled()
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
        compassSensorState = compassSensorState,
        onRequestLocation = {
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
    compassSensorState: State<CompassSensorState>,
    onRequestLocation: () -> Unit,
    onOpenLocationSettings: () -> Unit,
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
                        compassSensorState = compassSensorState,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
    compassSensorState: State<CompassSensorState>,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf: heading updates arrive up to 50×/s, but these only change
    // composition when the derived value actually flips.
    val hint by remember(compassSensorState) {
        derivedStateOf { compassSensorState.value.accuracyHint() }
    }
    val alignment = rememberAlignmentState(mizrach.bearingDegreesExact, compassSensorState)
    VibrateWhenAligned(aligned = alignment?.isAligned == true)

    InfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
    ) {
        CompassFace(
            bearingDegrees = mizrach.bearingDegrees,
            bearingDegreesExact = mizrach.bearingDegreesExact,
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
            Text(
                text = localizedString(english, hebrew),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
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

@Composable
private fun CompassFace(
    bearingDegrees: Int,
    bearingDegreesExact: Float,
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
        Canvas(modifier = Modifier.size(240.dp)) {
            // Sensor state is read here, in the draw phase, so each sensor tick only
            // redraws the needle instead of recomposing the whole screen.
            val sensorState = sensorStateProvider()
            val heading = sensorState.headingDegrees
            // Do not draw an assumed-north needle while waiting for the first sensor reading: it
            // would point incorrectly and then jump. A device with no compass sensor keeps the
            // explicitly documented static-bearing fallback.
            val showNeedle = heading != null || !sensorState.sensorsAvailable
            val live = heading != null && !sensorState.unreliable
            val relativeDirectionDegrees = heading?.let { targetDirectionOnScreen(bearingDegreesExact, it) }
                ?: bearingDegreesExact
            val needleColor = when {
                isAligned -> alignedColor
                live -> primary
                else -> primary.copy(alpha = 0.35f)
            }
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
                drawLine(
                    color = needleColor,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 8.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(color = needleColor, radius = 9.dp.toPx(), center = needleEnd)
                drawCircle(color = needleColor, radius = 5.dp.toPx(), center = center)
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
                ValuePill(
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

private data class CompassAlignment(
    val isAligned: Boolean,
    val label: String,
)

/**
 * The accuracy message to show under the compass, as an English/Hebrew string-resource pair, or
 * null when the reading is healthy. One message at a time, straight from the platform's own
 * signals: the figure-eight hint exactly while Android reports low/unreliable accuracy, and a
 * soft "approximate" note while the fused source's own error estimate is high. No app-side
 * magnetic guessing.
 */
private fun CompassSensorState.accuracyHint(): Pair<Int, Int>? = when {
    !sensorsAvailable ->
        R.string.mizrach_no_compass_sensor to R.string.mizrach_no_compass_sensor_hebrew
    // Accuracy outranks "waiting": a long unreliable stretch invalidates the heading, and the
    // actionable message then is the accuracy one, not "waiting for the compass sensor".
    needsCalibration || unreliableStatus ->
        R.string.mizrach_compass_accuracy_low to R.string.mizrach_compass_accuracy_low_hebrew
    headingDegrees == null ->
        R.string.mizrach_heading_unavailable to R.string.mizrach_heading_unavailable_hebrew
    lowConfidence ->
        R.string.mizrach_compass_low_confidence to R.string.mizrach_compass_low_confidence_hebrew
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
    compassSensorState: State<CompassSensorState>,
): CompassAlignment? {
    // Keyed on the bearing: a location refinement resets the hysteresis, so a stale aligned
    // state can't ride the wider exit threshold against a new target direction.
    val gate = remember(bearingDegrees) { AlignmentGate() }
    // Derived: recomposes only when the coarse direction changes, not on every heading tick.
    // Mutating the gate inside derivedStateOf is safe because update() is idempotent for
    // identical inputs — a same-input re-evaluation returns the same result and end state.
    val direction by remember(bearingDegrees, compassSensorState, gate) {
        derivedStateOf {
            val state = compassSensorState.value
            gate.update(
                // Unreliable data (magnet nearby / OS says untrusted) gives no guidance at all:
                // even a coarse turn direction can be wrong then.
                relativeDirectionDegrees = state.headingDegrees
                    ?.takeUnless { state.unreliable }
                    ?.let { heading -> targetDirectionOnScreen(bearingDegrees, heading) },
                degraded = state.hasLowAccuracy,
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
    // Buzz on every genuine alignment entry; the gate's angular hysteresis (enter 5°, leave 8°)
    // is what stops needle noise from re-firing this.
    LaunchedEffect(aligned) {
        if (aligned) {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                manager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return@LaunchedEffect
            if (!vibrator.hasVibrator()) return@LaunchedEffect

            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 90L, 70L, 120L),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1,
                ),
            )
        }
    }
}
