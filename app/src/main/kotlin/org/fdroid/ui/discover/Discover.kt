package org.fdroid.ui.discover
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.ui.lists.AppListType
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
fun Discover(
  discoverModel: DiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
  scrollState: LazyListState = rememberLazyListState()
) {
  Scaffold(
    modifier = modifier,
    containerColor = Color.Transparent, 
  ) { paddingValues ->
    when (discoverModel) {
      is FirstStartDiscoverModel ->
        FirstStart(
          networkState = discoverModel.networkState,
          repoUpdateState = discoverModel.repoUpdateState,
          modifier = Modifier.fillMaxSize().padding(paddingValues),
        )
      is LoadingDiscoverModel -> BigLoadingIndicator(Modifier.padding(paddingValues))
      is LoadedDiscoverModel -> {
        
        // 🚨 ADDED SAFE CALL TO mostDownloadedApps 🚨
        val newPkgs = discoverModel.newApps.map { it.packageName }.toSet()
        val uniqueUpdated = discoverModel.recentlyUpdatedApps.filterNot { it.packageName in newPkgs }
        val updatedPkgs = uniqueUpdated.map { it.packageName }.toSet()
        val uniqueDownloaded = discoverModel.mostDownloadedApps?.filterNot { it.packageName in newPkgs || it.packageName in updatedPkgs }

        val curatedModel = LoadedDiscoverModel(
            recentlyUpdatedApps = uniqueUpdated,
            newApps = discoverModel.newApps,
            mostDownloadedApps = uniqueDownloaded,
            categories = emptyMap(), 
            searchTextFieldState = discoverModel.searchTextFieldState,
            hasRepoIssues = discoverModel.hasRepoIssues
        )

        DiscoverContent(
          discoverModel = curatedModel,
          onListTap = onListTap,
          onAppTap = onAppTap,
          onNav = onNav,
          scrollState = scrollState,
          modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        )
      }
      NoEnabledReposDiscoverModel -> {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(paddingValues)) {
          Text(text = stringResource(R.string.no_repos_enabled), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp), color = Color.Gray)
        }
      }
    }
  }
}
