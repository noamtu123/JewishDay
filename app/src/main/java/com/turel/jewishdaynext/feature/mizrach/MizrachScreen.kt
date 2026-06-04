package com.turel.jewishdaynext.feature.mizrach

import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.data.hasLocationPermission
import com.turel.jewishdaynext.model.MizrachInfo
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.ValuePill
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.localizedString
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MizrachScreen(
    modifier: Modifier = Modifier,
    viewModel: MizrachViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compassSensorState = rememberCompassSensorState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) viewModel.refreshCurrentLocation()
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) viewModel.refreshCurrentLocation()
    }

    MizrachContent(
        mizrach = uiState.mizrachInfo,
        hasLocationPermission = hasLocationPermission,
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
        modifier = modifier,
    )
}

@Composable
private fun MizrachContent(
    mizrach: MizrachInfo,
    hasLocationPermission: Boolean,
    hasCurrentLocation: Boolean,
    compassSensorState: CompassSensorState,
    onRequestLocation: () -> Unit,
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
                    Text(
                        text = localizedString(R.string.mizrach_waiting_location, R.string.mizrach_waiting_location_hebrew),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
    compassSensorState: CompassSensorState,
    modifier: Modifier = Modifier,
) {
    val headingDegrees = compassSensorState.headingDegrees
    val alignment = rememberAlignmentState(mizrach.bearingDegrees, headingDegrees)
    VibrateWhenAligned(aligned = alignment.isAligned)

    InfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
    ) {
        CompassFace(
            bearingDegrees = mizrach.bearingDegrees,
            headingDegrees = headingDegrees,
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
        if (headingDegrees == null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = localizedString(R.string.mizrach_heading_unavailable, R.string.mizrach_heading_unavailable_hebrew),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        } else if (compassSensorState.hasLowAccuracy) {
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
    headingDegrees: Float?,
    alignment: CompassAlignment,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val alignedColor = MaterialTheme.colorScheme.tertiary
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    val relativeDirectionDegrees = headingDegrees?.let { heading ->
        normalizeDegrees(bearingDegrees - heading)
    } ?: bearingDegrees.toFloat()

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val inset = 16.dp.toPx()
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
                color = outline,
                start = Offset(center.x, inset),
                end = Offset(center.x, inset + 16.dp.toPx()),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
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
        }
        Text(
            text = localizedString(R.string.mizrach_north, R.string.mizrach_north_hebrew),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
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

@Composable
private fun rememberAlignmentState(
    bearingDegrees: Int,
    headingDegrees: Float?,
): CompassAlignment {
    val relative = headingDegrees?.let { heading -> normalizeDegrees(bearingDegrees - heading) }
    val difference = relative?.let(::smallestAngleDistance) ?: 180f
    val turnRight = relative != null && relative in 0f..180f
    val isAligned = difference <= 5f
    val label = when {
        isAligned -> localizedString(R.string.mizrach_aligned, R.string.mizrach_aligned_hebrew)
        turnRight -> localizedString(R.string.mizrach_turn_right, R.string.mizrach_turn_right_hebrew)
        else -> localizedString(R.string.mizrach_turn_left, R.string.mizrach_turn_left_hebrew)
    }
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

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createWaveform(
                        longArrayOf(0L, 90L, 70L, 120L),
                        intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                        -1,
                    ),
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0L, 90L, 70L, 120L), -1)
            }
        }
    }
}

@Composable
private fun rememberCompassSensorState(): CompassSensorState {
    val context = LocalContext.current
    var sensorState by remember { mutableStateOf(CompassSensorState()) }

    DisposableEffect(context) {
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

            fun updateHeadingFromMatrix(accuracy: Int?) {
                sensorState = CompassSensorState(
                    headingDegrees = rotationMatrix.pointingHeadingDegrees(),
                    accuracy = accuracy,
                )
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
                            event.values.copyInto(gravity, endIndex = 3)
                            hasGravity = true
                            if (hasGeomagnetic && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                updateHeadingFromMatrix(event.accuracy)
                            }
                        }
                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            event.values.copyInto(geomagnetic, endIndex = 3)
                            hasGeomagnetic = true
                            if (hasGravity && SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                                updateHeadingFromMatrix(event.accuracy)
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    sensorState = sensorState.copy(accuracy = accuracy)
                }
            }
            if (rotationVector != null) {
                sensorManager.registerListener(listener, rotationVector, SensorManager.SENSOR_DELAY_UI)
            } else {
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
                sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_UI)
            }
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    return sensorState
}

private fun FloatArray.pointingHeadingDegrees(): Float? {
    val topEdge = horizontalVectorHeading(east = this[1], north = this[4])
    val backCamera = horizontalVectorHeading(east = -this[2], north = -this[5])
    val best = listOfNotNull(topEdge, backCamera).maxByOrNull { it.horizontalStrength }
    return best?.takeIf { it.horizontalStrength >= 0.12f }?.headingDegrees
}

private data class HorizontalHeading(
    val headingDegrees: Float,
    val horizontalStrength: Float,
)

private fun horizontalVectorHeading(east: Float, north: Float): HorizontalHeading? {
    val horizontalStrength = sqrt(east * east + north * north)
    if (horizontalStrength == 0f) return null

    return HorizontalHeading(
        headingDegrees = normalizeDegrees(Math.toDegrees(atan2(east, north).toDouble()).toFloat()),
        horizontalStrength = horizontalStrength,
    )
}

private fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

private fun normalizeDegrees(degrees: Int): Int = ((degrees % 360) + 360) % 360

private fun smallestAngleDistance(degrees: Float): Float =
    minOf(degrees, 360f - degrees)
