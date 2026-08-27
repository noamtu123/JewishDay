// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import java.time.LocalDate

/** One release on GitHub that carries an installable APK. */
data class AppRelease(
    val version: AppVersion,
    /** The release's own title, e.g. "Alpha 0.9.1". */
    val title: String,
    /** The release notes, shown in the update dialog. Markdown as GitHub returns it. */
    val notes: String,
    val downloadUrl: String,
    /** Size of the APK in bytes, as GitHub reports it; 0 when unknown. */
    val sizeBytes: Long,
    /** The day it was published, or null when GitHub did not say. */
    val publishedOn: LocalDate? = null,
)
