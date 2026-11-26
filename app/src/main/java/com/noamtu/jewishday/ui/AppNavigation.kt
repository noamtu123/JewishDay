// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.noamtu.jewishday.ui.theme.LocalGlassStyle
import com.noamtu.jewishday.ui.theme.LocalGlassTheme

@Composable
internal fun AppNavigationBar(
    currentDestination: NavDestination?,
    onDestinationClick: (String) -> Unit,
) {
    if (LocalGlassTheme.current) {
        GlassNavigationPill(currentDestination, onDestinationClick)
        return
    }
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        AppDestination.bottomBarDestinations.forEach { destination ->
            val selected = currentDestination.isSelected(destination)
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                // The label beside it names the tab; labelling the icon too would read it twice.
                icon = { Icon(ImageVector.vectorResource(destination.icon), contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}

@Composable
internal fun AppNavigationRail(
    currentDestination: NavDestination?,
    onDestinationClick: (String) -> Unit,
) {
    NavigationRail(containerColor = if (LocalGlassTheme.current) LocalGlassStyle.current.bar else MaterialTheme.colorScheme.surface) {
        AppDestination.bottomBarDestinations.forEach { destination ->
            val selected = currentDestination.isSelected(destination)
            val label = stringResource(destination.labelRes)
            NavigationRailItem(
                selected = selected,
                onClick = { onDestinationClick(destination.route) },
                // The label beside it names the tab; labelling the icon too would read it twice.
                icon = { Icon(ImageVector.vectorResource(destination.icon), contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}

private fun NavDestination?.isSelected(destination: AppDestination): Boolean =
    this?.hierarchy?.any { it.route == destination.route } == true

internal fun NavHostController.navigateTopLevelTo(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
    }
}

internal fun NavHostController.navigateSecondaryTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

/**
 * The Glass theme's navigation: a pill floating above the bottom edge instead of a bar across it,
 * with the current tab picked out inside it.
 */
@Composable
private fun GlassNavigationPill(
    currentDestination: NavDestination?,
    onDestinationClick: (String) -> Unit,
) {
    val glass = LocalGlassStyle.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(glass.bar)
                .border(1.dp, glass.edge, CircleShape)
                // Only a large font size makes the tabs wider than the screen; then the pill scrolls
                // rather than cutting the last tab off. At the default size they fit and it is still.
                .horizontalScroll(rememberScrollState())
                .padding(6.dp)
                // A tab row to TalkBack, like the plain navigation bar: "Tab 1 of 2".
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            AppDestination.bottomBarDestinations.forEach { destination ->
                val selected = currentDestination.isSelected(destination)
                val label = stringResource(destination.labelRes)
                val contentColor = if (selected) glass.onSelected else MaterialTheme.colorScheme.onSurface
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (selected) glass.selected else Color.Transparent)
                        // A tab, and whether it is the current one, rather than a bare clickable.
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onDestinationClick(destination.route) },
                        )
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(ImageVector.vectorResource(destination.icon), contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                    Text(label, color = contentColor, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
