package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.audio.AudioDspSessionHolder
import com.example.ui.EqViewModel
import com.example.ui.components.InAppMediaSource
import com.example.ui.components.QuickSplitDspDeck
import com.example.ui.components.YouTubeMiniDockControls
import com.example.ui.components.YouTubeTopToolbar
import com.example.ui.components.configureWebViewForSource
import com.example.ui.components.createEmbeddedYouTubeWebView
import com.example.ui.screens.DspSubAmpScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.MixerLrScreen
import com.example.ui.screens.SessionsRoutingScreen
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EqProAudioTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val ROUTE_EQUALIZER = "equalizer"
const val ROUTE_YOUTUBE = "youtube"
const val ROUTE_MIXER = "mixer"
const val ROUTE_DSP_AMP = "dsp_amp"
const val ROUTE_SESSIONS = "sessions"

private data class StudioNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val studioNavItems = listOf(
    StudioNavItem(
        route = ROUTE_EQUALIZER,
        labelRes = R.string.nav_equalizer,
        selectedIcon = Icons.Filled.Equalizer,
        unselectedIcon = Icons.Outlined.Equalizer,
        testTag = "nav_equalizer"
    ),
    StudioNavItem(
        route = ROUTE_YOUTUBE,
        labelRes = R.string.nav_youtube,
        selectedIcon = Icons.Filled.PlayCircle,
        unselectedIcon = Icons.Outlined.PlayCircle,
        testTag = "nav_youtube"
    ),
    StudioNavItem(
        route = ROUTE_MIXER,
        labelRes = R.string.nav_mixer,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_mixer"
    ),
    StudioNavItem(
        route = ROUTE_DSP_AMP,
        labelRes = R.string.nav_dsp_amp,
        selectedIcon = Icons.Filled.Speaker,
        unselectedIcon = Icons.Outlined.Speaker,
        testTag = "nav_dsp_amp"
    ),
    StudioNavItem(
        route = ROUTE_SESSIONS,
        labelRes = R.string.nav_sessions,
        selectedIcon = Icons.Filled.GraphicEq,
        unselectedIcon = Icons.Outlined.GraphicEq,
        testTag = "nav_sessions"
    )
)

private val GOTUBE_PACKAGES = listOf(
    "premium.gotube.adblock.utube",
    "com.gotube.app",
    "com.biomes.gotube"
)

private val SPOTIFY_PACKAGES = listOf(
    "com.spotify.music",
    "com.spotify.lite"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AudioDspSessionHolder.handleControlPanelIntent(this, intent)
        enableEdgeToEdge()
        setContent {
            EqProAudioTheme {
                EqProAudioApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AudioDspSessionHolder.handleControlPanelIntent(this, intent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqProAudioApp(
    viewModel: EqViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_EQUALIZER
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

    var isMediaLoaded by remember { mutableStateOf(false) }
    var activeMediaSource by remember { mutableStateOf(InAppMediaSource.YOUTUBE) }
    var isSplitEqMode by remember { mutableStateOf(true) }
    var mediaPageTitle by remember { mutableStateOf("YouTube") }
    var mediaCanGoBack by remember { mutableStateOf(false) }

    val embeddedMediaWebView = remember {
        createEmbeddedYouTubeWebView(
            context = context,
            onTitleChanged = { mediaPageTitle = it },
            onUrlChanged = { url ->
                activeMediaSource = when {
                    url.contains("open.spotify.com", ignoreCase = true) -> InAppMediaSource.SPOTIFY_WEB
                    url.contains("music.youtube.com", ignoreCase = true) -> InAppMediaSource.YT_MUSIC
                    else -> InAppMediaSource.YOUTUBE
                }
            },
            onCanGoBackChanged = { mediaCanGoBack = it }
        )
    }
    val defaultMobileUa = remember(embeddedMediaWebView) {
        embeddedMediaWebView.settings.userAgentString
    }

    DisposableEffect(Unit) {
        onDispose {
            embeddedMediaWebView.stopLoading()
            embeddedMediaWebView.destroy()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.ensureServiceRunning()
    }

    val recordAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.rebindDspEngine()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        viewModel.ensureServiceRunning()
    }

    val navigateTo: (String) -> Unit = { route ->
        if (route == ROUTE_YOUTUBE && !isMediaLoaded) {
            isMediaLoaded = true
            configureWebViewForSource(embeddedMediaWebView, activeMediaSource, defaultMobileUa)
        }
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openInAppSource: (InAppMediaSource) -> Unit = { source ->
        activeMediaSource = source
        isMediaLoaded = true
        configureWebViewForSource(embeddedMediaWebView, source, defaultMobileUa)
        navigateTo(ROUTE_YOUTUBE)
    }

    val closeInAppMedia: () -> Unit = {
        embeddedMediaWebView.loadUrl("about:blank")
        isMediaLoaded = false
    }

    val launchGoTubeApp: () -> Unit = {
        viewModel.launchExternalPlayerCandidates(
            packageNames = GOTUBE_PACKAGES,
            fallbackUrl = "https://play.google.com/store/apps/details?id=premium.gotube.adblock.utube"
        )
    }

    val launchSpotifyApp: () -> Unit = {
        viewModel.launchExternalPlayerCandidates(
            packageNames = SPOTIFY_PACKAGES,
            fallbackUrl = "https://open.spotify.com"
        )
    }

    val requestLiveFft: () -> Unit = {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.rebindDspEngine()
        } else {
            recordAudioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler(enabled = currentRoute == ROUTE_YOUTUBE && mediaCanGoBack) {
        embeddedMediaWebView.goBack()
    }

    val isPlayerScreen = currentRoute == ROUTE_YOUTUBE

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(StudioObsidian, StudioRack.copy(alpha = 0.7f), StudioObsidian)
                )
            )
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                if (!isPlayerScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = StudioObsidian.copy(alpha = 0.92f),
                            titleContentColor = TextPrimary
                        )
                    )
                }
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = StudioRack,
                        contentColor = TextPrimary
                    ) {
                        studioNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(item.labelRes),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = CyanPulse,
                                    selectedTextColor = CyanPulse,
                                    indicatorColor = CyanGlow,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = StudioRack,
                        contentColor = TextPrimary,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        studioNavItems.forEach { item ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route == item.route
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                modifier = Modifier.testTag(item.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = CyanPulse,
                                    selectedTextColor = CyanPulse,
                                    indicatorColor = CyanGlow,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isPlayerScreen) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            NavHost(
                                navController = navController,
                                startDestination = ROUTE_EQUALIZER,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 840.dp)
                            ) {
                                composable(ROUTE_EQUALIZER) {
                                    EqualizerScreen(
                                        uiState = uiState,
                                        onToggleDsp = viewModel::toggleDsp,
                                        onOpenYouTube = { navigateTo(ROUTE_YOUTUBE) },
                                        onRequestLiveFft = requestLiveFft,
                                        onSelectOutputMode = viewModel::setOutputMode,
                                        onSelectEqChannelTarget = viewModel::selectEqChannelTab,
                                        onSelectPreset = viewModel::applyPreset,
                                        onDeletePreset = viewModel::deleteCustomPreset,
                                        onSavePreset = viewModel::saveCustomPreset,
                                        onResetFlat = viewModel::resetFlat,
                                        onBandGainChange = viewModel::updateBandGain,
                                        onPreAmpChange = viewModel::setPreAmp
                                    )
                                }
                                composable(ROUTE_YOUTUBE) {
                                    // Rendered in persistent WebView slot below
                                }
                                composable(ROUTE_MIXER) {
                                    MixerLrScreen(
                                        uiState = uiState,
                                        onSelectOutputMode = viewModel::setOutputMode,
                                        onToggleLinkLrVolume = viewModel::setLinkLrVolume,
                                        onLeftVolumeChange = viewModel::setLeftVolumeDb,
                                        onRightVolumeChange = viewModel::setRightVolumeDb,
                                        onToggleLeftMute = viewModel::toggleLeftMuted,
                                        onToggleRightMute = viewModel::toggleRightMuted,
                                        onMixerMasterChange = viewModel::setMixerMasterDb,
                                        onMixerMediaChange = viewModel::setMixerMediaBusDb,
                                        onMixerLowChange = viewModel::setMixerLowBusDb,
                                        onMixerMidChange = viewModel::setMixerMidBusDb,
                                        onMixerHighChange = viewModel::setMixerHighBusDb,
                                        onToggleMixerMute = viewModel::toggleMixerMute,
                                        onResetMixer = viewModel::resetMixer,
                                        onToggleTestTone = viewModel::toggleTestTone,
                                        onOpenSoundSettings = viewModel::openAccessibilityAudioSettings
                                    )
                                }
                                composable(ROUTE_DSP_AMP) {
                                    DspSubAmpScreen(
                                        uiState = uiState,
                                        onSubwooferEnabledChange = viewModel::setSubwooferEnabled,
                                        onSubGainChange = viewModel::setSubGainDb,
                                        onSubPunchChange = viewModel::setSubPunch,
                                        onSubPhaseChange = viewModel::setSubPhaseInverted,
                                        onCrossoverEnabledChange = viewModel::setCrossoverEnabled,
                                        onSelectSpeakerProfile = viewModel::applySpeakerProfile,
                                        onMainHpfChange = viewModel::setMainHpfHz,
                                        onSubLpfChange = viewModel::setSubLpfHz,
                                        onMidHighSplitChange = viewModel::setMidHighSplitHz,
                                        onSlopeChange = viewModel::setCrossoverSlope,
                                        onSpeakerDelayChange = viewModel::setSpeakerDelayMs,
                                        onPowerAmpEnabledChange = viewModel::setPowerAmpEnabled,
                                        onAmpClassChange = viewModel::setAmpClassMode,
                                        onAmpDriveChange = viewModel::setAmpDrive,
                                        onLimiterEnabledChange = viewModel::setLimiterEnabled,
                                        onLimiterThresholdChange = viewModel::setLimiterThreshold,
                                        onBassBoostChange = viewModel::setBassBoost,
                                        onVirtualizerChange = viewModel::setVirtualizer,
                                        onLoudnessChange = viewModel::setLoudness,
                                        onMbcPunchChange = viewModel::setMbcPunch,
                                        onToggleTestTone = viewModel::toggleTestTone
                                    )
                                }
                                composable(ROUTE_SESSIONS) {
                                    SessionsRoutingScreen(
                                        uiState = uiState,
                                        onSelectRoutingMode = viewModel::setRoutingMode,
                                        onOpenYouTube = { openInAppSource(InAppMediaSource.YOUTUBE) },
                                        onOpenYouTubeMusic = { openInAppSource(InAppMediaSource.YT_MUSIC) },
                                        onOpenSpotifyWeb = { openInAppSource(InAppMediaSource.SPOTIFY_WEB) },
                                        onLaunchGoTubeApp = launchGoTubeApp,
                                        onLaunchSpotifyApp = launchSpotifyApp,
                                        onRebindEngine = viewModel::rebindDspEngine,
                                        onRequestLiveFft = requestLiveFft
                                    )
                                }
                            }
                        }
                    }

                    if (isPlayerScreen) {
                        YouTubeTopToolbar(
                            canGoBack = mediaCanGoBack,
                            activeSource = activeMediaSource,
                            isSplitEqMode = isSplitEqMode,
                            onBack = {
                                if (embeddedMediaWebView.canGoBack()) {
                                    embeddedMediaWebView.goBack()
                                }
                            },
                            onReload = { embeddedMediaWebView.reload() },
                            onSelectSource = { source ->
                                activeMediaSource = source
                                isMediaLoaded = true
                                configureWebViewForSource(embeddedMediaWebView, source, defaultMobileUa)
                            },
                            onToggleSplitEq = { isSplitEqMode = !isSplitEqMode },
                            onLaunchGoTube = launchGoTubeApp,
                            onLaunchSpotifyApp = launchSpotifyApp
                        )
                    }

                    val showWebContainer = isPlayerScreen || isMediaLoaded
                    val webContainerModifier = when {
                        isPlayerScreen -> Modifier
                            .weight(1f)
                            .fillMaxWidth()
                        isMediaLoaded -> Modifier
                            .fillMaxWidth()
                            .height(74.dp)
                        else -> Modifier
                            .fillMaxWidth()
                            .height(0.dp)
                    }

                    Surface(
                        modifier = webContainerModifier,
                        color = StudioRack,
                        border = if (showWebContainer && !isPlayerScreen) {
                            BorderStroke(1.dp, StudioBorder)
                        } else null
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val webBoxModifier = if (isPlayerScreen) {
                                Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            } else {
                                Modifier
                                    .width(if (isMediaLoaded) 128.dp else 0.dp)
                                    .fillMaxHeight()
                            }

                            Box(modifier = webBoxModifier) {
                                AndroidView(
                                    factory = { embeddedMediaWebView },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            if (!isPlayerScreen && isMediaLoaded) {
                                YouTubeMiniDockControls(
                                    sourceLabel = activeMediaSource.label,
                                    pageTitle = mediaPageTitle,
                                    isDspEnabled = uiState.config.isDspEnabled,
                                    onExpandYouTube = { navigateTo(ROUTE_YOUTUBE) },
                                    onCloseDock = closeInAppMedia,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    if (isPlayerScreen && isSplitEqMode) {
                        QuickSplitDspDeck(
                            uiState = uiState,
                            onToggleDsp = viewModel::toggleDsp,
                            onSelectOutputMode = viewModel::setOutputMode,
                            onSelectPreset = viewModel::applyPreset,
                            onBandGainChange = viewModel::updateBandGain,
                            onSubGainChange = viewModel::setSubGainDb,
                            onLeftVolumeChange = viewModel::setLeftVolumeDb,
                            onRightVolumeChange = viewModel::setRightVolumeDb,
                            modifier = Modifier
                                .height(296.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
