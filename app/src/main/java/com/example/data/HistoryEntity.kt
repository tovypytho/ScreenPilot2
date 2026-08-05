package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

object HistoryQuestionType {
    const val MULTIPLE_CHOICE = "MULTIPLE_CHOICE"
    const val MULTIPLE_SELECT = "MULTIPLE_SELECT"
    const val FREE_RESPONSE = "FREE_RESPONSE"
    const val UNCLEAR = "UNCLEAR"
    const val ERROR = "ERROR"
}

@Entity(tableName = "history_entries")
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val answerIndex: Int, // 1..5 for single MC, 0 for multi-select/non-MC success, -1 for error
    val confidence: Double?,
    val modelName: String,
    val timestamp: Long,
    val requestDurationMs: Long,
    val httpStatus: Int,
    val errorMessage: String?,
    val imageCount: Int = 1,
    val captureMode: String = "Single",
    val successfulKeySlotId: String? = null,
    val successfulKeyLabel: String? = null,
    val keyAttempts: Int = 0,
    val sameKeyRetries: Int = 0,
    val failoverUsed: Boolean = false,
    @ColumnInfo(defaultValue = "'MULTIPLE_CHOICE'")
    val questionType: String = HistoryQuestionType.MULTIPLE_CHOICE,
    // FREE_RESPONSE stores the answer text. MULTIPLE_SELECT stores sorted indices as CSV, e.g. "1,2".
    val answerText: String? = null
)
