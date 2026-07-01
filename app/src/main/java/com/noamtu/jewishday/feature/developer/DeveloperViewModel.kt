package com.noamtu.jewishday.feature.developer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DeveloperOverrides
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** A quick "jump the clock to the next …" target for exercising calendar-driven features. */
enum class DeveloperJumpTarget(val label: String) {
    RoshHashana("Rosh Hashana"),
    YomKippur("Yom Kippur"),
    Sukkot("Sukkot"),
    Chanukah("Chanukah (day 1)"),
    TenthTevet("10 Tevet (fast)"),
    TaanitEsther("Ta'anit Esther (fast)"),
    Purim("Purim"),
    ErevPesach("Erev Pesach"),
    Pesach("Pesach (day 1)"),
    LagBaomer("Lag BaOmer"),
    Shavuot("Shavuot"),
    SeventeenTammuz("17 Tammuz (fast)"),
    TishaBeav("Tisha B'Av (fast)"),
    TzomGedaliah("Tzom Gedaliah (fast)"),
    RoshChodesh("Rosh Chodesh"),
    ErevShabbat("Erev Shabbat"),
}

data class DeveloperUiState(
    val overrides: DeveloperOverrides = DeveloperOverrides(),
    val inIsrael: Boolean = true,
    val effectiveDateTime: String = "",
    val effectiveLocation: String = "",
    val jewishDate: String = "",
    val dayInfo: String = "",
)

@HiltViewModel
class DeveloperViewModel @Inject constructor(
    private val developerOverridesRepository: DeveloperOverridesRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val clock: Clock,
) : ViewModel() {

    val uiState: StateFlow<DeveloperUiState> = combine(
        developerOverridesRepository.state,
        appSettingsRepository.settings,
    ) { overrides, settings ->
        buildUiState(overrides, settings.zmanimSettings.inIsrael)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeveloperUiState())

    fun setTimeOverrideEnabled(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setTimeOverrideEnabled(enabled)
    }

    fun setTimeFrozen(frozen: Boolean) = launchOverride {
        developerOverridesRepository.setTimeFrozen(frozen)
    }

    fun shiftDays(days: Long) = launchOverride {
        developerOverridesRepository.shiftVirtualTime(Duration.ofDays(days).toMillis())
    }

    fun shiftHours(hours: Long) = launchOverride {
        developerOverridesRepository.shiftVirtualTime(Duration.ofHours(hours).toMillis())
    }

    /** Sets the overridden date, keeping the current time-of-day. */
    fun setOverrideDate(date: LocalDate) = launchOverride {
        val zone = clock.zone
        val timeOfDay = clock.instant().atZone(zone).toLocalTime()
        val virtual = date.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
        developerOverridesRepository.setVirtualTime(virtual)
    }

    fun jumpTo(target: DeveloperJumpTarget) = viewModelScope.launch {
        val inIsrael = appSettingsRepository.settings.first().zmanimSettings.inIsrael
        val today = LocalDate.now(clock.zone)
        val date = (0..420L)
            .map { today.plusDays(it) }
            .firstOrNull { matches(target, it, inIsrael) }
            ?: today
        val zone = clock.zone
        // Anchor fasts/holidays at 09:00 local so a fast's morning is visible; nudge with +/- hour.
        val virtual = date.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()
        developerOverridesRepository.setVirtualTime(virtual)
    }

    fun setLocationOverrideEnabled(enabled: Boolean) = viewModelScope.launch {
        developerOverridesRepository.setLocationOverrideEnabled(enabled)
    }

    fun setLocationPreset(id: String) = viewModelScope.launch {
        developerOverridesRepository.setLocationPreset(id)
    }

    fun setInIsrael(inIsrael: Boolean) = viewModelScope.launch {
        val current = appSettingsRepository.settings.first().zmanimSettings
        appSettingsRepository.setZmanimSettings(current.copy(inIsrael = inIsrael))
    }

    fun resetOverrides() = viewModelScope.launch {
        developerOverridesRepository.clearOverrides()
    }

    private fun launchOverride(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private fun buildUiState(overrides: DeveloperOverrides, inIsrael: Boolean): DeveloperUiState {
        val location = currentLocationRepository.currentLocationOrDefault()
        val instant = clock.instant()
        val zoned = instant.atZone(location.zoneId)
        val localDate = zoned.toLocalDate()

        val jewishCalendar = JewishCalendar(localDate).apply {
            isUseModernHolidays = true
            setInIsrael(inIsrael)
        }
        val hebrewFormatter = HebrewDateFormatter().apply { isHebrewFormat = true }

        return DeveloperUiState(
            overrides = overrides,
            inIsrael = inIsrael,
            effectiveDateTime = zoned.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy  HH:mm", Locale.US)),
            effectiveLocation = "${location.name} (${location.zoneId})",
            jewishDate = hebrewFormatter.format(jewishCalendar),
            dayInfo = describeDay(jewishCalendar),
        )
    }

    private fun describeDay(jewishCalendar: JewishCalendar): String {
        val parts = buildList {
            HebrewDateFormatter().formatYomTov(jewishCalendar).takeIf { it.isNotBlank() }?.let(::add)
            if (jewishCalendar.isRoshChodesh) add("Rosh Chodesh")
            if (jewishCalendar.isTaanis) add("Fast day")
            if (jewishCalendar.isChanukah) add("Chanukah day ${jewishCalendar.dayOfChanukah}")
            if (jewishCalendar.dayOfOmer != -1) add("Omer ${jewishCalendar.dayOfOmer}")
        }
        return if (parts.isEmpty()) "Regular day" else parts.joinToString(" • ")
    }

    private fun matches(target: DeveloperJumpTarget, date: LocalDate, inIsrael: Boolean): Boolean {
        val jc = JewishCalendar(date).apply {
            isUseModernHolidays = true
            setInIsrael(inIsrael)
        }
        return when (target) {
            DeveloperJumpTarget.RoshHashana -> jc.yomTovIndex == JewishCalendar.ROSH_HASHANA
            DeveloperJumpTarget.YomKippur -> jc.yomTovIndex == JewishCalendar.YOM_KIPPUR
            DeveloperJumpTarget.Sukkot -> jc.yomTovIndex == JewishCalendar.SUCCOS
            DeveloperJumpTarget.Chanukah -> jc.isChanukah && jc.dayOfChanukah == 1
            DeveloperJumpTarget.TenthTevet -> jc.yomTovIndex == JewishCalendar.TENTH_OF_TEVES
            DeveloperJumpTarget.TaanitEsther -> jc.yomTovIndex == JewishCalendar.FAST_OF_ESTHER
            DeveloperJumpTarget.Purim -> jc.yomTovIndex == JewishCalendar.PURIM
            DeveloperJumpTarget.ErevPesach -> jc.yomTovIndex == JewishCalendar.EREV_PESACH
            DeveloperJumpTarget.Pesach -> jc.yomTovIndex == JewishCalendar.PESACH
            DeveloperJumpTarget.LagBaomer -> jc.dayOfOmer == 33
            DeveloperJumpTarget.Shavuot -> jc.yomTovIndex == JewishCalendar.SHAVUOS
            DeveloperJumpTarget.SeventeenTammuz -> jc.yomTovIndex == JewishCalendar.SEVENTEEN_OF_TAMMUZ
            DeveloperJumpTarget.TishaBeav -> jc.yomTovIndex == JewishCalendar.TISHA_BEAV
            DeveloperJumpTarget.TzomGedaliah -> jc.yomTovIndex == JewishCalendar.FAST_OF_GEDALYAH
            DeveloperJumpTarget.RoshChodesh -> jc.isRoshChodesh
            DeveloperJumpTarget.ErevShabbat -> date.dayOfWeek == java.time.DayOfWeek.FRIDAY
        }
    }
}
