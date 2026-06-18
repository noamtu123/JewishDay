package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedLocationName
import com.noamtu.jewishday.ui.localizedString

@Composable
fun ZmanimScreen(
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ZmanimContent(
        header = uiState.header,
        nextZman = uiState.nextZman,
        groups = uiState.groups,
        modifier = modifier,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ZmanimContent(
    header: ZmanimHeaderUi?,
    nextZman: NextZmanUi?,
    groups: List<ZmanimGroupUi>,
    modifier: Modifier = Modifier,
) {
    val useHebrew = LocalUseHebrewInterface.current

    if (header == null) {
        ZmanimLoadingContent(modifier = modifier)
        return
    }

    ScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.readableWidth().fillMaxSize()) {
            if (nextZman != null) {
                NextZmanBar(
                    nextZman = nextZman,
                    useHebrew = useHebrew,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = ScreenPaddingValues,
            ) {
                item(key = "zmanim-header", contentType = "header") {
                    ZmanimHeader(
                        locationName = localizedLocationName(header.locationName),
                        date = if (useHebrew) header.dateHebrew else header.date,
                        zoneId = header.zoneId,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                groups.forEach { group ->
                    stickyHeader(key = group.key, contentType = "group-header") {
                        ZmanimGroupHeader(group = group, useHebrew = useHebrew)
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
private fun NextZmanBar(
    nextZman: NextZmanUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedString(R.string.zmanim_next_label, R.string.zmanim_next_label_hebrew),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                )
                Text(
                    text = if (useHebrew) nextZman.titleHebrew else nextZman.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = if (useHebrew) nextZman.valueHebrew else nextZman.value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ZmanimHeader(
    locationName: String,
    date: String,
    zoneId: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            text = localizedString(R.string.zmanim_calculated_for, R.string.zmanim_calculated_for_hebrew, locationName),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = localizedString(R.string.zmanim_date_and_zone, R.string.zmanim_date_and_zone_hebrew, date, zoneId),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    val value = if (useHebrew) row.valueHebrew else row.value
    val description = if (useHebrew) row.descriptionHebrew else row.description
    // Long values (e.g. daily-learning references) stack under the title instead of a pill.
    val stackValue = value.length > 18 || value.contains(" · ")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (useHebrew) row.titleHebrew else row.title,
                style = MaterialTheme.typography.bodyLarge,
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
            if (stackValue) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (!stackValue) {
            Spacer(Modifier.width(12.dp))
            ValuePill(text = value)
        }
    }
}
