package com.noamtu.jewishday.feature.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.model.JewishDayInfo
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.ValuePill
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.localizedString
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    TodayContent(
        dayInfo = uiState.dayInfo,
        preferHebrewDates = uiState.preferHebrewDates,
        modifier = modifier,
    )
}

@Composable
private fun TodayContent(
    dayInfo: JewishDayInfo?,
    preferHebrewDates: Boolean,
    modifier: Modifier = Modifier,
) {
    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                TodayHero(modifier = Modifier.fillMaxWidth())
            }
            item {
                if (dayInfo == null) {
                    TodayLoadingCard(modifier = Modifier.fillMaxWidth())
                } else {
                    JewishDateCard(
                        dayInfo = dayInfo,
                        preferHebrewDates = preferHebrewDates,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayHero(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = localizedString(R.string.today_title, R.string.today_title_hebrew),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = localizedString(R.string.today_subtitle, R.string.today_subtitle_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TodayLoadingCard(modifier: Modifier = Modifier) {
    InfoCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = 0.dp,
    ) {
        Text(
            text = localizedString(R.string.today_loading, R.string.today_loading_hebrew),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun JewishDateCard(
    dayInfo: JewishDayInfo,
    preferHebrewDates: Boolean,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val useHebrew = LocalUseHebrewInterface.current
    val displayLocale = if (useHebrew) Locale.forLanguageTag("he") else locale
    val formatter = remember(displayLocale) {
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", displayLocale)
    }
    val gregorianDate = dayInfo.gregorianDate.format(formatter)
    val primaryHebrewDate = if (preferHebrewDates) dayInfo.hebrewDateHebrew else dayInfo.hebrewDateEnglish
    val secondaryHebrewDate = if (preferHebrewDates) dayInfo.hebrewDateEnglish else dayInfo.hebrewDateHebrew

    InfoCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        elevation = 0.dp,
    ) {
        Text(
            text = localizedString(R.string.today_overline, R.string.today_overline_hebrew),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = primaryHebrewDate,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = secondaryHebrewDate,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        Spacer(modifier = Modifier.height(22.dp))
        ValuePill(
            text = gregorianDate,
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSurface,
        )
    }
}
