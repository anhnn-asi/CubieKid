package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyGoalEntity
import com.example.data.SolveSessionEntity
import com.example.data.TrainingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val todayCompleted: Int = 0,
    val todayTarget: Int = 5,
    val streakDays: Int = 0,
    val bestTimeMs: Long? = null,
    val ao5Ms: Long? = null,
    val totalSolves: Int = 0,
    val recentSessions: List<SolveSessionEntity> = emptyList()
)

class HomeViewModel(
    private val repository: TrainingRepository,
    private val appContainer: AppContainer
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repository.allSessions,
        repository.recentDailyGoals,
        repository.observeTodayGoal()
    ) { sessions, goals, todayGoal ->
        HomeUiState(
            todayCompleted = todayGoal?.completedSolves ?: 0,
            todayTarget = todayGoal?.targetSolves ?: 5,
            streakDays = repository.calculateStreakDays(goals),
            bestTimeMs = sessions.minOfOrNull { it.totalDurationMs },
            ao5Ms = repository.computeAo5(sessions),
            totalSolves = sessions.size,
            recentSessions = sessions.take(3)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun startPracticeChallenge() {
        appContainer.generateNewPracticeCube()
    }

    fun updateDailyTarget(target: Int) {
        viewModelScope.launch {
            repository.updateTodayTarget(target)
        }
    }

    class Factory(
        private val repository: TrainingRepository,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository, appContainer) as T
        }
    }
}
