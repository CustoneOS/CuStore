package org.fdroid.ui.details
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fdroid.R
import org.fdroid.install.InstallState
import org.fdroid.ui.ClickyButton
import org.fdroid.ui.utils.AsyncShimmerImage
import org.fdroid.ui.utils.startActivitySafe
import org.fdroid.ui.LocalSharedTransitionScope
import org.fdroid.ui.LocalAnimatedVisibilityScope

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppDetailsHeader(item: AppDetailsItem, innerPadding: PaddingValues, originPrefix: String = "discover_") {
    val context = LocalContext.current
    val sharedScope = LocalSharedTransitionScope.current
    val animScope = LocalAnimatedVisibilityScope.current

    val columnModifier = Modifier.fillMaxWidth().padding(top = innerPadding.calculateTopPadding() + 16.dp, bottom = 16.dp).padding(horizontal = 24.dp)

    Column(modifier = columnModifier) {
        
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            var iconModifier: Modifier = Modifier.size(84.dp)
            if (sharedScope != null && animScope != null) {
                with(sharedScope) {
                    iconModifier = iconModifier.sharedElement(rememberSharedContentState(key = "${originPrefix}icon_${item.app.packageName}"), animatedVisibilityScope = animScope)
                }
            }
            iconModifier = iconModifier.clip(RoundedCornerShape(20.dp))

            AsyncShimmerImage(model = item.icon, contentDescription = item.name, contentScale = ContentScale.Crop, modifier = iconModifier)
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(text = item.name, fontSize = 24.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                item.app.authorName?.let { authorName -> Text(text = authorName, fontSize = 14.sp, color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp)) }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        AnimatedVisibility(visible = item.mainButtonState == MainButtonState.PROGRESS) {
            var targetProgressFraction = 0f
            var targetDownloadedBytes = 0L
            var targetTotalBytes = 1L
            
            val state = item.installState
            if (state != null && state.javaClass.simpleName.contains("Download", true)) {
                try {
                    val fields = state.javaClass.declaredFields
                    val progField = fields.find { it.name.contains("progress", true) || it.name.contains("byte", true) }
                    val totField = fields.find { it.name.contains("total", true) || it.name.contains("max", true) }
                    
                    progField?.isAccessible = true
                    totField?.isAccessible = true
                    
                    targetDownloadedBytes = (progField?.get(state) as? Number)?.toLong() ?: 0L
                    targetTotalBytes = (totField?.get(state) as? Number)?.toLong()?.takeIf { it > 0 } ?: 1L
                    
                    if (targetDownloadedBytes > 0) {
                        targetProgressFraction = targetDownloadedBytes.toFloat() / targetTotalBytes.toFloat()
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }

            val animatedProgress by animateFloatAsState(targetValue = targetProgressFraction, animationSpec = tween(600, easing = LinearEasing), label = "bar")
            val animatedBytes by animateFloatAsState(targetValue = targetDownloadedBytes.toFloat(), animationSpec = tween(600, easing = LinearEasing), label = "text")

            val mbDown = String.format("%.2f", animatedBytes / 1048576f)
            val mbTotal = String.format("%.2f", targetTotalBytes / 1048576f)
            val pct = (animatedProgress * 100).toInt().coerceIn(0, 100)
            
            val progressText = if (targetDownloadedBytes > 0) "Downloading • $pct% ($mbDown MB / $mbTotal MB)" else "Preparing installation..."

            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = progressText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.fillMaxWidth(animatedProgress.coerceAtLeast(0.01f)).height(8.dp).background(Color(0xFF007AFF), RoundedCornerShape(4.dp)))
                }
            }
        }

        AnimatedVisibility(
            visible = item.mainButtonState == MainButtonState.INSTALL || item.mainButtonState == MainButtonState.UPDATE || (item.showOpenButton && item.mainButtonState != MainButtonState.PROGRESS)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (item.mainButtonState != MainButtonState.NONE) {
                    val btnText = if (item.mainButtonState == MainButtonState.INSTALL) "Install" else "Update"
                    ClickyButton(
                        text = btnText, isPrimary = true, modifier = Modifier.weight(2f).height(48.dp),
                        onClick = { require(item.suggestedVersion != null) { "suggestedVersion was null" }; item.actions.installAction(item.app, item.suggestedVersion, item.icon) }
                    )
                }
                
                if (item.showOpenButton) {
                    ClickyButton(
                        text = stringResource(R.string.menu_open), isPrimary = item.mainButtonState == MainButtonState.NONE, 
                        modifier = Modifier.weight(1f).height(48.dp), onClick = { val i = context.packageManager.getLaunchIntentForPackage(item.app.packageName); if (i != null) context.startActivity(i) else android.widget.Toast.makeText(context, "App cannot be opened directly", android.widget.Toast.LENGTH_SHORT).show() }
                    )
                }
            }
        }
    }
}
