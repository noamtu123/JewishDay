// SPDX-License-Identifier: GPL-3.0-or-later

package com.noamtu.jewishday.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Receives the installer's progress. The first thing it sends back is usually a request to show the
 * system's confirmation dialog, which has to be launched from here as a new task.
 */
@AndroidEntryPoint
class ApkInstallResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var installResults: InstallResults


    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ActionInstallResult) return
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = intent.confirmationIntent()
                    ?: return installResults.publish(InstallOutcome.Failed(null))
                // Android refuses activity launches from the background, so if the app is not in
                // front — the user is still in Settings, say — this dialog never appears. Reporting
                // it is the difference between an error and a screen that sits there forever.
                runCatching { context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    .onFailure { error -> installResults.publish(InstallOutcome.Failed(error.message)) }
            }

            PackageInstaller.STATUS_SUCCESS -> installResults.publish(InstallOutcome.Succeeded)

            else -> installResults.publish(
                InstallOutcome.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)),
            )
        }
    }

    private fun Intent.confirmationIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_INTENT)
        }

    companion object {
        const val ActionInstallResult = "com.noamtu.jewishday.INSTALL_RESULT"
    }
}
