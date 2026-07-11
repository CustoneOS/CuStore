package org.fdroid.ui.repositories.details

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_TEXT
import io.ktor.client.engine.ProxyConfig
import org.fdroid.R
import org.fdroid.database.Repository
import org.fdroid.download.Mirror
import org.fdroid.download.NetworkState
import org.fdroid.repo.RepoUpdateProgress
import org.fdroid.repo.RepoUpdateState
import org.fdroid.ui.utils.flagEmoji
import org.fdroid.ui.utils.startActivitySafe

interface RepoDetailsInfo {
  val model: RepoDetailsModel
  val actions: RepoDetailsActions
}

interface RepoDetailsActions {
  fun deleteRepository()
  fun updateUsernameAndPassword(username: String, password: String)
  fun setMirrorEnabled(mirror: Mirror, enabled: Boolean)
  fun deleteUserMirror(mirror: Mirror)
  fun setArchiveRepoEnabled(enabled: Boolean)
  fun onOnboardingSeen()
}

data class RepoDetailsModel(
  val repo: Repository?,
  val numberApps: Int?,
  val officialMirrors: List<OfficialMirrorItem>,
  val userMirrors: List<UserMirrorItem>,
  val archiveState: ArchiveState,
  val showOnboarding: Boolean,
  val updateState: RepoUpdateState?,
  val networkState: NetworkState,
  val proxy: ProxyConfig?,
) {
  val showOfficialMirrors: Boolean = officialMirrors.size >= 2
  val showUserMirrors: Boolean = userMirrors.isNotEmpty()
  val isUpdateButtonEnabled: Boolean = repo?.enabled == true && updateState !is RepoUpdateProgress

  fun shareRepo(context: Context) {
    require(repo != null) { "repo was null when sharing it" }
    val intent = Intent(ACTION_SEND).apply {
        type = "text/plain"
        putExtra(EXTRA_TEXT, repo.address) // 🚨 Explicitly use raw address, NOT F-Droid URI
    }
    val chooserTitle = context.getString(R.string.share_repository)
    context.startActivitySafe(Intent.createChooser(intent, chooserTitle))
  }
}

data class OfficialMirrorItem(
  val mirror: Mirror,
  val isEnabled: Boolean,
  val isRepoAddress: Boolean,
) : MirrorItem(mirror.baseUrl), Comparable<OfficialMirrorItem> {
  private val isOnion = mirror.isOnion()
  val emoji: String = if (isOnion) "🧅" else if (mirror.countryCode == null) { if (isRepoAddress) "⭐" else "" } else mirror.countryCode?.flagEmoji ?: ""

  override fun compareTo(other: OfficialMirrorItem): Int {
    return if (isRepoAddress && !other.isRepoAddress) -1
    else if (!isRepoAddress && other.isRepoAddress) 1
    else if (isOnion && !other.isOnion) 1
    else if (!isOnion && other.isOnion) -1
    else if (isOnion) mirror.baseUrl.compareTo(other.mirror.baseUrl)
    else if (mirror.countryCode == other.mirror.countryCode) mirror.baseUrl.compareTo(other.mirror.baseUrl)
    else {
      val countryCode = mirror.countryCode ?: ""
      val otherCountryCode = other.mirror.countryCode ?: ""
      countryCode.compareTo(otherCountryCode)
    }
  }
}

data class UserMirrorItem(val mirror: Mirror, val isEnabled: Boolean) : MirrorItem(mirror.baseUrl) {
  fun share(context: Context, fingerprint: String) {
    val uri = mirror.baseUrl // 🚨 Explicitly use raw address, NOT F-Droid URI
    val intent = Intent(ACTION_SEND).apply {
        type = "text/plain"
        putExtra(EXTRA_TEXT, uri)
    }
    context.startActivitySafe(Intent.createChooser(intent, context.getString(R.string.share_mirror)))
  }
}

abstract class MirrorItem(baseUrl: String) {
  val url: String = baseUrl.removePrefix("https://").removePrefix("http://").removeSuffix("/fdroid/repo").removeSuffix("/repo").removeSuffix("/")
}

enum class ArchiveState { ENABLED, DISABLED, LOADING, UNKNOWN }
