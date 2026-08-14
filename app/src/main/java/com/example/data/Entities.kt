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
    val status: String = "NOT_TRIED", // NOT_TRIED, IN_PROGRESS, MASTERED
    val lastAccuracyScore: Int = 0, // 0-100
    val attemptCount: Int = 0,
    val lastTrainedTimestamp: Long = 0L
)

@Entity(tableName = "session_history")
data class SessionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val durationSeconds: Int,
    val itemsTrainedCount: Int,
    val averageScore: Int,
    val timestamp: Long = System.currentTimeMillis()
)
