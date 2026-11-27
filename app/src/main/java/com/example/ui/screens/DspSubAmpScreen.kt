package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AmpClassMode
import com.example.audio.AudioOutputMode
import com.example.audio.CrossoverSlope
import com.example.audio.SpeakerProfile
import com.example.audio.TestToneType
import com.example.ui.EqUiState
import com.example.ui.components.CrossoverCurveCanvas
import com.example.ui.components.StudioRotaryKnob
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DspSubAmpScreen(
    uiState: EqUiState,
    onSubwooferEnabledChange: (Boolean) -> Unit,
    onSubGainChange: (Float) -> Unit,
    onSubPunchChange: (Float) -> Unit,
    onSubPhaseChange: (Boolean) -> Unit,
    onCrossoverEnabledChange: (Boolean) -> Unit,
    onSelectSpeakerProfile: (SpeakerProfile) -> Unit,
    onMainHpfChange: (Float) -> Unit,
    onSubLpfChange: (Float) -> Unit,
    onMidHighSplitChange: (Float) -> Unit,
    onSlopeChange: (CrossoverSlope) -> Unit,
    onSpeakerDelayChange: (Float) -> Unit,
    onPowerAmpEnabledChange: (Boolean) -> Unit,
    onAmpClassChange: (AmpClassMode) -> Unit,
    onAmpDriveChange: (Float) -> Unit,
    onLimiterEnabledChange: (Boolean) -> Unit,
    onLimiterThresholdChange: (Float) -> Unit,
    onBassBoostChange: (Float) -> Unit,
    onVirtualizerChange: (Float) -> Unit,
    onLoudnessChange: (Float) -> Unit,
    onMbcPunchChange: (Float) -> Unit,
    onToggleTestTone: (TestToneType) -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.config
    val enabled = cfg.isDspEnabled
    val isMono = cfg.outputMode == AudioOutputMode.MONO

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Dedicated Subwoofer DSP Console
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioRack,
            border = BorderStroke(
                1.dp,
                if (cfg.subwooferEnabled && enabled) AmberWarm.copy(alpha = 0.55f) else StudioBorder
            )
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
                            text = "SUBWOOFER DSP ENGINE",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Deep sub-bass shelf, excursion punch & phase polarity",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = cfg.subwooferEnabled,
                        onCheckedChange = onSubwooferEnabledChange,
                        enabled = enabled,
                        modifier = Modifier.testTag("subwoofer_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioObsidian,
                            checkedTrackColor = AmberWarm
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SUBWOOFER GAIN", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = "${formatDbShort(cfg.subGainDb)} dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cfg.subwooferEnabled && enabled) AmberWarm else TextMuted
                    )
                }
                Slider(
                    value = cfg.subGainDb,
                    onValueChange = { onSubGainChange((it * 2f).roundToInt() / 2f) },
                    valueRange = -12f..15f,
                    enabled = enabled && cfg.subwooferEnabled,
                    modifier = Modifier.testTag("sub_gain_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberWarm,
                        activeTrackColor = AmberWarm,
                        inactiveTrackColor = StudioObsidian
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("SUB PUNCH / EXCURSION", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = "${(cfg.subPunch * 100f).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cfg.subwooferEnabled && enabled) AmberWarm else TextMuted
                    )
                }
                Slider(
                    value = cfg.subPunch,
                    onValueChange = onSubPunchChange,
                    valueRange = 0f..1f,
                    enabled = enabled && cfg.subwooferEnabled,
                    modifier = Modifier.testTag("sub_punch_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberWarm,
                        activeTrackColor = AmberWarm,
                        inactiveTrackColor = StudioObsidian
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !cfg.subPhaseInverted,
                        onClick = { onSubPhaseChange(false) },
                        enabled = enabled && cfg.subwooferEnabled,
                        modifier = Modifier.testTag("sub_phase_0"),
                        label = { Text("Phase 0°", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGlow,
                            selectedLabelColor = AmberWarm
                        )
                    )
                    FilterChip(
                        selected = cfg.subPhaseInverted,
                        onClick = { onSubPhaseChange(true) },
                        enabled = enabled && cfg.subwooferEnabled,
                        modifier = Modifier.testTag("sub_phase_180"),
                        label = { Text("Phase 180°", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberGlow,
                            selectedLabelColor = AmberWarm
                        )
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    val subTesting = uiState.activeTestTone == TestToneType.SUB_BASS
                    FilledTonalButton(
                        onClick = { onToggleTestTone(TestToneType.SUB_BASS) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("sub_test_65hz_button"),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (subTesting) AmberGlow else StudioModule,
                            contentColor = if (subTesting) AmberWarm else TextPrimary
                        )
                    ) {
                        Icon(
                            imageVector = if (subTesting) Icons.Filled.Stop else Icons.Filled.Audiotrack,
                            contentDescription = "Test 65Hz Sub Bass",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (subTesting) "Stop 65Hz" else "Test 65Hz")
                    }
                }
            }
        }

        // 2. Crossover / Speaker Management (DSP)
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
                            text = "CROSSOVER / MANAGEMENT SPEAKER",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "Active 3-way HPF/LPF band split & speaker protection",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = cfg.crossoverEnabled,
                        onCheckedChange = onCrossoverEnabledChange,
                        enabled = enabled,
                        modifier = Modifier.testTag("crossover_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioObsidian,
                            checkedTrackColor = CyanPulse
                        )
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(SpeakerProfile.entries) { profile ->
                        val selected = cfg.speakerProfile == profile
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSpeakerProfile(profile) },
                            modifier = Modifier.testTag("speaker_profile_${profile.name}"),
                            label = { Text(profile.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioObsidian,
                                labelColor = TextSecondary,
                                selectedContainerColor = CyanGlow,
                                selectedLabelColor = CyanPulse
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = StudioBorder,
                                selectedBorderColor = CyanPulse
                            )
                        )
                    }
                }

                CrossoverCurveCanvas(
                    crossoverEnabled = cfg.crossoverEnabled && enabled,
                    subwooferEnabled = cfg.subwooferEnabled,
                    mainHpfHz = cfg.mainHpfHz,
                    subLpfHz = cfg.subLpfHz,
                    midHighSplitHz = cfg.midHighSplitHz,
                    slope = cfg.crossoverSlope
                )

                DspParamSlider(
                    title = "SUBWOOFER LPF CUTOFF",
                    valueText = "${cfg.subLpfHz.roundToInt()} Hz",
                    value = cfg.subLpfHz,
                    range = 40f..180f,
                    enabled = enabled && cfg.crossoverEnabled,
                    accentColor = AmberWarm,
                    onValueChange = onSubLpfChange,
                    testTag = "xover_sub_lpf_slider"
                )

                DspParamSlider(
                    title = "MAIN SPEAKER HPF",
                    valueText = "${cfg.mainHpfHz.roundToInt()} Hz",
                    value = cfg.mainHpfHz,
                    range = 25f..220f,
                    enabled = enabled && cfg.crossoverEnabled,
                    accentColor = CyanPulse,
                    onValueChange = onMainHpfChange,
                    testTag = "xover_main_hpf_slider"
                )

                DspParamSlider(
                    title = "MID / HIGH CROSSOVER",
                    valueText = "${cfg.midHighSplitHz.roundToInt()} Hz",
                    value = cfg.midHighSplitHz,
                    range = 1200f..6500f,
                    enabled = enabled && cfg.crossoverEnabled,
                    accentColor = VioletSpatial,
                    onValueChange = onMidHighSplitChange,
                    testTag = "xover_mid_high_slider"
                )

                DspParamSlider(
                    title = "ALIGNMENT DELAY",
                    valueText = "${(cfg.speakerDelayMs * 10f).roundToInt() / 10f} ms",
                    value = cfg.speakerDelayMs,
                    range = 0f..20f,
                    enabled = enabled && cfg.crossoverEnabled,
                    accentColor = EmeraldActive,
                    onValueChange = { onSpeakerDelayChange((it * 10f).roundToInt() / 10f) },
                    testTag = "xover_delay_slider"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CrossoverSlope.entries.forEach { slope ->
                        val selected = cfg.crossoverSlope == slope
                        FilterChip(
                            selected = selected,
                            onClick = { onSlopeChange(slope) },
                            enabled = enabled && cfg.crossoverEnabled,
                            modifier = Modifier.testTag("xover_slope_${slope.name}"),
                            label = { Text(slope.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyanGlow,
                                selectedLabelColor = CyanPulse
                            )
                        )
                    }
                }
            }
        }

        // 3. Studio Power Amplifier Stage
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
                            text = "POWER AMPLIFIER STAGE",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = cfg.ampClassMode.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = cfg.powerAmpEnabled,
                        onCheckedChange = onPowerAmpEnabledChange,
                        enabled = enabled,
                        modifier = Modifier.testTag("power_amp_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioObsidian,
                            checkedTrackColor = EmeraldActive
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AmpClassMode.entries.forEach { mode ->
                        val selected = cfg.ampClassMode == mode
                        FilterChip(
                            selected = selected,
                            onClick = { onAmpClassChange(mode) },
                            enabled = enabled && cfg.powerAmpEnabled,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("amp_class_${mode.name}"),
                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioObsidian,
                                labelColor = TextSecondary,
                                selectedContainerColor = CyanGlow,
                                selectedLabelColor = CyanPulse
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = StudioBorder,
                                selectedBorderColor = CyanPulse
                            )
                        )
                    }
                }

                // Output Power Drive + Headroom Bar
                val estimatedWatts = if (enabled && cfg.powerAmpEnabled) {
                    (25 + cfg.ampDrive * 225).roundToInt()
                } else 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("AMP DRIVE & OUTPUT", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text(
                        text = "${(cfg.ampDrive * 100f).roundToInt()}% • ${estimatedWatts}W Peak",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (cfg.ampDrive > 0.78f) AmberWarm else EmeraldActive
                    )
                }
                LinearProgressIndicator(
                    progress = { if (enabled && cfg.powerAmpEnabled) cfg.ampDrive.coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (cfg.ampDrive > 0.8f) CoralPeak else EmeraldActive,
                    trackColor = StudioObsidian
                )
                Slider(
                    value = cfg.ampDrive,
                    onValueChange = onAmpDriveChange,
                    valueRange = 0f..1f,
                    enabled = enabled && cfg.powerAmpEnabled,
                    modifier = Modifier.testTag("amp_drive_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldActive,
                        activeTrackColor = EmeraldActive,
                        inactiveTrackColor = StudioObsidian
                    )
                )

                // Peak Limiter Ceiling inside Power Amp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CLIP GUARD LIMITER (${(cfg.limiterThresholdDb * 10f).roundToInt() / 10f} dB)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Switch(
                        checked = cfg.limiterEnabled,
                        onCheckedChange = onLimiterEnabledChange,
                        enabled = enabled,
                        modifier = Modifier.testTag("limiter_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = StudioObsidian,
                            checkedTrackColor = AmberWarm
                        )
                    )
                }
                Slider(
                    value = cfg.limiterThresholdDb,
                    onValueChange = { onLimiterThresholdChange((it * 2f).roundToInt() / 2f) },
                    valueRange = -12f..0f,
                    enabled = enabled && cfg.limiterEnabled,
                    modifier = Modifier.testTag("limiter_threshold_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberWarm,
                        activeTrackColor = AmberWarm,
                        inactiveTrackColor = StudioObsidian
                    )
                )
            }
        }

        // 4. Rotary Tone & Spatial FX Knobs
        Text(
            text = "SPATIAL & DYNAMICS KNOBS",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val fourColumns = maxWidth >= 620.dp
            if (fourColumns) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudioRotaryKnob(
                        label = "Bass Boost",
                        value = cfg.bassBoost,
                        accentColor = CyanPulse,
                        isDspEnabled = enabled,
                        onValueChange = onBassBoostChange,
                        testTag = "knob_bass_boost",
                        modifier = Modifier.weight(1f)
                    )
                    StudioRotaryKnob(
                        label = if (isMono) "3D (Mono Off)" else "3D Soundstage",
                        value = if (isMono) 0f else cfg.virtualizer,
                        accentColor = VioletSpatial,
                        isDspEnabled = enabled && !isMono,
                        onValueChange = onVirtualizerChange,
                        testTag = "knob_virtualizer",
                        modifier = Modifier.weight(1f)
                    )
                    StudioRotaryKnob(
                        label = "Loudness",
                        value = cfg.loudness,
                        accentColor = EmeraldActive,
                        isDspEnabled = enabled,
                        onValueChange = onLoudnessChange,
                        testTag = "knob_loudness",
                        modifier = Modifier.weight(1f)
                    )
                    StudioRotaryKnob(
                        label = "MBC Punch",
                        value = cfg.mbcPunch,
                        accentColor = AmberWarm,
                        isDspEnabled = enabled,
                        onValueChange = onMbcPunchChange,
                        testTag = "knob_mbc_punch",
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioRotaryKnob(
                            label = "Bass Boost",
                            value = cfg.bassBoost,
                            accentColor = CyanPulse,
                            isDspEnabled = enabled,
                            onValueChange = onBassBoostChange,
                            testTag = "knob_bass_boost",
                            modifier = Modifier.weight(1f)
                        )
                        StudioRotaryKnob(
                            label = if (isMono) "3D (Mono Off)" else "3D Soundstage",
                            value = if (isMono) 0f else cfg.virtualizer,
                            accentColor = VioletSpatial,
                            isDspEnabled = enabled && !isMono,
                            onValueChange = onVirtualizerChange,
                            testTag = "knob_virtualizer",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioRotaryKnob(
                            label = "Loudness",
                            value = cfg.loudness,
                            accentColor = EmeraldActive,
                            isDspEnabled = enabled,
                            onValueChange = onLoudnessChange,
                            testTag = "knob_loudness",
                            modifier = Modifier.weight(1f)
                        )
                        StudioRotaryKnob(
                            label = "MBC Punch",
                            value = cfg.mbcPunch,
                            accentColor = AmberWarm,
                            isDspEnabled = enabled,
                            onValueChange = onMbcPunchChange,
                            testTag = "knob_mbc_punch",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 5. Studio Reference Audition Deck
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioModule,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "STUDIO REFERENCE AUDITION",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TestToneType.entries.forEach { tone ->
                        val isPlaying = uiState.activeTestTone == tone
                        FilledTonalButton(
                            onClick = { onToggleTestTone(tone) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("test_tone_${tone.name}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isPlaying) CyanGlow else StudioRack,
                                contentColor = if (isPlaying) CyanPulse else TextSecondary
                            )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.Audiotrack,
                                contentDescription = stringResource(R.string.cd_test_audio),
                                modifier = Modifier.size(16.dp),
                                tint = if (isPlaying) CoralPeak else TextSecondary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(tone.label, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun DspParamSlider(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    accentColor: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit,
    testTag: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelSmall,
                color = if (enabled) accentColor else TextMuted
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onValueChange,
            valueRange = range,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = StudioObsidian
            )
        )
    }
}
