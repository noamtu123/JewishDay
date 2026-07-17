// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import kotlin.math.abs

/**
 * Pure, testable quality reduction for the prayer compass.
 *
 * Only objective platform signals are used: Android's sensor accuracy status and the fused
 * source's own heading-error estimate. Magnetic conditions are left to Android's calibration and
 * accuracy reporting rather than inferred from raw field magnitude.
 */

// SensorManager.SENSOR_STATUS_* values (stable public API constants), mirrored here so this
// file stays JVM-pure. NO_CONTACT = -1, UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3.
const val SensorStatusNoContact = -1
const val SensorStatusUnreliable = 0
const val SensorStatusAccuracyLow = 1
const val SensorStatusAccuracyMedium = 2
const val SensorStatusAccuracyHigh = 3

/** Human-readable form of a SENSOR_STATUS_* value for the developer diagnostics overlay. */
fun sensorStatusLabel(status: Int?): String = when (status) {
    null -> "—"
    SensorStatusNoContact -> "No contact (-1)"
    SensorStatusUnreliable -> "Unreliable (0)"
    SensorStatusAccuracyLow -> "Low (1)"
    SensorStatusAccuracyMedium -> "Medium (2)"
    SensorStatusAccuracyHigh -> "High (3)"
    else -> "Unknown ($status)"
}

/**
 * A read-only snapshot of the compass pipeline internals, populated only while the hidden
 * developer "monitor compass" switch is on. Everything here is what the app actually reads from
 * the Android sensors and derives — the active source, each sensor's reported accuracy status and
 * delivery rate, the fused heading-error estimate, the raw vs. declination-corrected heading, and
 * the quality verdict that drives the needle. It exists purely for on-device diagnosis.
 */
data class CompassDiagnostics(
    val activeSource: String,
    /** SENSOR_STATUS_* of the fused rotation-vector source, or null when not the active source. */
    val fusedStatus: Int?,
    val accelerometerStatus: Int?,
    val magnetometerStatus: Int?,
    /** Fused source's own heading-error estimate (values[4]), in degrees; NaN when unreported. */
    val headingErrorDegrees: Float,
    /** Screen-relative magnetic heading before declination; null at an ambiguous attitude. */
    val magneticHeadingDegrees: Float?,
    /** Filtered, declination-corrected true heading; null before the first usable sample. */
    val trueHeadingDegrees: Float?,
    val declinationDegrees: Float,
    val fusedRateHz: Float,
    val accelerometerRateHz: Float,
    val magnetometerRateHz: Float,
    val needsCalibration: Boolean,
    val lowConfidence: Boolean,
    val unreliable: Boolean,
    /** Android's supplementary accuracy has not arrived yet, so the needle stays cautious. */
    val qualityPending: Boolean,
)

/** Fused heading error (values[4]) above this suppresses directional guidance and alignment. */
const val LowConfidenceHeadingErrorDegrees = 10f

data class CompassQuality(
    /** Android reports degraded accuracy, so show the figure-eight calibration hint. */
    val needsCalibration: Boolean,
    /** The platform says the data cannot be trusted at all — no directional claims. */
    val unreliableStatus: Boolean,
    /** The fused heading's own error estimate is too large for confident alignment. */
    val lowConfidence: Boolean,
)

/**
 * Reduces the platform quality signals to UI-facing flags.
 *
 * [primaryStatus] is the active fused-source accuracy, or the accelerometer in raw fallback;
 * [magnetometerStatus] is the raw/monitor magnetometer status. The worst known status wins: a LOW
 * magnetometer is a genuine
 * calibration signal even while gyro fusion keeps the fused status higher. Unknown (null) statuses
 * make no claim in either direction. [headingErrorDegrees] is the fused source's own error
 * estimate (rotation-vector values[4], in degrees; NaN when unreported) — it signals uncertainty,
 * not a calibration need, so it feeds a separate flag.
 */
fun compassQuality(
    primaryStatus: Int?,
    magnetometerStatus: Int?,
    headingErrorDegrees: Float,
): CompassQuality {
    val worst = listOfNotNull(primaryStatus, magnetometerStatus).minOrNull()
    return CompassQuality(
        needsCalibration = worst != null && worst <= SensorStatusAccuracyLow,
        unreliableStatus = worst != null && worst <= SensorStatusUnreliable,
        lowConfidence = headingErrorDegrees.isFinite() &&
            headingErrorDegrees > LowConfidenceHeadingErrorDegrees,
    )
}

/** Both vectors used by the raw fallback must be present and recent in one elapsed-time domain. */
fun sensorPairIsFresh(
    nowNanos: Long,
    firstSampleNanos: Long,
    secondSampleNanos: Long,
    maxAgeNanos: Long,
    maxSkewNanos: Long = maxAgeNanos,
): Boolean = firstSampleNanos > 0L && secondSampleNanos > 0L &&
    nowNanos >= firstSampleNanos && nowNanos >= secondSampleNanos &&
    nowNanos - firstSampleNanos <= maxAgeNanos &&
    nowNanos - secondSampleNanos <= maxAgeNanos &&
    abs(firstSampleNanos - secondSampleNanos) <= maxSkewNanos

/** Rejects callbacks that predate this registration, arrived stale, or repeat an old sample. */
fun sensorEventIsCurrent(
    nowNanos: Long,
    eventTimestampNanos: Long,
    sourceStartedNanos: Long,
    previousTimestampNanos: Long,
    maxAgeNanos: Long,
): Boolean = sourceStartedNanos > 0L &&
    eventTimestampNanos >= sourceStartedNanos &&
    eventTimestampNanos > previousTimestampNanos &&
    nowNanos >= eventTimestampNanos &&
    nowNanos - eventTimestampNanos <= maxAgeNanos
