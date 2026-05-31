package com.turel.jewishdaynext.feature.zmanim

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.turel.jewishdaynext.R
import com.turel.jewishdaynext.model.ZmanItem
import com.turel.jewishdaynext.model.ZmanimDay
import com.turel.jewishdaynext.model.ZmanimGroup
import com.turel.jewishdaynext.ui.components.InfoCard
import com.turel.jewishdaynext.ui.components.ScreenPaddingValues
import com.turel.jewishdaynext.ui.components.ScreenSurface
import com.turel.jewishdaynext.ui.components.ValuePill
import com.turel.jewishdaynext.ui.components.readableWidth
import com.turel.jewishdaynext.ui.LocalUseHebrewInterface
import com.turel.jewishdaynext.ui.localizedLocationName
import com.turel.jewishdaynext.ui.localizedString
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ZmanimScreen(
    modifier: Modifier = Modifier,
    viewModel: ZmanimViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ZmanimContent(
        zmanim = uiState.zmanimDay,
        use24HourTime = uiState.use24HourTime,
        modifier = modifier,
    )
}

@Composable
private fun ZmanimContent(
    zmanim: ZmanimDay,
    use24HourTime: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val useHebrew = LocalUseHebrewInterface.current
    val displayLocale = if (useHebrew) Locale.forLanguageTag("he") else locale
    val timeFormatter = remember(displayLocale, zmanim.zoneId, use24HourTime) {
        DateTimeFormatter
            .ofPattern(if (use24HourTime) "HH:mm" else "h:mm a", displayLocale)
            .withZone(zmanim.zoneId)
    }
    val dateFormatter = remember(displayLocale) {
        DateTimeFormatter.ofPattern("EEEE, MMMM d", displayLocale)
    }

    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                ZmanimHeader(
                    locationName = localizedLocationName(zmanim.locationName),
                    date = zmanim.date.format(dateFormatter),
                    zoneId = zmanim.zoneId.id,
                )
            }
            items(
                items = zmanim.groups,
                key = { group -> group.title },
            ) { group ->
                ZmanimGroupCard(
                    group = group,
                    timeFormatter = timeFormatter,
                    useHebrew = useHebrew,
                    modifier = Modifier.fillMaxWidth(),
                )
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
private fun ZmanimGroupCard(
    group: ZmanimGroup,
    timeFormatter: DateTimeFormatter,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    InfoCard(modifier = modifier) {
        Text(
            text = if (useHebrew) group.titleHebrew else group.title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(10.dp))
        group.items.forEachIndexed { index, item ->
            ZmanRow(
                item = item,
                formatter = timeFormatter,
                useHebrew = useHebrew,
            )
            if (index != group.items.lastIndex) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun ZmanRow(
    item: ZmanItem,
    formatter: DateTimeFormatter,
    useHebrew: Boolean,
    modifier: Modifier = Modifier,
) {
    val value = if (useHebrew) item.valueHebrew else item.value
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (useHebrew) item.titleHebrew else item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (useHebrew) item.descriptionHebrew else item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        ValuePill(text = value ?: item.time.formatTime(formatter))
    }
}

private fun Instant?.formatTime(formatter: DateTimeFormatter): String = this?.let(formatter::format) ?: "--"
