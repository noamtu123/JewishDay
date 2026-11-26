// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.model

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.YerushalmiYomiCalculator
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.CalendarText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.uiText
import java.time.LocalDate

/** The daily learning KosherJava can work out offline, shown until (or unless) Hebcal answers. */
internal fun dailyLearningItems(
    jewishCalendar: JewishCalendar,
    date: LocalDate,
    inIsrael: Boolean,
): List<ZmanItem> = buildList {
    add(
        ZmanItem(
            title = uiText(DailyLearningType.DafYomiBavli.label),
            time = null,
            description = uiText(R.string.learning_daf_yomi_kosherjava_description),
            value = CalendarText.DafYomiBavli(date, inIsrael).asUiText(),
            id = DailyLearningType.DafYomiBavli.storageValue,
        ),
    )
    // The Yerushalmi cycle skips a few days a year (Yom Kippur, Tisha B'Av); the row goes with them.
    val hasYerushalmiDaf = runCatching { YerushalmiYomiCalculator.getDafYomiYerushalmi(jewishCalendar) }.getOrNull() != null
    if (hasYerushalmiDaf) {
        add(
            ZmanItem(
                title = uiText(DailyLearningType.DafYomiYerushalmi.label),
                time = null,
                description = uiText(R.string.learning_yerushalmi_kosherjava_description),
                value = CalendarText.DafYomiYerushalmi(date, inIsrael).asUiText(),
                id = DailyLearningType.DafYomiYerushalmi.storageValue,
            ),
        )
    }
    add(
        ZmanItem(
            title = uiText(DailyLearningType.TehillimYomi.label),
            time = null,
            description = uiText(R.string.learning_tehillim_kosherjava_description),
            value = CalendarText.TehillimYomi(date, inIsrael).asUiText(),
            id = DailyLearningType.TehillimYomi.storageValue,
        ),
    )
}
