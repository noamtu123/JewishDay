// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.i18n.LocalTextResolver
import com.noamtu.jewishday.i18n.resolve
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.spokenText
import com.noamtu.jewishday.ui.theme.LocalGlassTheme
import com.noamtu.jewishday.ui.theme.glassTint

@Composable
internal fun ZmanimGroupHeader(
    group: ZmanimGroupUi,
    modifier: Modifier = Modifier,
) {
    // Opaque background so rows don't bleed through while the header is pinned. In Glass the
    // header is not pinned (see the list), so it is plain text straight on the sky.
    val glass = LocalGlassTheme.current
    Text(
        modifier = modifier
            .fillMaxWidth()
            .then(if (glass) Modifier else Modifier.background(MaterialTheme.colorScheme.background))
            .padding(top = 16.dp, bottom = 6.dp, start = 4.dp, end = 4.dp)
            .semantics { heading() },
        text = group.title?.resolve().orEmpty(),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
internal fun ZmanimRow(
    row: ZmanimRowUi,
    modifier: Modifier = Modifier,
) {
    val resolver = LocalTextResolver.current
    val rawValue = row.value.resolve()
    val description = row.description.resolve()
    // A Rambam Yomi value is trimmed to fit and comes in a narrower form too; any other is as it is.
    val rambam = remember(rawValue, resolver) { if (row.isRambam) rambamRowText(rawValue, resolver.language) else null }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val titleText = row.title.resolve()

    BoxWithConstraints(
        // One TalkBack stop that reads the way it would be said — "Sunrise, 6:42 AM", then the
        // caption saying how it is reckoned — rather than a stop for each text. The value is read
        // whole, even where the screen trims it (a Rambam row) to fit.
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = spokenText(titleText, rawValue, description) },
    ) {
        val valueStyle = MaterialTheme.typography.labelLarge
        val titleStyle = MaterialTheme.typography.bodyLarge
        val rowWidth = with(density) { (maxWidth - 8.dp).roundToPx().coerceAtLeast(0) }
        val pillExtraWidth = with(density) { (12.dp + 24.dp).roundToPx() }
        val titleWidth = textMeasurer.measure(
            text = AnnotatedString(titleText),
            style = titleStyle,
            maxLines = 1,
        ).size.width
        fun textWidth(text: String): Int = textMeasurer.measure(
            text = AnnotatedString(text),
            style = valueStyle,
            maxLines = 1,
        ).size.width
        fun widestLineWidth(candidate: String): Int = candidate.lines().maxOf(::textWidth)

        val candidates = rambam?.candidates ?: listOf(rawValue)
        // Place the value beside the title (like every other daily-learning row) when its widest
        // line fits there — this includes the two-line merged Rambam bubble. Only stack it in a
        // full-width bubble below the title when even the narrowest candidate is too wide.
        val besideTitleValue = candidates.firstOrNull { candidate ->
            titleWidth + widestLineWidth(candidate) + pillExtraWidth <= rowWidth
        }
        val value = besideTitleValue ?: candidates.first()
        val stackBelow = besideTitleValue == null

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titleText,
                    style = titleStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (description.isNotBlank()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (stackBelow) {
                    Spacer(Modifier.height(4.dp))
                    ValueBubble(text = value, valueStyle = valueStyle)
                }
            }
            if (!stackBelow) {
                Spacer(Modifier.width(12.dp))
                // A multi-line value (the merged Rambam bubble) uses a rounded bubble; a single
                // line uses the standard round pill like every other row.
                if (value.contains('\n')) {
                    ValueBubble(text = value, valueStyle = valueStyle)
                } else {
                    ValuePill(text = value)
                }
            }
        }
    }
}

@Composable
private fun ValueBubble(text: String, valueStyle: androidx.compose.ui.text.TextStyle) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = glassTint(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = valueStyle,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}
