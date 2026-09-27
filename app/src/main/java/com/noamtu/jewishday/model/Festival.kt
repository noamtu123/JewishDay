// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar

/**
 * The festivals a day can belong to, each with a picture of its own: the Yamim Noraim, the three
 * pilgrim festivals with their intermediate days, and the rabbinic and modern holidays. A fast, Rosh
 * Chodesh or Isru Chag is not one of them.
 */
enum class Festival {
    RoshHashana,
    YomKippur,
    Sukkot,
    SimchatTorah,
    Chanukah,
    TuBishvat,
    Purim,
    Pesach,
    YomHaatzmaut,
    LagBaomer,
    Shavuot,
}

/**
 * The festival [calendar]'s day belongs to, or null on any other day. An erev counts as its festival,
 * since from its morning the day is already announced by name and spent getting ready for it.
 */
internal fun festivalOf(calendar: JewishCalendar): Festival? = when (calendar.yomTovIndex) {
    JewishCalendar.EREV_ROSH_HASHANA, JewishCalendar.ROSH_HASHANA -> Festival.RoshHashana
    JewishCalendar.EREV_YOM_KIPPUR, JewishCalendar.YOM_KIPPUR -> Festival.YomKippur
    JewishCalendar.EREV_SUCCOS, JewishCalendar.SUCCOS, JewishCalendar.CHOL_HAMOED_SUCCOS,
    JewishCalendar.HOSHANA_RABBA -> Festival.Sukkot
    JewishCalendar.SHEMINI_ATZERES, JewishCalendar.SIMCHAS_TORAH -> Festival.SimchatTorah
    JewishCalendar.CHANUKAH -> Festival.Chanukah
    JewishCalendar.TU_BESHVAT -> Festival.TuBishvat
    JewishCalendar.PURIM, JewishCalendar.SHUSHAN_PURIM -> Festival.Purim
    JewishCalendar.EREV_PESACH, JewishCalendar.PESACH, JewishCalendar.CHOL_HAMOED_PESACH -> Festival.Pesach
    JewishCalendar.YOM_HAATZMAUT -> Festival.YomHaatzmaut
    JewishCalendar.LAG_BAOMER -> Festival.LagBaomer
    JewishCalendar.EREV_SHAVUOS, JewishCalendar.SHAVUOS -> Festival.Shavuot
    else -> null
}
