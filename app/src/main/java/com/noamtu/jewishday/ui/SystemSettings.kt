// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens this app's page in the system settings, where a permanently denied permission is granted. */
internal fun Context.openAppSettings() =
    openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))

/** Opens the system's location settings, where location services are switched on. */
internal fun Context.openLocationSettings() = openSettings(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))

// Some builds have no such screen; the main settings are the nearest place to find it.
private fun Context.openSettings(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
