package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Song queries
    @Query("SELECT * FROM songs ORDER BY id ASC")
    fun getAllSongs(): Flow<List<Song>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1")
    fun getFavoriteSongs(): Flow<List<Song>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<Song>)

    @Update
    suspend fun updateSong(song: Song)

    @Query("UPDATE songs SET isFavorite = :isFavorite WHERE id = :songId")
    suspend fun toggleFavorite(songId: Int, isFavorite: Boolean)

    // DAC Config queries
    @Query("SELECT * FROM dac_config WHERE id = 1 LIMIT 1")
    fun getDacConfig(): Flow<DacConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDacConfig(config: DacConfig)

    @Update
    suspend fun updateDacConfig(config: DacConfig)
}
