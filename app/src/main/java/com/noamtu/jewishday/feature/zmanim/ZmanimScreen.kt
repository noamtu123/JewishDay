// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.noamtu.jewishday.ui.theme.LocalGlassTheme
import com.noamtu.jewishday.ui.theme.glassBorder
import com.noamtu.jewishday.ui.theme.glassOr
import com.noamtu.jewishday.ui.theme.glassTint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
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
import com.noamtu.jewishday.i18n.LocalTextResolver
import com.noamtu.jewishday.i18n.resolve
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenHorizontalPadding
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenVerticalPadding
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.components.spokenText

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
        dayOffset = uiState.dayOffset,
        onCandleLightingSelected = viewModel::selectCandleLightingMethod,
        onCandleLightingMinutesEntered = viewModel::selectCandleLightingMinutes,
        onStepDay = viewModel::stepDay,
        onShowToday = viewModel::showToday,
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
    dayOffset: Int,
    onCandleLightingSelected: (CandleLightingMethod) -> Unit,
    onCandleLightingMinutesEntered: (Int) -> Unit,
    onStepDay: (Int) -> Unit,
    onShowToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            DayStepper(
                dayOffset = dayOffset,
                onStepDay = onStepDay,
                onShowToday = onShowToday,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp),
            )
            // When the times aren't from a fresh device fix, note it in tiny print. The Jerusalem
            // fallback is coloured red so it's obvious the times aren't for where you are; a named
            // place (dev preset) is muted. Kept to a single small line so it barely takes space.
            val locationSource = locationSourceForName(header.locationName)
            val locationNote = when (locationSource) {
                LocationSource.CurrentFix -> null
                LocationSource.Jerusalem -> stringResource(R.string.zmanim_location_jerusalem)
                LocationSource.Named -> stringResource(R.string.zmanim_location_named, header.locationName)
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
            val fastStart = header.fastStart?.resolve()
            val fastEnd = header.fastEnd?.resolve()
            if (fastStart != null && fastEnd != null) {
                ObservanceTimesCard(
                    startText = fastStart,
                    endText = fastEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            val holyDayStart = header.holyDayStart?.resolve()
            val holyDayEnd = header.holyDayEnd?.resolve()
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
            val sequel = header.holyDaySequel?.resolve()
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
                    onMinutesEntered = onCandleLightingMinutesEntered,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            val glassTheme = LocalGlassTheme.current
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Whatever sits last above the list — a card, the warning line, or the day stepper
                // with its tall touch targets — already leaves room, and the first group header
                // brings its own top padding, so the usual screen padding would double the gap.
                contentPadding = PaddingValues(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    top = 2.dp,
                    bottom = ScreenVerticalPadding,
                ),
            ) {
                val glass = glassTheme
                groups.forEach { group ->
                    // The day's events have no title of their own.
                    if (group.title != null) {
                        // Pinned while its section scrolls, which needs a solid backing so the rows
                        // do not show through it. Glass has nothing solid to give it, so there the
                        // title scrolls away with its rows instead.
                        if (glass) {
                            item(key = group.key, contentType = "group-header") {
                                ZmanimGroupHeader(group = group)
                            }
                        } else {
                            stickyHeader(key = group.key, contentType = "group-header") {
                                ZmanimGroupHeader(group = group)
                            }
                        }
                    }
                    items(
                        items = group.rows,
                        key = ZmanimRowUi::key,
                        contentType = { "zman-row" },
                    ) { row ->
                        ZmanimRow(row = row)
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
    onMinutesEntered: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var showCustomEntry by rememberSaveable { mutableStateOf(false) }
    var customMinutes by rememberSaveable { mutableStateOf("18") }
    Surface(
        modifier = modifier.clickable(role = Role.Button) { showDialog = true },
        shape = MaterialTheme.shapes.large,
        color = glassOr(MaterialTheme.colorScheme.secondaryContainer),
        border = glassBorder(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.zmanim_candle_prompt_question),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.zmanim_candle_prompt_choose),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(stringResource(R.string.zmanim_candle_prompt_title), modifier = Modifier.semantics { heading() })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.zmanim_candle_prompt_body))
                    Spacer(Modifier.height(4.dp))
                    // The common minhagim only — the typed-in option belongs in Settings, not in a
                    // first-launch question.
                    CandleLightingMethod.PromptOptions.forEach { method ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showDialog = false
                                onSelected(method)
                            },
                        ) {
                            // Non-null for every prompt option, by construction.
                            val minutes = method.offsetMinutes ?: 18
                            Text(pluralStringResource(R.plurals.method_minutes_value, minutes, minutes))
                        }
                    }
                    // The three numbers cover most practices but not all — 18 minutes among them —
                    // so the same question also takes a typed-in answer.
                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            showDialog = false
                            showCustomEntry = true
                        },
                    ) {
                        Text(stringResource(R.string.zmanim_candle_prompt_custom))
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
    if (showCustomEntry) {
        val minutes = customMinutes.toIntOrNull()
        val fieldLabel = stringResource(R.string.a11y_minutes_field)
        AlertDialog(
            onDismissRequest = { showCustomEntry = false },
            title = {
                Text(stringResource(R.string.zmanim_candle_prompt_title), modifier = Modifier.semantics { heading() })
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.zmanim_candle_prompt_body))
                    OutlinedTextField(
                        // The field has no visible label (the question above it is the label), so
                        // TalkBack is told what the number is.
                        modifier = Modifier.semantics { contentDescription = fieldLabel },
                        value = customMinutes,
                        onValueChange = { typed ->
                            if (typed.length <= 3 && typed.all(Char::isDigit)) customMinutes = typed
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = minutes != null,
                    onClick = {
                        minutes?.let(onMinutesEntered)
                        showCustomEntry = false
                    },
                ) {
                    Text(stringResource(R.string.zmanim_candle_prompt_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomEntry = false }) {
                    Text(stringResource(R.string.settings_cancel))
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
            text = stringResource(R.string.zmanim_developer_time_override, ),
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
                        text = stringResource(R.string.zmanim_loading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

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
private fun DayStepper(
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
private fun DateBar(
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
private fun ObservanceTimesCard(
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

@Composable
private fun ZmanimGroupHeader(
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
private fun ZmanimRow(
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