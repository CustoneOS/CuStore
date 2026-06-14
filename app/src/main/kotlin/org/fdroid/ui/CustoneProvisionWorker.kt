package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Coroutine Asynchronous Engine */

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.fdroid.database.AppMetadata
import org.fdroid.database.AppVersion
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.install.AppInstallManager
import org.fdroid.repo.RepoUpdateWorker
import kotlin.coroutines.resume

// 🚨 DEADLOCK BYPASS: Yields the thread so Room can fetch the DB query, then resumes
suspend fun <T> LiveData<T>.awaitValue(): T? = withContext(Dispatchers.Main.immediate) {
    val currentValue = this@awaitValue.value
    if (currentValue != null && (currentValue !is List<*> || currentValue.isNotEmpty())) {
        return@withContext currentValue
    }
    suspendCancellableCoroutine { cont ->
        val observer = object : Observer<T> {
            override fun onChanged(value: T) {
                if (value != null && (value !is List<*> || (value as List<*>).isNotEmpty())) {
                    this@awaitValue.removeObserver(this)
                    if (cont.isActive) cont.resume(value)
                }
            }
        }
        this@awaitValue.observeForever(observer)
        cont.invokeOnCancellation { this@awaitValue.removeObserver(observer) }
    }
}

@HiltWorker
class CustoneProvisionWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: FDroidDatabase,
    private val appInstallManager: AppInstallManager
) : CoroutineWorker(appContext, workerParams) {

    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "provisioning_channel"

    private fun createForegroundInfo(message: String): ForegroundInfo {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "System Provisioning",
                NotificationManager.IMPORTANCE_MAX
            ).apply { description = "Installs initial system applications" }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("CuStore Zero-Touch Setup")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .build()

        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("custone_store_prefs", Context.MODE_PRIVATE)
        
        if (prefs.getBoolean("is_provisioned", false)) return Result.success()
        if (!prefs.getBoolean("is_account_linked", false)) return Result.retry()

        try { setForeground(createForegroundInfo("Fetching Decentralized Catalogs...")) } catch (e: Exception) {}

        try {
            val repos = database.getRepositoryDao().getRepositories()
            for (repo in repos) {
                try { RepoUpdateWorker.updateNow(applicationContext as Application, repo.repoId) } catch (e: Exception) {}
            }
        } catch (e: Exception) {}

        val pushedPackages = mutableSetOf<String>()
        val startTime = System.currentTimeMillis()
        val durationLimit = 20 * 60 * 1000L 

        while (System.currentTimeMillis() - startTime < durationLimit) { 
            delay(10_000) 

            try {
                val allApps = database.getAppDao().getAllApps()
                val targetAppItems = allApps.filter { 
                    (it.packageName.contains("fossify", ignoreCase = true) || 
                     it.packageName.contains("google", ignoreCase = true)) &&
                    !pushedPackages.contains(it.packageName) 
                }

                if (targetAppItems.isNotEmpty()) {
                    try { setForeground(createForegroundInfo("Pushing ${targetAppItems.size} apps to installer...")) } catch (e: Exception) {}

                    for (appItem in targetAppItems) {
                        if (System.currentTimeMillis() - startTime >= durationLimit) break

                        var targetVersion: AppVersion? = null
                        var targetRepo: Repository? = null
                        var targetMetadata: AppMetadata? = null

                        val repoIds = database.getAppDao().getRepositoryIdsForApp(appItem.packageName)
                        
                        for (rId in repoIds) {
                            try {
                                val versionsRaw: Any? = database.getVersionDao().getAppVersions(rId, appItem.packageName)
                                
                                if (versionsRaw is LiveData<*>) {
                                    val list = (versionsRaw as LiveData<*>).awaitValue() as? List<*>
                                    targetVersion = list?.firstOrNull() as? AppVersion
                                } else if (versionsRaw is List<*>) {
                                    targetVersion = versionsRaw.firstOrNull() as? AppVersion
                                }

                                if (targetVersion != null) {
                                    val wrapper = database.getAppDao().getApp(rId, appItem.packageName)
                                    if (wrapper != null) {
                                        targetRepo = database.getRepositoryDao().getRepository(rId)
                                        targetMetadata = wrapper.metadata
                                        break
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("CustoneProvision", "Database read error: ${e.message}")
                            }
                        }
                        
                        if (targetVersion != null && targetRepo != null && targetMetadata != null) {
                            Log.e("CustoneProvision", "✅ INSTALLING: ${appItem.packageName} (v${targetVersion.versionCode}) from Repo ${targetRepo.repoId}")
                            appInstallManager.install(
                                appMetadata = targetMetadata,
                                version = targetVersion,
                                currentVersionName = null,
                                repo = targetRepo,
                                iconModel = null,
                                canAskPreApprovalNow = false
                            )
                            pushedPackages.add(appItem.packageName)
                            delay(1500) 
                        } else {
                             Log.e("CustoneProvision", "⏳ WAITING: Room hasn't hydrated AppVersion for ${appItem.packageName} yet.")
                        }
                    }
                    try { setForeground(createForegroundInfo("Awaiting further index unpacking...")) } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                Log.e("CustoneProvision", "Sweep crash: ${e.message}")
            }
        }

        prefs.edit().putBoolean("is_provisioned", true).apply()
        return Result.success()
    }
}
