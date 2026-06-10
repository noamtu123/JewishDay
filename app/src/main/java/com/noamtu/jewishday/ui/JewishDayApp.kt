package com.noamtu.jewishday.ui

import android.content.Intent
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.noamtu.jewishday.feature.mizrach.MizrachScreen
import com.noamtu.jewishday.feature.settings.SettingsScreen
import com.noamtu.jewishday.feature.today.TodayScreen
import com.noamtu.jewishday.feature.zmanim.ZmanimScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JewishDayApp(useHebrewInterface: Boolean = false) {
    CompositionLocalProvider(LocalUseHebrewInterface provides useHebrewInterface) {
        JewishDayNavHost(useHebrewInterface = useHebrewInterface)
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
        } ?: AppDestination.Today
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
                    startDestination = AppDestination.Today.route,
                    modifier = Modifier.padding(contentPadding),
                ) {
                    composable(AppDestination.Today.route) { TodayScreen() }
                    composable(AppDestination.Zmanim.route) { ZmanimScreen() }
                    composable(AppDestination.Mizrach.route) { MizrachScreen() }
                    composable(AppDestination.Settings.route) { SettingsScreen() }
                    composable(AppDestination.About.route) { AboutScreen() }
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
