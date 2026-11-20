package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.MilestoneCatalog
import com.example.data.MilestoneOverview
import com.example.data.MilestoneRepository
import com.example.data.SolveSessionEntity
import com.example.data.TrainingRepository
import com.example.data.UserProfile
import com.example.data.UserProfileRepository
import com.example.data.WcaMetric
import com.example.data.WcaTimerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val todayCompleted: Int = 0,
    val todayTarget: Int = 5,
    val streakDays: Int = 0,
    val bestTimeMs: Long? = null,
    val ao5Ms: Long? = null,
    val totalSolves: Int = 0,
    val recentSessions: List<SolveSessionEntity> = emptyList(),
    val wcaTotalSolves: Int = 0,
    val wcaPb: WcaMetric = WcaMetric.None,
    val wcaAo5: WcaMetric = WcaMetric.None,
    val wcaAo12: WcaMetric = WcaMetric.None,
    val unlockedBadgesCount: Int = 0,
    val totalBadgesCount: Int = MilestoneCatalog.all.size,
    val unseenBadgesCount: Int = 0
)

class HomeViewModel(
    private val repository: TrainingRepository,
    private val userProfileRepository: UserProfileRepository,
    private val wcaTimerRepository: WcaTimerRepository,
    private val milestoneRepository: MilestoneRepository,
    private val appContainer: AppContainer
) : ViewModel() {

    private val baseMetricsFlow = combine(
        repository.allSessions,
        repository.recentDailyGoals,
        repository.observeTodayGoal(),
        userProfileRepository.userProfile,
        wcaTimerRepository.allSolves
    ) { sessions, goals, todayGoal, profile, wcaSolves ->
        HomeUiState(
            userProfile = profile,
            todayCompleted = todayGoal?.completedSolves ?: 0,
            todayTarget = todayGoal?.targetSolves ?: 5,
            streakDays = repository.calculateStreakDays(goals),
            bestTimeMs = sessions.minOfOrNull { it.totalDurationMs },
            ao5Ms = repository.computeAo5(sessions),
            totalSolves = sessions.size,
            recentSessions = sessions.take(3),
            wcaTotalSolves = wcaSolves.size,
            wcaPb = wcaTimerRepository.computeBestPb(wcaSolves),
            wcaAo5 = wcaTimerRepository.computeWcaAo5(wcaSolves),
            wcaAo12 = wcaTimerRepository.computeWcaAo12(wcaSolves)
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        baseMetricsFlow,
        milestoneRepository.milestoneOverview
    ) { base: HomeUiState, milestones: MilestoneOverview ->
        base.copy(
            streakDays = maxOf(base.streakDays, milestones.currentStreakDays),
            unlockedBadgesCount = milestones.unlockedCount,
            totalBadgesCount = milestones.totalCount,
            unseenBadgesCount = milestones.unseenCount
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
        private val userProfileRepository: UserProfileRepository,
        private val wcaTimerRepository: WcaTimerRepository,
        private val milestoneRepository: MilestoneRepository,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(
                repository = repository,
                userProfileRepository = userProfileRepository,
                wcaTimerRepository = wcaTimerRepository,
                milestoneRepository = milestoneRepository,
                appContainer = appContainer
            ) as T
        }
    }
}
