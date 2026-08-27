// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.InstallMobile
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.noamtu.jewishday.R
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.noamtu.jewishday.ui.LocalUseHebrewInterface
import com.noamtu.jewishday.ui.localizedString
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DialogMaxWidth = 450.dp
private val DialogOuterPadding = 24.dp
private val CardCornerRadius = 20.dp
private val ButtonHeight = 52.dp
private val ProgressBarHeight = 8.dp
private val HeroIconContainerSize = 64.dp
private val HeroIconSize = 30.dp
private const val AnimationMillis = 300
private const val DialogEnterScale = 0.92f

// Tints are built from the theme's own roles at low alpha rather than fixed colors, so the same
// surfaces read correctly in both the light and dark palettes.
private const val ContainerAlpha = 0.14f
private const val BorderAlpha = 0.40f

/**
 * One dialog that follows an update the whole way: what is available, the download, the install,
 * and whatever went wrong.
 *
 * It fills the screen rather than sitting in a small alert box — an update is the only thing being
 * asked about, so it gets the whole surface, a large centered title, and full-width stacked
 * actions. The body crossfades between states so the dialog changes without jumping.
 */
@Composable
fun AppUpdateDialog(
    state: UpdateState,
    onDownload: (AppRelease) -> Unit,
    onOpenInstallSettings: () -> Unit,
    onDismiss: () -> Unit,
    /** Hidden developer switch: show the English changelog whatever the interface language is. */
    notesInEnglish: Boolean = false,
) {
    if (state is UpdateState.Idle) return

    // Keyed on which body is showing, never on the state object itself: a download emits a new
    // Downloading value on every progress callback, and crossfading between those restarts the
    // animation many times a second, which reads as the screen flickering.
    val content = state.content()

    UpdateDialogShell(
        title = state.title(),
        onDismissRequest = { if (state.isDismissable) onDismiss() },
        footer = {
            AnimatedContent(
                targetState = content,
                transitionSpec = crossfade(),
                modifier = Modifier.fillMaxWidth(),
                label = "updateFooter",
            ) { current ->
                UpdateDialogFooter(state.forContent(current), onDownload, onOpenInstallSettings, onDismiss)
            }
        },
    ) {
        AnimatedContent(
            targetState = content,
            transitionSpec = crossfade(),
            modifier = Modifier.fillMaxWidth(),
            label = "updateBody",
        ) { current -> UpdateDialogBody(state.forContent(current), notesInEnglish) }
    }
}

/**
 * The state this crossfade child should draw.
 *
 * The child arriving draws the live state, so a download's progress keeps moving. The one leaving
 * keeps the state it was created with, so it fades out still showing its own content instead of
 * blanking or borrowing its successor's.
 */
@Composable
private fun UpdateState.forContent(content: UpdateDialogContent): UpdateState {
    val captured = remember { this }
    return if (this.content() == content) this else captured
}

/** The distinct bodies this dialog can show, so states that share one do not restart its animation. */
private enum class UpdateDialogContent { None, Available, Downloading, Installing, Failed, NeedsPermission }

private fun UpdateState.content(): UpdateDialogContent = when (this) {
    is UpdateState.Idle -> UpdateDialogContent.None
    is UpdateState.Available -> UpdateDialogContent.Available
    is UpdateState.Downloading -> UpdateDialogContent.Downloading
    is UpdateState.Installing -> UpdateDialogContent.Installing
    is UpdateState.Failed -> UpdateDialogContent.Failed
    is UpdateState.NeedsInstallPermission -> UpdateDialogContent.NeedsPermission
}

/**
 * The full-screen frame every state shares: background, enter animation, centered column, the
 * large title, and the actions pinned below the content.
 */
@Composable
private fun UpdateDialogShell(
    title: String,
    onDismissRequest: () -> Unit,
    footer: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(AnimationMillis, easing = LinearOutSlowInEasing)) +
                    scaleIn(initialScale = DialogEnterScale, animationSpec = tween(AnimationMillis, easing = FastOutSlowInEasing)),
                exit = fadeOut() + scaleOut(targetScale = DialogEnterScale),
                modifier = Modifier.fillMaxSize(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(DialogOuterPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = DialogMaxWidth)
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        // The title tracks the state, so it crossfades in step with the body
                        // instead of snapping a new string into place.
                        AnimatedContent(
                            targetState = title,
                            transitionSpec = crossfade(),
                            label = "updateTitle",
                        ) { current ->
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                text = current,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Column(
                            modifier = Modifier
                                .padding(top = 24.dp)
                                .weight(1f, fill = false),
                            content = content,
                        )
                        Box(modifier = Modifier.padding(top = 28.dp)) { footer() }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateDialogBody(state: UpdateState, notesInEnglish: Boolean) {
    when (state) {
        is UpdateState.Idle -> Unit

        is UpdateState.Available -> Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            HeroCard(
                icon = Icons.Outlined.SystemUpdate,
                title = state.release.title.isolateLtr(),
                meta = state.release.metaLine(),
                metaIcon = Icons.Outlined.Schedule,
            )
            // Leaving the test channel lands on the newest *stable*, which can be an earlier build
            // than the pre-release in hand. It installs cleanly — pre-releases share the stable's
            // versionCode — but it is a step back, and the dialog should not call that an update
            // without saying so.
            if (state.isDowngrade) {
                Notice(
                    icon = Icons.Outlined.ErrorOutline,
                    text = localizedString(R.string.update_downgrade_body, R.string.update_downgrade_body_hebrew),
                    tone = MaterialTheme.colorScheme.error,
                )
            }
            if (state.release.notes.isNotBlank()) {
                ReleaseNotes(notes = state.release.notes, showInEnglish = notesInEnglish)
            }
        }

        is UpdateState.Downloading -> DownloadProgress(state)

        is UpdateState.Installing -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            CircularProgressIndicator()
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = localizedString(R.string.update_installing_body, R.string.update_installing_body_hebrew),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        is UpdateState.Failed -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Notice(
                icon = Icons.Outlined.ErrorOutline,
                text = localizedString(R.string.update_failed_body, R.string.update_failed_body_hebrew),
                tone = MaterialTheme.colorScheme.error,
            )
            // The installer's own words, when it gave any — usually more use than ours.
            state.message?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is UpdateState.NeedsInstallPermission -> Notice(
            icon = Icons.Outlined.InstallMobile,
            text = localizedString(R.string.update_permission_body, R.string.update_permission_body_hebrew),
            tone = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * The headline block: a tinted icon badge beside the release being offered, with the version and
 * one line about it. Laid out along the row rather than stacked, so it stays a compact header and
 * leaves the height to the release notes below.
 *
 * [footer] is drawn full width underneath, which is where the download bar goes.
 */
@Composable
private fun HeroCard(
    icon: ImageVector,
    title: String,
    meta: String?,
    metaIcon: ImageVector? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val accent = MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius),
        color = accent.copy(alpha = ContainerAlpha),
        border = BorderStroke(1.dp, accent.copy(alpha = BorderAlpha * 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(HeroIconContainerSize),
                    shape = RoundedCornerShape(percent = 50),
                    color = accent.copy(alpha = ContainerAlpha * 1.6f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            modifier = Modifier.size(HeroIconSize),
                            imageVector = icon,
                            contentDescription = null,
                            tint = accent,
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    meta?.let {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            metaIcon?.let { vector ->
                                Icon(
                                    modifier = Modifier.size(16.dp),
                                    imageVector = vector,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            footer?.invoke()
        }
    }
}

/** Publish date and download size on one line, whichever of the two GitHub gave us. */
@Composable
private fun AppRelease.metaLine(): String? {
    val date = publishedOn?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    val size = sizeBytes
        .takeIf { it > 0 }
        ?.let { localizedString(R.string.update_size, R.string.update_size_hebrew, it.asMegabytes()) }
    return listOfNotNull(date, size).takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

/** A full-width tinted block carrying a warning or a status line. */
@Composable
private fun Notice(icon: ImageVector, text: String, tone: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(CardCornerRadius * 0.7f),
        color = tone.copy(alpha = ContainerAlpha),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(modifier = Modifier.size(24.dp), imageVector = icon, contentDescription = null, tint = tone)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = tone)
        }
    }
}

/**
 * What's new: the release's own changelog, grouped into New / Improved / Fixed.
 *
 * The body carries English in the open and Hebrew inside an HTML comment (see [parseReleaseNotes]);
 * this picks the language matching the interface and takes the heading and text direction with it —
 * an English changelog laid out right-to-left puts the bullets on the wrong side and mangles the
 * punctuation. Its own scrolling area, so long notes never push the buttons off screen.
 */
@Composable
private fun ReleaseNotes(notes: String, showInEnglish: Boolean) {
    val useHebrew = LocalUseHebrewInterface.current && !showInEnglish
    val sections = remember(notes, useHebrew) { parseReleaseNotes(notes, useHebrew) }
    CompositionLocalProvider(
        LocalUseHebrewInterface provides useHebrew,
        LocalLayoutDirection provides if (useHebrew) LayoutDirection.Rtl else LayoutDirection.Ltr,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = localizedString(R.string.update_notes_title, R.string.update_notes_title_hebrew),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                sections.forEach { section -> ReleaseNoteGroup(section) }
            }
        }
    }
}

/** One category and its entries: a tinted label, then the bullets under it. */
@Composable
private fun ReleaseNoteGroup(section: ReleaseNoteSection) {
    val tone = section.category.tone()
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        section.label()?.let { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                section.category.icon()?.let { icon ->
                    Icon(
                        modifier = Modifier.size(16.dp),
                        imageVector = icon,
                        contentDescription = null,
                        tint = tone,
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = tone,
                )
            }
        }
        section.items.forEach { item ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "\u2022",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = item.asInlineMarkdown(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A recognised category is labelled in the interface language, not with the heading as written — so
 * a release whose Hebrew block is missing still says "תיקונים" rather than "Bug fixes". An
 * unrecognised heading keeps its own words, which is the only honest thing to do with it.
 */
@Composable
private fun ReleaseNoteSection.label(): String? = when (category) {
    ReleaseNoteCategory.New -> localizedString(R.string.update_notes_new, R.string.update_notes_new_hebrew)
    ReleaseNoteCategory.Improved -> localizedString(R.string.update_notes_improved, R.string.update_notes_improved_hebrew)
    ReleaseNoteCategory.Fixed -> localizedString(R.string.update_notes_fixed, R.string.update_notes_fixed_hebrew)
    ReleaseNoteCategory.Other -> heading.takeIf(String::isNotBlank)
}

@Composable
private fun ReleaseNoteCategory.tone(): Color = when (this) {
    ReleaseNoteCategory.New -> MaterialTheme.colorScheme.tertiary
    ReleaseNoteCategory.Improved -> MaterialTheme.colorScheme.primary
    ReleaseNoteCategory.Fixed -> MaterialTheme.colorScheme.secondary
    ReleaseNoteCategory.Other -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun ReleaseNoteCategory.icon(): ImageVector? = when (this) {
    ReleaseNoteCategory.New -> Icons.Outlined.AutoAwesome
    ReleaseNoteCategory.Improved -> Icons.Outlined.TrendingUp
    ReleaseNoteCategory.Fixed -> Icons.Outlined.BugReport
    ReleaseNoteCategory.Other -> null
}

/** Bold runs and code spans inside one changelog entry. */
private fun String.asInlineMarkdown(): AnnotatedString =
    buildAnnotatedString { appendInline(this@asInlineMarkdown) }

/** Bold runs and code spans within one line; everything else is left as written. */
private fun AnnotatedString.Builder.appendInline(line: String) {
    var rest = line.replace("`", "")
    while (true) {
        val open = rest.indexOf(BoldMark)
        val close = if (open < 0) -1 else rest.indexOf(BoldMark, open + BoldMark.length)
        if (open < 0 || close < 0) {
            append(rest)
            return
        }
        append(rest.substring(0, open))
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
            append(rest.substring(open + BoldMark.length, close))
        }
        rest = rest.substring(close + BoldMark.length)
    }
}

private const val BoldMark = "**"

/**
 * The download, with the bar animated between callbacks so it glides rather than stepping with
 * every buffer read. An unknown total shows an indeterminate bar instead of one pinned at zero.
 */
@Composable
private fun DownloadProgress(state: UpdateState.Downloading) {
    val progress = state.progress
    val animated by animateFloatAsState(
        targetValue = progress ?: 0f,
        animationSpec = tween(AnimationMillis),
        label = "downloadProgress",
    )
    val barModifier = Modifier
        .fillMaxWidth()
        .height(ProgressBarHeight)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    HeroCard(
        icon = Icons.Outlined.Download,
        title = state.release.title.isolateLtr(),
        meta = if (state.totalBytes > 0) {
            localizedString(
                R.string.update_progress_detail,
                R.string.update_progress_detail_hebrew,
                state.downloadedBytes.asMegabytes(),
                state.totalBytes.asMegabytes(),
                (animated * 100).toInt(),
            )
        } else {
            localizedString(
                R.string.update_progress_downloaded,
                R.string.update_progress_downloaded_hebrew,
                state.downloadedBytes.asMegabytes(),
            )
        },
        footer = {
            if (progress == null) {
                LinearProgressIndicator(modifier = barModifier, trackColor = trackColor)
            } else {
                LinearProgressIndicator(progress = { animated }, modifier = barModifier, trackColor = trackColor)
            }
        },
    )
}

/** The actions, stacked full width: the thing to do, then the way out. */
@Composable
private fun UpdateDialogFooter(
    state: UpdateState,
    onDownload: (AppRelease) -> Unit,
    onOpenInstallSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (state) {
            is UpdateState.Available -> DialogButton(
                text = localizedString(R.string.update_action_download, R.string.update_action_download_hebrew),
                icon = Icons.Outlined.Download,
                filled = true,
                onClick = { onDownload(state.release) },
            )

            is UpdateState.Failed -> DialogButton(
                text = localizedString(R.string.update_action_retry, R.string.update_action_retry_hebrew),
                icon = Icons.Outlined.Download,
                filled = true,
                onClick = { state.release?.let(onDownload) ?: onDismiss() },
            )

            // Opening Settings is all this does: the install is retried when the app comes back,
            // which is both the first moment the answer exists and the first moment Android will
            // let the confirmation dialog appear.
            is UpdateState.NeedsInstallPermission -> DialogButton(
                text = localizedString(R.string.update_action_open_settings, R.string.update_action_open_settings_hebrew),
                icon = Icons.Outlined.InstallMobile,
                filled = true,
                onClick = onOpenInstallSettings,
            )

            // Downloading and installing run to their own conclusion; there is nothing to confirm.
            else -> Unit
        }

        if (state.isDismissable) {
            val later = state is UpdateState.Available
            DialogButton(
                text = localizedString(
                    if (later) R.string.update_action_later else R.string.update_action_close,
                    if (later) R.string.update_action_later_hebrew else R.string.update_action_close_hebrew,
                ),
                icon = null,
                filled = false,
                onClick = onDismiss,
            )
        }
    }
}

/**
 * A dialog action: full width, tall enough to hit without looking, and tinted from the theme's
 * primary rather than filled solid, so the pair reads as one stack instead of two rival buttons.
 */
@Composable
private fun DialogButton(
    text: String,
    icon: ImageVector?,
    filled: Boolean,
    onClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    val shape = RoundedCornerShape(CardCornerRadius)
    val border = BorderStroke(1.dp, accent.copy(alpha = if (filled) BorderAlpha else BorderAlpha * 0.6f))
    val modifier = Modifier
        .fillMaxWidth()
        .height(ButtonHeight)
    val contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val content: @Composable RowScope.() -> Unit = {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let { Icon(modifier = Modifier.size(20.dp), imageVector = it, contentDescription = null) }
            Text(text = text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }

    if (filled) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = accent.copy(alpha = ContainerAlpha * 1.8f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            border = border,
            contentPadding = contentPadding,
            content = content,
        )
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            border = border,
            contentPadding = contentPadding,
            content = content,
        )
    }
}

/** Every state change in this dialog is a crossfade, so they are all specified in one place. */
private fun <S> crossfade(): AnimatedContentTransitionScope<S>.() -> ContentTransform = {
    fadeIn(tween(AnimationMillis)) togetherWith fadeOut(tween(AnimationMillis))
}

@Composable
private fun UpdateState.title(): String = when (this) {
    is UpdateState.Downloading -> localizedString(R.string.update_downloading_title, R.string.update_downloading_title_hebrew)
    is UpdateState.Installing -> localizedString(R.string.update_installing_title, R.string.update_installing_title_hebrew)
    is UpdateState.Failed -> localizedString(R.string.update_failed_title, R.string.update_failed_title_hebrew)
    is UpdateState.NeedsInstallPermission ->
        localizedString(R.string.update_permission_title, R.string.update_permission_title_hebrew)
    else -> localizedString(R.string.update_available_title, R.string.update_available_title_hebrew)
}

/**
 * Every state can be closed. A download keeps running without its dialog and reopens it when it
 * finishes, and the system owns an install once it starts — so closing only ever puts our own
 * dialog away, and no state can strand anyone behind a screen with no way out.
 */
private val UpdateState.isDismissable: Boolean
    get() = this !is UpdateState.Idle

internal fun Long.asMegabytes(): String = String.format(Locale.getDefault(), "%.1f", this / 1_048_576.0)

/**
 * Keeps a version number reading left to right inside Hebrew text, which otherwise reorders it —
 * "0.9.1" is not a phrase to be mirrored. Written as escapes because the isolate characters are
 * invisible, and an invisible character pasted into source is exactly what lint warns about.
 */
internal fun String.isolateLtr(): String = "\u2066" + this + "\u2069"
