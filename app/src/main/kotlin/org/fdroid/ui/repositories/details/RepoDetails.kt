package org.fdroid.ui.repositories.details
/* Copyright (C) 2026 Phillip Ahlgren - CustoneOS Polish */

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.telephony.TelephonyManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import org.fdroid.R
import org.fdroid.repo.RepoUpdateWorker
import org.fdroid.ui.ClickyButton
import org.fdroid.ui.utils.BigLoadingIndicator
import org.fdroid.ui.utils.MeteredConnectionDialog
import org.fdroid.ui.utils.asRelativeTimeString

@SuppressLint("HardwareIds", "MissingPermission")
fun getDeviceImei(context: Context): String {
    return try {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        telephonyManager.imei ?: "IMEI Not Found"
    } catch (e: Exception) {
        "Unavailable"
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RepoDetails(
  info: RepoDetailsInfo,
  onShowAppsClicked: (String, Long) -> Unit,
  onBackNav: (() -> Unit)?,
) {
  val context = LocalContext.current
  val repo = info.model.repo
  val isDark = isSystemInDarkTheme()
  val localeList = LocaleListCompat.getDefault()

  val bgColor = if (isDark) Color(0xEE000000) else Color(0xEEFFFFFF)
  val textColor = if (isDark) Color.White else Color.Black
  val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

  var showMeteredDialog by remember { mutableStateOf<(() -> Unit)?>(null) }
  val meteredLambda = showMeteredDialog
  if (meteredLambda != null) MeteredConnectionDialog(numBytes = null, onConfirm = { meteredLambda() }, onDismiss = { showMeteredDialog = null })

  if (repo == null) BigLoadingIndicator()
  else {
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Scaffold(
          modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
          topBar = {
            TopAppBar(
              navigationIcon = { 
                  if (onBackNav != null) {
                      IconButton(onClick = onBackNav) {
                          Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                      }
                  }
              },
              title = { Text("Account", fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor) },
              actions = {
                
                // 🚨 KINETIC SYNC BUTTON 🚨
                var refreshRotation by remember { mutableFloatStateOf(0f) }
                val rotation by animateFloatAsState(targetValue = refreshRotation, animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing), label = "spin")

                IconButton(
                    onClick = { 
                        refreshRotation += 360f
                        if (info.model.networkState.isMetered) showMeteredDialog = { RepoUpdateWorker.updateNow(context, repo.repoId) }
                        else RepoUpdateWorker.updateNow(context, repo.repoId)
                    },
                    enabled = info.model.isUpdateButtonEnabled
                ) {
                    Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.repo_force_update), tint = textColor, modifier = Modifier.graphicsLayer { rotationZ = rotation })
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
              scrollBehavior = scrollBehavior
            )
          },
          bottomBar = {
              Column(
                  modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp),
                  horizontalAlignment = Alignment.CenterHorizontally
              ) {
                  Text("Updated ${repo.timestamp.asRelativeTimeString()}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
              }
          },
          containerColor = Color.Transparent
        ) { paddingValues ->
            
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val cardBg = if (isDark) Color(0xFF1E1E1E).copy(alpha=0.35f) else Color.White.copy(alpha=0.4f)
                val borderColor = if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f)

                Box(
                    modifier = Modifier.fillMaxWidth(0.85f).background(cardBg, RoundedCornerShape(32.dp)).border(1.dp, borderColor, RoundedCornerShape(32.dp)).padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        
                        var qrCodeBitmap by remember(repo.address) { mutableStateOf<android.graphics.Bitmap?>(null) }
                        
                        LaunchedEffect(repo.address) { 
                            qrCodeBitmap = info.actions.generateQrCode(repo) 
                        }
                        
                        if (qrCodeBitmap != null) {
                            Image(
                                bitmap = qrCodeBitmap!!.asImageBitmap(), contentDescription = "Device Linked Account",
                                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(Color.White).padding(8.dp)
                            )
                        } else {
                            Box(modifier = Modifier.size(120.dp).background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(24.dp)))
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        Text(text = "Primary Account", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        val imei = remember { getDeviceImei(context) }
                        Text(text = "Device ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                        Text(text = imei, fontSize = 14.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                        Spacer(modifier = Modifier.height(32.dp))

                        ClickyButton(
                            text = "Browse Linked Apps", isPrimary = true, modifier = Modifier.fillMaxWidth().height(48.dp),
                            onClick = { onShowAppsClicked(repo.getName(localeList) ?: "Unknown Repository", repo.repoId) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
  }
}
