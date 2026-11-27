package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.GraphicEq
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.EqViewModel
import com.example.ui.screens.DspSubAmpScreen
import com.example.ui.screens.EqualizerScreen
import com.example.ui.screens.MixerLrScreen
import com.example.ui.screens.SessionsRoutingScreen
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EqProAudioTheme
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

const val ROUTE_EQUALIZER = "equalizer"
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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EqProAudioTheme {
                EqProAudioApp()
            }
        }
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
    val currentDestination = navBackStackEntry?.destination
    val context = LocalContext.current

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

    val openYouTube: () -> Unit = {
        viewModel.launchExternalPlayer(
            packageName = "com.google.android.youtube",
            fallbackUrl = "https://www.youtube.com"
        )
    }

    val openYouTubeMusic: () -> Unit = {
        viewModel.launchExternalPlayer(
            packageName = "com.google.android.apps.youtube.music",
            fallbackUrl = "https://music.youtube.com"
        )
    }

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = StudioObsidian.copy(alpha = 0.92f),
                        titleContentColor = TextPrimary
                    )
                )
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
                                label = { Text(stringResource(item.labelRes)) },
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

                Box(
                    modifier = Modifier.fillMaxSize(),
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
                                onOpenYouTube = openYouTube,
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
                                onOpenYouTube = openYouTube,
                                onOpenYouTubeMusic = openYouTubeMusic,
                                onRebindEngine = viewModel::rebindDspEngine,
                                onRequestLiveFft = requestLiveFft
                            )
                        }
                    }
                }
            }
        }
    }
}
