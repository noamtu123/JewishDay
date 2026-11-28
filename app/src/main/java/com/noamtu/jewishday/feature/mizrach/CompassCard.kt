// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.mizrach

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.AlignmentDirection
import com.noamtu.jewishday.core.model.AlignmentGate
import com.noamtu.jewishday.core.model.MizrachInfo
import com.noamtu.jewishday.core.model.targetDirectionOnScreen
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.spokenSentences
import com.noamtu.jewishday.ui.theme.LocalCelestialLight
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay

@Composable
internal fun CompassCard(
    mizrach: MizrachInfo,
    locationTrust: CompassLocationTrust,
    compassSensorState: State<CompassSensorState>,
    onRequestPreciseLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // derivedStateOf: heading updates arrive up to 50×/s, but these only change
    // composition when the derived value actually flips.
    val locationTrusted = locationTrust == CompassLocationTrust.Trusted
    val hint by remember(compassSensorState, locationTrust) {
        derivedStateOf { compassSensorState.value.accuracyHint(locationTrust) }
    }
    val alignment = rememberAlignmentState(
        bearingDegrees = mizrach.bearingDegreesExact,
        locationTrusted = locationTrusted,
        compassSensorState = compassSensorState,
    )
    VibrateWhenAligned(aligned = alignment?.isAligned == true)
    // What TalkBack says for the dial: where Kodesh HaKodashim is, then which way to turn (or, with
    // no usable heading, why not). It is announced as it changes, but only once it has held for a
    // moment, so a reading wavering around a boundary does not talk over itself.
    val dialDescription = pluralStringResource(
        R.plurals.mizrach_compass_dial_description,
        mizrach.bearingDegrees,
        mizrach.bearingDegrees,
    )
    val guidance = alignment?.label ?: hint?.let { stringResource(it) }
    val spokenState = rememberSettled(spokenSentences(dialDescription, guidance), CompassSpeechSettleMillis)

    InfoCard(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        elevation = 0.dp,
    ) {
        CompassFace(
            bearingDegrees = mizrach.bearingDegrees,
            bearingDegreesExact = mizrach.bearingDegreesExact,
            locationTrusted = locationTrusted,
            sensorStateProvider = { compassSensorState.value },
            alignment = alignment,
            spokenState = spokenState,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.mizrach_compass_caption),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            textAlign = TextAlign.Center,
        )
        // Calibrating helps any phone's compass, so the tip is always there — except while a warning
        // below already asks for the figure-eight, which would say it twice.
        if (hint != R.string.mizrach_compass_accuracy_low && hint != R.string.mizrach_compass_unreliable) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.mizrach_figure_eight_tip),
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
            )
        }
        hint?.let { hintRes ->
            Spacer(Modifier.height(8.dp))
            val message = stringResource(hintRes)
            // The unreliable warning leads with "Compass is unreliable right now"; emphasize that
            // clause (bold + error colour) so it reads as a stronger caution than low accuracy.
            val text = if (hintRes == R.string.mizrach_compass_unreliable) {
                emphasizeLeadClause(message, MaterialTheme.colorScheme.error)
            } else {
                AnnotatedString(message)
            }
            Text(
                text = text,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                textAlign = TextAlign.Center,
            )
        }
        val locationAction = when (locationTrust) {
            CompassLocationTrust.NeedsPrecisePermission -> onRequestPreciseLocation
            CompassLocationTrust.NeedsBetterFix -> onRetryLocation
            CompassLocationTrust.Trusted,
            CompassLocationTrust.DeveloperOverride,
            -> null
        }
        locationAction?.let { action ->
            Spacer(Modifier.height(10.dp))
            Button(onClick = action, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                val label = if (locationTrust == CompassLocationTrust.NeedsPrecisePermission) {
                    stringResource(R.string.mizrach_allow_precise_location)
                } else {
                    stringResource(R.string.mizrach_retry_location)
                }
                Text(label)
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            ValuePill(text = stringResource(R.string.mizrach_distance_km, mizrach.distanceKm))
        }
    }
}

@Composable
private fun CompassFace(
    bearingDegrees: Int,
    bearingDegreesExact: Float,
    locationTrusted: Boolean,
    sensorStateProvider: () -> CompassSensorState,
    alignment: CompassAlignment?,
    spokenState: String,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface.copy(alpha = 0.64f)
    val outline = MaterialTheme.colorScheme.outlineVariant
    val primary = MaterialTheme.colorScheme.primary
    val alignedColor = MaterialTheme.colorScheme.tertiary
    // Glass: once aligned the needle is the sun (or moon) in the sky right now — its colours and a
    // soft halo like its own — rather than a flat accent.
    val celestial = LocalCelestialLight.current
    // On a dark face the needle is the light's bright centre; on a light face, the deeper shade
    // it is seen as, so it stays readable there.
    val onDarkFace = MaterialTheme.colorScheme.background.luminance() < 0.5f
    // The needle and the temple marker it points at are always the same colour.
    val alignedMark = celestial?.let { if (onDarkFace) it.core else it.seen } ?: alignedColor
    val textColor = MaterialTheme.colorScheme.onSecondaryContainer
    val isAligned = alignment?.isAligned == true

    Box(
        // The dial, the degrees and the "turn left" pill are one TalkBack stop, which says what they
        // show together (see [spokenState]) and announces it, politely, when it changes — "Turn left"
        // becoming "Facing Kodesh HaKodashim" is the whole point of the screen for someone who cannot
        // see the needle. The live heading itself is never spoken: it is read in the draw phase on
        // purpose, and pulling it up here would recompose the screen at the sensor rate.
        modifier = modifier.clearAndSetSemantics {
            contentDescription = spokenState
            liveRegion = LiveRegionMode.Polite
        },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier.size(240.dp),
        ) {
            // Sensor state is read here, in the draw phase, so each sensor tick only
            // redraws the needle instead of recomposing the whole screen.
            val sensorState = sensorStateProvider()
            val heading = sensorState.headingDegrees
            // Do not draw an assumed-north needle while waiting for the first sensor reading: it
            // would point incorrectly and then jump. A device with no compass sensor keeps the
            // explicitly documented static-bearing fallback.
            val showNeedle = heading != null || !sensorState.sensorsAvailable
            val live = heading != null && !sensorState.hasLowAccuracy && locationTrusted
            val relativeDirectionDegrees = heading?.let { targetDirectionOnScreen(bearingDegreesExact, it) }
                ?: bearingDegreesExact
            val needleColor = if (isAligned) alignedColor else primary
            // Faded when the reading cannot be trusted. Kept apart from the color so it can be
            // applied to the needle as a whole rather than to each of its shapes.
            val needleAlpha = if (isAligned || live) 1f else FadedNeedleAlpha
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
            if (showNeedle) {
                drawNeedle(
                    color = if (isAligned) alignedMark else needleColor,
                    alpha = needleAlpha,
                    center = center,
                    tip = needleEnd,
                    strokeWidth = 8.dp.toPx(),
                    tipRadius = 9.dp.toPx(),
                    hubRadius = 5.dp.toPx(),
                )
            }
            drawTempleMarker(
                center = Offset(center.x, center.y - radius + 32.dp.toPx()),
                color = if (isAligned) alignedMark else primary,
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
                text = stringResource(R.string.mizrach_toward_jerusalem),
                style = MaterialTheme.typography.bodyMedium,
                color = textColor,
            )
            if (alignment != null) {
                Spacer(Modifier.height(8.dp))
                ValuePill(
                    text = alignment.label,
                    containerColor = if (isAligned) alignedColor else MaterialTheme.colorScheme.surface,
                    contentColor = if (isAligned) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
                )
            }
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

/**
 * The needle: a shaft, a tip and a hub, drawn as one shape.
 *
 * A faded needle has to fade as a whole. Drawing the three parts each at [alpha] lets the shaft
 * show through the circles it runs under, and the overlaps read darker than the rest — so when it
 * is translucent they are composited together in one layer first, and that layer is faded.
 */
private fun DrawScope.drawNeedle(
    color: Color,
    alpha: Float,
    center: Offset,
    tip: Offset,
    strokeWidth: Float,
    tipRadius: Float,
    hubRadius: Float,
) {
    fun drawParts() {
        drawLine(color = color, start = center, end = tip, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        drawCircle(color = color, radius = tipRadius, center = tip)
        drawCircle(color = color, radius = hubRadius, center = center)
    }

    if (alpha >= 1f) {
        drawParts()
        return
    }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), Paint().apply { this.alpha = alpha })
        drawParts()
        canvas.restore()
    }
}

private const val FadedNeedleAlpha = 0.35f

// How long the compass's spoken state must hold before TalkBack announces it.
private const val CompassSpeechSettleMillis = 1_000L

/**
 * [value], but only once it has stayed the same for [settleMillis]; until then, the last value that
 * did. The first value is taken at once.
 */
@Composable
private fun <T> rememberSettled(value: T, settleMillis: Long): T {
    var settled by remember { mutableStateOf(value) }
    LaunchedEffect(value) {
        delay(settleMillis)
        settled = value
    }
    return settled
}

private data class CompassAlignment(
    val isAligned: Boolean,
    val label: String,
)

/**
 * The accuracy message to show under the compass, as an English/Hebrew string-resource pair, or
 * null when the reading is healthy. A degraded reading always fades the needle and suppresses
 * guidance; the message distinguishes an *unreliable* reading (Android says the data can't be
 * trusted — a magnet/metal is interfering) from ordinary low accuracy that a figure-eight fixes.
 */
/**
 * Emphasizes the leading clause (everything before the "—") of a warning: bold and coloured, so
 * the headline caution stands out from the calmer instructions that follow. Falls back to a plain
 * annotated string when no separator is present.
 */
private fun emphasizeLeadClause(message: String, highlightColor: Color): AnnotatedString {
    val separatorIndex = message.indexOf('—')
    if (separatorIndex <= 0) return AnnotatedString(message)
    return buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = highlightColor)) {
            append(message.substring(0, separatorIndex).trim())
        }
        append(' ')
        append(message.substring(separatorIndex))
    }
}

@StringRes
private fun CompassSensorState.accuracyHint(locationTrust: CompassLocationTrust): Int? = when {
    !sensorsAvailable ->
        R.string.mizrach_no_compass_sensor
    // Checked before needsCalibration: an unreliable status also trips needsCalibration
    // (UNRELIABLE ≤ LOW), but it warrants the stronger "can't be trusted" wording.
    unreliableStatus ->
        R.string.mizrach_compass_unreliable
    needsCalibration || lowConfidence ->
        R.string.mizrach_compass_accuracy_low
    headingDegrees == null ->
        R.string.mizrach_heading_unavailable
    locationTrust == CompassLocationTrust.NeedsPrecisePermission ->
        R.string.mizrach_precise_location_required
    locationTrust == CompassLocationTrust.DeveloperOverride ->
        R.string.mizrach_developer_location_compass
    locationTrust == CompassLocationTrust.NeedsBetterFix ->
        R.string.mizrach_location_accuracy_low
    else -> null
}

/**
 * Live guidance toward the target, or null while there is no usable heading (waiting, stale, or
 * missing sensors) — the UI must not claim a direction it doesn't have. Hysteresis and the
 * degraded-reading suppression live in [AlignmentGate].
 */
@Composable
private fun rememberAlignmentState(
    bearingDegrees: Float,
    locationTrusted: Boolean,
    compassSensorState: State<CompassSensorState>,
): CompassAlignment? {
    // Keyed on the bearing: a location refinement resets the hysteresis, so a stale aligned
    // state can't ride the wider exit threshold against a new target direction.
    val gate = remember(bearingDegrees) { AlignmentGate() }
    // Derived: recomposes only when the coarse direction changes, not on every heading tick.
    // Mutating the gate inside derivedStateOf is safe because update() is idempotent for
    // identical inputs — a same-input re-evaluation returns the same result and end state.
    val direction by remember(bearingDegrees, locationTrusted, compassSensorState, gate) {
        derivedStateOf {
            val state = compassSensorState.value
            val degraded = state.hasLowAccuracy || !locationTrusted
            gate.update(
                // Android-reported degraded data gives no guidance at all: even a coarse turn
                // direction can be wrong when a quality signal is degraded.
                relativeDirectionDegrees = state.headingDegrees
                    ?.takeUnless { degraded }
                    ?.let { heading -> targetDirectionOnScreen(bearingDegrees, heading) },
                degraded = degraded,
            )
        }
    }
    val label = when (direction) {
        AlignmentDirection.Aligned -> stringResource(R.string.mizrach_aligned)
        AlignmentDirection.TurnRight -> stringResource(R.string.mizrach_turn_right)
        AlignmentDirection.TurnLeft -> stringResource(R.string.mizrach_turn_left)
        null -> return null
    }
    val isAligned = direction == AlignmentDirection.Aligned
    return remember(isAligned, label) { CompassAlignment(isAligned = isAligned, label = label) }
}

@Composable
private fun VibrateWhenAligned(aligned: Boolean) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    // Buzz on every genuine alignment entry. Angular hysteresis prevents sensor noise from
    // repeatedly crossing the boundary.
    DisposableEffect(vibrator, aligned) {
        if (aligned && vibrator?.hasVibrator() == true) {
            vibrator.vibrate(
                VibrationEffect.createWaveform(
                    longArrayOf(0L, 90L, 70L, 120L),
                    intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                    -1,
                ),
            )
        }
        onDispose {
            if (aligned) vibrator?.cancel()
        }
    }
}
