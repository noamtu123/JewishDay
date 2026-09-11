// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The update currently waiting to be offered, held outside any one view model.
 *
 * The launch check and the developer tools' manual check are run by different view models, in
 * different stores, so a manual check had no way to raise the banner — it could only report what it
 * found and tell you to restart the app. Keeping the pending release here lets either check offer
 * it, and the banner reads the same flow whichever one found it.
 *
 * Dismissing the dialog is "not now", not "never": the release stays here so the banner can keep
 * offering it, and only an actual install (or a check that finds nothing) clears it.
 */
@Singleton
class PendingUpdateStore @Inject constructor() {
    private val _pending = MutableStateFlow<UpdateState.Available?>(null)
    val pending: StateFlow<UpdateState.Available?> = _pending.asStateFlow()

    fun offer(release: AppRelease, isDowngrade: Boolean) {
        _pending.value = UpdateState.Available(release, isDowngrade)
    }

    fun clear() {
        _pending.value = null
    }
}
