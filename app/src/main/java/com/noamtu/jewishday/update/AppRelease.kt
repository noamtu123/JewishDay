// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One release on GitHub that carries an installable APK. */
data class AppRelease(
    val version: AppVersion,
    /** The release's own title, e.g. "1.0.0". */
    val title: String,
    /** The release notes, shown in the update dialog. Markdown as GitHub returns it. */
    val notes: String,
    val downloadUrl: String,
    /** Size of the APK in bytes, as GitHub reports it; 0 when unknown. */
    val sizeBytes: Long,
    /**
     * When GitHub published it. This is what decides which release is the newest — not the version
     * string. Whatever was published last is the latest, full stop.
     */
    val publishedAt: Instant? = null,
    /** Marked pre-release on GitHub, or carrying a pre-release version suffix. */
    val isPreRelease: Boolean = false,
) {
    /** The publish day, for the line under the title in the dialog. */
    val publishedOn: LocalDate? get() = publishedAt?.atZone(ZoneId.systemDefault())?.toLocalDate()
}
