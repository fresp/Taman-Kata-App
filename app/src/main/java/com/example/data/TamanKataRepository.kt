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
            val stages = listOf(
                Stage(id = 0, title = "Tahap 1: Vokal", isUnlocked = true),
                Stage(id = 1, title = "Tahap 2: KV", isUnlocked = false),
                Stage(id = 2, title = "Tahap 3: KVK", isUnlocked = false),
                Stage(id = 3, title = "Tahap 4: Kluster", isUnlocked = false),
                Stage(id = 4, title = "Tahap 5", isUnlocked = false),
                Stage(id = 5, title = "Tahap 6", isUnlocked = false),
                Stage(id = 6, title = "Tahap 7", isUnlocked = false),
                Stage(id = 7, title = "Tahap 8", isUnlocked = false),
                Stage(id = 8, title = "Tahap 9", isUnlocked = false)
            )
            dao.insertStages(stages)
            
            val items = mutableListOf<LearningItem>()
            // Stage 0 (Tahap 1)
            items.add(LearningItem(stageId = 0, text = "a", syllables = "a"))
            items.add(LearningItem(stageId = 0, text = "i", syllables = "i"))
            items.add(LearningItem(stageId = 0, text = "u", syllables = "u"))
            
            // Stage 1 (Tahap 2)
            items.add(LearningItem(stageId = 1, text = "ba", syllables = "ba"))
            items.add(LearningItem(stageId = 1, text = "bi", syllables = "bi"))
            items.add(LearningItem(stageId = 1, text = "bu", syllables = "bu"))
            
            // Stage 2 (Tahap 3 - Suku Kata Tertutup)
            val stage2Items = listOf("ma-kan", "mi-num", "du-duk", "ku-cing", "pin-tar", "kan-cil", "ban-tal", "can-tik")
            stage2Items.forEach { 
                items.add(LearningItem(stageId = 2, text = it.replace("-", ""), syllables = it))
            }
            
            // Stage 3 (Tahap 4 - Kluster)
            val stage3Items = listOf("per-gi", "ko-tak", "sen-dok", "tem-pat", "si-kat", "ba-pak", "ker-tas", "ce-pat")
            stage3Items.forEach {
                items.add(LearningItem(stageId = 3, text = it.replace("-", ""), syllables = it))
            }
            
            // Rest
            for (i in 4..8) {
                items.add(LearningItem(stageId = i, text = "ta-mat", syllables = "ta-mat"))
            }
            
            dao.insertItems(items)
        }
    }
}
