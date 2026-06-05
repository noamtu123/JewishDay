package com.turel.jewishdaynext.feature.zmanim

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.ValuePill
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.LocalUseHebrewInterface
import com.turel.jewishdaynext.ui.localizedLocationName
import com.turel.jewishdaynext.ui.localizedString

@Composable
fun ZmanimScreen(
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ZmanimContent(
        header = uiState.header,
        items = uiState.items,
        modifier = modifier,
    )
}

@Composable
private fun ZmanimContent(
    header: ZmanimHeaderUi?,
    items: List<ZmanimListItem>,
    modifier: Modifier = Modifier,
) {
    val useHebrew = LocalUseHebrewInterface.current

    if (header == null) {
        ZmanimLoadingContent(modifier = modifier)
        return
    }

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(
                key = "zmanim-header",
                contentType = "header",
            ) {
                ZmanimHeader(
                    locationName = localizedLocationName(header.locationName),
                    date = if (useHebrew) header.dateHebrew else header.date,
                    zoneId = header.zoneId,
                )
            }
            items(
                items = items,
                key = ZmanimListItem::key,
                contentType = { item -> item.contentType },
            ) { item ->
                when (item) {
                    is ZmanimGroupHeaderUi -> ZmanimGroupHeader(item = item, useHebrew = useHebrew)
                    is ZmanimRowUi -> ZmanimRowCard(
                        row = item,
                        useHebrew = useHebrew,
                        modifier = Modifier.fillMaxWidth(),
                    )
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
    item: ZmanimGroupHeaderUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp, start = 4.dp, end = 4.dp),
        text = if (useHebrew) item.titleHebrew else item.title,
        style = MaterialTheme.typography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ZmanimRowCard(
    row: ZmanimRowUi,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (useHebrew) row.titleHebrew else row.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (useHebrew) row.descriptionHebrew else row.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        ValuePill(text = if (useHebrew) row.valueHebrew else row.value)
    }
}

private val ZmanimListItem.contentType: String
    get() = when (this) {
        is ZmanimGroupHeaderUi -> "group-header"
        is ZmanimRowUi -> "zman-row"
    }
