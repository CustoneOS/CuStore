package org.fdroid.ui.lists
/* Copyright (C) 2026 Phillip Ahlgren - CustoneOS Prefix Logic */

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import org.fdroid.ui.bounceClick
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope

@Composable
fun AppList(
  appListInfo: AppListInfo,
  currentPackageName: String?,
  modifier: Modifier = Modifier,
  onBackClicked: () -> Unit,
  onItemClick: (String) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black
    val bgColor = if (isDark) Color(0xEE000000) else Color(0xEEFFFFFF)

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Column(Modifier.fillMaxSize().padding(top = 48.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBackClicked, modifier = Modifier.background(Color.Transparent)) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = textColor)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(appListInfo.list.title, fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor)
            }
            
            val apps = appListInfo.model.apps
            if (apps == null) {
                BigLoadingIndicator()
            } else {
                val listState = rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()

                val letters = remember(apps) {
                    apps.mapIndexedNotNull { index, app ->
                        val letter = app.name.firstOrNull()?.uppercaseChar()?.takeIf { it.isLetter() } ?: '#'
                        Pair(letter, index)
                    }.distinctBy { it.first }
                }

                Row(Modifier.fillMaxSize()) {
                    if (apps.size > 15) {
                        Column(
                            modifier = Modifier.width(36.dp).fillMaxHeight().padding(vertical = 16.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            letters.forEach { (letter, index) ->
                                Text(
                                    text = letter.toString(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.Gray else Color.DarkGray,
                                    modifier = Modifier.padding(2.dp).clickable { coroutineScope.launch { listState.animateScrollToItem(index) } }
                                )
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(start = 8.dp, end = 16.dp, bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp), 
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        itemsIndexed(apps, key = { _, item -> item.packageName }) { _, navItem ->
                            CustoneAppListItemRouted(item = navItem, onClick = { onItemClick(navItem.packageName) })
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CustoneAppListItemRouted(item: AppListItem, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    var rowModifier = Modifier.fillMaxWidth().height(88.dp).bounceClick { onClick() }
    
    // 🚨 SPATIAL EXPANSION ENGINE INJECTED 🚨
    if (sharedScope != null && animScope != null) {
        with(sharedScope) { 
            rowModifier = rowModifier.sharedBounds(
                rememberSharedContentState(key = "list_bounds_${item.packageName}"), 
                animatedVisibilityScope = animScope,
                renderInOverlayDuringTransition = false
            ) 
        }
    }
    
    rowModifier = rowModifier
        .background(if (isDark) Color(0xFF1E1E1E).copy(alpha=0.35f) else Color.White.copy(alpha=0.4f), RoundedCornerShape(16.dp))
        .border(1.dp, if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f), RoundedCornerShape(16.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
        var iconModifier: Modifier = Modifier.size(56.dp)
        
        if (sharedScope != null && animScope != null) {
            with(sharedScope) { 
                iconModifier = iconModifier.sharedElement(
                    rememberSharedContentState(key = "list_icon_${item.packageName}"), 
                    animatedVisibilityScope = animScope
                ) 
            }
        }
        
        AsyncShimmerImage(model = item.iconModel, contentDescription = item.name, modifier = iconModifier.clip(RoundedCornerShape(12.dp)))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = item.packageName, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(12.dp))
        val statusBg = if (item.isInstalled) Color.Gray.copy(alpha=0.2f) else Color(0xFF007AFF).copy(alpha=0.15f)
        Text(
            text = if (item.isInstalled) "Installed" else "Install", 
            fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (item.isInstalled) Color.Gray else Color(0xFF007AFF), 
            modifier = Modifier.background(statusBg, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}
