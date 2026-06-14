package org.fdroid.ui.details
/* Copyright (C) 2026 Phillip Ahlgren - Glitch-Free Transitions */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.asRelativeTimeString
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
fun AppDetails(
  item: AppDetailsItem?,
  originPrefix: String = "discover_",
  onNav: (NavigationKey) -> Unit,
  onBackNav: (() -> Unit)?,
  modifier: Modifier = Modifier,
  packageName: String = item?.app?.packageName ?: "", // 🚨 Fallback default injected to satisfy AppDetailsEntry 🚨
) {
  val topAppBarState = rememberTopAppBarState()
  var showInstallError by remember { mutableStateOf(false) }
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
  val isDark = isSystemInDarkTheme()
  val textColor = if (isDark) Color.White else Color.Black

  val sharedScope = LocalSharedTransitionScope.current
  val animScope = LocalAnimatedVisibilityScope.current
  var rootModifier = modifier.fillMaxSize()

  if (sharedScope != null && animScope != null && packageName.isNotEmpty()) {
      with(sharedScope) {
          rootModifier = rootModifier.sharedBounds(
              rememberSharedContentState(key = "${originPrefix}bounds_${packageName}"),
              animatedVisibilityScope = animScope,
              renderInOverlayDuringTransition = false 
          )
      }
  }

  val expandedBg = if (isDark) Color(0xFF1E1E1E) else Color.White
  
  Box(modifier = rootModifier.background(expandedBg)) {
      if (item == null) {
          BigLoadingIndicator()
      } else {
        Scaffold(
          modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection), 
          topBar = { AppDetailsTopAppBar(item, topAppBarState, scrollBehavior, onBackNav) },
          bottomBar = {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
              ) {
                  val vName = item.suggestedVersion?.versionName ?: "Unknown"
                  val vCode = item.suggestedVersion?.versionCode ?: "0"
                  val date = item.app.lastUpdated.asRelativeTimeString()

                  Text("Version $vName ($vCode)", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                  Spacer(modifier = Modifier.height(2.dp))
                  Text("Updated $date", fontSize = 11.sp, color = Color.Gray.copy(alpha = 0.7f))
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(item.app.packageName, fontSize = 10.sp, color = Color.Gray.copy(alpha = 0.4f), fontFamily = FontFamily.Monospace)
              }
          },
          containerColor = Color.Transparent, 
        ) { innerPadding ->
          LaunchedEffect(item.installState) {
            val state = item.installState
            if (state is InstallState.UserConfirmationNeeded) item.actions.requestUserConfirmation(state)
            else if (state is InstallState.Error) showInstallError = true
          }
          
          val scrollState = rememberScrollState()
          var size by remember { mutableStateOf(IntSize.Zero) }
          
          Column(
            modifier = Modifier.verticalScroll(scrollState).fillMaxWidth().padding(bottom = innerPadding.calculateBottomPadding() + 80.dp).onGloballyPositioned { coordinates -> size = coordinates.size }
          ) {
            AppDetailsHeader(item, innerPadding, originPrefix)
            AnimatedVisibility(item.showWarnings) { AppDetailsWarnings(item, Modifier.padding(horizontal = 16.dp)) }

            if (item.installedVersionCode != null && (item.whatsNew != null || item.app.changelog != null)) {
              val cardBg = if (isDark) Color(0xFF1E1E1E).copy(alpha=0.4f) else Color.White.copy(alpha=0.5f)
              ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
              ) {
                Text(text = stringResource(R.string.whats_new_title), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
                if (item.whatsNew != null) SelectionContainer { Text(text = item.whatsNew, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) }
                else if (item.app.changelog != null) {
                  Text(text = buildAnnotatedString { withLink(LinkAnnotation.Url(item.app.changelog!!)) { append(item.app.changelog) } }, modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp))
                }
              }
            }

            item.description?.let { description ->
              val maxLines = 4
              val textMeasurer = rememberTextMeasurer()
              val allowExpand = remember(size.width, description) { textMeasurer.measure(text = description, constraints = Constraints.fixedWidth(size.width)).lineCount > maxLines }
              var descriptionExpanded by remember(allowExpand) { mutableStateOf(!allowExpand) }
              val htmlDescription = AnnotatedString.fromHtml(description)
              
              AnimatedVisibility(visible = descriptionExpanded, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
                SelectionContainer { Text(text = htmlDescription, color = textColor.copy(alpha=0.9f), modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) }
              }
              if (allowExpand) {
                AnimatedVisibility(!descriptionExpanded) { Text(text = htmlDescription, maxLines = maxLines, color = textColor.copy(alpha=0.9f), overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 16.dp)) }
                TextButton(onClick = { descriptionExpanded = !descriptionExpanded }, modifier = Modifier.padding(start = 12.dp)) { Text(text = if (descriptionExpanded) stringResource(R.string.less) else stringResource(R.string.more), color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) }
              }
            }
            if (!item.antiFeatures.isNullOrEmpty()) AntiFeatures(item.antiFeatures)
            if (item.phoneScreenshots.isNotEmpty()) Screenshots(item.networkState.isMetered, item.phoneScreenshots)
          }
        }
      }
  }

  if (showInstallError && item != null && item.installState is InstallState.Error)
    AlertDialog(onDismissRequest = { showInstallError = false }, title = { Text(stringResource(R.string.install_error_notify_title, item.name)) }, text = { Text(item.installState.msg ?: stringResource(R.string.app_details_install_error_text)) }, confirmButton = { TextButton(onClick = { showInstallError = false }) { Text(stringResource(R.string.ok)) } })
}
