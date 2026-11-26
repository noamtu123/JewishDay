// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
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
import com.noamtu.jewishday.i18n.ProvideTextResolver
import com.noamtu.jewishday.ui.theme.GlassBackdrop
import com.noamtu.jewishday.ui.theme.LocalGlassTheme
import com.noamtu.jewishday.update.AppUpdateViewModel
import com.noamtu.jewishday.update.UpdateBanner
import com.noamtu.jewishday.update.UpdateState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewishDayApp(
    showDayRequest: ShowDayRequest? = null,
) {
    // The activity is already in the app's language — strings, layout direction and all — so all
    // that is left is a resolver for the text the view models describe.
    ProvideTextResolver {
        // Shared deliberately: the banner in the scaffold and the dialog below are two views of
        // one update, so they must be looking at the same state.
        val updateViewModel: AppUpdateViewModel = hiltViewModel()
        JewishDayNavHost(
            updateViewModel = updateViewModel,
            showDayRequest = showDayRequest,
        )
        // The two flows share the system permission dialog, which can only show one at a time.
        // Ask for location first; only once that's settled do we ask for notifications.
        var locationSettled by remember { mutableStateOf(false) }
        LocationAvailabilityPrompt(onSettled = { locationSettled = true })
        NotificationPermissionSetup(canPrompt = locationSettled)
        GlassThemeNotice(canShow = locationSettled)
        // Last in the queue, so a first launch answers the system permission dialogs before this
        // one appears.
        AppUpdatePrompt(updateViewModel)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JewishDayNavHost(
    updateViewModel: AppUpdateViewModel,
    showDayRequest: ShowDayRequest?,
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentAppDestination = remember(currentDestination) {
        AppDestination.entries.firstOrNull { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        } ?: AppDestination.Zmanim
    }

    // A widget tap shows its day on the zmanim screen, from whichever screen the app was left on.
    LaunchedEffect(showDayRequest) {
        if (showDayRequest != null) navController.navigateTopLevelTo(AppDestination.Zmanim.route)
    }

    // Under the Glass theme the app's own chrome goes see-through so the sky shows behind it.
    val glass = LocalGlassTheme.current
    val chromeColor = if (glass) Color.Transparent else MaterialTheme.colorScheme.background

    GlassBackdrop(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavigationRail) {
                AppNavigationRail(
                    currentDestination = currentDestination,
                    onDestinationClick = navController::navigateTopLevelTo,
                )
            }

            Scaffold(
                containerColor = chromeColor,
                topBar = {
                    CenterAlignedTopAppBar(
                        // The screen's name: a heading, so TalkBack's heading navigation starts here.
                        title = {
                            Text(
                                text = stringResource(currentAppDestination.labelRes),
                                modifier = Modifier.semantics { heading() },
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = chromeColor,
                            scrolledContainerColor = chromeColor,
                        ),
                        actions = {
                            val reportLabel = stringResource(R.string.report_issue)
                            IconButton(onClick = { context.openFeedbackEmail() }) {
                                Icon(ImageVector.vectorResource(R.drawable.ic_bug_report), contentDescription = reportLabel)
                            }
                            if (currentAppDestination != AppDestination.Settings) {
                                val settingsLabel = stringResource(AppDestination.Settings.labelRes)
                                IconButton(onClick = { navController.navigateSecondaryTo(AppDestination.Settings.route) }) {
                                    Icon(ImageVector.vectorResource(AppDestination.Settings.icon), contentDescription = settingsLabel)
                                }
                            }
                            if (currentAppDestination != AppDestination.About) {
                                val aboutLabel = stringResource(AppDestination.About.labelRes)
                                IconButton(onClick = { navController.navigateSecondaryTo(AppDestination.About.route) }) {
                                    Icon(ImageVector.vectorResource(AppDestination.About.icon), contentDescription = aboutLabel)
                                }
                            }
                        },
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        AppNavigationBar(
                            currentDestination = currentDestination,
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
                        composable(AppDestination.Zmanim.route) {
                            ZmanimScreen(showDayRequest = showDayRequest)
                        }
                        composable(AppDestination.Mizrach.route) { MizrachScreen() }
                        composable(AppDestination.Settings.route) { SettingsScreen() }
                        composable(AppDestination.About.route) {
                            AboutScreen(
                                onOpenDeveloperTools = { navController.navigateSecondaryTo(AppDestination.Developer.route) },
                            )
                        }
                        composable(AppDestination.Developer.route) {
                            DeveloperScreen(onExit = { navController.popBackStack() })
                        }
                    }
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
