// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.testing

import android.content.Intent
import com.noamtu.jewishday.core.model.JewishLocation
import com.noamtu.jewishday.core.model.defaultJerusalemLocation
import com.noamtu.jewishday.data.CurrentLocationRepository
import com.noamtu.jewishday.data.CurrentLocationState
import com.noamtu.jewishday.data.RootUiSettings
import com.noamtu.jewishday.data.StartupSettingsCache
import com.noamtu.jewishday.i18n.PerAppLanguage
import com.noamtu.jewishday.notification.DateStatusIconScheduler
import com.noamtu.jewishday.update.ApkInstaller
import com.noamtu.jewishday.update.AppRelease
import com.noamtu.jewishday.update.AppUpdateRepository
import com.noamtu.jewishday.update.UpdateCheckReport
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// Stand-ins for the Android-backed pieces the view models are built from. Settings and the
// developer overrides are the real DataStore-backed repositories over a file (see TestStores.kt).

/** A phone that is always at [location], with a fix in hand. */
class FakeCurrentLocationRepository(location: JewishLocation = defaultJerusalemLocation) : CurrentLocationRepository {
    override val currentLocationState = MutableStateFlow(CurrentLocationState(location = location))
    override val currentLocation: StateFlow<JewishLocation?> = MutableStateFlow(location)
    var refreshes = 0
        private set

    override fun refreshCurrentLocation(force: Boolean) {
        refreshes++
    }

    override fun useJerusalemFallback() = Unit

    override fun currentLocationOrDefault(): JewishLocation = currentLocation.value ?: defaultJerusalemLocation
}

/** Records what the date icon was asked to do. */
class RecordingDateStatusIconScheduler : DateStatusIconScheduler {
    val syncs = mutableListOf<Boolean>()

    override fun sync(enabled: Boolean) {
        syncs += enabled
    }
}

/** Android's per-app language, kept in a field. */
class FakePerAppLanguage(var applied: String? = null) : PerAppLanguage {
    override val supported = listOf("en", "he")
    override fun current() = applied
    override fun set(tag: String) {
        applied = tag
    }
    override fun isInitialized() = true
    override fun markInitialized() = Unit
    override fun systemLanguage() = "en"
}

/** GitHub, answering every check with [report] and every download with [download]. */
class FakeAppUpdateRepository(
    var report: UpdateCheckReport,
    private val download: () -> File = { error("no download expected") },
) : AppUpdateRepository {
    var checks = 0
        private set
    var downloadsCleared = 0
        private set

    override suspend fun findUpdate(): AppRelease? = (check() as? UpdateCheckReport.Available)?.release

    override suspend fun check(): UpdateCheckReport {
        checks++
        return report
    }

    override suspend fun download(release: AppRelease, onProgress: (downloaded: Long, total: Long) -> Unit): File {
        onProgress(release.sizeBytes, release.sizeBytes)
        return download()
    }

    override fun clearDownloads() {
        downloadsCleared++
    }
}

/** The system installer: [canInstall] decides whether the permission is there; commits are recorded. */
class FakeApkInstaller(var canInstall: Boolean = true) : ApkInstaller {
    val committed = mutableListOf<Int>()
    val abandoned = mutableListOf<Int>()

    override fun canInstall(): Boolean = canInstall

    override fun manageUnknownSourcesIntent(): Intent = error("not opened in unit tests")

    override suspend fun stage(apk: File): Int = StagedSession

    override fun commit(sessionId: Int) {
        committed += sessionId
    }

    override fun abandon(sessionId: Int) {
        abandoned += sessionId
    }

    companion object {
        const val StagedSession = 7
    }
}

/** No synchronous theme cache: every read goes to the DataStore. */
object NoStartupCache : StartupSettingsCache {
    override fun read(): RootUiSettings? = null
    override fun write(settings: RootUiSettings) = Unit
}
