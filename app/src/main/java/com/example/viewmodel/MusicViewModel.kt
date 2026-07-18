package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.BuildConfig
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = MusicRepository(application, db.musicDao())

    val songs: StateFlow<List<Song>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSongs: StateFlow<List<Song>> = repository.favoriteSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dacConfig: StateFlow<DacConfig?> = repository.dacConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Player UI States
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackProgress = MutableStateFlow(0f)
    val playbackProgress: StateFlow<Float> = _playbackProgress.asStateFlow()

    // Now Playing Sheet State: true = Expanded, false = Mini Player
    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    // Real-time audio telemetry simulation
    private val _latencyMs = MutableStateFlow(1.5f)
    val latencyMs: StateFlow<Float> = _latencyMs.asStateFlow()

    // Simulated spectrum analyzer heights (12 frequency bars)
    private val _spectrumData = MutableStateFlow(List(12) { 0.1f })
    val spectrumData: StateFlow<List<Float>> = _spectrumData.asStateFlow()

    // Active screen navigation tab
    private val _activeTab = MutableStateFlow("listen_now") // "listen_now", "browse", "library"
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    // Offline Scanner states
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow("")
    val scanProgress: StateFlow<String> = _scanProgress.asStateFlow()

    // Online Lyrics states
    private val _lyricsState = MutableStateFlow<String?>(null)
    val lyricsState: StateFlow<String?> = _lyricsState.asStateFlow()

    private val _isLyricsLoading = MutableStateFlow(false)
    val isLyricsLoading: StateFlow<Boolean> = _isLyricsLoading.asStateFlow()

    // Online Cover Loading state
    private val _isCoverLoading = MutableStateFlow(false)
    val isCoverLoading: StateFlow<Boolean> = _isCoverLoading.asStateFlow()

    // Hardware Storage & DAC runtime permissions simulation
    private val _storagePermissionGranted = MutableStateFlow(false)
    val storagePermissionGranted: StateFlow<Boolean> = _storagePermissionGranted.asStateFlow()

    private val _usbPermissionGranted = MutableStateFlow(false)
    val usbPermissionGranted: StateFlow<Boolean> = _usbPermissionGranted.asStateFlow()

    private var playbackJob: Job? = null
    private var telemetryJob: Job? = null

    init {
        viewModelScope.launch {
            repository.initializeDatabase()
            // Set first song as default current song once loaded
            songs.filter { it.isNotEmpty() }.collectFirst { songList ->
                if (_currentSong.value == null) {
                    val defaultSong = songList.firstOrNull()
                    _currentSong.value = defaultSong
                    // Pre-fill lyrics if already present
                    _lyricsState.value = defaultSong?.lyrics
                }
            }
        }
        startTelemetryLoop()
    }

    private fun startTelemetryLoop() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            var tick = 0f
            while (true) {
                if (_isPlaying.value) {
                    tick += 0.2f
                    // Smoothly oscillate simulated DAC latency based on buffer size, bypass mode, and EQ processing overhead
                    val config = dacConfig.value
                    val eqOverhead = if (config?.isEqEnabled == true) 0.8f else 0f
                    val baseLatency = if (config?.isBypassEnabled == true) {
                        (config.bufferSize.toFloat() / 44.1f / 4f) + eqOverhead // e.g. 128 / 44.1 / 4 = ~0.7 ms + EQ
                    } else {
                        12.5f + eqOverhead // standard Android audio mixer latency
                    }
                    val drift = kotlin.math.sin(tick) * 0.12f
                    _latencyMs.value = (baseLatency + drift).coerceAtLeast(0.4f)

                    // Generate responsive oscillating wave visualizer data (frequencies modified by active EQ sliders!)
                    val bandsSum = if (config != null && config.isEqEnabled) {
                        (config.eqBand31 + config.eqBand62 + config.eqBand125 + config.eqBand250 + config.eqBand500 + 
                         config.eqBand1k + config.eqBand2k + config.eqBand4k + config.eqBand8k + config.eqBand16k) / 10f
                    } else 0f

                    _spectrumData.value = List(12) { index ->
                        val speed = 0.5f + (index * 0.1f)
                        val multiplier = 0.4f + (index % 3) * 0.2f
                        val eqFactor = if (config != null && config.isEqEnabled) {
                            val specificBand = when(index) {
                                0 -> config.eqBand31
                                1 -> config.eqBand62
                                2 -> config.eqBand125
                                3 -> config.eqBand250
                                4 -> config.eqBand500
                                5 -> config.eqBand1k
                                6 -> config.eqBand2k
                                7 -> config.eqBand4k
                                8 -> config.eqBand8k
                                9, 10, 11 -> config.eqBand16k
                                else -> 0f
                            }
                            1.0f + (specificBand / 24f) // Modulate bar height by EQ band gain
                        } else 1.0f

                        val baseVal = kotlin.math.sin(tick * speed) * 0.35f + 0.45f
                        val noise = (Random.nextFloat() - 0.5f) * 0.1f
                        ((baseVal * multiplier * eqFactor) + noise).coerceIn(0.05f, 1.2f)
                    }
                } else {
                    // Quiet, breathing spectrum waves when paused
                    _spectrumData.value = _spectrumData.value.map { it * 0.85f }.map { it.coerceAtLeast(0.02f) }
                }
                delay(80)
            }
        }
    }

    fun selectTab(tab: String) {
        _activeTab.value = tab
    }

    fun selectSong(song: Song) {
        _currentSong.value = song
        _lyricsState.value = song.lyrics // Sync lyrics to view
        _playbackProgress.value = 0f
        setPlaying(true)
    }

    fun setPlaying(playing: Boolean) {
        _isPlaying.value = playing
        if (playing) {
            startPlaybackTimer()
        } else {
            playbackJob?.cancel()
        }
    }

    private fun startPlaybackTimer() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val song = _currentSong.value ?: break
                val nextProgress = _playbackProgress.value + 1f
                if (nextProgress >= song.durationSeconds) {
                    _playbackProgress.value = 0f
                    playNextSong()
                } else {
                    _playbackProgress.value = nextProgress
                }
            }
        }
    }

    fun seekTo(progress: Float) {
        _playbackProgress.value = progress
    }

    fun playNextSong() {
        val currentList = songs.value
        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        if (currentIndex != -1 && currentList.isNotEmpty()) {
            val nextIndex = (currentIndex + 1) % currentList.size
            selectSong(currentList[nextIndex])
        }
    }

    fun playPreviousSong() {
        val currentList = songs.value
        val currentIndex = currentList.indexOfFirst { it.id == _currentSong.value?.id }
        if (currentIndex != -1 && currentList.isNotEmpty()) {
            val prevIndex = if (currentIndex - 1 < 0) currentList.size - 1 else currentIndex - 1
            selectSong(currentList[prevIndex])
        }
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun toggleFavoriteCurrentSong() {
        val song = _currentSong.value ?: return
        val newFav = !song.isFavorite
        viewModelScope.launch {
            repository.toggleFavorite(song.id, newFav)
            _currentSong.value = song.copy(isFavorite = newFav)
        }
    }

    // Storage Permissions Toggles
    fun setStoragePermissionGranted(granted: Boolean) {
        _storagePermissionGranted.value = granted
    }

    fun setUsbPermissionGranted(granted: Boolean) {
        _usbPermissionGranted.value = granted
    }

    // Offline Music Scanning Routine
    fun scanLocalOfflineMusic() {
        if (_isScanning.value) return
        _isScanning.value = true
        _scanProgress.value = "Starting Bit-Perfect Scanner..."

        viewModelScope.launch {
            val mockFolders = listOf(
                "/storage/emulated/0/Music/HighRes/Beethoven_Symphony9_24bit.flac",
                "/storage/emulated/0/Music/Audiophile/Franz_Liszt_LaCampanella_DSD256.dsf",
                "/storage/emulated/0/Music/HiFi/Stan_Getz_Autumn_Leaves_ALAC_192.m4a",
                "/storage/emulated/0/Download/Claude_Debussy_ClairDeLune_384kHz.wav"
            )

            for (path in mockFolders) {
                _scanProgress.value = "Scanning file metadata:\n$path"
                delay(800)
            }

            _scanProgress.value = "Rebuilding audio library indices..."
            delay(1000)

            val scannedSongs = listOf(
                Song(
                    title = "Symphony No. 9 (Choral)",
                    artist = "Ludwig van Beethoven",
                    album = "Beethoven: The Masterworks",
                    durationSeconds = 480,
                    qualityLabel = "Hi-Res Lossless 24-bit / 96kHz",
                    sampleRate = "96 kHz",
                    bitDepth = "24-bit",
                    format = "FLAC",
                    albumArtResId = R.drawable.img_album_celestial_1784403506496,
                    isFavorite = true,
                    isOffline = true,
                    lyrics = "[00:00] (Instrumental Symphony Introduction)\n[00:45] Freude, schöner Götterfunken,\n[00:53] Tochter aus Elysium,\n[01:01] Wir betreten feuertrunken,\n[01:09] Himmlische, dein Heiligtum!\n[01:17] Deine Zauber binden wieder\n[01:25] Was die Mode streng geteilt;\n[01:33] Alle Menschen werden Brüder,\n[01:41] Wo dein sanfter Flügel weilt."
                ),
                Song(
                    title = "La Campanella",
                    artist = "Franz Liszt",
                    album = "Liszt: Virtuoso Piano Works",
                    durationSeconds = 285,
                    qualityLabel = "Native DSD256 Bit-Perfect Direct",
                    sampleRate = "11.2 MHz",
                    bitDepth = "1-bit",
                    format = "DSD (DSF)",
                    albumArtResId = R.drawable.img_album_jazz_1784403518249,
                    isFavorite = false,
                    isOffline = true,
                    lyrics = "[00:00] (Complex Solo Piano Performance)\n[00:30] (High Register Bell Imitations)\n[01:15] (Rapid Arpeggio and Jump Sequences)\n[02:00] (Interlocking Octaves Presto)\n[02:40] (Climactic Virtuoso Coda)"
                ),
                Song(
                    title = "Autumn Leaves",
                    artist = "Stan Getz",
                    album = "Warm Afternoon Jazz",
                    durationSeconds = 310,
                    qualityLabel = "Hi-Res Lossless 24-bit / 192kHz",
                    sampleRate = "192 kHz",
                    bitDepth = "24-bit",
                    format = "ALAC",
                    albumArtResId = R.drawable.img_album_neon_1784403535381,
                    isFavorite = true,
                    isOffline = true,
                    lyrics = "[00:00] (Tenor Saxophone Intro)\n[00:25] The falling leaves drift by the window\n[00:37] The autumn leaves of red and gold\n[00:50] I see your lips, the summer kisses\n[01:02] The sun-burned hands I used to hold\n[01:15] Since you went away the days grow long\n[01:27] And soon I'll hear old winter's song\n[01:39] But I miss you most of all my darling\n[01:52] When autumn leaves start to fall."
                ),
                Song(
                    title = "Clair de Lune",
                    artist = "Claude Debussy",
                    album = "Debussy: Piano Nocturnes",
                    durationSeconds = 305,
                    qualityLabel = "DXD Studio Master 32-bit / 384kHz",
                    sampleRate = "384 kHz",
                    bitDepth = "32-bit",
                    format = "WAV",
                    albumArtResId = R.drawable.img_album_celestial_1784403506496,
                    isFavorite = false,
                    isOffline = true,
                    lyrics = "[00:00] (Tranquil Impressionist Piano)\n[00:45] (Tempo Rubato Expression)\n[01:30] (Glistening Waterfall Cadenza)\n[02:20] (Ethereal Chord Dissolution)"
                )
            )

            repository.insertSongs(scannedSongs)
            _scanProgress.value = "Import Complete! Scanned 4 Bit-Perfect high-res master tracks."
            delay(1200)
            _isScanning.value = false
        }
    }

    // Online Lyrics fetcher using Direct REST Gemini API (with safe fallback)
    fun fetchOnlineLyrics() {
        val song = _currentSong.value ?: return
        _isLyricsLoading.value = true
        _lyricsState.value = null

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    // Fallback to high-fidelity offline lyrics if key is empty or placeholder
                    delay(1200)
                    val offlineLyrics = getFallbackLyrics(song)
                    _lyricsState.value = offlineLyrics
                    val updated = song.copy(lyrics = offlineLyrics)
                    repository.updateSong(updated)
                    _currentSong.value = updated
                    return@launch
                }

                val prompt = "Generate beautifully styled, line-by-line synchronized lyrics with [mm:ss] time brackets for the song '${song.title}' by '${song.artist}'. Return ONLY the lyrics with time tags, no other conversational intro or outro text. Use standard LRC format style."

                val client = OkHttpClient.Builder()
                    .connectTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(25, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val responseText = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")
                        response.body?.string() ?: throw Exception("Empty response body")
                    }
                }

                val jsonResponse = JSONObject(responseText)
                val candidates = jsonResponse.getJSONArray("candidates")
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.getJSONObject("content")
                val parts = content.getJSONArray("parts")
                val lyricsText = parts.getJSONObject(0).getString("text")

                _lyricsState.value = lyricsText
                val updatedSong = song.copy(lyrics = lyricsText)
                repository.updateSong(updatedSong)
                _currentSong.value = updatedSong
            } catch (e: Exception) {
                e.printStackTrace()
                // Graceful fallback to cached/offline custom high-quality lyrics on network error
                delay(1000)
                val offlineLyrics = getFallbackLyrics(song)
                _lyricsState.value = offlineLyrics
                val updated = song.copy(lyrics = offlineLyrics)
                repository.updateSong(updated)
                _currentSong.value = updated
            } finally {
                _isLyricsLoading.value = false
            }
        }
    }

    // Online Cover fetcher using Direct REST Gemini API (chooses dynamic hex gradient color codes representing song)
    fun fetchOnlineCover() {
        val song = _currentSong.value ?: return
        _isCoverLoading.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    delay(1200)
                    val randId = Random.nextInt(1000, 9999)
                    val generatedCover = "https://picsum.photos/seed/$randId/600/600"
                    val updatedSong = song.copy(onlineCoverUrl = generatedCover)
                    repository.updateSong(updatedSong)
                    _currentSong.value = updatedSong
                    return@launch
                }

                val prompt = "Choose two beautiful complementary hex colors representing the mood of the song '${song.title}' by '${song.artist}'. Return ONLY a JSON object with fields 'color1' and 'color2', e.g. {\"color1\": \"#FF0055\", \"color2\": \"#330022\"}."

                val client = OkHttpClient.Builder().build()
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        })
                    })
                }

                val requestBody = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(requestBody)
                    .build()

                val responseText = withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP error: ${response.code}")
                        response.body?.string() ?: throw Exception("Empty response body")
                    }
                }

                val jsonResponse = JSONObject(responseText)
                val textResponse = jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val cleanedText = textResponse.replace("```json", "").replace("```", "").trim()
                val colorObj = JSONObject(cleanedText)
                val c1 = colorObj.optString("color1", "D0BCFE").replace("#", "")
                val c2 = colorObj.optString("color2", "381E72").replace("#", "")

                val generatedCover = "https://placehold.co/600x600/$c1/$c2.png?text=${song.title.replace(" ", "+")}"
                val updatedSong = song.copy(onlineCoverUrl = generatedCover)
                repository.updateSong(updatedSong)
                _currentSong.value = updatedSong
            } catch (e: Exception) {
                e.printStackTrace()
                delay(1000)
                val randId = Random.nextInt(1000, 9999)
                val generatedCover = "https://picsum.photos/seed/$randId/600/600"
                val updatedSong = song.copy(onlineCoverUrl = generatedCover)
                repository.updateSong(updatedSong)
                _currentSong.value = updatedSong
            } finally {
                _isCoverLoading.value = false
            }
        }
    }

    private fun getFallbackLyrics(song: Song): String {
        return when {
            song.title.contains("Celestial") -> {
                "[00:00] (Instrumental Electronic Chords)\n[00:20] Rising above the starlight canopy\n[00:35] Echoes of a galaxy far away\n[00:50] Walking through cosmic solar winds\n[01:10] In the celestial sphere we stay\n[01:30] (Deep Space Synthesizer Solo)\n[02:10] Floating light, a quiet dreamscape\n[02:40] Rest under the astral velvet dome."
            }
            song.title.contains("Midnight") -> {
                "[00:00] (Smooth Brushed Drums and Double Bass)\n[00:15] Rain falls on the dimly lit neon street\n[00:32] A trumpet sings in minor chords so sweet\n[00:48] Sipping black coffee in the late hour\n[01:05] Lost in a vapor and valve power\n[01:25] (Jazz Trio Extended Improvisation)\n[02:20] The evening fades, but resonance remains."
            }
            song.title.contains("Neon") -> {
                "[00:00] (High Tempo Synthwave Arpeggio)\n[00:12] Driving through 1988 into the dark\n[00:24] Neon lights reflections in the park\n[00:36] Digital dreams of speed and sound\n[00:48] We spin the stereo all around!\n[01:02] (Overdrive Guitar and Synthesizer Duel)\n[01:45] The grid lines stretch out to the glowing sky."
            }
            song.title.contains("Ethereal") -> {
                "[00:00] (Soft Acoustic Flute and Gentle Rain)\n[00:10] Can you hear the whisper of the trees?\n[00:28] Echoes flowing on the mountain breeze\n[00:46] Underneath the forest roof of green\n[01:04] Finding wonders that we've never seen\n[01:22] (Acoustic Pluck and Violin Resonance)\n[02:00] Rain stops, the sunshine breaking clear."
            }
            else -> {
                "[00:00] (Music Playing)\n[00:15] Lyrics for '${song.title}'\n[00:30] Brought to you by mika online lyrics\n[00:45] Beautiful melodies in ${song.format} format\n[01:00] Playing bit-perfect at ${song.sampleRate}\n[01:30] (Instrumental Solo Section)\n[02:15] Thank you for listening offline!"
            }
        }
    }

    // Audiophile Setting Toggles
    fun toggleDacBypass() {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(isBypassEnabled = !current.isBypassEnabled)
            repository.updateDacConfig(updated)
        }
    }

    fun toggleUsbDacConnected() {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(isUsbDacConnected = !current.isUsbDacConnected)
            repository.updateDacConfig(updated)
        }
    }

    fun updateSampleRate(rate: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(sampleRate = rate)
            repository.updateDacConfig(updated)
        }
    }

    fun updateBitDepth(depth: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(bitDepth = depth)
            repository.updateDacConfig(updated)
        }
    }

    fun updateDriverHost(driver: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(driverHost = driver)
            repository.updateDacConfig(updated)
        }
    }

    fun updateBufferSize(frames: Int) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(bufferSize = frames)
            repository.updateDacConfig(updated)
        }
    }

    fun toggleMqaDecoder() {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(isMqaCoreDecoderEnabled = !current.isMqaCoreDecoderEnabled)
            repository.updateDacConfig(updated)
        }
    }

    fun updateDacVolume(volume: Int) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(volumeHardwareDac = volume)
            repository.updateDacConfig(updated)
        }
    }

    fun updateDsdNativeMode(mode: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(dsdNativeMode = mode)
            repository.updateDacConfig(updated)
        }
    }

    // High Fidelity Parametric Equalizer Controllers
    fun toggleEqEnabled() {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(isEqEnabled = !current.isEqEnabled)
            repository.updateDacConfig(updated)
        }
    }

    fun updateEqPreamp(gainDb: Float) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(eqPreampDb = gainDb)
            repository.updateDacConfig(updated)
        }
    }

    fun updateEqBand(bandHz: Int, gainDb: Float) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = when (bandHz) {
                31 -> current.copy(eqBand31 = gainDb, eqPresetName = "Custom")
                62 -> current.copy(eqBand62 = gainDb, eqPresetName = "Custom")
                125 -> current.copy(eqBand125 = gainDb, eqPresetName = "Custom")
                250 -> current.copy(eqBand250 = gainDb, eqPresetName = "Custom")
                500 -> current.copy(eqBand500 = gainDb, eqPresetName = "Custom")
                1000 -> current.copy(eqBand1k = gainDb, eqPresetName = "Custom")
                2000 -> current.copy(eqBand2k = gainDb, eqPresetName = "Custom")
                4000 -> current.copy(eqBand4k = gainDb, eqPresetName = "Custom")
                8000 -> current.copy(eqBand8k = gainDb, eqPresetName = "Custom")
                16000 -> current.copy(eqBand16k = gainDb, eqPresetName = "Custom")
                else -> current
            }
            repository.updateDacConfig(updated)
        }
    }

    fun updateEqQ(qFactor: Float) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(eqBandwidthQ = qFactor)
            repository.updateDacConfig(updated)
        }
    }

    fun updateEqFilterType(type: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = current.copy(eqFilterType = type)
            repository.updateDacConfig(updated)
        }
    }

    fun applyEqPreset(preset: String) {
        val current = dacConfig.value ?: return
        viewModelScope.launch {
            val updated = when (preset) {
                "Flat" -> current.copy(
                    eqPresetName = "Flat",
                    eqBand31 = 0f, eqBand62 = 0f, eqBand125 = 0f, eqBand250 = 0f, eqBand500 = 0f,
                    eqBand1k = 0f, eqBand2k = 0f, eqBand4k = 0f, eqBand8k = 0f, eqBand16k = 0f
                )
                "Audiophile Reference" -> current.copy(
                    eqPresetName = "Audiophile Reference",
                    eqBand31 = 1.0f, eqBand62 = 1.5f, eqBand125 = 0.5f, eqBand250 = 0.0f, eqBand500 = 0.0f,
                    eqBand1k = 0.5f, eqBand2k = 1.0f, eqBand4k = 1.5f, eqBand8k = 1.0f, eqBand16k = 2.0f
                )
                "Bass Boost" -> current.copy(
                    eqPresetName = "Bass Boost",
                    eqBand31 = 8.0f, eqBand62 = 6.0f, eqBand125 = 4.0f, eqBand250 = 2.0f, eqBand500 = 0.0f,
                    eqBand1k = 0.0f, eqBand2k = 0.0f, eqBand4k = 0.0f, eqBand8k = 0.0f, eqBand16k = 0.0f
                )
                "Vocal Clarity" -> current.copy(
                    eqPresetName = "Vocal Clarity",
                    eqBand31 = -3.0f, eqBand62 = -1.5f, eqBand125 = 0.0f, eqBand250 = 1.5f, eqBand500 = 3.0f,
                    eqBand1k = 4.0f, eqBand2k = 3.0f, eqBand4k = 1.5f, eqBand8k = 0.5f, eqBand16k = 0.0f
                )
                "Treble Boost" -> current.copy(
                    eqPresetName = "Treble Boost",
                    eqBand31 = 0.0f, eqBand62 = 0.0f, eqBand125 = 0.0f, eqBand250 = 0.0f, eqBand500 = 0.0f,
                    eqBand1k = 1.0f, eqBand2k = 2.5f, eqBand4k = 4.0f, eqBand8k = 6.0f, eqBand16k = 8.0f
                )
                else -> current
            }
            repository.updateDacConfig(updated)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        telemetryJob?.cancel()
    }
}

// Extension to collect only first value
private suspend fun <T> Flow<T>.collectFirst(action: suspend (T) -> Unit) {
    take(1).collect(action)
}
