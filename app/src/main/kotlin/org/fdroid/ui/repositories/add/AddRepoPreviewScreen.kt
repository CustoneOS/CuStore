package org.fdroid.ui.repositories.add
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import android.annotation.SuppressLint
import android.content.Context
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import org.fdroid.repo.FetchResult.*
import org.fdroid.repo.Fetching
import org.fdroid.ui.ClickyButton
import org.fdroid.ui.repositories.RepoIcon

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
fun AddRepoPreviewScreen(
    state: Fetching,
    proxyConfig: ProxyConfig?,
    modifier: Modifier = Modifier,
    onAddRepo: () -> Unit,
    onExistingRepo: (Long) -> Unit,
) {
    val context = LocalContext.current
    val localeList = LocaleListCompat.getDefault()
    val repo = state.receivedRepo ?: error("repo was null")
    val isDark = isSystemInDarkTheme()
    val textColor = if (isDark) Color.White else Color.Black

    val isNew = state.fetchResult is IsNewRepository || state.fetchResult is IsNewRepoAndNewMirror || state.fetchResult is IsNewMirror
    val buttonText = if (isNew) "Add Account" else "Open Account"
    
    val buttonAction: () -> Unit = when (val res = state.fetchResult) {
        is IsNewRepository, is IsNewRepoAndNewMirror, is IsNewMirror -> onAddRepo
        is IsExistingRepository -> { { onExistingRepo(res.existingRepoId) } }
        is IsExistingMirror -> { { onExistingRepo(res.existingRepoId) } }
        else -> error("Unexpected fetch state: ${state.fetchResult}")
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val cardBg = if (isDark) Color(0xFF1E1E1E).copy(alpha=0.35f) else Color.White.copy(alpha=0.4f)
        val borderColor = if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f)

        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(cardBg, RoundedCornerShape(32.dp))
                .border(1.dp, borderColor, RoundedCornerShape(32.dp))
                .padding(32.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                
                RepoIcon(
                    repo = repo, 
                    proxy = proxyConfig, // Fixed parameter name here!
                    modifier = Modifier.size(120.dp).clip(RoundedCornerShape(24.dp))
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(text = repo.getName(localeList) ?: "Unknown Repository", fontSize = 24.sp, fontWeight = FontWeight.Black, color = textColor, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = repo.address.replaceFirst("https://", ""), fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                
                Spacer(modifier = Modifier.height(24.dp))

                val imei = remember { getDeviceImei(context) }
                Text(text = "Device ID", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF007AFF))
                Text(text = imei, fontSize = 14.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)

                Spacer(modifier = Modifier.height(32.dp))

                ClickyButton(
                    text = buttonText,
                    isPrimary = true,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    onClick = buttonAction
                )
            }
        }
        
        Spacer(modifier = Modifier.height(80.dp))
    }
}
