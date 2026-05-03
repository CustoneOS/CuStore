package org.fdroid.ui.repositories.add
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.telephony.TelephonyManager
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.download.NetworkMonitor
import org.fdroid.index.RepoManager
import org.fdroid.repo.AddRepoError
import org.fdroid.repo.AddRepoState
import org.fdroid.repo.Added
import org.fdroid.repo.Adding
import org.fdroid.repo.Fetching
import org.fdroid.repo.None
import org.fdroid.settings.SettingsManager
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher

@HiltViewModel
class AddRepoViewModel
@Inject
constructor(
  app: Application,
  networkMonitor: NetworkMonitor,
  settingsManager: SettingsManager,
  private val repoManager: RepoManager,
  private val updateManager: UpdatesManager,
  @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

  private val log = KotlinLogging.logger {}
  val state: StateFlow<AddRepoState> = repoManager.addRepoState

  val proxyConfig = settingsManager.proxyConfig
  val networkState = networkMonitor.networkState

  override fun onCleared() {
    log.info { "onCleared() abort adding repository" }
    repoManager.abortAddingRepository()
  }

  fun onFetchRepo(uriStr: String) {
    // 1. THE OEM LOCK: Instantly kill any unauthorized repository attempts
    if (!uriStr.contains("custoneos.com")) {
        log.warn { "SECURITY BLOCK: Attempted to add unauthorized repo: $uriStr" }
        return // This stops the entire function immediately!
    }

    // 2. Get the IMEI
    val telephonyManager = getApplication<Application>().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    @SuppressLint("MissingPermission", "HardwareIds")
    val hardwareImei = try { telephonyManager.imei ?: "UNKNOWN" } catch (e: Exception) { "DENIED" }

    // 3. The Path Injection
    var cleanUri = uriStr
    if (!cleanUri.startsWith("http")) cleanUri = "https://$cleanUri"
    if (cleanUri.endsWith("/")) cleanUri = cleanUri.dropLast(1)
    
    cleanUri = cleanUri.replace(Regex("([?&])imei=[^&]*"), "")
    cleanUri = cleanUri.replace(Regex("/imei-[a-zA-Z0-9]+"), "")
    
    val hackedUriStr = "$cleanUri/imei-$hardwareImei"

    log.info { "onFetchRepo($hackedUriStr)" }
    ioScope.launch {
      repoManager.fetchRepositoryPreview(hackedUriStr) 
    }
  }

  fun addFetchedRepository() {
    log.info { "addFetchedRepository()" }
    ioScope.launch {
      repoManager.addFetchedRepository()
      // wait for repo to get added, so we can load updates afterward
      repoManager.addRepoState.collect {
        when (it) {
          is Fetching,
          Adding,
          None -> {}
          is Added -> {
            updateManager.loadUpdates().join()
            cancel()
          }
          is AddRepoError -> cancel()
        }
      }
    }
  }
}
