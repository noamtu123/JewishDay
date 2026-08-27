// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

/**
 * A release version — which build this is, not which build is newer.
 *
 * This is an *identity*, not an ordering: which release is newest is decided by GitHub's publish
 * date, never by comparing these. All this has to do is tell two releases apart — which is why the
 * pre-release suffix is kept, so `1.0.0-pre.1` and `1.0.0` are not mistaken for the same build and
 * a tester on the pre-release still gets offered the finished one.
 *
 * A bare `X.Y.Z` and the historical `alpha-X.Y.Z` tags are accepted too, so a change of tagging
 * convention does not silently stop updates from being found. Note the difference: `alpha-1.0.0` is
 * a *prefix* and parses as the stable 1.0.0, while `1.0.0-alpha.2` is a suffix and parses as a
 * pre-release — which matches how each was actually published.
 */
data class AppVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    /** Null for a stable release; any number marks this as a pre-release of that version. */
    val preRelease: Int? = null,
) {

    val isPreRelease: Boolean get() = preRelease != null

    override fun toString(): String =
        if (preRelease == null) "$major.$minor.$patch" else "$major.$minor.$patch-pre.$preRelease"

    companion object {
        // The suffix is optional, and its number is optional within it: "-pre" alone counts as the
        // first pre-release of that version rather than as no pre-release at all.
        private val Pattern = Regex(
            """(\d+)\.(\d+)\.(\d+)(?:-(?:pre|beta|rc|alpha)\.?(\d+)?)?""",
            RegexOption.IGNORE_CASE,
        )

        /** Reads the first version in [text], ignoring any prefix. Null when there is none. */
        fun parse(text: String?): AppVersion? {
            val match = text?.let(Pattern::find) ?: return null
            val (major, minor, patch, preRelease) = match.destructured
            val hasSuffix = match.value.contains('-')
            return AppVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null,
                preRelease = when {
                    !hasSuffix -> null
                    preRelease.isEmpty() -> 1
                    else -> preRelease.toIntOrNull() ?: 1
                },
            )
        }
    }
}
