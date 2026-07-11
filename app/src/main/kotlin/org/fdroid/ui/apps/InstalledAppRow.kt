package org.fdroid.ui.apps
/* Copyright (C) 2026 Phillip Ahlgren - CustoneOS Spatial Engine */

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.database.AppIssue
import org.fdroid.database.NoCompatibleSigner
import org.fdroid.database.UpdateInOtherRepo
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.Names
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun InstalledAppRow(
  app: MyInstalledAppItem,
  isSelected: Boolean,
  modifier: Modifier = Modifier,
  appIssue: AppIssue? = null,
) {
  val sharedScope = LocalSharedTransitionScope.current
  val animScope = LocalAnimatedVisibilityScope.current
  val isDark = isSystemInDarkTheme()
  val textColor = if (isDark) Color.White else Color.Black

  Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
    BadgedBox(
      badge = {
        if (appIssue != null)
          BadgeIcon(
            icon = Icons.Filled.Error,
            color = if (appIssue is UpdateInOtherRepo) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.error,
            contentDescription = stringResource(R.string.my_apps_header_apps_with_issue),
          )
      }
    ) {
      var iconModifier = Modifier.size(56.dp).semantics { hideFromAccessibility() }
      
      if (sharedScope != null && animScope != null) {
          with(sharedScope) {
              iconModifier = iconModifier.sharedElement(
                  rememberSharedContentState(key = "myapps_icon_${app.packageName}"),
                  animatedVisibilityScope = animScope
              )
          }
      }

      AsyncShimmerImage(
        model = app.iconModel,
        error = painterResource(R.drawable.ic_repo_app_default),
        contentDescription = null,
        modifier = iconModifier.clip(RoundedCornerShape(12.dp)),
      )
    }
    
    Spacer(modifier = Modifier.width(16.dp))
    
    Column(modifier = Modifier.weight(1f)) {
      Text(text = app.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
      Spacer(modifier = Modifier.height(4.dp))
      Text(text = app.installedVersionName, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
  }
}

@Preview
@Composable
fun InstalledAppRowPreview() {
  val app = InstalledAppItem(packageName = "", name = Names.randomName, installedVersionName = "1.0.1", installedVersionCode = 10001, lastUpdated = System.currentTimeMillis() - 5000)
  FDroidContent { Column { InstalledAppRow(app, false) } }
}
