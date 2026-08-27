// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.provider.Settings
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/** How an install attempt ended. */
sealed interface InstallOutcome {
    data object Succeeded : InstallOutcome

    /** Includes the user simply declining the system dialog. */
    data class Failed(val message: String?) : InstallOutcome
}

/**
 * Hands a downloaded APK to the system installer.
 *
 * Uses [PackageInstaller] sessions rather than an ACTION_VIEW intent on a content URI: the file
 * never leaves the app's own storage, so there is no FileProvider and no read permission granted to
 * other apps. The user still confirms in the system dialog — this stages the bytes and asks.
 */
@Singleton
class ApkInstaller @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /** True when the user has allowed this app to install packages. */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    /** The system screen where that permission is granted, for when it has to be asked for. */
    fun manageUnknownSourcesIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            .setData("package:${context.packageName}".toUri())

    /**
     * Copies [apk] into a fresh install session and returns its id, without committing.
     *
     * Staging and committing are separate because only one of them can be done from the
     * background. Copying several megabytes is fine there; committing raises the system's
     * confirmation dialog, which Android will not allow a backgrounded app to do. Splitting them
     * means the waiting can happen while the user is still in Settings, leaving nothing but the
     * confirmation itself for the moment they come back.
     */
    suspend fun stage(apk: File): Int = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            .apply { setAppPackageName(context.packageName) }

        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite(WriteName, 0, apk.length()).use { output ->
                apk.inputStream().use { input -> input.copyTo(output) }
                session.fsync(output)
            }
        }
        sessionId
    }

    /**
     * Commits an already staged session, which is what raises the system's confirmation dialog.
     * Call it only with the app in the foreground. The outcome arrives through [InstallResults].
     */
    fun commit(sessionId: Int) {
        val installer = context.packageManager.packageInstaller
        installer.openSession(sessionId).use { session ->
            val callback = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(context, ApkInstallResultReceiver::class.java)
                    .setAction(ApkInstallResultReceiver.ActionInstallResult),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(callback.intentSender)
        }
    }

    /** Throws away a session that was staged but never committed. */
    fun abandon(sessionId: Int) {
        runCatching { context.packageManager.packageInstaller.abandonSession(sessionId) }
    }

    private companion object {
        const val WriteName = "jewishday-update"
    }
}

/** Where install outcomes surface, since the installer reports them by broadcast. */
@Singleton
class InstallResults @Inject constructor() {
    private val _outcomes = MutableSharedFlow<InstallOutcome>(extraBufferCapacity = 1)
    val outcomes: SharedFlow<InstallOutcome> = _outcomes.asSharedFlow()

    fun publish(outcome: InstallOutcome) {
        _outcomes.tryEmit(outcome)
    }
}
