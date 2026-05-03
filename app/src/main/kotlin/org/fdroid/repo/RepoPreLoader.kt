package org.fdroid.repo
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import android.content.Context
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.InitialRepository
import org.fdroid.database.RepositoryDao

@Singleton
class RepoPreLoader @Inject constructor(@param:ApplicationContext private val context: Context) {

  private val lenientJson = Json { ignoreUnknownKeys = true }

  @get:WorkerThread
  val defaultRepoAddresses: Set<String> by lazy { getDefaultRepos().map { it.address }.toSet() }

  @WorkerThread
  @OptIn(ExperimentalSerializationApi::class)
  fun addPreloadedRepositories(db: FDroidDatabase) {
    addRepositories(db.getRepositoryDao(), getDefaultRepos())
  }

  @WorkerThread
  @OptIn(ExperimentalSerializationApi::class)
  private fun getDefaultRepos(): List<DefaultRepository> {
    return context.assets.open("default_repos.json").use { inputStream ->
      lenientJson.decodeFromStream<List<DefaultRepository>>(inputStream)
    }
  }

  private fun addRepositories(repositoryDao: RepositoryDao, repositories: List<DefaultRepository>) {
    repositories.forEach { repository ->
      val initialRepository =
        InitialRepository(
          name = repository.name,
          address = repository.address,
          mirrors = repository.mirrors,
          description = repository.description,
          certificate = repository.certificate,
          version = 1,
          enabled = repository.enabled,
        )
      repositoryDao.insert(initialRepository)
    }
  }
}

@Serializable
data class DefaultRepository(
  val name: String,
  val address: String,
  val mirrors: List<String> = emptyList(),
  val description: String,
  val certificate: String = "",
  val enabled: Boolean = true,
)
