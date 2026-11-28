// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * A home-screen widget's request to open the zmanim screen on [date] — the Shabbat widget's on that
 * Shabbat or chag, the others on today. Deliberately not a data class: each tap is its own request,
 * even for the same day, so the screen acts on it once.
 */
@Immutable
class ShowDayRequest(val date: LocalDate)
