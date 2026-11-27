// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.testing.FakeApkInstaller
import com.noamtu.jewishday.testing.FakeAppUpdateRepository
import com.noamtu.jewishday.testing.MainDispatcherRule
import com.noamtu.jewishday.testing.TestStores
import com.noamtu.jewishday.testing.awaitFirst
import com.noamtu.jewishday.testing.eventually
import java.io.File
import java.io.IOException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** The update check on opening the app, and the download and install that follow. */
class AppUpdateViewModelTest {
    @get:Rule
    val folder = TemporaryFolder()

    @get:Rule
    val main = MainDispatcherRule()

    private val installed = requireNotNull(AppVersion.parse("1.3.0"))
    private val release = AppRelease(
        version = requireNotNull(AppVersion.parse("1.4.0")),
        title = "1.4.0",
        notes = "### Fixed\n- Something",
        downloadUrl = "https://github.com/x/y/releases/download/v1.4.0/app.apk",
        sizeBytes = 3L,
    )

    private class Scenario(
        val viewModel: AppUpdateViewModel,
        val updates: FakeAppUpdateRepository,
        val installer: FakeApkInstaller,
        val results: InstallResults,
        val pending: PendingUpdateStore,
    )

    private fun scenario(
        report: UpdateCheckReport = UpdateCheckReport.Available(installed, release),
        canInstall: Boolean = true,
        download: () -> File = { folder.newFile("update.apk").apply { writeText("apk") } },
        block: suspend Scenario.() -> Unit,
    ) = runBlocking {
        TestStores(folder).use { stores ->
            val updates = FakeAppUpdateRepository(report, download)
            val installer = FakeApkInstaller(canInstall)
            val results = InstallResults()
            val pending = PendingUpdateStore()
            val viewModel = AppUpdateViewModel(updates, installer, results, stores.developerOverrides, pending)
            try {
                Scenario(viewModel, updates, installer, results, pending).block()
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }

    @Test
    fun openingTheAppFindsTheUpdateAndRaisesOnlyTheBanner() = scenario {
        viewModel.onAppShown()
        assertEquals(release, viewModel.pendingRelease.awaitFirst { it != null }?.release)
        assertEquals(UpdateState.Idle, viewModel.state.value)
        // Leftovers of an earlier session are cleared before the first check.
        assertEquals(1, updates.downloadsCleared)
    }

    @Test
    fun aRotationIsNotAReopening() = scenario(report = UpdateCheckReport.UpToDate(installed, installed)) {
        viewModel.onAppShown()
        eventually { updates.checks == 1 }
        viewModel.onAppHidden(changingConfigurations = true)
        viewModel.onAppShown()
        viewModel.onAppHidden(changingConfigurations = false)
        viewModel.onAppShown()
        eventually { updates.checks == 2 }
        assertEquals(1, updates.downloadsCleared)
    }

    @Test
    fun theBannerOpensTheDialogForTheWaitingRelease() = scenario {
        viewModel.onAppShown()
        viewModel.pendingRelease.awaitFirst { it != null }
        viewModel.showPendingRelease()
        assertTrue(viewModel.state.value is UpdateState.Available)
    }

    @Test
    fun aDownloadGoesStraightToTheInstallerWhenAllowed() = scenario {
        viewModel.download(release)
        viewModel.state.awaitFirst { it is UpdateState.Installing }
        eventually { installer.committed == listOf(FakeApkInstaller.StagedSession) }
    }

    @Test
    fun withoutTheInstallPermissionItIsAskedFor() = scenario(canInstall = false) {
        viewModel.download(release)
        assertEquals(UpdateState.NeedsInstallPermission(release), viewModel.state.awaitFirst { it is UpdateState.NeedsInstallPermission })
        assertTrue(installer.committed.isEmpty())
    }

    @Test
    fun aFailedDownloadSaysSo() = scenario(download = { throw IOException("connection reset") }) {
        viewModel.download(release)
        assertEquals(UpdateState.Failed(release, "connection reset"), viewModel.state.awaitFirst { it is UpdateState.Failed })
    }

    @Test
    fun aSuccessfulInstallClearsEverything() = scenario {
        viewModel.onAppShown()
        viewModel.pendingRelease.awaitFirst { it != null }
        viewModel.download(release)
        viewModel.state.awaitFirst { it is UpdateState.Installing }
        results.publish(InstallOutcome.Succeeded)
        viewModel.state.awaitFirst { it == UpdateState.Idle }
        viewModel.pendingRelease.awaitFirst { it == null }
    }

    @Test
    fun decliningTheSystemDialogOffersTheInstallAgain() = scenario {
        viewModel.download(release)
        viewModel.state.awaitFirst { it is UpdateState.Installing }
        results.publish(InstallOutcome.Failed("declined"))
        assertEquals(UpdateState.Failed(release, "declined"), viewModel.state.awaitFirst { it is UpdateState.Failed })
    }

    /** GitHub with a download that moves on only as the test sends it bytes, and ends when it closes. */
    private class SteppedDownload(private val release: AppRelease, private val apk: File) : AppUpdateRepository {
        val bytes = Channel<Long>(Channel.UNLIMITED)
        var downloads = 0
            private set
        var reported = 0L
            private set

        override suspend fun findUpdate() = release
        override suspend fun check(): UpdateCheckReport = error("not checked here")
        override suspend fun download(release: AppRelease, onProgress: (downloaded: Long, total: Long) -> Unit): File {
            downloads++
            for (downloaded in bytes) {
                onProgress(downloaded, release.sizeBytes)
                reported = downloaded
            }
            return apk
        }
        override fun clearDownloads() = Unit
    }

    @Test
    fun aClosedDownloadStaysClosedUntilItIsDone() = runBlocking {
        TestStores(folder).use { stores ->
            val updates = SteppedDownload(release, folder.newFile("update.apk").apply { writeText("apk") })
            val installer = FakeApkInstaller()
            val pending = PendingUpdateStore().apply { offer(release, isDowngrade = false) }
            val viewModel = AppUpdateViewModel(updates, installer, InstallResults(), stores.developerOverrides, pending)
            try {
                viewModel.download(release)
                updates.bytes.send(1)
                viewModel.state.awaitFirst { it == UpdateState.Downloading(release, 1, release.sizeBytes) }
                // "Close": the download goes on without its dialog.
                viewModel.dismiss()
                updates.bytes.send(2)
                eventually { updates.reported == 2L }
                // It used to reopen the dialog on the very next progress report.
                assertEquals(UpdateState.Idle, viewModel.state.value)
                // The banner opens it again on the download in progress, not on a second one.
                viewModel.showPendingRelease()
                assertEquals(UpdateState.Downloading(release, 2, release.sizeBytes), viewModel.state.value)
                viewModel.download(release)
                assertEquals(1, updates.downloads)
                viewModel.dismiss()
                // Finished, it opens by itself, for the install.
                updates.bytes.close()
                viewModel.state.awaitFirst { it is UpdateState.Installing }
                eventually { installer.committed == listOf(FakeApkInstaller.StagedSession) }
                assertFalse(viewModel.state.value is UpdateState.Downloading)
            } finally {
                viewModel.viewModelScope.cancel()
            }
        }
    }
}
