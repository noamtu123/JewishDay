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
    val groups: List<ZmanimGroupUi> = emptyList(),
)

/** Date header pinned at the top of the tab: the Jewish date with the Gregorian date beneath. */
@Immutable
data class ZmanimHeaderUi(
    val jewishDate: String,
    val jewishDateHebrew: String,
    val gregorianDate: String,
    val gregorianDateHebrew: String,
)

@Immutable
data class ZmanimGroupUi(
    val key: String,
    val title: String,
    val titleHebrew: String,
    val rows: List<ZmanimRowUi>,
)

@Immutable
data class ZmanimRowUi(
    val key: String,
    val title: String,
    val titleHebrew: String,
    val description: String,
    val descriptionHebrew: String,
    val value: String,
    val valueHebrew: String,
    val valueHebrewOneLineCandidates: List<String> = emptyList(),
)

private data class ZmanimSettingsSnapshot(
    val use24HourTime: Boolean,
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
        ::DailyLearningRequest,
    )
        .distinctUntilChanged()
        .flatMapLatest { request ->
            dailyLearningRepository.learningItems(
                date = request.date,
                inIsrael = request.inIsrael,
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
                .mergeRambamRows()
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

/**
 * When both Rambam Yomi tracks are shown, collapse them into one "Rambam Yomi" entry whose value
 * lists the 1-chapter and 3-chapter references on separate lines, so they read as one section
 * rather than two look-alike rows.
 */
private fun ZmanimDay.mergeRambamRows(): ZmanimDay {
    val merged = groups.map { group ->
        if (group.title != DailyLearningGroupTitle) return@map group
        val one = group.items.firstOrNull { it.id == DailyLearningType.RambamYomi.storageValue }
        val three = group.items.firstOrNull { it.id == DailyLearningType.RambamYomiThreeChapters.storageValue }
        if (one == null || three == null) return@map group
        val mergedRow = one.copy(
            description = "",
            descriptionHebrew = "",
            value = "1 chapter: ${one.value.orEmpty()}\n3 chapters: ${three.value.orEmpty()}",
            valueHebrew = "פרק אחד: ${one.valueHebrew.orEmpty()}\n3 פרקים: ${three.valueHebrew.orEmpty()}",
        )
        val items = group.items.mapNotNull { item ->
            when (item.id) {
                one.id -> mergedRow
                three.id -> null
                else -> item
            }
        }
        group.copy(items = items)
    }
    return copy(groups = merged)
}

private fun ZmanimDay.toUiState(use24HourTime: Boolean): ZmanimUiState {
    val englishLocale = Locale.getDefault()
    val hebrewLocale = Locale.forLanguageTag("he")
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val englishTimeFormatter = DateTimeFormatter.ofPattern(timePattern, englishLocale).withZone(zoneId)
    val hebrewTimeFormatter = DateTimeFormatter.ofPattern(timePattern, hebrewLocale).withZone(zoneId)
    val englishDateFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d", englishLocale)
    val hebrewDateFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", hebrewLocale)

    val uiGroups = groups.mapIndexed { groupIndex, group ->
        ZmanimGroupUi(
            key = "group:$groupIndex:${group.title}",
            title = group.title,
            titleHebrew = group.titleHebrew,
            rows = group.items.mapIndexed { itemIndex, item ->
                item.toUiRow(
                    key = "row:$groupIndex:$itemIndex:${item.title}",
                    englishTimeFormatter = englishTimeFormatter,
                    hebrewTimeFormatter = hebrewTimeFormatter,
                )
            },
        )
    }

    return ZmanimUiState(
        header = ZmanimHeaderUi(
            jewishDate = hebrewDateEnglish,
            jewishDateHebrew = hebrewDateHebrew,
            gregorianDate = date.format(englishDateFormatter),
            gregorianDateHebrew = date.format(hebrewDateFormatter),
        ),
        groups = uiGroups,
    )
}

private fun ZmanItem.toUiRow(
    key: String,
    englishTimeFormatter: DateTimeFormatter,
    hebrewTimeFormatter: DateTimeFormatter,
): ZmanimRowUi {
    val resolvedValueHebrew = valueHebrew ?: time.formatTime(hebrewTimeFormatter)
    val displayValueHebrew = if (id.isRambamId()) {
        resolvedValueHebrew.lines().joinToString("\n", transform = ::rambamLineWithoutHalachot)
    } else {
        resolvedValueHebrew
    }
    return ZmanimRowUi(
        key = key,
        title = title,
        titleHebrew = titleHebrew,
        description = description,
        descriptionHebrew = descriptionHebrew,
        value = value ?: time.formatTime(englishTimeFormatter),
        valueHebrew = displayValueHebrew,
        valueHebrewOneLineCandidates = rambamHebrewOneLineCandidates(displayValueHebrew, id),
    )
}

private fun rambamHebrewOneLineCandidates(valueHebrew: String, id: String?): List<String> {
    if (!id.isRambamId()) return emptyList()
    val lineOptions = valueHebrew.lines().map(::rambamLineOneLineOptions)
    return combinedLineOptions(lineOptions)
        .distinct()
}

private fun String?.isRambamId(): Boolean =
    this == DailyLearningType.RambamYomi.storageValue || this == DailyLearningType.RambamYomiThreeChapters.storageValue

private fun rambamLineWithoutHalachot(line: String): String {
    val prefixEnd = line.indexOf(": ")
    val prefix = if (prefixEnd >= 0) line.substring(0, prefixEnd + 2) else ""
    val reference = if (prefixEnd >= 0) line.substring(prefixEnd + 2) else line
    return prefix + reference.removePrefix("הלכות ").trimStart()
}

private fun rambamLineOneLineOptions(line: String): List<String> {
    val withoutChapterWord = line
        .replace(Regex("\\s+פרקים\\s+"), " ")
        .replace(Regex("\\s+פרק\\s+"), " ")
        .replace(Regex("^פרקים\\s+"), "")
        .replace(Regex("^פרק\\s+"), "")
        .replace(Regex("\\s+"), " ")
        .trim()
    return listOf(line, withoutChapterWord).distinct()
}

private fun combinedLineOptions(lineOptions: List<List<String>>): List<String> {
    val combinations = mutableListOf<Pair<List<Int>, String>>()

    fun build(index: Int, selectedIndexes: List<Int>, selectedLines: List<String>) {
        if (index == lineOptions.size) {
            combinations += selectedIndexes to selectedLines.joinToString("\n")
            return
        }
        lineOptions[index].forEachIndexed { optionIndex, option ->
            build(index + 1, selectedIndexes + optionIndex, selectedLines + option)
        }
    }

    build(index = 0, selectedIndexes = emptyList(), selectedLines = emptyList())
    return combinations
        .sortedWith(compareBy<Pair<List<Int>, String>>(
            { (indexes, _) -> indexes.maxOrNull() ?: 0 },
            { (indexes, _) -> indexes.sum() },
        ))
        .map { (_, value) -> value }
}

private fun Instant?.formatTime(formatter: DateTimeFormatter): String = this?.let(formatter::format) ?: "--"
