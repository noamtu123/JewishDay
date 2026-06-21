package com.noamtu.jewishday.feature.zmanim

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
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
            DateBar(
                header = header,
                useHebrew = useHebrew,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 4.dp),
            )
            if (showCandleLightingPrompt) {
                CandleLightingPrompt(
                    useHebrew = useHebrew,
                    onSelected = onCandleLightingSelected,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = ScreenPaddingValues,
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
    useHebrew: Boolean,
    onSelected: (CandleLightingMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDialog by remember { mutableStateOf(false) }
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
                text = if (useHebrew) "כמה דקות לפני שקיעה אתה מקבל שבת?" else "How many minutes before sunset do you welcome Shabbat?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (useHebrew) "בחר" else "Choose",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(if (useHebrew) "קבלת שבת" else "Welcome Shabbat")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (useHebrew) "בחר כמה דקות לפני שקיעה:" else "Choose how many minutes before sunset:")
                    Spacer(Modifier.height(4.dp))
                    CandleLightingMethod.entries.forEach { method ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                showDialog = false
                                onSelected(method)
                            },
                        ) {
                            Text(if (useHebrew) "${method.offsetMinutes} דקות" else "${method.offsetMinutes} minutes")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(if (useHebrew) "ביטול" else "Cancel")
                }
            },
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
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Text(
                text = if (useHebrew) header.jewishDateHebrew else header.jewishDate,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (useHebrew) header.gregorianDateHebrew else header.gregorianDate,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
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
        val fullWidthBubbleTextWidth = with(density) { (maxWidth - 32.dp).roundToPx().coerceAtLeast(0) }
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

        val sidePillValue = if (useHebrew && row.valueHebrewOneLineCandidates.isNotEmpty()) {
            row.valueHebrewOneLineCandidates.firstOrNull { candidate ->
                !candidate.contains('\n') && titleWidth + textWidth(candidate) + pillExtraWidth <= rowWidth
            }
        } else {
            null
        }
        val value = sidePillValue ?: if (useHebrew && row.valueHebrewOneLineCandidates.isNotEmpty()) {
            row.valueHebrewOneLineCandidates.firstOrNull { candidate ->
                candidate.lines().all { line ->
                    textWidth(line) <= fullWidthBubbleTextWidth
                }
            } ?: rawValue
        } else {
            rawValue
        }
        val oneLineCandidateSelected = value != rawValue
        // Long values (e.g. daily-learning references) stack under the title instead of a pill.
        val stackValue = sidePillValue == null && (value.length > 18 || value.contains(" · "))

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
                if (stackValue) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            text = value,
                            style = valueStyle,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = if (oneLineCandidateSelected) value.lines().size else Int.MAX_VALUE,
                        )
                    }
                }
            }
            if (!stackValue) {
                Spacer(Modifier.width(12.dp))
                ValuePill(text = value)
            }
        }
    }
}
