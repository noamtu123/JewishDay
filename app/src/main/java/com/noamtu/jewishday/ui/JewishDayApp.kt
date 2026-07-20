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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.CenterAlignedTopAppBar
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.noamtu.jewishday.data.hasLocationPermission
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewishDayApp(useHebrewInterface: Boolean = false) {
    val layoutDirection = if (useHebrewInterface) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(
        LocalUseHebrewInterface provides useHebrewInterface,
        LocalLayoutDirection provides layoutDirection,
    ) {
        JewishDayNavHost(useHebrewInterface = useHebrewInterface)
        // The two flows share the system permission dialog, which can only show one at a time.
        // Ask for location first; only once that's settled do we ask for notifications.
        var locationSettled by remember { mutableStateOf(false) }
        LocationAvailabilityPrompt(onSettled = { locationSettled = true })
        NotificationPermissionSetup(canPrompt = locationSettled)
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
}

private enum class LocationPromptKind { None, Permission, Services }

/**
 * App-wide location handling, run on every foreground (cold start and each return from recents):
 *
 * - Permission + location on → pull a fresh fix immediately (zmanim compute right away).
 * - No permission → ask the OS directly the first time; if denied (or on later opens) show a dialog
 *   offering "Grant permission" or "Use Jerusalem".
 * - Location switch off → show a dialog offering "Turn on location" or "Use Jerusalem". (Android has
 *   no in-app system prompt to flip that switch without Google Play Services, so this deep-links to
 *   Settings rather than firing a one-tap request like the permission case.)
 *
 * The app never remembers a past location, so the fallback is always Jerusalem.
 */
@Composable
private fun LocationAvailabilityPrompt(
    onSettled: () -> Unit,
    viewModel: LocationPromptViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(LocationPromptKind.None) }
    // Fire the system permission request at most once per foreground; after that (or a denial) use
    // our own dialog instead of re-spamming the OS prompt.
    var systemAsked by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        kind = when {
            !granted -> { viewModel.useJerusalemFallback(); LocationPromptKind.Permission }
            !context.isLocationServicesEnabled() -> { viewModel.useJerusalemFallback(); LocationPromptKind.Services }
            else -> { viewModel.refreshCurrentLocation(); LocationPromptKind.None }
        }
        // The location system dialog has now closed, so the notification prompt can safely open.
        onSettled()
    }

    fun evaluate() {
        val hasPermission = context.hasLocationPermission()
        val servicesEnabled = context.isLocationServicesEnabled()
        when {
            hasPermission && servicesEnabled -> {
                viewModel.refreshCurrentLocation()
                kind = LocationPromptKind.None
                onSettled()
            }
            !hasPermission -> {
                viewModel.useJerusalemFallback()
                if (!systemAsked) {
                    // First open this foreground: ask the OS directly. onSettled() waits for the
                    // dialog's result callback so the notification dialog doesn't overlap it.
                    systemAsked = true
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                } else {
                    kind = LocationPromptKind.Permission
                    onSettled()
                }
            }
            else -> {
                // Permission granted, but the location switch is off.
                viewModel.useJerusalemFallback()
                kind = LocationPromptKind.Services
                onSettled()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> evaluate()
                Lifecycle.Event.ON_STOP -> systemAsked = false // ask again on the next fresh open
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (kind == LocationPromptKind.None) return
    val isPermission = kind == LocationPromptKind.Permission
    AlertDialog(
        onDismissRequest = { kind = LocationPromptKind.None },
        title = {
            Text(
                if (isPermission) {
                    localizedString(R.string.location_prompt_permission_title, R.string.location_prompt_permission_title_hebrew)
                } else {
                    localizedString(R.string.location_prompt_services_title, R.string.location_prompt_services_title_hebrew)
                },
            )
        },
        text = {
            Text(
                if (isPermission) {
                    localizedString(R.string.location_prompt_permission_body, R.string.location_prompt_permission_body_hebrew)
                } else {
                    localizedString(R.string.location_prompt_services_body, R.string.location_prompt_services_body_hebrew)
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    kind = LocationPromptKind.None
                    if (isPermission) context.openAppSettings() else context.openLocationSettings()
                },
            ) {
                Text(
                    if (isPermission) {
                        localizedString(R.string.location_prompt_grant, R.string.location_prompt_grant_hebrew)
                    } else {
                        localizedString(R.string.location_prompt_turn_on, R.string.location_prompt_turn_on_hebrew)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { kind = LocationPromptKind.None }) {
                Text(localizedString(R.string.location_prompt_use_jerusalem, R.string.location_prompt_use_jerusalem_hebrew))
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
private fun JewishDayNavHost(useHebrewInterface: Boolean) {
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
                NavHost(
                    navController = navController,
                    startDestination = AppDestination.Zmanim.route,
                    modifier = Modifier.padding(contentPadding),
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