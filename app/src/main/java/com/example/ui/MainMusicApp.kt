package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Headset
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.DacConfig
import com.example.data.Song
import com.example.ui.theme.AppleMusicPink
import com.example.ui.theme.AppleMusicRed
import com.example.ui.theme.AudiophileGold
import com.example.ui.theme.HiResBlue
import com.example.viewmodel.MusicViewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMusicApp(
    viewModel: MusicViewModel,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val favoriteSongs by viewModel.favoriteSongs.collectAsStateWithLifecycle()
    val dacConfig by viewModel.dacConfig.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val progress by viewModel.playbackProgress.collectAsStateWithLifecycle()
    val isExpanded by viewModel.isNowPlayingExpanded.collectAsStateWithLifecycle()
    val latencyMs by viewModel.latencyMs.collectAsStateWithLifecycle()
    val spectrumData by viewModel.spectrumData.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()

    var showDacSettingsDialog by remember { mutableStateOf(false) }
    var showEqSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Persistent Custom Bottom Navigation Bar
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 8.dp,
                    windowInsets = WindowInsets.navigationBars,
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = activeTab == "listen_now",
                        onClick = { viewModel.selectTab("listen_now") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "listen_now") Icons.Filled.Headset else Icons.Outlined.Headset,
                                contentDescription = "Listen Now"
                            )
                        },
                        label = { Text("Listen Now", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == "browse",
                        onClick = { viewModel.selectTab("browse") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "browse") Icons.Filled.Explore else Icons.Outlined.Explore,
                                contentDescription = "Browse"
                            )
                        },
                        label = { Text("Browse", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                    NavigationBarItem(
                        selected = activeTab == "library",
                        onClick = { viewModel.selectTab("library") },
                        icon = {
                            Icon(
                                imageVector = if (activeTab == "library") Icons.Filled.LibraryMusic else Icons.Outlined.LibraryMusic,
                                contentDescription = "Library"
                            )
                        },
                        label = { Text("Library", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AppleMusicPink,
                            selectedTextColor = AppleMusicPink,
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            val maxHeight = maxHeight
            val minHeight = 68.dp // Mini Player height
            
            // Background Tab Screens
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(bottom = minHeight) // Leave space for mini player
            ) {
                when (activeTab) {
                    "listen_now" -> ListenNowScreen(
                        songs = songs,
                        dacConfig = dacConfig,
                        onSongSelect = { viewModel.selectSong(it) },
                        onDacPanelClick = { showDacSettingsDialog = true },
                        onToggleBypass = { viewModel.toggleDacBypass() }
                    )
                    "browse" -> BrowseScreen(
                        songs = songs,
                        onSongSelect = { viewModel.selectSong(it) }
                    )
                    "library" -> {
                        val storagePermissionGranted by viewModel.storagePermissionGranted.collectAsStateWithLifecycle()
                        val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
                        val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
                        
                        LibraryScreen(
                            favoriteSongs = favoriteSongs,
                            onSongSelect = { viewModel.selectSong(it) },
                            onDacPanelClick = { showDacSettingsDialog = true },
                            storagePermissionGranted = storagePermissionGranted,
                            isScanning = isScanning,
                            scanProgress = scanProgress,
                            onGrantPermission = { viewModel.setStoragePermissionGranted(true) },
                            onScanClick = { viewModel.scanLocalOfflineMusic() }
                        )
                    }
                }
            }

            // Expandable Now Playing Sheet (Sliding Panel)
            val slideOffset by animateDpAsState(
                targetValue = if (isExpanded) 0.dp else maxHeight - minHeight,
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessLow),
                label = "NowPlayingSlide"
            )

            Box(
                modifier = Modifier
                    .offset(y = slideOffset)
                    .fillMaxWidth()
                    .height(maxHeight)
                    .clip(
                        if (isExpanded) RoundedCornerShape(0.dp) else RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp
                        )
                    )
                    .background(
                        if (isExpanded) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.surface.copy(
                            alpha = 0.97f
                        )
                    )
                    .border(
                        width = if (isExpanded) 0.dp else 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Close if dragged down substantially
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (dragAmount.y > 15 && isExpanded) {
                                    viewModel.setNowPlayingExpanded(false)
                                } else if (dragAmount.y < -15 && !isExpanded) {
                                    viewModel.setNowPlayingExpanded(true)
                                }
                            }
                        )
                    }
            ) {
                if (isExpanded) {
                    // Full Screen Now Playing Layout
                    currentSong?.let { song ->
                        val lyricsState by viewModel.lyricsState.collectAsStateWithLifecycle()
                        val isLyricsLoading by viewModel.isLyricsLoading.collectAsStateWithLifecycle()
                        val isCoverLoading by viewModel.isCoverLoading.collectAsStateWithLifecycle()

                        NowPlayingExpandedScreen(
                            song = song,
                            isPlaying = isPlaying,
                            progress = progress,
                            dacConfig = dacConfig,
                            latencyMs = latencyMs,
                            spectrumData = spectrumData,
                            lyricsState = lyricsState,
                            isLyricsLoading = isLyricsLoading,
                            isCoverLoading = isCoverLoading,
                            onCollapse = { viewModel.setNowPlayingExpanded(false) },
                            onPlayPauseToggle = { viewModel.setPlaying(!isPlaying) },
                            onNext = { viewModel.playNextSong() },
                            onPrev = { viewModel.playPreviousSong() },
                            onSeek = { viewModel.seekTo(it) },
                            onFavoriteToggle = { viewModel.toggleFavoriteCurrentSong() },
                            onDacIndicatorClick = { showDacSettingsDialog = true },
                            onFetchLyrics = { viewModel.fetchOnlineLyrics() },
                            onFetchCover = { viewModel.fetchOnlineCover() },
                            onOpenEq = { showEqSettingsDialog = true }
                        )
                    }
                } else {
                    // Collapsed Mini Player
                    currentSong?.let { song ->
                        MiniPlayerRow(
                            song = song,
                            isPlaying = isPlaying,
                            onPlayPauseToggle = { viewModel.setPlaying(!isPlaying) },
                            onNext = { viewModel.playNextSong() },
                            onExpand = { viewModel.setNowPlayingExpanded(true) },
                            dacConfig = dacConfig
                        )
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No music playing",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }

    // High-Fidelity Bypass DAC & Audio Controller Settings Panel (Custom Dialog)
    if (showDacSettingsDialog && dacConfig != null) {
        DacSettingsOverlayDialog(
            config = dacConfig!!,
            onDismiss = { showDacSettingsDialog = false },
            onToggleBypass = { viewModel.toggleDacBypass() },
            onToggleUsbConnected = { viewModel.toggleUsbDacConnected() },
            onSampleRateChange = { viewModel.updateSampleRate(it) },
            onBitDepthChange = { viewModel.updateBitDepth(it) },
            onDriverChange = { viewModel.updateDriverHost(it) },
            onBufferChange = { viewModel.updateBufferSize(it) },
            onMqaToggle = { viewModel.toggleMqaDecoder() },
            onVolumeChange = { viewModel.updateDacVolume(it) },
            onDsdModeChange = { viewModel.updateDsdNativeMode(it) }
        )
    }

    // High-Fidelity Parametric Equalizer Dialog Panel
    if (showEqSettingsDialog && dacConfig != null) {
        EqualizerSettingsOverlayDialog(
            config = dacConfig!!,
            onDismiss = { showEqSettingsDialog = false },
            onToggleEq = { viewModel.toggleEqEnabled() },
            onPreampChange = { viewModel.updateEqPreamp(it) },
            onBandChange = { hz, db -> viewModel.updateEqBand(hz, db) },
            onQChange = { viewModel.updateEqQ(it) },
            onFilterTypeChange = { viewModel.updateEqFilterType(it) },
            onPresetChange = { viewModel.applyEqPreset(it) }
        )
    }
}

// ==========================================
// SCREEN 1: LISTEN NOW (HOME)
// ==========================================
@Composable
fun ListenNowScreen(
    songs: List<Song>,
    dacConfig: DacConfig?,
    onSongSelect: (Song) -> Unit,
    onDacPanelClick: () -> Unit,
    onToggleBypass: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // App Title Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Listen Now",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "High-Fidelity Audio Center",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Beautiful metallic user profile placeholder
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(AppleMusicRed, AudiophileGold)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "MM",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // DAC Telemetry Dashboard Banner
        item {
            dacConfig?.let { config ->
                Card(
                    onClick = onDacPanelClick,
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (config.isBypassEnabled) Color(0xFF1C1C24) else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dac_status_banner")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (config.isBypassEnabled && config.isUsbDacConnected) AudiophileGold else Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (config.isBypassEnabled) "DIRECT DAC BYPASS: ACTIVE" else "STANDARD MIXER",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.isBypassEnabled) AudiophileGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    letterSpacing = 1.2.sp
                                )
                            }
                            // Smooth Toggle switch
                            Switch(
                                checked = config.isBypassEnabled,
                                onCheckedChange = { onToggleBypass() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = AudiophileGold,
                                    checkedTrackColor = AudiophileGold.copy(alpha = 0.4f),
                                    uncheckedThumbColor = Color.LightGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = if (config.isBypassEnabled && config.isUsbDacConnected) {
                                "Bit-Perfect Host Output Mode Enabled"
                            } else {
                                "Android AudioFlinger Resampler (Filtered)"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (config.isBypassEnabled) Color.White else MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Live audio properties status indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Device: ${if (config.isUsbDacConnected) "Topping DX3 Pro+ USB" else "Internal Audio Driver"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (config.isBypassEnabled) AudiophileGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${config.sampleRate} / ${config.bitDepth}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.isBypassEnabled) AudiophileGold else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Header: Recently Played
        item {
            Text(
                text = "Recently Played",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Horizontal Row of Beautiful Album Cards
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(songs) { song ->
                    Column(
                        modifier = Modifier
                            .width(140.dp)
                            .clickable { onSongSelect(song) }
                            .testTag("recent_song_${song.id}")
                    ) {
                        // Glossy glassmorphic card container for Album Art
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            modifier = Modifier
                                .size(140.dp)
                                .aspectRatio(1f)
                        ) {
                            Image(
                                painter = painterResource(id = song.albumArtResId),
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = song.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = song.artist,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        
                        // Small audio badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppleMusicRed.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (song.qualityLabel.contains("DSD")) "DSD" else "HI-RES",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppleMusicPink
                            )
                        }
                    }
                }
            }
        }

        // Section: Audiophile Masterpieces
        item {
            Text(
                text = "Audiophile Showcase",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        items(songs) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSongSelect(song) }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = song.albumArtResId),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${song.artist} • ${song.album}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Bit-perfect Audio Quality Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (song.qualityLabel.contains("DSD")) {
                                    AudiophileGold.copy(alpha = 0.2f)
                                } else {
                                    HiResBlue.copy(alpha = 0.15f)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (song.qualityLabel.contains("DSD")) "DSD256" else "24-BIT PCM",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (song.qualityLabel.contains("DSD")) AudiophileGold else HiResBlue
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.sampleRate,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 2: BROWSE SCREEN
// ==========================================
@Composable
fun BrowseScreen(
    songs: List<Song>,
    onSongSelect: (Song) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Browse",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Discover pure uncompressed acoustic excellence",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Featured Premium Banners
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Fallback gorgeous gradient backdrop
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AppleMusicPink, HiResBlue)
                                )
                            )
                    )
                    // Ambient text overlays
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "EXCLUSIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Column {
                            Text(
                                text = "Lossless DXD Recording Series",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Mastered directly to 32-bit/384kHz WAV and DSD512 format.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Hi-Res Essentials",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Featured grid/list of browsing tracks
        items(songs) { song ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSongSelect(song) }
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = song.albumArtResId),
                    contentDescription = song.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = song.artist,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                IconButton(onClick = { onSongSelect(song) }) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = "Play",
                        tint = AppleMusicPink,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 3: LIBRARY SCREEN
// ==========================================
@Composable
fun LibraryScreen(
    favoriteSongs: List<Song>,
    onSongSelect: (Song) -> Unit,
    onDacPanelClick: () -> Unit,
    storagePermissionGranted: Boolean,
    isScanning: Boolean,
    scanProgress: String,
    onGrantPermission: () -> Unit,
    onScanClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Library",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "My local bit-perfect audio library",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Offline Bit-Perfect Local Directory Scanner Control Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(BorderStroke(1.dp, AppleMusicPink.copy(alpha = 0.25f)), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Storage Scanner",
                        tint = AppleMusicPink,
                        modifier = Modifier.size(28.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Music Library Scanner",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Scan external folders and SD Card for FLAC, WAV & DSD audio files.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))
                
                if (isScanning) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LinearProgressIndicator(
                            color = AppleMusicPink,
                            trackColor = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = scanProgress,
                            fontSize = 11.sp,
                            color = AppleMusicPink,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (!storagePermissionGranted) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Permission required: Storage",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        Button(
                            onClick = onGrantPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AppleMusicPink),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Grant Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(imageVector = Icons.Filled.Security, contentDescription = "Granted", tint = AudiophileGold, modifier = Modifier.size(14.dp))
                            Text(
                                text = "Permission Active • Scanner Ready",
                                fontSize = 11.sp,
                                color = AudiophileGold,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = onScanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AudiophileGold),
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("Run Deep Scan", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.Black)
                        }
                    }
                }
            }
        }

        // Horizontal navigation list for normal library folders
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                listOf(
                    Pair("Playlists", Icons.Filled.QueueMusic),
                    Pair("Albums", Icons.Filled.Album),
                    Pair("Artists", Icons.Filled.People),
                    Pair("DAC Device Configurations", Icons.Filled.SettingsInputHdmi)
                ).forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (item.first.contains("DAC")) {
                                    onDacPanelClick()
                                }
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.second,
                            contentDescription = item.first,
                            tint = AppleMusicPink
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = item.first,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Go",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (index < 3) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                }
            }
        }

        item {
            Text(
                text = "Favorites / Offline High-Res Hits",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (favoriteSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No favorite tracks yet",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            items(favoriteSongs) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSongSelect(song) }
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = song.albumArtResId),
                        contentDescription = song.title,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = song.artist,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = "Favorited",
                        tint = AppleMusicPink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// COMPONENT: MINI PLAYER ROW (COLLAPSED STATE)
// ==========================================
@Composable
fun MiniPlayerRow(
    song: Song,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    dacConfig: DacConfig?
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onExpand() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rotating album cover or static centered cover with premium borders
        Image(
            painter = painterResource(id = song.albumArtResId),
            contentDescription = "Mini album art",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artist,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (dacConfig?.isBypassEnabled == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(AudiophileGold.copy(alpha = 0.25f))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "BYPASS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AudiophileGold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Play/Pause Action with ripple feedback
        IconButton(
            onClick = onPlayPauseToggle,
            modifier = Modifier.testTag("mini_play_pause")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = "Play/Pause",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }

        // Skip Next Action
        IconButton(
            onClick = onNext,
            modifier = Modifier.testTag("mini_skip_next")
        ) {
            Icon(
                imageVector = Icons.Filled.SkipNext,
                contentDescription = "Skip Next",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ==========================================
// COMPONENT: NOW PLAYING FULL SCREEN (EXPANDED STATE)
// ==========================================
@Composable
fun NowPlayingExpandedScreen(
    song: Song,
    isPlaying: Boolean,
    progress: Float,
    dacConfig: DacConfig?,
    latencyMs: Float,
    spectrumData: List<Float>,
    lyricsState: String?,
    isLyricsLoading: Boolean,
    isCoverLoading: Boolean,
    onCollapse: () -> Unit,
    onPlayPauseToggle: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onFavoriteToggle: () -> Unit,
    onDacIndicatorClick: () -> Unit,
    onFetchLyrics: () -> Unit,
    onFetchCover: () -> Unit,
    onOpenEq: () -> Unit
) {
    var isLyricsActive by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic dynamic color/gradient soft blur background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        
        // Blurred ambient orb layers to mimic Apple Music background fluidity
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.6f }
        ) {
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-50).dp)
                    .blur(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (song.title.contains("Celestial")) Color(0xFF5D3EBB) else AppleMusicPink.copy(
                                    alpha = 0.8f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = 120.dp)
                    .blur(100.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (song.title.contains("Jazz")) AudiophileGold.copy(alpha = 0.7f) else HiResBlue.copy(
                                    alpha = 0.7f
                                ),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Main sheet content container
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 28.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag handle and top row close button
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.35f))
                        .clickable { onCollapse() }
                )
                
                Spacer(modifier = Modifier.height(14.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Collapse",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 2.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Synced Lyrics button
                        IconButton(onClick = { isLyricsActive = !isLyricsActive }) {
                            Icon(
                                imageVector = Icons.Filled.ChatBubbleOutline,
                                contentDescription = "Toggle Synced Lyrics",
                                tint = if (isLyricsActive) AppleMusicPink else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // DSP Equalizer button
                        IconButton(onClick = onOpenEq) {
                            Icon(
                                imageVector = Icons.Filled.Equalizer,
                                contentDescription = "DSP Parametric EQ",
                                tint = if (dacConfig?.isEqEnabled == true) AudiophileGold else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // DAC options button
                        IconButton(onClick = onDacIndicatorClick) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "DAC Status Panel",
                                tint = if (dacConfig?.isBypassEnabled == true) AudiophileGold else Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            if (isLyricsActive) {
                // Interactive real-time lyric scrolling pane
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLyricsLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = AppleMusicPink, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "mika AI generating & synchronizing lyrics...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else if (song.lyrics == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = "Offline lyrics missing",
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No lyrics found for this song",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onFetchLyrics,
                                colors = ButtonDefaults.buttonColors(containerColor = AppleMusicPink),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fetch Online Lyrics", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Display beautiful line-by-line synchronized lyrics
                        val lines = remember(song.lyrics) {
                            song.lyrics.split("\n").map { line ->
                                // Parse bracket timestamps e.g., [01:25] text
                                val regex = "\\[(\\d+):(\\d+)\\](.*)".toRegex()
                                val match = regex.find(line)
                                if (match != null) {
                                    val minutes = match.groupValues[1].toIntOrNull() ?: 0
                                    val seconds = match.groupValues[2].toIntOrNull() ?: 0
                                    val timeInSeconds = minutes * 60 + seconds
                                    val text = match.groupValues[3].trim()
                                    timeInSeconds to text
                                } else {
                                    -1 to line.trim()
                                }
                            }
                        }
                        
                        val scrollState = rememberScrollState()
                        val activeLineIndex = lines.indexOfLast { progress >= it.first && it.first != -1 }
                        
                        LaunchedEffect(activeLineIndex) {
                            if (activeLineIndex != -1) {
                                scrollState.animateScrollTo((activeLineIndex * 45).coerceAtLeast(0))
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(100.dp))
                            lines.forEachIndexed { idx, pair ->
                                val isActive = idx == activeLineIndex
                                val alpha by animateFloatAsState(targetValue = if (isActive) 1.0f else 0.45f)
                                val scaleLine by animateFloatAsState(targetValue = if (isActive) 1.15f else 0.95f)
                                val textColor = if (isActive) AudiophileGold else Color.White
                                
                                Text(
                                    text = pair.second,
                                    color = textColor,
                                    fontSize = 17.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .scale(scaleLine)
                                        .graphicsLayer(alpha = alpha)
                                        .padding(horizontal = 8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(120.dp))
                        }
                    }
                }
            } else {
                // Signature Interactive Album Cover with scaling & shadow bouncy spring animation
                val scale by animateFloatAsState(
                    targetValue = if (isPlaying) 1.0f else 0.82f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "AlbumArtScale"
                )
                
                val coverShadowDp by animateDpAsState(
                    targetValue = if (isPlaying) 28.dp else 4.dp,
                    animationSpec = spring(stiffness = Spring.StiffnessLow),
                    label = "AlbumArtShadow"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier
                            .size(280.dp)
                            .aspectRatio(1f)
                            .scale(scale)
                            .shadow(
                                elevation = coverShadowDp,
                                shape = RoundedCornerShape(22.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = if (song.title.contains("Celestial")) Color(0xFF5D3EBB) else AppleMusicPink
                            )
                            .testTag("expanded_album_art")
                    ) {
                        if (song.onlineCoverUrl != null) {
                            AsyncImage(
                                model = song.onlineCoverUrl,
                                contentDescription = "Online Album Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = song.albumArtResId)
                            )
                        } else {
                            Image(
                                painter = painterResource(id = song.albumArtResId),
                                contentDescription = "Local Album Cover",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // Dynamic Song Details, Favorite toggle & Audiophile Quality tags
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(0.7f)) {
                        Text(
                            text = song.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            fontSize = 18.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Remote Cover fetcher trigger
                        if (isCoverLoading) {
                            CircularProgressIndicator(
                                color = AppleMusicPink,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            IconButton(onClick = onFetchCover) {
                                Icon(
                                    imageVector = Icons.Filled.CloudDownload,
                                    contentDescription = "Search Online Cover",
                                    tint = if (song.onlineCoverUrl != null) AudiophileGold else Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onFavoriteToggle,
                            modifier = Modifier.testTag("favorite_button")
                        ) {
                            Icon(
                                imageVector = if (song.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isFavorite) AppleMusicPink else Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // High Fidelity Audio Quality Info Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onDacIndicatorClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (dacConfig?.isBypassEnabled == true) AudiophileGold else HiResBlue)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (song.qualityLabel.contains("DSD")) "NATIVE DSD" else "HI-RES LOSSLESS",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }

                    Text(
                        text = "${song.format} • ${song.sampleRate} • ${song.bitDepth}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    if (dacConfig?.isBypassEnabled == true) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Bypass active",
                            tint = AudiophileGold,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // High Fidelity active spectrum visualization waves
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    spectrumData.forEach { heightMultiplier ->
                        val animatedHeight by animateDpAsState(
                            targetValue = 30.dp * heightMultiplier,
                            animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
                            label = "SpectrumBar"
                        )
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(animatedHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AppleMusicPink,
                                            if (dacConfig?.isBypassEnabled == true) AudiophileGold else HiResBlue
                                        )
                                    )
                                )
                        )
                    }
                }
            }

            // Sliding Progress Slider Track with time indexes
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = { onSeek(it) },
                    valueRange = 0f..song.durationSeconds.toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.24f),
                        thumbColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("playback_progress_slider")
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(progress.toInt()),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "-${formatTime(song.durationSeconds - progress.toInt())}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Transport Control Buttons (Prev, Play, Next)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Main circular glass/metallic play button with scale on click
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onPlayPauseToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play / Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            // Volume controls and DAC bypass hardware latency bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = Icons.Filled.VolumeMute,
                    contentDescription = "Mute",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                
                // Volume slider representation
                var currentVolumeSim by remember { mutableStateOf(0.75f) }
                Slider(
                    value = currentVolumeSim,
                    onValueChange = { currentVolumeSim = it },
                    colors = SliderDefaults.colors(
                        activeTrackColor = Color.White.copy(alpha = 0.8f),
                        inactiveTrackColor = Color.White.copy(alpha = 0.16f),
                        thumbColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )
                
                Icon(
                    imageVector = Icons.Filled.VolumeUp,
                    contentDescription = "Max Volume",
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Real-Time Hardware Telemetry info footer
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.4f))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Memory,
                    contentDescription = "Processor Core",
                    tint = if (dacConfig?.isBypassEnabled == true) AudiophileGold else Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (dacConfig?.isBypassEnabled == true) {
                        "DRIVER: ${dacConfig.driverHost} • BUFFER: ${dacConfig.bufferSize} FRAMES • LATENCY: ${String.format("%.2f", latencyMs)}ms"
                    } else {
                        "SYSTEM DRIVER: AudioTrack • BUFFER: Auto • LATENCY: ${String.format("%.1f", latencyMs)}ms"
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (dacConfig?.isBypassEnabled == true) AudiophileGold else Color.White.copy(alpha = 0.6f),
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ==========================================
// DIALOG: HIGH-FIDELITY BYPASS DAC CONFIGURATION
// ==========================================
@Composable
fun DacSettingsOverlayDialog(
    config: DacConfig,
    onDismiss: () -> Unit,
    onToggleBypass: () -> Unit,
    onToggleUsbConnected: () -> Unit,
    onSampleRateChange: (String) -> Unit,
    onBitDepthChange: (String) -> Unit,
    onDriverChange: (String) -> Unit,
    onBufferChange: (Int) -> Unit,
    onMqaToggle: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onDsdModeChange: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF13131A)),
            border = BorderStroke(1.dp, AudiophileGold.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("dac_settings_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header with custom gold glow
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "mika DAC Controller",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = AudiophileGold
                        )
                        Text(
                            text = "Direct USB Bypass & Bit-Perfect Engine",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.6f)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close settings",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Section 1: USB DAC Plugged state (simulated)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .clickable { onToggleUsbConnected() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Usb,
                            contentDescription = "USB Connection Status",
                            tint = if (config.isUsbDacConnected) AudiophileGold else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "USB OTG DAC Hardware",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (config.isUsbDacConnected) "Connected (External USB Device)" else "Disconnected (System fallback)",
                                fontSize = 11.sp,
                                color = if (config.isUsbDacConnected) AudiophileGold else Color.Gray
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (config.isUsbDacConnected) Color(0xFF1E3F20) else Color(0xFF333333))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (config.isUsbDacConnected) "READY" else "OFFLINE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (config.isUsbDacConnected) Color.Green else Color.LightGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Bit-Perfect Direct USB Bypass Mode Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(0.75f)) {
                        Text(
                            text = "Bit-Perfect Direct Output",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Bypasses Android standard mixer, volume limitation, and resampler. Sends clean binary stream straight to hardware.",
                            fontSize = 11.sp,
                            color = Color.LightGray.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = config.isBypassEnabled,
                        onCheckedChange = { onToggleBypass() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AudiophileGold,
                            checkedTrackColor = AudiophileGold.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.testTag("dac_bypass_toggle")
                    )
                }

                if (config.isBypassEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "HARDWARE ENGINE CUSTOMIZATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AudiophileGold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // PCM / DSD Native Selection Card
                    DacOptionSelector(
                        title = "DSD Output Mode",
                        currentValue = config.dsdNativeMode,
                        options = listOf("Native DSD (DoP)", "Direct Raw DSD", "PCM Convert"),
                        onSelect = onDsdModeChange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio Driver Selection Card
                    DacOptionSelector(
                        title = "Hardware Audio Host",
                        currentValue = config.driverHost,
                        options = listOf("Direct ALSA Host", "AAudio Direct", "OpenSL ES"),
                        onSelect = onDriverChange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Hardware Buffer frames selector (for ultra-low latency)
                    DacOptionSelectorInt(
                        title = "Hardware Audio Buffer",
                        currentValue = config.bufferSize,
                        options = listOf(64, 128, 256, 512),
                        suffix = " frames",
                        onSelect = onBufferChange
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // MQA Core Decoder Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "MQA Core Decoder",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Performs initial unfold of MQA Studio streams",
                                fontSize = 11.sp,
                                color = Color.LightGray.copy(alpha = 0.7f)
                            )
                        }
                        Checkbox(
                            checked = config.isMqaCoreDecoderEnabled,
                            onCheckedChange = { onMqaToggle() },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AudiophileGold,
                                checkmarkColor = Color.Black
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Hardware Physical Volume Regulation limit
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Hardware DAC Volume Control",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "${config.volumeHardwareDac}%",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AudiophileGold
                            )
                        }
                        Slider(
                            value = config.volumeHardwareDac.toFloat(),
                            onValueChange = { onVolumeChange(it.roundToInt()) },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = AudiophileGold,
                                inactiveTrackColor = Color.DarkGray,
                                thumbColor = AudiophileGold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// Option selector helper
@Composable
fun DacOptionSelector(
    title: String,
    currentValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == currentValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AudiophileGold else Color.White.copy(alpha = 0.08f))
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Option selector integer helper
@Composable
fun DacOptionSelectorInt(
    title: String,
    currentValue: Int,
    options: List<Int>,
    suffix: String,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .padding(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == currentValue
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AudiophileGold else Color.White.copy(alpha = 0.08f))
                        .clickable { onSelect(option) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$option$suffix",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Helper: Formats track progress/duration from raw seconds integer to MM:SS
private fun formatTime(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%d:%02d", m, s)
}

// ==========================================
// DIALOG: AUDIOPHILE PARAMETRIC EQUALIZER
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSettingsOverlayDialog(
    config: DacConfig,
    onDismiss: () -> Unit,
    onToggleEq: () -> Unit,
    onPreampChange: (Float) -> Unit,
    onBandChange: (Int, Float) -> Unit,
    onQChange: (Float) -> Unit,
    onFilterTypeChange: (String) -> Unit,
    onPresetChange: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.dp, AudiophileGold.copy(alpha = 0.35f)), RoundedCornerShape(24.dp)),
            color = Color(0xFF121216)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Filled.Equalizer,
                            contentDescription = "Parametric EQ",
                            tint = AudiophileGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Parametric Equalizer",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "64-bit Bit-Perfect DSP Engine",
                                fontSize = 10.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Dismiss",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }

                // EQ bypass switch and preset picker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DSP Equalizer Processing", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = if (config.isEqEnabled) "DSP ACTIVE" else "DSP BYPASSED",
                            fontSize = 10.sp,
                            color = if (config.isEqEnabled) AudiophileGold else Color.Gray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = config.isEqEnabled,
                        onCheckedChange = { onToggleEq() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AudiophileGold
                        )
                    )
                }

                if (config.isEqEnabled) {
                    // Preamp Section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Hardware Pre-Amplifier Gain", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                            Text("${String.format("%.1f", config.eqPreampDb)} dB", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AudiophileGold)
                        }
                        Slider(
                            value = config.eqPreampDb,
                            onValueChange = onPreampChange,
                            valueRange = -12f..12f,
                            colors = SliderDefaults.colors(
                                activeTrackColor = AudiophileGold,
                                inactiveTrackColor = Color.White.copy(alpha = 0.1f),
                                thumbColor = AudiophileGold
                            )
                        )
                    }

                    // Presets Carousel
                    Column {
                        Text("Equalizer Target Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val presets = listOf("Flat", "Audiophile Reference", "Bass Boost", "Vocal Clarity", "Treble Boost")
                            items(presets) { preset ->
                                val isSelected = config.eqPresetName == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { onPresetChange(preset) },
                                    label = { Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AudiophileGold,
                                        selectedLabelColor = Color.Black,
                                        containerColor = Color.White.copy(alpha = 0.05f),
                                        labelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    // 10 bands EQ Sliders
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Frequency Response (10-Band EQ)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.03f))
                                .padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val bands = listOf(
                                31 to config.eqBand31,
                                62 to config.eqBand62,
                                125 to config.eqBand125,
                                250 to config.eqBand250,
                                500 to config.eqBand500,
                                1000 to config.eqBand1k,
                                2000 to config.eqBand2k,
                                4000 to config.eqBand4k,
                                8000 to config.eqBand8k,
                                16000 to config.eqBand16k
                            )
                            
                            bands.forEach { (hz, gain) ->
                                val hzLabel = if (hz >= 1000) "${hz/1000}k" else "$hz"
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxHeight().weight(1f)
                                ) {
                                    Text(
                                        text = "${if (gain > 0) "+" else ""}${gain.roundToInt()}",
                                        fontSize = 8.sp,
                                        color = if (gain != 0f) AudiophileGold else Color.Gray,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(modifier = Modifier.weight(1f).width(20.dp), contentAlignment = Alignment.Center) {
                                        // Custom visual representation using scaled slider
                                        Slider(
                                            value = gain,
                                            onValueChange = { onBandChange(hz, it) },
                                            valueRange = -12f..12f,
                                            colors = SliderDefaults.colors(
                                                activeTrackColor = if (gain != 0f) AudiophileGold else Color.Gray,
                                                thumbColor = if (gain != 0f) AudiophileGold else Color.Gray
                                            ),
                                            modifier = Modifier
                                                .graphicsLayer {
                                                    rotationZ = -90f
                                                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                                                }
                                                .width(100.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = hzLabel,
                                        fontSize = 9.sp,
                                        color = Color.LightGray,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Q Factor and Filter Type Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Q-Factor
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Bandwidth Q", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text("${String.format("%.2f", config.eqBandwidthQ)}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AudiophileGold)
                            }
                            Slider(
                                value = config.eqBandwidthQ,
                                onValueChange = onQChange,
                                valueRange = 0.2f..5f,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = AudiophileGold,
                                    thumbColor = AudiophileGold
                                )
                            )
                        }

                        // Filter Type Selection
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(10.dp)
                        ) {
                            Text("Filter Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.height(6.dp))
                            val filterTypes = listOf("Peaking", "Low Shelf", "High Shelf")
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                filterTypes.forEach { type ->
                                    val isSel = config.eqFilterType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSel) AudiophileGold else Color.White.copy(alpha = 0.06f))
                                            .clickable { onFilterTypeChange(type) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = type.split(" ")[0],
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) Color.Black else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Disabled placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = "Bypassed",
                                tint = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.size(80.dp)
                            )
                            Text(
                                text = "DSP Parametric Equalizer is Bypassed",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Enable processing to adjust frequency levels",
                                color = Color.White.copy(alpha = 0.25f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
