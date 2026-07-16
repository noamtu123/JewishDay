// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

/**
 * Pure, testable quality reduction for the prayer compass.
 *
 * Only objective platform signals are used: Android's sensor accuracy status and the fused
 * source's own heading-error estimate. Magnetic conditions are left to Android's calibration and
 * accuracy reporting rather than inferred from raw field magnitude.
 */

// SensorManager.SENSOR_STATUS_* values (stable public API constants), mirrored here so this
// file stays JVM-pure. NO_CONTACT = -1, UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3.
const val SensorStatusUnreliable = 0
const val SensorStatusAccuracyLow = 1

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
 * [primaryStatus] is the accuracy of the active heading source (the fused rotation vector, or the
 * magnetometer in the raw fallback); [magnetometerStatus] is the supplementary calibration status
 * when a raw monitor is registered. The worst known status wins: a LOW magnetometer is a genuine
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
): Boolean = firstSampleNanos > 0L && secondSampleNanos > 0L &&
    nowNanos >= firstSampleNanos && nowNanos >= secondSampleNanos &&
    nowNanos - firstSampleNanos <= maxAgeNanos &&
    nowNanos - secondSampleNanos <= maxAgeNanos
