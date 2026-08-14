package com.example

import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.Stage
import com.example.data.TamanKataDao
import com.example.data.TamanKataRepository
import com.example.ui.SessionState
import com.example.ui.TamanKataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OfflineFallbackTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeDao : TamanKataDao {
        val items = mutableListOf<LearningItem>()
        val stages = mutableListOf<Stage>()
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
        override suspend fun updateItem(item: LearningItem) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx >= 0) items[idx] = item
        }
        override suspend fun insertSessionHistory(history: SessionHistory) { this.history.add(history) }
        override suspend fun getWeakItems(limit: Int): List<LearningItem> = items.take(limit)
        override suspend fun getStageCount(): Int = stages.size
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testOfflineFallbackDirectlyTriggersParentHelpWithOfflineReason() = runTest {
        val fakeDao = FakeDao()
        fakeDao.items.add(LearningItem(id = 1, stageId = 1, text = "ba", syllables = "ba"))
        val repo = TamanKataRepository(fakeDao)
        val viewModel = TamanKataViewModel(repo)

        viewModel.startSession(1)
        advanceUntilIdle()

        assertTrue(viewModel.sessionState.value is SessionState.Playing)

        // Evaluate when offline (isOnline = false)
        viewModel.evaluateAudio("dummy_audio_base64", isOnline = false)
        advanceUntilIdle()

        val state = viewModel.sessionState.value
        assertTrue("State must be SttFallback", state is SessionState.SttFallback)

        // Parent confirms it is correct
        viewModel.evaluateSttResult((state as SessionState.SttFallback).item, null)
        advanceUntilIdle()
        val sttFailedState = viewModel.sessionState.value
        assertTrue("State must be Feedback", sttFailedState is SessionState.Feedback)
        val feedback = sttFailedState as SessionState.Feedback
        assertTrue("showParentHelp must be true", feedback.showParentHelp)
        assertEquals("OFFLINE", feedback.parentHelpReason)
        viewModel.manualParentEvaluation(isCorrect = true)
        advanceUntilIdle()

        val updatedState = viewModel.sessionState.value
        assertTrue("State should transition to Feedback mastered", updatedState is SessionState.Feedback)
        val updatedFeedback = updatedState as SessionState.Feedback
        assertTrue(updatedFeedback.isCorrect)
        assertFalse(updatedFeedback.showParentHelp)
    }
}
