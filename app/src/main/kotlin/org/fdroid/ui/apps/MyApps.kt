package org.fdroid.ui.apps
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle.State.STARTED
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation3.runtime.NavKey
import org.fdroid.R
import org.fdroid.install.InstallConfirmationState
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.search.TopSearchBar
import org.fdroid.ui.utils.BigLoadingIndicator

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
fun MyApps(
  myAppsInfo: MyAppsInfo,
  currentPackageName: String?,
  onAppItemClick: (String) -> Unit,
  onNav: (NavKey) -> Unit,
  modifier: Modifier = Modifier,
) {
  val myAppsModel = myAppsInfo.model
  val isDark = isSystemInDarkTheme()
  val bgColor = if (isDark) Color(0xEE000000) else Color(0xEEFFFFFF)
  val textColor = if (isDark) Color.White else Color.Black

  val lifecycleOwner = LocalLifecycleOwner.current
  LaunchedEffect(myAppsModel.appToConfirm) {
    val app = myAppsModel.appToConfirm
    if (app != null && lifecycleOwner.lifecycle.currentState.isAtLeast(STARTED)) {
      val state = app.installState as InstallConfirmationState
      myAppsInfo.actions.confirmAppInstall(app.packageName, state)
    }
  }
  
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
  var searchActive by rememberSaveable { mutableStateOf(false) }
  val onSearchCleared = { myAppsInfo.actions.search("") }
  
  val onBackPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher

  BackHandler(enabled = searchActive) {
    searchActive = false
    onSearchCleared()
  }
  
  Box(modifier = modifier.fillMaxSize().background(bgColor)) {
      Scaffold(
        topBar = {
          if (searchActive) {
            TopSearchBar(onSearch = myAppsInfo.actions::search, onSearchCleared = onSearchCleared) {
              onBackPressedDispatcher?.onBackPressed()
            }
          } else
            TopAppBar(
              navigationIcon = { 
                  IconButton(onClick = { onBackPressedDispatcher?.onBackPressed() }) {
                      Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                  }
              },
              title = { Text("My Apps", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor) },
              actions = {
                  IconButton(onClick = { searchActive = true }) {
                      Icon(Icons.Default.Search, contentDescription = "Search")
                  }
              },
              colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
              scrollBehavior = scrollBehavior,
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      ) { paddingValues ->
        if (myAppsModel.appUpdates == null && myAppsModel.installedApps == null) BigLoadingIndicator()
        else if (myAppsModel.installingApps.isEmpty() && myAppsModel.appUpdates.isNullOrEmpty() && myAppsModel.appsWithIssue.isNullOrEmpty() && myAppsModel.installedApps.isNullOrEmpty()) {
          Text(
            text = if (searchActive) stringResource(R.string.search_my_apps_no_results) else "No Apps Installed",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(paddingValues).fillMaxSize().padding(16.dp),
          )
        } else {
          MyAppsList(
            myAppsInfo = myAppsInfo,
            currentPackageName = currentPackageName,
            onAppItemClick = onAppItemClick,
            paddingValues = paddingValues,
          )
        }
      }
  }
}
