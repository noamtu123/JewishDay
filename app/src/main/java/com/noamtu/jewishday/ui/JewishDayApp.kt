// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noamtu.jewishday.data.hasLocationPermission
import com.noamtu.jewishday.data.hasNotificationPermission
import com.noamtu.jewishday.data.isLocationServicesEnabled
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.noamtu.jewishday.R
import com.noamtu.jewishday.feature.about.AboutScreen
import com.noamtu.jewishday.feature.developer.DeveloperScreen
import com.noamtu.jewishday.feature.mizrach.MizrachScreen
import com.noamtu.jewishday.feature.settings.SettingsScreen
import com.noamtu.jewishday.feature.zmanim.ZmanimScreen
import com.noamtu.jewishday.update.AppUpdateDialog
import com.noamtu.jewishday.update.UpdateBanner
import com.noamtu.jewishday.update.UpdateState
import com.noamtu.jewishday.update.AppUpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewishDayApp(useHebrewInterface: Boolean = false) {
    val layoutDirection = if (useHebrewInterface) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalUseHebrewInterface provides useHebrewInterface,
        LocalLayoutDirection provides layoutDirection,
    ) {
        // Shared deliberately: the banner in the scaffold and the dialog below are two views of
        // one update, so they must be looking at the same state.
        val updateViewModel: AppUpdateViewModel = hiltViewModel()
        JewishDayNavHost(useHebrewInterface = useHebrewInterface, updateViewModel = updateViewModel)
        // The two flows share the system permission dialog, which can only show one at a time.
        // Ask for location first; only once that's settled do we ask for notifications.
        var locationSettled by remember { mutableStateOf(false) }
        LocationAvailabilityPrompt(onSettled = { locationSettled = true })
        NotificationPermissionSetup(canPrompt = locationSettled)
        // Last in the queue, so a first launch answers the system permission dialogs before this
        // one appears.
        AppUpdatePrompt(updateViewModel)
    }
}

/**
 * First-launch only: request the POST_NOTIFICATIONS permission (Android 13+) so the Hebrew date
 * icon — on by default — can actually appear. Runs only after the location prompt has settled, so
 * the two system dialogs don't collide. On grant the setting is turned on and its service starts
 * immediately; on rejection the setting is turned off. On older versions the permission is implicit,
 * so we just start it. Shown once, then never again.
 */
@Composable
private fun NotificationPermissionSetup(
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
 * Looks for a new release once per launch and, only if there is one, opens the update dialog. A
 * check that finds nothing — or fails outright — says nothing at all, so an ordinary launch is
 * untouched by it.
 */
@Composable
private fun AppUpdatePrompt(viewModel: AppUpdateViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val notesInEnglish by viewModel.notesInEnglish.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.checkOnLaunch() }

    // Granting the install permission happens on a system screen, so the only place to notice it
    // was granted is on the way back in.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.onResumed()
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
private fun LocationAvailabilityPrompt(
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
        icon = { Icon(Icons.Outlined.LocationOn, contentDescription = null) },
        title = {
            Text(
                text = if (isPermission) {
                    localizedString(R.string.location_prompt_permission_title, R.string.location_prompt_permission_title_hebrew)
                } else {
                    localizedString(R.string.location_prompt_services_title, R.string.location_prompt_services_title_hebrew)
                },
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Text(
                text = if (isPermission) {
                    localizedString(R.string.location_prompt_permission_body, R.string.location_prompt_permission_body_hebrew)
                } else {
                    localizedString(R.string.location_prompt_services_body, R.string.location_prompt_services_body_hebrew)
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
                            localizedString(R.string.location_prompt_grant, R.string.location_prompt_grant_hebrew)
                        } else {
                            localizedString(R.string.location_prompt_turn_on, R.string.location_prompt_turn_on_hebrew)
                        },
                    )
                }
                TextButton(onClick = onUseJerusalemOnce, modifier = Modifier.fillMaxWidth()) {
                    Text(localizedString(R.string.location_prompt_jerusalem_once, R.string.location_prompt_jerusalem_once_hebrew))
                }
                TextButton(onClick = onUseJerusalemAlways, modifier = Modifier.fillMaxWidth()) {
                    Text(localizedString(R.string.location_prompt_jerusalem_always, R.string.location_prompt_jerusalem_always_hebrew))
                }
            }
        },
    )
}

private fun android.content.Context.openLocationSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun android.content.Context.openAppSettings() {
    try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JewishDayNavHost(useHebrewInterface: Boolean, updateViewModel: AppUpdateViewModel) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentAppDestination = remember(currentDestination) {
        AppDestination.entries.firstOrNull { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        } ?: AppDestination.Zmanim
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                AppNavigationRail(
                    currentDestination = currentDestination,
                    useHebrewInterface = useHebrewInterface,
                    onDestinationClick = navController::navigateTopLevelTo,
                )
            }

            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(currentAppDestination.labelRes(useHebrewInterface))) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                        ),
                        actions = {
                            val reportLabel = localizedString(R.string.report_issue, R.string.report_issue_hebrew)
                            IconButton(onClick = { context.openFeedbackEmail() }) {
                                Icon(Icons.Outlined.BugReport, contentDescription = reportLabel)
                            }
                            if (currentAppDestination != AppDestination.Settings) {
                                val settingsLabel = stringResource(AppDestination.Settings.labelRes(useHebrewInterface))
                                IconButton(onClick = { navController.navigateSecondaryTo(AppDestination.Settings.route) }) {
                                    Icon(AppDestination.Settings.icon, contentDescription = settingsLabel)
                                }
                            }
                            if (currentAppDestination != AppDestination.About) {
                                val aboutLabel = stringResource(AppDestination.About.labelRes(useHebrewInterface))
                                IconButton(onClick = { navController.navigateSecondaryTo(AppDestination.About.route) }) {
                                    Icon(AppDestination.About.icon, contentDescription = aboutLabel)
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        AppNavigationBar(
                            currentDestination = currentDestination,
                            useHebrewInterface = useHebrewInterface,
                            onDestinationClick = navController::navigateTopLevelTo,
                        )
                    }
                },
            ) { contentPadding ->
                val pendingUpdate by updateViewModel.pendingRelease.collectAsStateWithLifecycle()
                val updateState by updateViewModel.state.collectAsStateWithLifecycle()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    // Only while the dialog is closed — otherwise it is saying the same thing twice.
                    UpdateBanner(
                        visible = pendingUpdate != null && updateState is UpdateState.Idle,
                        onClick = updateViewModel::showPendingRelease,
                    )
                    NavHost(
                        navController = navController,
                        startDestination = AppDestination.Zmanim.route,
                        modifier = Modifier.weight(1f),
                    ) {
                        composable(AppDestination.Zmanim.route) { ZmanimScreen() }
                        composable(AppDestination.Mizrach.route) { MizrachScreen() }
                        composable(AppDestination.Settings.route) { SettingsScreen() }
                        composable(AppDestination.About.route) {
                            AboutScreen(
                                onOpenDeveloperTools = { navController.navigateSecondaryTo(AppDestination.Developer.route) },
                            )
                        }
                        composable(AppDestination.Developer.route) { DeveloperScreen() }
                    }
                }
            }
        }
    }
}

private fun android.content.Context.openFeedbackEmail() {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:jewishdayapp@gmail.com".toUri()
        putExtra(Intent.EXTRA_SUBJECT, "JewishDay bug report / feature request")
    }
    runCatching { startActivity(intent) }
}

@Composable
private fun AppNavigationBar(
    currentDestination: NavDestination?,
    useHebrewInterface: Boolean,
    onDestinationClick: (String) -> Unit,
) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppDestination.bottomBarDestinations.forEach { destination ->
            val selected = currentDestination.isSelected(destination)
            val label = stringResource(destination.labelRes(useHebrewInterface))
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                icon = { Icon(destination.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentDestination: NavDestination?,
    useHebrewInterface: Boolean,
    onDestinationClick: (String) -> Unit,
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        AppDestination.bottomBarDestinations.forEach { destination ->
            val selected = currentDestination.isSelected(destination)
            val label = stringResource(destination.labelRes(useHebrewInterface))
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                icon = { Icon(destination.icon, contentDescription = label) },
                label = { Text(label) },
            )
        }
    }
}

private fun NavDestination?.isSelected(destination: AppDestination): Boolean =
    this?.hierarchy?.any { it.route == destination.route } == true

private fun AppDestination.labelRes(useHebrewInterface: Boolean): Int =
    if (useHebrewInterface) hebrewLabelRes else labelRes

private fun NavHostController.navigateTopLevelTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

private fun NavHostController.navigateSecondaryTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}