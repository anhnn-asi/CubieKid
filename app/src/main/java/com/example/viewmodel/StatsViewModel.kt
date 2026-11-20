package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyGoalEntity
import com.example.data.PhaseSpeedBreakdown
import com.example.data.SolveSessionEntity
import com.example.data.TrainingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SessionFilter(val viLabel: String) {
    ALL("Tất cả"),
    CAMERA("Từ Camera"),
    PRACTICE("Tự tập")
}

data class StatsUiState(
    val filter: SessionFilter = SessionFilter.ALL,
    val sessions: List<SolveSessionEntity> = emptyList(),
    val dailyGoals: List<DailyGoalEntity> = emptyList(),
    val bestTimeMs: Long? = null,
    val ao5Ms: Long? = null,
    val averageTimeMs: Long? = null,
    val phaseBreakdown: PhaseSpeedBreakdown? = null,
    val streakDays: Int = 0
)

class StatsViewModel(
    private val repository: TrainingRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(SessionFilter.ALL)

    val uiState: StateFlow<StatsUiState> = combine(
        repository.allSessions,
        repository.recentDailyGoals,
        _filter
    ) { allSessions, goals, filter ->
        val filtered = when (filter) {
            SessionFilter.ALL -> allSessions
            SessionFilter.CAMERA -> allSessions.filter { it.sourceType == "CAMERA_VIDEO" }
            SessionFilter.PRACTICE -> allSessions.filter { it.sourceType == "PRACTICE_SCRAMBLE" }
        }
        StatsUiState(
            filter = filter,
            sessions = filtered,
            dailyGoals = goals.reversed(), // chronological left-to-right for chart
            bestTimeMs = filtered.minOfOrNull { it.totalDurationMs },
            ao5Ms = repository.computeAo5(filtered),
            averageTimeMs = if (filtered.isEmpty()) null else filtered.map { it.totalDurationMs }.average().toLong(),
            phaseBreakdown = repository.computePhaseBreakdown(filtered),
            streakDays = repository.calculateStreakDays(goals)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StatsUiState()
    )

    fun setFilter(filter: SessionFilter) {
        _filter.value = filter
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    class Factory(
        private val repository: TrainingRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StatsViewModel(repository) as T
        }
    }
}
