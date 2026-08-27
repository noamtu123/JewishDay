// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import org.junit.Assert.assertEquals
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
    fun versionsCompareByEachPartInTurn() {
        assertTrue(AppVersion(0, 10, 0) > AppVersion(0, 9, 9))
        assertTrue(AppVersion(1, 0, 0) > AppVersion(0, 99, 99))
        assertTrue(AppVersion(0, 9, 2) > AppVersion(0, 9, 1))
        assertEquals(0, AppVersion(0, 9, 1).compareTo(AppVersion(0, 9, 1)))
    }

    @Test
    fun theNewestInstallableReleaseWins() {
        val releases = parseReleases(
            """
            [
              {"tag_name":"alpha-0.9.0","name":"Alpha 0.9.0","body":"first",
               "assets":[{"name":"JewishDay-alpha-0.9.0.apk","browser_download_url":"https://x/0.9.0.apk","size":1000}]},
              {"tag_name":"alpha-0.10.0","name":"Alpha 0.10.0","body":"newest",
               "assets":[{"name":"JewishDay-alpha-0.10.0.apk","browser_download_url":"https://x/0.10.0.apk","size":2000}]}
            ]
            """.trimIndent(),
        )

        assertEquals(2, releases.size)
        val newest = requireNotNull(releases.maxByOrNull { it.version })
        assertEquals(AppVersion(0, 10, 0), newest.version)
        assertEquals("https://x/0.10.0.apk", newest.downloadUrl)
        assertEquals(2000L, newest.sizeBytes)
        assertEquals("newest", newest.notes)
    }

    @Test
    fun releasesWithoutAnInstallableApkAreSkipped() {
        val releases = parseReleases(
            """
            [
              {"tag_name":"alpha-0.9.2","assets":[{"name":"mapping.txt","browser_download_url":"https://x/m.txt","size":5}]},
              {"tag_name":"alpha-0.9.3","draft":true,
               "assets":[{"name":"JewishDay-alpha-0.9.3.apk","browser_download_url":"https://x/d.apk","size":5}]},
              {"tag_name":"nightly",
               "assets":[{"name":"JewishDay-nightly.apk","browser_download_url":"https://x/n.apk","size":5}]}
            ]
            """.trimIndent(),
        )

        // No APK, a draft, and an unparseable tag — none of them installable.
        assertTrue(releases.toString(), releases.isEmpty())
    }

    @Test
    fun prereleasesCount() {
        val releases = parseReleases(
            """
            [{"tag_name":"alpha-0.9.1","prerelease":true,
              "assets":[{"name":"JewishDay-alpha-0.9.1.apk","browser_download_url":"https://x/a.apk","size":9}]}]
            """.trimIndent(),
        )

        // Every alpha ships as a pre-release, so excluding them would find nothing, ever.
        assertEquals(AppVersion(0, 9, 1), releases.single().version)
    }
}
