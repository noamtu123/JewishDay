// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException
import org.json.JSONArray

/**
 * Reads the app's own releases from GitHub.
 *
 * Pre-releases are deliberately included: every alpha is published as one, so filtering them out
 * would mean never finding an update at all. The API is called unauthenticated, which allows 60
 * requests an hour per address — far more than the once-a-day check this is used for.
 */
@Singleton
class GitHubReleaseClient @Inject constructor() {

    /** The newest release carrying an APK, or null when the list has none. */
    fun fetchLatestRelease(): AppRelease? {
        val connection = URL(ReleasesUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = ConnectTimeoutMillis
        connection.readTimeout = ReadTimeoutMillis
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        return try {
            if (connection.responseCode !in 200..299) {
                // 404 is the answer for a private repository as well as a missing one: GitHub does
                // not admit that a repo you cannot see exists. Worth saying, since the releases can
                // look perfectly fine in a browser you are signed into.
                val hint = if (connection.responseCode == 404) {
                    " (the repository is private or does not exist)"
                } else {
                    ""
                }
                throw IOException("GitHub returned HTTP ${connection.responseCode}$hint")
            }
            val body = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
            parseReleases(body).maxByOrNull { it.version }
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val ReleasesUrl = "https://api.github.com/repos/noamtu123/JewishDay/releases?per_page=20"
        const val ConnectTimeoutMillis = 10_000
        const val ReadTimeoutMillis = 15_000
    }
}

/**
 * Turns the releases payload into the ones this app can actually install: a parseable version and
 * an APK asset. Drafts are skipped — they are not downloadable.
 */
internal fun parseReleases(json: String): List<AppRelease> {
    val releases = JSONArray(json)
    return (0 until releases.length()).mapNotNull { index ->
        val release = releases.optJSONObject(index) ?: return@mapNotNull null
        if (release.optBoolean("draft", false)) return@mapNotNull null

        val version = AppVersion.parse(release.optString("tag_name", null))
            ?: AppVersion.parse(release.optString("name", null))
            ?: return@mapNotNull null

        val assets = release.optJSONArray("assets") ?: return@mapNotNull null
        val apk = (0 until assets.length())
            .mapNotNull(assets::optJSONObject)
            .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
            ?: return@mapNotNull null
        val downloadUrl = apk.optString("browser_download_url", "").ifBlank { return@mapNotNull null }

        AppRelease(
            version = version,
            title = release.optString("name", "").ifBlank { version.toString() },
            notes = release.optString("body", "").trim(),
            downloadUrl = downloadUrl,
            sizeBytes = apk.optLong("size", 0L),
            publishedOn = parsePublishedOn(release.optString("published_at", "")),
        )
    }
}

/**
 * GitHub timestamps this in UTC; it is shown as a plain day, so it is read in the device's own
 * zone. A release GitHub never dated — or dated in a shape this does not know — simply has none.
 */
private fun parsePublishedOn(timestamp: String): LocalDate? {
    if (timestamp.isBlank()) return null
    return try {
        Instant.parse(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    } catch (_: DateTimeParseException) {
        null
    }
}
