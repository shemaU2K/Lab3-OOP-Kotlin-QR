package com.qrvault.scanner.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {

    @Query("SELECT * FROM scan_history ORDER BY scanned_at DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query(
        """
        SELECT * FROM scan_history
        WHERE (:typeFilter IS NULL OR payload_type = :typeFilter)
          AND (:onlyFavorites = 0 OR is_favorite = 1)
          AND (raw_value LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' OR display_title LIKE '%' || :query || '%')
        ORDER BY scanned_at DESC
        """
    )
    fun searchScans(
        query: String,
        typeFilter: String?,
        onlyFavorites: Boolean
    ): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanHistoryEntity): Long

    @Query("UPDATE scan_history SET is_favorite = :favorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, favorite: Boolean)

    @Query("UPDATE scan_history SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String)

    @Delete
    suspend fun delete(entity: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}
