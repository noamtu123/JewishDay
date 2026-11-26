// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.settings

import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.core.model.AlotHashacharMethod
import com.noamtu.jewishday.core.model.CandleLightingMethod
import com.noamtu.jewishday.core.model.CustomZmanUnit
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import com.noamtu.jewishday.i18n.AppLanguageController
import com.noamtu.jewishday.testing.FakeAppUpdateRepository
import com.noamtu.jewishday.testing.FakePerAppLanguage
import com.noamtu.jewishday.testing.MainDispatcherRule
import com.noamtu.jewishday.testing.RecordingDateStatusIconScheduler
import com.noamtu.jewishday.testing.TestStores
import com.noamtu.jewishday.testing.awaitFirst
import com.noamtu.jewishday.testing.eventually
import com.noamtu.jewishday.update.AppRelease
import com.noamtu.jewishday.update.AppVersion
import com.noamtu.jewishday.update.PendingUpdateStore
import com.noamtu.jewishday.update.UpdateCheckReport
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** What the Settings screen's switches and resets store, and what they set off. */
class SettingsViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @get:Rule
    val main = MainDispatcherRule()

    private val installed = requireNotNull(AppVersion.parse("1.3.0"))
    private val newer = AppRelease(
        version = requireNotNull(AppVersion.parse("1.4.0-pre.1")),
        title = "pre-1.4.0",
        notes = "",
        downloadUrl = "https://github.com/x/y/releases/download/v1.4.0-pre.1/app.apk",
        sizeBytes = 1L,
        isPreRelease = true,
    )

    private class Scenario(
        val viewModel: SettingsViewModel,
        val stores: TestStores,
        val language: FakePerAppLanguage,
        val icon: RecordingDateStatusIconScheduler,
        val updates: FakeAppUpdateRepository,
        val pending: PendingUpdateStore,
    )

    private fun scenario(
        report: UpdateCheckReport = UpdateCheckReport.UpToDate(installed, installed),
        block: suspend Scenario.() -> Unit,
    ) = runBlocking {
        TestStores(folder).use { stores ->
            val language = FakePerAppLanguage()
            val icon = RecordingDateStatusIconScheduler()
            val updates = FakeAppUpdateRepository(report)
            val pending = PendingUpdateStore()
            val viewModel = SettingsViewModel(
                appSettingsRepository = stores.settings,
                appLanguageController = AppLanguageController(language, stores.settings),
                dateStatusIconScheduler = icon,
                appUpdateRepository = updates,
                pendingUpdates = pending,
            )
            try {
                Scenario(viewModel, stores, language, icon, updates, pending).block()
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun turningTheIconOffTakesItDownAndRemembersIt() = scenario {
        viewModel.setHebrewDateStatusIconEnabled(false)
        assertEquals(listOf(false), icon.syncs)
        stores.settings.settings.awaitFirst { !it.hebrewDateStatusIconEnabled }
        viewModel.uiState.awaitFirst { !it.hebrewDateStatusIconEnabled }
    }

    @Test
    fun aLanguageIsAppliedAtOnceAndRecorded() = scenario {
        viewModel.setAppLanguage("he")
        assertEquals("he", language.applied)
        stores.settings.settings.awaitFirst { it.language == "he" }
    }

    @Test
    fun theClockSwitchReachesTheScreenState() = scenario {
        viewModel.setUse24HourTime(false)
        viewModel.uiState.awaitFirst { !it.use24HourTime }
    }

    @Test
    fun joiningTheTestChannelOffersWhatItFinds() = scenario(UpdateCheckReport.Available(installed, newer)) {
        viewModel.setIncludePreReleases(true)
        stores.settings.settings.awaitFirst { it.includePreReleases }
        assertEquals(newer, pending.pending.awaitFirst { it != null }?.release)
        assertEquals(1, updates.checks)
    }

    @Test
    fun leavingTheChannelWithNothingNewWithdrawsTheOffer() = scenario {
        pending.offer(newer, isDowngrade = false)
        viewModel.setIncludePreReleases(false)
        assertNull(pending.pending.awaitFirst { it == null })
    }

    @Test
    fun aFailedCheckLeavesTheOfferAlone() = scenario(UpdateCheckReport.Failed("offline")) {
        pending.offer(newer, isDowngrade = false)
        viewModel.setIncludePreReleases(true)
        eventually { updates.checks == 1 }
        assertEquals(newer, pending.pending.value?.release)
    }

    @Test
    fun resettingTheMethodsKeepsTheFirstLaunchCandleLighting() = scenario {
        stores.settings.setCandleLightingDefault(CandleLightingMethod.Minutes40)
        stores.settings.setZmanimSettings(
            ZmanimCalculationSettings(alotHashacharMethod = AlotHashacharMethod.CustomMinutes, candleLightingMethod = CandleLightingMethod.Minutes20),
        )
        viewModel.resetZmanimMethods()
        val reset = stores.settings.settings.awaitFirst { it.zmanimSettings.alotHashacharMethod == ZmanimCalculationSettings().alotHashacharMethod }
        assertEquals(ZmanimCalculationSettings(candleLightingMethod = CandleLightingMethod.Minutes40), reset.zmanimSettings)
    }

    @Test
    fun resettingTheZmanimListShowsThemAllAgain() = scenario {
        stores.settings.setEnabledZmanimTimes(setOf(ZmanimTimeOption.Sunrise))
        viewModel.resetZmanimTimes()
        stores.settings.settings.awaitFirst { it.enabledZmanimTimes == ZmanimTimeOption.Default }
    }

    @Test
    fun aCustomValueSelectsItsOptionAndKeepsTheNumber() = scenario {
        viewModel.setAlotHashacharCustomValue(AlotHashacharMethod.CustomMinutes, CustomZmanUnit.Minutes, 90.0)
        val stored = stores.settings.settings.awaitFirst { it.zmanimSettings.alotHashacharMethod == AlotHashacharMethod.CustomMinutes }
        assertEquals(90, stored.zmanimSettings.alotHashacharCustom.minutes)
    }
}
