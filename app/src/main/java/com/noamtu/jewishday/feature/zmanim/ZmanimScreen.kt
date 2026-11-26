// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.data.LocationSource
import com.noamtu.jewishday.data.locationSourceForName
import com.noamtu.jewishday.i18n.resolve
import com.noamtu.jewishday.ui.ShowDayRequest
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenHorizontalPadding
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ScreenVerticalPadding
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.theme.LocalGlassTheme

@Composable
fun ZmanimScreen(
    modifier: Modifier = Modifier,
    showDayRequest: ShowDayRequest? = null,
    viewModel: ZmanimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // A widget tap opens the screen on the day the widget shows.
    LaunchedEffect(showDayRequest) { showDayRequest?.let(viewModel::showDay) }

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
