// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.feature.about

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.BuildConfig
import com.noamtu.jewishday.R
import com.noamtu.jewishday.ui.components.InfoCard
import com.noamtu.jewishday.ui.components.ScreenPaddingValues
import com.noamtu.jewishday.ui.components.ScreenSurface
import com.noamtu.jewishday.ui.components.readableWidth
import com.noamtu.jewishday.ui.localizedString

private const val GithubUrl = "https://github.com/noamtu123/JewishDay"
private const val ContactEmail = "jewishdayapp@gmail.com"
private const val TapsToUnlockDeveloperMode = 7

@Composable
fun AboutScreen(
    modifier: Modifier = Modifier,
    onOpenDeveloperTools: () -> Unit = {},
    viewModel: AboutViewModel = hiltViewModel(),
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val developerModeUnlocked by viewModel.developerModeUnlocked.collectAsStateWithLifecycle()
    val aboutInEnglish by viewModel.aboutInEnglish.collectAsStateWithLifecycle()

    // Hidden unlock: tap the version number 7x (like Android's Developer Options).
    val tapState = remember { VersionTapState() }

    // Developer override: force this page (and its layout direction) to English even when the rest
    // of the app is in Hebrew. Off, it renders in the current interface language as usual.
    val showHebrew = LocalUseHebrewInterface.current && !aboutInEnglish
    CompositionLocalProvider(
        LocalUseHebrewInterface provides showHebrew,
        LocalLayoutDirection provides if (showHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
    ScreenSurface(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .readableWidth()
                .fillMaxSize(),
            contentPadding = ScreenPaddingValues,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                AboutHeader(
                    onVersionTap = {
                        if (!developerModeUnlocked && tapState.registerTap()) {
                            viewModel.unlockDeveloperMode()
                            Toast.makeText(context, "Developer mode unlocked", Toast.LENGTH_SHORT).show()
                        }
                    },
                )
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = localizedString(R.string.about_body, R.string.about_body_hebrew),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                InfoCard(modifier = Modifier.fillMaxWidth()) {
                    AboutLinkRow(
                        label = localizedString(R.string.about_github, R.string.about_github_hebrew),
                        value = "github.com/noamtu123/JewishDay",
                        onClick = { uriHandler.openUri(GithubUrl) },
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                    AboutLinkRow(
                        label = localizedString(R.string.about_email, R.string.about_email_hebrew),
                        value = ContactEmail,
                        onClick = { uriHandler.openUri("mailto:$ContactEmail") },
                    )
                }
            }
            if (developerModeUnlocked) {
                item {
                    InfoCard(modifier = Modifier.fillMaxWidth()) {
                        AboutLinkRow(
                            label = "Developer tools",
                            value = "Override date, time and location for testing",
                            onClick = onOpenDeveloperTools,
                        )
                    }
                }
            }
        }
    }
    }
}

/** Counts rapid taps on the version line; a slow tap resets the run. */
private class VersionTapState {
    private var count = 0
    private var lastTapMs = 0L

    /** Returns true when this tap completes the unlock sequence. */
    fun registerTap(): Boolean {
        val now = System.currentTimeMillis()
        count = if (now - lastTapMs <= TapWindowMs) count + 1 else 1
        lastTapMs = now
        return count >= TapsToUnlockDeveloperMode
    }

    private companion object {
        const val TapWindowMs = 2_000L
    }
}

@Composable
private fun AboutHeader(
    onVersionTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = localizedString(R.string.about_tagline, R.string.about_tagline_hebrew),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.clickable(onClick = onVersionTap),
            text = localizedString(R.string.about_version, R.string.about_version_hebrew, BuildConfig.VERSION_NAME),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun AboutLinkRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}