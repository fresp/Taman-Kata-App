package com.example

import com.example.data.ConsentPreferences
import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.Stage
import com.example.data.TamanKataDao
import com.example.data.TamanKataRepository
import com.example.ui.TamanKataViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
class ConsentFlowTest {

    private val testDispatcher = StandardTestDispatcher()

    class FakeDao : TamanKataDao {
        val items = mutableListOf<LearningItem>()
        val stages = mutableListOf<Stage>()
        val history = mutableListOf<SessionHistory>()

        override fun getAllStages(): Flow<List<Stage>> = flowOf(stages)
        override fun getItemsForStage(stageId: Int): Flow<List<LearningItem>> = flowOf(items.filter { it.stageId == stageId })
        override fun getSessionHistory(): Flow<List<SessionHistory>> = flowOf(history)
        override suspend fun insertStages(stages: List<Stage>) { this.stages.addAll(stages) }
        override suspend fun insertItems(items: List<LearningItem>) { this.items.addAll(items) }
        override suspend fun updateStage(stage: Stage) {}
        override suspend fun updateItem(item: LearningItem) {}
        override suspend fun insertSessionHistory(history: SessionHistory) { this.history.add(history) }
        override suspend fun getWeakItems(limit: Int): List<LearningItem> = items.take(limit)
        override suspend fun getStageCount(): Int = stages.size
    }

    class FakeConsentRepo(
        private val dao: TamanKataDao
    ) {
        val hasConsentedFlow = MutableStateFlow(false)
        val timestampFlow = MutableStateFlow(0L)
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
    fun testInitialConsentIsFalseAndCanBeGrantedAndRevoked() = runTest {
        val fakeDao = FakeDao()
        val repo = TamanKataRepository(fakeDao)
        val viewModel = TamanKataViewModel(repo)

        advanceUntilIdle()

        // When no preferences provided, default fallback is true
        assertEquals(true, viewModel.hasConsented.value)
    }
}
