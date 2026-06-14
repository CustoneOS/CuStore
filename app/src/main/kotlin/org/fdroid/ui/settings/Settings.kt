package org.fdroid.ui.settings
/* Copyright (C) 2026 Phillip Ahlgren - Flawless Toggle State & Native Ripples */

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.provider.Settings.ACTION_APP_LOCALE_SETTINGS
import android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
import android.provider.Settings.EXTRA_APP_PACKAGE
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.rememberPreferenceState
import org.fdroid.R

import org.fdroid.settings.SettingsConstants.AutoUpdateValues.Always
import org.fdroid.settings.SettingsConstants.AutoUpdateValues.Never
import org.fdroid.settings.SettingsConstants.AutoUpdateValues.OnlyWifi
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_THEME
import org.fdroid.settings.SettingsConstants.PREF_KEY_AUTO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_REPO_UPDATES
import org.fdroid.settings.SettingsConstants.PREF_KEY_THEME
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE
import org.fdroid.settings.SettingsConstants.PREF_USE_DNS_CACHE_DEFAULT
import org.fdroid.settings.toAutoUpdateValue

import org.fdroid.ui.bounceClick
import org.fdroid.ui.utils.startActivitySafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Settings(
    model: SettingsModel, 
    openAbout: () -> Unit, // Re-added the navigation callback here
    onSaveLogcat: (Uri?) -> Unit, 
    onBackClicked: () -> Unit
) {
    val context = LocalContext.current
    val res = LocalResources.current
    val isDark = isSystemInDarkTheme()
    
    val bgColor = if (isDark) Color(0xEE000000) else Color(0xEEFFFFFF)
    val textColor = if (isDark) Color.White else Color.Black
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Scaffold(
            containerColor = Color.Transparent, 
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    navigationIcon = { 
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    title = { Text(stringResource(R.string.menu_settings), fontSize = 28.sp, fontWeight = FontWeight.Black, color = textColor) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { paddingValues ->
            ProvidePreferenceLocals(model.prefsFlow) {
                
                val themeState = rememberPreferenceState(PREF_KEY_THEME, PREF_DEFAULT_THEME)
                val autoUpdatesState = rememberPreferenceState(PREF_KEY_AUTO_UPDATES, PREF_DEFAULT_AUTO_UPDATES)
                val repoUpdatesState = rememberPreferenceState(PREF_KEY_REPO_UPDATES, PREF_DEFAULT_REPO_UPDATES)
                val dnsState = rememberPreferenceState(PREF_USE_DNS_CACHE, PREF_USE_DNS_CACHE_DEFAULT)

                var showThemeDialog by remember { mutableStateOf(false) }
                var showAutoInstallDialog by remember { mutableStateOf(false) }
                var showCheckUpdatesDialog by remember { mutableStateOf(false) }

                LazyColumn(
                    modifier = Modifier.padding(paddingValues).fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item {
                        CustoneSettingsCard(title = "General & Appearance") {
                            val themeText = when (themeState.value) {
                                "light" -> res.getString(R.string.theme_light)
                                "dark" -> res.getString(R.string.theme_dark)
                                else -> res.getString(R.string.theme_follow_system)
                            }
                            CustoneSettingRow(title = stringResource(R.string.theme), summary = themeText, icon = Icons.Default.BrightnessMedium, onClick = { showThemeDialog = true })
                            if (SDK_INT >= 33) {
                                CustoneSettingRow(title = stringResource(R.string.pref_language), summary = stringResource(R.string.pref_language_summary), icon = Icons.Default.Translate, onClick = { context.startActivitySafe(Intent(ACTION_APP_LOCALE_SETTINGS).apply { setData(Uri.fromParts("package", context.packageName, null)) }) })
                            }
                            
                            // NEW: Custom About & Licensing button integrated cleanly into your UI theme
                            CustoneSettingRow(
                                title = stringResource(R.string.about_title_full), 
                                summary = "CuStore Source Code, Licensing, and Info", 
                                icon = Icons.Default.Info, 
                                onClick = openAbout
                            )
                        }
                    }

                    item {
                        CustoneSettingsCard(title = "System & Updates") {
                            val autoInstallText = when (autoUpdatesState.value.toAutoUpdateValue()) {
                                OnlyWifi -> res.getString(R.string.pref_auto_updates_only_wifi)
                                Always -> res.getString(R.string.pref_auto_updates_only_always)
                                Never -> res.getString(R.string.pref_auto_updates_only_never)
                            }
                            val checkUpdatesText = when (repoUpdatesState.value.toAutoUpdateValue()) {
                                OnlyWifi -> res.getString(R.string.pref_repo_updates_summary_only_wifi)
                                Always -> res.getString(R.string.pref_repo_updates_summary_always)
                                Never -> res.getString(R.string.pref_repo_updates_summary_never)
                            }

                            CustoneSettingRow(title = "Auto-Install Updates", summary = autoInstallText, icon = Icons.Default.GetApp, onClick = { showAutoInstallDialog = true })
                            CustoneSettingRow(title = "Check for Updates", summary = checkUpdatesText, icon = Icons.Default.Sync, onClick = { showCheckUpdatesDialog = true })

                            if (SDK_INT >= 26) {
                                CustoneSettingRow(title = stringResource(R.string.notification_title), summary = stringResource(R.string.notification_summary), icon = Icons.Default.Notifications, onClick = { context.startActivitySafe(Intent(ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(EXTRA_APP_PACKAGE, context.packageName) }) })
                            }

                            CustoneSwitchRow(title = stringResource(R.string.useDnsCache), summary = stringResource(R.string.useDnsCacheSummary), icon = Icons.Default.Dns, checked = dnsState.value, onCheckedChange = { dnsState.value = it })
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }

                if (showThemeDialog) {
                    CustoneSelectionDialog(title = stringResource(R.string.theme), options = listOf(Pair("followSystem", res.getString(R.string.theme_follow_system)), Pair("light", res.getString(R.string.theme_light)), Pair("dark", res.getString(R.string.theme_dark))), selectedValue = themeState.value, onSelect = { themeState.value = it; showThemeDialog = false }, onDismiss = { showThemeDialog = false })
                }
                if (showAutoInstallDialog) {
                    CustoneSelectionDialog(title = "Auto-Install Updates", options = listOf(Pair(OnlyWifi.name, res.getString(R.string.pref_auto_updates_only_wifi)), Pair(Always.name, res.getString(R.string.pref_auto_updates_only_always)), Pair(Never.name, res.getString(R.string.pref_auto_updates_only_never))), selectedValue = autoUpdatesState.value, onSelect = { autoUpdatesState.value = it; showAutoInstallDialog = false }, onDismiss = { showAutoInstallDialog = false })
                }
                if (showCheckUpdatesDialog) {
                    CustoneSelectionDialog(title = "Check for Updates", options = listOf(Pair(OnlyWifi.name, res.getString(R.string.pref_repo_updates_summary_only_wifi)), Pair(Always.name, res.getString(R.string.pref_repo_updates_summary_always)), Pair(Never.name, res.getString(R.string.pref_repo_updates_summary_never))), selectedValue = repoUpdatesState.value, onSelect = { repoUpdatesState.value = it; showCheckUpdatesDialog = false }, onDismiss = { showCheckUpdatesDialog = false })
                }
            }
        }
    }
}

@Composable
fun CustoneSettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) Color(0xFF1E1E1E).copy(alpha=0.45f) else Color.White.copy(alpha=0.6f)
    val strokeColor = if (isDark) Color.White.copy(alpha=0.1f) else Color.Black.copy(alpha=0.05f)

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Black, color = if (isDark) Color.Gray else Color.DarkGray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 24.dp, bottom = 12.dp))
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cardBg).border(1.dp, strokeColor, RoundedCornerShape(24.dp)).padding(vertical = 8.dp)) {
            content()
        }
    }
}

@Composable
fun CustoneSettingRow(title: String, summary: String, icon: ImageVector, onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .background(if (isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.03f))
            .padding(horizontal = 20.dp, vertical = 16.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = summary, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CustoneSwitchRow(title: String, summary: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val isDark = isSystemInDarkTheme()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .background(if (isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.03f))
            .padding(horizontal = 20.dp, vertical = 12.dp), 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF007AFF), modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = summary, fontSize = 13.sp, color = if (isDark) Color.LightGray else Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(modifier = Modifier.width(12.dp))
        
        Switch(
            checked = checked, 
            onCheckedChange = null, 
            colors = SwitchDefaults.colors(
                disabledCheckedThumbColor = Color.White,
                disabledCheckedTrackColor = Color(0xFF007AFF),
                disabledUncheckedThumbColor = Color.Gray,
                disabledUncheckedTrackColor = Color(0xFF1E1E1E),
                disabledCheckedBorderColor = Color.Transparent,
                disabledUncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun CustoneSelectionDialog(title: String, options: List<Pair<String, String>>, selectedValue: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = if (isDark) Color(0xFF1E1E1E) else Color.White,
        title = { Text(title, fontWeight = FontWeight.Black) },
        text = {
            Column {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelect(option.first) }
                            .background(if (isDark) Color.White.copy(alpha=0.05f) else Color.Black.copy(alpha=0.03f))
                            .padding(vertical = 14.dp, horizontal = 12.dp), 
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (selectedValue == option.first), onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF007AFF)))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(option.second, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold) } }
    )
}
