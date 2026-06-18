package com.noamtu.jewishday.feature.zmanim

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.DailyLearningGroupTitle
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.dateBoundaryTicker
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.withDailyLearningItems
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    val includeRambamThreeChapters: Boolean,
    val enabledZmanimTimes: Set<ZmanimTimeOption>,
    val enabledDailyLearning: Set<DailyLearningType>,
    val calculationSettings: ZmanimCalculationSettings,
)

private data class ZmanimCalculationInput(
    val calculationSettings: ZmanimCalculationSettings,
    val location: JewishLocation,
)

private data class ZmanimDisplayInput(
    val zmanimDay: ZmanimDay,
    val dailyLearningItems: List<ZmanItem>,
    val use24HourTime: Boolean,
    val enabledZmanimTimes: Set<ZmanimTimeOption>,
    val enabledDailyLearning: Set<DailyLearningType>,
)

private data class DailyLearningRequest(
    val date: LocalDate,
    val inIsrael: Boolean,
    val includeRambamThreeChapters: Boolean,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ZmanimViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
    dailyLearningRepository: DailyLearningRepository,
    clock: Clock,
) : ViewModel() {
    private val settings = appSettingsRepository.settings
        .map { settings ->
            ZmanimSettingsSnapshot(
                use24HourTime = settings.use24HourTime,
                includeRambamThreeChapters = settings.rambamThreeChaptersEnabled,
                enabledZmanimTimes = settings.enabledZmanimTimes,
                enabledDailyLearning = settings.enabledDailyLearning,
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

    private val includeRambamThreeChapters = settings
        .map { settings -> settings.includeRambamThreeChapters }
        .distinctUntilChanged()

    private val enabledZmanimTimes = settings
        .map { settings -> settings.enabledZmanimTimes }
        .distinctUntilChanged()

    private val enabledDailyLearning = settings
        .map { settings -> settings.enabledDailyLearning }
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
        // Re-emit at each date boundary so zmanim roll over while the screen stays open.
        .flatMapLatest { input ->
            dateBoundaryTicker(clock, input.location, input.calculationSettings)
                .map { input }
        }
        .map { input ->
            jewishDayRepository.getZmanim(
                location = input.location,
                settings = input.calculationSettings,
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .shareIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            replay = 1,
        )

    private val dailyLearningItems = combine(
        zmanimDay.map { day -> day.date }.distinctUntilChanged(),
        calculationSettings.map { settings -> settings.inIsrael }.distinctUntilChanged(),
        includeRambamThreeChapters,
        ::DailyLearningRequest,
    )
        .distinctUntilChanged()
        .flatMapLatest { request ->
            dailyLearningRepository.learningItems(
                date = request.date,
                inIsrael = request.inIsrael,
                includeRambamThreeChapters = request.includeRambamThreeChapters,
            ).onStart { emit(emptyList()) }
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<ZmanimUiState> = combine(
        zmanimDay,
        dailyLearningItems,
        use24HourTime,
        enabledZmanimTimes,
        enabledDailyLearning,
    ) { day, learning, use24, times, limudim ->
        ZmanimDisplayInput(day, learning, use24, times, limudim)
    }
        .distinctUntilChanged()
        .conflate()
        .map { input ->
            input.zmanimDay
                .withDailyLearningItems(input.dailyLearningItems)
                .filterForDisplay(input.enabledZmanimTimes, input.enabledDailyLearning)
                .toUiState(use24HourTime = input.use24HourTime)
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZmanimUiState(),
        )
}

/**
 * Drops the Zmanim rows and Daily-Learning rows the user has hidden, then removes any group
 * left empty. Rows without an id (always-on) and other groups are untouched.
 */
private fun ZmanimDay.filterForDisplay(
    enabledZmanimTimes: Set<ZmanimTimeOption>,
    enabledDailyLearning: Set<DailyLearningType>,
): ZmanimDay {
    val zmanimIds = enabledZmanimTimes.mapTo(mutableSetOf()) { it.storageValue }
    val learningIds = enabledDailyLearning.mapTo(mutableSetOf()) { it.storageValue }
    val filtered = groups.mapNotNull { group ->
        val items = when (group.title) {
            ZmanimGroupTitle -> group.items.filter { it.id == null || it.id in zmanimIds }
            DailyLearningGroupTitle -> group.items.filter { it.id == null || it.id in learningIds }
            else -> group.items
        }
        if (items.isEmpty()) null else group.copy(items = items)
    }
    return copy(groups = filtered)
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
