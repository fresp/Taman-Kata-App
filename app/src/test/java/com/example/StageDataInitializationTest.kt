package com.example

import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.Stage
import com.example.data.TamanKataDao
import com.example.data.TamanKataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StageDataInitializationTest {

    class FakeDao : TamanKataDao {
        val stages = mutableListOf<Stage>()
        val items = mutableListOf<LearningItem>()
        val history = mutableListOf<SessionHistory>()
        val stories = mutableListOf<com.example.data.Story>()
        override fun getAllStories(): Flow<List<com.example.data.Story>> = flowOf(stories)
        override suspend fun getStoryCount(): Int = stories.size
        override suspend fun insertStories(stories: List<com.example.data.Story>) { this.stories.addAll(stories) }
        var checkpoint: com.example.data.SessionCheckpoint? = null
        override suspend fun getSessionCheckpoint(): com.example.data.SessionCheckpoint? = checkpoint
        override suspend fun insertOrUpdateSessionCheckpoint(cp: com.example.data.SessionCheckpoint) { checkpoint = cp }
        override suspend fun deleteSessionCheckpoint() { checkpoint = null }

        override fun getAllStages(): Flow<List<Stage>> = flowOf(stages)
        override fun getItemsForStage(stageId: Int): Flow<List<LearningItem>> = flowOf(items.filter { it.stageId == stageId })
        override fun getSessionHistory(): Flow<List<SessionHistory>> = flowOf(history)
        override fun getMasteredItems(): Flow<List<LearningItem>> = flowOf(items.filter { it.status == "MASTERED" })
        override suspend fun insertStages(stages: List<Stage>) { this.stages.addAll(stages) }
        override suspend fun insertItems(items: List<LearningItem>) { this.items.addAll(items) }
        override suspend fun updateStage(stage: Stage) {}
        override suspend fun updateItem(item: LearningItem) {}
        override suspend fun insertSessionHistory(history: SessionHistory) { this.history.add(history) }
        override suspend fun getWeakItems(limit: Int): List<LearningItem> = items.take(limit)
        override suspend fun getStageCount(): Int = stages.size
    }

    @Test
    fun testStage0AndStage1ItemsInitialization() = runTest {
        val fakeDao = FakeDao()
        val repo = TamanKataRepository(fakeDao)

        repo.initializeDummyData()

        val stage0Items = fakeDao.items.filter { it.stageId == 0 }
        val stage1Items = fakeDao.items.filter { it.stageId == 1 }

        // Verify Stage 0 has 5 vowels (a, i, u, e, o)
        assertEquals(5, stage0Items.size)
        val expectedVowels = listOf("a", "i", "u", "e", "o")
        assertEquals(expectedVowels, stage0Items.map { it.text })
        stage0Items.forEach {
            assertEquals(it.text, it.syllables)
        }

        // Verify Stage 1 has 105 items (21 consonant families x 5 vowels)
        assertEquals(105, stage1Items.size)
        val expectedConsonants = listOf("b", "m", "p", "t", "d", "n", "l", "k", "s", "r", "g", "h", "c", "j", "w", "y", "f", "v", "z", "ng", "ny")
        val expectedKV = expectedConsonants.flatMap { c -> expectedVowels.map { v -> "$c$v" } }
        assertEquals(expectedKV, stage1Items.map { it.text })
        stage1Items.forEach {
            assertEquals(it.text, it.syllables)
        }
    }
}
