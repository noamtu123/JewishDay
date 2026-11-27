// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.zmanim

import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.data.DailyLearningRepository
import com.noamtu.jewishday.data.DefaultJewishDayRepository
import com.noamtu.jewishday.day.ZmanItem
import com.noamtu.jewishday.i18n.TestText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.verbatim
import com.noamtu.jewishday.testing.FakeCurrentLocationRepository
import com.noamtu.jewishday.testing.MainDispatcherRule
import com.noamtu.jewishday.testing.NoDailyLearning
import com.noamtu.jewishday.testing.TestStores
import com.noamtu.jewishday.testing.awaitFirst
import com.noamtu.jewishday.ui.ShowDayRequest
import java.time.Clock
import java.time.LocalDate
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The zmanim screen's state, from the clock, the settings and the day switcher (Jerusalem). */
class ZmanimViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @get:Rule
    val main = MainDispatcherRule()

    // Wednesday 2 September 2026, noon: Shabbat is three days off.
    private val wednesday = LocalDate.of(2026, 9, 2)
    private val clock = Clock.fixed(wednesday.atTime(12, 0).atZone(defaultJerusalemLocation.zoneId).toInstant(), defaultJerusalemLocation.zoneId)

    private fun scenario(
        dailyLearning: DailyLearningRepository = NoDailyLearning(),
        block: suspend (ZmanimViewModel, TestStores) -> Unit,
    ) = runBlocking {
        TestStores(folder).use { stores ->
            val viewModel = ZmanimViewModel(
                jewishDayRepository = DefaultJewishDayRepository(clock),
                appSettingsRepository = stores.settings,
                currentLocationRepository = FakeCurrentLocationRepository(),
                dailyLearningRepository = dailyLearning,
                developerOverridesRepository = stores.developerOverrides,
                clock = clock,
            )
            try {
                block(viewModel, stores)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    private fun hebrewDateOf(date: LocalDate) = TestText.english.resolve(CalendarText.HebrewDate(date).asUiText())

    private suspend fun ZmanimViewModel.shown(offset: Int) = uiState.awaitFirst { it.header != null && it.dayOffset == offset }

    // A step is computed off the main thread, so a test of what a day shows waits for that day's header.
    private suspend fun ZmanimViewModel.showing(date: LocalDate) = uiState.awaitFirst { state ->
        state.header?.let { TestText.english.resolve(it.jewishDate) } == hebrewDateOf(date)
    }

    @Test
    fun todayIsTheClocksDay() = scenario { viewModel, _ ->
        val state = viewModel.shown(0)
        assertEquals(hebrewDateOf(wednesday), TestText.english.resolve(requireNotNull(state.header).jewishDate))
    }

    @Test
    fun steppingMovesByWholeDaysAndBack() = scenario { viewModel, _ ->
        viewModel.shown(0)
        viewModel.stepDay(2)
        assertEquals(2, viewModel.showing(wednesday.plusDays(2)).dayOffset)
        viewModel.showToday()
        assertEquals(0, viewModel.showing(wednesday).dayOffset)
    }

    /** Every state the screen is given while stepping, recorded as each one is set. */
    private suspend fun ZmanimViewModel.statesWhile(steps: suspend ZmanimViewModel.() -> Unit): List<ZmanimUiState> {
        val seen = Collections.synchronizedList(mutableListOf<ZmanimUiState>())
        coroutineScope {
            // Unconfined, so the recorder runs as each state is set and misses none in between.
            val recorder = launch(Dispatchers.Unconfined) { uiState.collect { seen += it } }
            steps()
            recorder.cancel()
        }
        return seen.toList()
    }

    @Test
    fun theStepCountAlwaysBelongsToTheDayShown() = scenario { viewModel, _ ->
        viewModel.shown(0)
        val states = viewModel.statesWhile {
            stepDay(2)
            showing(wednesday.plusDays(2))
            stepDay(-3)
            showing(wednesday.minusDays(1))
            showToday()
            showing(wednesday)
        }
        // The pager and the "back to today" button read the count: paired with another day's content,
        // a step briefly showed Wednesday's times as if they were Friday's.
        for (state in states.filter { it.header != null }) {
            assertEquals(
                "a state counting ${state.dayOffset}",
                hebrewDateOf(wednesday.plusDays(state.dayOffset.toLong())),
                TestText.english.resolve(requireNotNull(state.header).jewishDate),
            )
        }
    }

    /** Hebcal's daf for each day, named after the day so a row shows whose it is. */
    private class DafPerDay : DailyLearningRepository {
        override fun learningItems(date: LocalDate, inIsrael: Boolean): Flow<List<ZmanItem>> = flowOf(
            listOf(ZmanItem(title = verbatim("daf"), time = null, description = verbatim("$date"), id = DailyLearningType.DafYomiBavli.storageValue)),
        )
    }

    @Test
    fun aSteppedDayNeverShowsAnotherDaysLearning() = scenario(dailyLearning = DafPerDay()) { viewModel, _ ->
        viewModel.uiState.awaitFirst { state -> state.header != null && dafOf(state) == "$wednesday" }
        val states = viewModel.statesWhile {
            stepDay(1)
            uiState.awaitFirst { state -> state.dayOffset == 1 && dafOf(state) == "${wednesday.plusDays(1)}" }
            stepDay(1)
            uiState.awaitFirst { state -> state.dayOffset == 2 && dafOf(state) == "${wednesday.plusDays(2)}" }
        }
        for (state in states.filter { it.header != null }) {
            val daf = dafOf(state) ?: continue
            assertEquals("the daf shown on day ${state.dayOffset}", "${wednesday.plusDays(state.dayOffset.toLong())}", daf)
        }
    }

    // The Hebcal daf's description, which [DafPerDay] sets to its day; null while the offline one shows.
    private fun dafOf(state: ZmanimUiState): String? = state.groups.flatMap { it.rows }
        .firstOrNull { TestText.english.resolve(it.title) == "daf" }
        ?.let { TestText.english.resolve(it.description) }

    @Test
    fun aWidgetTapStepsToItsDayOnlyOnce() = scenario { viewModel, _ ->
        viewModel.shown(0)
        val shabbat = ShowDayRequest(wednesday.plusDays(3))
        viewModel.showDay(shabbat)
        viewModel.shown(3)
        // Coming back to the screen hands it the same request: it must not step there again.
        viewModel.showToday()
        viewModel.showDay(shabbat)
        viewModel.shown(0)
        // A new tap is a new request, even for the same day.
        viewModel.showDay(ShowDayRequest(wednesday.plusDays(3)))
        viewModel.shown(3)
    }

    @Test
    fun aDayAlreadyBegunOpensOnToday() = scenario { viewModel, _ ->
        viewModel.shown(0)
        viewModel.stepDay(1)
        viewModel.shown(1)
        viewModel.showDay(ShowDayRequest(wednesday.minusDays(1)))
        viewModel.shown(0)
    }

    @Test
    fun hiddenZmanimAreNotListed() = scenario { viewModel, stores ->
        val sunrise = TestText.english.string(R.string.zman_sunrise)
        fun titles(state: ZmanimUiState) = state.groups.flatMap { it.rows }.map { TestText.english.resolve(it.title) }
        assertTrue(titles(viewModel.shown(0)).contains(sunrise))
        stores.settings.setEnabledZmanimTimes(ZmanimTimeOption.Default - ZmanimTimeOption.Sunrise)
        val hidden = viewModel.uiState.awaitFirst { it.header != null && sunrise !in titles(it) }
        assertFalse(titles(hidden).contains(sunrise))
    }

    @Test
    fun theFirstCandleLightingAnswerIsSavedAsTheDefault() = scenario { viewModel, stores ->
        viewModel.selectCandleLightingMethod(CandleLightingMethod.Minutes30)
        val settings = stores.settings.settings.awaitFirst { it.candleLightingPromptHandled && it.candleLightingDefault != null }
        assertEquals(CandleLightingMethod.Minutes30, settings.zmanimSettings.candleLightingMethod)
        assertEquals(CandleLightingMethod.Minutes30, settings.candleLightingDefault)
    }

    @Test
    fun typedMinutesAreKeptAsTyped() = scenario { viewModel, stores ->
        // No upper limit: a time earlier than plag hamincha is held at plag when it is shown.
        viewModel.selectCandleLightingMinutes(500)
        val settings = stores.settings.settings.awaitFirst { it.candleLightingPromptHandled && it.candleLightingDefault != null }
        assertEquals(CandleLightingMethod.Custom, settings.zmanimSettings.candleLightingMethod)
        assertEquals(500, settings.zmanimSettings.candleLightingCustomMinutes)
        assertEquals(CandleLightingMethod.Custom, settings.candleLightingDefault)
    }
}
