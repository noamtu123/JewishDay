// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.noamtu.jewishday.R
import com.noamtu.jewishday.model.Festival

/** Each festival's picture: what the day is known by at a glance, the same in the app and the widget. */
@get:DrawableRes
val Festival.iconRes: Int
    get() = when (this) {
        Festival.RoshHashana -> R.drawable.ic_festival_rosh_hashana
        Festival.YomKippur -> R.drawable.ic_festival_yom_kippur
        Festival.Sukkot -> R.drawable.ic_festival_sukkot
        Festival.SimchatTorah -> R.drawable.ic_festival_simchat_torah
        Festival.Chanukah -> R.drawable.ic_festival_chanukah
        Festival.TuBishvat -> R.drawable.ic_festival_tu_bishvat
        Festival.Purim -> R.drawable.ic_festival_purim
        Festival.Pesach -> R.drawable.ic_festival_pesach
        Festival.YomHaatzmaut -> R.drawable.ic_festival_yom_haatzmaut
        Festival.LagBaomer -> R.drawable.ic_festival_lag_baomer
        Festival.Shavuot -> R.drawable.ic_festival_shavuot
    }

/** The festival's picture, [size] square. The day's name is said beside it, so it is left unread. */
@Composable
fun FestivalPicture(festival: Festival, size: Dp, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(festival.iconRes),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}
