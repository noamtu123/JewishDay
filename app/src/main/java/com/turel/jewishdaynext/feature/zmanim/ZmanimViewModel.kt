package com.turel.jewishdaynext.feature.zmanim

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.turel.jewishdaynext.data.AppSettingsRepository
import com.turel.jewishdaynext.data.CurrentLocationRepository
import com.turel.jewishdaynext.data.JewishDayRepository
import com.turel.jewishdaynext.model.JewishLocation
import com.turel.jewishdaynext.model.ZmanItem
import com.turel.jewishdaynext.model.ZmanimCalculationSettings
import com.turel.jewishdaynext.model.ZmanimDay
import com.turel.jewishdaynext.model.defaultJerusalemLocation
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

@Immutable
data class ZmanimUiState(
    val header: ZmanimHeaderUi? = null,
    val items: List<ZmanimListItem> = emptyList(),
)

@Immutable
data class ZmanimHeaderUi(
    val locationName: String,
    val date: String,
    val dateHebrew: String,
    val zoneId: String,
)

@Immutable
sealed interface ZmanimListItem {
    val key: String
}

@Immutable
data class ZmanimGroupHeaderUi(
    override val key: String,
    val title: String,
    val titleHebrew: String,
) : ZmanimListItem

@Immutable
data class ZmanimRowUi(
    override val key: String,
    val title: String,
    val titleHebrew: String,
    val description: String,
    val descriptionHebrew: String,
    val value: String,
    val valueHebrew: String,
) : ZmanimListItem

private data class ZmanimSettingsSnapshot(
    val use24HourTime: Boolean,
    val calculationSettings: ZmanimCalculationSettings,
)

private data class ZmanimCalculationInput(
    val calculationSettings: ZmanimCalculationSettings,
    val location: JewishLocation,
)

private data class ZmanimDisplayInput(
    val zmanimDay: ZmanimDay,
    val use24HourTime: Boolean,
)

@HiltViewModel
class ZmanimViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
) : ViewModel() {
    private val settings = appSettingsRepository.settings
        .map { settings ->
            ZmanimSettingsSnapshot(
                use24HourTime = settings.use24HourTime,
                calculationSettings = settings.zmanimSettings,
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )

    private val use24HourTime = settings
        .map { settings -> settings.use24HourTime }
        .distinctUntilChanged()

    private val calculationSettings = settings
        .map { settings -> settings.calculationSettings }
        .distinctUntilChanged()

    private val location = currentLocationRepository.currentLocation
        .map { currentLocation -> currentLocation ?: defaultJerusalemLocation }
        .distinctUntilChanged()

    private val zmanimDay = combine(
        calculationSettings,
        location,
        ::ZmanimCalculationInput,
    )
        .distinctUntilChanged()
        .conflate()
        .map { input ->
            jewishDayRepository.getZmanim(
                location = input.location,
                settings = input.calculationSettings,
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<ZmanimUiState> = combine(
        zmanimDay,
        use24HourTime,
        ::ZmanimDisplayInput,
    )
        .distinctUntilChanged()
        .conflate()
        .map { input ->
            input.zmanimDay.toUiState(use24HourTime = input.use24HourTime)
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZmanimUiState(),
        )
}

private fun ZmanimDay.toUiState(use24HourTime: Boolean): ZmanimUiState {
    val englishLocale = Locale.getDefault()
    val hebrewLocale = Locale.forLanguageTag("he")
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val englishTimeFormatter = DateTimeFormatter.ofPattern(timePattern, englishLocale).withZone(zoneId)
    val hebrewTimeFormatter = DateTimeFormatter.ofPattern(timePattern, hebrewLocale).withZone(zoneId)
    val englishDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", englishLocale)
    val hebrewDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", hebrewLocale)
    val items = buildList(capacity = groups.size + groups.sumOf { group -> group.items.size }) {
        groups.forEachIndexed { groupIndex, group ->
            add(
                ZmanimGroupHeaderUi(
                    key = "group:$groupIndex:${group.title}",
                    title = group.title,
                    titleHebrew = group.titleHebrew,
                ),
            )
            group.items.forEachIndexed { itemIndex, item ->
                add(
                    item.toUiRow(
                        key = "row:$groupIndex:$itemIndex:${item.title}",
                        englishTimeFormatter = englishTimeFormatter,
                        hebrewTimeFormatter = hebrewTimeFormatter,
                    ),
                )
            }
        }
    }

    return ZmanimUiState(
        header = ZmanimHeaderUi(
            locationName = locationName,
            date = date.format(englishDateFormatter),
            dateHebrew = date.format(hebrewDateFormatter),
            zoneId = zoneId.id,
        ),
        items = items,
    )
}

private fun ZmanItem.toUiRow(
    key: String,
    englishTimeFormatter: DateTimeFormatter,
    hebrewTimeFormatter: DateTimeFormatter,
): ZmanimRowUi = ZmanimRowUi(
    key = key,
    title = title,
    titleHebrew = titleHebrew,
    description = description,
    descriptionHebrew = descriptionHebrew,
    value = value ?: time.formatTime(englishTimeFormatter),
    valueHebrew = valueHebrew ?: time.formatTime(hebrewTimeFormatter),
)

private fun Instant?.formatTime(formatter: DateTimeFormatter): String = this?.let(formatter::format) ?: "--"
