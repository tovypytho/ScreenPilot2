package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()

    suspend fun insert(entry: HistoryEntry, limit: Int = 30) {
        historyDao.insertEntry(entry)
        historyDao.pruneHistory(limit)
    }

    suspend fun clear() {
        historyDao.clearHistory()
    }
}
