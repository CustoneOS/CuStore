package org.fdroid.ui.discover
/* Copyright (C) 2026 Phillip Ahlgren - Isolated Parallax & Anti-Flash Engine */

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.delay
import org.fdroid.R
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.ui.CustoneLayeredMist
import org.fdroid.ui.bounceClick
import org.fdroid.ui.lists.*
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope
import java.util.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoverContent(
  discoverModel: LoadedDiscoverModel,
  onListTap: (AppListType) -> Unit,
  onAppTap: (AppDiscoverItem) -> Unit,
  onNav: (NavKey) -> Unit,
  scrollState: LazyListState,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val isDark = isSystemInDarkTheme()
  val textColor = if (isDark) Color.White else Color.Black

  val prefs = remember { context.getSharedPreferences("custone_store_prefs", Context.MODE_PRIVATE) }
  var isCinematic by remember { mutableStateOf(prefs.getBoolean("is_first_dashboard_load", false)) }
  val cinematicAnim = remember { Animatable(if (isCinematic) 0f else 1f) }

  LaunchedEffect(isCinematic) {
      if (isCinematic) {
          delay(400)
          cinematicAnim.animateTo(1f, tween(7500, easing = LinearOutSlowInEasing))
          prefs.edit().putBoolean("is_first_dashboard_load", false).apply()
          isCinematic = false
      }
  }

  val cp = cinematicAnim.value
  
  val uiAlpha = if (isCinematic) { if (cp < 0.65f) 0f else ((cp - 0.65f) / 0.35f).coerceIn(0f, 1f) } else 1f
  val vibrantAlpha = if (isCinematic) { if (cp < 0.1f) 0f else if (cp < 0.4f) (cp - 0.1f) / 0.3f else 1f - ((cp - 0.4f) / 0.6f) } else 0f

  Box(modifier = Modifier.fillMaxSize()) {
      
      CustoneLayeredMist(isOverlay = false)

      if (isCinematic && vibrantAlpha > 0f) {
          Box(modifier = Modifier.fillMaxSize().alpha(vibrantAlpha).background(Brush.radialGradient(listOf(Color(0xFF00E5FF).copy(alpha=0.6f), Color(0xFFFF00FF).copy(alpha=0.6f), Color.Transparent))))
      }

      val insets = WindowInsets.systemBars.asPaddingValues()

      LazyColumn(
          state = scrollState, 
          modifier = Modifier.fillMaxSize(), 
          contentPadding = PaddingValues(
              top = insets.calculateTopPadding() + 48.dp, 
              bottom = insets.calculateBottomPadding() + 120.dp
          )
      ) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp).graphicsLayer(alpha = uiAlpha), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "CuStore", fontSize = 32.sp, fontWeight = FontWeight.Black, color = textColor, letterSpacing = 1.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    var refreshRotation by remember { mutableFloatStateOf(0f) }
                    val rotation by animateFloatAsState(targetValue = refreshRotation, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "spin")
                    IconButton(onClick = { 
                        refreshRotation += 360f
                        try { RepoUpdateWorker.updateNow(context.applicationContext as Application, 1L); Toast.makeText(context, "Checking CuStore...", Toast.LENGTH_SHORT).show() } catch (e: Exception) {} 
                    }) { 
                        Icon(Icons.Filled.Sync, contentDescription = "Refresh", tint = textColor, modifier = Modifier.graphicsLayer { rotationZ = rotation }) 
                    }
                    IconButton(onClick = { onNav(NavigationKey.MyApps) }) { Icon(Icons.Filled.Apps, contentDescription = "My Apps", tint = textColor) }
                    IconButton(onClick = { onNav(NavigationKey.RepoDetails(1L)) }) { Icon(Icons.Filled.AccountCircle, contentDescription = "Account", tint = textColor) }
                    IconButton(onClick = { onNav(NavigationKey.Settings) }) { Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = textColor) }
                }
            }
        }
        item { 
            Box(modifier = Modifier.graphicsLayer(alpha = uiAlpha)) { 
                CustoneSearchBarPlaceholder(onClick = { onNav(NavigationKey.Search) }) 
            } 
        }

        var tileIndexCount = 0
        if (discoverModel.newApps.isNotEmpty()) {
          item { val listType = AppListType.New(stringResource(R.string.app_list_new)); CustoneAppGrid(title = listType.title, apps = discoverModel.newApps, startIndex = tileIndexCount, cp = cp, uiAlpha = uiAlpha, isCinematic = isCinematic, onTitleTap = { onListTap(listType) }, onAppTap = onAppTap); tileIndexCount += 6 }
        }
        if (!discoverModel.recentlyUpdatedApps.isEmpty()) {
          item { val listType = AppListType.RecentlyUpdated(stringResource(R.string.app_list_recently_updated)); CustoneAppGrid(title = listType.title, apps = discoverModel.recentlyUpdatedApps, startIndex = tileIndexCount, cp = cp, uiAlpha = uiAlpha, isCinematic = isCinematic, onTitleTap = { onListTap(listType) }, onAppTap = onAppTap); tileIndexCount += 6 }
        }
        if (!discoverModel.mostDownloadedApps.isNullOrEmpty()) {
          item { val listType = AppListType.MostDownloaded(stringResource(R.string.app_list_most_downloaded)); CustoneAppGrid(title = listType.title, apps = discoverModel.mostDownloadedApps, startIndex = tileIndexCount, cp = cp, uiAlpha = uiAlpha, isCinematic = isCinematic, onTitleTap = { onListTap(listType) }, onAppTap = onAppTap); tileIndexCount += 6 }
        }
      }

      if (isCinematic && cp < 1f) {
          val fogAlpha = 1f - (cp / 0.4f).coerceIn(0f, 1f)
          if (fogAlpha > 0f) {
              Box(modifier = Modifier.fillMaxSize().alpha(fogAlpha).blur(20.dp).background(if (isDark) Color.Black else Color.White))
          }
          
          Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
              awaitPointerEventScope { while (true) { awaitPointerEvent().changes.forEach { it.consume() } } }
          })
      }

      if (isCinematic && cp >= 0.70f) {
          val flashPhase = ((cp - 0.70f) / 0.30f).coerceIn(0f, 1f)
          val bloomScale = 1f + (flashPhase * 80f) 
          val bloomAlpha = when {
              flashPhase < 0.4f -> flashPhase / 0.4f
              flashPhase > 0.6f -> 1f - ((flashPhase - 0.6f) / 0.4f)
              else -> 1f
          }

          Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
              Box(modifier = Modifier.size(60.dp).scale(bloomScale).alpha(bloomAlpha).blur(20.dp).background(Color.White, CircleShape))
              Box(modifier = Modifier.fillMaxSize().alpha(bloomAlpha * 0.85f).background(Color.White))
          }
      }
  }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CustoneSearchBarPlaceholder(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    var boxModifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    if (sharedScope != null && animScope != null) {
        with(sharedScope) { boxModifier = boxModifier.sharedBounds(rememberSharedContentState(key = "search_bar_bounds"), animatedVisibilityScope = animScope) }
    }
    Row(modifier = boxModifier.bounceClick { onClick() }.background(if (isDark) Color(0xFF1E1E1E).copy(alpha=0.4f) else Color.White.copy(alpha=0.5f), RoundedCornerShape(24.dp)).border(1.dp, if (isDark) Color.White.copy(alpha=0.15f) else Color.Black.copy(alpha=0.1f), RoundedCornerShape(24.dp)).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = if (isDark) Color.White else Color.Black)
        Spacer(modifier = Modifier.width(12.dp))
        Text("Search CuStore...", fontSize = 16.sp, color = if (isDark) Color.LightGray else Color.DarkGray)
    }
}

@Composable
fun CustoneAppGrid(title: String, apps: List<AppDiscoverItem>, startIndex: Int, cp: Float, uiAlpha: Float, isCinematic: Boolean, onTitleTap: () -> Unit, onAppTap: (AppDiscoverItem) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().graphicsLayer(alpha = uiAlpha).bounceClick { onTitleTap() }.padding(vertical = 14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = if (isSystemInDarkTheme()) Color.White else Color.Black)
            Text(text = "See All >", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF007AFF))
        }
        val chunkedApps = apps.take(6).chunked(2)
        chunkedApps.forEachIndexed { rowIndex, rowApps ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowApps.forEachIndexed { colIndex, app -> 
                    val tileIndex = startIndex + (rowIndex * 2) + colIndex
                    CustoneAppCard(app = app, index = tileIndex, cp = cp, isCinematic = isCinematic, modifier = Modifier.weight(1f), onClick = { onAppTap(app) }) 
                }
                if (rowApps.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CustoneAppCard(app: AppDiscoverItem, index: Int, cp: Float, isCinematic: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    val randomEngine = remember { Random(app.packageName.hashCode().toLong()) }
    val signX = remember { if (randomEngine.nextBoolean()) 1f else -1f }
    val signY = remember { if (randomEngine.nextBoolean()) 1f else -1f }
    val randomOffsetX = remember { signX * (1500f + randomEngine.nextFloat() * 1500f) }
    val randomOffsetY = remember { signY * (1500f + randomEngine.nextFloat() * 1500f) }
    val randomAngle = remember { (randomEngine.nextInt(91) - 45).toFloat() } 

    val startTrigger = remember { randomEngine.nextFloat() * 0.2f }
    val duration = remember { 0.35f + randomEngine.nextFloat() * 0.15f } 
    
    val tileP = ((cp - startTrigger) / duration).coerceIn(0f, 1f)
    val floatyBezier = remember { CubicBezierEasing(0.05f, 0.95f, 0.1f, 1f) }
    val easedP = floatyBezier.transform(tileP)

    val tileScale = if (isCinematic) 1f + 1.2f * (1f - easedP) else 1f
    val tileRotation = if (isCinematic) randomAngle * (1f - easedP) else 0f
    
    // 🚨 ANTI-FLASH ENGINE: Hard 0f alpha until local startTrigger hits 🚨
    val tileAlpha = if (isCinematic) { if (cp <= startTrigger) 0f else easedP } else 1f
    val textAlpha = if (isCinematic) { if (cp < 0.85f) 0f else ((cp - 0.85f) / 0.15f).coerceIn(0f, 1f) } else 1f

    var boxModifier = modifier.height(145.dp).bounceClick { onClick() }
        .graphicsLayer {
            scaleX = tileScale
            scaleY = tileScale
            rotationZ = tileRotation
            translationX = if (isCinematic) randomOffsetX * (1f - easedP) else 0f
            translationY = if (isCinematic) randomOffsetY * (1f - easedP) else 0f
            alpha = tileAlpha
        }
    
    // 🚨 DISABLING SHARED BOUNDS DURING CINEMATIC ENTRY PREVENTS GHOST FLASHING 🚨
    if (sharedScope != null && animScope != null && !isCinematic) {
        with(sharedScope) { 
            boxModifier = boxModifier.sharedBounds(
                rememberSharedContentState(key = "discover_bounds_${app.packageName}"), 
                animatedVisibilityScope = animScope,
                renderInOverlayDuringTransition = false 
            ) 
        }
    }
    
    boxModifier = boxModifier.background(if (isDark) Color(0xFF1E1E1E).copy(alpha=0.25f) else Color.White.copy(alpha=0.3f), RoundedCornerShape(20.dp)).border(1.dp, if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f), RoundedCornerShape(20.dp)).padding(14.dp)

    Box(modifier = boxModifier) {
        Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            var iconModifier: Modifier = Modifier.size(52.dp)
            if (sharedScope != null && animScope != null && !isCinematic) {
                with(sharedScope) { iconModifier = iconModifier.sharedElement(rememberSharedContentState(key = "discover_icon_${app.packageName}"), animatedVisibilityScope = animScope) }
            }
            AsyncShimmerImage(model = app.imageModel, contentDescription = app.name, modifier = iconModifier.clip(RoundedCornerShape(12.dp)))
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(text = app.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha=textAlpha) else Color.Black.copy(alpha=textAlpha), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            
            val statusBg = if (app.isInstalled) Color.Gray.copy(alpha=0.2f) else Color(0xFF007AFF).copy(alpha=0.15f)
            
            Box(
                modifier = Modifier.background(statusBg, RoundedCornerShape(8.dp)).padding(start = 12.dp, end = 12.dp, top = 1.dp, bottom = 6.dp).alpha(textAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (app.isInstalled) "Installed" else "Install", 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = (if (app.isInstalled) Color.Gray else Color(0xFF007AFF)).copy(alpha=textAlpha)
                )
            }
        }
    }
}
