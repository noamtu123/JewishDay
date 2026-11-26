// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

import androidx.compose.runtime.Immutable

/**
 * One of Hebcal's daily-learning readings, as Hebcal sent it: its track ([category], e.g. "dafyomi"),
 * its English [title], and the optional Hebrew and memo fields. Worded by `CalendarText.Hebcal`.
 */
@Immutable
public data class HebcalLearningEntry(
    val category: String,
    val title: String,
    val hebrew: String? = null,
    val memo: String? = null,
)
