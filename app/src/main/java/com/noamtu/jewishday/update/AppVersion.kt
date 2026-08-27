// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

/**
 * A release version, for deciding whether what is on GitHub is newer than what is installed.
 *
 * Releases are tagged `alpha-X.Y.Z`, but `vX.Y.Z` and a bare `X.Y.Z` are accepted too so a change
 * of tagging convention does not silently stop updates from being found.
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int = compareValuesBy(
        this,
        other,
        AppVersion::major,
        AppVersion::minor,
        AppVersion::patch,
    )

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val Pattern = Regex("""(\d+)\.(\d+)\.(\d+)""")

        /** Reads the first `X.Y.Z` in [text], ignoring any prefix. Null when there is none. */
        fun parse(text: String?): AppVersion? {
            val match = text?.let(Pattern::find) ?: return null
            val (major, minor, patch) = match.destructured
            return AppVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null,
            )
        }
    }
}
