// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.resolve
import com.noamtu.jewishday.ui.components.spokenText
import com.noamtu.jewishday.ui.theme.glassBorder
import com.noamtu.jewishday.ui.theme.glassOr
import com.noamtu.jewishday.ui.theme.glassTint

/**
 * Steps the whole screen a day at a time. The times are the plain calendar day's and turn over at
 * midnight, so looking up tomorrow morning's tefillah on the way to bed is something you ask for
 * rather than something the screen does to you.
 *
 * The arrows follow the layout direction, so in the Hebrew (RTL) layout "previous" sits on the
 * right. The middle says "היום" on today; on any other day it becomes the way back, "חזרה להיום",
 * coloured so a screen showing another day cannot be mistaken for now.
 */
@Composable
internal fun DayStepper(
    dayOffset: Int,
    onStepDay: (Int) -> Unit,
    onShowToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isToday = dayOffset == 0
    val label = if (isToday) {
        stringResource(R.string.zmanim_day_today)
    } else {
        stringResource(R.string.zmanim_day_back_to_today)
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = { onStepDay(-1) }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.zmanim_day_previous),
            )
        }
        TextButton(onClick = onShowToday, enabled = !isToday) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = { onStepDay(1) }) {
            Icon(
                imageVector = ImageVector.vectorResource(R.drawable.ic_arrow_forward),
                contentDescription = stringResource(R.string.zmanim_day_next),
            )
        }
    }
}

@Composable
internal fun DateBar(
    header: ZmanimHeaderUi,
    modifier: Modifier = Modifier,
) {
    // The chip names whichever observance is actually current: a fast and a holy day can be on
    // screen together — Tzom Gedalyah is announced during Rosh Hashana, and a fast can fall on a
    // Friday — and the one merely announced must not steal the name from the one happening now.
    //
    // With nothing under way it falls back to the day's own name ("ערב פסח", "פורים"), which is
    // where those days are said now that they have no row of their own; and failing that, ahead of
    // Shabbat, to the coming week's parasha.
    val holyDayName = header.holyDayName?.resolve()
    val currentFastName = header.fastName?.resolve()
    val underWayName = if (header.fastLeadsHeader) currentFastName ?: holyDayName else holyDayName ?: currentFastName
    val dayName = header.dayName?.resolve()
    val parshaName = header.parshaName?.resolve()
    val chipLabel = underWayName ?: dayName ?: parshaName
    val jewishDate = header.jewishDate.resolve()
    val gregorianDate = header.gregorianDate.resolve()
    val headlineStyle = MaterialTheme.typography.headlineSmall
    val chipLabelStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val largeFont = density.fontScale > 1f
    val textMeasurer = rememberTextMeasurer()
    Surface(
        // One TalkBack stop, the dates first and then the day: in the compact layout the chip is
        // drawn before the dates, which is the order TalkBack would otherwise read them in. A heading,
        // so the screen's top can be jumped to.
        modifier = modifier.clearAndSetSemantics {
            contentDescription = spokenText(jewishDate, gregorianDate, chipLabel)
            heading()
        },
        shape = MaterialTheme.shapes.large,
        color = glassOr(MaterialTheme.colorScheme.primaryContainer),
        border = glassBorder(),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            // Would the prominent Hebrew-date line run past the fast chip's left edge if the chip sat
            // beside it? If so, switch to the "title" layout instead of overlapping.
            val dateOverlapsChip = chipLabel != null && run {
                val contentWidthPx = with(density) { maxWidth.toPx() }
                val dateWidthPx = textMeasurer.measure(jewishDate, headlineStyle).size.width
                val chipTextWidthPx = textMeasurer.measure(chipLabel, chipLabelStyle).size.width
                // Chip span = its text + its horizontal padding (10.dp each side). Require a few dp of
                // real overlap before rearranging, so a near-miss (like a short "Fast of Esther") is
                // left in the compact layout.
                val chipPaddingPx = with(density) { 20.dp.toPx() }
                val minOverlapPx = with(density) { 4.dp.toPx() }
                dateWidthPx + chipTextWidthPx + chipPaddingPx - contentWidthPx > minOverlapPx
            }
            if (chipLabel != null && dateOverlapsChip) {
                // A long label can't fit beside the date, so give the Hebrew date its own line,
                // centered like a title, and put the chip on the day line beside the civil date.
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = jewishDate,
                        style = headlineStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (largeFont) {
                        // Under a larger font size the chip can be wide enough to squeeze the civil
                        // date to nothing beside it, so there it moves under the date instead.
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                modifier = Modifier.align(Alignment.CenterVertically).padding(end = 12.dp),
                                text = gregorianDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            ObservanceChip(
                                label = chipLabel,
                                style = chipLabelStyle,
                                modifier = Modifier.align(Alignment.CenterVertically),
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                modifier = Modifier.weight(1f),
                                text = gregorianDate,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            )
                            Spacer(Modifier.width(12.dp))
                            ObservanceChip(label = chipLabel, style = chipLabelStyle)
                        }
                    }
                }
            } else {
                // Date fits beside the chip: keep the compact layout — date at the start, chip pinned
                // to the far side (the visual left in the RTL Hebrew layout), vertically centered.
                if (chipLabel != null) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        ObservanceChip(label = chipLabel, style = chipLabelStyle)
                    }
                }
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = jewishDate,
                        style = headlineStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = gregorianDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ObservanceChip(
    label: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = glassTint(MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = label,
            style = style,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/**
 * Entry/exit times for the current observance — a fast or Shabbat — in their own small card (like
 * the candle-lighting prompt). Start and end each sit on one line at opposite ends of the card;
 * because the Row follows the layout direction, in Hebrew (RTL) the start is on the right and the
 * end on the left, mirrored in English.
 */
@Composable
internal fun ObservanceTimesCard(
    startText: String,
    endText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        // Start then end as one TalkBack stop, whichever side of the card each is drawn on.
        modifier = modifier.clearAndSetSemantics { contentDescription = spokenText(startText, endText) },
        shape = MaterialTheme.shapes.large,
        color = glassOr(MaterialTheme.colorScheme.secondaryContainer),
        border = glassBorder(),
    ) {
        val rowModifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
        val times: @Composable RowScope.() -> Unit = {
            Text(
                text = startText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = endText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        if (LocalDensity.current.fontScale > 1f) {
            // Under a larger font size the two lines may not fit side by side, and the end would be
            // squeezed off the card; there it moves to a line of its own instead.
            FlowRow(modifier = rowModifier, horizontalArrangement = Arrangement.SpaceBetween) { times() }
        } else {
            Row(
                modifier = rowModifier,
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = times,
            )
        }
    }
}
