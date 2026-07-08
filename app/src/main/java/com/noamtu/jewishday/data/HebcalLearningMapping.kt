// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.ZmanItem

internal fun List<HebcalLearningEntry>.toZmanItems(): List<ZmanItem> {
    val byCategory = groupBy(HebcalLearningEntry::category)
    fun entry(category: String): HebcalLearningEntry? = byCategory[category]?.firstOrNull()

    return buildList {
        entry("dafyomi")?.let {
            add(it.toRow(DailyLearningType.DafYomiBavli, "Daf Yomi Bavli", "דף יומי בבלי", "Hebcal Daf Yomi cycle", "מחזור דף יומי של Hebcal"))
        }
        entry("yerushalmi")?.let {
            add(it.toRow(DailyLearningType.DafYomiYerushalmi, "Daf Yomi Yerushalmi", "דף יומי ירושלמי", "Hebcal Yerushalmi cycle", "מחזור ירושלמי של Hebcal"))
        }
        entry("mishnayomi")?.let {
            add(it.toRow(DailyLearningType.MishnahYomi, "Mishnah Yomi", "משנה יומית", "Hebcal Mishnah Yomi cycle", "מחזור משנה יומית של Hebcal"))
        }

        // Both Rambam tracks are independent daily-learning items (each shown/hidden on its own)
        // but share the "Rambam Yomi" title and identical formatting, so when both are enabled
        // they read as one section with a 1-chapter and a 3-chapter entry.
        entry("dailyRambam1")?.let {
            add(it.toRow(DailyLearningType.RambamYomi, "Rambam Yomi", "רמב״ם יומי", "1 chapter", "פרק אחד"))
        }
        entry("dailyRambam3")?.let {
            add(it.toRow(DailyLearningType.RambamYomiThreeChapters, "Rambam Yomi", "רמב״ם יומי", "3 chapters", "3 פרקים"))
        }

        entry("dailyPsalms")?.let {
            add(it.toRow(DailyLearningType.TehillimYomi, "Tehillim Yomi", "תהילים יומי", "Daily Tehillim division", "חלוקת תהילים יומית"))
        }
        entry("tanakhYomi")?.let {
            add(it.toRow(DailyLearningType.TanakhYomi, "Tanakh Yomi", "תנ״ך יומי", "Hebcal Tanakh Yomi cycle", "מחזור תנ״ך יומי של Hebcal"))
        }
        entry("shemiratHaLashon")?.let {
            add(it.toShemiratHaLashonRow())
        }
        entry("kitzurShulchanAruch")?.let {
            add(it.toRow(DailyLearningType.KitzurShulchanAruch, "Kitzur Shulchan Aruch", "קיצור שולחן ערוך", "Daily halacha from Hebcal", "הלכה יומית של Hebcal"))
        }
    }
}

private fun HebcalLearningEntry.toRow(
    type: DailyLearningType,
    title: String,
    titleHebrew: String,
    description: String,
    descriptionHebrew: String,
): ZmanItem = ZmanItem(
    title = title,
    titleHebrew = titleHebrew,
    time = null,
    description = description,
    descriptionHebrew = descriptionHebrew,
    value = formattedEnglish(),
    valueHebrew = formattedHebrew(),
    id = type.storageValue,
)

/** The English text to display for an entry. */
private fun HebcalLearningEntry.formattedEnglish(): String = when (category) {
    // On multi-book Rambam days Hebcal's memo is a Sefaria link list ("<name> 8\nhttps://…"),
    // so the memo must never be used as the display value — the title already has both books.
    "dailyRambam1", "dailyRambam3" -> title
    else -> displayEnglish()
}

private fun HebcalLearningEntry.toShemiratHaLashonRow(): ZmanItem = ZmanItem(
    title = "Shemirat HaLashon",
    titleHebrew = "שמירת הלשון",
    time = null,
    description = "Daily Shemirat HaLashon",
    descriptionHebrew = "שמירת הלשון יומית",
    value = displayEnglish(),
    valueHebrew = formatShemiratHaLashonHebrew(displayHebrew()),
    id = DailyLearningType.ShemiratHaLashon.storageValue,
)

/** The Hebrew text to display for an entry, applying per-track gematria formatting. */
private fun HebcalLearningEntry.formattedHebrew(): String = when (category) {
    // Source Hebrew has the right name but Arabic numerals (e.g. "כלים 11:7-8").
    "mishnayomi" -> formatMishnahYomi(displayHebrew()) ?: displayHebrew().arabicDigitsToGematria()
    // Both Rambam tracks share one format so the 1- and 3-chapter rows look identical.
    "dailyRambam1", "dailyRambam3" -> formatRambam(displayHebrew())
    "dailyPsalms" -> displayHebrew().replace("תהלים", "תהילים")
    // English-only "161:18-162:5" -> "קס״א: יח - קס״ב: ה".
    "kitzurShulchanAruch" -> formatKitzurShulchanAruch(title) ?: displayHebrew()
    // Source can start with "תהלים" even though the track is Tanakh Yomi; the title carries
    // both the real book name and the seder number (e.g. "Genesis 13", "Psalms Seder 3").
    "tanakhYomi" -> formatTanakhYomi(displayHebrew(), title)
    else -> displayHebrew()
}

/**
 * Rambam arrives as "הלכות <name> פרק כח" (1 chapter, already Hebrew letters) or
 * "הלכות <name> פרק 2-4" (3 chapters, Arabic digits + a singular פרק). Convert digit runs to
 * plain gematria letters (no geresh, matching the 1-chapter style) and pluralize פרק -> פרקים
 * for a chapter range, so both tracks render in the same style.
 */
private fun formatRambam(sourceHebrew: String): String {
    val isRange = Regex("\\d+\\s*-\\s*\\d+").containsMatchIn(sourceHebrew)
    val withLetters = Regex("\\d+").replace(sourceHebrew) { gematriaLetters(it.value.toInt()) }
    return if (isRange) withLetters.replaceFirst("פרק ", "פרקים ") else withLetters
}

private fun formatMishnahYomi(sourceHebrew: String): String? {
    Regex("(.+?)\\s+(\\d+):(\\d+)\\s*-\\s*(\\d+)").find(sourceHebrew)?.let { match ->
        val (name, chapter, first, last) = match.destructured
        return "$name ${hebrewNumber(chapter.toInt())}: ${gematriaLetters(first.toInt())}-${gematriaLetters(last.toInt())}"
    }
    Regex("(.+?)\\s+(\\d+):(\\d+)").find(sourceHebrew)?.let { match ->
        val (name, chapter, mishnah) = match.destructured
        return "$name ${hebrewNumber(chapter.toInt())}: ${gematriaLetters(mishnah.toInt())}"
    }
    return null
}

private fun formatKitzurShulchanAruch(title: String): String? {
    Regex("(\\d+):(\\d+)\\s*-\\s*(\\d+):(\\d+)").find(title)?.let { match ->
        val (a, b, c, d) = match.destructured
        return "${hebrewNumber(a.toInt())}: ${gematriaLetters(b.toInt())} - ${hebrewNumber(c.toInt())}: ${gematriaLetters(d.toInt())}"
    }
    Regex("(\\d+):(\\d+)").find(title)?.let { match ->
        val (a, b) = match.destructured
        return "${hebrewNumber(a.toInt())}: ${gematriaLetters(b.toInt())}"
    }
    return null
}

private fun formatTanakhYomi(sourceHebrew: String, title: String): String {
    val number = title.lastIntegerOrNull() ?: return sourceHebrew
    val tokens = sourceHebrew.trim().split(Regex("\\s+"))
    if (tokens.size < 2) return sourceHebrew
    val bookName = EnglishToHebrewTanakhBook.entries
        .firstOrNull { title.contains(it.key, ignoreCase = true) }?.value
        ?: tokens.first()
    return (listOf(bookName) + tokens.drop(1).dropLast(1) + hebrewNumber(number)).joinToString(" ")
}

private val EnglishToHebrewTanakhBook = linkedMapOf(
    "I Chronicles" to "דברי הימים א",
    "II Chronicles" to "דברי הימים ב",
    "I Samuel" to "שמואל א",
    "II Samuel" to "שמואל ב",
    "I Kings" to "מלכים א",
    "II Kings" to "מלכים ב",
    "Song of Songs" to "שיר השירים",
    "Genesis" to "בראשית",
    "Exodus" to "שמות",
    "Leviticus" to "ויקרא",
    "Numbers" to "במדבר",
    "Deuteronomy" to "דברים",
    "Joshua" to "יהושע",
    "Judges" to "שופטים",
    "Isaiah" to "ישעיה",
    "Jeremiah" to "ירמיה",
    "Ezekiel" to "יחזקאל",
    "Hosea" to "הושע",
    "Joel" to "יואל",
    "Amos" to "עמוס",
    "Obadiah" to "עובדיה",
    "Jonah" to "יונה",
    "Micah" to "מיכה",
    "Nahum" to "נחום",
    "Habakkuk" to "חבקוק",
    "Zephaniah" to "צפניה",
    "Haggai" to "חגי",
    "Zechariah" to "זכריה",
    "Malachi" to "מלאכי",
    "Psalms" to "תהלים",
    "Proverbs" to "משלי",
    "Job" to "איוב",
    "Ruth" to "רות",
    "Lamentations" to "איכה",
    "Ecclesiastes" to "קהלת",
    "Esther" to "אסתר",
    "Daniel" to "דניאל",
    "Ezra" to "עזרא",
    "Nehemiah" to "נחמיה",
    "Samuel" to "שמואל",
    "Kings" to "מלכים",
    "Chronicles" to "דברי הימים",
)

/**
 * Sefer Shemirat HaLashon has two chelek (books): chelek alef is divided into named shaarim
 * (Hakdamah; Shaar HaZechira/Shaar Alef, 17 perakim; Shaar HaTevuna/Shaar Bet, 17 perakim; Shaar
 * HaTorah/Shaar Gimel, 10 perakim; a 7-perek Chatima), while chelek bet has no named shaarim —
 * just 30 perakim followed by a 4-perek Chatima.
 *
 * Hebcal's Hebrew field already translates a shaar/Chatima name when there is one (e.g.
 * "Book I, שער הזכירה 1.1-1.4"), but for chelek bet's unnamed body it isn't translated at all
 * (e.g. "Book II 1.1-1.2" — identical to the English title). This parses the "Book I"/"Book II"
 * prefix and the trailing perek.halacha reference structurally instead of relying on the source
 * always supplying a translated name, and renders "<gate> <perek>: <halacha>" to match the
 * chapter:halacha convention already used for Mishnah Yomi / Kitzur Shulchan Aruch.
 */
private fun formatShemiratHaLashonHebrew(hebrew: String): String {
    val bookNumber = when {
        hebrew.startsWith("Book II") -> 2
        hebrew.startsWith("Book I") -> 1
        else -> return hebrew.arabicDigitsToGematria()
    }
    val rest = hebrew
        .removePrefix("Book II,").removePrefix("Book II")
        .removePrefix("Book I,").removePrefix("Book I")
        .trim()
    val bookLabel = "חלק ${hebrewNumber(bookNumber)}"
    val match = Regex("^(.*?)\\s*([\\d.\\-]+)$").find(rest)
        ?: return listOf(bookLabel, rest).filter(String::isNotBlank).joinToString(" ")
    val gateName = match.groupValues[1].trim()
    val reference = formatShemiratHaLashonReference(match.groupValues[2])
    return listOf(bookLabel, gateName, reference).filter(String::isNotBlank).joinToString(" ")
}

/** Renders a "perek.halacha[-perek.halacha]" or plain "halacha-halacha" (e.g. Hakdamah) reference. */
private fun formatShemiratHaLashonReference(reference: String): String {
    Regex("^(\\d+)\\.(\\d+)-(\\d+)\\.(\\d+)$").find(reference)?.let { match ->
        val (c1, h1, c2, h2) = match.destructured
        return if (c1 == c2) {
            "${hebrewNumber(c1.toInt())}: ${gematriaLetters(h1.toInt())}-${gematriaLetters(h2.toInt())}"
        } else {
            "${hebrewNumber(c1.toInt())}: ${gematriaLetters(h1.toInt())} - ${hebrewNumber(c2.toInt())}: ${gematriaLetters(h2.toInt())}"
        }
    }
    Regex("^(\\d+)\\.(\\d+)$").find(reference)?.let { match ->
        val (chapter, halacha) = match.destructured
        return "${hebrewNumber(chapter.toInt())}: ${gematriaLetters(halacha.toInt())}"
    }
    Regex("^(\\d+)-(\\d+)$").find(reference)?.let { match ->
        val (first, last) = match.destructured
        return "${gematriaLetters(first.toInt())}-${gematriaLetters(last.toInt())}"
    }
    Regex("^(\\d+)$").find(reference)?.let { match -> return gematriaLetters(match.value.toInt()) }
    return reference.arabicDigitsToGematria()
}

private fun HebcalLearningEntry.displayEnglish(): String = memo?.takeIf(String::isNotBlank) ?: title
private fun HebcalLearningEntry.displayHebrew(): String = hebrew?.takeIf(String::isNotBlank) ?: displayEnglish()