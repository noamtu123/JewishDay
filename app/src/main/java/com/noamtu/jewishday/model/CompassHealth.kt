// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

/**
 * Pure, testable quality reduction for the prayer compass.
 *
 * Only *objective platform signals* are used — the sensor accuracy status Android reports and
 * the fused source's own heading-error estimate. There is deliberately no app-side magnetic
 * field heuristic: a magnet's effect on the calibrated magnetometer is indistinguishable from
 * ordinary environment distortion and from the OS re-learning its calibration (a magnet placed
 * directly on the phone gets absorbed into the calibration and then "reappears" after removal),
 * so any magnitude-based detector either cries wolf or gets stuck. When something magnetic
 * genuinely degrades the compass, the platform reports low accuracy and the figure-eight hint
 * covers it.
 */

// SensorManager.SENSOR_STATUS_* values (stable public API constants), mirrored here so this
// file stays JVM-pure. NO_CONTACT = -1, UNRELIABLE = 0, LOW = 1, MEDIUM = 2, HIGH = 3.
const val SensorStatusUnreliable = 0
const val SensorStatusAccuracyLow = 1

/** Fused heading error (values[4]) above this suppresses confident alignment claims. */
const val LowConfidenceHeadingErrorDegrees = 45f

data class CompassQuality(
    /** The platform reports the sensor wants recalibration (the figure-eight hint is honest). */
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
