package org.fdroid.ui.apps
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.ui.ClickyButton
import org.fdroid.ui.utils.MeteredConnectionDialog

@Composable
fun MyAppsList(
  myAppsInfo: MyAppsInfo,
  currentPackageName: String?,
  onAppItemClick: (String) -> Unit,
  paddingValues: PaddingValues,
  modifier: Modifier = Modifier,
) {
  val updatableApps = myAppsInfo.model.appUpdates
  val installingApps = myAppsInfo.model.installingApps
  val installedApps = myAppsInfo.model.installedApps
  val lazyListState = rememberLazyListState()
  
  var showMeteredDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
  var showUpdateAllButton by remember(updatableApps) { mutableStateOf(true) }

  LazyColumn(
    state = lazyListState,
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp, top = paddingValues.calculateTopPadding()),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    modifier = modifier.fillMaxSize()
  ) {
    
    if (!updatableApps.isNullOrEmpty()) {
      item(key = "header_updates") {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
          Text(text = "Updates Available", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
          Spacer(modifier = Modifier.weight(1f))
          if (showUpdateAllButton) {
            ClickyButton(
                text = "Update All", isPrimary = true, modifier = Modifier.height(36.dp),
                onClick = {
                  val installLambda = { myAppsInfo.actions.updateAll(); showUpdateAllButton = false }
                  if (myAppsInfo.model.networkState.isMetered) showMeteredDialog = installLambda else installLambda()
                }
            )
          }
        }
      }
      
      items(items = updatableApps, key = { it.packageName }) { app ->
        val onClick = { onAppItemClick(app.packageName) }
        CustoneAppRowHijack(packageName = app.packageName, onClick = onClick) { UpdatableAppRow(app, false, Modifier.fillMaxWidth()) }
      }
    }

    if (installingApps.isNotEmpty()) {
      item(key = "header_installing") { Text(text = "Installing", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
      items(items = installingApps, key = { it.packageName }) { app ->
        val onClick = { onAppItemClick(app.packageName) }
        CustoneAppRowHijack(packageName = app.packageName, onClick = onClick) { InstallingAppRow(app, false, Modifier.fillMaxWidth()) }
      }
    }

    val hasContentAbove = installingApps.isNotEmpty() || !updatableApps.isNullOrEmpty()
    if (hasContentAbove && !installedApps.isNullOrEmpty()) {
      item(key = "header_installed") { Text(text = "Installed Apps", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) }
    }
    
    if (installedApps != null) {
      items(items = installedApps, key = { it.packageName }) { app ->
        val onClick = { onAppItemClick(app.packageName) }
        CustoneAppRowHijack(packageName = app.packageName, onClick = onClick) { InstalledAppRow(app, false, Modifier.fillMaxWidth(), null) }
      }
    }
  }

  val meteredLambda = showMeteredDialog
  if (meteredLambda != null) MeteredConnectionDialog(numBytes = myAppsInfo.model.appUpdatesBytes, onConfirm = { meteredLambda() }, onDismiss = { showMeteredDialog = null })
}
