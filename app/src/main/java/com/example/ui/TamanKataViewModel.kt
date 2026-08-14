package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.SessionCheckpoint
import com.example.data.Stage
import com.example.data.TamanKataRepository
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.ThinkingConfig

import com.example.network.GenerationConfig
import com.example.network.InlineData
import com.example.network.Part
import com.example.network.RetrofitClient
import android.util.Log
import retrofit2.HttpException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.data.Story
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.LinkedList

data class GeminiEvalResult(val score: Int, val fluency: Int, val isCorrect: Boolean, val intonationMatched: Boolean)

sealed class SessionState {
    object Loading : SessionState()
    data class Playing(val item: LearningItem, val isRecording: Boolean = false, val isEvaluating: Boolean = false) : SessionState()
    data class SttFallback(val item: LearningItem) : SessionState()
    data class Feedback(
        val item: LearningItem,
        val isCorrect: Boolean,
        val showParentHelp: Boolean = false,
        val fluency: Int = 0,
        val intonationMatched: Boolean = false,
        val parentHelpReason: String = "REPEATED_FAIL"
    ) : SessionState()
    data class Comprehension(val item: LearningItem, val questions: List<QuestionData>, val currentQuestionIndex: Int, val correctAnswers: Int) : SessionState()
    data class Graduation(val studentName: String, val totalHours: Double) : SessionState()
    data class ResumePrompt(val checkpoint: SessionCheckpoint) : SessionState()
    data class Error(val item: LearningItem, val message: String, val debugMessage: String? = null) : SessionState()
    object Finished : SessionState()
}

data class QuestionData(val question: String, val options: List<String>, val correctIndex: Int)

class TamanKataViewModel(private val repository: TamanKataRepository) : ViewModel() {

    val hasConsented: StateFlow<Boolean?> = repository.hasConsented
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    val consentTimestamp: StateFlow<Long> = repository.consentTimestamp
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = 0L
        )

    fun setConsent(consented: Boolean) {
        viewModelScope.launch {
            repository.saveConsent(consented)
        }
    }

    fun revokeConsent() {
        viewModelScope.launch {
            repository.revokeConsent()
        }
    }

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

    val masteredItems: StateFlow<List<LearningItem>> = repository.masteredItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val stories: StateFlow<List<Story>> = repository.allStories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val hasGraduated: StateFlow<Boolean> = stages
        .map { list -> list.find { it.id == 8 }?.isUnlocked == true }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState = _sessionState.asStateFlow()

    private var sessionQueue = LinkedList<LearningItem>()
    private var allSessionItems = listOf<LearningItem>()
    private var totalScoreSum = 0
    private var totalFluencySum = 0
    private var totalComprehensionSum = 0
    private var totalWcpmSum = 0
    private var totalWordsRead = 0
    private var independentItemsCount = 0
    private var itemsEvaluatedCount = 0
    private var currentStageId = 0
    private var sessionStartTime = 0L

    // Tracks how many times current item has failed consecutively
    private var currentItemFailCount = 0
    private var currentItemTtsCount = 0
    private var isTimeLimitEnded = false

    fun isSoftLimitReached(): Boolean {
        return sessionStartTime > 0 && (System.currentTimeMillis() - sessionStartTime) >= SOFT_LIMIT_MS
    }

    fun isHardLimitReached(): Boolean {
        return sessionStartTime > 0 && (System.currentTimeMillis() - sessionStartTime) >= HARD_LIMIT_MS
    }

    fun getTotalSessionItems(): Int = allSessionItems.size
    
    fun getCurrentItemProgress(): Pair<Int, Int> {
        val total = allSessionItems.size.coerceAtLeast(1)
        val remaining = sessionQueue.size + if (_sessionState.value is SessionState.Playing || _sessionState.value is SessionState.Feedback || _sessionState.value is SessionState.Comprehension) 1 else 0
        val current = (total - remaining + 1).coerceIn(1, total)
        return Pair(current, total)
    }

    init {
        viewModelScope.launch {
            repository.initializeDummyData()
        }
    }

    fun startSession(stageId: Int) {
        viewModelScope.launch {
            val checkpoint = repository.getSessionCheckpoint()
            val sixHoursMs = 6 * 60 * 60 * 1000L
            if (checkpoint != null && checkpoint.stageId == stageId && (System.currentTimeMillis() - checkpoint.lastUpdateTimestamp < sixHoursMs)) {
                _sessionState.value = SessionState.ResumePrompt(checkpoint)
            } else {
                if (checkpoint != null) {
                    repository.deleteSessionCheckpoint()
                }
                startNewSessionOverridingCheckpoint(stageId)
            }
        }
    }

    fun startNewSessionOverridingCheckpoint(stageId: Int) {
        currentStageId = stageId
        totalScoreSum = 0
        totalFluencySum = 0
        totalComprehensionSum = 0
        totalWcpmSum = 0
        totalWordsRead = 0
        independentItemsCount = 0
        itemsEvaluatedCount = 0
        sessionStartTime = System.currentTimeMillis()
        currentItemFailCount = 0
        currentItemTtsCount = 0
        isTimeLimitEnded = false

        viewModelScope.launch {
            val testingItems = if (stageId == 7) {
                repository.getWeakItems(10)
            } else {
                val items = repository.getItemsForStage(stageId).stateIn(viewModelScope).value
                if (items.isNotEmpty()) items else {
                    listOf("a", "i", "u", "ba", "bi", "bu", "ma", "mi", "mu", "ka").mapIndexed { index, text ->
                        LearningItem(id = index, stageId = stageId, text = text)
                    }
                }
            }
            // Fallback for Stage 8 if no weak items
            val finalItems = if (testingItems.isEmpty() && stageId == 7) {
                listOf(LearningItem(id = 99, stageId = 7, text = "Aku anak pintar."))
            } else testingItems
            
            // Sesi dibatasi maksimal 10 item (prinsip micro-session ramah anak).
            allSessionItems = finalItems.take(10) // Take up to 10 for a session
            sessionQueue = LinkedList(allSessionItems)

            saveCurrentCheckpoint()

            nextItem()
        }
    }

    fun resumeSession(checkpoint: SessionCheckpoint) {
        currentStageId = checkpoint.stageId
        totalScoreSum = checkpoint.totalScoreSum
        totalFluencySum = checkpoint.totalFluencySum
        totalComprehensionSum = checkpoint.totalComprehensionSum
        totalWcpmSum = checkpoint.totalWcpmSum
        totalWordsRead = checkpoint.totalWordsRead
        independentItemsCount = checkpoint.independentItemsCount
        itemsEvaluatedCount = checkpoint.itemsEvaluatedCount
        sessionStartTime = checkpoint.sessionStartTime
        currentItemFailCount = 0
        currentItemTtsCount = 0
        isTimeLimitEnded = false

        viewModelScope.launch {
            val allItems = repository.getItemsForStage(checkpoint.stageId).stateIn(viewModelScope).value
            
            // Reconstruct remaining items from IDs
            val remainingIds = try {
                org.json.JSONArray(checkpoint.remainingItemIds).let { arr ->
                    List(arr.length()) { arr.getInt(it) }
                }
            } catch (e: Exception) { emptyList() }
            
            val remainingItems = remainingIds.mapNotNull { id -> allItems.find { it.id == id } }
            
            allSessionItems = remainingItems // In resume, allSessionItems only tracks remaining so progress bar is relative
            sessionQueue = LinkedList(remainingItems)
            nextItem()
        }
    }

    private fun nextItem() {
        val next = sessionQueue.poll()
        if (next == null) {
            _sessionState.value = SessionState.Finished
        } else {
            currentItemFailCount = 0
            currentItemTtsCount = 0
            _sessionState.value = SessionState.Playing(next)
        }
    }

    private var currentRecordingStartTime = 0L

    fun onTtsPlayed(isReplay: Boolean) {
        if (isReplay) {
            currentItemTtsCount++
        }
    }

    fun setRecording(isRecording: Boolean) {
        val currentState = _sessionState.value
        if (currentState is SessionState.Playing) {
            if (isRecording) {
                currentRecordingStartTime = System.currentTimeMillis()
            }
            _sessionState.value = currentState.copy(isRecording = isRecording)
        }
    }

    fun getRecordingDuration(): Long {
        return System.currentTimeMillis() - currentRecordingStartTime
    }

    fun evaluateAudio(audioBase64: String, isOnline: Boolean = true) {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Playing) return

        val currentItem = currentState.item
        val durationMs = System.currentTimeMillis() - currentRecordingStartTime
        val durationSecs = (durationMs / 1000f).coerceAtLeast(1f)
        _sessionState.value = currentState.copy(isRecording = false, isEvaluating = true)

        if (!isOnline) {
            // Offline fallback: Use OnDevice STT
            _sessionState.value = SessionState.SttFallback(currentItem)
            return
        }

        viewModelScope.launch {
            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey.contains("MY_GEMINI_API_KEY")) {
                _sessionState.value = SessionState.Error(
                    currentItem, 
                    "Konfigurasi API belum siap — hubungi orang tua/developer",
                    "API Key is missing or default"
                )
                return@launch
            }

            val evalResult = evaluateWithGemini(currentItem.text, audioBase64)
            
            if (evalResult.isFailure) {
                _sessionState.value = SessionState.SttFallback(currentItem)
                return@launch
            }
            
            android.util.Log.i("TamanKata_Eval", "Item evaluated via: GEMINI")
            
            val result = evalResult.getOrThrow()
            
            val score = result.score
            val fluency = result.fluency
            val isCorrect = result.isCorrect
            val intonationMatched = result.intonationMatched

            val wordCount = currentItem.text.split(Regex("\\s+")).size
            val wcpm = ((wordCount * (score / 100f)) / (durationSecs / 60f)).toInt()

            handleEvaluationResult(currentItem, score, fluency, isCorrect, intonationMatched, wcpm)
        }
    }
    
    fun manualParentEvaluation(isCorrect: Boolean) {
        val currentState = _sessionState.value
        if (currentState !is SessionState.Feedback) return
        val currentItem = currentState.item
        
        currentItemFailCount = 0
        viewModelScope.launch {
            // Treat as 100 if correct manually, else 0
            handleEvaluationResult(currentItem, if (isCorrect) 100 else 0, 100, isCorrect, true, 20, isFromParent = true)
        }
    }

    private fun saveCurrentCheckpoint() {
        viewModelScope.launch {
            try {
                val remainingIdsJson = org.json.JSONArray(sessionQueue.map { it.id }).toString()
                
                // Construct completed items as allSessionItems minus sessionQueue
                val remainingIdsSet = sessionQueue.map { it.id }.toSet()
                val completedIds = allSessionItems.map { it.id }.filter { it !in remainingIdsSet }
                val completedIdsJson = org.json.JSONArray(completedIds).toString()
                
                val checkpoint = SessionCheckpoint(
                    stageId = currentStageId,
                    completedItemIds = completedIdsJson,
                    remainingItemIds = remainingIdsJson,
                    totalScoreSum = totalScoreSum,
                    totalFluencySum = totalFluencySum,
                    totalComprehensionSum = totalComprehensionSum,
                    totalWcpmSum = totalWcpmSum,
                    totalWordsRead = totalWordsRead,
                    independentItemsCount = independentItemsCount,
                    itemsEvaluatedCount = itemsEvaluatedCount,
                    sessionStartTime = sessionStartTime,
                    lastUpdateTimestamp = System.currentTimeMillis()
                )
                repository.saveSessionCheckpoint(checkpoint)
            } catch(e: Exception) {
                // Ignore
            }
        }
    }

    private suspend fun handleEvaluationResult(item: LearningItem, score: Int, fluency: Int, isCorrect: Boolean, intonationMatched: Boolean, wcpm: Int, isFromParent: Boolean = false) {
        totalScoreSum += score
        totalFluencySum += fluency
        totalWcpmSum += wcpm
        if (currentItemTtsCount == 0) {
            independentItemsCount++
        }
        itemsEvaluatedCount++
        
        // Save progress to DB
        val newFluency = maxOf(item.highestFluencyScore, fluency)
        repository.updateItem(item.copy(
            lastAccuracyScore = score,
            highestFluencyScore = newFluency,
            status = if (isCorrect) "MASTERED" else "IN_PROGRESS",
            attemptCount = item.attemptCount + 1,
            lastTrainedTimestamp = System.currentTimeMillis()
        ))

        if (isCorrect) {
            if (item.extraData.isNotEmpty() && currentStageId == 6) {
                // Parse questions and move to comprehension state
                try {
                    val jsonObj = JSONObject(item.extraData)
                    val questions = mutableListOf<QuestionData>()
                    if (jsonObj.has("q1")) {
                        val opts = jsonObj.getJSONArray("o1").let { arr -> List(arr.length()) { arr.getString(it) } }
                        questions.add(QuestionData(jsonObj.getString("q1"), opts, jsonObj.getInt("a1")))
                    }
                    if (jsonObj.has("q2")) {
                        val opts = jsonObj.getJSONArray("o2").let { arr -> List(arr.length()) { arr.getString(it) } }
                        questions.add(QuestionData(jsonObj.getString("q2"), opts, jsonObj.getInt("a2")))
                    }
                    if (questions.isNotEmpty()) {
                        _sessionState.value = SessionState.Comprehension(item, questions, 0, 0)
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TamanKata_JSON", "Gagal parsing pertanyaan pemahaman: ${e.javaClass.simpleName} - ${e.message}", e)
                }
            }
            _sessionState.value = SessionState.Feedback(item, isCorrect = true, showParentHelp = false, fluency = fluency, intonationMatched = intonationMatched)
        } else {
            currentItemFailCount++
            if (currentItemFailCount >= 3 && !isFromParent) {
                // Ask parent for help
                _sessionState.value = SessionState.Feedback(item, isCorrect = false, showParentHelp = true, parentHelpReason = "REPEATED_FAIL")
            } else {
                // Spaced repetition: add back to queue, e.g., 2 items later
                val reinsertIndex = minOf(2, sessionQueue.size)
                sessionQueue.add(reinsertIndex, item)
                
                _sessionState.value = SessionState.Feedback(item, isCorrect = false, showParentHelp = false)
            }
        }
        
        saveCurrentCheckpoint()
    }

    fun answerComprehension(selectedIndex: Int) {
        val currentState = _sessionState.value
        if (currentState is SessionState.Comprehension) {
            val q = currentState.questions[currentState.currentQuestionIndex]
            val isCorrect = selectedIndex == q.correctIndex
            val newCorrectCount = currentState.correctAnswers + if (isCorrect) 1 else 0
            
            if (currentState.currentQuestionIndex < currentState.questions.size - 1) {
                _sessionState.value = currentState.copy(
                    currentQuestionIndex = currentState.currentQuestionIndex + 1,
                    correctAnswers = newCorrectCount
                )
            } else {
                // Finished comprehension for this item
                val totalQ = currentState.questions.size
                totalComprehensionSum += (newCorrectCount * 100) / totalQ
                saveCurrentCheckpoint()
                _sessionState.value = SessionState.Feedback(currentState.item, isCorrect = true)
            }
        }
    }

    fun continueToNext() {
        if (isHardLimitReached()) {
            isTimeLimitEnded = true
            _sessionState.value = SessionState.Finished
        } else {
            nextItem()
        }
    }

    fun resetToPlaying() {
        val currentState = _sessionState.value
        if (currentState is SessionState.Error) {
            _sessionState.value = SessionState.Playing(currentState.item)
        }
    }

    fun finishSession(onSessionFinished: (duration: Int, itemsCount: Int, avgScore: Int, passed: Boolean, isTimeLimit: Boolean) -> Unit) {
        viewModelScope.launch { repository.deleteSessionCheckpoint() }
        val duration = ((System.currentTimeMillis() - sessionStartTime) / 1000).toInt()
        val avgScore = if (itemsEvaluatedCount > 0) totalScoreSum / itemsEvaluatedCount else 0
        val avgFluency = if (itemsEvaluatedCount > 0) totalFluencySum / itemsEvaluatedCount else 0
        val indepPercentage = if (itemsEvaluatedCount > 0) (independentItemsCount * 100) / itemsEvaluatedCount else 0
        val avgComprehension = if (itemsEvaluatedCount > 0 && currentStageId >= 6) totalComprehensionSum / itemsEvaluatedCount else 0
        val avgWcpm = if (itemsEvaluatedCount > 0) totalWcpmSum / itemsEvaluatedCount else 0
        
        val passed = when (currentStageId) {
            2 -> avgScore >= 80 && avgFluency >= 70 // Tahap 3 (KVK)
            3 -> avgScore >= 75 && indepPercentage >= 80 // Tahap 4 (Kluster)
            4 -> avgScore >= 75 && avgFluency >= 80 // Tahap 5 (3+ Suku kata)
            5 -> avgScore >= 75 // Tahap 6 (Kalimat)
            6 -> avgScore >= 75 && avgComprehension == 100 // Tahap 7 (2 dari 2 = 100%)
            7 -> avgScore >= 75 && avgWcpm >= 20 // Tahap 8 WCPM requirement (e.g., 20 words per minute)
            else -> avgScore >= 70 // Tahap lainnya
        }

        val timeLimitEnded = isTimeLimitEnded

        viewModelScope.launch {
            repository.saveSession(
                SessionHistory(
                    durationSeconds = duration,
                    itemsTrainedCount = itemsEvaluatedCount,
                    averageScore = avgScore,
                    averageFluency = avgFluency,
                    independencePercentage = indepPercentage,
                    averageComprehension = avgComprehension,
                    averageWcpm = avgWcpm
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
            
            if (passed && currentStageId == 7 && !timeLimitEnded) {
                // Graduate!
                val totalSecs = sessionHistory.value.sumOf { it.durationSeconds }
                val hours = totalSecs / 3600.0
                _sessionState.value = SessionState.Graduation("Anak Hebat", hours)
            } else {
                onSessionFinished(duration, allSessionItems.size, avgScore, passed, timeLimitEnded)
            }
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
    private suspend fun evaluateWithGemini(targetText: String, audioBase64: String): Result<GeminiEvalResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = "Kamu adalah guru penilai pengucapan bahasa Indonesia untuk anak-anak TK. " +
                    "Evaluasi apakah audio ucapan anak ini merupakan pengucapan yang benar dari teks target: '${targetText}'. " +
                    "Nilai kemiripan fonemik saja, bukan transkripsi kalimat utuh (karena ini suku kata pendek). " +
                    "Maklumi pelafalan anak kecil yang mungkin kurang sempurna/cadel. " +
                    "Berikan juga skor 'fluency' (kelancaran, 0-100) berdasarkan ada tidaknya jeda mengeja antar suku kata (makin lancar tanpa jeda = makin tinggi). " +
                    "Evaluasi juga 'intonationMatched' (true/false) khusus jika target memiliki tanda baca (?, !, .), apakah nada bicara anak sudah sesuai tanda baca tersebut dan berhenti di koma. " +
                    "PENTING: Jika audio hening/tidak ada ucapan sama sekali/hanya noise latar, maka score = 0, isCorrect = false, fluency = 0. Toleransi pelafalan cadel HANYA berlaku jika anak benar-benar mengucapkan sesuatu yang bisa didengar. " +
                    "Keluarkan JSON tanpa format markdown, murni berisi: {\"score\": <0-100>, \"fluency\": <0-100>, \"isCorrect\": <boolean>, \"intonationMatched\": <boolean>}. " +
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
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    thinkingConfig = ThinkingConfig(thinkingLevel = "low") // Dapat dicoba "minimal" untuk latency lebih rendah jika akurasi tetap terjaga
                )
            )

            val apiKey = com.example.BuildConfig.GEMINI_API_KEY
            val response = RetrofitClient.service.generateContent(apiKey, request)
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Clean up backticks if model returned markdown
            val cleanJson = jsonText.replace("```json", "").replace("```", "").trim()
            val jsonObj = JSONObject(cleanJson)
            val score = jsonObj.optInt("score", 0)
            val fluency = jsonObj.optInt("fluency", 0)
            val isCorrect = jsonObj.optBoolean("isCorrect", false)
            val intonationMatched = jsonObj.optBoolean("intonationMatched", true)
            
            Result.success(GeminiEvalResult(score, fluency, isCorrect, intonationMatched))
        } catch (e: HttpException) {
            val errorBody = try { e.response()?.errorBody()?.string() } catch(e2: Exception) { "Could not read body" }
            val errorMsg = "HTTP ${e.code()} - $errorBody"
            Log.e("TamanKata_GeminiEval", "Evaluasi gagal (HttpException): $errorMsg", e)
            Result.failure(Exception(errorMsg, e))
        } catch (e: Exception) {
            Log.e("TamanKata_GeminiEval", "Evaluasi gagal: ${e.javaClass.simpleName} - ${e.message}", e)
            // Do NOT fallback to mock here, return failure to indicate failure
            Result.failure(e)
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
        // Durasi sesi belajar ideal untuk anak TK B (15 - 20 menit)
        // Dibuat variabel di companion object agar mudah disesuaikan atau diuji
        var SOFT_LIMIT_MS = 15 * 60 * 1000L // 15 menit (dalam milidetik)
        var HARD_LIMIT_MS = 20 * 60 * 1000L // 20 menit (dalam milidetik)

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

    fun evaluateSttResult(item: com.example.data.LearningItem, recognizedText: String?) {
        if (recognizedText == null) {
            // STT failed, fallback to parent
            _sessionState.value = SessionState.Feedback(
                item = item,
                isCorrect = false,
                showParentHelp = true,
                parentHelpReason = "OFFLINE"
            )
            return
        }

        val similarity = com.example.util.SimplePhoneticMatcher.calculateSimilarity(item.text, recognizedText)
        if (similarity >= 70) {
            android.util.Log.i("TamanKata_Eval", "Item evaluated via: ON_DEVICE")
            viewModelScope.launch {
                val wordCount = item.text.split(Regex("\\s+")).size
                val durationSecs = 2f // Approx
                val wcpm = ((wordCount * (similarity / 100f)) / (durationSecs / 60f)).toInt()
                handleEvaluationResult(item, similarity, similarity, true, true, wcpm)
            }
        } else {
            // STT score too low, fallback to parent
            _sessionState.value = SessionState.Feedback(
                item = item,
                isCorrect = false,
                showParentHelp = true,
                parentHelpReason = "OFFLINE"
            )
        }
    }
}
