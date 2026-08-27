// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenHorizontalPadding
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenVerticalPadding
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedString

@Composable
fun ZmanimScreen(
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ZmanimContent(
        header = uiState.header,
        groups = uiState.groups,
        showCandleLightingPrompt = uiState.showCandleLightingPrompt,
        developerTimeOverrideActive = uiState.developerTimeOverrideActive,
        onCandleLightingSelected = viewModel::selectCandleLightingMethod,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZmanimContent(
    header: ZmanimHeaderUi?,
    groups: List<ZmanimGroupUi>,
    showCandleLightingPrompt: Boolean,
    developerTimeOverrideActive: Boolean,
    onCandleLightingSelected: (CandleLightingMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    val useHebrew = LocalUseHebrewInterface.current

    if (header == null) {
        ZmanimLoadingContent(modifier = modifier)
        return
    }

    ScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.readableWidth().fillMaxSize()) {
            // Every time below is simulated while the hidden developer clock is pinned, so say so
            // on the screen itself — the developer tools are the only other place that knows, and
            // the override outlives the session that set it.
            if (developerTimeOverrideActive) {
                DeveloperTimeOverrideBanner(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 12.dp),
                )
            }
            DateBar(
                header = header,
                useHebrew = useHebrew,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            // When the times aren't from a fresh device fix, note it in tiny print. The Jerusalem
            // fallback is coloured red so it's obvious the times aren't for where you are; a named
            // place (dev preset) is muted. Kept to a single small line so it barely takes space.
            val locationSource = locationSourceForName(header.locationName)
            val locationNote = when (locationSource) {
                LocationSource.CurrentFix -> null
                LocationSource.Jerusalem -> localizedString(R.string.zmanim_location_jerusalem, R.string.zmanim_location_jerusalem_hebrew)
                LocationSource.Named -> localizedString(R.string.zmanim_location_named, R.string.zmanim_location_named_hebrew, header.locationName)
            }
            if (locationNote != null) {
                Text(
                    text = locationNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (locationSource == LocationSource.Jerusalem) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp),
                )
            }
            val fastStart = if (useHebrew) header.fastStartHebrew else header.fastStart
            val fastEnd = if (useHebrew) header.fastEndHebrew else header.fastEnd
            if (fastStart != null && fastEnd != null) {
                ObservanceTimesCard(
                    startText = fastStart,
                    endText = fastEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            val holyDayStart = if (useHebrew) header.holyDayStartHebrew else header.holyDayStart
            val holyDayEnd = if (useHebrew) header.holyDayEndHebrew else header.holyDayEnd
            // A fast on a Friday overlaps Shabbat but is a genuinely different window, so both
            // cards show. Identical windows would just repeat, so they collapse to one.
            val holyDayRepeatsFast = holyDayStart == fastStart && holyDayEnd == fastEnd
            if (holyDayStart != null && holyDayEnd != null && !holyDayRepeatsFast) {
                ObservanceTimesCard(
                    startText = holyDayStart,
                    endText = holyDayEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            // Warns that another observance begins the instant this one ends. Kept as its own
            // line rather than inside a card, so the cards stay one row tall.
            val sequel = if (useHebrew) header.holyDaySequelHebrew else header.holyDaySequel
            if (sequel != null) {
                Text(
                    text = sequel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 6.dp),
                )
            }
            if (showCandleLightingPrompt) {
                CandleLightingPrompt(
                    onSelected = onCandleLightingSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // The warning line already sits close under the cards, and the first group header
                // brings its own top padding, so the usual screen padding would double the gap.
                contentPadding = PaddingValues(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    top = if (sequel != null) 4.dp else ScreenVerticalPadding,
                    bottom = ScreenVerticalPadding,
                ),
            ) {
                groups.forEach { group ->
                    val title = if (useHebrew) group.titleHebrew else group.title
                    if (title.isNotBlank()) {
                        stickyHeader(key = group.key, contentType = "group-header") {
                            ZmanimGroupHeader(group = group, useHebrew = useHebrew)
                        }
                    }
                    items(
                        items = group.rows,
                        key = ZmanimRowUi::key,
                        contentType = { "zman-row" },
                    ) { row ->
                        ZmanimRow(row = row, useHebrew = useHebrew)
                        if (row !== group.rows.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandleLightingPrompt(
    onSelected: (CandleLightingMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier.clickable { showDialog = true },
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = localizedString(R.string.zmanim_candle_prompt_question, R.string.zmanim_candle_prompt_question_hebrew),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = localizedString(R.string.zmanim_candle_prompt_choose, R.string.zmanim_candle_prompt_choose_hebrew),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(localizedString(R.string.zmanim_candle_prompt_title, R.string.zmanim_candle_prompt_title_hebrew))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(localizedString(R.string.zmanim_candle_prompt_body, R.string.zmanim_candle_prompt_body_hebrew))
                    Spacer(Modifier.height(4.dp))
                    CandleLightingMethod.entries.forEach { method ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showDialog = false
                                onSelected(method)
                            },
                        ) {
                            Text(
                                localizedString(
                                    R.string.zmanim_candle_prompt_minutes,
                                    R.string.zmanim_candle_prompt_minutes_hebrew,
                                    method.offsetMinutes,
                                ),
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(localizedString(R.string.settings_cancel, R.string.settings_cancel_hebrew))
                }
            },
        )
    }
}

/** Loud, unmissable warning that the hidden developer clock is falsifying every time on screen. */
@Composable
private fun DeveloperTimeOverrideBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            text = localizedString(
                R.string.zmanim_developer_time_override,
                R.string.zmanim_developer_time_override_hebrew,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun ZmanimLoadingContent(modifier: Modifier = Modifier) {
    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localizedString(R.string.zmanim_loading, R.string.zmanim_loading_hebrew),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun DateBar(
    header: ZmanimHeaderUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    // The chip names whichever observance is actually current: a fast and a holy day can be on
    // screen together — Tzom Gedalyah is announced during Rosh Hashana, and a fast can fall on a
    // Friday — and the one merely announced must not steal the name from the one happening now.
    val holyDayName = if (useHebrew) header.holyDayNameHebrew else header.holyDayName
    val currentFastName = if (useHebrew) header.fastNameHebrew else header.fastName
    val fastName = if (header.fastLeadsHeader) currentFastName ?: holyDayName else holyDayName ?: currentFastName
    val jewishDate = if (useHebrew) header.jewishDateHebrew else header.jewishDate
    val headlineStyle = MaterialTheme.typography.headlineSmall
    val chipLabelStyle = MaterialTheme.typography.labelLarge
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            val gregorianDate = if (useHebrew) header.gregorianDateHebrew else header.gregorianDate
            // Would the prominent Hebrew-date line run past the fast chip's left edge if the chip sat
            // beside it? If so, switch to the "title" layout instead of overlapping.
            val dateOverlapsChip = fastName != null && run {
                val contentWidthPx = with(density) { maxWidth.toPx() }
                val dateWidthPx = textMeasurer.measure(jewishDate, headlineStyle).size.width
                val chipTextWidthPx = textMeasurer.measure(fastName, chipLabelStyle).size.width
                // Chip span = its text + its horizontal padding (10.dp each side). Require a few dp of
                // real overlap before rearranging, so a near-miss (like a short "Fast of Esther") is
                // left in the compact layout.
                val chipPaddingPx = with(density) { 20.dp.toPx() }
                val minOverlapPx = with(density) { 4.dp.toPx() }
                dateWidthPx + chipTextWidthPx + chipPaddingPx - contentWidthPx > minOverlapPx
            }
            if (fastName != null && dateOverlapsChip) {
                // A long fast name can't fit beside the date, so give the Hebrew date its own line,
                // centered like a title, and put the fast chip on the day line beside the civil date.
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = jewishDate,
                        style = headlineStyle,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
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
                        FastNameChip(fastName = fastName, style = chipLabelStyle)
                    }
                }
            } else {
                // Date fits beside the chip: keep the compact layout — date at the start, chip pinned
                // to the far side (the visual left in the RTL Hebrew layout), vertically centered.
                if (fastName != null) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        FastNameChip(fastName = fastName, style = chipLabelStyle)
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
private fun FastNameChip(
    fastName: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            text = fastName,
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
private fun ObservanceTimesCard(
    startText: String,
    endText: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
    }
}

@Composable
private fun ZmanimGroupHeader(
    group: ZmanimGroupUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    // Opaque background so rows don't bleed through while the header is pinned.
    Text(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 16.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
        text = if (useHebrew) group.titleHebrew else group.title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ZmanimRow(
    row: ZmanimRowUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    val rawValue = if (useHebrew) row.valueHebrew else row.value
    val description = if (useHebrew) row.descriptionHebrew else row.description
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val valueStyle = MaterialTheme.typography.labelLarge
        val titleStyle = MaterialTheme.typography.bodyLarge
        val rowWidth = with(density) { (maxWidth - 8.dp).roundToPx().coerceAtLeast(0) }
        val pillExtraWidth = with(density) { (12.dp + 24.dp).roundToPx() }
        val titleText = if (useHebrew) row.titleHebrew else row.title
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

        val candidates = (if (useHebrew) row.valueHebrewCandidates else row.valueCandidates)
            .ifEmpty { listOf(rawValue) }
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
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            text = text,
            style = valueStyle,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}