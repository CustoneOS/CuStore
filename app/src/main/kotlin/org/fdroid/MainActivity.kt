package org.fdroid
/* Copyright (C) 2026 Phillip Ahlgren - Modifications for CustoneOS */

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.view.WindowManager.LayoutParams.FLAG_SECURE
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.Main
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        // Global flag so the deeper Compose screens know we are in Setup Wizard mode
        var isFromSetupWizard = false
    }

    val requestPermissionLauncher = registerForActivityResult(RequestPermission()) {}

    @Inject lateinit var settingsManager: SettingsManager

    private var intentUrl by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            settingsManager.preventScreenshotsFlow.collect { preventScreenshots ->
                if (preventScreenshots) window?.addFlags(FLAG_SECURE) else window?.clearFlags(FLAG_SECURE)
            }
        }

        // Catch the URL and the Setup Wizard flag natively
        if (intent?.action == Intent.ACTION_VIEW) {
            intentUrl = intent?.dataString
        }
        isFromSetupWizard = intent?.getBooleanExtra("from_setup_wizard", false) == true

        enableEdgeToEdge()
        setContent {
            Main(initialUrl = intentUrl) {
                if (intent != null) {
                    onNewIntent(intent)
                    intent = null
                }
            }
        }
        
        if (SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            intentUrl = intent.dataString
        }
        if (intent.hasExtra("from_setup_wizard")) {
            isFromSetupWizard = intent.getBooleanExtra("from_setup_wizard", false)
        }
    }
}
