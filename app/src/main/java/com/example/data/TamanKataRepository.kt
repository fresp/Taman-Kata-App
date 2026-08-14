package com.example.data

import kotlinx.coroutines.flow.Flow

class TamanKataRepository(private val dao: TamanKataDao) {
    val allStages: Flow<List<Stage>> = dao.getAllStages()
    val sessionHistory: Flow<List<SessionHistory>> = dao.getSessionHistory()

    fun getItemsForStage(stageId: Int): Flow<List<LearningItem>> {
        return dao.getItemsForStage(stageId)
    }

    suspend fun updateStage(stage: Stage) = dao.updateStage(stage)
    suspend fun updateItem(item: LearningItem) = dao.updateItem(item)
    suspend fun saveSession(history: SessionHistory) = dao.insertSessionHistory(history)
    
    suspend fun initializeDummyData() {
        if (dao.getStageCount() == 0) {
            val stages = (0..8).map { i ->
                Stage(
                    id = i, 
                    title = "Tahap ${i + 1}", 
                    isUnlocked = i == 0 // Only first stage is unlocked initially
                )
            }
            dao.insertStages(stages)
            
            val items = mutableListOf<LearningItem>()
            for (i in 0..8) {
                // Add 3 dummy items per stage
                items.add(LearningItem(stageId = i, text = "ba"))
                items.add(LearningItem(stageId = i, text = "bi"))
                items.add(LearningItem(stageId = i, text = "bu"))
            }
            dao.insertItems(items)
        }
    }
}
