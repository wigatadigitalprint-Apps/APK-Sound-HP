package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val StudioDarkColorScheme = darkColorScheme(
    primary = CyanPulse,
    onPrimary = StudioObsidian,
    primaryContainer = CyanMuted.copy(alpha = 0.25f),
    onPrimaryContainer = CyanPulse,
    secondary = AmberWarm,
    onSecondary = StudioObsidian,
    secondaryContainer = AmberWarm.copy(alpha = 0.18f),
    onSecondaryContainer = AmberWarm,
    tertiary = VioletSpatial,
    onTertiary = StudioObsidian,
    tertiaryContainer = VioletSpatial.copy(alpha = 0.20f),
    onTertiaryContainer = VioletSpatial,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioRack,
    onSurface = TextPrimary,
    surfaceVariant = StudioModule,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLow = StudioRack,
    surfaceContainer = StudioModule,
    surfaceContainerHigh = StudioElevated,
    outline = StudioBorder,
    outlineVariant = StudioBorder.copy(alpha = 0.55f),
    error = CoralPeak,
    onError = StudioObsidian
)

private val StudioShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun EqProAudioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StudioDarkColorScheme,
        typography = EqStudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
