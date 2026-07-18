package com.example.data

import android.content.Context
import com.example.R
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class MusicRepository(private val context: Context, private val musicDao: MusicDao) {

    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = musicDao.getFavoriteSongs()
    val dacConfig: Flow<DacConfig?> = musicDao.getDacConfig()

    suspend fun initializeDatabase() {
        // Pre-populate with beautiful default tracks
        val currentSongs = musicDao.getAllSongs().firstOrNull()
        if (currentSongs.isNullOrEmpty()) {
            val defaultSongs = listOf(
                Song(
                    title = "Celestial Symphony",
                    artist = "Aethelgard",
                    album = "Astral Journeys",
                    durationSeconds = 342,
                    qualityLabel = "Hi-Res Lossless 24-bit / 192kHz",
                    sampleRate = "192 kHz",
                    bitDepth = "24-bit",
                    format = "FLAC",
                    albumArtResId = R.drawable.img_album_celestial_1784403506496,
                    isFavorite = true
                ),
                Song(
                    title = "Midnight Jazz Lounge",
                    artist = "The Blue Notes Trio",
                    album = "Vapor & Valve",
                    durationSeconds = 295,
                    qualityLabel = "Native DSD256 Bit-Perfect Direct",
                    sampleRate = "11.2 MHz",
                    bitDepth = "1-bit",
                    format = "DSD (DSF)",
                    albumArtResId = R.drawable.img_album_jazz_1784403518249,
                    isFavorite = false
                ),
                Song(
                    title = "Neon Horizon",
                    artist = "Spectral Core",
                    album = "Overdrive 1988",
                    durationSeconds = 218,
                    qualityLabel = "DXD Studio Master 32-bit / 384kHz",
                    sampleRate = "384 kHz",
                    bitDepth = "32-bit",
                    format = "WAV",
                    albumArtResId = R.drawable.img_album_neon_1784403535381,
                    isFavorite = false
                ),
                Song(
                    title = "Ethereal Echoes",
                    artist = "Maya Lin",
                    album = "Forest Rain",
                    durationSeconds = 184,
                    qualityLabel = "Lossless 24-bit / 96kHz MQA",
                    sampleRate = "96 kHz",
                    bitDepth = "24-bit",
                    format = "ALAC",
                    albumArtResId = R.drawable.img_album_celestial_1784403506496, // fallback
                    isFavorite = false
                )
            )
            musicDao.insertSongs(defaultSongs)
        }

        // Pre-populate DAC default config if missing
        val currentDac = musicDao.getDacConfig().firstOrNull()
        if (currentDac == null) {
            val defaultConfig = DacConfig()
            musicDao.insertDacConfig(defaultConfig)
        }
    }

    suspend fun toggleFavorite(songId: Int, isFavorite: Boolean) {
        musicDao.toggleFavorite(songId, isFavorite)
    }

    suspend fun insertSongs(songs: List<Song>) {
        musicDao.insertSongs(songs)
    }

    suspend fun updateSong(song: Song) {
        musicDao.updateSong(song)
    }

    suspend fun updateDacConfig(config: DacConfig) {
        musicDao.updateDacConfig(config)
    }
}
