// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

/**
 * The rows of the main "Zmanim" group that the user can individually show or hide on the
 * Zmanim tab. [storageValue] is the stable id persisted in settings and stamped onto the
 * matching row id (`ZmanItem.id` in the app) so the row can be filtered without matching on display text.
 */
public enum class ZmanimTimeOption(public val storageValue: String) {
    AlotHashachar("alot"),
    TallitTefillin("tallit_tefillin"),
    Sunrise("sunrise"),
    SofZmanShemaMagenAvraham("shema_mga"),
    SofZmanShemaGra("shema_gra"),
    SofZmanTefillahMagenAvraham("tefillah_mga"),
    SofZmanTefillahGra("tefillah_gra"),
    ChatzotHaYom("chatzot_hayom"),
    MinchaGedola("mincha_gedola"),
    MinchaKetana("mincha_ketana"),
    PlagHamincha("plag_hamincha"),
    Sunset("sunset"),
    Tzeit("tzeit"),
    ChatzotHaLaila("chatzot_halaila"),
    // Shabbat-section rows, shown/hidden by the same "Zmanim to show" list.
    ShabbatCandleLighting("shabbat_candles"),
    ShabbatSunset("shabbat_sunset"),
    MotzeiShabbat("motzei_shabbat"),
    RabbeinuTam("rabbeinu_tam"),
    ;

    public companion object {
        public val Default: Set<ZmanimTimeOption> = entries.toSet()

        public fun fromStorageValue(value: String?): ZmanimTimeOption? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/**
 * The daily-learning tracks the user can individually show or hide. [storageValue] is what the
 * settings store and each row's id (once the names of Hebcal's categories, which the app read from
 * before it worked every track out itself, so stored choices carry over). Default: Daf Yomi Bavli, Daf Yomi Yerushalmi, and Rambam Yomi (1 chapter).
 */
public enum class DailyLearningType(public val storageValue: String) {
    DafYomiBavli("dafyomi"),
    DafYomiYerushalmi("yerushalmi"),
    MishnahYomi("mishnayomi"),
    RambamYomi("dailyRambam1"),
    RambamYomiThreeChapters("dailyRambam3"),
    TehillimYomi("dailyPsalms"),
    TanakhYomi("tanakhYomi"),
    ShemiratHaLashon("shemiratHaLashon"),
    KitzurShulchanAruch("kitzurShulchanAruch"),
    ;

    public companion object {
        public val Default: Set<DailyLearningType> = setOf(DafYomiBavli, DafYomiYerushalmi, RambamYomi)

        public fun fromStorageValue(value: String?): DailyLearningType? =
            entries.firstOrNull { it.storageValue == value }
    }
}
