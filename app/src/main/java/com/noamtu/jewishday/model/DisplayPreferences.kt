package com.noamtu.jewishday.model

/**
 * The rows of the main "Zmanim" group that the user can individually show or hide on the
 * Zmanim tab. [storageValue] is the stable id persisted in settings and stamped onto the
 * matching [ZmanItem.id] so the row can be filtered without matching on display text.
 */
enum class ZmanimTimeOption(
    val storageValue: String,
    val labelEnglish: String,
    val labelHebrew: String,
) {
    AlotHashachar("alot", "Alot Hashachar", "עלות השחר"),
    TallitTefillin("tallit_tefillin", "Tallit & Tefillin", "זמן טלית ותפילין"),
    Sunrise("sunrise", "Sunrise", "הנץ החמה"),
    SofZmanShemaMagenAvraham("shema_mga", "Sof Zman Shema (Magen Avraham)", "סוף זמן קריאת שמע (מגן אברהם)"),
    SofZmanShemaGra("shema_gra", "Sof Zman Shema (GRA)", "סוף זמן קריאת שמע (גר״א)"),
    SofZmanTefillahMagenAvraham("tefillah_mga", "Sof Zman Tefillah (Magen Avraham)", "סוף זמן תפילה (מגן אברהם)"),
    SofZmanTefillahGra("tefillah_gra", "Sof Zman Tefillah (GRA)", "סוף זמן תפילה (גר״א)"),
    ChatzotHaYom("chatzot_hayom", "Chatzot HaYom", "חצות היום"),
    MinchaGedola("mincha_gedola", "Mincha Gedola", "מנחה גדולה"),
    MinchaKetana("mincha_ketana", "Mincha Ketana", "מנחה קטנה"),
    PlagHamincha("plag_hamincha", "Plag Hamincha", "פלג המנחה"),
    Sunset("sunset", "Sunset", "שקיעה"),
    Tzeit("tzeit", "Tzeit", "צאת הכוכבים"),
    ChatzotHaLaila("chatzot_halaila", "Chatzot HaLaila", "חצות הלילה"),
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
 * both sources. Default keeps the list short: Daf Yomi Bavli and Rambam Yomi (1 chapter).
 */
enum class DailyLearningType(
    val storageValue: String,
    val labelEnglish: String,
    val labelHebrew: String,
) {
    DafYomiBavli("dafyomi", "Daf Yomi Bavli", "דף יומי בבלי"),
    DafYomiYerushalmi("yerushalmi", "Daf Yomi Yerushalmi", "דף יומי ירושלמי"),
    MishnahYomi("mishnayomi", "Mishnah Yomi", "משנה יומית"),
    RambamYomi("dailyRambam1", "Rambam Yomi", "רמב״ם יומי"),
    TehillimYomi("dailyPsalms", "Tehillim Yomi", "תהילים יומי"),
    TanakhYomi("tanakhYomi", "Tanakh Yomi", "תנ״ך יומי"),
    ShemiratHaLashon("shemiratHaLashon", "Shemirat HaLashon", "שמירת הלשון"),
    KitzurShulchanAruch("kitzurShulchanAruch", "Kitzur Shulchan Aruch", "קיצור שולחן ערוך"),
    ;

    companion object {
        val Default: Set<DailyLearningType> = setOf(DafYomiBavli, RambamYomi)

        fun fromStorageValue(value: String?): DailyLearningType? =
            entries.firstOrNull { it.storageValue == value }
    }
}
