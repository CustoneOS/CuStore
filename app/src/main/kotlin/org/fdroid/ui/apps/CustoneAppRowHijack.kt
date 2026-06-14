package org.fdroid.ui.apps
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.fdroid.ui.bounceClick
import org.fdroid.ui.LocalAnimatedVisibilityScope
import org.fdroid.ui.LocalSharedTransitionScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CustoneAppRowHijack(packageName: String, onClick: () -> Unit, content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current
    var boxModifier = Modifier.fillMaxWidth().bounceClick { onClick() }

    // 🚨 SPATIAL EXPANSION ENGINE INJECTED 🚨
    if (sharedScope != null && animScope != null) {
        with(sharedScope) {
            boxModifier = boxModifier.sharedBounds(
                rememberSharedContentState(key = "myapps_bounds_${packageName}"),
                animatedVisibilityScope = animScope,
                renderInOverlayDuringTransition = false
            )
        }
    }
    
    boxModifier = boxModifier.background(if (isDark) Color(0xFF1E1E1E).copy(alpha=0.35f) else Color.White.copy(alpha=0.4f), RoundedCornerShape(16.dp)).border(1.dp, if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f), RoundedCornerShape(16.dp)).padding(horizontal = 4.dp, vertical = 2.dp)

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent, surfaceVariant = Color.Transparent, background = Color.Transparent)) { content() }
    }
}
