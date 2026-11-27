package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.audio.AudioOutputMode
import com.example.audio.CHANNEL_VOL_MAX_DB
import com.example.audio.CHANNEL_VOL_MIN_DB
import com.example.audio.EQ_BAND_LABELS
import com.example.audio.EqPreset
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
import kotlin.math.roundToInt

enum class InAppMediaSource(
    val label: String,
    val url: String,
    val useDesktopUserAgent: Boolean
) {
    YOUTUBE("YouTube", "https://m.youtube.com", false),
    YT_MUSIC("YT Music", "https://music.youtube.com", false),
    SPOTIFY_WEB("Spotify Web", "https://open.spotify.com", true)
}

private const val DESKTOP_CHROME_UA =
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

private val ALLOWED_MEDIA_HOSTS = setOf(
    "youtube.com",
    "m.youtube.com",
    "www.youtube.com",
    "music.youtube.com",
    "youtu.be",
    "accounts.google.com",
    "accounts.youtube.com",
    "consent.youtube.com",
    "open.spotify.com",
    "accounts.spotify.com",
    "www.spotify.com",
    "spotify.com",
    "login.spotify.com",
    "challenge.spotify.com"
)

fun configureWebViewForSource(webView: WebView, source: InAppMediaSource, defaultMobileUa: String) {
    webView.settings.userAgentString = if (source.useDesktopUserAgent) {
        DESKTOP_CHROME_UA
    } else {
        defaultMobileUa
    }
    webView.loadUrl(source.url)
}

@SuppressLint("SetJavaScriptEnabled")
fun createEmbeddedYouTubeWebView(
    context: Context,
    onTitleChanged: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    onCanGoBackChanged: (Boolean) -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        setBackgroundColor(AndroidColor.parseColor("#0B0E14"))
        setLayerType(View.LAYER_TYPE_HARDWARE, null)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = false
            displayZoomControls = false
            allowFileAccess = false
            allowContentAccess = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }

        webChromeClient = object : WebChromeClient() {
            override fun onReceivedTitle(view: WebView?, title: String?) {
                if (!title.isNullOrBlank()) {
                    onTitleChanged(title)
                }
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                val originHost = request?.origin?.host?.lowercase().orEmpty()
                val isAllowedOrigin = ALLOWED_MEDIA_HOSTS.any {
                    originHost == it || originHost.endsWith(".$it")
                }
                if (isAllowedOrigin &&
                    request != null &&
                    request.resources.contains(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID)
                ) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID))
                } else {
                    request?.deny()
                }
            }
        }

        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val uri = request?.url ?: return false
                val host = uri.host?.lowercase() ?: return false
                val allowed = ALLOWED_MEDIA_HOSTS.any { host == it || host.endsWith(".$it") }
                if (allowed) {
                    return false
                }
                if (uri.scheme == "https" || uri.scheme == "http") {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (!url.isNullOrBlank()) {
                    onUrlChanged(url)
                }
                onCanGoBackChanged(view?.canGoBack() == true)
            }
        }
    }
}

@Composable
fun YouTubeTopToolbar(
    canGoBack: Boolean,
    activeSource: InAppMediaSource,
    isSplitEqMode: Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onSelectSource: (InAppMediaSource) -> Unit,
    onToggleSplitEq: () -> Unit,
    onLaunchGoTube: () -> Unit,
    onLaunchSpotifyApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioRack,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    enabled = canGoBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("yt_web_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (canGoBack) TextPrimary else TextMuted
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InAppMediaSource.entries.forEach { source ->
                        val selected = activeSource == source
                        val accent = if (source == InAppMediaSource.SPOTIFY_WEB) EmeraldActive else CoralPeak
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectSource(source) },
                            modifier = Modifier.testTag("media_tab_${source.name}"),
                            label = { Text(source.label, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = StudioObsidian,
                                labelColor = TextSecondary,
                                selectedContainerColor = accent.copy(alpha = 0.18f),
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

                    FilterChip(
                        selected = false,
                        onClick = onLaunchGoTube,
                        modifier = Modifier.testTag("player_open_gotube"),
                        label = { Text("GoTube App", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StudioObsidian,
                            labelColor = AmberWarm
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = AmberWarm.copy(alpha = 0.45f)
                        )
                    )

                    FilterChip(
                        selected = false,
                        onClick = onLaunchSpotifyApp,
                        modifier = Modifier.testTag("player_open_spotify_app"),
                        label = { Text("Spotify App", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StudioObsidian,
                            labelColor = EmeraldActive
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = EmeraldActive.copy(alpha = 0.45f)
                        )
                    )
                }

                FilterChip(
                    selected = isSplitEqMode,
                    onClick = onToggleSplitEq,
                    modifier = Modifier.testTag("yt_toggle_split_eq"),
                    leadingIcon = {
                        Icon(
                            imageVector = if (isSplitEqMode) Icons.Filled.Equalizer else Icons.Filled.Fullscreen,
                            contentDescription = "Toggle Split EQ View",
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = if (isSplitEqMode) "Split" else "Full",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = StudioObsidian,
                        labelColor = TextSecondary,
                        selectedContainerColor = CyanGlow,
                        selectedLabelColor = CyanPulse
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSplitEqMode,
                        borderColor = StudioBorder,
                        selectedBorderColor = CyanPulse
                    )
                )

                IconButton(
                    onClick = onReload,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("yt_web_reload")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Reload Player",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun YouTubeMiniDockControls(
    sourceLabel: String,
    pageTitle: String,
    isDspEnabled: Boolean,
    onExpandYouTube: () -> Unit,
    onCloseDock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "IN-APP ${sourceLabel.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sourceLabel.contains("Spotify", ignoreCase = true)) EmeraldActive else CoralPeak
                )
                Text(
                    text = if (isDspEnabled) "• DSP ON" else "• BYPASS",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDspEnabled) EmeraldActive else TextMuted
                )
            }
            Text(
                text = pageTitle.ifBlank { "$sourceLabel Audio Active" },
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onExpandYouTube,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("yt_dock_expand")
        ) {
            Icon(
                imageVector = Icons.Filled.OpenInFull,
                contentDescription = "Expand Media Player",
                tint = CyanPulse
            )
        }

        IconButton(
            onClick = onCloseDock,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("yt_dock_close")
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Stop In-App Player",
                tint = TextSecondary
            )
        }
    }
}

@Composable
fun QuickSplitDspDeck(
    uiState: EqUiState,
    onToggleDsp: () -> Unit,
    onSelectOutputMode: (AudioOutputMode) -> Unit,
    onSelectPreset: (EqPreset) -> Unit,
    onBandGainChange: (Int, Float) -> Unit,
    onSubGainChange: (Float) -> Unit,
    onLeftVolumeChange: (Float) -> Unit,
    onRightVolumeChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val cfg = uiState.config
    val enabled = cfg.isDspEnabled

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = StudioRack,
        border = BorderStroke(1.dp, StudioBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onToggleDsp,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("split_dsp_power"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (enabled) EmeraldGlow else StudioModule,
                    border = BorderStroke(1.dp, if (enabled) EmeraldActive else StudioBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PowerSettingsNew,
                            contentDescription = "Toggle DSP",
                            tint = if (enabled) EmeraldActive else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (enabled) "DSP ON" else "OFF",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (enabled) EmeraldActive else TextMuted
                        )
                    }
                }

                AudioOutputMode.entries.forEach { mode ->
                    val selected = cfg.outputMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectOutputMode(mode) },
                        modifier = Modifier.testTag("split_mode_${mode.name}"),
                        label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = if (mode == AudioOutputMode.MONO) AmberGlow else CyanGlow,
                            selectedLabelColor = if (mode == AudioOutputMode.MONO) AmberWarm else CyanPulse
                        )
                    )
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(uiState.allPresets, key = { it.id }) { preset ->
                    val selected = cfg.activePresetId == preset.id
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.name, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = StudioObsidian,
                            selectedContainerColor = CyanGlow,
                            selectedLabelColor = CyanPulse
                        )
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EQ_BAND_LABELS.forEachIndexed { idx, label ->
                    VerticalStudioFader(
                        label = label,
                        valueDb = cfg.leftBandGainsDb.getOrElse(idx) { 0f },
                        minDb = -15f,
                        maxDb = 15f,
                        isEnabled = enabled,
                        accentColor = CyanPulse,
                        onValueChange = { onBandGainChange(idx, it) },
                        testTag = "split_eq_fader_$idx",
                        heightDp = 112,
                        widthDp = 44
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("SUB GAIN", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text("${formatDbShort(cfg.subGainDb)}dB", style = MaterialTheme.typography.labelSmall, color = AmberWarm)
                    }
                    Slider(
                        value = cfg.subGainDb,
                        onValueChange = { onSubGainChange((it * 2f).roundToInt() / 2f) },
                        valueRange = -12f..15f,
                        enabled = enabled && cfg.subwooferEnabled,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberWarm,
                            activeTrackColor = AmberWarm,
                            inactiveTrackColor = StudioObsidian
                        )
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("L / R VOL", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text = "${formatDbShort(cfg.leftVolumeDb)} / ${formatDbShort(cfg.rightVolumeDb)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPulse
                        )
                    }
                    Slider(
                        value = cfg.leftVolumeDb,
                        onValueChange = { raw ->
                            val snapped = (raw * 2f).roundToInt() / 2f
                            onLeftVolumeChange(snapped)
                            if (cfg.linkLrVolume) onRightVolumeChange(snapped)
                        },
                        valueRange = CHANNEL_VOL_MIN_DB..CHANNEL_VOL_MAX_DB,
                        enabled = enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = CyanPulse,
                            activeTrackColor = CyanPulse,
                            inactiveTrackColor = StudioObsidian
                        )
                    )
                }
            }
        }
    }
}
