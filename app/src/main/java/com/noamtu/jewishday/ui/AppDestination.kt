// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.noamtu.jewishday.R

enum class AppDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @StringRes val hebrewLabelRes: Int,
    @DrawableRes val icon: Int,
) {
    Zmanim("zmanim", R.string.nav_zmanim, R.string.nav_zmanim_hebrew, R.drawable.ic_calendar_month),
    Mizrach("mizrach", R.string.nav_mizrach, R.string.nav_mizrach_hebrew, R.drawable.ic_explore),
    Settings("settings", R.string.nav_settings, R.string.nav_settings_hebrew, R.drawable.ic_settings),
    About("about", R.string.nav_about, R.string.nav_about_hebrew, R.drawable.ic_info),
    Developer("developer", R.string.nav_developer, R.string.nav_developer_hebrew, R.drawable.ic_bug_report),

    ;

    companion object {
        val bottomBarDestinations = listOf(Zmanim, Mizrach)
    }
}