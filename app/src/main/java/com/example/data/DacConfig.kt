package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dac_config")
data class DacConfig(
    @PrimaryKey val id: Int = 1, // Single-row config
    val isBypassEnabled: Boolean = true,
    val isUsbDacConnected: Boolean = true, // Simulated connection state
    val sampleRate: String = "192 kHz",
    val bitDepth: String = "24-bit",
    val driverHost: String = "Direct ALSA Host", // e.g. "Direct ALSA Host", "AAudio Direct", "OpenSL ES"
    val bufferSize: Int = 128,                 // e.g. 64, 128, 256, 512, 1024
    val isMqaCoreDecoderEnabled: Boolean = true,
    val volumeHardwareDac: Int = 85,          // Real physical volume limit if bypass is enabled
    val dsdNativeMode: String = "Native DSD (DoP)", // e.g., "Native DSD (DoP)", "Direct Raw DSD", "PCM Convert"

    // High Fidelity Parametric Equalizer Parameters
    val isEqEnabled: Boolean = false,
    val eqPreampDb: Float = 0f,            // -12.0f to +12.0f dB
    val eqBand31: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand62: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand125: Float = 0f,             // -12.0f to +12.0f dB
    val eqBand250: Float = 0f,             // -12.0f to +12.0f dB
    val eqBand500: Float = 0f,             // -12.0f to +12.0f dB
    val eqBand1k: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand2k: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand4k: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand8k: Float = 0f,              // -12.0f to +12.0f dB
    val eqBand16k: Float = 0f,             // -12.0f to +12.0f dB
    val eqBandwidthQ: Float = 1.0f,         // 0.1f to 10.0f Q-factor
    val eqFilterType: String = "Peaking",  // Peaking, Low Shelf, High Shelf, Notch, Bandpass
    val eqPresetName: String = "Flat"      // Flat, Audiophile Reference, Bass Boost, Vocal Clarity, Treble Boost
)
