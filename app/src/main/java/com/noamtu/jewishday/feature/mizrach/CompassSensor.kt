// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import android.content.Context
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
    diagnosticsEnabled: Boolean = false,
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
    var displayRotationDegrees by remember(view) { mutableIntStateOf(view.displayRotationDegrees()) }
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

    DisposableEffect(context, enabled, isResumed, displayRotationDegrees, diagnosticsEnabled) {
        if (!enabled || !isResumed) {
            // A paused-visible activity must not present a stopped sensor as live guidance.
            stateHolder.value = CompassSensorState()
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
                diagnosticsEnabled = diagnosticsEnabled,
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

internal fun View.displayRotationDegrees(): Int = when (display?.rotation) {
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> 0
}
