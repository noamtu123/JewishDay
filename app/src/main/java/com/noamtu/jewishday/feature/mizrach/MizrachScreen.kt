package com.noamtu.jewishday.feature.mizrach

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.GeomagneticField
import android.hardware.SensorManager
import android.os.SystemClock
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
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import com.noamtu.jewishday.model.MizrachInfo
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedString
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

private const val CompassSensorDelayMicros = 50_000
private const val CompassSensorUpdateIntervalMillis = 50L
private const val CompassHeadingChangeThresholdDegrees = 0.35f
private const val CompassHeadingSmoothingFactor = 0.30f
private const val CompassFallbackSensorSmoothingFactor = 0.18f
private const val CompassMinimumHorizontalStrength = 0.10f

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
        if (hasLocationPermission) viewModel.refreshCurrentLocation()
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
    // compass appears the moment location is turned on, without a manual refresh.
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                hasLocationPermission = context.hasLocationPermission()
                locationServicesEnabled = context.isLocationServicesEnabled()
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
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = localizedString(R.string.mizrach_from, R.string.mizrach_from_hebrew, mizrach.fromLocationName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompassCard(
    mizrach: MizrachInfo,
    compassSensorState: State<CompassSensorState>,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf: heading updates arrive up to 20×/s, but these only change
    // composition when the derived value actually flips.
    val headingUnavailable by remember(compassSensorState) {
        derivedStateOf { compassSensorState.value.headingDegrees == null }
    }
    val hasLowAccuracy by remember(compassSensorState) {
        derivedStateOf { compassSensorState.value.hasLowAccuracy }
    }
    val alignment = rememberAlignmentState(mizrach.bearingDegreesExact, compassSensorState)
    VibrateWhenAligned(aligned = alignment.isAligned)

    InfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
    ) {
        CompassFace(
            bearingDegrees = mizrach.bearingDegrees,
            bearingDegreesExact = mizrach.bearingDegreesExact,
            headingDegreesProvider = { compassSensorState.value.headingDegrees },
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
        if (headingUnavailable) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = localizedString(R.string.mizrach_heading_unavailable, R.string.mizrach_heading_unavailable_hebrew),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        } else if (hasLowAccuracy) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = localizedString(R.string.mizrach_compass_accuracy_low, R.string.mizrach_compass_accuracy_low_hebrew),
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
    headingDegreesProvider: () -> Float?,
    alignment: CompassAlignment,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val alignedColor = MaterialTheme.colorScheme.tertiary
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            // Heading is read here, in the draw phase, so each sensor tick only
            // redraws the needle instead of recomposing the whole screen.
            val relativeDirectionDegrees = headingDegreesProvider()?.let { heading ->
                normalizeDegrees(bearingDegreesExact - heading)
            } ?: bearingDegreesExact
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
            drawLine(
                color = if (alignment.isAligned) alignedColor else primary,
                start = center,
                end = needleEnd,
                strokeWidth = 8.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawCircle(color = if (alignment.isAligned) alignedColor else primary, radius = 9.dp.toPx(), center = needleEnd)
            drawCircle(color = if (alignment.isAligned) alignedColor else primary, radius = 5.dp.toPx(), center = center)
            drawTempleMarker(
                center = Offset(center.x, center.y - radius + 32.dp.toPx()),
                color = if (alignment.isAligned) alignedColor else primary,
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
            Spacer(Modifier.height(8.dp))
            ValuePill(
                text = alignment.label,
                containerColor = if (alignment.isAligned) alignedColor else MaterialTheme.colorScheme.surface,
                contentColor = if (alignment.isAligned) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
            )
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

private data class CompassSensorState(
    val headingDegrees: Float? = null,
    val accuracy: Int? = null,
) {
    val hasLowAccuracy: Boolean = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
        accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
}

private enum class AlignmentDirection { Aligned, TurnRight, TurnLeft }

@Composable
private fun rememberAlignmentState(
    bearingDegrees: Float,
    compassSensorState: State<CompassSensorState>,
): CompassAlignment {
    // Derived: recomposes only when the coarse direction changes, not on every heading tick.
    val direction by remember(bearingDegrees, compassSensorState) {
        derivedStateOf {
            val relative = compassSensorState.value.headingDegrees
                ?.let { heading -> normalizeDegrees(bearingDegrees - heading) }
            val difference = relative?.let(::smallestAngleDistance) ?: 180f
            when {
                difference <= 5f -> AlignmentDirection.Aligned
                relative != null && relative in 0f..180f -> AlignmentDirection.TurnRight
                else -> AlignmentDirection.TurnLeft
            }
        }
    }
    val label = when (direction) {
        AlignmentDirection.Aligned -> localizedString(R.string.mizrach_aligned, R.string.mizrach_aligned_hebrew)
        AlignmentDirection.TurnRight -> localizedString(R.string.mizrach_turn_right, R.string.mizrach_turn_right_hebrew)
        AlignmentDirection.TurnLeft -> localizedString(R.string.mizrach_turn_left, R.string.mizrach_turn_left_hebrew)
    }
    val isAligned = direction == AlignmentDirection.Aligned
    return remember(isAligned, label) { CompassAlignment(isAligned = isAligned, label = label) }
}

@Composable
private fun VibrateWhenAligned(aligned: Boolean) {
    val context = LocalContext.current
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

@Composable
private fun rememberCompassSensorState(
    enabled: Boolean,
    magneticDeclinationDegrees: Float,
): State<CompassSensorState> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sensorStateHolder = remember { mutableStateOf(CompassSensorState()) }
    var sensorState by sensorStateHolder
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isResumed = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY,
                -> isResumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(context, enabled, isResumed, magneticDeclinationDegrees) {
        if (!enabled || !isResumed) {
            sensorState = CompassSensorState()
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
                ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

            if (rotationVector == null && (accelerometer == null || magnetometer == null)) {
                sensorState = CompassSensorState()
                onDispose { }
            } else {
                val rotationMatrix = FloatArray(9)
                val gravity = FloatArray(3)
                val geomagnetic = FloatArray(3)
                var hasGravity = false
                var hasGeomagnetic = false
                var latestMagneticAccuracy: Int? = null
                var lastPublishedHeadingDegrees: Float? = null
                var lastPublishedAccuracy: Int? = null
                var lastPublishedAtMillis = 0L

                fun updateHeadingFromMatrix(accuracy: Int?) {
                    val rawHeading = rotationMatrix.pointingHeadingDegrees()
                    val heading = rawHeading?.let { magneticHeading ->
                        val trueHeading = normalizeDegrees(magneticHeading + magneticDeclinationDegrees)
                        smoothHeadingDegrees(lastPublishedHeadingDegrees, trueHeading)
                    }
                    val now = SystemClock.elapsedRealtime()
                    val previousHeading = lastPublishedHeadingDegrees
                    val headingChanged = previousHeading == null || heading == null ||
                        smallestAngleDistanceBetween(previousHeading, heading) >= CompassHeadingChangeThresholdDegrees
                    val accuracyChanged = accuracy != lastPublishedAccuracy

                    if (accuracyChanged || (headingChanged && now - lastPublishedAtMillis >= CompassSensorUpdateIntervalMillis)) {
                        lastPublishedHeadingDegrees = heading
                        lastPublishedAccuracy = accuracy
                        lastPublishedAtMillis = now
                        sensorState = CompassSensorState(
                            headingDegrees = heading,
                            accuracy = accuracy,
                        )
                    }
                }

                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        when (event.sensor.type) {
                            Sensor.TYPE_ROTATION_VECTOR,
                            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
                            -> {
                                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                                updateHeadingFromMatrix(event.accuracy)
                            }
                            Sensor.TYPE_ACCELEROMETER -> {
                                if (hasGravity) {
                                    lowPassInto(event.values, gravity)
                                } else {
                                    event.values.copyInto(gravity, endIndex = 3)
                                    hasGravity = true
                                }
                                if (hasGeomagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                    updateHeadingFromMatrix(latestMagneticAccuracy)
                                }
                            }
                            Sensor.TYPE_MAGNETIC_FIELD -> {
                                latestMagneticAccuracy = event.accuracy
                                if (hasGeomagnetic) {
                                    lowPassInto(event.values, geomagnetic)
                                } else {
                                    event.values.copyInto(geomagnetic, endIndex = 3)
                                    hasGeomagnetic = true
                                }
                                if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                    updateHeadingFromMatrix(latestMagneticAccuracy)
                                }
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                        if (accuracy != lastPublishedAccuracy) {
                            lastPublishedAccuracy = accuracy
                            sensorState = sensorState.copy(accuracy = accuracy)
                        }
                    }
                }
                if (rotationVector != null) {
                    sensorManager.registerListener(listener, rotationVector, CompassSensorDelayMicros)
                } else {
                    sensorManager.registerListener(listener, accelerometer, CompassSensorDelayMicros)
                    sensorManager.registerListener(listener, magnetometer, CompassSensorDelayMicros)
                }
                onDispose { sensorManager.unregisterListener(listener) }
            }
        }
    }

    return sensorStateHolder
}

private fun FloatArray.pointingHeadingDegrees(): Float? {
    val topEdge = horizontalVector(east = this[1], north = this[4])
    val backCamera = horizontalVector(east = -this[2], north = -this[5])
    return blendedHeading(topEdge, backCamera)
}

private data class HorizontalVector(
    val east: Float,
    val north: Float,
    val horizontalStrength: Float,
)

private fun horizontalVector(east: Float, north: Float): HorizontalVector? {
    val horizontalStrength = sqrt(east * east + north * north)
    if (horizontalStrength == 0f) return null

    return HorizontalVector(
        east = east / horizontalStrength,
        north = north / horizontalStrength,
        horizontalStrength = horizontalStrength,
    )
}

private fun blendedHeading(vararg vectors: HorizontalVector?): Float? {
    var weightedEast = 0f
    var weightedNorth = 0f
    vectors.filterNotNull().forEach { vector ->
        val weight = vector.horizontalStrength * vector.horizontalStrength
        weightedEast += vector.east * weight
        weightedNorth += vector.north * weight
    }

    val blendedStrength = sqrt(weightedEast * weightedEast + weightedNorth * weightedNorth)
    if (blendedStrength < CompassMinimumHorizontalStrength) return null
    return normalizeDegrees(Math.toDegrees(atan2(weightedEast, weightedNorth).toDouble()).toFloat())
}

private fun lowPassInto(values: FloatArray, output: FloatArray) {
    repeat(3) { index ->
        output[index] += (values[index] - output[index]) * CompassFallbackSensorSmoothingFactor
    }
}

private fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

private fun smoothHeadingDegrees(previous: Float?, current: Float): Float {
    if (previous == null) return current

    val delta = shortestSignedAngleDelta(from = previous, to = current)
    val smoothing = when {
        abs(delta) > 45f -> 0.55f
        abs(delta) > 15f -> 0.42f
        else -> CompassHeadingSmoothingFactor
    }
    return normalizeDegrees(previous + delta * smoothing)
}

private fun smallestAngleDistanceBetween(from: Float, to: Float): Float = abs(shortestSignedAngleDelta(from, to))

private fun shortestSignedAngleDelta(from: Float, to: Float): Float {
    val delta = normalizeDegrees(to - from)
    return if (delta > 180f) delta - 360f else delta
}

private fun smallestAngleDistance(degrees: Float): Float =
    minOf(degrees, 360f - degrees)
