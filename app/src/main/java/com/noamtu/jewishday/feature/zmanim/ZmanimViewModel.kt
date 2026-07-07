package com.noamtu.jewishday.feature.zmanim

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.DailyLearningGroupTitle
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.isInIsrael
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupTitle
import com.noamtu.jewishday.model.ZmanimPreset
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ZmanimUiState(
    val header: ZmanimHeaderUi? = null,
    val groups: List<ZmanimGroupUi> = emptyList(),
    val showCandleLightingPrompt: Boolean = false,
)

/** Date header pinned at the top of the tab: the Jewish date with the Gregorian date beneath. */
@Immutable
data class ZmanimHeaderUi(
    val jewishDate: String,
    val jewishDateHebrew: String,
    val gregorianDate: String,
    val gregorianDateHebrew: String,
    val fastName: String? = null,
    val fastNameHebrew: String? = null,
    val fastStart: String? = null,
    val fastStartHebrew: String? = null,
    val fastEnd: String? = null,
    val fastEndHebrew: String? = null,
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
    // Alternative renderings of the value (most informative first), used to decide whether it fits
    // beside the title; currently only the merged Rambam Yomi row provides more than one.
    val valueCandidates: List<String> = emptyList(),
    val valueHebrewCandidates: List<String> = emptyList(),
)

private data class ZmanimSettingsSnapshot(
    val use24HourTime: Boolean,
    val enabledZmanimTimes: Set<ZmanimTimeOption>,
    val enabledDailyLearning: Set<DailyLearningType>,
    val calculationSettings: ZmanimCalculationSettings,
    val candleLightingPromptHandled: Boolean,
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
    val showCandleLightingPrompt: Boolean,
)

private data class DailyLearningRequest(
    val date: LocalDate,
    val inIsrael: Boolean,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ZmanimViewModel @Inject constructor(
    jewishDayRepository: JewishDayRepository,
    private val appSettingsRepository: AppSettingsRepository,
    currentLocationRepository: CurrentLocationRepository,
    dailyLearningRepository: DailyLearningRepository,
    developerOverridesRepository: DeveloperOverridesRepository,
    clock: Clock,
) : ViewModel() {
    private val settings = appSettingsRepository.settings
        .map { settings ->
            ZmanimSettingsSnapshot(
                use24HourTime = settings.use24HourTime,
                enabledZmanimTimes = settings.enabledZmanimTimes,
                enabledDailyLearning = settings.enabledDailyLearning,
                calculationSettings = settings.zmanimSettings,
                candleLightingPromptHandled = settings.candleLightingPromptHandled,
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

    // The developer time override changes the injected Clock; fold its state into the recompute
    // trigger so picking a date/time in the developer tools refreshes the screen immediately.
    // (StateFlow is already conflated/distinct, so no distinctUntilChanged here.)
    private val developerOverrides = developerOverridesRepository.state

    private val zmanimDay = combine(
        calculationSettings,
        location,
        developerOverrides,
    ) { settings, currentLocation, overrides ->
        Triple(settings, currentLocation, overrides)
    }
        .distinctUntilChanged()
        .conflate()
        // Re-emit at each date boundary so zmanim roll over while the screen stays open.
        .flatMapLatest { (settings, currentLocation, _) ->
            dateBoundaryTicker(clock, currentLocation, settings)
                .map { ZmanimCalculationInput(settings, currentLocation) }
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
        location.map { it.isInIsrael }.distinctUntilChanged(),
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
        settings,
    ) { day, learning, settings ->
        ZmanimDisplayInput(
            zmanimDay = day,
            dailyLearningItems = learning,
            use24HourTime = settings.use24HourTime,
            enabledZmanimTimes = settings.enabledZmanimTimes,
            enabledDailyLearning = settings.enabledDailyLearning,
            showCandleLightingPrompt = !settings.candleLightingPromptHandled,
        )
    }
        .distinctUntilChanged()
        .conflate()
        .map { input ->
            input.zmanimDay
                .withDailyLearningItems(input.dailyLearningItems)
                .filterForDisplay(input.enabledZmanimTimes, input.enabledDailyLearning)
                .mergeRambamRows()
                .toUiState(
                    use24HourTime = input.use24HourTime,
                    showCandleLightingPrompt = input.showCandleLightingPrompt,
                )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZmanimUiState(),
        )

    fun selectCandleLightingMethod(method: CandleLightingMethod) {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first()
            appSettingsRepository.setZmanimSettings(
                current.zmanimSettings.copy(
                    preset = ZmanimPreset.Custom,
                    candleLightingMethod = method,
                ),
            )
            appSettingsRepository.setCandleLightingPromptHandled(true)
            // Remember the first-launch choice as the candle-lighting default (shown in the
            // picker and restored by Reset).
            appSettingsRepository.setCandleLightingDefault(method)
        }
    }
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
            description = "Daily Rambam cycle",
            descriptionHebrew = "הרמב״ם היומי",
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

private fun ZmanimDay.toUiState(
    use24HourTime: Boolean,
    showCandleLightingPrompt: Boolean,
): ZmanimUiState {
    // Always format the "English" date/time in English regardless of the device locale — otherwise
    // a Hebrew system locale makes Locale.getDefault() render the English header in Hebrew too.
    val englishLocale = Locale.ENGLISH
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
            fastName = fastDayInfo?.name,
            fastNameHebrew = fastDayInfo?.nameHebrew,
            fastStart = fastDayInfo?.startTime.formatTime(englishTimeFormatter).takeIf { fastDayInfo != null },
            fastStartHebrew = fastDayInfo?.startTime.formatTime(hebrewTimeFormatter).takeIf { fastDayInfo != null },
            fastEnd = fastDayInfo?.endTime.formatTime(englishTimeFormatter).takeIf { fastDayInfo != null },
            fastEndHebrew = fastDayInfo?.endTime.formatTime(hebrewTimeFormatter).takeIf { fastDayInfo != null },
        ),
        groups = uiGroups,
        showCandleLightingPrompt = showCandleLightingPrompt,
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
        valueCandidates = rambamValueCandidates(value ?: time.formatTime(englishTimeFormatter), id, chapterWords = listOf("chapters", "chapter")),
        valueHebrewCandidates = rambamValueCandidates(displayValueHebrew, id, chapterWords = listOf("פרקים", "פרק")),
    )
}

/**
 * Rendering candidates for a merged Rambam Yomi value ("פרק אחד: <ref1>\n3 פרקים: <ref2>"). The
 * "פרק אחד"/"3 פרקים" labels are always kept; the only variation offered is dropping the inner
 * "פרק"/"chapter" word from the references so the (still two-line) bubble is narrow enough to sit
 * beside the title when the halacha names are short. The row places whichever candidate fits beside
 * the title there, and stacks the full one below only when neither fits.
 */
private fun rambamValueCandidates(value: String, id: String?, chapterWords: List<String>): List<String> {
    if (!id.isRambamId()) return emptyList()
    val lines = value.lines().map { rambamPrefixAndReference(it) }
    val full = lines.joinToString("\n") { (label, reference) -> label + reference }
    val abbreviated = lines.joinToString("\n") { (label, reference) -> label + abbreviateRambamReference(reference, chapterWords) }
    return listOf(full, abbreviated).distinct()
}

private fun String?.isRambamId(): Boolean =
    this == DailyLearningType.RambamYomi.storageValue || this == DailyLearningType.RambamYomiThreeChapters.storageValue

private fun rambamLineWithoutHalachot(line: String): String {
    val (label, reference) = rambamPrefixAndReference(line)
    return label + reference.removePrefix("הלכות ").trimStart()
}

/** Splits a Rambam line into its "פרק אחד: "/"3 פרקים: " label (empty when unmerged) and reference. */
private fun rambamPrefixAndReference(line: String): Pair<String, String> {
    val prefixEnd = line.indexOf(": ")
    return if (prefixEnd >= 0) {
        line.substring(0, prefixEnd + 2) to line.substring(prefixEnd + 2)
    } else {
        "" to line
    }
}

/** Drops the "chapter"/"פרק" word from a reference (e.g. "שבת פרק כח" -> "שבת כח") so it fits. */
private fun abbreviateRambamReference(reference: String, chapterWords: List<String>): String {
    var result = reference
    for (word in chapterWords) {
        result = result
            .replace(Regex("\\s+" + Regex.escape(word) + "\\s+"), " ")
            .replace(Regex("^" + Regex.escape(word) + "\\s+"), "")
    }
    return result.replace(Regex("\\s+"), " ").trim()
}

private fun Instant?.formatTime(formatter: DateTimeFormatter): String = this?.let(formatter::format) ?: "--"
