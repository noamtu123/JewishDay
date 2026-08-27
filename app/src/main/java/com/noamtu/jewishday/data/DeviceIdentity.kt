// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.data

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifies the phone the app is running on, so settings arriving from a backup can be told apart
 * from settings that were already here.
 *
 * ANDROID_ID is scoped to this app's signing key on this device and this user, needs no permission,
 * and is not an advertising or tracking identifier — it never leaves the device, and is only ever
 * compared with the copy stored in the settings.
 *
 * True of every install marker: it changes on a factory reset, which is treated as a new device.
 */
interface DeviceIdentity {
    /** Null when the platform will not say, in which case nothing is judged. */
    val id: String?
}

@Singleton
class AndroidDeviceIdentity @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdentity {
    override val id: String? by lazy { readAndroidId() }

    @SuppressLint("HardwareIds")
    private fun readAndroidId(): String? = runCatching {
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }.getOrNull()?.takeIf { it.isNotBlank() }
}
