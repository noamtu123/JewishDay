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
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.DateStyle
import com.noamtu.jewishday.i18n.LanguageFormatter
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.pluralText
import com.noamtu.jewishday.i18n.uiText
import com.noamtu.jewishday.i18n.verbatim
import com.noamtu.jewishday.model.CandleLightingMethod
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.JewishLocation
import com.noamtu.jewishday.model.isInIsrael
import com.noamtu.jewishday.model.ZmanItem
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.ZmanimDay
import com.noamtu.jewishday.model.ZmanimGroupKind
import com.noamtu.jewishday.model.ZmanimTimeOption
import com.noamtu.jewishday.model.dateBoundaryTicker
import com.noamtu.jewishday.model.defaultJerusalemLocation
import com.noamtu.jewishday.model.withDailyLearningItems
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
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

/**
 * Date header pinned at the top of the tab: the Jewish date with the Gregorian date beneath. Like the
 * rest of the state it holds [UiText], worded by the screen in the app's current language — so a
 * language change needs no new state, and the view model never holds a sentence.
 */
@Immutable
data class ZmanimHeaderUi(
    val jewishDate: UiText,
    val gregorianDate: UiText,
    // The name of the location the times were computed for; the UI turns this into a small caption
    // when it isn't a fresh current-location fix (last-known, Jerusalem, or a named place).
    val locationName: String = "",
    val fastName: UiText? = null,
    // Fully written lines: "כניסת הצום 04:10", "יציאת הצום מחר 20:17".
    val fastStart: UiText? = null,
    val fastEnd: UiText? = null,
    val holyDayName: UiText? = null,
    // The day's own name ("ערב פסח", "פורים") and, ahead of Shabbat, the coming week's parasha.
    // Both are fallbacks for the chip, behind whatever is actually under way.
    val dayName: UiText? = null,
    val parshaName: UiText? = null,
    val holyDayStart: UiText? = null,
    val holyDayEnd: UiText? = null,
    // Set when another holy day begins the moment this one ends.
    val holyDaySequel: UiText? = null,
    val fastLeadsHeader: Boolean = false,
)

@Immutable
data class ZmanimGroupUi(
    val key: String,
    // Null for the untitled group of the day's events.
    val title: UiText?,
    val rows: List<ZmanimRowUi>,
)

@Immutable
data class ZmanimRowUi(
    val key: String,
    val title: UiText,
    val description: UiText,
    val value: UiText,
    // A Rambam Yomi row, whose value the screen trims to fit (see [rambamRowText]).
    val isRambam: Boolean = false,
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
            developerTimeOverrideActive = overrides.timeOverrideEnabled && !overrides.hideTimeOverrideBanner,
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
                    candleLightingMethod = method,
                ),
            )
            appSettingsRepository.setCandleLightingPromptHandled(true)
            // Remember the first-launch choice as the candle-lighting default (shown in the
            // picker and restored by Reset).
            appSettingsRepository.setCandleLightingDefault(method)
        }
    }

    /**
     * A typed-in answer to the same question, for the practices the three offered numbers do not
     * cover — 18 minutes among them.
     */
    fun selectCandleLightingMinutes(minutes: Int) {
        viewModelScope.launch {
            val current = appSettingsRepository.settings.first()
            appSettingsRepository.setZmanimSettings(
                current.zmanimSettings.copy(
                    candleLightingMethod = CandleLightingMethod.Custom,
                    candleLightingCustomMinutes = minutes.coerceIn(0, 120),
                ),
            )
            appSettingsRepository.setCandleLightingPromptHandled(true)
            appSettingsRepository.setCandleLightingDefault(CandleLightingMethod.Custom)
        }
    }
}

/**
 * Drops the Zmanim rows and Daily-Learning rows the user has hidden, then removes any group
 * left empty. Rows without an id (always-on) and other groups are untouched.
 */
internal fun ZmanimDay.filterForDisplay(
    enabledZmanimTimes: Set<ZmanimTimeOption>,
    enabledDailyLearning: Set<DailyLearningType>,
): ZmanimDay {
    val zmanimIds = enabledZmanimTimes.mapTo(mutableSetOf()) { it.storageValue }
    val learningIds = enabledDailyLearning.mapTo(mutableSetOf()) { it.storageValue }
    val filtered = groups.mapNotNull { group ->
        val items = when (group.kind) {
            ZmanimGroupKind.Zmanim, ZmanimGroupKind.Shabbat -> group.items.filter { it.id == null || it.id in zmanimIds }
            ZmanimGroupKind.DailyLearning -> group.items.filter { it.id == null || it.id in learningIds }
            ZmanimGroupKind.Events -> group.items
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
internal fun ZmanimDay.mergeRambamRows(): ZmanimDay {
    val merged = groups.map { group ->
        if (group.kind != ZmanimGroupKind.DailyLearning) return@map group
        val one = group.items.firstOrNull { it.id == DailyLearningType.RambamYomi.storageValue }
        val three = group.items.firstOrNull { it.id == DailyLearningType.RambamYomiThreeChapters.storageValue }
        if (one == null || three == null) return@map group
        val mergedRow = one.copy(
            description = uiText(R.string.learning_rambam_merged_description),
            value = uiText(
                R.string.learning_rambam_merged,
                pluralText(R.plurals.learning_rambam_chapters, 1),
                one.value ?: verbatim(""),
                pluralText(R.plurals.learning_rambam_chapters, 3),
                three.value ?: verbatim(""),
            ),
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

internal fun ZmanimDay.toUiState(
    use24HourTime: Boolean,
    showCandleLightingPrompt: Boolean,
    developerTimeOverrideActive: Boolean,
    dayOffset: Int,
): ZmanimUiState {
    fun time(instant: Instant): UiText = UiText.Time(instant, zoneId, use24HourTime)

    val uiGroups = groups.mapIndexed { groupIndex, group ->
        ZmanimGroupUi(
            key = "group:$groupIndex:${group.kind}",
            title = group.kind.title?.let { uiText(it) },
            rows = group.items.mapIndexed { itemIndex, item ->
                ZmanimRowUi(
                    key = "row:$groupIndex:$itemIndex:${item.id ?: item.kind ?: ""}",
                    title = item.title,
                    description = item.description,
                    value = item.value ?: item.time?.let(::time) ?: verbatim("--"),
                    isRambam = item.id.isRambamId(),
                )
            },
        )
    }

    return ZmanimUiState(
        header = ZmanimHeaderUi(
            jewishDate = hebrewDate,
            // The weekday and the day-of-month come from different days: the weekday belongs to the
            // Jewish day and rolls at tzeit — Thursday evening is already "Friday", along with the
            // Hebrew date above it — while the day-of-month is the calendar date the times below
            // belong to, which turns over at midnight.
            gregorianDate = uiText(
                R.string.header_date,
                UiText.Date(displayedDate, DateStyle.Weekday),
                UiText.Date(date, DateStyle.DayMonth),
            ),
            locationName = locationName,
            // The fast's name belongs to it only while it is on; the times show a day ahead.
            fastName = fastDayInfo?.takeIf { it.isUnderWay }?.name,
            fastStart = fastDayInfo?.startTime?.let { observanceLine(R.string.fast_starts, time(it)) },
            fastEnd = fastDayInfo?.endTime?.let { observanceLine(R.string.fast_ends, time(it)) },
            // The name belongs to the holy day only while it is in; the times show a day ahead.
            holyDayName = holyDayInfo?.takeIf { it.isUnderWay }?.name,
            dayName = dayName,
            // While Shabbat is only announced — from the moment the entry/exit card appears — the
            // parasha gives the card a heading without claiming Shabbat has begun.
            parshaName = holyDayInfo?.takeUnless { it.isUnderWay }?.parsha,
            // The two ends of the span are named separately: a Yom Tov entering on Friday goes out
            // on Shabbat, so "כניסת החג" is paired with "צאת שבת".
            holyDayStart = holyDayInfo?.startTime?.let { observanceLine(holyDayInfo.entryTerm.starts, time(it)) },
            holyDayEnd = holyDayInfo?.endTime?.let { observanceLine(holyDayInfo.exitTerm.ends, time(it)) },
            holyDaySequel = holyDayInfo?.sequel,
            fastLeadsHeader = fastLeadsHeader,
        ),
        groups = uiGroups,
        showCandleLightingPrompt = showCandleLightingPrompt,
        developerTimeOverrideActive = developerTimeOverrideActive,
        dayOffset = dayOffset,
    )
}

private fun String?.isRambamId(): Boolean =
    this == DailyLearningType.RambamYomi.storageValue || this == DailyLearningType.RambamYomiThreeChapters.storageValue

/** One line of an observance card: what the time is, then the time — "צאת חג שני 19:20". */
private fun observanceLine(label: Int, time: UiText): UiText = uiText(R.string.observance_line, uiText(label), time)

/** How a Rambam Yomi row reads once worded: the value it shows, and the forms of it that may fit. */
data class RambamRowText(val value: String, val candidates: List<String>)

/**
 * A worded Rambam Yomi value as its row shows it ("פרק אחד: <ref1>\n3 פרקים: <ref2>" when both
 * tracks share the row), with the forms the row may place beside its title. The "פרק אחד"/"3 פרקים"
 * labels are always kept; the only variation offered is dropping the inner "פרק"/"chapter" word from
 * the references so the (still two-line) bubble is narrow enough to sit beside the title when the
 * halacha names are short. The row places whichever candidate fits beside the title there, and stacks
 * the full one below only when neither fits.
 */
fun rambamRowText(value: String, language: LanguageFormatter): RambamRowText {
    val display = value.lines().joinToString("\n") { line ->
        val (label, reference) = rambamPrefixAndReference(line)
        label + language.rambamRowReference(reference)
    }
    val lines = display.lines().map { rambamPrefixAndReference(it) }
    val full = lines.joinToString("\n") { (label, reference) -> label + reference }
    val abbreviated = lines.joinToString("\n") { (label, reference) ->
        label + abbreviateRambamReference(reference, language.rambamChapterWords)
    }
    return RambamRowText(display, listOf(full, abbreviated).distinct())
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

/** A year either way is plenty for looking something up, and keeps the stepper from running off. */
private const val MaxDayOffset = 365
