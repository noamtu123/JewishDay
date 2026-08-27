// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import android.content.Context
import com.noamtu.jewishday.BuildConfig
import com.noamtu.jewishday.data.DeveloperOverridesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

@Singleton
class AppUpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val releaseClient: GitHubReleaseClient,
    private val developerOverrides: DeveloperOverridesRepository,
) {

    /**
     * The version to compare releases against: this build's, or whatever the developer tools are
     * spoofing so the flow can be exercised without publishing anything.
     *
     * Suspends because the overrides are read from DataStore. Reading the cached snapshot instead
     * would race the launch check and silently see "no spoof" every time.
     */
    private suspend fun installedVersion(): AppVersion? =
        AppVersion.parse(developerOverrides.current().spoofedVersionName.ifBlank { BuildConfig.VERSION_NAME })

    /**
     * The newest release worth offering, or null when this build is already current — or when the
     * check simply could not be made. A failed check is not worth interrupting anyone over.
     */
    suspend fun findUpdate(): AppRelease? = (check() as? UpdateCheckReport.Available)?.release

    /**
     * The same check as [findUpdate], but saying what happened rather than only what to install.
     *
     * Nothing user-facing shows this: a launch check that finds nothing, or cannot reach GitHub at
     * all, stays quiet by design. The developer tools show it, because "no dialog appeared" is
     * otherwise indistinguishable between "already current" and "the request never succeeded".
     */
    suspend fun check(): UpdateCheckReport = withContext(Dispatchers.IO) {
        val installed = installedVersion()
            ?: return@withContext UpdateCheckReport.Failed("Could not read the installed version")
        val latest = runCatching { releaseClient.fetchLatestRelease() }
            .getOrElse { error ->
                return@withContext UpdateCheckReport.Failed(
                    error.message ?: error::class.java.simpleName,
                )
            }
            ?: return@withContext UpdateCheckReport.NoReleases(installed)
        if (latest.version > installed) {
            UpdateCheckReport.Available(installed, latest)
        } else {
            UpdateCheckReport.UpToDate(installed, latest.version)
        }
    }

    /**
     * Downloads [release] into the app's own cache, reporting bytes written and the total expected.
     * The total is 0 while the server declines to say how large the file is.
     */
    suspend fun download(release: AppRelease, onProgress: (downloaded: Long, total: Long) -> Unit): File =
        withContext(Dispatchers.IO) {
            val target = File(updatesDir(), "JewishDay-${release.version}.apk")
            target.delete()

            val connection = URL(release.downloadUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = ConnectTimeoutMillis
            connection.readTimeout = ReadTimeoutMillis
            connection.instanceFollowRedirects = true
            try {
                if (connection.responseCode !in 200..299) {
                    throw IOException("Download returned HTTP ${connection.responseCode}")
                }
                val total = connection.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                var written = 0L
                connection.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(DownloadBufferBytes)
                        while (true) {
                            coroutineContext.ensureActive()
                            val read = input.read(buffer)
                            if (read <= 0) break
                            output.write(buffer, 0, read)
                            written += read
                            onProgress(written, total)
                        }
                    }
                }
                target
            } catch (error: Throwable) {
                // A half-written APK is worse than none: the installer would only reject it.
                target.delete()
                throw error
            } finally {
                connection.disconnect()
            }
        }

    /** Clears anything left behind by an update that was downloaded but never installed. */
    fun clearDownloads() {
        updatesDir().listFiles()?.forEach(File::delete)
    }

    private fun updatesDir(): File = File(context.cacheDir, "updates").apply { mkdirs() }

    private companion object {
        const val ConnectTimeoutMillis = 15_000
        const val ReadTimeoutMillis = 30_000
        const val DownloadBufferBytes = 64 * 1024
    }
}

/** The outcome of one update check, in enough detail to explain a check that offered nothing. */
sealed interface UpdateCheckReport {
    data class Available(val installed: AppVersion, val release: AppRelease) : UpdateCheckReport

    data class UpToDate(val installed: AppVersion, val latest: AppVersion) : UpdateCheckReport

    /** GitHub answered, but no release carried an installable APK. */
    data class NoReleases(val installed: AppVersion) : UpdateCheckReport

    data class Failed(val reason: String) : UpdateCheckReport
}
