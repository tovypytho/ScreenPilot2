package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HistoryEntry): Long

    @Query("DELETE FROM history_entries WHERE id NOT IN (SELECT id FROM (SELECT id FROM history_entries ORDER BY timestamp DESC LIMIT :limit))")
    suspend fun pruneHistory(limit: Int)

    @Query("DELETE FROM history_entries")
    suspend fun clearHistory()
}
