// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import java.time.format.DateTimeParseException
import org.json.JSONArray

/**
 * Reads the app's own releases from GitHub.
 *
 * Pre-releases are excluded unless the user has opted into them in Settings: a stable install must
 * never be pulled onto a test build by accident. The API is called unauthenticated, which allows 60
 * requests an hour per address — far more than the once-per-launch check this is used for.
 */
@Singleton
class GitHubReleaseClient @Inject constructor() {

    /** Every installable release, newest first. Which one to offer is the repository's decision. */
    fun fetchReleases(): List<AppRelease> {
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
            parseReleases(body)
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
        val downloadUrl = apk.optString("browser_download_url", "")
        // Defence in depth. The asset URL is attacker-controlled input in the sense that it comes
        // off the wire, and it ends up being downloaded and handed to the package installer, so a
        // release that names anywhere other than GitHub over https is simply not offered.
        if (!isTrustedDownloadUrl(downloadUrl)) return@mapNotNull null

        AppRelease(
            version = version,
            // GitHub's own flag is authoritative, but a version carrying a pre-release suffix counts
            // too — forgetting the checkbox must not push a test build to everyone.
            isPreRelease = release.optBoolean("prerelease", false) || version.isPreRelease,
            title = release.optString("name", "").ifBlank { version.toString() },
            notes = release.optString("body", "").trim(),
            downloadUrl = downloadUrl,
            sizeBytes = apk.optLong("size", 0L),
            publishedAt = parsePublishedAt(release.optString("published_at", "")),
        )
    }
        // Newest first, by when GitHub published it. Deliberately not by version string: whatever
        // was released last is the latest, and nothing here has to understand numbering to say so.
        .sortedByDescending { release -> release.publishedAt ?: Instant.MIN }
}

/** True only for `https://github.com/...` (or a github.com subdomain) asset URLs. */
internal fun isTrustedDownloadUrl(value: String): Boolean = runCatching {
    val url = URL(value)
    val host = url.host.orEmpty().lowercase()
    url.protocol.equals("https", ignoreCase = true) &&
        (host == "github.com" || host.endsWith(".github.com"))
}.getOrDefault(false)

/**
 * GitHub timestamps this in UTC. A release GitHub never dated — or dated in a shape this does not
 * know — has none, and sorts to the bottom rather than being mistaken for the newest.
 */
private fun parsePublishedAt(timestamp: String): Instant? {
    if (timestamp.isBlank()) return null
    return try {
        Instant.parse(timestamp)
    } catch (_: DateTimeParseException) {
        null
    }
}
