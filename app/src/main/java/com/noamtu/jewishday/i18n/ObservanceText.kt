// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.i18n.CalendarText
import com.noamtu.jewishday.core.model.FastDayInfo
import com.noamtu.jewishday.core.model.HolyDayInfo
import com.noamtu.jewishday.core.model.HolyDayName
import com.noamtu.jewishday.core.model.JewishDayInfo
import com.noamtu.jewishday.core.model.SequelPart

// The date header's observances and the status-bar icon's date, worded.

/** "צום גדליה". */
val FastDayInfo.nameText: UiText get() = uiText(fast.label)

/** "ראש השנה", "יום כיפור ושבת", "שבת", "שבת חול המועד פסח". */
val HolyDayInfo.nameText: UiText get() = when (val name = day) {
    is HolyDayName.YomTov ->
        if (name.onShabbat) uiText(R.string.holy_day_with_shabbat, name.name.asUiText()) else name.name.asUiText()
    is HolyDayName.Shabbat ->
        name.dayName?.let { uiText(R.string.holy_day_shabbat_named, it.asUiText()) } ?: uiText(R.string.holy_day_shabbat)
}

/** "פרשת נצבים", while the coming Shabbat is only announced. */
val HolyDayInfo.parshaText: UiText? get() = parsha?.let { uiText(R.string.parsha_named, it.asUiText()) }

/** What is still ahead, joined with "+": "חג כפול + שבת + צום". */
val HolyDayInfo.sequelText: UiText? get() = sequel?.map { it.text }?.reduce { ahead, next ->
    uiText(R.string.sequel_join, ahead, next)
}

private val SequelPart.text: UiText get() = when (this) {
    is SequelPart.YomTov -> {
        val yomTov = uiText(
            when {
                days >= 3 -> R.string.sequel_three_day_yom_tov
                days == 2 -> R.string.sequel_two_day_yom_tov
                else -> R.string.sequel_yom_tov
            },
        )
        if (includesShabbat) uiText(R.string.sequel_with_shabbat, yomTov) else yomTov
    }
    SequelPart.Shabbat -> uiText(R.string.sequel_shabbat)
    SequelPart.Fast -> uiText(R.string.sequel_fast)
}

/** The icon's weekday: the civil day's, so it rolls at midnight (see [JewishDayInfo]). */
val JewishDayInfo.weekday: UiText get() = uiText(gregorianDate.dayOfWeek.iconName)

val JewishDayInfo.hebrewDate: UiText get() = CalendarText.HebrewDate(jewishDate).asUiText()
