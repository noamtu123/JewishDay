// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import androidx.annotation.StringRes
import com.noamtu.jewishday.R

/**
 * The rows of the main "Zmanim" group that the user can individually show or hide on the
 * Zmanim tab. [storageValue] is the stable id persisted in settings and stamped onto the
 * matching [ZmanItem.id] so the row can be filtered without matching on display text.
 */
enum class ZmanimTimeOption(
    val storageValue: String,
    // What the row is called, on the zmanim screen and in the "Zmanim to show" list.
    @StringRes val label: Int,
    // The same, for the narrow rows of a home-screen widget.
    @StringRes val shortLabel: Int = label,
) {
    AlotHashachar("alot", R.string.zman_alot),
    TallitTefillin("tallit_tefillin", R.string.zman_tallit_tefillin),
    Sunrise("sunrise", R.string.zman_sunrise),
    SofZmanShemaMagenAvraham("shema_mga", R.string.zman_shema_mga, R.string.zman_shema_mga_short),
    SofZmanShemaGra("shema_gra", R.string.zman_shema_gra, R.string.zman_shema_gra_short),
    SofZmanTefillahMagenAvraham("tefillah_mga", R.string.zman_tefillah_mga),
    SofZmanTefillahGra("tefillah_gra", R.string.zman_tefillah_gra),
    ChatzotHaYom("chatzot_hayom", R.string.zman_chatzot_hayom),
    MinchaGedola("mincha_gedola", R.string.zman_mincha_gedola),
    MinchaKetana("mincha_ketana", R.string.zman_mincha_ketana),
    PlagHamincha("plag_hamincha", R.string.zman_plag),
    Sunset("sunset", R.string.zman_sunset),
    Tzeit("tzeit", R.string.zman_tzeit),
    ChatzotHaLaila("chatzot_halaila", R.string.zman_chatzot_halaila),
    // Shabbat-section rows, shown/hidden by the same "Zmanim to show" list.
    ShabbatCandleLighting("shabbat_candles", R.string.zman_option_shabbat_candles),
    ShabbatSunset("shabbat_sunset", R.string.zman_option_shabbat_sunset),
    MotzeiShabbat("motzei_shabbat", R.string.zman_motzei_shabbat),
    RabbeinuTam("rabbeinu_tam", R.string.zman_rabbeinu_tam),
    ;

    companion object {
        val Default: Set<ZmanimTimeOption> = entries.toSet()

        fun fromStorageValue(value: String?): ZmanimTimeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * The daily-learning tracks the user can individually show or hide. [storageValue] matches
 * both the Hebcal API category and the offline KosherJava row id, so a single set filters
 * both sources. Default: Daf Yomi Bavli, Daf Yomi Yerushalmi, and Rambam Yomi (1 chapter).
 */
enum class DailyLearningType(
    val storageValue: String,
    @StringRes val label: Int,
) {
    DafYomiBavli("dafyomi", R.string.learning_daf_yomi_bavli),
    DafYomiYerushalmi("yerushalmi", R.string.learning_daf_yomi_yerushalmi),
    MishnahYomi("mishnayomi", R.string.learning_mishnah_yomi),
    RambamYomi("dailyRambam1", R.string.learning_rambam_yomi),
    RambamYomiThreeChapters("dailyRambam3", R.string.learning_rambam_yomi_three),
    TehillimYomi("dailyPsalms", R.string.learning_tehillim_yomi),
    TanakhYomi("tanakhYomi", R.string.learning_tanakh_yomi),
    ShemiratHaLashon("shemiratHaLashon", R.string.learning_shemirat_halashon),
    KitzurShulchanAruch("kitzurShulchanAruch", R.string.learning_kitzur),
    ;

    companion object {
        val Default: Set<DailyLearningType> = setOf(DafYomiBavli, DafYomiYerushalmi, RambamYomi)

        fun fromStorageValue(value: String?): DailyLearningType? =
            entries.firstOrNull { it.storageValue == value }
    }
}