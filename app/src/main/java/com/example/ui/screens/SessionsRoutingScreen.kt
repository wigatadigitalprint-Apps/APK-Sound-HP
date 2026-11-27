package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OndemandVideo
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.RoutingMode
import com.example.ui.EqUiState
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralPeak
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioModule
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SessionsRoutingScreen(
    uiState: EqUiState,
    onSelectRoutingMode: (RoutingMode) -> Unit,
    onOpenYouTube: () -> Unit,
    onOpenYouTubeMusic: () -> Unit,
    onOpenSpotifyWeb: () -> Unit,
    onLaunchGoTubeApp: () -> Unit,
    onLaunchSpotifyApp: () -> Unit,
    onRebindEngine: () -> Unit,
    onRequestLiveFft: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Streaming Apps & In-App Player Integration Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioRack,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "YOUTUBE, GOTUBE & SPOTIFY",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = uiState.runtime.engineTier.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanPulse
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (uiState.runtime.serviceRunning) EmeraldGlow else StudioModule
                    ) {
                        Text(
                            text = if (uiState.runtime.serviceRunning) "SERVICE ON" else "STOPPED",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (uiState.runtime.serviceRunning) EmeraldActive else TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Text(
                    text = "Play YouTube, YT Music, or Spotify Web directly inside the Player tab, or launch the external GoTube & Spotify apps while this DSP service stays active in the background.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Row 1: In-App Players (YouTube & YT Music)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenYouTube,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sessions_open_youtube"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPeak,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = stringResource(R.string.cd_open_youtube),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YouTube")
                    }

                    FilledTonalButton(
                        onClick = onOpenYouTubeMusic,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sessions_open_yt_music"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioObsidian,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LibraryMusic,
                            contentDescription = stringResource(R.string.cd_open_yt_music),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("YT Music")
                    }
                }

                // Row 2: GoTube App & Spotify (App + Web)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onLaunchGoTubeApp,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sessions_open_gotube"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AmberGlow,
                            contentColor = AmberWarm
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.OndemandVideo,
                            contentDescription = "Open GoTube App",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GoTube App")
                    }

                    FilledTonalButton(
                        onClick = onLaunchSpotifyApp,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("sessions_open_spotify_app"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = EmeraldGlow,
                            contentColor = EmeraldActive
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MusicNote,
                            contentDescription = "Open Spotify App",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Spotify App")
                    }
                }

                FilledTonalButton(
                    onClick = onOpenSpotifyWeb,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("sessions_open_spotify_web"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = StudioModule,
                        contentColor = EmeraldActive
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = "Open Spotify Web Inside App",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Spotify Web Inside Player")
                }

                OutlinedButton(
                    onClick = onRebindEngine,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("rebind_dsp_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StudioBorder)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh DSP Session Attachment",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh & Re-Attach DSP Engine")
                }
            }
        }

        // Spotify & GoTube Tip Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioModule,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "TIP FOR SPOTIFY & GOTUBE",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldActive
                )
                Text(
                    text = "• Spotify: Open Spotify Settings → Playback → Equalizer and choose EQ Pro Audio (or keep Auto Routing enabled here).\n• GoTube: Keep Auto Routing enabled—Global Output Mix + session detection automatically enhances GoTube audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Routing Mode Selection
        Text(
            text = "DSP ROUTING MODE",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RoutingMode.entries.forEach { mode ->
                val selected = uiState.config.routingMode == mode
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRoutingMode(mode) }
                        .testTag("routing_mode_${mode.name}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) CyanGlow else StudioRack,
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (selected) CyanPulse else StudioBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = if (selected) {
                                Icons.Filled.RadioButtonChecked
                            } else {
                                Icons.Filled.RadioButtonUnchecked
                            },
                            contentDescription = mode.title,
                            tint = if (selected) CyanPulse else TextMuted
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selected) CyanPulse else TextPrimary
                            )
                            Text(
                                text = mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }

        // Detected External Player Sessions
        Text(
            text = "DETECTED AUDIO SESSIONS",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )

        if (uiState.runtime.attachedSessions.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StudioModule,
                border = BorderStroke(1.dp, StudioBorder)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = "No dedicated session broadcast yet",
                        tint = CyanPulse,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.runtime.isGlobalMixAttached) {
                                "Global Output Mix (#0) Active"
                            } else {
                                "No external session broadcast yet"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Start playing music in YouTube, GoTube, or Spotify. Players that emit audio session broadcasts appear here, while Global Output Mix processes playback automatically.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                uiState.runtime.attachedSessions.forEach { session ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("session_card_${session.sessionId}"),
                        shape = RoundedCornerShape(14.dp),
                        color = StudioModule,
                        border = BorderStroke(
                            1.dp,
                            if (session.isYouTube || session.packageName.contains("spotify", true)) {
                                EmeraldActive.copy(alpha = 0.6f)
                            } else {
                                StudioBorder
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.appLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = session.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldGlow
                            ) {
                                Text(
                                    text = "SESSION #${session.sessionId}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldActive,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Optional Live FFT Visualizer Permission Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioRack,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = stringResource(R.string.cd_live_fft),
                    tint = if (uiState.runtime.liveFftActive) EmeraldActive else AmberWarm,
                    modifier = Modifier.size(26.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (uiState.runtime.liveFftActive) {
                            "Live Spectrum Analyzer Enabled"
                        } else {
                            "Live Spectrum Analyzer (Optional)"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Android's Visualizer API uses audio capture permission solely to animate real-time FFT spectrum bars synced with your music.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (!uiState.runtime.liveFftActive) {
                    FilledTonalButton(
                        onClick = onRequestLiveFft,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("grant_fft_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AmberWarm.copy(alpha = 0.2f),
                            contentColor = AmberWarm
                        )
                    ) {
                        Text("Enable")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}
