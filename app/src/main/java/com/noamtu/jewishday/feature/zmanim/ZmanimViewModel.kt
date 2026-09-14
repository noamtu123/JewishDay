// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.DeveloperOverrides
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.DailyLearningGroupTitle
import com.noamtu.jewishday.model.ShabbatGroupTitle
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
import kotlinx.coroutines.flow.MutableStateFlow
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
    /** How many days from today the screen is showing; 0 is today. */
    val dayOffset: Int = 0,
    /**
     * The hidden developer clock override is pinning "now", so every time on this screen — and the
     * status-bar icon — is simulated. It survives process death and travels in the settings backup,
     * so it must be visible on the screen it falsifies, not only in the developer tools.
     */
    val developerTimeOverrideActive: Boolean = false,
)

/** Date header pinned at the top of the tab: the Jewish date with the Gregorian date beneath. */
@Immutable
data class ZmanimHeaderUi(
    val jewishDate: String,
    val jewishDateHebrew: String,
    val gregorianDate: String,
    val gregorianDateHebrew: String,
    // The name of the location the times were computed for; the UI turns this into a small caption
    // when it isn't a fresh current-location fix (last-known, Jerusalem, or a named place).
    val locationName: String = "",
    val fastName: String? = null,
    val fastNameHebrew: String? = null,
    // Fully written lines: "כניסת הצום 04:10", "יציאת הצום מחר 20:17".
    val fastStart: String? = null,
    val fastStartHebrew: String? = null,
    val fastEnd: String? = null,
    val fastEndHebrew: String? = null,
    val holyDayName: String? = null,
    val holyDayNameHebrew: String? = null,
    // The day's own name ("ערב פסח", "פורים") and, ahead of Shabbat, the coming week's parasha.
    // Both are fallbacks for the chip, behind whatever is actually under way.
    val dayName: String? = null,
    val dayNameHebrew: String? = null,
    val parshaName: String? = null,
    val parshaNameHebrew: String? = null,
    val holyDayStart: String? = null,
    val holyDayStartHebrew: String? = null,
    val holyDayEnd: String? = null,
    val holyDayEndHebrew: String? = null,
    // Set when another holy day begins the moment this one ends.
    val holyDaySequel: String? = null,
    val holyDaySequelHebrew: String? = null,
    val fastLeadsHeader: Boolean = false,
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
    val dayOffset: Int,
)

private data class ZmanimDisplayInput(
    val zmanimDay: ZmanimDay,
    val dailyLearningItems: List<ZmanItem>,
    val use24HourTime: Boolean,
    val enabledZmanimTimes: Set<ZmanimTimeOption>,
    val enabledDailyLearning: Set<DailyLearningType>,
    val showCandleLightingPrompt: Boolean,
    val developerTimeOverrideActive: Boolean,
    val dayOffset: Int,
)

private data class ZmanimTrigger(
    val calculationSettings: ZmanimCalculationSettings,
    val location: JewishLocation,
    val overrides: DeveloperOverrides,
    val dayOffset: Int,
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

    private val _dayOffset = MutableStateFlow(0)

    private val zmanimDay = combine(
        calculationSettings,
        location,
        developerOverrides,
        _dayOffset,
    ) { settings, currentLocation, overrides, dayOffset ->
        ZmanimTrigger(settings, currentLocation, overrides, dayOffset)
    }
        .distinctUntilChanged()
        .conflate()
        // Re-emit at each date boundary so zmanim roll over while the screen stays open.
        .flatMapLatest { trigger ->
            dateBoundaryTicker(clock, trigger.location, trigger.calculationSettings)
                .map { ZmanimCalculationInput(trigger.calculationSettings, trigger.location, trigger.dayOffset) }
        }
        .map { input ->
            jewishDayRepository.getZmanim(
                location = input.location,
                settings = input.calculationSettings,
                dayOffset = input.dayOffset,
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
        developerOverrides,
        _dayOffset,
    ) { day, learning, settings, overrides, dayOffset ->
        ZmanimDisplayInput(
            zmanimDay = day,
            dailyLearningItems = learning,
            use24HourTime = settings.use24HourTime,
            enabledZmanimTimes = settings.enabledZmanimTimes,
            enabledDailyLearning = settings.enabledDailyLearning,
            showCandleLightingPrompt = !settings.candleLightingPromptHandled,
            developerTimeOverrideActive = overrides.timeOverrideEnabled,
            dayOffset = dayOffset,
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
                    developerTimeOverrideActive = input.developerTimeOverrideActive,
                    dayOffset = input.dayOffset,
                )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ZmanimUiState(),
        )

    /** Steps the screen to another day, or back to today with [showToday]. */
    fun stepDay(days: Int) {
        _dayOffset.value = (_dayOffset.value + days).coerceIn(-MaxDayOffset, MaxDayOffset)
    }

    fun showToday() {
        _dayOffset.value = 0
    }

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
            ZmanimGroupTitle, ShabbatGroupTitle -> group.items.filter { it.id == null || it.id in zmanimIds }
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
    developerTimeOverrideActive: Boolean,
    dayOffset: Int,
): ZmanimUiState {
    // Always format the "English" date/time in English regardless of the device locale — otherwise
    // a Hebrew system locale makes Locale.getDefault() render the English header in Hebrew too.
    val englishLocale = Locale.ENGLISH
    val hebrewLocale = Locale.forLanguageTag("he")
    val timePattern = if (use24HourTime) "HH:mm" else "h:mm a"
    val englishTimeFormatter = DateTimeFormatter.ofPattern(timePattern, englishLocale).withZone(zoneId)
    val hebrewTimeFormatter = DateTimeFormatter.ofPattern(timePattern, hebrewLocale).withZone(zoneId)
    // The weekday and the day-of-month come from different days, so they are formatted separately:
    // the weekday belongs to the Jewish day and rolls at tzeit — Thursday evening is already
    // "Friday", along with the Hebrew date above it — while the day-of-month is the calendar date
    // the times below belong to, which turns over at midnight.
    val englishWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", englishLocale)
    val englishDayMonthFormatter = DateTimeFormatter.ofPattern("MMMM d", englishLocale)
    // Hebrew writes the month with a "ב" prefix ("17 ביולי"). CLDR keeps that prefix as a literal
    // in the locale's own date pattern rather than in the month name, so a custom pattern has to
    // carry it explicitly — MMMM alone yields the bare "יולי".
    val hebrewWeekdayFormatter = DateTimeFormatter.ofPattern("EEEE", hebrewLocale)
    val hebrewDayMonthFormatter = DateTimeFormatter.ofPattern("d 'ב'MMMM", hebrewLocale)

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
            gregorianDate = "${displayedDate.format(englishWeekdayFormatter)}, ${date.format(englishDayMonthFormatter)}",
            gregorianDateHebrew = "${displayedDate.format(hebrewWeekdayFormatter)}, ${date.format(hebrewDayMonthFormatter)}",
            locationName = locationName,
            // The fast's name belongs to it only while it is on; the times show a day ahead.
            fastName = fastDayInfo?.takeIf { it.isUnderWay }?.name,
            fastNameHebrew = fastDayInfo?.takeIf { it.isUnderWay }?.nameHebrew,
            fastStart = fastDayInfo?.startTime?.let { observanceLine("Fast starts", it, englishTimeFormatter) },
            fastStartHebrew = fastDayInfo?.startTime?.let { observanceLine("כניסת הצום", it, hebrewTimeFormatter) },
            fastEnd = fastDayInfo?.endTime?.let { observanceLine("Fast ends", it, englishTimeFormatter) },
            fastEndHebrew = fastDayInfo?.endTime?.let { observanceLine("צאת הצום", it, hebrewTimeFormatter) },
            // The name belongs to the holy day only while it is in; the times show a day ahead.
            holyDayName = holyDayInfo?.takeIf { it.isUnderWay }?.name,
            holyDayNameHebrew = holyDayInfo?.takeIf { it.isUnderWay }?.nameHebrew,
            dayName = dayName,
            dayNameHebrew = dayNameHebrew,
            // While Shabbat is only announced — from the moment the entry/exit card appears — the
            // parasha gives the card a heading without claiming Shabbat has begun.
            parshaName = holyDayInfo?.takeUnless { it.isUnderWay }?.parsha,
            parshaNameHebrew = holyDayInfo?.takeUnless { it.isUnderWay }?.parshaHebrew,
            holyDayStart = holyDayInfo?.startTime?.let {
                observanceLine("${holyDayInfo.term} starts", it, englishTimeFormatter)
            },
            holyDayStartHebrew = holyDayInfo?.startTime?.let {
                observanceLine("כניסת ${holyDayInfo.termHebrew}", it, hebrewTimeFormatter)
            },
            holyDayEnd = holyDayInfo?.endTime?.let {
                observanceLine("${holyDayInfo.term} ends", it, englishTimeFormatter)
            },
            holyDayEndHebrew = holyDayInfo?.endTime?.let {
                observanceLine("צאת ${holyDayInfo.termHebrew}", it, hebrewTimeFormatter)
            },
            holyDaySequel = holyDayInfo?.sequel,
            holyDaySequelHebrew = holyDayInfo?.sequelHebrew,
            fastLeadsHeader = fastLeadsHeader,
        ),
        groups = uiGroups,
        showCandleLightingPrompt = showCandleLightingPrompt,
        developerTimeOverrideActive = developerTimeOverrideActive,
        dayOffset = dayOffset,
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

/** One line of an observance card: what the time is, then the time — "צאת חג שני 19:20". */
private fun observanceLine(label: String, time: Instant, formatter: DateTimeFormatter): String =
    "$label ${formatter.format(time)}"

/** A year either way is plenty for looking something up, and keeps the stepper from running off. */
private const val MaxDayOffset = 365
