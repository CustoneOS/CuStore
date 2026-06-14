package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Expedited Boot Interceptor */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*

class CustoneBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<CustoneProvisionWorker>()
                .setConstraints(constraints)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST) // 🚨 FORCES OS TO LAUNCH FROM BACKGROUND
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "CustoneProvisioningTask",
                ExistingWorkPolicy.REPLACE, // 🚨 Kills any stuck overnight zombie tasks
                request
            )
        }
    }
}
