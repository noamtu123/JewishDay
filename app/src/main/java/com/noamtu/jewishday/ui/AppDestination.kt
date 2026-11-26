// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.noamtu.jewishday.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val icon: Int,
) {
    Zmanim("zmanim", R.string.nav_zmanim, R.drawable.ic_calendar_month),
    Mizrach("mizrach", R.string.nav_mizrach, R.drawable.ic_explore),
    Settings("settings", R.string.nav_settings, R.drawable.ic_settings),
    About("about", R.string.nav_about, R.drawable.ic_info),
    Developer("developer", R.string.nav_developer, R.drawable.ic_bug_report),

    ;

    companion object {
        val bottomBarDestinations = listOf(Zmanim, Mizrach)
    }
}