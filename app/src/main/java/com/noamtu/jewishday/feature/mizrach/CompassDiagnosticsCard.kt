// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.core.model.CompassDiagnostics
import com.noamtu.jewishday.core.model.sensorStatusLabel
import com.noamtu.jewishday.ui.components.InfoCard

/**
 * Developer-only live overlay of the compass pipeline internals, shown when the hidden "Monitor
 * compass" switch is on. English-only, matching the rest of the developer tooling — it exposes the
 * active sensor source, each sensor's Android accuracy status and delivery rate, the fused
 * heading-error estimate, the raw vs. declination-corrected heading, and the derived quality
 * verdict, so magnetic problems can be diagnosed on-device from the platform's own signals.
 */
@Composable
internal fun CompassDiagnosticsCard(
    compassSensorState: State<CompassSensorState>,
    modifier: Modifier = Modifier,
) {
    val diagnostics by remember(compassSensorState) {
        derivedStateOf { compassSensorState.value.diagnostics }
    }
    InfoCard(modifier = modifier) {
        Text(
            text = "Compass monitor (developer)",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(8.dp))
        val current = diagnostics
        if (current == null) {
            Text(
                text = "Waiting for the first sensor reading…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@InfoCard
        }
        DiagnosticsRow("Active source", current.activeSource)
        DiagnosticsRow("Quality verdict", current.qualityVerdict())
        DiagnosticsRow("Heading (true)", current.trueHeadingDegrees.formatDegrees())
        DiagnosticsRow("Heading (magnetic)", current.magneticHeadingDegrees.formatDegrees())
        DiagnosticsRow("Declination", current.declinationDegrees.formatDegrees())
        DiagnosticsRow("Heading error (values[4])", current.headingErrorDegrees.formatDegreesOrUnknown())
        DiagnosticsRow("Rotation-vector status", sensorStatusLabel(current.fusedStatus))
        DiagnosticsRow("Accelerometer status", sensorStatusLabel(current.accelerometerStatus))
        DiagnosticsRow("Magnetometer status", sensorStatusLabel(current.magnetometerStatus))
        DiagnosticsRow("Rotation-vector rate", current.fusedRateHz.formatHz())
        DiagnosticsRow("Accelerometer rate", current.accelerometerRateHz.formatHz())
        DiagnosticsRow("Magnetometer rate", current.magnetometerRateHz.formatHz())
    }
}

private fun CompassDiagnostics.qualityVerdict(): String {
    val flags = buildList {
        if (unreliable) add("unreliable")
        if (needsCalibration) add("needs calibration")
        if (lowConfidence) add("low confidence")
        if (qualityPending) add("awaiting status")
    }
    return if (flags.isEmpty()) "OK (live)" else flags.joinToString(", ")
}

private fun Float?.formatDegrees(): String =
    if (this == null || !this.isFinite()) "—" else String.format(java.util.Locale.US, "%.1f°", this)

private fun Float.formatDegreesOrUnknown(): String =
    if (!isFinite()) "unknown" else String.format(java.util.Locale.US, "%.1f°", this)

private fun Float.formatHz(): String = String.format(java.util.Locale.US, "%.1f Hz", this)

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = label,
            modifier = Modifier.width(150.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
