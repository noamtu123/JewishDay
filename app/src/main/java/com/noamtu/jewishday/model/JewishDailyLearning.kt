package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.HebrewDateFormatter
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator

internal fun dailyLearningItems(
    jewishCalendar: JewishCalendar,
    englishFormatter: HebrewDateFormatter,
    hebrewFormatter: HebrewDateFormatter,
): List<ZmanItem> = buildList {
    add(
        ZmanItem(
            title = "Daf Yomi Bavli",
            titleHebrew = "דף יומי בבלי",
            time = null,
            description = "KosherJava Daf Yomi cycle",
            descriptionHebrew = "מחזור דף יומי של KosherJava",
            value = englishFormatter.formatDafYomiBavli(jewishCalendar.dafYomiBavli),
            valueHebrew = hebrewFormatter.formatDafYomiBavli(jewishCalendar.dafYomiBavli),
        ),
    )
    val yerushalmiDaf = runCatching { YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar) }.getOrNull()
    if (yerushalmiDaf != null) {
        add(
            ZmanItem(
                title = "Daf Yomi Yerushalmi",
                titleHebrew = "דף יומי ירושלמי",
                time = null,
                description = "KosherJava Yerushalmi cycle",
                descriptionHebrew = "מחזור ירושלמי של KosherJava",
                value = englishFormatter.formatDafYomiYerushalmi(yerushalmiDaf),
                valueHebrew = hebrewFormatter.formatDafYomiYerushalmi(yerushalmiDaf),
            ),
        )
    }
    add(
        ZmanItem(
            title = "Tehillim Yomi",
            titleHebrew = "תהילים יומי",
            time = null,
            description = "Monthly Tehillim division by Hebrew date",
            descriptionHebrew = "חלוקה חודשית לפי היום בחודש העברי",
            value = jewishCalendar.tehillimYomiEnglish(),
            valueHebrew = jewishCalendar.tehillimYomiHebrew(hebrewFormatter),
        ),
    )
}
