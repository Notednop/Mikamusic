package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class Song(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val durationSeconds: Int,
    val qualityLabel: String, // e.g., "Hi-Res Lossless", "Direct DSD256", "MQA Studio"
    val sampleRate: String,   // e.g., "192 kHz", "11.2 MHz", "384 kHz"
    val bitDepth: String,     // e.g., "24-bit", "1-bit", "32-bit"
    val format: String,       // e.g., "FLAC", "DSD", "ALAC", "WAV"
    val albumArtResId: Int,   // Drawable ID
    val isFavorite: Boolean = false,
    val isOffline: Boolean = false,      // For locally scanned storage files
    val lyrics: String? = null,          // Fetched online lyrics
    val onlineCoverUrl: String? = null   // Fetched online cover/background URL
)
