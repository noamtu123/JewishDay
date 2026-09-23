// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.developer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.AppThemeOption
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DeveloperOverrides
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.model.ZmanimCalculationSettings
import com.noamtu.jewishday.model.isInIsrael
import com.noamtu.jewishday.model.jewishDayCivilDate
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import com.noamtu.jewishday.update.AppUpdateRepository
import com.noamtu.jewishday.update.PendingUpdateStore
import com.noamtu.jewishday.update.UpdateCheckReport
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // The date and time the app is currently being told it is. The pickers open on these rather
    // than on the real today, so coming back to this screen resumes the spoof instead of silently
    // offering to reset it to now.
    // LocalDate.EPOCH needs API 34; the app runs from 26. Any placeholder does — this is replaced
    // the moment the real state is built.
    val effectiveDate: LocalDate = LocalDate.of(1970, 1, 1),
    val effectiveTime: LocalTime = LocalTime.MIDNIGHT,
)

@HiltViewModel
class DeveloperViewModel @Inject constructor(
    private val developerOverridesRepository: DeveloperOverridesRepository,
    private val currentLocationRepository: CurrentLocationRepository,
    private val appSettingsRepository: AppSettingsRepository,
    @ApplicationContext private val context: Context,
    private val clock: Clock,
    private val appUpdateRepository: AppUpdateRepository,
    private val pendingUpdates: PendingUpdateStore,
) : ViewModel() {

    // Israel/diaspora now follows the effective location, so re-render whenever it changes.
    val uiState: StateFlow<DeveloperUiState> = combine(
        developerOverridesRepository.state,
        currentLocationRepository.currentLocation,
        appSettingsRepository.settings,
    ) { overrides, _, settings ->
        buildUiState(overrides, settings.zmanimSettings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DeveloperUiState())

    private val _updateCheckResult = MutableStateFlow<String?>(null)

    /** What the last manual update check said, or null when none has been run. */
    val updateCheckResult: StateFlow<String?> = _updateCheckResult.asStateFlow()

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

    /** Sets the overridden date, keeping the time-of-day currently being simulated. */
    fun setOverrideDate(date: LocalDate) = launchOverride {
        val zone = clock.zone
        val timeOfDay = clock.instant().atZone(zone).toLocalTime()
        val virtual = date.atTime(timeOfDay).atZone(zone).toInstant().toEpochMilli()
        developerOverridesRepository.setVirtualTime(virtual)
    }

    /** Sets the overridden time-of-day, keeping the date currently being simulated. */
    fun setOverrideTime(time: LocalTime) = launchOverride {
        val zone = clock.zone
        val date = clock.instant().atZone(zone).toLocalDate()
        val virtual = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
        developerOverridesRepository.setVirtualTime(virtual)
    }

    fun jumpTo(target: DeveloperJumpTarget) = launchOverride {
        val inIsrael = currentLocationRepository.currentLocationOrDefault().isInIsrael
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

    fun setLocationOverrideEnabled(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setLocationOverrideEnabled(enabled)
    }

    fun setLocationPreset(id: String) = launchOverride {
        developerOverridesRepository.setLocationPreset(id)
    }

    fun setAboutInEnglish(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setAboutInEnglish(enabled)
    }

    fun setUpdateNotesInEnglish(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setUpdateNotesInEnglish(enabled)
    }

    fun setSpoofedVersionName(versionName: String) = launchOverride {
        developerOverridesRepository.setSpoofedVersionName(versionName)
        // A stale verdict next to a version that just changed reads as the new one's answer, and a
        // banner raised for the old spoof is the same lie in a louder place.
        _updateCheckResult.value = null
        pendingUpdates.clear()
    }

    /**
     * Runs the same check the app runs at launch and says what it found. The launch check is silent
     * about everything except an available update, so without this there is no way to tell an
     * up-to-date app from one that never reached GitHub at all.
     *
     * A found update is also handed to [PendingUpdateStore], which is what the home screen's banner
     * reads — so the offer appears straight away instead of only after a restart. A check that
     * finds nothing clears any offer a previous check left, so the banner never outlives the
     * spoofed version that produced it.
     */
    fun runUpdateCheck() {
        if (_updateCheckResult.value == CheckingLabel) return
        viewModelScope.launch {
            _updateCheckResult.value = CheckingLabel
            _updateCheckResult.value = when (val report = appUpdateRepository.check()) {
                is UpdateCheckReport.Available -> {
                    pendingUpdates.offer(report.release, report.isDowngrade)
                    "Update found: ${report.release.version} (installed ${report.installed}). " +
                        "It is being offered on the home screen now."
                }
                is UpdateCheckReport.UpToDate -> {
                    pendingUpdates.clear()
                    "Up to date: newest release is ${report.latest}, installed is ${report.installed}."
                }
                is UpdateCheckReport.NoReleases -> {
                    pendingUpdates.clear()
                    "GitHub answered, but no release has an APK attached."
                }
                // A check that never reached GitHub found nothing out, so it must not withdraw an
                // offer that an earlier, successful check made.
                is UpdateCheckReport.Failed -> "Check failed: ${report.reason}"
            }
        }
    }

    fun setCompassMonitoringEnabled(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setCompassMonitoringEnabled(enabled)
    }

    fun setGlassThemeAvailable(enabled: Boolean) = launchOverride {
        developerOverridesRepository.setGlassThemeAvailable(enabled)
        if (!enabled) leaveGlassTheme()
    }

    /**
     * Glass is only offered while developer mode lists it. Once it is hidden, someone still on it
     * would be stuck on a theme they cannot see or choose, so they go back to the default.
     */
    private suspend fun leaveGlassTheme() {
        if (appSettingsRepository.settings.first().themeOption == AppThemeOption.Glass) {
            appSettingsRepository.setThemeOption(AppThemeOption.Default)
        }
    }

    fun resetOverrides() = launchOverride {
        developerOverridesRepository.clearOverrides()
        leaveGlassTheme()
        _updateCheckResult.value = null
        pendingUpdates.clear()
    }

    /** Clears the overrides and locks the tools away again — see [DeveloperOverridesRepository]. */
    fun disableDeveloperMode() = launchOverride {
        developerOverridesRepository.disableDeveloperMode()
        leaveGlassTheme()
        _updateCheckResult.value = null
        pendingUpdates.clear()
    }

    private fun launchOverride(block: suspend () -> Unit) {
        viewModelScope.launch {
            block()
            // The virtual clock/location just changed; re-render the status-bar date icon so it
            // reflects the override immediately instead of waiting for the next scheduled tzeit.
            DateStatusIconScheduler.refresh(context)
        }
    }

    private fun buildUiState(
        overrides: DeveloperOverrides,
        zmanimSettings: ZmanimCalculationSettings,
    ): DeveloperUiState {
        val location = currentLocationRepository.currentLocationOrDefault()
        val inIsrael = location.isInIsrael
        val instant = clock.instant()
        val zoned = instant.atZone(location.zoneId)
        val localDate = zoned.toLocalDate()

        // The readout names the Hebrew date the app is showing, which rolls at tzeit like the main
        // screen's header — shifting the clock past nightfall must move it to the next date.
        val jewishCalendar = JewishCalendar(jewishDayCivilDate(location, zmanimSettings, instant)).apply {
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
            effectiveDate = localDate,
            effectiveTime = zoned.toLocalTime(),
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

    private companion object {
        const val CheckingLabel = "Checking…"
    }
}
