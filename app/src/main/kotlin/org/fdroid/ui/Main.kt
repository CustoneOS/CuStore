package org.fdroid.ui
/* Copyright (C) 2026 Phillip Ahlgren - Opaque Mist & Account Link Bypass */

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer

import org.fdroid.settings.SettingsConstants.PREF_DEFAULT_DYNAMIC_COLORS
import org.fdroid.ui.discover.*
import org.fdroid.ui.apps.*
import org.fdroid.ui.details.*
import org.fdroid.ui.lists.*
import org.fdroid.ui.repositories.*
import org.fdroid.ui.repositories.add.*
import org.fdroid.ui.repositories.details.*
import org.fdroid.ui.history.*
import org.fdroid.ui.search.*
import org.fdroid.ui.settings.*
import org.fdroid.ui.navigation.IntentRouter
import org.fdroid.ui.navigation.MainNavKey
import org.fdroid.ui.navigation.NavigationKey
import org.fdroid.ui.navigation.Navigator
import org.fdroid.ui.navigation.rememberNavigationState
import org.fdroid.ui.navigation.topLevelRoutes

@kotlin.OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalSharedTransitionApi::class)
@Composable
fun Main(initialUrl: String? = null, onListeningForIntent: () -> Unit = {}) {
  val navigationState = rememberNavigationState(startRoute = NavigationKey.Discover, topLevelRoutes = topLevelRoutes)
  val navigator = remember { Navigator(navigationState) }
  val context = LocalContext.current
  val activity = (LocalActivity.current as ComponentActivity)
  val coroutineScope = rememberCoroutineScope()

  SideEffect {
      WindowCompat.setDecorFitsSystemWindows(activity.window, false)
      activity.window.statusBarColor = android.graphics.Color.TRANSPARENT
      activity.window.navigationBarColor = android.graphics.Color.TRANSPARENT
      activity.window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
  }

  val isLauncherIntent = activity.intent?.action == Intent.ACTION_MAIN && activity.intent?.hasCategory(Intent.CATEGORY_LAUNCHER) == true
  val prefs = remember { context.getSharedPreferences("custone_store_prefs", Context.MODE_PRIVATE) }
  val isFirstLaunch = remember { prefs.getBoolean("first_launch_v4", true) }
  
  // 🚨 SMART ACCOUNT ROUTING MEMORY 🚨
  val isAccountLinked = remember { prefs.getBoolean("is_account_linked", false) }
  var setupState by rememberSaveable { mutableStateOf(if (isFirstLaunch && isLauncherIntent) "WELCOME" else "DONE") }
  var capturedRepoUrl by rememberSaveable { mutableStateOf("") }
  var cameFromSetup by rememberSaveable { mutableStateOf(false) }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
      if (isGranted) setupState = "CAMERA_VIEW"
      else android.widget.Toast.makeText(context, "Camera permission required to scan profile.", android.widget.Toast.LENGTH_SHORT).show()
  }

  LaunchedEffect(initialUrl) { 
      if (!initialUrl.isNullOrBlank()) {
          // If setup wizard passed an intent, flag account as linked
          prefs.edit().putBoolean("is_account_linked", true).apply()
          navigator.navigate(NavigationKey.AddRepo(uri = initialUrl)) 
      }
  }
  
  DisposableEffect(navigator) {
    val intentListener = IntentRouter(navigator)
    activity.addOnNewIntentListener(intentListener)
    onListeningForIntent()
    onDispose { activity.removeOnNewIntentListener(intentListener) }
  }
  
  val windowAdaptiveInfo = currentWindowAdaptiveInfo()
  val directive = remember(windowAdaptiveInfo) { calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(horizontalPartitionSpacerSize = 2.dp) }
  val isBigScreen = directive.maxHorizontalPartitions > 1
  val showBottomBar = !isBigScreen && navigator.last is MainNavKey && setupState == "DONE"
  val viewModel = hiltViewModel<MainViewModel>()
  val dynamicColors = viewModel.dynamicColors.collectAsStateWithLifecycle(PREF_DEFAULT_DYNAMIC_COLORS).value
  val numUpdates = viewModel.numUpdates.collectAsStateWithLifecycle().value
  val hasAppIssues = viewModel.hasAppIssues.collectAsStateWithLifecycle(false).value

  val discoverScrollState = rememberLazyListState()

  MainContent(
    isBigScreen = isBigScreen, dynamicColors = dynamicColors, showBottomBar = showBottomBar,
    currentNavKey = navigationState.topLevelRoute, numUpdates = numUpdates, hasAppIssues = hasAppIssues,
    onNav = { navKey -> navigator.navigate(navKey) },
  ) { _ -> 

    BackHandler(enabled = navigator.last != NavigationKey.Discover && setupState == "DONE") { navigator.goBack() }
    BackHandler(enabled = setupState == "CAMERA_VIEW") { setupState = "QR_SCAN" }
    BackHandler(enabled = setupState == "QR_SCAN") { setupState = "WELCOME" }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                val sharedScope = this
                AnimatedContent(
                    targetState = navigator.last ?: NavigationKey.Discover,
                    transitionSpec = {
                        val floatTween = tween<Float>(550, easing = FastOutSlowInEasing)
                        val offsetTween = tween<IntOffset>(500, easing = FastOutSlowInEasing)

                        if (targetState is NavigationKey.AppDetails || initialState is NavigationKey.AppDetails) {
                            val isEntering = targetState is NavigationKey.AppDetails
                            if (isEntering) {
                                fadeIn(animationSpec = floatTween) togetherWith scaleOut(targetScale = 0.93f, animationSpec = floatTween).plus(fadeOut(animationSpec = floatTween))
                            } else {
                                scaleIn(initialScale = 0.93f, animationSpec = floatTween).plus(fadeIn(animationSpec = floatTween)) togetherWith fadeOut(animationSpec = floatTween)
                            }
                        } else {
                            val stepOrder = listOf(NavigationKey.Discover, NavigationKey.MyApps, NavigationKey.Settings, NavigationKey.About)
                            val targetIndex = stepOrder.indexOf(targetState).takeIf { it != -1 } ?: 99
                            val initialIndex = stepOrder.indexOf(initialState).takeIf { it != -1 } ?: 99
                            val isBackward = targetIndex < initialIndex || targetState == NavigationKey.Discover || (initialState is NavigationKey.Search && targetState == NavigationKey.Discover)

                            if (isBackward) {
                                slideInVertically(initialOffsetY = { -it }, animationSpec = offsetTween).plus(fadeIn(animationSpec = floatTween)) togetherWith slideOutHorizontally(targetOffsetX = { it }, animationSpec = offsetTween).plus(scaleOut(targetScale = 0.85f, animationSpec = floatTween)).plus(fadeOut(animationSpec = floatTween))
                            } else {
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = offsetTween).plus(scaleIn(initialScale = 0.85f, animationSpec = floatTween)).plus(fadeIn(animationSpec = floatTween)) togetherWith slideOutVertically(targetOffsetY = { -it }, animationSpec = offsetTween).plus(fadeOut(animationSpec = floatTween))
                            }
                        }
                    },
                    label = "CustoneRouter"
                ) { targetKey ->
                    CompositionLocalProvider(LocalSharedTransitionScope provides sharedScope, LocalAnimatedVisibilityScope provides this@AnimatedContent) {
                        when (targetKey) {
                            is NavigationKey.Discover -> {
                                val vModel = hiltViewModel<DiscoverViewModel>()
                                Discover(discoverModel = vModel.discoverModel.collectAsStateWithLifecycle().value, scrollState = discoverScrollState, onListTap = { navigator.navigate(NavigationKey.AppList(it)) }, onAppTap = { app -> val new = NavigationKey.AppDetails(app.packageName); if (navigator.last is NavigationKey.AppDetails) navigator.replaceLast(new) else navigator.navigate(new) }, onNav = { navigator.navigate(it) })
                            }
                            is NavigationKey.AppDetails -> {
                                val vModel = hiltViewModel<AppDetailsViewModel, AppDetailsViewModel.Factory>(key = targetKey.packageName, creationCallback = { it.create(targetKey.packageName) })
                                val otherScreen = if (this@AnimatedContent.transition.targetState is NavigationKey.AppDetails) this@AnimatedContent.transition.currentState else this@AnimatedContent.transition.targetState
                                val originPrefix = when (otherScreen) { is NavigationKey.Discover -> "discover_"; is NavigationKey.AppList -> "list_"; is NavigationKey.MyApps -> "myapps_"; else -> "discover_" }
                                AppDetails(packageName = targetKey.packageName, item = vModel.appDetails.collectAsStateWithLifecycle().value, originPrefix = originPrefix, onNav = { navigator.navigate(it) }, onBackNav = if (isBigScreen) null else { { navigator.goBack() } })
                            }
                            is NavigationKey.MyApps -> {
                                val vModel = hiltViewModel<MyAppsViewModel>()
                                val info = object : MyAppsInfo { override val model = vModel.myAppsModel.collectAsStateWithLifecycle().value; override val actions = vModel }
                                MyApps(myAppsInfo = info, currentPackageName = if (isBigScreen) (navigator.last as? NavigationKey.AppDetails)?.packageName else null, onAppItemClick = { pkg -> val new = NavigationKey.AppDetails(pkg); if (navigator.last is NavigationKey.AppDetails) navigator.replaceLast(new) else navigator.navigate(new) }, onNav = { navigator.navigate(it) })
                            }
                            is NavigationKey.AppList -> {
                                val vModel = hiltViewModel<AppListViewModel, AppListViewModel.Factory>(key = targetKey.type.toString(), creationCallback = { it.create(targetKey.type) })
                                val info = object : AppListInfo { override val model = vModel.appListModel.collectAsStateWithLifecycle().value; override val list = targetKey.type; override val actions = vModel; override val showFilters = vModel.showFilters.collectAsStateWithLifecycle().value; override val showOnboarding = vModel.showOnboarding.collectAsStateWithLifecycle().value }
                                AppList(appListInfo = info, currentPackageName = if (isBigScreen) (navigator.last as? NavigationKey.AppDetails)?.packageName else null, onBackClicked = { navigator.goBack() }) { pkg -> val new = NavigationKey.AppDetails(pkg); if (navigator.last is NavigationKey.AppDetails) navigator.replaceLast(new) else navigator.navigate(new) }
                            }
                            is NavigationKey.Repos -> {
                                val vModel = hiltViewModel<RepositoriesViewModel>()
                                val info = object : RepositoryInfo { override val model = vModel.model.collectAsStateWithLifecycle().value; override val currentRepositoryId = if (isBigScreen) (navigator.last as? NavigationKey.RepoDetails)?.repoId else null; override fun onOnboardingSeen() = vModel.onOnboardingSeen(); override fun onRepositorySelected(repo: RepositoryItem) { val new = NavigationKey.RepoDetails(repo.repoId); if (navigator.last is NavigationKey.RepoDetails) navigator.replaceLast(new) else navigator.navigate(new) }; override fun onRepositoryEnabled(id: Long, enabled: Boolean) = vModel.onRepositoryEnabled(id, enabled); override fun onAddRepo() { navigator.navigate(NavigationKey.AddRepo()) }; override fun onRepositoryMoved(from: Long, to: Long) = vModel.onRepositoriesMoved(from, to); override fun onRepositoriesFinishedMoving(from: Long, to: Long) = vModel.onRepositoriesFinishedMoving(from, to) }
                                Repositories(info, isBigScreen) { navigator.goBack() }
                            }
                            is NavigationKey.RepoDetails -> {
                                val vModel = hiltViewModel<RepoDetailsViewModel, RepoDetailsViewModel.Factory>(key = targetKey.repoId.toString(), creationCallback = { it.create(targetKey.repoId) })
                                val info = object : RepoDetailsInfo { override val model = vModel.model.collectAsStateWithLifecycle().value; override val actions = vModel }
                                RepoDetails(info, onShowAppsClicked = { title, id -> navigator.navigate(NavigationKey.AppList(AppListType.Repository(title, id))) }, onBackNav = if (isBigScreen) null else { { navigator.goBack() } })
                            }
                            is NavigationKey.AddRepo -> {
                                val vModel = hiltViewModel<AddRepoViewModel>()
                                LaunchedEffect(targetKey) { if (targetKey.uri != null) vModel.onFetchRepo(targetKey.uri) }
                                AddRepo(
                                    state = vModel.state.collectAsStateWithLifecycle().value, networkStateFlow = vModel.networkState, proxyConfig = vModel.proxyConfig, onFetchRepo = vModel::onFetchRepo, onAddRepo = vModel::addFetchedRepository, 
                                    onExistingRepo = { id -> 
                                        cameFromSetup = false
                                        prefs.edit().putBoolean("is_first_dashboard_load", true).commit()
                                        navigator.goBack() 
                                    }, 
                                    onRepoAdded = { title, id -> 
                                        cameFromSetup = false
                                        prefs.edit().putBoolean("is_first_dashboard_load", true).commit()
                                        navigator.goBack() 
                                    }, 
                                    onBackClicked = { 
                                        if (cameFromSetup) {
                                            setupState = "QR_SCAN"
                                            cameFromSetup = false
                                        }
                                        navigator.goBack() 
                                    }
                                )
                            }
                            is NavigationKey.Search -> {
                                val vModel = hiltViewModel<SearchViewModel>()
                                ExpandedSearch(vModel.textFieldState, vModel.searchResults.collectAsStateWithLifecycle().value, vModel::search, { navigator.navigate(it) }, { navigator.goBack() }, vModel::onSearchCleared)
                            }
                            is NavigationKey.Settings -> {
                                val vModel = hiltViewModel<SettingsViewModel>()
                                Settings(
                                    model = vModel.model,
                                    openAbout = { navigator.navigate(NavigationKey.About) },
                                    onSaveLogcat = { vModel.onSaveLogcat(it); navigator.goBack() },
                                    onBackClicked = { navigator.goBack() }
                                )
                            }
                            is NavigationKey.InstallationHistory -> {
                                val vModel = hiltViewModel<HistoryViewModel>()
                                History(vModel.items.collectAsStateWithLifecycle().value, vModel.useInstallHistory.collectAsStateWithLifecycle(null).value, vModel::useInstallHistory, vModel::deleteHistory, { navigator.goBack() })
                            }
                            is NavigationKey.About -> About(onBackClicked = if (isBigScreen) null else { { navigator.goBack() } })
                            else -> {}
                        }
                    }
                }
            }
        }

        if (setupState != "DONE") {
            var startAnims by remember { mutableStateOf(false) }
            LaunchedEffect(setupState) { if (setupState == "WELCOME") startAnims = true }
            
            val fogAlpha by animateFloatAsState(targetValue = if (setupState == "TRANSITION") 0f else 1f, animationSpec = tween(1500, easing = LinearOutSlowInEasing), label = "fogAlpha")
            val staggeredOffset1 by animateIntOffsetAsState(targetValue = if (startAnims) IntOffset.Zero else IntOffset(0, 400), animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.65f), label = "stag1")
            val staggeredOffset2 by animateIntOffsetAsState(targetValue = if (startAnims) IntOffset.Zero else IntOffset(0, 550), animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = 0.6f), label = "stag2")

            // 🚨 SOLID OPACITY: Solid black base background so Dashboard doesn't bleed through 🚨
            Box(modifier = Modifier.fillMaxSize().alpha(fogAlpha).background(Color.Black), contentAlignment = Alignment.Center) {

                if (setupState == "WELCOME" || setupState == "QR_SCAN") {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CustoneLayeredMist(isOverlay = true)
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
                    }
                }

                Crossfade(targetState = setupState, animationSpec = tween(600), label = "setupCrossfade") { state ->
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (state == "WELCOME") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset { staggeredOffset1 }) {
                                    Text(text = "Welcome to CuStore!", fontSize = 36.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = (-0.5).sp)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = "Tap here to get started.", fontSize = 15.sp, color = Color.White.copy(alpha=0.8f), textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                                }
                                Spacer(modifier = Modifier.height(48.dp))
                                Box(modifier = Modifier.offset { staggeredOffset2 }) {
                                    ClickyButton(text = "Let's Download Some Apps!", isPrimary = true, modifier = Modifier.fillMaxWidth().height(58.dp), onClick = {
                                        // 🚨 BYPASS LOGIC: If account is linked, skip scanner entirely 🚨
                                        if (isAccountLinked) {
                                            setupState = "TRANSITION"
                                            coroutineScope.launch {
                                                prefs.edit().putBoolean("play_cinematic_grid", true).apply()
                                                delay(1500)
                                                setupState = "DONE"
                                                prefs.edit().putBoolean("first_launch_v4", false).apply()
                                            }
                                        } else {
                                            setupState = "QR_SCAN"
                                        }
                                    })
                                }
                            }
                        } else if (state == "QR_SCAN") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                                Icon(Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(72.dp), tint = Color.White)
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(text = "Connect Profile", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(text = "Scan the QR code found at\nhttps://custoneos.com/help\nto link your account.", fontSize = 14.sp, color = Color.White.copy(alpha=0.8f), textAlign = TextAlign.Center, lineHeight = 20.sp)
                                Spacer(modifier = Modifier.height(48.dp))
                                
                                ClickyButton(text = "Scan QR Code", isPrimary = true, modifier = Modifier.fillMaxWidth().height(58.dp), onClick = {
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                        setupState = "CAMERA_VIEW"
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                })
                            }
                        } else if (state == "CAMERA_VIEW") {
                            QrScannerView(
                                onQrScanned = { url -> 
                                    capturedRepoUrl = url
                                    cameFromSetup = true
                                    prefs.edit().putBoolean("is_account_linked", true).apply()
                                    setupState = "DONE"
                                    prefs.edit().putBoolean("first_launch_v4", false).apply()
                                    if (capturedRepoUrl.isNotBlank()) navigator.navigate(NavigationKey.AddRepo(uri = capturedRepoUrl))
                                },
                                onCancel = { setupState = "QR_SCAN" }
                            )
                        } else if (state == "TRANSITION") {
                            // Blank state while the cinematic fog Alpha layer fully handles the fade out
                        }
                    }
                }
            }
        }
    }
  }
}

@kotlin.OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerView(onQrScanned: (String) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasScanned by remember { mutableStateOf(false) }
    var scanSuccess by remember { mutableStateOf(false) }
    var scannedUrl by remember { mutableStateOf("") }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) { onDispose { try { cameraExecutor.shutdownNow() } catch (e: Exception) {} } }

    LaunchedEffect(scanSuccess) { if (scanSuccess) { delay(1200); onQrScanned(scannedUrl) } }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                try { ProcessCameraProvider.configureInstance(androidx.camera.camera2.Camera2Config.defaultConfig()) } catch (e: Exception) {}
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val reader = MultiFormatReader()
                    val imageAnalyzer = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also { analysis ->
                            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (hasScanned) { imageProxy.close(); return@setAnalyzer }
                                try {
                                    val buffer = imageProxy.planes[0].buffer
                                    val data = ByteArray(buffer.remaining())
                                    buffer.get(data)
                                    val source = PlanarYUVLuminanceSource(data, imageProxy.width, imageProxy.height, 0, 0, imageProxy.width, imageProxy.height, false)
                                    val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                                    val result = reader.decode(binaryBitmap)
                                    val url = result.text.trim()
                                    if (url.contains("custoneos.com")) {
                                        hasScanned = true; scannedUrl = url; scanSuccess = true
                                    }
                                } catch (e: Exception) {} finally { imageProxy.close() }
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer)
                    } catch (e: Exception) {}
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            onRelease = { try { ProcessCameraProvider.getInstance(context).get().unbindAll() } catch (e: Exception) {} },
            modifier = Modifier.fillMaxSize()
        )

        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSize = size.width * 0.65f
            val boxTop = (size.height - boxSize) / 2
            val boxLeft = (size.width - boxSize) / 2
            val cornerLength = 60f
            val stroke = Stroke(width = 12f)
            val reticleColor = if (scanSuccess) Color.Green else Color.White.copy(alpha = 0.8f)

            drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft + cornerLength, boxTop), stroke.width)
            drawLine(reticleColor, Offset(boxLeft, boxTop), Offset(boxLeft, boxTop + cornerLength), stroke.width)
            drawLine(reticleColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize - cornerLength, boxTop), stroke.width)
            drawLine(reticleColor, Offset(boxLeft + boxSize, boxTop), Offset(boxLeft + boxSize, boxTop + cornerLength), stroke.width)
            drawLine(reticleColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft + cornerLength, boxTop + boxSize), stroke.width)
            drawLine(reticleColor, Offset(boxLeft, boxTop + boxSize), Offset(boxLeft, boxTop + boxSize - cornerLength), stroke.width)
            drawLine(reticleColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize - cornerLength, boxTop + boxSize), stroke.width)
            drawLine(reticleColor, Offset(boxLeft + boxSize, boxTop + boxSize), Offset(boxLeft + boxSize, boxTop + boxSize - cornerLength), stroke.width)
        }

        AnimatedVisibility(visible = scanSuccess, enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.5f, animationSpec = spring(0.5f, 500f)), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
            Box(modifier = Modifier.size(120.dp).background(Color(0xCC000000), shape = RoundedCornerShape(60.dp)), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(modifier = Modifier.size(60.dp)) {
                    val path = Path().apply { moveTo(size.width * 0.2f, size.height * 0.5f); lineTo(size.width * 0.45f, size.height * 0.7f); lineTo(size.width * 0.8f, size.height * 0.25f) }
                    drawPath(path = path, color = Color.Green, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
        }
    }
}
