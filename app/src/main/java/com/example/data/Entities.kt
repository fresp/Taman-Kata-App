package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stages")
data class Stage(
    @PrimaryKey val id: Int, // 0 to 8
    val title: String,
    val isUnlocked: Boolean = false
)

@Entity(tableName = "learning_items")
data class LearningItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val stageId: Int,
    val text: String,
    val syllables: String = "",
    val status: String = "NOT_TRIED", // NOT_TRIED, IN_PROGRESS, MASTERED
    val lastAccuracyScore: Int = 0, // 0-100
    val highestFluencyScore: Int = 0, // 0-100
    val extraData: String = "", // Used for comprehension questions JSON
    val attemptCount: Int = 0,
    val lastTrainedTimestamp: Long = 0L
)

@Entity(tableName = "session_history")
data class SessionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationSeconds: Int,
    val itemsTrainedCount: Int,
    val averageScore: Int,
    val averageFluency: Int = 0,
    val independencePercentage: Int = 0,
    val averageComprehension: Int = 0,
    val averageWcpm: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "session_checkpoint")
data class SessionCheckpoint(
    @PrimaryKey val id: Int = 1,
    val stageId: Int,
    val completedItemIds: String, // JSON array of Int
    val remainingItemIds: String, // JSON array of Int
    val totalScoreSum: Int,
    val totalFluencySum: Int,
    val totalComprehensionSum: Int,
    val totalWcpmSum: Int,
    val totalWordsRead: Int,
    val independentItemsCount: Int,
    val itemsEvaluatedCount: Int,
    val sessionStartTime: Long,
    val lastUpdateTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "stories")
data class Story(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val body: String, // Paragraphs separated by \n\n
    val category: String,
    val sourceAttribution: String = ""
)
