package org.fdroid.ui.apps
/* Copyright (C) 2026 Phillip Ahlgren - CustoneOS Spatial Engine */

import android.text.format.Formatter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.ui.FDroidContent
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BadgeIcon
import org.fdroid.ui.utils.ExpandIconArrow
import org.fdroid.ui.utils.getPreviewVersion
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun UpdatableAppRow(app: AppUpdateItem, isSelected: Boolean, modifier: Modifier = Modifier) {
  var isExpanded by remember { mutableStateOf(false) }
  val sharedScope = LocalSharedTransitionScope.current
  val animScope = LocalAnimatedVisibilityScope.current
  val isDark = isSystemInDarkTheme()
  val textColor = if (isDark) Color.White else Color.Black

  Column(modifier = modifier) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
      BadgedBox(
        badge = {
          BadgeIcon(
            icon = Icons.Filled.NewReleases,
            color = MaterialTheme.colorScheme.secondary,
            contentDescription = stringResource(R.string.notification_title_single_update_available),
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
        VersionLine(app)
      }
      
      if (app.whatsNew != null) {
        IconButton(onClick = { isExpanded = !isExpanded }) { ExpandIconArrow(isExpanded) }
      }
    }
    
    AnimatedVisibility(
      visible = isExpanded,
      modifier = Modifier.padding(top = 12.dp).semantics { liveRegion = LiveRegionMode.Polite },
    ) {
      Card(modifier = Modifier.fillMaxWidth()) {
        Text(text = app.whatsNew ?: "", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(12.dp))
      }
    }
  }
}

@Composable
fun VersionLine(app: AppUpdateItem) {
  val size = app.update.size?.let { Formatter.formatFileSize(LocalContext.current, it) }
  val test = buildAnnotatedString {
    if (LocalLayoutDirection.current == LayoutDirection.Ltr) { append(app.installedVersionName) } else { append("\u202A${app.update.versionName}\u202C") }
    appendInlineContent("arrowId", " → ")
    if (LocalLayoutDirection.current == LayoutDirection.Ltr) { append("\u202A${app.update.versionName}\u202C") } else { append("\u202A${app.installedVersionName}\u202C") }
    append(" • $size")
  }
  val inlineContent = mapOf(
    Pair("arrowId", InlineTextContent(Placeholder(width = 24.sp, height = 20.sp, placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter)) {
        Icon(Icons.AutoMirrored.Default.ArrowRightAlt, contentDescription = null, modifier = Modifier.padding(horizontal = 2.dp), tint = Color.Gray)
      }
    )
  )
  Text(text = test, inlineContent = inlineContent, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Preview
@Composable
fun UpdatableAppRowPreview() {
  val app1 = AppUpdateItem(repoId = 1, packageName = "A", name = "App Update 123", installedVersionName = "1.0.1", update = getPreviewVersion("1.1.0", 123456789), whatsNew = "New stuff.")
  FDroidContent { Column { UpdatableAppRow(app1, false) } }
}

@Preview(locale = "fa")
@Composable
private fun UpdatableAppRowRtl() {
  val app1 = AppUpdateItem(repoId = 1, packageName = "A", name = "App Update 123", installedVersionName = "1.0.1-alpha", update = getPreviewVersion("1.1.0-beta", 123456789), whatsNew = "New stuff.")
  FDroidContent { Column { UpdatableAppRow(app1, false) } }
}
