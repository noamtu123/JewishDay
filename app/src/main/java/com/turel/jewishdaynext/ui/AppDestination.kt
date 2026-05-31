package com.turel.jewishdaynext.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.graphics.vector.ImageVector
import com.turel.jewishdaynext.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val hebrewLabelRes: Int,
    val icon: ImageVector,
) {
    Today("today", R.string.nav_today, R.string.nav_today_hebrew, Icons.Outlined.Today),
    Zmanim("zmanim", R.string.nav_zmanim, R.string.nav_zmanim_hebrew, Icons.Outlined.CalendarMonth),
    Mizrach("mizrach", R.string.nav_mizrach, R.string.nav_mizrach_hebrew, Icons.Outlined.Explore),
    Locations("locations", R.string.nav_locations, R.string.nav_locations_hebrew, Icons.Outlined.LocationOn),
    Settings("settings", R.string.nav_settings, R.string.nav_settings_hebrew, Icons.Outlined.Settings),
    About("about", R.string.nav_about, R.string.nav_about_hebrew, Icons.Outlined.Info),

    ;

    companion object {
        val bottomBarDestinations = listOf(Today, Zmanim, Mizrach, Locations)
    }
}
