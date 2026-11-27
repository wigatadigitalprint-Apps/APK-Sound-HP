package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.audio.AudioOutputMode
import com.example.audio.CHANNEL_VOL_MAX_DB
import com.example.audio.CHANNEL_VOL_MIN_DB
import com.example.audio.TestToneType
import com.example.ui.EqUiState
import com.example.ui.components.VerticalStudioFader
import com.example.ui.components.formatDbShort
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralPeak
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioModule
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSpatial
import kotlin.math.roundToInt

@Composable
fun MixerLrScreen(
    uiState: EqUiState,
    onSelectOutputMode: (AudioOutputMode) -> Unit,
    onToggleLinkLrVolume: (Boolean) -> Unit,
    onLeftVolumeChange: (Float) -> Unit,
    onRightVolumeChange: (Float) -> Unit,
    onToggleLeftMute: () -> Unit,
    onToggleRightMute: () -> Unit,
    onMixerMasterChange: (Float) -> Unit,
    onMixerMediaChange: (Float) -> Unit,
    onMixerLowChange: (Float) -> Unit,
    onMixerMidChange: (Float) -> Unit,
    onMixerHighChange: (Float) -> Unit,
    onToggleMixerMute: (String) -> Unit,
    onResetMixer: () -> Unit,
    onToggleTestTone: (TestToneType) -> Unit,
    onOpenSoundSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val enabled = uiState.config.isDspEnabled
    val isMono = uiState.config.outputMode == AudioOutputMode.MONO

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stereo / Mono Output Topology Card
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
                            text = "STEREO / MONO OUTPUT",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isMono) {
                                "Mono mode unifies L/R EQ curves & disables 3D widening"
                            } else {
                                "Full dual-channel Stereo processing with spatial imaging"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AudioOutputMode.entries.forEach { mode ->
                        val selected = uiState.config.outputMode == mode
                        val accent = if (mode == AudioOutputMode.MONO) AmberWarm else CyanPulse
                        val glow = if (mode == AudioOutputMode.MONO) AmberGlow else CyanGlow
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectOutputMode(mode) },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("mixer_output_${mode.name}"),
                            label = {
                                Text(
                                    text = if (mode == AudioOutputMode.STEREO) "STEREO (2CH)" else "MONO (SUM)",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioObsidian,
                                labelColor = TextSecondary,
                                selectedContainerColor = glow,
                                selectedLabelColor = accent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = StudioBorder,
                                selectedBorderColor = accent
                            )
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTestingLr = uiState.activeTestTone == TestToneType.LEFT_RIGHT_CHECK
                    FilledTonalButton(
                        onClick = { onToggleTestTone(TestToneType.LEFT_RIGHT_CHECK) },
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("lr_channel_check_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (isTestingLr) CyanGlow else StudioModule,
                            contentColor = if (isTestingLr) CyanPulse else TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (isTestingLr) Icons.Filled.Stop else Icons.Filled.Audiotrack,
                            contentDescription = "Test Left and Right Channels",
                            modifier = Modifier.size(16.dp),
                            tint = if (isTestingLr) CoralPeak else CyanPulse
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTestingLr) "Stop L/R Test" else "L/R Test Tone")
                    }

                    OutlinedButton(
                        onClick = onOpenSoundSettings,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("system_sound_settings_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, StudioBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Android Sound Settings",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("OS Audio")
                    }
                }
            }
        }

        // Separate Left & Right Channel Volume Controls
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
                            text = "SEPARATE L / R VOLUME",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Independent Left & Right channel gain & mute",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (uiState.config.linkLrVolume) Icons.Filled.Link else Icons.Filled.LinkOff,
                            contentDescription = "Link Left and Right Volume",
                            tint = if (uiState.config.linkLrVolume) CyanPulse else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.config.linkLrVolume,
                            onCheckedChange = onToggleLinkLrVolume,
                            modifier = Modifier.testTag("link_lr_volume_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StudioObsidian,
                                checkedTrackColor = CyanPulse
                            )
                        )
                    }
                }

                // Left Volume Row
                ChannelVolumeControlRow(
                    channelLabel = "LEFT (L)",
                    volumeDb = uiState.config.leftVolumeDb,
                    isMuted = uiState.config.leftMuted,
                    isEnabled = enabled,
                    accentColor = CyanPulse,
                    onVolumeChange = onLeftVolumeChange,
                    onToggleMute = onToggleLeftMute,
                    sliderTag = "left_volume_slider",
                    muteTag = "left_mute_button"
                )

                // Right Volume Row
                ChannelVolumeControlRow(
                    channelLabel = "RIGHT (R)",
                    volumeDb = uiState.config.rightVolumeDb,
                    isMuted = uiState.config.rightMuted,
                    isEnabled = enabled,
                    accentColor = AmberWarm,
                    onVolumeChange = onRightVolumeChange,
                    onToggleMute = onToggleRightMute,
                    sliderTag = "right_volume_slider",
                    muteTag = "right_mute_button"
                )
            }
        }

        // Studio Audio Mixer Console (5 Buses)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioRack,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "STUDIO AUDIO MIXER",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Multi-bus gain trim & channel muting",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    TextButton(
                        onClick = onResetMixer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("reset_mixer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Reset Mixer",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fitsAll = maxWidth >= 420.dp
                    val rowMod = if (fitsAll) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.horizontalScroll(rememberScrollState())
                    }

                    Row(
                        modifier = rowMod,
                        horizontalArrangement = if (fitsAll) {
                            Arrangement.SpaceBetween
                        } else {
                            Arrangement.spacedBy(10.dp)
                        },
                        verticalAlignment = Alignment.Top
                    ) {
                        MixerBusColumn(
                            label = "MEDIA",
                            db = uiState.config.mixerMediaBusDb,
                            minDb = -15f,
                            maxDb = 12f,
                            isMuted = uiState.config.mixerMediaMuted,
                            isEnabled = enabled,
                            accentColor = CyanPulse,
                            onDbChange = onMixerMediaChange,
                            onToggleMute = { onToggleMixerMute("MEDIA") },
                            tagPrefix = "mixer_media"
                        )
                        MixerBusColumn(
                            label = "SUB/LOW",
                            db = uiState.config.mixerLowBusDb,
                            minDb = -15f,
                            maxDb = 12f,
                            isMuted = uiState.config.mixerLowMuted,
                            isEnabled = enabled,
                            accentColor = AmberWarm,
                            onDbChange = onMixerLowChange,
                            onToggleMute = { onToggleMixerMute("LOW") },
                            tagPrefix = "mixer_low"
                        )
                        MixerBusColumn(
                            label = "MID/VOC",
                            db = uiState.config.mixerMidBusDb,
                            minDb = -15f,
                            maxDb = 12f,
                            isMuted = uiState.config.mixerMidMuted,
                            isEnabled = enabled,
                            accentColor = EmeraldActive,
                            onDbChange = onMixerMidChange,
                            onToggleMute = { onToggleMixerMute("MID") },
                            tagPrefix = "mixer_mid"
                        )
                        MixerBusColumn(
                            label = "AIR/HI",
                            db = uiState.config.mixerHighBusDb,
                            minDb = -15f,
                            maxDb = 12f,
                            isMuted = uiState.config.mixerHighMuted,
                            isEnabled = enabled,
                            accentColor = VioletSpatial,
                            onDbChange = onMixerHighChange,
                            onToggleMute = { onToggleMixerMute("HIGH") },
                            tagPrefix = "mixer_high"
                        )
                        MixerBusColumn(
                            label = "MASTER",
                            db = uiState.config.mixerMasterDb,
                            minDb = -24f,
                            maxDb = 12f,
                            isMuted = false,
                            isEnabled = enabled,
                            accentColor = CoralPeak,
                            onDbChange = onMixerMasterChange,
                            onToggleMute = null,
                            tagPrefix = "mixer_master"
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun ChannelVolumeControlRow(
    channelLabel: String,
    volumeDb: Float,
    isMuted: Boolean,
    isEnabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onVolumeChange: (Float) -> Unit,
    onToggleMute: () -> Unit,
    sliderTag: String,
    muteTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = StudioModule,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag(muteTag)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                    contentDescription = "Mute $channelLabel",
                    tint = if (isMuted) CoralPeak else accentColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = channelLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMuted) CoralPeak else TextPrimary
                    )
                    Text(
                        text = if (isMuted) "MUTED" else "${formatDbShort(volumeDb)} dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isMuted) CoralPeak else accentColor
                    )
                }
                Slider(
                    value = volumeDb,
                    onValueChange = { raw ->
                        val snapped = (raw * 2f).roundToInt() / 2f
                        onVolumeChange(snapped.coerceIn(CHANNEL_VOL_MIN_DB, CHANNEL_VOL_MAX_DB))
                    },
                    valueRange = CHANNEL_VOL_MIN_DB..CHANNEL_VOL_MAX_DB,
                    enabled = isEnabled && !isMuted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(sliderTag),
                    colors = SliderDefaults.colors(
                        thumbColor = accentColor,
                        activeTrackColor = accentColor,
                        inactiveTrackColor = StudioObsidian
                    )
                )
            }
        }
    }
}

@Composable
private fun MixerBusColumn(
    label: String,
    db: Float,
    minDb: Float,
    maxDb: Float,
    isMuted: Boolean,
    isEnabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onDbChange: (Float) -> Unit,
    onToggleMute: (() -> Unit)?,
    tagPrefix: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        VerticalStudioFader(
            label = label,
            valueDb = db,
            minDb = minDb,
            maxDb = maxDb,
            isEnabled = isEnabled && !isMuted,
            accentColor = accentColor,
            onValueChange = onDbChange,
            testTag = "${tagPrefix}_fader",
            heightDp = 152,
            widthDp = 56
        )

        if (onToggleMute != null) {
            Surface(
                onClick = onToggleMute,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("${tagPrefix}_mute"),
                shape = RoundedCornerShape(8.dp),
                color = if (isMuted) CoralPeak.copy(alpha = 0.22f) else StudioModule,
                border = BorderStroke(
                    1.dp,
                    if (isMuted) CoralPeak else StudioBorder
                )
            ) {
                Text(
                    text = if (isMuted) "MUTE" else "ON",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMuted) CoralPeak else TextSecondary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = CoralPeak.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, CoralPeak.copy(alpha = 0.45f))
            ) {
                Text(
                    text = "BUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = CoralPeak,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
