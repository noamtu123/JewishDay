// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Whether the app may post notifications. Always true below Android 13, where POST_NOTIFICATIONS is
 * granted implicitly at install time and there is no runtime prompt to show.
 */
fun Context.hasNotificationPermission(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

/** True when a runtime prompt for POST_NOTIFICATIONS is possible on this OS version. */
val notificationPermissionIsRuntime: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
