package com.noamtu.jewishday.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.ui.graphics.vector.ImageVector
import com.noamtu.jewishday.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val hebrewLabelRes: Int,
    val icon: ImageVector,
) {
    Zmanim("zmanim", R.string.nav_zmanim, R.string.nav_zmanim_hebrew, Icons.Outlined.CalendarMonth),
    Mizrach("mizrach", R.string.nav_mizrach, R.string.nav_mizrach_hebrew, Icons.Outlined.Explore),
    Settings("settings", R.string.nav_settings, R.string.nav_settings_hebrew, Icons.Outlined.Settings),
    About("about", R.string.nav_about, R.string.nav_about_hebrew, Icons.Outlined.Info),
    Developer("developer", R.string.nav_developer, R.string.nav_developer_hebrew, Icons.Outlined.BugReport),

    ;

    companion object {
        val bottomBarDestinations = listOf(Zmanim, Mizrach)
    }
}
