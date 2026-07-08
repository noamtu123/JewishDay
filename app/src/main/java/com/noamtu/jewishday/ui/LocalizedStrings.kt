// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.res.stringResource

val LocalUseHebrewInterface = staticCompositionLocalOf { false }

@Composable
fun localizedString(
    @StringRes englishRes: Int,
    @StringRes hebrewRes: Int,
    vararg formatArgs: Any,
): String {
    val resId = if (LocalUseHebrewInterface.current) hebrewRes else englishRes
    return if (formatArgs.isEmpty()) stringResource(resId) else stringResource(resId, *formatArgs)
}