package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.AudioOutputMode
import com.example.audio.EQ_BAND_LABELS
import com.example.audio.EqChannelEditTarget
import com.example.audio.EqPreset
import com.example.audio.PREAMP_MAX_DB
import com.example.audio.PREAMP_MIN_DB
import com.example.ui.EqUiState
import com.example.ui.components.DspStatusBanner
import com.example.ui.components.FrequencyResponseCanvas
import com.example.ui.components.VerticalEqFader
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioModule
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    uiState: EqUiState,
    onToggleDsp: () -> Unit,
    onOpenYouTube: () -> Unit,
    onRequestLiveFft: () -> Unit,
    onSelectOutputMode: (AudioOutputMode) -> Unit,
    onSelectEqChannelTarget: (EqChannelEditTarget) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onDeletePreset: (String) -> Unit,
    onSavePreset: (String) -> Unit,
    onResetFlat: () -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onPreAmpChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var presetNameInput by remember { mutableStateOf("") }

    val isMono = uiState.config.outputMode == AudioOutputMode.MONO
    val activeChannelColor = if (!uiState.config.linkEqChannels &&
        uiState.selectedEqChannel == EqChannelEditTarget.RIGHT
    ) {
        AmberWarm
    } else {
        CyanPulse
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DspStatusBanner(
            isDspEnabled = uiState.config.isDspEnabled,
            runtime = uiState.runtime,
            onToggleDsp = onToggleDsp,
            onOpenYouTube = onOpenYouTube
        )

        FrequencyResponseCanvas(
            leftBandGainsDb = uiState.config.leftBandGainsDb,
            rightBandGainsDb = uiState.config.rightBandGainsDb,
            linkEqChannels = uiState.config.linkEqChannels,
            isMonoMode = isMono,
            fftSpectrum = uiState.fftSpectrum,
            isDspEnabled = uiState.config.isDspEnabled,
            liveFftActive = uiState.runtime.liveFftActive,
            onRequestLiveFft = onRequestLiveFft
        )

        // Stereo / Mono + Dual L/R EQ Channel Selector Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StudioRack,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "OUTPUT & CHANNEL EQ",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AudioOutputMode.entries.forEach { mode ->
                            val selected = uiState.config.outputMode == mode
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectOutputMode(mode) },
                                modifier = Modifier.testTag("eq_mode_${mode.name}"),
                                label = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = StudioObsidian,
                                    labelColor = TextSecondary,
                                    selectedContainerColor = if (mode == AudioOutputMode.MONO) AmberGlow else CyanGlow,
                                    selectedLabelColor = if (mode == AudioOutputMode.MONO) AmberWarm else CyanPulse
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = StudioBorder,
                                    selectedBorderColor = if (mode == AudioOutputMode.MONO) AmberWarm else CyanPulse
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EqChannelEditTarget.entries.forEach { target ->
                        val selected = uiState.selectedEqChannel == target
                        val chipColor = if (target == EqChannelEditTarget.RIGHT) AmberWarm else CyanPulse
                        val chipGlow = if (target == EqChannelEditTarget.RIGHT) AmberGlow else CyanGlow
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectEqChannelTarget(target) },
                            enabled = !isMono || target == EqChannelEditTarget.LINKED,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("eq_channel_${target.name}"),
                            leadingIcon = if (target == EqChannelEditTarget.LINKED) {
                                {
                                    Icon(
                                        imageVector = if (uiState.config.linkEqChannels) Icons.Filled.Link else Icons.Filled.LinkOff,
                                        contentDescription = target.label,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else null,
                            label = {
                                Text(
                                    text = target.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioObsidian,
                                labelColor = TextSecondary,
                                selectedContainerColor = chipGlow,
                                selectedLabelColor = chipColor
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = StudioBorder,
                                selectedBorderColor = chipColor
                            )
                        )
                    }
                }
            }
        }

        // Preset selector row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "STUDIO PRESETS",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("save_preset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(R.string.cd_save_preset),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.preset_save))
                }
                IconButton(
                    onClick = onResetFlat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("reset_flat_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = stringResource(R.string.cd_reset_eq),
                        tint = TextSecondary
                    )
                }
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 2.dp)
        ) {
            items(uiState.allPresets, key = { it.id }) { preset ->
                val selected = uiState.config.activePresetId == preset.id
                FilterChip(
                    selected = selected,
                    onClick = { onSelectPreset(preset) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("preset_chip_${preset.id}"),
                    label = {
                        Text(
                            text = preset.name,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    trailingIcon = if (!preset.isBuiltIn) {
                        {
                            IconButton(
                                onClick = { onDeletePreset(preset.id) },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_delete_preset),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = StudioRack,
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

        // 10-Band Precision Studio Fader Bank
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioRack,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isMono -> "10-BAND MONO SUM EQ"
                            uiState.config.linkEqChannels -> "10-BAND L+R EQUALIZER"
                            uiState.selectedEqChannel == EqChannelEditTarget.RIGHT -> "RIGHT CHANNEL (R) EQ"
                            else -> "LEFT CHANNEL (L) EQ"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = activeChannelColor
                    )
                    Text(
                        text = "2x tap fader = 0 dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))

                val displayedGains = uiState.activeDisplayedBandGains
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val fitsAll = maxWidth >= 500.dp
                    val faderRowModifier = if (fitsAll) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.horizontalScroll(rememberScrollState())
                    }

                    Row(
                        modifier = faderRowModifier,
                        horizontalArrangement = if (fitsAll) {
                            Arrangement.SpaceBetween
                        } else {
                            Arrangement.spacedBy(4.dp)
                        },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EQ_BAND_LABELS.forEachIndexed { idx, label ->
                            VerticalEqFader(
                                freqLabel = label,
                                gainDb = displayedGains.getOrElse(idx) { 0f },
                                isDspEnabled = uiState.config.isDspEnabled,
                                accentColor = activeChannelColor,
                                onGainChange = { newDb -> onBandGainChange(idx, newDb) },
                                testTag = "eq_fader_$idx"
                            )
                        }
                    }
                }
            }
        }

        // Master Input Pre-Amp Module
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StudioModule,
            border = BorderStroke(1.dp, StudioBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INPUT PRE-AMP GAIN",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    val roundedPreAmp = (uiState.config.preAmpDb * 10f).roundToInt() / 10f
                    Text(
                        text = if (roundedPreAmp > 0f) "+$roundedPreAmp dB" else "$roundedPreAmp dB",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (roundedPreAmp > 3f) AmberWarm else CyanPulse
                    )
                }
                Slider(
                    value = uiState.config.preAmpDb,
                    onValueChange = { raw ->
                        val snapped = (raw * 2f).roundToInt() / 2f
                        onPreAmpChange(snapped.coerceIn(PREAMP_MIN_DB, PREAMP_MAX_DB))
                    },
                    valueRange = PREAMP_MIN_DB..PREAMP_MAX_DB,
                    enabled = uiState.config.isDspEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preamp_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = CyanPulse,
                        activeTrackColor = CyanPulse,
                        inactiveTrackColor = StudioObsidian
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = StudioModule,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Save Custom Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Save your current 10-band curve, bass boost, and virtualizer profile.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = presetNameInput,
                        onValueChange = { presetNameInput = it },
                        singleLine = true,
                        label = { Text("Preset Name") },
                        placeholder = { Text("e.g. YouTube Lo-Fi") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("preset_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetNameInput.isNotBlank()) {
                            onSavePreset(presetNameInput)
                            presetNameInput = ""
                            showSaveDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_save_preset")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
