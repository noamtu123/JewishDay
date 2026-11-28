// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.core.model

/**
 * The day's portion of the monthly Tehillim division: whole chapters, or (on the 25th and 26th) half
 * of chapter 119 by verse. Worded by each language's formatter — see `CalendarText.TehillimYomi`.
 */
internal data class TehillimRange(
    val chapterStart: Int,
    val chapterEnd: Int,
    val verseStart: Int? = null,
    val verseEnd: Int? = null,
)

internal fun tehillimRangeForDay(dayOfMonth: Int, daysInMonth: Int): TehillimRange = when (dayOfMonth) {
    1 -> TehillimRange(1, 9)
    2 -> TehillimRange(10, 17)
    3 -> TehillimRange(18, 22)
    4 -> TehillimRange(23, 28)
    5 -> TehillimRange(29, 34)
    6 -> TehillimRange(35, 38)
    7 -> TehillimRange(39, 43)
    8 -> TehillimRange(44, 48)
    9 -> TehillimRange(49, 54)
    10 -> TehillimRange(55, 59)
    11 -> TehillimRange(60, 65)
    12 -> TehillimRange(66, 68)
    13 -> TehillimRange(69, 71)
    14 -> TehillimRange(72, 76)
    15 -> TehillimRange(77, 78)
    16 -> TehillimRange(79, 82)
    17 -> TehillimRange(83, 87)
    18 -> TehillimRange(88, 89)
    19 -> TehillimRange(90, 96)
    20 -> TehillimRange(97, 103)
    21 -> TehillimRange(104, 105)
    22 -> TehillimRange(106, 107)
    23 -> TehillimRange(108, 112)
    24 -> TehillimRange(113, 118)
    25 -> TehillimRange(119, 119, 1, 96)
    26 -> TehillimRange(119, 119, 97, 176)
    27 -> TehillimRange(120, 134)
    28 -> TehillimRange(135, 139)
    29 -> if (daysInMonth == 29) TehillimRange(140, 150) else TehillimRange(140, 144)
    else -> TehillimRange(145, 150)
}
