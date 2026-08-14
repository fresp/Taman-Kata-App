package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LearningItem
import com.example.data.SessionHistory
import com.example.data.Stage
import com.example.data.TamanKataRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    init {
        viewModelScope.launch {
            repository.initializeDummyData()
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

    fun saveSession(duration: Int, itemsTrained: Int, averageScore: Int, currentStageId: Int, passed: Boolean) {
        viewModelScope.launch {
            repository.saveSession(
                SessionHistory(
                    durationSeconds = duration,
                    itemsTrainedCount = itemsTrained,
                    averageScore = averageScore
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
        }
    }

    fun updateItemProgress(item: LearningItem, score: Int) {
        viewModelScope.launch {
            val status = if (score >= 80) "MASTERED" else "IN_PROGRESS"
            repository.updateItem(
                item.copy(
                    lastAccuracyScore = score,
                    status = status,
                    attemptCount = item.attemptCount + 1,
                    lastTrainedTimestamp = System.currentTimeMillis()
                )
            )
        }
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
