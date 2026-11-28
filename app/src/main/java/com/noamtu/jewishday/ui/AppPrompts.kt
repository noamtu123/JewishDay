// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noamtu.jewishday.R
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.hasNotificationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import com.noamtu.jewishday.update.AppUpdateDialog
import com.noamtu.jewishday.update.AppUpdateViewModel

/**
 * First-launch only: request the POST_NOTIFICATIONS permission (Android 13+) so the Hebrew date
 * icon — on by default — can actually appear. Runs only after the location prompt has settled, so
 * the two system dialogs don't collide. On grant the setting is turned on and its service starts
 * immediately; on rejection the setting is turned off. On older versions the permission is implicit,
 * so we just start it. Shown once, then never again.
 */
@Composable
internal fun NotificationPermissionSetup(
    canPrompt: Boolean,
    viewModel: NotificationSetupViewModel = hiltViewModel(),
) {
    val needsPrompt by viewModel.needsFirstLaunchPrompt.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onPermissionResult(granted) }

    LaunchedEffect(canPrompt, needsPrompt) {
        if (!canPrompt || !needsPrompt) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            viewModel.onPermissionResult(granted = true)
        }
    }

    // On every later foreground, keep the setting honest: without the permission it is forced off
    // (so the switch can never promise an icon the OS will not let us post), and with it the
    // service is restarted, since it does not survive an upgrade, a force-stop, or being reclaimed.
    // One-directional — granting the permission from system settings never turns a switch the user
    // left off back on. Skipped while the first-launch prompt above still owns the setting.
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, needsPrompt) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START && !needsPrompt) {
                viewModel.reconcile(hasPermission = context.hasNotificationPermission())
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * Looks for a new release every time the app is opened — cold, or warm from the background — and,
 * only if there is one, raises the update banner. A check that finds nothing — or fails outright —
 * says nothing at all, so an ordinary launch is untouched by it.
 */
@Composable
internal fun AppUpdatePrompt(viewModel: AppUpdateViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notesInEnglish by viewModel.notesInEnglish.collectAsStateWithLifecycle()

    // ON_START is the app becoming visible: a cold launch and every return from the background,
    // but not a system popup (such as the location permission prompt) closing over it, which only
    // pauses and resumes. A rotation stops and starts the screen as well, so ON_STOP reports whether
    // the Activity is only being recreated, and that start is not counted as an opening.
    //
    // Granting the install permission happens on a system screen, so the only place to notice it
    // was granted is on the way back in — ON_RESUME.
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> viewModel.onAppShown()
                Lifecycle.Event.ON_RESUME -> viewModel.onResumed()
                Lifecycle.Event.ON_STOP -> viewModel.onAppHidden(activity?.isChangingConfigurations == true)
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AppUpdateDialog(
        state = state,
        notesInEnglish = notesInEnglish,
        onDownload = viewModel::download,
        onOpenInstallSettings = {
            runCatching {
                context.startActivity(viewModel.installPermissionIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        },
        onDismiss = viewModel::dismiss,
    )
}

/**
 * Once, after the update that made Glass the theme for everyone: a short note that the app moved
 * them to it, and that Settings has their old theme and every other. Waits for the location prompt,
 * so the two dialogs never stack.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GlassThemeNotice(
    canShow: Boolean,
    viewModel: GlassThemeNoticeViewModel = hiltViewModel(),
) {
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    if (!pending || !canShow) return
    // A small card rather than an AlertDialog, whose fixed title, padding and button row make a
    // two-line note look like a form.
    BasicAlertDialog(onDismissRequest = viewModel::dismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 8.dp, bottom = 4.dp)) {
                Text(
                    stringResource(R.string.glass_theme_notice_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp),
                )
                TextButton(
                    onClick = viewModel::dismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text(stringResource(R.string.glass_theme_notice_ok))
                }
            }
        }
    }
}

private enum class LocationPromptKind { None, Permission, Services }

/**
 * App-wide location handling, run on every foreground (cold start and each return from recents):
 *
 * - Permission + location on → pull a fresh fix immediately (zmanim compute right away).
 * - No permission → ask the OS directly the first time; if denied (or on later opens) show our own
 *   dialog offering the three real answers.
 * - Location switch off → the same dialog, pointing at Settings instead. (Android has no in-app
 *   system prompt to flip that switch without Google Play Services.)
 *
 * The dialog used to be a two-button "Grant permission" / "Use Jerusalem", where "Use Jerusalem"
 * meant only "not now" — so someone who had decided got asked again on every single launch. It now
 * separates "this time" from "always", and honours the second.
 *
 * The app never remembers a past location, so the fallback is always Jerusalem.
 */
@Composable
internal fun LocationAvailabilityPrompt(
    onSettled: () -> Unit,
    viewModel: LocationPromptViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    // Null while the stored choice is still being read; nothing below acts until it is known.
    val alwaysUseJerusalem by viewModel.alwaysUseJerusalem.collectAsStateWithLifecycle()
    var kind by remember { mutableStateOf(LocationPromptKind.None) }
    // What ON_START decided, parked until the stored choice above has loaded.
    var pending by remember { mutableStateOf<LocationPromptKind?>(null) }
    // Fire the system permission request at most once per foreground; after that (or a denial) use
    // our own dialog instead of re-spamming the OS prompt.
    var systemAsked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        kind = when {
            !granted -> {
                viewModel.useJerusalemOnce()
                LocationPromptKind.Permission
            }
            !context.isLocationServicesEnabled() -> {
                viewModel.useJerusalemOnce()
                LocationPromptKind.Services
            }
            else -> {
                viewModel.useCurrentLocation()
                LocationPromptKind.None
            }
        }
        // The location system dialog has now closed, so the notification prompt can safely open.
        onSettled()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    val hasPermission = context.hasLocationPermission()
                    val servicesEnabled = context.isLocationServicesEnabled()
                    pending = when {
                        hasPermission && servicesEnabled -> {
                            viewModel.useCurrentLocation()
                            LocationPromptKind.None
                        }
                        !hasPermission -> {
                            viewModel.useJerusalemOnce()
                            LocationPromptKind.Permission
                        }
                        // Permission granted, but the location switch is off.
                        else -> {
                            viewModel.useJerusalemOnce()
                            LocationPromptKind.Services
                        }
                    }
                }
                Lifecycle.Event.ON_STOP -> systemAsked = false // ask again on the next fresh open
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(pending, alwaysUseJerusalem) {
        val wanted = pending ?: return@LaunchedEffect
        val always = alwaysUseJerusalem ?: return@LaunchedEffect // still loading
        pending = null
        when {
            // Nothing to ask about, or the user settled it for good and must not be asked again.
            wanted == LocationPromptKind.None || always -> {
                kind = LocationPromptKind.None
                onSettled()
            }
            // First open of this foreground with no permission: let the OS ask directly. onSettled()
            // waits for the result callback so the notification dialog cannot overlap it.
            wanted == LocationPromptKind.Permission && !systemAsked -> {
                systemAsked = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
            else -> {
                kind = wanted
                onSettled()
            }
        }
    }

    if (kind == LocationPromptKind.None) return
    val isPermission = kind == LocationPromptKind.Permission
    LocationPromptDialog(
        isPermission = isPermission,
        onAllow = {
            kind = LocationPromptKind.None
            if (isPermission) context.openAppSettings() else context.openLocationSettings()
        },
        onUseJerusalemOnce = {
            kind = LocationPromptKind.None
            viewModel.useJerusalemOnce()
        },
        onUseJerusalemAlways = {
            kind = LocationPromptKind.None
            viewModel.useJerusalemAlways()
        },
        onDismiss = { kind = LocationPromptKind.None },
    )
}

/**
 * Three answers, ranked: the one that makes the app work best is the filled button, and the two
 * ways of declining sit below it as quieter text buttons — separated, because "not now" and "stop
 * asking" are genuinely different answers and collapsing them into one is what made the old dialog
 * nag. Stacked full width rather than crammed into a button row so the longer Hebrew labels fit.
 */
@Composable
private fun LocationPromptDialog(
    isPermission: Boolean,
    onAllow: () -> Unit,
    onUseJerusalemOnce: () -> Unit,
    onUseJerusalemAlways: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(ImageVector.vectorResource(R.drawable.ic_location_on), contentDescription = null) },
        title = {
            Text(
                text = if (isPermission) {
                    stringResource(R.string.location_prompt_permission_title)
                } else {
                    stringResource(R.string.location_prompt_services_title)
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
        },
        text = {
            Text(
                text = if (isPermission) {
                    stringResource(R.string.location_prompt_permission_body)
                } else {
                    stringResource(R.string.location_prompt_services_body)
                },
                textAlign = TextAlign.Center,
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Button(onClick = onAllow, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (isPermission) {
                            stringResource(R.string.location_prompt_grant)
                        } else {
                            stringResource(R.string.location_prompt_turn_on)
                        },
                    )
                }
                TextButton(onClick = onUseJerusalemOnce, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.location_prompt_jerusalem_once))
                }
                TextButton(onClick = onUseJerusalemAlways, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.location_prompt_jerusalem_always))
                }
            }
        },
    )
}
