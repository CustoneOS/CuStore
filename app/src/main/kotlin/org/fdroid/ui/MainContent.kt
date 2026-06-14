package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import com.viktormykhailiv.compose.hints.HintHost
import org.fdroid.ui.navigation.MainNavKey

@Composable
fun MainContent(
  isBigScreen: Boolean,
  dynamicColors: Boolean,
  showBottomBar: Boolean,
  currentNavKey: NavKey,
  numUpdates: Int,
  hasAppIssues: Boolean,
  onNav: (MainNavKey) -> Unit,
  content: @Composable (Modifier) -> Unit,
) =
  FDroidContent(dynamicColors = dynamicColors) {
    HintHost {
      // 🚨 REMOVED PADDING: THE APP NOW DRAWS EDGE-TO-EDGE 🚨
      Scaffold { _ ->
        content(Modifier.fillMaxSize())
      }
    }
  }
