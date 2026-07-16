// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noamtu.jewishday.model.CompassMinimumHorizontalStrength
import com.noamtu.jewishday.model.CompassQuality
import com.noamtu.jewishday.model.CompassRecoverHorizontalStrength
import com.noamtu.jewishday.model.HeadingFilter
import com.noamtu.jewishday.model.angularDistanceDegrees
import com.noamtu.jewishday.model.compassQuality
import com.noamtu.jewishday.model.isFiniteVector
import com.noamtu.jewishday.model.screenRelativeHeadingDegrees
import com.noamtu.jewishday.model.sensorPairIsFresh
import com.noamtu.jewishday.model.trueHeadingDegrees

/**
 * Screen-relative *true* heading (declination-corrected, display-rotation-corrected, filtered),
 * plus Android's quality signals about why the reading might not be trustworthy right now.
 */
data class CompassSensorState(
    val headingDegrees: Float? = null,
    val sensorsAvailable: Boolean = true,
    /** The platform reports the sensor needs recalibration. */
    val needsCalibration: Boolean = false,
    /** The fused heading's own error estimate is too large for confident alignment. */
    val lowConfidence: Boolean = false,
    /** The platform says the data cannot be trusted at all. */
    val unreliableStatus: Boolean = false,
) {
    /** Too suspect for any directional claim: no turn hints, no alignment, dimmed needle. */
    val unreliable: Boolean get() = unreliableStatus

    /** Anything that must block *confident* alignment (green needle, haptic). */
    val hasLowAccuracy: Boolean get() = unreliable || needsCalibration || lowConfidence
}

/**
 * Subscribes to orientation sensors while [enabled] and the lifecycle is resumed, and exposes the
 * filtered heading as state readable in the draw phase.
 *
 * Source preference: the fused TYPE_ROTATION_VECTOR (gyro+accel+mag — tilt-compensated, gyro
 * stabilized, hardware calibrated), then TYPE_GEOMAGNETIC_ROTATION_VECTOR, then a manual
 * accelerometer+magnetometer fusion as the last resort. The magnetometer is additionally sampled
 * at a slow rate for its Android accuracy status.
 *
 * Declination is read through [rememberUpdatedState], so a location refinement mid-session
 * adjusts the heading smoothly instead of tearing the sensors down and re-seeding the filter.
 * A display-rotation change *does* restart the pipeline: the screen-relative frame itself changed,
 * so re-seeding from the next sample is the correct (and instant) behavior.
 */
@Composable
fun rememberCompassSensorState(
    enabled: Boolean,
    magneticDeclinationDegrees: Float,
): State<CompassSensorState> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val stateHolder = remember { mutableStateOf(CompassSensorState()) }
    val declination = rememberUpdatedState(magneticDeclinationDegrees)

    // Track the display rotation with a DisplayListener: 180° jumps (portrait↔reverse-portrait,
    // landscape↔reverse-landscape) don't reliably produce a configuration change, so keying off
    // LocalConfiguration alone can miss them. The configuration read stays as a second trigger
    // for the ordinary recreate/recompose path.
    val configuration = LocalConfiguration.current
    val view = LocalView.current
    var displayRotationDegrees by remember(view) { mutableStateOf(view.displayRotationDegrees()) }
    LaunchedEffect(configuration) { displayRotationDegrees = view.displayRotationDegrees() }
    DisposableEffect(view) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                if (displayId == view.display?.displayId) {
                    displayRotationDegrees = view.displayRotationDegrees()
                }
            }
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }

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

    DisposableEffect(context, enabled, isResumed, displayRotationDegrees) {
        if (!enabled || !isResumed) {
            // While paused we keep the last heading on screen (resume re-seeds it within one
            // sensor period); when disabled outright there is nothing meaningful to show.
            if (!enabled) stateHolder.value = CompassSensorState()
            onDispose { }
        } else {
            // A new lifecycle/display generation has no current heading yet. Never expose the
            // previous controller's orientation as live while waiting for its first event.
            stateHolder.value = CompassSensorState()
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val controller = CompassSensorController(
                sensorManager = sensorManager,
                displayRotationDegrees = displayRotationDegrees,
                declinationDegrees = { declination.value },
                onState = { stateHolder.value = it },
            )
            if (controller.start()) {
                onDispose { controller.stop() }
            } else {
                stateHolder.value = CompassSensorState(sensorsAvailable = false)
                onDispose { }
            }
        }
    }

    return stateHolder
}

private fun View.displayRotationDegrees(): Int = when (display?.rotation) {
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> 0
}

private class CompassSensorController(
    private val sensorManager: SensorManager,
    private val displayRotationDegrees: Int,
    private val declinationDegrees: () -> Float,
    private val onState: (CompassSensorState) -> Unit,
) : SensorEventListener {
    private val rotationMatrix = FloatArray(9)
    private val rotationVector4 = FloatArray(4)
    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false
    private var usingRotationVector = false
    private var rawFallbackActive = false
    private var hasMagnetometer = false

    private val filter = HeadingFilter()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val staleTimeout = Runnable { invalidateHeading(clearQuality = true) }
    private val sourceFailoverTimeout = Runnable { failoverToNextSource() }
    private val sourceRetryTimeout = Runnable { retrySources() }
    private val initialQualityTimeout = Runnable {
        waitingForInitialMagnetometerStatus = false
        publish()
    }
    private var requiredStrength = CompassMinimumHorizontalStrength
    private var lastUsableHeadingNanos = 0L
    private var gravitySampleNanos = 0L
    private var geomagneticSampleNanos = 0L
    private var fusedCandidates: List<Sensor> = emptyList()
    private var nextFusedCandidateIndex = 0
    private var accelerometerSensor: Sensor? = null
    private var magnetometerSensor: Sensor? = null
    private var activeFusedSensor: Sensor? = null
    private var rawFallbackAttempted = false
    private var waitingForInitialMagnetometerStatus = false
    private var stopped = false

    // null = the platform has made no claim yet. Read from every event, not only
    // onAccuracyChanged: the framework fires that callback only on *changes* against a cache
    // that defaults to UNRELIABLE, so an initially-unreliable sensor never triggers it.
    private var fusedStatus: Int? = null
    private var magnetometerStatus: Int? = null

    // SystemClock stamp (not event.timestamp — vendor timestamp bases vary) so a stalled
    // monitor's last LOW report can't pin the calibration warning forever.
    private var magnetometerStatusNanos = 0L
    private var headingAccuracyDegrees = Float.NaN
    private var published = CompassSensorState()

    fun start(): Boolean {
        // registerListener can fail even when the sensor object exists; try each fused source,
        // then the accel+mag pair, rather than waiting forever for events that never come.
        fusedCandidates = listOfNotNull(
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
            sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR),
        )
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        stopped = false
        return startNextSource()
    }

    fun stop() {
        stopped = true
        mainHandler.removeCallbacks(staleTimeout)
        mainHandler.removeCallbacks(sourceFailoverTimeout)
        mainHandler.removeCallbacks(sourceRetryTimeout)
        mainHandler.removeCallbacks(initialQualityTimeout)
        sensorManager.unregisterListener(this)
    }

    private fun startNextSource(): Boolean {
        mainHandler.removeCallbacks(staleTimeout)
        mainHandler.removeCallbacks(sourceFailoverTimeout)
        mainHandler.removeCallbacks(sourceRetryTimeout)
        mainHandler.removeCallbacks(initialQualityTimeout)
        sensorManager.unregisterListener(this)
        resetSourceState()

        while (nextFusedCandidateIndex < fusedCandidates.size) {
            val candidate = fusedCandidates[nextFusedCandidateIndex++]
            if (sensorManager.registerListener(this, candidate, HeadingSamplingPeriodMicros)) {
                usingRotationVector = true
                activeFusedSensor = candidate
                hasMagnetometer = magnetometerSensor?.let {
                    sensorManager.registerListener(this, it, MonitorSamplingPeriodMicros)
                } == true
                if (hasMagnetometer) {
                    waitingForInitialMagnetometerStatus = true
                    mainHandler.postDelayed(initialQualityTimeout, InitialQualityWaitMillis)
                }
                armSourceTimeouts()
                return true
            }
        }

        if (rawFallbackAttempted) return false
        rawFallbackAttempted = true
        val accelerometer = accelerometerSensor ?: return false
        val magnetometer = magnetometerSensor ?: return false
        val accelerometerRegistered =
            sensorManager.registerListener(this, accelerometer, HeadingSamplingPeriodMicros)
        val magnetometerRegistered = accelerometerRegistered &&
            sensorManager.registerListener(this, magnetometer, HeadingSamplingPeriodMicros)
        if (!magnetometerRegistered) {
            sensorManager.unregisterListener(this)
            return false
        }
        usingRotationVector = false
        rawFallbackActive = true
        hasMagnetometer = true
        armSourceTimeouts()
        return true
    }

    private fun resetSourceState() {
        usingRotationVector = false
        rawFallbackActive = false
        hasMagnetometer = false
        activeFusedSensor = null
        hasGravity = false
        hasGeomagnetic = false
        gravitySampleNanos = 0L
        geomagneticSampleNanos = 0L
        fusedStatus = null
        magnetometerStatus = null
        magnetometerStatusNanos = 0L
        headingAccuracyDegrees = Float.NaN
        requiredStrength = CompassMinimumHorizontalStrength
        lastUsableHeadingNanos = 0L
        waitingForInitialMagnetometerStatus = false
        filter.reset()
        published = CompassSensorState()
        onState(published)
    }

    private fun armSourceTimeouts() {
        mainHandler.postDelayed(staleTimeout, StaleHeadingTimeoutMillis)
        mainHandler.postDelayed(sourceFailoverTimeout, SourceFailoverTimeoutMillis)
    }

    private fun noteHeadingSourceEvent() {
        mainHandler.removeCallbacks(sourceFailoverTimeout)
        mainHandler.postDelayed(sourceFailoverTimeout, SourceFailoverTimeoutMillis)
    }

    private fun failoverToNextSource() {
        if (stopped) return
        if (!startNextSource()) {
            // At least one source registered earlier in this cycle, so this is a runtime failure,
            // not proof that the device lacks compass hardware. Stay unavailable and retry.
            mainHandler.postDelayed(sourceRetryTimeout, SourceRetryTimeoutMillis)
        }
    }

    private fun retrySources() {
        if (stopped) return
        nextFusedCandidateIndex = 0
        rawFallbackAttempted = false
        if (!startNextSource()) {
            mainHandler.postDelayed(sourceRetryTimeout, SourceRetryTimeoutMillis)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (stopped) return
        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            -> {
                if (event.sensor !== activeFusedSensor) return
                val vectorLength = minOf(event.values.size, 4)
                if (vectorLength < 3 || !isFiniteVector(event.values, vectorLength)) return
                // Some devices deliver 5 values; getRotationMatrixFromVector accepts only 3 or 4.
                if (event.values.size >= 4) {
                    event.values.copyInto(rotationVector4, endIndex = 4)
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector4)
                } else {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                }
                // values[4] is the OS's own estimated heading error in radians (-1 = unknown).
                headingAccuracyDegrees = event.values.getOrNull(4)
                    ?.takeIf { it.isFinite() && it >= 0f }
                    ?.let { Math.toDegrees(it.toDouble()).toFloat() }
                    ?: Float.NaN
                fusedStatus = event.accuracy
                updateHeading(event.timestamp)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                if (!rawFallbackActive || !isFiniteVector(event.values)) return
                lowPassInto(event.values, gravity, seeded = hasGravity)
                hasGravity = true
                gravitySampleNanos = SystemClock.elapsedRealtimeNanos()
                if (hasGeomagnetic && fallbackVectorsFresh(gravitySampleNanos) &&
                    SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                ) {
                    updateHeading(event.timestamp)
                }
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                if (!hasMagnetometer) return
                magnetometerStatus = event.accuracy
                val nowNanos = SystemClock.elapsedRealtimeNanos()
                magnetometerStatusNanos = nowNanos
                noteInitialMagnetometerStatus()
                if (!usingRotationVector) {
                    if (!isFiniteVector(event.values)) {
                        publishQualityOnly()
                        return
                    }
                    lowPassInto(event.values, geomagnetic, seeded = hasGeomagnetic)
                    hasGeomagnetic = true
                    geomagneticSampleNanos = nowNanos
                    if (hasGravity && fallbackVectorsFresh(nowNanos) &&
                        SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)
                    ) {
                        updateHeading(event.timestamp)
                    } else {
                        publishQualityOnly()
                    }
                } else {
                    publishQualityOnly()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (stopped) return
        when (sensor?.type) {
            Sensor.TYPE_MAGNETIC_FIELD -> {
                magnetometerStatus = accuracy
                magnetometerStatusNanos = SystemClock.elapsedRealtimeNanos()
                noteInitialMagnetometerStatus()
            }
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
            -> {
                if (sensor !== activeFusedSensor) return
                fusedStatus = accuracy
            }
            else -> return
        }
        publishQualityOnly()
    }

    private fun updateHeading(timestampNanos: Long) {
        noteHeadingSourceEvent()
        // Heading events are flowing again; push the no-data deadline out.
        mainHandler.removeCallbacks(staleTimeout)
        mainHandler.postDelayed(staleTimeout, StaleHeadingTimeoutMillis)

        val magnetic = screenRelativeHeadingDegrees(rotationMatrix, displayRotationDegrees, requiredStrength)
        val declination = declinationDegrees()
        // Ambiguous-attitude hysteresis: after a null, demand a clearly valid reference before
        // resuming so noise at the singularity cannot flip the needle between opposite readings.
        requiredStrength =
            if (magnetic != null) CompassMinimumHorizontalStrength else CompassRecoverHorizontalStrength

        val quality = currentQuality()
        // Keep tracking degraded Android readings so the user still has a faded directional
        // reference. Quality suppresses alignment and turn claims; it does not hide the needle.
        if (magnetic != null && declination.isFinite()) {
            lastUsableHeadingNanos = timestampNanos
            filter.filter(trueHeadingDegrees(magnetic, declination), timestampNanos)
        } else {
            // Ambiguous attitude: hold the last heading briefly; a prolonged hold becomes
            // "unavailable" (filter reset), so recovery re-seeds instantly from the next sample.
            if (lastUsableHeadingNanos != 0L &&
                timestampNanos - lastUsableHeadingNanos > DegenerateHoldNanos
            ) {
                invalidateHeading(clearQuality = false)
                return
            }
        }
        publish(quality = quality)
    }

    /** Drops the heading so the next valid sample re-seeds instantly. */
    private fun invalidateHeading(clearQuality: Boolean) {
        filter.reset()
        if (clearQuality) {
            fusedStatus = null
            if (!usingRotationVector) magnetometerStatus = null
        }
        val quality = if (clearQuality) {
            compassQuality(null, null, Float.NaN)
        } else {
            currentQuality()
        }
        publish(quality = quality, forceHeadingNull = true)
    }

    private fun publishQualityOnly() {
        val quality = currentQuality()
        publish(quality = quality)
    }

    private fun publish(
        quality: CompassQuality = currentQuality(),
        forceHeadingNull: Boolean = false,
    ) {
        val filtered = filter.current
        val previousHeading = published.headingDegrees
        // Skip sub-visible needle movement so a resting compass doesn't redraw at sensor rate.
        val heading = when {
            forceHeadingNull || waitingForInitialMagnetometerStatus -> null
            filtered == null -> previousHeading
            previousHeading == null -> filtered
            angularDistanceDegrees(filtered, previousHeading) < PublishThresholdDegrees -> previousHeading
            else -> filtered
        }
        val state = CompassSensorState(
            headingDegrees = heading,
            sensorsAvailable = true,
            needsCalibration = quality.needsCalibration,
            lowConfidence = quality.lowConfidence,
            unreliableStatus = quality.unreliableStatus,
        )
        if (state != published) {
            published = state
            onState(state)
        }
    }

    /**
     * The platform's current quality verdict. The magnetometer monitor's status counts only
     * while fresh: if the monitor stalls after reporting LOW, its stale claim must not pin the
     * calibration warning while healthy fused events keep flowing (unknown makes no claim).
     */
    private fun currentQuality(): CompassQuality {
        val nowNanos = SystemClock.elapsedRealtimeNanos()
        val freshMagnetometerStatus = magnetometerStatus.takeIf {
            hasMagnetometer &&
                nowNanos - magnetometerStatusNanos <= MagnetometerStatusFreshnessNanos
        }
        return compassQuality(
            primaryStatus = if (usingRotationVector) fusedStatus else freshMagnetometerStatus,
            magnetometerStatus = freshMagnetometerStatus,
            headingErrorDegrees = headingAccuracyDegrees,
        )
    }

    private fun noteInitialMagnetometerStatus() {
        if (!waitingForInitialMagnetometerStatus) return
        waitingForInitialMagnetometerStatus = false
        mainHandler.removeCallbacks(initialQualityTimeout)
    }

    private fun fallbackVectorsFresh(nowNanos: Long): Boolean =
        sensorPairIsFresh(
            nowNanos = nowNanos,
            firstSampleNanos = gravitySampleNanos,
            secondSampleNanos = geomagneticSampleNanos,
            maxAgeNanos = FallbackVectorFreshnessNanos,
        )

    private fun lowPassInto(values: FloatArray, output: FloatArray, seeded: Boolean) {
        // Re-seed on the first sample — or if the stored vector somehow went non-finite, since
        // low-passing can never pull it back from NaN.
        if (!seeded || !isFiniteVector(output)) {
            values.copyInto(output, endIndex = 3)
            return
        }
        repeat(3) { index ->
            output[index] += (values[index] - output[index]) * FallbackVectorAlpha
        }
    }

    private companion object {
        /** 50 Hz for the heading source: smooth needle, still cheap for a foreground screen. */
        const val HeadingSamplingPeriodMicros = 20_000

        /** 5 Hz is plenty for calibration-status monitoring. */
        const val MonitorSamplingPeriodMicros = 200_000

        const val PublishThresholdDegrees = 0.05f

        /**
         * How long a heading may survive without support before it stops being shown as live.
         * Sensors run at 50 Hz (20 Hz on slow devices), so a full second without a usable
         * sample means the stream stopped or the attitude stayed ambiguous — either way the
         * needle must not keep claiming a direction.
         */
        const val StaleHeadingTimeoutMillis = 1_000L
        const val SourceFailoverTimeoutMillis = 2_000L
        const val SourceRetryTimeoutMillis = 5_000L
        const val InitialQualityWaitMillis = 500L
        const val DegenerateHoldNanos = 1_000_000_000L

        /** ~15 monitor periods at 5 Hz; a status older than this is treated as unknown. */
        const val MagnetometerStatusFreshnessNanos = 3_000_000_000L
        const val FallbackVectorFreshnessNanos = 1_000_000_000L

        /** Mild pre-smoothing for the raw accel/mag fallback; HeadingFilter does the real work. */
        const val FallbackVectorAlpha = 0.5f
    }
}
