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

    class FakeConsentPreferences(
        initialConsented: Boolean = true,
        initialTimestamp: Long = 123456789L
    ) : ConsentPreferences() {
        val consentedFlow = MutableStateFlow(initialConsented)
        val tsFlow = MutableStateFlow(initialTimestamp)

        override val hasConsented: Flow<Boolean> = consentedFlow
        override val consentTimestamp: Flow<Long> = tsFlow

        override suspend fun saveConsent(consented: Boolean, timestamp: Long) {
            consentedFlow.value = consented
            tsFlow.value = if (consented) timestamp else 0L
        }

        override suspend fun revokeConsent() {
            consentedFlow.value = false
            tsFlow.value = 0L
        }
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
    fun testFailClosedWhenConsentPreferencesIsNull() = runTest {
        val fakeDao = FakeDao()
        // Skenario: consentPreferences bernilai null (misal karena salah wiring/hilang konfigurasi)
        val repo = TamanKataRepository(fakeDao, consentPreferences = null)
        val viewModel = TamanKataViewModel(repo)

        advanceUntilIdle()

        // SENGAT DIBUAT FAIL-CLOSED DEMI KEAMANAN:
        // Jika consentPreferences null, hasConsented HARUS bernilai false (bukan true)
        // agar aplikasi tidak diam-diam mem-bypass gate persetujuan privasi orang tua.
        assertEquals(false, viewModel.hasConsented.value)
    }

    @Test
    fun testConsentProvidedAndAlreadyGrantedReturnsTrue() = runTest {
        val fakeDao = FakeDao()
        // Skenario positif: consentPreferences disediakan dan sudah pernah menyimpan consented = true
        val fakePrefs = FakeConsentPreferences(initialConsented = true, initialTimestamp = 1700000000000L)
        val repo = TamanKataRepository(fakeDao, consentPreferences = fakePrefs)
        val viewModel = TamanKataViewModel(repo)

        advanceUntilIdle()

        // Memastikan hasConsented mengembalikan true sesuai data yang tersimpan
        assertEquals(true, viewModel.hasConsented.value)
        assertEquals(1700000000000L, viewModel.consentTimestamp.value)

        // Verifikasi alur cabut persetujuan (revoke)
        viewModel.revokeConsent()
        advanceUntilIdle()
        assertEquals(false, viewModel.hasConsented.value)

        // Verifikasi alur pemberian persetujuan ulang (setConsent)
        viewModel.setConsent(true)
        advanceUntilIdle()
        assertEquals(true, viewModel.hasConsented.value)
    }
}
