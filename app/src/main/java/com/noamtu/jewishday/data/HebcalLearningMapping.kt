package com.noamtu.jewishday.data

import com.noamtu.jewishday.model.ZmanItem

internal fun List<HebcalLearningEntry>.toZmanItems(includeRambamThreeChapters: Boolean): List<ZmanItem> {
    val byCategory = groupBy(HebcalLearningEntry::category)
    fun entry(category: String): HebcalLearningEntry? = byCategory[category]?.firstOrNull()

    return buildList {
        entry("dafyomi")?.let {
            add(it.toZmanItem("Daf Yomi Bavli", "דף יומי בבלי", "Hebcal Daf Yomi cycle", "מחזור דף יומי של Hebcal"))
        }
        entry("mishnayomi")?.let {
            add(it.toZmanItem("Mishnah Yomi", "משנה יומית", "Hebcal Mishnah Yomi cycle", "מחזור משנה יומית של Hebcal"))
        }
        entry("yerushalmi")?.let {
            add(it.toZmanItem("Daf Yomi Yerushalmi", "דף יומי ירושלמי", "Hebcal Yerushalmi cycle", "מחזור ירושלמי של Hebcal"))
        }
        entry("dailyPsalms")?.let {
            add(it.toZmanItem("Tehillim Yomi", "תהילים יומי", "Daily Tehillim division", "חלוקת תהילים יומית"))
        }

        val rambamOne = entry("dailyRambam1")
        val rambamThree = entry("dailyRambam3")?.takeIf { includeRambamThreeChapters }
        if (rambamOne != null || rambamThree != null) {
            add(rambamItem(rambamOne, rambamThree))
        }

        entry("shemiratHaLashon")?.let {
            add(it.toZmanItem("Shemirat HaLashon", "שמירת הלשון", "Hebcal daily Shemirat HaLashon", "שמירת הלשון יומית של Hebcal"))
        }
        entry("kitzurShulchanAruch")?.let {
            add(it.toZmanItem("Kitzur Shulchan Aruch", "קיצור שולחן ערוך", "Daily halacha from Hebcal", "הלכה יומית של Hebcal"))
        }
        entry("tanakhYomi")?.let {
            add(it.toZmanItem("Tanakh Yomi", "תנ״ך יומי", "Hebcal Tanakh Yomi cycle", "מחזור תנ״ך יומי של Hebcal"))
        }
    }
}

private fun HebcalLearningEntry.toZmanItem(
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
    valueHebrew = displayHebrew(),
)

private fun rambamItem(
    rambamOne: HebcalLearningEntry?,
    rambamThree: HebcalLearningEntry?,
): ZmanItem {
    val oneEnglish = rambamOne?.displayEnglish()
    val oneHebrew = rambamOne?.displayHebrew()
    val threeEnglish = rambamThree?.displayEnglish()
    val threeHebrew = rambamThree?.displayHebrew()
    return ZmanItem(
        title = "Rambam Yomi",
        titleHebrew = "רמב״ם יומי",
        time = null,
        description = if (rambamThree == null) "Hebcal Rambam, 1 chapter" else "Hebcal Rambam, 1 and 3 chapter tracks",
        descriptionHebrew = if (rambamThree == null) "רמב״ם יומי של Hebcal, פרק אחד" else "רמב״ם יומי של Hebcal, פרק אחד ושלושה פרקים",
        value = listOfNotNull(
            oneEnglish?.let { "1 chapter: $it" },
            threeEnglish?.let { "3 chapters: $it" },
        ).joinToString(" · "),
        valueHebrew = listOfNotNull(
            oneHebrew?.let { "פרק אחד: $it" },
            threeHebrew?.let { "3 פרקים: $it" },
        ).joinToString(" · "),
    )
}

private fun HebcalLearningEntry.displayEnglish(): String = memo?.takeIf(String::isNotBlank) ?: title
private fun HebcalLearningEntry.displayHebrew(): String = hebrew?.takeIf(String::isNotBlank) ?: displayEnglish()
