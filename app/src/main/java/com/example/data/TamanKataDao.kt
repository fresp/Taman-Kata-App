package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TamanKataDao {
    @Query("SELECT * FROM stages ORDER BY id ASC")
    fun getAllStages(): Flow<List<Stage>>

    @Query("SELECT * FROM learning_items WHERE stageId = :stageId ORDER BY id ASC")
    fun getItemsForStage(stageId: Int): Flow<List<LearningItem>>

    @Query("SELECT * FROM session_history ORDER BY timestamp DESC")
    fun getSessionHistory(): Flow<List<SessionHistory>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStages(stages: List<Stage>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertItems(items: List<LearningItem>)

    @Update
    suspend fun updateStage(stage: Stage)

    @Update
    suspend fun updateItem(item: LearningItem)

    @Insert
    suspend fun insertSessionHistory(history: SessionHistory)
    
    @Query("SELECT * FROM learning_items WHERE attemptCount > 0 AND lastAccuracyScore < 80 ORDER BY lastAccuracyScore ASC LIMIT :limit")
    suspend fun getWeakItems(limit: Int): List<LearningItem>

    @Query("SELECT COUNT(*) FROM stages")
    suspend fun getStageCount(): Int
}
