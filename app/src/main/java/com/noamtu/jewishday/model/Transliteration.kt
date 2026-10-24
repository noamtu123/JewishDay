// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.Daf
import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import java.util.EnumMap

/**
 * The formatter for every English name the app shows — holidays, months, parshiot. KosherJava's
 * own transliteration is Ashkenazi ("Succos", "Shavuos", "Teves", "Bereshis"); the app speaks the
 * spelling most people read and write today ("Sukkot", "Shavuot", "Tevet", "Bereshit"), the same one
 * its own text already uses ("Shabbat", "Parashat"). Never build a plain `HebrewDateFormatter()` for
 * English text — it would bring the old spellings back.
 */
fun englishHebrewDateFormatter(): HebrewDateFormatter {
    ModernMasechtot.install()
    return HebrewDateFormatter().apply {
        transliteratedHolidayList = ModernHolidays
        transliteratedMonthList = ModernMonths
        transliteratedShabbosDayOfWeek = "Shabbat"
        setTransliteratedParshiosList(EnumMap(ModernParshiot))
    }
}

/**
 * The masechta names live in static arrays on [Daf] rather than on the formatter, so they are set
 * once per process, the first time any English formatter is built.
 */
private object ModernMasechtot {
    init {
        // Any Daf will do: the setters write the shared, static name lists.
        Daf(0, 2).apply {
            setMasechtaTransliterated(ModernBavliMasechtot)
            setYerushalmiMasechtaTransliterated(ModernYerushalmiMasechtot)
        }
    }

    fun install() = Unit
}

// Indexed like JewishCalendar's yom tov constants (EREV_PESACH = 0 … ISRU_CHAG = 35).
private val ModernHolidays = arrayOf(
    "Erev Pesach", "Pesach", "Chol Hamoed Pesach", "Pesach Sheni", "Erev Shavuot", "Shavuot",
    "Seventeenth of Tammuz", "Tisha B'Av", "Tu B'Av", "Erev Rosh Hashana", "Rosh Hashana",
    "Fast of Gedalyah", "Erev Yom Kippur", "Yom Kippur", "Erev Sukkot", "Sukkot",
    "Chol Hamoed Sukkot", "Hoshana Rabbah", "Shemini Atzeret", "Simchat Torah", "Erev Chanukah",
    "Chanukah", "Tenth of Tevet", "Tu BiShvat", "Fast of Esther", "Purim", "Shushan Purim",
    "Purim Katan", "Rosh Chodesh", "Yom HaShoah", "Yom HaZikaron", "Yom Ha'atzmaut",
    "Yom Yerushalayim", "Lag BaOmer", "Shushan Purim Katan", "Isru Chag",
)

// Indexed from Nissan, then Adar II and Adar I at the end, as KosherJava expects.
private val ModernMonths = arrayOf(
    "Nissan", "Iyar", "Sivan", "Tammuz", "Av", "Elul", "Tishrei", "Cheshvan", "Kislev", "Tevet",
    "Shevat", "Adar", "Adar II", "Adar I",
)

private val ModernParshiot: Map<JewishCalendar.Parsha, String> = mapOf(
    JewishCalendar.Parsha.NONE to "",
    JewishCalendar.Parsha.BERESHIS to "Bereshit",
    JewishCalendar.Parsha.NOACH to "Noach",
    JewishCalendar.Parsha.LECH_LECHA to "Lech Lecha",
    JewishCalendar.Parsha.VAYERA to "Vayera",
    JewishCalendar.Parsha.CHAYEI_SARA to "Chayei Sara",
    JewishCalendar.Parsha.TOLDOS to "Toldot",
    JewishCalendar.Parsha.VAYETZEI to "Vayetzei",
    JewishCalendar.Parsha.VAYISHLACH to "Vayishlach",
    JewishCalendar.Parsha.VAYESHEV to "Vayeshev",
    JewishCalendar.Parsha.MIKETZ to "Miketz",
    JewishCalendar.Parsha.VAYIGASH to "Vayigash",
    JewishCalendar.Parsha.VAYECHI to "Vayechi",
    JewishCalendar.Parsha.SHEMOS to "Shemot",
    JewishCalendar.Parsha.VAERA to "Vaera",
    JewishCalendar.Parsha.BO to "Bo",
    JewishCalendar.Parsha.BESHALACH to "Beshalach",
    JewishCalendar.Parsha.YISRO to "Yitro",
    JewishCalendar.Parsha.MISHPATIM to "Mishpatim",
    JewishCalendar.Parsha.TERUMAH to "Terumah",
    JewishCalendar.Parsha.TETZAVEH to "Tetzaveh",
    JewishCalendar.Parsha.KI_SISA to "Ki Tisa",
    JewishCalendar.Parsha.VAYAKHEL to "Vayakhel",
    JewishCalendar.Parsha.PEKUDEI to "Pekudei",
    JewishCalendar.Parsha.VAYIKRA to "Vayikra",
    JewishCalendar.Parsha.TZAV to "Tzav",
    JewishCalendar.Parsha.SHMINI to "Shmini",
    JewishCalendar.Parsha.TAZRIA to "Tazria",
    JewishCalendar.Parsha.METZORA to "Metzora",
    JewishCalendar.Parsha.ACHREI_MOS to "Acharei Mot",
    JewishCalendar.Parsha.KEDOSHIM to "Kedoshim",
    JewishCalendar.Parsha.EMOR to "Emor",
    JewishCalendar.Parsha.BEHAR to "Behar",
    JewishCalendar.Parsha.BECHUKOSAI to "Bechukotai",
    JewishCalendar.Parsha.BAMIDBAR to "Bamidbar",
    JewishCalendar.Parsha.NASSO to "Nasso",
    JewishCalendar.Parsha.BEHAALOSCHA to "Beha'alotcha",
    JewishCalendar.Parsha.SHLACH to "Sh'lach",
    JewishCalendar.Parsha.KORACH to "Korach",
    JewishCalendar.Parsha.CHUKAS to "Chukat",
    JewishCalendar.Parsha.BALAK to "Balak",
    JewishCalendar.Parsha.PINCHAS to "Pinchas",
    JewishCalendar.Parsha.MATOS to "Matot",
    JewishCalendar.Parsha.MASEI to "Masei",
    JewishCalendar.Parsha.DEVARIM to "Devarim",
    JewishCalendar.Parsha.VAESCHANAN to "Vaetchanan",
    JewishCalendar.Parsha.EIKEV to "Eikev",
    JewishCalendar.Parsha.REEH to "Re'eh",
    JewishCalendar.Parsha.SHOFTIM to "Shoftim",
    JewishCalendar.Parsha.KI_SEITZEI to "Ki Teitzei",
    JewishCalendar.Parsha.KI_SAVO to "Ki Tavo",
    JewishCalendar.Parsha.NITZAVIM to "Nitzavim",
    JewishCalendar.Parsha.VAYEILECH to "Vayeilech",
    JewishCalendar.Parsha.HAAZINU to "Ha'Azinu",
    JewishCalendar.Parsha.VZOS_HABERACHA to "Vezot Haberacha",
    JewishCalendar.Parsha.VAYAKHEL_PEKUDEI to "Vayakhel-Pekudei",
    JewishCalendar.Parsha.TAZRIA_METZORA to "Tazria-Metzora",
    JewishCalendar.Parsha.ACHREI_MOS_KEDOSHIM to "Acharei Mot-Kedoshim",
    JewishCalendar.Parsha.BEHAR_BECHUKOSAI to "Behar-Bechukotai",
    JewishCalendar.Parsha.CHUKAS_BALAK to "Chukat-Balak",
    JewishCalendar.Parsha.MATOS_MASEI to "Matot-Masei",
    JewishCalendar.Parsha.NITZAVIM_VAYEILECH to "Nitzavim-Vayeilech",
    JewishCalendar.Parsha.SHKALIM to "Shekalim",
    JewishCalendar.Parsha.ZACHOR to "Zachor",
    JewishCalendar.Parsha.PARA to "Parah",
    JewishCalendar.Parsha.HACHODESH to "Hachodesh",
    JewishCalendar.Parsha.SHUVA to "Shuva",
    JewishCalendar.Parsha.SHIRA to "Shira",
    JewishCalendar.Parsha.HAGADOL to "Hagadol",
    JewishCalendar.Parsha.CHAZON to "Chazon",
    JewishCalendar.Parsha.NACHAMU to "Nachamu",
)

private val ModernBavliMasechtot = arrayOf(
    "Berachot", "Shabbat", "Eruvin", "Pesachim", "Shekalim", "Yoma", "Sukkah", "Beitzah",
    "Rosh Hashana", "Taanit", "Megillah", "Moed Katan", "Chagigah", "Yevamot", "Ketubot", "Nedarim",
    "Nazir", "Sotah", "Gittin", "Kiddushin", "Bava Kamma", "Bava Metzia", "Bava Batra", "Sanhedrin",
    "Makkot", "Shevuot", "Avodah Zarah", "Horayot", "Zevachim", "Menachot", "Chullin", "Bechorot",
    "Arachin", "Temurah", "Keritot", "Meilah", "Kinnim", "Tamid", "Middot", "Niddah",
)

// The last entry is KosherJava's placeholder for the days the Yerushalmi cycle skips.
private val ModernYerushalmiMasechtot = arrayOf(
    "Berachot", "Pe'ah", "Demai", "Kilayim", "Shevi'it", "Terumot", "Ma'asrot", "Ma'aser Sheni",
    "Challah", "Orlah", "Bikkurim", "Shabbat", "Eruvin", "Pesachim", "Beitzah", "Rosh Hashana",
    "Yoma", "Sukkah", "Taanit", "Shekalim", "Megillah", "Chagigah", "Moed Katan", "Yevamot",
    "Ketubot", "Sotah", "Nedarim", "Nazir", "Gittin", "Kiddushin", "Bava Kamma", "Bava Metzia",
    "Bava Batra", "Shevuot", "Makkot", "Sanhedrin", "Avodah Zarah", "Horayot", "Niddah",
    "No Daf Today",
)
