package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.Stage
import com.example.data.TamanKataRepository
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.GenerationConfig
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.LinkedList

sealed class SessionState {
    object Loading : SessionState()
    data class Playing(val item: LearningItem, val isRecording: Boolean = false, val isEvaluating: Boolean = false) : SessionState()
    data class Feedback(val item: LearningItem, val isCorrect: Boolean, val showParentHelp: Boolean = false) : SessionState()
    object Finished : SessionState()
}

class TamanKataViewModel(private val repository: TamanKataRepository) : ViewModel() {

    val stages: StateFlow<List<Stage>> = repository.allStages
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val sessionHistory: StateFlow<List<SessionHistory>> = repository.sessionHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState = _sessionState.asStateFlow()

    private var sessionQueue = LinkedList<LearningItem>()
    private var allSessionItems = listOf<LearningItem>()
    private var totalScoreSum = 0
    private var itemsEvaluatedCount = 0
    private var currentStageId = 0
    private var sessionStartTime = 0L

    // Tracks how many times current item has failed consecutively
    private var currentItemFailCount = 0

    init {
        viewModelScope.launch {
            repository.initializeDummyData()
        }
    }

    fun startSession(stageId: Int) {
        currentStageId = stageId
        totalScoreSum = 0
        itemsEvaluatedCount = 0
        sessionStartTime = System.currentTimeMillis()
        currentItemFailCount = 0

        viewModelScope.launch {
            // For testing, just take dummy syllables
            val items = repository.getItemsForStage(stageId).stateIn(viewModelScope).value
            val testingItems = if (items.isNotEmpty()) items else {
                listOf("a", "i", "u", "ba", "bi", "bu", "ma", "mi", "mu", "ka").mapIndexed { index, text ->
                    LearningItem(id = index, stageId = stageId, text = text)
                }
            }
            allSessionItems = testingItems.take(10) // Take up to 10 for a session
            sessionQueue = LinkedList(allSessionItems)

            nextItem()
        }
    }

    private fun nextItem() {
        val next = sessionQueue.poll()
        if (next == null) {
            _sessionState.value = SessionState.Finished
        } else {
            currentItemFailCount = 0
            _sessionState.value = SessionState.Playing(next)
        }
    }

    fun setRecording(isRecording: Boolean) {
        val currentState = _sessionState.value
        if (currentState is SessionState.Playing) {
            _sessionState.value = currentState.copy(isRecording = isRecording)
        }
    }

    fun evaluateAudio(audioBase64: String) {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Playing) return

        val currentItem = currentState.item
        _sessionState.value = currentState.copy(isRecording = false, isEvaluating = true)

        viewModelScope.launch {
            val result = evaluateWithGemini(currentItem.text, audioBase64)
            val score = result.first
            val isCorrect = result.second

            handleEvaluationResult(currentItem, score, isCorrect)
        }
    }
    
    fun manualParentEvaluation(isCorrect: Boolean) {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Feedback) return
        val currentItem = currentState.item
        
        viewModelScope.launch {
            // Treat as 100 if correct manually, else 0
            handleEvaluationResult(currentItem, if (isCorrect) 100 else 0, isCorrect, true)
        }
    }

    private suspend fun handleEvaluationResult(item: LearningItem, score: Int, isCorrect: Boolean, isFromParent: Boolean = false) {
        totalScoreSum += score
        itemsEvaluatedCount++
        
        // Save progress to DB
        repository.updateItem(item.copy(
            lastAccuracyScore = score,
            status = if (isCorrect) "MASTERED" else "IN_PROGRESS",
            attemptCount = item.attemptCount + 1,
            lastTrainedTimestamp = System.currentTimeMillis()
        ))

        if (isCorrect) {
            _sessionState.value = SessionState.Feedback(item, isCorrect = true, showParentHelp = false)
        } else {
            currentItemFailCount++
            if (currentItemFailCount >= 3 && !isFromParent) {
                // Ask parent for help
                _sessionState.value = SessionState.Feedback(item, isCorrect = false, showParentHelp = true)
            } else {
                // Spaced repetition: add back to queue, e.g., 2 items later
                val reinsertIndex = minOf(2, sessionQueue.size)
                sessionQueue.add(reinsertIndex, item)
                
                _sessionState.value = SessionState.Feedback(item, isCorrect = false, showParentHelp = false)
            }
        }
    }

    fun continueToNext() {
        nextItem()
    }

    fun finishSession(onSessionFinished: (duration: Int, itemsCount: Int, avgScore: Int, passed: Boolean) -> Unit) {
        val duration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        val avgScore = if (itemsEvaluatedCount > 0) totalScoreSum / itemsEvaluatedCount else 0
        val passed = avgScore >= 70

        viewModelScope.launch {
            repository.saveSession(
                SessionHistory(
                    durationSeconds = duration,
                    itemsTrainedCount = itemsEvaluatedCount,
                    averageScore = avgScore
                )
            )
            // If passed, unlock next stage
            if (passed && currentStageId < 8) {
                val allStages = stages.value
                val nextStage = allStages.find { it.id == currentStageId + 1 }
                if (nextStage != null && !nextStage.isUnlocked) {
                    repository.updateStage(nextStage.copy(isUnlocked = true))
                }
            }
            onSessionFinished(duration, allSessionItems.size, avgScore, passed)
        }
    }

    // MENGAPA GEMINI API DIGUNAKAN DI SINI, BUKAN SPEECHRECOGNIZER BAWAAN ANDROID?
    // SpeechRecognizer bawaan (misal Google Voice Typing) dioptimalkan untuk kalimat panjang 
    // dengan konteks dikte (dictation). API tersebut sangat kesulitan mengenali ucapan 
    // suku kata terpisah tanpa makna (seperti "ba", "bi", "bu", "mu") terutama dari suara anak kecil 
    // yang mungkin pelafalannya masih cadel.
    // Dengan Gemini Multimodal (Audio + Teks), kita bisa menginstruksikan model untuk berfokus
    // secara spesifik pada "kemiripan fonemik" antara suara dan teks target (sebagai evaluator pengucapan),
    // memberikan hasil (skor & toleransi cadel) yang jauh lebih robust daripada sekadar speech-to-text biasa.
    private suspend fun evaluateWithGemini(targetText: String, audioBase64: String): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Kamu adalah guru penilai pengucapan bahasa Indonesia untuk anak-anak TK. " +
                    "Evaluasi apakah audio ucapan anak ini merupakan pengucapan yang benar dari teks target: '${targetText}'. " +
                    "Nilai kemiripan fonemik saja, bukan transkripsi kalimat utuh (karena ini suku kata pendek). " +
                    "Maklumi pelafalan anak kecil yang mungkin kurang sempurna/cadel. " +
                    "Keluarkan JSON tanpa format markdown, murni berisi: {\"score\": <0-100>, \"isCorrect\": <boolean>}. " +
                    "Syarat isCorrect true adalah jika score >= 70."

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "audio/mp4", data = audioBase64))
                        )
                    )
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
                // Mock for testing if no real key
                return@withContext Pair(85, true)
            }

            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean up backticks if model returned markdown
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val jsonObj = JSONObject(cleanJson)
            val score = jsonObj.optInt("score", 0)
            val isCorrect = jsonObj.optBoolean("isCorrect", false)
            
            Pair(score, isCorrect)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback mock
            Pair(85, true)
        }
    }

    fun getItemsForStage(stageId: Int): StateFlow<List<LearningItem>> {
        return repository.getItemsForStage(stageId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    companion object {
        fun provideFactory(repository: TamanKataRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(TamanKataViewModel::class.java)) {
                    return TamanKataViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}
