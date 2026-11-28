// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.i18n

import androidx.annotation.StringRes
import com.noamtu.jewishday.R
import com.noamtu.jewishday.core.model.DailyLearningType
import com.noamtu.jewishday.core.model.Fast
import com.noamtu.jewishday.core.model.HolyDayTerm
import com.noamtu.jewishday.core.model.OfflineLearningTrack
import com.noamtu.jewishday.core.model.ZmanimCalculationSettings
import com.noamtu.jewishday.core.model.ZmanimTimeOption
import java.time.DayOfWeek

// What the core's enums are called. The core only knows what each one is; these exhaustive `when`s
// are where a new entry is forced to get a name.

/** What the row is called, on the zmanim screen and in the "Zmanim to show" list. */
@get:StringRes
val ZmanimTimeOption.label: Int get() = when (this) {
    ZmanimTimeOption.AlotHashachar -> R.string.zman_alot
    ZmanimTimeOption.TallitTefillin -> R.string.zman_tallit_tefillin
    ZmanimTimeOption.Sunrise -> R.string.zman_sunrise
    ZmanimTimeOption.SofZmanShemaMagenAvraham -> R.string.zman_shema_mga
    ZmanimTimeOption.SofZmanShemaGra -> R.string.zman_shema_gra
    ZmanimTimeOption.SofZmanTefillahMagenAvraham -> R.string.zman_tefillah_mga
    ZmanimTimeOption.SofZmanTefillahGra -> R.string.zman_tefillah_gra
    ZmanimTimeOption.ChatzotHaYom -> R.string.zman_chatzot_hayom
    ZmanimTimeOption.MinchaGedola -> R.string.zman_mincha_gedola
    ZmanimTimeOption.MinchaKetana -> R.string.zman_mincha_ketana
    ZmanimTimeOption.PlagHamincha -> R.string.zman_plag
    ZmanimTimeOption.Sunset -> R.string.zman_sunset
    ZmanimTimeOption.Tzeit -> R.string.zman_tzeit
    ZmanimTimeOption.ChatzotHaLaila -> R.string.zman_chatzot_halaila
    ZmanimTimeOption.ShabbatCandleLighting -> R.string.zman_option_shabbat_candles
    ZmanimTimeOption.ShabbatSunset -> R.string.zman_option_shabbat_sunset
    ZmanimTimeOption.MotzeiShabbat -> R.string.zman_motzei_shabbat
    ZmanimTimeOption.RabbeinuTam -> R.string.zman_rabbeinu_tam
}

/** The same, for the narrow rows of a home-screen widget: "ק״ש", not "קריאת שמע". */
@get:StringRes
val ZmanimTimeOption.shortLabel: Int get() = when (this) {
    ZmanimTimeOption.SofZmanShemaMagenAvraham -> R.string.zman_shema_mga_short
    ZmanimTimeOption.SofZmanShemaGra -> R.string.zman_shema_gra_short
    else -> label
}

/**
 * The method that gives this zman, as its row's caption shows it. The Shabbat rows wrap it in which
 * day it falls on, and candle lighting held at plag says so instead (`CandleLighting.caption`).
 */
fun ZmanimTimeOption.methodCaption(settings: ZmanimCalculationSettings): UiText = when (this) {
    ZmanimTimeOption.AlotHashachar -> settings.alotHashacharMethod.caption(settings)
    ZmanimTimeOption.TallitTefillin -> settings.misheyakirMethod.caption(settings)
    ZmanimTimeOption.Sunrise -> settings.sunriseMethod.label
    ZmanimTimeOption.SofZmanShemaMagenAvraham -> settings.sofZmanShemaMethod.caption(settings)
    ZmanimTimeOption.SofZmanShemaGra -> settings.sofZmanShemaGraMethod.caption(settings)
    ZmanimTimeOption.SofZmanTefillahMagenAvraham -> settings.sofZmanTefillahMethod.caption(settings)
    ZmanimTimeOption.SofZmanTefillahGra -> settings.sofZmanTefillahGraMethod.caption(settings)
    ZmanimTimeOption.ChatzotHaYom -> settings.chatzotMethod.label
    ZmanimTimeOption.MinchaGedola -> settings.minchaGedolaMethod.caption(settings)
    ZmanimTimeOption.MinchaKetana -> settings.minchaKetanaMethod.caption(settings)
    ZmanimTimeOption.PlagHamincha -> settings.plagHaminchaMethod.caption(settings)
    ZmanimTimeOption.Sunset, ZmanimTimeOption.ShabbatSunset -> settings.sunsetMethod.label
    ZmanimTimeOption.Tzeit -> settings.tzeitHakochavimMethod.caption(settings)
    ZmanimTimeOption.ChatzotHaLaila -> settings.chatzotHaLailaMethod.label
    ZmanimTimeOption.ShabbatCandleLighting -> settings.candleLightingMethod.caption(settings)
    ZmanimTimeOption.MotzeiShabbat -> settings.motzeiShabbatMethod.caption(settings)
    ZmanimTimeOption.RabbeinuTam -> settings.rabbeinuTamMethod.caption(settings)
}

@get:StringRes
val DailyLearningType.label: Int get() = when (this) {
    DailyLearningType.DafYomiBavli -> R.string.learning_daf_yomi_bavli
    DailyLearningType.DafYomiYerushalmi -> R.string.learning_daf_yomi_yerushalmi
    DailyLearningType.MishnahYomi -> R.string.learning_mishnah_yomi
    DailyLearningType.RambamYomi -> R.string.learning_rambam_yomi
    DailyLearningType.RambamYomiThreeChapters -> R.string.learning_rambam_yomi_three
    DailyLearningType.TehillimYomi -> R.string.learning_tehillim_yomi
    DailyLearningType.TanakhYomi -> R.string.learning_tanakh_yomi
    DailyLearningType.ShemiratHaLashon -> R.string.learning_shemirat_halashon
    DailyLearningType.KitzurShulchanAruch -> R.string.learning_kitzur
}

/**
 * A daily-learning row's name: the track's own, except that both Rambam tracks are "Rambam Yomi" and
 * say which they are underneath, so shown together they read as one section.
 */
@get:StringRes
val OfflineLearningTrack.title: Int get() = when (this) {
    OfflineLearningTrack.RambamYomi, OfflineLearningTrack.RambamYomiThreeChapters -> R.string.learning_rambam_yomi
    else -> type.label
}

/** What a daily-learning row says under its name. */
val OfflineLearningTrack.description: UiText get() = when (this) {
    OfflineLearningTrack.DafYomiBavli -> uiText(R.string.learning_daf_yomi_kosherjava_description)
    OfflineLearningTrack.DafYomiYerushalmi -> uiText(R.string.learning_yerushalmi_kosherjava_description)
    OfflineLearningTrack.MishnahYomi -> uiText(R.string.learning_mishnah_yomi_description)
    OfflineLearningTrack.RambamYomi -> pluralText(R.plurals.learning_rambam_chapters, 1)
    OfflineLearningTrack.RambamYomiThreeChapters -> pluralText(R.plurals.learning_rambam_chapters, 3)
    OfflineLearningTrack.TehillimYomi -> uiText(R.string.learning_tehillim_kosherjava_description)
    OfflineLearningTrack.TanakhYomi -> uiText(R.string.learning_tanakh_yomi_description)
    OfflineLearningTrack.ShemiratHaLashon -> uiText(R.string.learning_shemirat_halashon_description)
    OfflineLearningTrack.KitzurShulchanAruch -> uiText(R.string.learning_kitzur_description)
}

@get:StringRes
val Fast.label: Int get() = when (this) {
    Fast.Gedalyah -> R.string.fast_gedalyah
    Fast.TishaBav -> R.string.fast_tisha_bav
    Fast.SeventeenthOfTammuz -> R.string.fast_17_tammuz
    Fast.TenthOfTevet -> R.string.fast_10_tevet
    Fast.Esther -> R.string.fast_esther
}

/** "כניסת חג ראשון": when the span this end belongs to comes in. */
@get:StringRes
val HolyDayTerm.starts: Int get() = when (this) {
    HolyDayTerm.Shabbat -> R.string.holy_day_starts_shabbat
    HolyDayTerm.YomTov -> R.string.holy_day_starts_yom_tov
    HolyDayTerm.FirstDay -> R.string.holy_day_starts_first_day
    HolyDayTerm.SecondDay -> R.string.holy_day_starts_second_day
    HolyDayTerm.ThirdDay -> R.string.holy_day_starts_third_day
}

/** "צאת חג שני": when the span this end belongs to goes out. */
@get:StringRes
val HolyDayTerm.ends: Int get() = when (this) {
    HolyDayTerm.Shabbat -> R.string.holy_day_ends_shabbat
    HolyDayTerm.YomTov -> R.string.holy_day_ends_yom_tov
    HolyDayTerm.FirstDay -> R.string.holy_day_ends_first_day
    HolyDayTerm.SecondDay -> R.string.holy_day_ends_second_day
    HolyDayTerm.ThirdDay -> R.string.holy_day_ends_third_day
}

/** The icon's own weekday names: Hebrew's Saturday is plain "שבת" there, not java.time's "יום שבת". */
@get:StringRes
val DayOfWeek.iconName: Int get() = when (this) {
    DayOfWeek.SUNDAY -> R.string.weekday_sunday
    DayOfWeek.MONDAY -> R.string.weekday_monday
    DayOfWeek.TUESDAY -> R.string.weekday_tuesday
    DayOfWeek.WEDNESDAY -> R.string.weekday_wednesday
    DayOfWeek.THURSDAY -> R.string.weekday_thursday
    DayOfWeek.FRIDAY -> R.string.weekday_friday
    DayOfWeek.SATURDAY -> R.string.weekday_saturday
}
