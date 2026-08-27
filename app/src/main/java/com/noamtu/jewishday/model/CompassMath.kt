// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Pure math for the prayer-compass heading pipeline: angle helpers, the tilt-compensated
 * screen-relative azimuth of a rotation matrix, and the adaptive heading filter. Nothing here
 * touches Android APIs, so all of it runs in plain JVM unit tests.
 */

fun normalizeDegrees(degrees: Float): Float = ((degrees % 360f) + 360f) % 360f

/** Signed shortest rotation that takes [from] to [to], in (-180, 180]. */
fun shortestSignedDeltaDegrees(from: Float, to: Float): Float {
    val delta = normalizeDegrees(to - from)
    return if (delta > 180f) delta - 360f else delta
}

/** Smallest separation between two headings, in [0, 180]. */
fun angularDistanceDegrees(a: Float, b: Float): Float = abs(shortestSignedDeltaDegrees(a, b))

/** Magnetic heading → true heading. Declination is east-positive, as GeomagneticField reports. */
fun trueHeadingDegrees(magneticHeadingDegrees: Float, declinationDegrees: Float): Float =
    normalizeDegrees(magneticHeadingDegrees + declinationDegrees)

/**
 * True when the first [count] components are all finite. Guards raw sensor vectors: one NaN
 * low-passed into a stored gravity/geomagnetic vector would otherwise stick forever
 * (`x + (NaN - x) * a` is NaN, and every later sample keeps it NaN).
 */
fun isFiniteVector(values: FloatArray, count: Int = 3): Boolean {
    if (count < 0 || values.size < count) return false
    for (index in 0 until count) {
        if (!values[index].isFinite()) return false
    }
    return true
}

/**
 * Tilt-compensated azimuth of "screen up", in degrees clockwise from (magnetic) north, or null
 * when the device attitude leaves no usable horizontal reference (mid-flip toward face-down,
 * where the two references cancel).
 *
 * [rotationMatrix] follows the SensorManager convention: row-major device→world, world axes =
 * (east, north, up), so each column is one device axis expressed in world coordinates.
 *
 * [displayRotationDegrees] is the Surface.ROTATION_* of the display expressed in degrees
 * (0/90/180/270). The screen-up axis per rotation is the device axis that the canonical
 * remapCoordinateSystem table maps onto the new Y axis: ROTATION_90 uses remap(AXIS_Y,
 * AXIS_MINUS_X), whose "new Y" is the device +X axis, and ROTATION_270 uses remap(AXIS_MINUS_Y,
 * AXIS_X), whose "new Y" is the device -X axis. So the same physical attitude yields the same
 * on-screen needle in portrait, landscape, and both reversed forms.
 *
 * The heading blends two horizontal references — the screen-up axis (exact when the device is
 * flat) and the back camera (exact when upright). The camera receives only the weight the
 * screen-up axis has lost to tilt (1 − |up_h|⁴), so a flat device follows its top edge *exactly*
 * regardless of roll (a magnitude-weighted blend would bend ~14° at 30° of roll), an upright
 * device follows the camera, and intermediate tilts transition continuously. For a pure pitch
 * sweep the references only cancel deep into face-down (~147°), and callers can pass a raised
 * [minimumStrength] after a null to add hysteresis so attitude noise at that singularity cannot
 * flip the needle back and forth.
 */
fun screenRelativeHeadingDegrees(
    rotationMatrix: FloatArray,
    displayRotationDegrees: Int,
    minimumStrength: Float = CompassMinimumHorizontalStrength,
): Float? {
    val upEast: Float
    val upNorth: Float
    when (normalizeDegrees(displayRotationDegrees.toFloat()).toInt()) {
        // ROTATION_90 is the device turned 90 deg counter-clockwise, so the natural right edge
        // (+X) is now the top of the screen; ROTATION_270 turns it the other way, making -X the top.
        90 -> {
            upEast = rotationMatrix[0]
            upNorth = rotationMatrix[3]
        }
        180 -> {
            upEast = -rotationMatrix[1]
            upNorth = -rotationMatrix[4]
        }
        270 -> {
            upEast = -rotationMatrix[0]
            upNorth = -rotationMatrix[3]
        }
        else -> {
            upEast = rotationMatrix[1]
            upNorth = rotationMatrix[4]
        }
    }
    val cameraEast = -rotationMatrix[2]
    val cameraNorth = -rotationMatrix[5]

    val upStrength = sqrt(upEast * upEast + upNorth * upNorth)
    val cameraStrength = sqrt(cameraEast * cameraEast + cameraNorth * cameraNorth)
    val upWeight = (upStrength * upStrength) * (upStrength * upStrength)
    var east = 0f
    var north = 0f
    if (upStrength > AxisEpsilon) {
        east += upEast / upStrength * upWeight
        north += upNorth / upStrength * upWeight
    }
    if (cameraStrength > AxisEpsilon) {
        val cameraWeight = 1f - upWeight
        east += cameraEast / cameraStrength * cameraWeight
        north += cameraNorth / cameraStrength * cameraWeight
    }
    if (!east.isFinite() || !north.isFinite()) return null
    if (sqrt(east * east + north * north) < minimumStrength) return null
    return normalizeDegrees(Math.toDegrees(atan2(east.toDouble(), north.toDouble())).toFloat())
}

/** Below this blended horizontal strength the attitude is ambiguous and no heading is reported. */
const val CompassMinimumHorizontalStrength = 0.10f

/**
 * The strength required to *resume* reporting after an ambiguous attitude. Higher than
 * [CompassMinimumHorizontalStrength] so sensor noise right at the singularity keeps the heading
 * unavailable instead of alternating between opposite readings.
 */
const val CompassRecoverHorizontalStrength = 0.30f

private const val AxisEpsilon = 1e-3f

/**
 * Adaptive circular smoothing for compass headings (a One Euro filter on the unwrapped angle,
 * plus an outlier gate).
 *
 * - Filtering happens on the shortest signed delta, so 359°→1° moves 2° through 0°, never the
 *   long way around.
 * - The low-pass cutoff rises with the (filtered) rotation speed: at rest the needle is heavily
 *   damped and steady; while the user actually turns, damping fades and the needle follows with
 *   only a couple of degrees of lag. This is the standard jitter-vs-lag tradeoff resolution for
 *   interactive input (Casiez et al., "1€ Filter", CHI 2012).
 * - A single sample implying an impossible rotation rate is ignored; if several consecutive
 *   samples agree on the new heading it is accepted immediately by re-seeding, so genuine
 *   discontinuities (sensor re-fusion, resume after pause) converge in ~3 samples instead of
 *   dragging through a slow animation.
 * - The first sample seeds the estimate directly: no sweep from north or any other placeholder.
 */
class HeadingFilter(
    private val minCutoffHz: Float = 0.4f,
    private val speedCoefficient: Float = 0.06f,
    private val derivativeCutoffHz: Float = 1.0f,
    private val outlierJumpDegrees: Float = 60f,
    private val outlierMaxSpeedDegreesPerSecond: Float = 500f,
    private val outlierConfirmationSamples: Int = 3,
) {
    private var hasEstimate = false
    private var headingDegrees = 0f
    private var speedDegreesPerSecond = 0f
    private var lastTimestampNanos = 0L
    private var outlierCount = 0
    private var lastOutlierDegrees = 0f

    /** The current filtered heading, or null before the first sample (or after [reset]). */
    val current: Float?
        get() = if (hasEstimate) headingDegrees else null

    fun reset() {
        hasEstimate = false
        speedDegreesPerSecond = 0f
        outlierCount = 0
    }

    fun filter(measurementDegrees: Float, timestampNanos: Long): Float {
        // A non-finite sample must never touch the estimate: NaN passes every `>` gate below
        // (NaN comparisons are false) and would poison the heading permanently.
        if (!measurementDegrees.isFinite()) return headingDegrees
        val measurement = normalizeDegrees(measurementDegrees)
        if (!hasEstimate) {
            seed(measurement, timestampNanos)
            return headingDegrees
        }
        // The accel+mag fallback interleaves two sensor streams whose timestamps are not jointly
        // monotonic; a backwards sample would clamp dt to the minimum and fake a huge speed.
        if (timestampNanos <= lastTimestampNanos) return headingDegrees
        val dtSeconds = ((timestampNanos - lastTimestampNanos) / 1e9)
            .toFloat()
            .coerceIn(MinDtSeconds, MaxDtSeconds)
        lastTimestampNanos = timestampNanos
        val delta = shortestSignedDeltaDegrees(headingDegrees, measurement)

        // Outlier gate: a jump no physical rotation could produce between two samples is a glitch
        // until consecutive samples agree it's the new reality.
        val jumpLimit = maxOf(outlierJumpDegrees, outlierMaxSpeedDegreesPerSecond * dtSeconds)
        if (abs(delta) > jumpLimit) {
            outlierCount = if (
                outlierCount > 0 &&
                angularDistanceDegrees(measurement, lastOutlierDegrees) < OutlierAgreementDegrees
            ) {
                outlierCount + 1
            } else {
                1
            }
            lastOutlierDegrees = measurement
            if (outlierCount >= outlierConfirmationSamples) seed(measurement, timestampNanos)
            return headingDegrees
        }
        outlierCount = 0

        val rawSpeed = delta / dtSeconds
        speedDegreesPerSecond += (rawSpeed - speedDegreesPerSecond) *
            smoothingAlpha(derivativeCutoffHz, dtSeconds)
        val cutoffHz = minCutoffHz + speedCoefficient * abs(speedDegreesPerSecond)
        headingDegrees = normalizeDegrees(headingDegrees + delta * smoothingAlpha(cutoffHz, dtSeconds))
        return headingDegrees
    }

    private fun seed(measurement: Float, timestampNanos: Long) {
        hasEstimate = true
        headingDegrees = measurement
        speedDegreesPerSecond = 0f
        lastTimestampNanos = timestampNanos
        outlierCount = 0
    }

    private fun smoothingAlpha(cutoffHz: Float, dtSeconds: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * cutoffHz)
        return 1f / (1f + tau / dtSeconds)
    }

    private companion object {
        const val MinDtSeconds = 0.001f
        const val MaxDtSeconds = 0.5f
        const val OutlierAgreementDegrees = 20f
    }
}

/**
 * Where the target appears on the dial: degrees clockwise from screen-up. The single definition
 * shared by rendering, alignment, and tests so the sign convention cannot drift.
 */
fun targetDirectionOnScreen(targetBearingDegrees: Float, deviceHeadingDegrees: Float): Float =
    normalizeDegrees(targetBearingDegrees - deviceHeadingDegrees)

enum class AlignmentDirection { Aligned, TurnRight, TurnLeft }

/**
 * Turns the on-screen target direction into user guidance, with hysteresis: alignment engages
 * within [enterDegrees] but persists to [exitDegrees], so filtered noise at the boundary cannot
 * toggle the aligned state (and re-fire its haptic). Returns null — no directional claim at all —
 * when there is no usable heading or the reading is degraded (a degraded compass must not make
 * even a coarse directional claim).
 */
class AlignmentGate(
    private val enterDegrees: Float = 5f,
    private val exitDegrees: Float = 8f,
) {
    private var aligned = false

    fun update(relativeDirectionDegrees: Float?, degraded: Boolean): AlignmentDirection? {
        if (relativeDirectionDegrees == null || degraded) {
            aligned = false
            return null
        }
        val difference = angularDistanceDegrees(relativeDirectionDegrees, 0f)
        val threshold = if (aligned) exitDegrees else enterDegrees
        if (difference <= threshold) {
            aligned = true
            return AlignmentDirection.Aligned
        }
        aligned = false
        return if (normalizeDegrees(relativeDirectionDegrees) <= 180f) {
            AlignmentDirection.TurnRight
        } else {
            AlignmentDirection.TurnLeft
        }
    }
}
