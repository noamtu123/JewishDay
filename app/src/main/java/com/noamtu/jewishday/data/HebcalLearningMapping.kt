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

/**
 * Shemirat HaLashon arrives only in English ("Book II 9.8-9.10"). Render it as
 * "כלל <chapter> <start>-<end>" with a "<n> הלכות יומיות" caption.
 */
private fun HebcalLearningEntry.toShemiratHaLashonRow(): ZmanItem {
    val parsed = parseShemiratHaLashon(title)
    val count = parsed?.halachaCount ?: 0
    return ZmanItem(
        title = "Shemirat HaLashon",
        titleHebrew = "שמירת הלשון",
        time = null,
        description = if (count > 0) "$count daily halachot" else "Daily Shemirat HaLashon",
        descriptionHebrew = if (count > 0) "$count הלכות יומיות" else "שמירת הלשון יומית",
        value = displayEnglish(),
        valueHebrew = parsed?.hebrew ?: displayHebrew(),
        id = DailyLearningType.ShemiratHaLashon.storageValue,
    )
}

/** The Hebrew text to display for an entry, applying per-track gematria formatting. */
private fun HebcalLearningEntry.formattedHebrew(): String = when (category) {
    // Source Hebrew has the right name but Arabic numerals (e.g. "כלים 11:7-8").
    "mishnayomi" -> formatMishnahYomi(displayHebrew()) ?: displayHebrew().arabicDigitsToGematria()
    // Both Rambam tracks share one format so the 1- and 3-chapter rows look identical.
    "dailyRambam1", "dailyRambam3" -> formatRambam(displayHebrew())
    "dailyPsalms" -> displayHebrew().replace("תהלים", "תהילים")
    // English-only "161:18-162:5" -> "קס״א: יח - קס״ב: ה".
    "kitzurShulchanAruch" -> formatKitzurShulchanAruch(title) ?: displayHebrew()
    // Source can start with "תהלים" even though the track is Tanakh Yomi.
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
    val name = if (tokens.first() == "תהלים") "תנ״ך" else tokens.first()
    return (listOf(name) + tokens.drop(1).dropLast(1) + hebrewNumber(number)).joinToString(" ")
}

private data class ShemiratHaLashon(val hebrew: String, val halachaCount: Int)

private fun parseShemiratHaLashon(title: String): ShemiratHaLashon? {
    Regex("(\\d+)\\.(\\d+)\\s*-\\s*(\\d+)\\.(\\d+)").find(title)?.let { match ->
        val (c1, p1, c2, p2) = match.destructured
        return if (c1 == c2) {
            ShemiratHaLashon(
                hebrew = "כלל ${hebrewNumber(c1.toInt())} ${gematriaLetters(p1.toInt())}-${gematriaLetters(p2.toInt())}",
                halachaCount = (p2.toInt() - p1.toInt() + 1).coerceAtLeast(1),
            )
        } else {
            ShemiratHaLashon(
                hebrew = "כלל ${hebrewNumber(c1.toInt())} ${gematriaLetters(p1.toInt())} - כלל ${hebrewNumber(c2.toInt())} ${gematriaLetters(p2.toInt())}",
                halachaCount = 0,
            )
        }
    }
    Regex("(\\d+)\\.(\\d+)").find(title)?.let { match ->
        val (c, p) = match.destructured
        return ShemiratHaLashon(
            hebrew = "כלל ${hebrewNumber(c.toInt())} ${gematriaLetters(p.toInt())}",
            halachaCount = 1,
        )
    }
    return null
}

private fun HebcalLearningEntry.displayEnglish(): String = memo?.takeIf(String::isNotBlank) ?: title
private fun HebcalLearningEntry.displayHebrew(): String = hebrew?.takeIf(String::isNotBlank) ?: displayEnglish()
