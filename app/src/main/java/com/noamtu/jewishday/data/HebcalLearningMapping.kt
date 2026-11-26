// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import androidx.annotation.StringRes
import com.noamtu.jewishday.R
import com.noamtu.jewishday.i18n.CalendarText
import com.noamtu.jewishday.i18n.UiText
import com.noamtu.jewishday.i18n.asUiText
import com.noamtu.jewishday.i18n.pluralText
import com.noamtu.jewishday.i18n.uiText
import com.noamtu.jewishday.model.DailyLearningType
import com.noamtu.jewishday.model.ZmanItem

internal fun List<HebcalLearningEntry>.toZmanItems(): List<ZmanItem> {
    val byCategory = groupBy(HebcalLearningEntry::category)
    fun entry(category: String): HebcalLearningEntry? = byCategory[category]?.firstOrNull()

    return buildList {
        entry("dafyomi")?.let { add(it.toRow(DailyLearningType.DafYomiBavli, uiText(R.string.learning_daf_yomi_hebcal_description))) }
        entry("yerushalmi")?.let { add(it.toRow(DailyLearningType.DafYomiYerushalmi, uiText(R.string.learning_yerushalmi_hebcal_description))) }
        entry("mishnayomi")?.let { add(it.toRow(DailyLearningType.MishnahYomi, uiText(R.string.learning_mishnah_yomi_hebcal_description))) }

        // Both Rambam tracks are independent daily-learning items (each shown/hidden on its own)
        // but share the "Rambam Yomi" title and identical formatting, so when both are enabled
        // they read as one section with a 1-chapter and a 3-chapter entry.
        entry("dailyRambam1")?.let {
            add(it.toRow(DailyLearningType.RambamYomi, pluralText(R.plurals.learning_rambam_chapters, 1), title = R.string.learning_rambam_yomi))
        }
        entry("dailyRambam3")?.let {
            add(it.toRow(DailyLearningType.RambamYomiThreeChapters, pluralText(R.plurals.learning_rambam_chapters, 3), title = R.string.learning_rambam_yomi))
        }

        entry("dailyPsalms")?.let { add(it.toRow(DailyLearningType.TehillimYomi, uiText(R.string.learning_tehillim_hebcal_description))) }
        entry("tanakhYomi")?.let { add(it.toRow(DailyLearningType.TanakhYomi, uiText(R.string.learning_tanakh_yomi_hebcal_description))) }
        entry("shemiratHaLashon")?.let { add(it.toRow(DailyLearningType.ShemiratHaLashon, uiText(R.string.learning_shemirat_halashon_hebcal_description))) }
        entry("kitzurShulchanAruch")?.let { add(it.toRow(DailyLearningType.KitzurShulchanAruch, uiText(R.string.learning_kitzur_hebcal_description))) }
    }
}

private fun HebcalLearningEntry.toRow(
    type: DailyLearningType,
    description: UiText,
    @StringRes title: Int = type.label,
): ZmanItem = ZmanItem(
    title = uiText(title),
    time = null,
    description = description,
    // Worded at the edge, in whatever language is showing: Hebcal sends English and Hebrew both.
    value = CalendarText.Hebcal(this).asUiText(),
    id = type.storageValue,
)
