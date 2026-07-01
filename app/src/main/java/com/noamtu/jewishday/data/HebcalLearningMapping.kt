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
    value = displayEnglish(),
    valueHebrew = formattedHebrew(),
    id = type.storageValue,
)

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
 * Hebcal's Shemirat HaLashon Hebrew comes as e.g. "Book I, הקדמה 1-2" or
 * "Book I, שער הזכירה 1.1-1.4" — the gate name is already Hebrew, but with an English "Book I,"/
 * "Book II," prefix and Arabic numerals (and on intro/epilogue days there is no chapter.halacha,
 * only a plain range). Translate the book prefix, turn "chapter.halacha" into "chapter:halacha",
 * and render the numbers as gematria.
 */
private fun formatShemiratHaLashonHebrew(hebrew: String): String = hebrew
    .replace("Book II,", "ספר ב׳")
    .replace("Book I,", "ספר א׳")
    .replace("Book II", "ספר ב׳")
    .replace("Book I", "ספר א׳")
    .replace('.', ':')
    .arabicDigitsToGematria()
    .replace(Regex("\\s+"), " ")
    .trim()

private fun HebcalLearningEntry.displayEnglish(): String = memo?.takeIf(String::isNotBlank) ?: title
private fun HebcalLearningEntry.displayHebrew(): String = hebrew?.takeIf(String::isNotBlank) ?: displayEnglish()
