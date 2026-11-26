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
        setTransliteratedParshiyosList(EnumMap(ModernParshiot))
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

private val ModernParshiot: Map<JewishCalendar.Parshah, String> = mapOf(
    JewishCalendar.Parshah.NONE to "",
    JewishCalendar.Parshah.BERESHIS to "Bereshit",
    JewishCalendar.Parshah.NOACH to "Noach",
    JewishCalendar.Parshah.LECH_LECHA to "Lech Lecha",
    JewishCalendar.Parshah.VAYERA to "Vayera",
    JewishCalendar.Parshah.CHAYEI_SARA to "Chayei Sara",
    JewishCalendar.Parshah.TOLDOS to "Toldot",
    JewishCalendar.Parshah.VAYETZEI to "Vayetzei",
    JewishCalendar.Parshah.VAYISHLACH to "Vayishlach",
    JewishCalendar.Parshah.VAYESHEV to "Vayeshev",
    JewishCalendar.Parshah.MIKETZ to "Miketz",
    JewishCalendar.Parshah.VAYIGASH to "Vayigash",
    JewishCalendar.Parshah.VAYECHI to "Vayechi",
    JewishCalendar.Parshah.SHEMOS to "Shemot",
    JewishCalendar.Parshah.VAERA to "Vaera",
    JewishCalendar.Parshah.BO to "Bo",
    JewishCalendar.Parshah.BESHALACH to "Beshalach",
    JewishCalendar.Parshah.YISRO to "Yitro",
    JewishCalendar.Parshah.MISHPATIM to "Mishpatim",
    JewishCalendar.Parshah.TERUMAH to "Terumah",
    JewishCalendar.Parshah.TETZAVEH to "Tetzaveh",
    JewishCalendar.Parshah.KI_SISA to "Ki Tisa",
    JewishCalendar.Parshah.VAYAKHEL to "Vayakhel",
    JewishCalendar.Parshah.PEKUDEI to "Pekudei",
    JewishCalendar.Parshah.VAYIKRA to "Vayikra",
    JewishCalendar.Parshah.TZAV to "Tzav",
    JewishCalendar.Parshah.SHMINI to "Shmini",
    JewishCalendar.Parshah.TAZRIA to "Tazria",
    JewishCalendar.Parshah.METZORA to "Metzora",
    JewishCalendar.Parshah.ACHREI_MOS to "Acharei Mot",
    JewishCalendar.Parshah.KEDOSHIM to "Kedoshim",
    JewishCalendar.Parshah.EMOR to "Emor",
    JewishCalendar.Parshah.BEHAR to "Behar",
    JewishCalendar.Parshah.BECHUKOSAI to "Bechukotai",
    JewishCalendar.Parshah.BAMIDBAR to "Bamidbar",
    JewishCalendar.Parshah.NASSO to "Nasso",
    JewishCalendar.Parshah.BEHAALOSCHA to "Beha'alotcha",
    JewishCalendar.Parshah.SHLACH to "Sh'lach",
    JewishCalendar.Parshah.KORACH to "Korach",
    JewishCalendar.Parshah.CHUKAS to "Chukat",
    JewishCalendar.Parshah.BALAK to "Balak",
    JewishCalendar.Parshah.PINCHAS to "Pinchas",
    JewishCalendar.Parshah.MATOS to "Matot",
    JewishCalendar.Parshah.MASEI to "Masei",
    JewishCalendar.Parshah.DEVARIM to "Devarim",
    JewishCalendar.Parshah.VAESCHANAN to "Vaetchanan",
    JewishCalendar.Parshah.EIKEV to "Eikev",
    JewishCalendar.Parshah.REEH to "Re'eh",
    JewishCalendar.Parshah.SHOFTIM to "Shoftim",
    JewishCalendar.Parshah.KI_SEITZEI to "Ki Teitzei",
    JewishCalendar.Parshah.KI_SAVO to "Ki Tavo",
    JewishCalendar.Parshah.NITZAVIM to "Nitzavim",
    JewishCalendar.Parshah.VAYEILECH to "Vayeilech",
    JewishCalendar.Parshah.HAAZINU to "Ha'Azinu",
    JewishCalendar.Parshah.VZOS_HABERACHA to "Vezot Haberacha",
    JewishCalendar.Parshah.VAYAKHEL_PEKUDEI to "Vayakhel-Pekudei",
    JewishCalendar.Parshah.TAZRIA_METZORA to "Tazria-Metzora",
    JewishCalendar.Parshah.ACHREI_MOS_KEDOSHIM to "Acharei Mot-Kedoshim",
    JewishCalendar.Parshah.BEHAR_BECHUKOSAI to "Behar-Bechukotai",
    JewishCalendar.Parshah.CHUKAS_BALAK to "Chukat-Balak",
    JewishCalendar.Parshah.MATOS_MASEI to "Matot-Masei",
    JewishCalendar.Parshah.NITZAVIM_VAYEILECH to "Nitzavim-Vayeilech",
    JewishCalendar.Parshah.SHKALIM to "Shekalim",
    JewishCalendar.Parshah.ZACHOR to "Zachor",
    JewishCalendar.Parshah.PARA to "Parah",
    JewishCalendar.Parshah.HACHODESH to "Hachodesh",
    JewishCalendar.Parshah.SHUVA to "Shuva",
    JewishCalendar.Parshah.SHIRA to "Shira",
    JewishCalendar.Parshah.HAGADOL to "Hagadol",
    JewishCalendar.Parshah.CHAZON to "Chazon",
    JewishCalendar.Parshah.NACHAMU to "Nachamu",
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
