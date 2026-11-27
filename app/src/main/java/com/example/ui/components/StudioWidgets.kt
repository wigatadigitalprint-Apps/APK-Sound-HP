package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.audio.CrossoverSlope
import com.example.audio.DspRuntimeStatus
import com.example.audio.EQ_BAND_LABELS
import com.example.audio.EQ_MAX_DB
import com.example.audio.EQ_MIN_DB
import com.example.ui.theme.AmberWarm
import com.example.ui.theme.CoralPeak
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.EmeraldActive
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioElevated
import com.example.ui.theme.StudioModule
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioRack
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VioletSpatial
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun DspStatusBanner(
    isDspEnabled: Boolean,
    runtime: DspRuntimeStatus,
    onToggleDsp: () -> Unit,
    onOpenYouTube: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasYtSession = runtime.attachedSessions.any { it.isYouTube }
    val statusColor by animateColorAsState(
        targetValue = when {
            !isDspEnabled -> TextMuted
            hasYtSession || runtime.isMediaPlayingOnDevice -> EmeraldActive
            else -> CyanPulse
        },
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioRack,
        border = BorderStroke(
            width = 1.dp,
            color = if (isDspEnabled) statusColor.copy(alpha = 0.45f) else StudioBorder
        ),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDspEnabled) EmeraldGlow else StudioElevated)
                    .border(
                        width = 1.5.dp,
                        color = if (isDspEnabled) statusColor else StudioBorder,
                        shape = CircleShape
                    )
                    .clickable(onClick = onToggleDsp)
                    .testTag("dsp_power_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(R.string.cd_dsp_power),
                    tint = if (isDspEnabled) statusColor else TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                statusColor.copy(alpha = if (isDspEnabled) pulseAlpha else 0.4f)
                            )
                    )
                    Text(
                        text = if (isDspEnabled) {
                            stringResource(R.string.dsp_active_label)
                        } else {
                            stringResource(R.string.dsp_bypassed_label)
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                    val activeSession = runtime.attachedSessions.firstOrNull()
                    if (activeSession != null) {
                        val badgeLabel = when {
                            activeSession.packageName.contains("spotify", ignoreCase = true) -> "SPOTIFY LOCK"
                            activeSession.packageName.contains("gotube", ignoreCase = true) -> "GOTUBE LOCK"
                            activeSession.isYouTube -> "YT LOCK"
                            else -> "SESSION LOCK"
                        }
                        val badgeColor = if (activeSession.packageName.contains("spotify", ignoreCase = true)) {
                            EmeraldActive
                        } else {
                            CoralPeak
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = badgeColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = runtime.activeAppSummary,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            FilledTonalButton(
                onClick = onOpenYouTube,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("open_youtube_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = CoralPeak.copy(alpha = 0.16f),
                    contentColor = CoralPeak
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.cd_open_youtube),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Player",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun FrequencyResponseCanvas(
    leftBandGainsDb: List<Float>,
    rightBandGainsDb: List<Float>,
    linkEqChannels: Boolean,
    isMonoMode: Boolean,
    fftSpectrum: List<Float>,
    isDspEnabled: Boolean,
    liveFftActive: Boolean,
    onRequestLiveFft: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animLeft = leftBandGainsDb.map { target ->
        val animated by animateFloatAsState(
            targetValue = if (isDspEnabled) target else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
            label = "leftGain"
        )
        animated
    }
    val animRight = rightBandGainsDb.map { target ->
        val animated by animateFloatAsState(
            targetValue = if (isDspEnabled) target else 0f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f),
            label = "rightGain"
        )
        animated
    }

    val leftCurveColor = if (isDspEnabled) CyanPulse else TextMuted
    val rightCurveColor = if (isDspEnabled) AmberWarm else TextMuted

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StudioObsidian,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isMonoMode) {
                            "MONO SUM CURVE"
                        } else if (linkEqChannels) {
                            "10-BAND L+R CURVE"
                        } else {
                            "DUAL L / R CURVES"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    if (!linkEqChannels && !isMonoMode) {
                        Text(
                            text = "L",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPulse,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "R",
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberWarm,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (liveFftActive) CyanGlow else StudioModule,
                    border = BorderStroke(
                        1.dp,
                        if (liveFftActive) CyanPulse.copy(alpha = 0.6f) else StudioBorder
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onRequestLiveFft)
                        .testTag("live_fft_chip")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = stringResource(R.string.cd_live_fft),
                            tint = if (liveFftActive) CyanPulse else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (liveFftActive) "LIVE FFT ON" else "ENABLE FFT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (liveFftActive) CyanPulse else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height
                val topPad = 12f
                val bottomPad = 14f
                val drawH = h - topPad - bottomPad

                fun dbToY(db: Float): Float {
                    val norm = (EQ_MAX_DB - db) / (EQ_MAX_DB - EQ_MIN_DB)
                    return topPad + norm * drawH
                }

                listOf(12f, 6f, 0f, -6f, -12f).forEach { db ->
                    val y = dbToY(db)
                    drawLine(
                        color = if (db == 0f) StudioBorder.copy(alpha = 0.95f) else StudioBorder.copy(alpha = 0.35f),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = if (db == 0f) 1.8f else 1f
                    )
                }

                val count = animLeft.size.coerceAtLeast(2)
                val xStep = w / (count - 1).toFloat()
                for (i in 0 until count) {
                    val x = i * xStep
                    drawLine(
                        color = StudioBorder.copy(alpha = 0.28f),
                        start = Offset(x, topPad),
                        end = Offset(x, h - bottomPad),
                        strokeWidth = 1f
                    )
                }

                if (liveFftActive && fftSpectrum.isNotEmpty()) {
                    val barCount = fftSpectrum.size
                    val slotW = w / barCount.toFloat()
                    val barW = (slotW * 0.62f).coerceAtLeast(3f)
                    fftSpectrum.forEachIndexed { idx, mag ->
                        val barH = (mag.coerceIn(0f, 1f) * drawH * 0.85f)
                        if (barH > 2f) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        AmberWarm.copy(alpha = 0.55f),
                                        CyanPulse.copy(alpha = 0.16f)
                                    )
                                ),
                                topLeft = Offset(idx * slotW + (slotW - barW) / 2f, h - bottomPad - barH),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(4f, 4f)
                            )
                        }
                    }
                }

                fun drawEqSpline(gains: List<Float>, color: Color, fillAlpha: Float) {
                    val pts = gains.mapIndexed { i, db -> Offset(i * xStep, dbToY(db)) }
                    val linePath = Path().apply {
                        moveTo(pts.first().x, pts.first().y)
                        for (i in 0 until pts.size - 1) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx = (p0.x + p1.x) / 2f
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }
                    if (fillAlpha > 0f) {
                        val fillPath = Path().apply {
                            addPath(linePath)
                            lineTo(pts.last().x, dbToY(0f))
                            lineTo(pts.first().x, dbToY(0f))
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(color.copy(alpha = fillAlpha), color.copy(alpha = 0.02f))
                            )
                        )
                    }
                    drawPath(
                        path = linePath,
                        color = color,
                        style = Stroke(width = 4.2f, cap = StrokeCap.Round)
                    )
                    pts.forEach { pt ->
                        drawCircle(color = StudioObsidian, radius = 6.5f, center = pt)
                        drawCircle(color = color, radius = 4f, center = pt)
                    }
                }

                if (isMonoMode) {
                    val monoGains = List(10) { i -> (animLeft[i] + animRight[i]) * 0.5f }
                    drawEqSpline(monoGains, leftCurveColor, if (isDspEnabled) 0.25f else 0.08f)
                } else {
                    if (!linkEqChannels) {
                        drawEqSpline(animRight, rightCurveColor, if (isDspEnabled) 0.14f else 0.04f)
                    }
                    drawEqSpline(animLeft, leftCurveColor, if (isDspEnabled) 0.25f else 0.08f)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                EQ_BAND_LABELS.forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
fun CrossoverCurveCanvas(
    crossoverEnabled: Boolean,
    subwooferEnabled: Boolean,
    mainHpfHz: Float,
    subLpfHz: Float,
    midHighSplitHz: Float,
    slope: CrossoverSlope,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StudioObsidian,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "CROSSOVER DSP RESPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Text(
                    text = "${subLpfHz.toInt()}Hz / ${(midHighSplitHz / 1000f).let { String.format("%.1fk", it) }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (crossoverEnabled) CyanPulse else TextMuted
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
            ) {
                val w = size.width
                val h = size.height
                val minLog = log10(20f)
                val maxLog = log10(20000f)
                val steepness = if (slope == CrossoverSlope.SLOPE_24) 4.0 else 2.0

                fun freqToX(hz: Float): Float {
                    val norm = ((log10(hz) - minLog) / (maxLog - minLog)).coerceIn(0f, 1f)
                    return norm * w
                }

                fun dbToY(db: Float): Float {
                    val clamped = db.coerceIn(-24f, 3f)
                    val norm = (3f - clamped) / 27f
                    return 6f + norm * (h - 12f)
                }

                // 0 dB reference line
                val zeroY = dbToY(0f)
                drawLine(
                    color = StudioBorder.copy(alpha = 0.6f),
                    start = Offset(0f, zeroY),
                    end = Offset(w, zeroY),
                    strokeWidth = 1.2f
                )

                val steps = 64
                val subPath = Path()
                val midPath = Path()
                val highPath = Path()

                for (i in 0..steps) {
                    val norm = i.toFloat() / steps.toFloat()
                    val freq = 10f.pow(minLog + norm * (maxLog - minLog))
                    val x = norm * w

                    // Sub LPF response
                    val subRatio = (freq / subLpfHz).toDouble().pow(steepness)
                    val subDb = (-10.0 * log10(1.0 + subRatio)).toFloat()

                    // Mid Bandpass (HPF at mainHpfHz, LPF at midHighSplitHz)
                    val hpfRatio = (mainHpfHz / freq).toDouble().pow(steepness)
                    val midLpfRatio = (freq / midHighSplitHz).toDouble().pow(steepness)
                    val midDb = (-10.0 * log10(1.0 + hpfRatio) - 10.0 * log10(1.0 + midLpfRatio)).toFloat()

                    // High HPF
                    val highRatio = (midHighSplitHz / freq).toDouble().pow(steepness)
                    val highDb = (-10.0 * log10(1.0 + highRatio)).toFloat()

                    if (i == 0) {
                        subPath.moveTo(x, dbToY(subDb))
                        midPath.moveTo(x, dbToY(midDb))
                        highPath.moveTo(x, dbToY(highDb))
                    } else {
                        subPath.lineTo(x, dbToY(subDb))
                        midPath.lineTo(x, dbToY(midDb))
                        highPath.lineTo(x, dbToY(highDb))
                    }
                }

                val activeAlpha = if (crossoverEnabled) 1f else 0.35f
                if (subwooferEnabled) {
                    drawPath(
                        path = subPath,
                        color = AmberWarm.copy(alpha = activeAlpha),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }
                drawPath(
                    path = midPath,
                    color = CyanPulse.copy(alpha = activeAlpha),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = highPath,
                    color = VioletSpatial.copy(alpha = activeAlpha),
                    style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                )

                // Crossover marker lines
                drawLine(
                    color = AmberWarm.copy(alpha = 0.45f),
                    start = Offset(freqToX(subLpfHz), 0f),
                    end = Offset(freqToX(subLpfHz), h),
                    strokeWidth = 1.5f
                )
                drawLine(
                    color = VioletSpatial.copy(alpha = 0.45f),
                    start = Offset(freqToX(midHighSplitHz), 0f),
                    end = Offset(freqToX(midHighSplitHz), h),
                    strokeWidth = 1.5f
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("20Hz", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("SUB", style = MaterialTheme.typography.labelSmall, color = AmberWarm)
                Text("MID", style = MaterialTheme.typography.labelSmall, color = CyanPulse)
                Text("HIGH", style = MaterialTheme.typography.labelSmall, color = VioletSpatial)
                Text("20kHz", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }
}

@Composable
fun VerticalStudioFader(
    label: String,
    valueDb: Float,
    minDb: Float,
    maxDb: Float,
    isEnabled: Boolean,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    heightDp: Int = 172,
    widthDp: Int = 48
) {
    val activeColor = if (isEnabled) accentColor else TextMuted
    var dragValue by remember(valueDb) { mutableFloatStateOf(valueDb) }

    Column(
        modifier = modifier
            .width(widthDp.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = formatDbShort(valueDb),
            style = MaterialTheme.typography.labelSmall,
            color = if (valueDb == 0f) TextMuted else activeColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(widthDp.dp)
                .height(heightDp.dp)
                .pointerInput(minDb, maxDb) {
                    detectTapGestures(
                        onDoubleTap = { onValueChange(0f) },
                        onTap = { offset ->
                            val norm = 1f - (offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            val newDb = minDb + norm * (maxDb - minDb)
                            val snapped = (newDb * 2f).roundToInt() / 2f
                            onValueChange(snapped.coerceIn(minDb, maxDb))
                        }
                    )
                }
                .pointerInput(minDb, maxDb) {
                    detectVerticalDragGestures(
                        onDragStart = { dragValue = valueDb },
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            val deltaDb = (-dragAmount / size.height.toFloat()) * (maxDb - minDb)
                            dragValue = (dragValue + deltaDb).coerceIn(minDb, maxDb)
                            val snapped = (dragValue * 2f).roundToInt() / 2f
                            onValueChange(snapped)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val centerX = size.width / 2f
                val trackTop = 14f
                val trackBottom = size.height - 14f
                val trackH = trackBottom - trackTop
                val trackW = 8f

                fun dbToY(db: Float): Float {
                    val norm = (maxDb - db) / (maxDb - minDb)
                    return trackTop + norm * trackH
                }

                listOf(minDb * 0.5f, 0f, maxDb * 0.5f).forEach { tick ->
                    val ty = dbToY(tick)
                    val tickHalf = if (tick == 0f) 14f else 9f
                    drawLine(
                        color = if (tick == 0f) TextSecondary.copy(alpha = 0.6f) else StudioBorder,
                        start = Offset(centerX - tickHalf, ty),
                        end = Offset(centerX + tickHalf, ty),
                        strokeWidth = if (tick == 0f) 2f else 1.2f
                    )
                }

                drawRoundRect(
                    color = StudioObsidian,
                    topLeft = Offset(centerX - trackW / 2f, trackTop),
                    size = Size(trackW, trackH),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                val zeroY = dbToY(0f.coerceIn(minDb, maxDb))
                val thumbY = dbToY(valueDb.coerceIn(minDb, maxDb))
                val fillTop = minOf(zeroY, thumbY)
                val fillH = kotlin.math.abs(thumbY - zeroY).coerceAtLeast(2f)

                drawRoundRect(
                    color = activeColor.copy(alpha = 0.85f),
                    topLeft = Offset(centerX - trackW / 2f, fillTop),
                    size = Size(trackW, fillH),
                    cornerRadius = CornerRadius(trackW / 2f, trackW / 2f)
                )

                val capW = 30f
                val capH = 18f
                drawRoundRect(
                    color = StudioElevated,
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = activeColor,
                    topLeft = Offset(centerX - capW / 2f, thumbY - capH / 2f),
                    size = Size(capW, capH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 1.6f)
                )
                drawLine(
                    color = activeColor,
                    start = Offset(centerX - 8f, thumbY),
                    end = Offset(centerX + 8f, thumbY),
                    strokeWidth = 2.5f,
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary,
            maxLines = 1
        )
    }
}

@Composable
fun VerticalEqFader(
    freqLabel: String,
    gainDb: Float,
    isDspEnabled: Boolean,
    accentColor: Color = CyanPulse,
    onGainChange: (Float) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    VerticalStudioFader(
        label = freqLabel,
        valueDb = gainDb,
        minDb = EQ_MIN_DB,
        maxDb = EQ_MAX_DB,
        isEnabled = isDspEnabled,
        accentColor = if (gainDb > 8f) AmberWarm else accentColor,
        onValueChange = onGainChange,
        testTag = testTag,
        modifier = modifier
    )
}

@Composable
fun StudioRotaryKnob(
    label: String,
    value: Float,
    accentColor: Color,
    isDspEnabled: Boolean,
    onValueChange: (Float) -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isDspEnabled) accentColor else TextMuted
    var dragValue by remember(value) { mutableFloatStateOf(value) }
    val percent = (value * 100f).roundToInt()

    Surface(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        color = StudioRack,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .size(92.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onValueChange(0f) })
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { dragValue = value },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                val delta = -dragAmount / 180f
                                dragValue = (dragValue + delta).coerceIn(0f, 1f)
                                onValueChange(dragValue)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = 10f
                    val arcPadding = 12f
                    val arcSize = Size(size.width - arcPadding * 2, size.height - arcPadding * 2)
                    val topLeft = Offset(arcPadding, arcPadding)
                    val startAngle = 135f
                    val maxSweep = 270f

                    drawArc(
                        color = StudioObsidian,
                        startAngle = startAngle,
                        sweepAngle = maxSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    val sweep = (value.coerceIn(0f, 1f) * maxSweep).coerceAtLeast(1f)
                    drawArc(
                        color = activeColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )

                    val radius = (size.minDimension / 2f) - 22f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = StudioElevated, radius = radius, center = center)
                    drawCircle(
                        color = StudioBorder,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 1.5f)
                    )

                    val angleRad = ((startAngle + value * maxSweep) * PI / 180.0).toFloat()
                    val innerR = radius * 0.35f
                    val outerR = radius * 0.82f
                    drawLine(
                        color = activeColor,
                        start = Offset(
                            center.x + innerR * cos(angleRad),
                            center.y + innerR * sin(angleRad)
                        ),
                        end = Offset(
                            center.x + outerR * cos(angleRad),
                            center.y + outerR * sin(angleRad)
                        ),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "$percent%",
                style = MaterialTheme.typography.titleMedium,
                color = activeColor
            )
            Text(
                text = "Drag • 2x tap reset",
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
        }
    }
}

fun formatDbShort(db: Float): String {
    val rounded = (db * 10f).roundToInt() / 10f
    return when {
        rounded > 0f -> "+${if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()}"
        rounded < 0f -> if (rounded % 1f == 0f) rounded.toInt().toString() else rounded.toString()
        else -> "0"
    }
}
