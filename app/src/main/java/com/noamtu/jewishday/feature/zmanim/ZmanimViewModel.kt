// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.dayOffsetTo
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.core.model.isInIsrael
import com.noamtu.jewishday.data.AppSettingsRepository
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.DeveloperOverrides
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import com.noamtu.jewishday.data.JewishDayRepository
import com.noamtu.jewishday.day.ZmanItem
import com.noamtu.jewishday.day.ZmanimDay
import com.noamtu.jewishday.day.withDailyLearningItems
import com.noamtu.jewishday.ui.ShowDayRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
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
    private val clock: Clock,
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

    // The widget tap last acted on, so the screen coming back into view does not act on it again.
    private var shownRequest: ShowDayRequest? = null

    /**
     * Steps the screen to the day a widget tap asks for — or stays on today once that day has begun
     * (see [dayOffsetTo]). Each request is acted on once.
     */
    fun showDay(request: ShowDayRequest) {
        if (request === shownRequest) return
        shownRequest = request
        viewModelScope.launch {
            val offset = dayOffsetTo(request.date, location.first(), calculationSettings.first(), clock.instant())
            _dayOffset.value = offset.coerceAtMost(MaxDayOffset)
        }
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

/** A year either way is plenty for looking something up, and keeps the stepper from running off. */
private const val MaxDayOffset = 365
