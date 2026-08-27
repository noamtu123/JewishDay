// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParsingTest {

    @Test
    fun versionsParseFromEveryTagShapeTheProjectUses() {
        assertEquals(AppVersion(0, 9, 1), AppVersion.parse("alpha-0.9.1"))
        assertEquals(AppVersion(1, 0, 0), AppVersion.parse("v1.0.0"))
        assertEquals(AppVersion(2, 13, 4), AppVersion.parse("2.13.4"))
        assertEquals(AppVersion(0, 9, 0), AppVersion.parse("Alpha 0.9.0"))
        assertNull(AppVersion.parse("nightly"))
        assertNull(AppVersion.parse(null))
    }

    @Test
    fun aPreReleaseIsADifferentVersionFromTheStableOfTheSameNumber() {
        // The only job the suffix has. Without it a tester on 1.0.0-pre.1 would look like they were
        // already running 1.0.0 and would never be offered the finished build.
        assertEquals(AppVersion(1, 0, 0, preRelease = 3), AppVersion.parse("v1.0.0-pre.3"))
        assertEquals(AppVersion(1, 0, 0, preRelease = 1), AppVersion.parse("v1.0.0-pre"))
        assertEquals(AppVersion(2, 1, 0, preRelease = 4), AppVersion.parse("2.1.0-beta.4"))
        assertTrue(AppVersion.parse("v1.0.0-pre.1") != AppVersion.parse("v1.0.0"))

        // "alpha-" is a prefix on the historical tags, not a pre-release suffix.
        assertEquals(AppVersion(0, 9, 1), AppVersion.parse("alpha-0.9.1"))
        assertFalse(AppVersion.parse("alpha-0.9.1")!!.isPreRelease)
        assertTrue(AppVersion.parse("v1.0.0-pre.1")!!.isPreRelease)

        assertEquals("1.0.0-pre.3", AppVersion.parse("v1.0.0-pre.3").toString())
        assertEquals("1.0.0", AppVersion.parse("v1.0.0").toString())
    }

    @Test
    fun releasesComeBackNewestFirstByPublishDateNotByVersionString() {
        // 0.9.0 was published *after* 2.0.0 here. GitHub's date is what decides, so 0.9.0 is newest
        // — sorting by the version string would get this backwards.
        val releases = parseReleases(
            """
            [
              {"tag_name":"v2.0.0","published_at":"2026-01-01T10:00:00Z",
               "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/2.apk","size":2}]},
              {"tag_name":"v0.9.0","published_at":"2026-06-01T10:00:00Z",
               "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/0.apk","size":1}]}
            ]
            """.trimIndent(),
        )

        assertEquals(
            listOf(AppVersion(0, 9, 0), AppVersion(2, 0, 0)),
            releases.map { it.version },
        )
        assertEquals(Instant.parse("2026-06-01T10:00:00Z"), releases.first().publishedAt)
    }

    @Test
    fun anUndatedReleaseSortsLastRatherThanFirst() {
        val releases = parseReleases(
            """
            [
              {"tag_name":"v1.0.0",
               "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/1.apk","size":1}]},
              {"tag_name":"v1.1.0","published_at":"2026-06-01T10:00:00Z",
               "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/2.apk","size":1}]}
            ]
            """.trimIndent(),
        )

        assertEquals(AppVersion(1, 1, 0), releases.first().version)
        assertNull(releases.last().publishedAt)
    }

    @Test
    fun theNewestReleaseCarriesItsDetails() {
        val releases = parseReleases(
            """
            [{"tag_name":"v1.0.0","name":"1.0.0","body":"newest","published_at":"2026-06-01T10:00:00Z",
              "assets":[{"name":"JewishDay-1.0.0.apk","browser_download_url":"https://github.com/x/1.0.0.apk","size":2000}]}]
            """.trimIndent(),
        )

        val newest = releases.single()
        assertEquals("https://github.com/x/1.0.0.apk", newest.downloadUrl)
        assertEquals(2000L, newest.sizeBytes)
        assertEquals("newest", newest.notes)
    }

    @Test
    fun releasesWithoutAnInstallableApkAreSkipped() {
        val releases = parseReleases(
            """
            [
              {"tag_name":"v0.9.2","assets":[{"name":"mapping.txt","browser_download_url":"https://github.com/x/m.txt","size":5}]},
              {"tag_name":"v0.9.3","draft":true,
               "assets":[{"name":"JewishDay-0.9.3.apk","browser_download_url":"https://github.com/x/d.apk","size":5}]},
              {"tag_name":"nightly",
               "assets":[{"name":"JewishDay-nightly.apk","browser_download_url":"https://github.com/x/n.apk","size":5}]},
              {"tag_name":"v0.9.4",
               "assets":[{"name":"JewishDay-0.9.4.apk","browser_download_url":"http://evil.example/x.apk","size":5}]}
            ]
            """.trimIndent(),
        )

        // No APK, a draft, an unparseable tag, and an asset that is not https GitHub.
        assertTrue(releases.toString(), releases.isEmpty())
    }

    @Test
    fun preReleasesAreParsedButFlagged() {
        val releases = parseReleases(
            """
            [
              {"tag_name":"v1.0.0-pre.2","prerelease":true,"published_at":"2026-06-02T10:00:00Z",
               "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/a.apk","size":9}]},
              {"tag_name":"v0.9.0","published_at":"2026-06-01T10:00:00Z",
               "assets":[{"name":"b.apk","browser_download_url":"https://github.com/x/b.apk","size":9}]}
            ]
            """.trimIndent(),
        )

        // Parsing keeps both; the channel filter in the repository is what drops the pre-release.
        assertTrue(releases[0].isPreRelease)
        assertFalse(releases[1].isPreRelease)
        assertEquals(AppVersion(0, 9, 0), releases.first { !it.isPreRelease }.version)
    }

    @Test
    fun aMissedPreReleaseCheckboxIsCaughtByTheVersion() {
        // The GitHub flag is authoritative, but forgetting it must not push a test build at everyone.
        val releases = parseReleases(
            """
            [{"tag_name":"v1.2.0-pre.1",
              "assets":[{"name":"a.apk","browser_download_url":"https://github.com/x/a.apk","size":9}]}]
            """.trimIndent(),
        )

        assertTrue(releases.single().isPreRelease)
    }
}
