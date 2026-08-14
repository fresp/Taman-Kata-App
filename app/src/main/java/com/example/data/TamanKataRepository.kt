package com.example.data

import kotlinx.coroutines.flow.Flow

class TamanKataRepository(
    private val dao: TamanKataDao,
    private val consentPreferences: ConsentPreferences? = null
) {
    val allStages: Flow<List<Stage>> = dao.getAllStages()
    val sessionHistory: Flow<List<SessionHistory>> = dao.getSessionHistory()

    val hasConsented: Flow<Boolean> = consentPreferences?.hasConsented ?: kotlinx.coroutines.flow.flowOf(false)
    val consentTimestamp: Flow<Long> = consentPreferences?.consentTimestamp ?: kotlinx.coroutines.flow.flowOf(0L)

    suspend fun saveConsent(consented: Boolean, timestamp: Long = System.currentTimeMillis()) {
        consentPreferences?.saveConsent(consented, timestamp)
    }

    suspend fun revokeConsent() {
        consentPreferences?.revokeConsent()
    }

    fun getItemsForStage(stageId: Int): Flow<List<LearningItem>> {
        return dao.getItemsForStage(stageId)
    }

    suspend fun updateStage(stage: Stage) = dao.updateStage(stage)
    suspend fun updateItem(item: LearningItem) = dao.updateItem(item)
    suspend fun saveSession(history: SessionHistory) = dao.insertSessionHistory(history)
    suspend fun getWeakItems(limit: Int) = dao.getWeakItems(limit)
    
    suspend fun initializeDummyData() {
        if (dao.getStageCount() == 0) {
            val stages = listOf(
                Stage(id = 0, title = "Tahap 1: Vokal", isUnlocked = true),
                Stage(id = 1, title = "Tahap 2: KV", isUnlocked = false),
                Stage(id = 2, title = "Tahap 3: KVK", isUnlocked = false),
                Stage(id = 3, title = "Tahap 4: Kluster", isUnlocked = false),
                Stage(id = 4, title = "Tahap 5: 3+ Suku Kata", isUnlocked = false),
                Stage(id = 5, title = "Tahap 6: Kalimat", isUnlocked = false),
                Stage(id = 6, title = "Tahap 7: Paragraf Mini", isUnlocked = false),
                Stage(id = 7, title = "Tahap 8: Kelancaran", isUnlocked = false),
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
            
            // Stage 4 (Tahap 5)
            val stage4Items = listOf("ke-lin-ci", "se-pa-tu", "ma-ta-ha-ri", "ke-la-pa", "ce-la-na")
            stage4Items.forEach {
                items.add(LearningItem(stageId = 4, text = it.replace("-", ""), syllables = it))
            }

            // Stage 5 (Tahap 6)
            val stage5Items = listOf("Ini bola saya.", "Kucing itu lucu.", "Adik makan nasi.", "Ayo bermain!")
            stage5Items.forEach {
                items.add(LearningItem(stageId = 5, text = it, syllables = it))
            }

            // Stage 6 (Tahap 7) Paragraf
            val p1Extra = "{\"q1\": \"Hewan apa peliharaan Budi?\", \"o1\": [\"Anjing 🐶\", \"Kucing 🐱\"], \"a1\": 1, \"q2\": \"Warna apa kucingnya?\", \"o2\": [\"Hitam ⚫\", \"Putih ⚪\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Ini kucing Budi. Kucing Budi warna hitam. Budi suka main bola sama kucing.", extraData = p1Extra))
            
            val p2Extra = "{\"q1\": \"Siapa yang lari?\", \"o1\": [\"Kelinci 🐇\", \"Kura-kura 🐢\"], \"a1\": 0, \"q2\": \"Di mana kelinci lari?\", \"o2\": [\"Di taman 🌳\", \"Di rumah 🏠\"], \"a2\": 0}"
            items.add(LearningItem(stageId = 6, text = "Kelinci suka lari. Kelinci lari di taman. Taman itu sangat luas.", extraData = p2Extra))
            
            // Rest
            for (i in 7..8) {
                items.add(LearningItem(stageId = i, text = "ta-mat", syllables = "ta-mat"))
            }
            
            dao.insertItems(items)
        }
    }
}
