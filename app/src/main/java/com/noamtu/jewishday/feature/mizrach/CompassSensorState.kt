// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import com.noamtu.jewishday.core.model.CompassDiagnostics

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
    /** Supplementary Android accuracy has not arrived yet; remain visually cautious. */
    val qualityPending: Boolean = false,
    /** Live pipeline internals for the developer overlay; null unless monitoring is enabled. */
    val diagnostics: CompassDiagnostics? = null,
) {
    /** Anything that must block *confident* alignment (green needle, haptic). */
    val hasLowAccuracy: Boolean
        get() = unreliableStatus || needsCalibration || lowConfidence || qualityPending
}
