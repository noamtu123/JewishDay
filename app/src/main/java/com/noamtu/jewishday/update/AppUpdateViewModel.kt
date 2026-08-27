// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.launch

/** Everything the update dialog can be showing. */
sealed interface UpdateState {
    /** Nothing to say; no dialog. */
    data object Idle : UpdateState

    data class Available(
        val release: AppRelease,
        /** Offered but older than what is installed: it needs an uninstall, not an update. */
        val isDowngrade: Boolean = false,
    ) : UpdateState

    /** [totalBytes] is 0 while the server has not said how large the download is. */
    data class Downloading(
        val release: AppRelease,
        val downloadedBytes: Long,
        val totalBytes: Long,
    ) : UpdateState {
        /** Null when the size is unknown, which the bar shows as indeterminate. */
        val progress: Float? get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else null
    }

    /** Staged, waiting on the system's install confirmation. */
    data class Installing(val release: AppRelease) : UpdateState

    data class Failed(val release: AppRelease?, val message: String?) : UpdateState

    /** Permission to install from this app has not been granted yet. */
    data class NeedsInstallPermission(val release: AppRelease) : UpdateState
}

@HiltViewModel
class AppUpdateViewModel @Inject constructor(
    private val repository: AppUpdateRepository,
    private val installer: ApkInstaller,
    installResults: InstallResults,
    developerOverrides: DeveloperOverridesRepository,
) : ViewModel() {

    /** Hidden developer switch: read the English changelog without switching the whole interface. */
    val notesInEnglish: StateFlow<Boolean> = developerOverrides.state
        .map { overrides -> overrides.updateNotesInEnglish }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _pendingRelease = MutableStateFlow<UpdateState.Available?>(null)

    /**
     * The update that is waiting, whether or not its dialog is open.
     *
     * Dismissing the dialog is "not now", not "never": the release stays here so the home screen
     * can keep a banner offering it, and only an actual install clears it.
     */
    val pendingRelease: StateFlow<UpdateState.Available?> = _pendingRelease.asStateFlow()

    private var downloaded: File? = null

    /** A session already written to disk, waiting only for the app to be in front to commit. */
    private var stagedSessionId: Int? = null
    private var permissionWatch: Job? = null

    /**
     * Whether this session already got an answer from GitHub. The check runs from a
     * `LaunchedEffect(Unit)`, which re-fires on every Activity recreation — so without this a
     * rotation issued a fresh API request each time. Deliberately per *session*, not per day: a
     * hotfix must still reach people the very next time they open the app.
     */
    private var checkedThisSession = false

    init {
        viewModelScope.launch {
            installResults.outcomes.collect { outcome ->
                when (outcome) {
                    is InstallOutcome.Succeeded -> {
                        permissionWatch?.cancel()
                        repository.clearDownloads()
                        _pendingRelease.value = null
                        _state.value = UpdateState.Idle
                    }
                    // Declining the system dialog lands here too, so offer the install again
                    // rather than treating it as a dead end.
                    is InstallOutcome.Failed -> _state.value = (_state.value as? UpdateState.Installing)
                        ?.let { UpdateState.Failed(it.release, outcome.message) }
                        ?: _state.value
                }
            }
        }
    }

    /**
     * The only way an update is offered: a check on every launch that says nothing at all unless
     * there is something to install. A failed check is not worth interrupting anyone over.
     *
     * Finding one raises the banner and nothing else. Opening the app is not the moment to take
     * the whole screen away from someone who came to read a zman — the banner says an update is
     * there, and the dialog opens only when they ask for it.
     */
    fun checkOnLaunch() {
        if (checkedThisSession || _pendingRelease.value != null) return
        checkedThisSession = true
        viewModelScope.launch {
            // Anything still on disk belongs to a previous session that never finished installing —
            // this view model is new, so nothing here references it. Only a successful install used
            // to clear the cache, which left a whole APK behind after every abandoned update.
            repository.clearDownloads()
            when (val report = repository.check()) {
                is UpdateCheckReport.Available ->
                    _pendingRelease.value = UpdateState.Available(report.release, report.isDowngrade)
                // A check that never reached GitHub answered nothing, so it does not count as this
                // session's answer — offline at launch must not mean silence until the next one.
                is UpdateCheckReport.Failed -> checkedThisSession = false
                is UpdateCheckReport.UpToDate,
                is UpdateCheckReport.NoReleases,
                -> Unit
            }
        }
    }

    /** Reopens the dialog for the waiting update, from the banner. */
    fun showPendingRelease() {
        val pending = _pendingRelease.value ?: return
        if (_state.value == UpdateState.Idle) _state.value = pending
    }

    fun download(release: AppRelease) {
        viewModelScope.launch {
            _state.value = UpdateState.Downloading(release, 0L, release.sizeBytes)
            runCatching {
                repository.download(release) { downloaded, total ->
                    _state.value = UpdateState.Downloading(release, downloaded, total)
                }
            }.onSuccess { file ->
                downloaded = file
                startInstall(release)
            }.onFailure { error ->
                _state.value = UpdateState.Failed(release, error.message)
            }
        }
    }

    /**
     * Called whenever the app comes back to the front, which is the first moment Android will let
     * the system's confirmation dialog be raised. If the permission was granted while the user was
     * away, the APK has already been staged by [watchForInstallPermission] and this is only the
     * commit — so the confirmation appears at once rather than after a copy.
     */
    fun onResumed() {
        stagedSessionId?.let { sessionId ->
            stagedSessionId = null
            runCatching { installer.commit(sessionId) }
                .onFailure { error ->
                    val release = (_state.value as? UpdateState.Installing)?.release
                    _state.value = UpdateState.Failed(release, error.message)
                }
            return
        }
        val release = (_state.value as? UpdateState.NeedsInstallPermission)?.release ?: return
        if (installer.canInstall()) startInstall(release)
    }

    /**
     * Watches for the install permission being granted on the system screen.
     *
     * Nothing tells an app when that switch is flipped, so this asks. The moment it is granted the
     * APK is staged — work that is allowed from the background — leaving only the confirmation for
     * when the app is next in front. Android does not permit a backgrounded app to raise that
     * dialog, so this is as immediate as the install can be made.
     */
    private fun watchForInstallPermission(release: AppRelease) {
        permissionWatch?.cancel()
        permissionWatch = viewModelScope.launch {
            withTimeoutOrNull(PermissionWatchMillis) {
                while (!installer.canInstall()) delay(PermissionPollMillis)
            } ?: return@launch

            val apk = downloaded?.takeIf { it.exists() } ?: return@launch
            runCatching { installer.stage(apk) }
                .onSuccess { sessionId ->
                    stagedSessionId = sessionId
                    _state.value = UpdateState.Installing(release)
                }
                .onFailure { error -> _state.value = UpdateState.Failed(release, error.message) }
        }
    }

    private fun startInstall(release: AppRelease) {
        val apk = downloaded
        if (apk == null || !apk.exists()) {
            _state.value = UpdateState.Failed(release, null)
            return
        }
        if (!installer.canInstall()) {
            _state.value = UpdateState.NeedsInstallPermission(release)
            watchForInstallPermission(release)
            return
        }
        permissionWatch?.cancel()
        viewModelScope.launch {
            _state.value = UpdateState.Installing(release)
            runCatching { installer.commit(installer.stage(apk)) }
                .onFailure { error -> _state.value = UpdateState.Failed(release, error.message) }
        }
    }

    fun installPermissionIntent() = installer.manageUnknownSourcesIntent()

    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        permissionWatch?.cancel()
        // A session written but never committed would otherwise sit in the installer's storage.
        stagedSessionId?.let(installer::abandon)
        stagedSessionId = null
    }

    private companion object {
        const val PermissionPollMillis = 400L

        /** Long enough to find the switch, short enough not to poll forever if they never do. */
        const val PermissionWatchMillis = 5 * 60 * 1000L
    }
}
