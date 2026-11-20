package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppSettings
import com.example.data.SettingsRepository
import com.example.data.SolveSessionEntity
import com.example.data.TrainingRepository
import com.example.rubik.CfopSolution
import com.example.rubik.CfopSolver
import com.example.rubik.CfopStage
import com.example.rubik.CubeState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SolverTabMode {
    GUIDE,
    TIMER
}

enum class TimerPhase {
    IDLE,
    RUNNING_CROSS,
    RUNNING_F2L,
    RUNNING_OLL,
    RUNNING_PLL,
    COMPLETED
}

data class SolverTimerUiState(
    val activeTab: SolverTabMode = SolverTabMode.GUIDE,
    val sourceType: String = "PRACTICE_SCRAMBLE",
    val scrambleNotation: String = "",
    val solution: CfopSolution? = null,
    val isComputingSolution: Boolean = true,
    val currentStepIndex: Int = 0, // 0 = initial state, 1..steps.size = after step i
    val isAutoPlaying: Boolean = false,
    val playStepDelayMs: Long = AppSettings.DEFAULT_PLAY_STEP_DELAY_MS,
    // Interactive 4-stage tap-to-advance timer
    val timerPhase: TimerPhase = TimerPhase.IDLE,
    val elapsedTotalMs: Long = 0L,
    val crossSplitMs: Long = 0L,
    val f2lSplitMs: Long = 0L,
    val ollSplitMs: Long = 0L,
    val pllSplitMs: Long = 0L,
    val savedSession: SolveSessionEntity? = null
) {
    val displayedCube: CubeState
        get() {
            val sol = solution ?: return CubeState.solved()
            return if (currentStepIndex <= 0 || sol.steps.isEmpty()) {
                sol.initialCube
            } else {
                sol.steps[(currentStepIndex - 1).coerceIn(0, sol.steps.lastIndex)].cubeAfter
            }
        }
}

class SolverTimerViewModel(
    private val repository: TrainingRepository,
    private val settingsRepository: SettingsRepository,
    private val appContainer: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(SolverTimerUiState())
    val uiState: StateFlow<SolverTimerUiState> = _uiState.asStateFlow()

    private var autoPlayJob: Job? = null
    private var timerJob: Job? = null
    private var phaseStartMs: Long = 0L
    private var solveStartMs: Long = 0L

    init {
        viewModelScope.launch {
            appContainer.activeChallenge.collectLatest { challenge ->
                computeSolutionFor(challenge)
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collectLatest { appSettings ->
                _uiState.update { it.copy(playStepDelayMs = appSettings.playStepDelayMs) }
            }
        }
    }

    private suspend fun computeSolutionFor(challenge: ActiveCubeChallenge) {
        autoPlayJob?.cancel()
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                sourceType = challenge.sourceType,
                scrambleNotation = challenge.scrambleNotation,
                isComputingSolution = true,
                currentStepIndex = 0,
                isAutoPlaying = false,
                timerPhase = TimerPhase.IDLE,
                elapsedTotalMs = 0L,
                crossSplitMs = 0L,
                f2lSplitMs = 0L,
                ollSplitMs = 0L,
                pllSplitMs = 0L,
                savedSession = null
            )
        }
        val sol = withContext(Dispatchers.Default) {
            CfopSolver.solve(challenge.cube)
        }
        _uiState.update {
            it.copy(
                solution = sol,
                isComputingSolution = false
            )
        }
    }

    fun selectTab(tab: SolverTabMode) {
        if (tab != SolverTabMode.GUIDE) {
            autoPlayJob?.cancel()
            _uiState.update { it.copy(isAutoPlaying = false) }
        }
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun stepForward() {
        val total = _uiState.value.solution?.steps?.size ?: return
        _uiState.update {
            it.copy(currentStepIndex = (it.currentStepIndex + 1).coerceAtMost(total))
        }
    }

    fun stepBackward() {
        _uiState.update {
            it.copy(currentStepIndex = (it.currentStepIndex - 1).coerceAtLeast(0))
        }
    }

    fun jumpToStage(stage: CfopStage) {
        val steps = _uiState.value.solution?.steps ?: return
        val firstOfStage = steps.indexOfFirst { it.stage == stage }
        if (firstOfStage != -1) {
            _uiState.update { it.copy(currentStepIndex = firstOfStage) }
        }
    }

    fun jumpToStep(stepIndex: Int) {
        val total = _uiState.value.solution?.steps?.size ?: return
        _uiState.update { it.copy(currentStepIndex = stepIndex.coerceIn(0, total)) }
    }

    fun resetSteps() {
        autoPlayJob?.cancel()
        _uiState.update { it.copy(currentStepIndex = 0, isAutoPlaying = false) }
    }

    fun toggleAutoPlay() {
        val currentlyPlaying = _uiState.value.isAutoPlaying
        if (currentlyPlaying) {
            autoPlayJob?.cancel()
            _uiState.update { it.copy(isAutoPlaying = false) }
            return
        }
        val total = _uiState.value.solution?.steps?.size ?: return
        if (total == 0) return
        if (_uiState.value.currentStepIndex >= total) {
            _uiState.update { it.copy(currentStepIndex = 0) }
        }
        _uiState.update { it.copy(isAutoPlaying = true) }
        autoPlayJob = viewModelScope.launch {
            delay(120L)
            while (_uiState.value.currentStepIndex < total) {
                stepForward()
                if (_uiState.value.currentStepIndex >= total) break
                delay(_uiState.value.playStepDelayMs)
            }
            _uiState.update { it.copy(isAutoPlaying = false) }
        }
    }

    fun generateNewPracticeScramble() {
        appContainer.generateNewPracticeCube()
    }

    fun onTimerPadTapped() {
        val now = System.currentTimeMillis()
        when (_uiState.value.timerPhase) {
            TimerPhase.IDLE, TimerPhase.COMPLETED -> {
                solveStartMs = now
                phaseStartMs = now
                _uiState.update {
                    it.copy(
                        timerPhase = TimerPhase.RUNNING_CROSS,
                        elapsedTotalMs = 0L,
                        crossSplitMs = 0L,
                        f2lSplitMs = 0L,
                        ollSplitMs = 0L,
                        pllSplitMs = 0L,
                        savedSession = null
                    )
                }
                startTicker()
            }
            TimerPhase.RUNNING_CROSS -> {
                val split = (now - phaseStartMs).coerceAtLeast(10L)
                phaseStartMs = now
                _uiState.update {
                    it.copy(timerPhase = TimerPhase.RUNNING_F2L, crossSplitMs = split)
                }
            }
            TimerPhase.RUNNING_F2L -> {
                val split = (now - phaseStartMs).coerceAtLeast(10L)
                phaseStartMs = now
                _uiState.update {
                    it.copy(timerPhase = TimerPhase.RUNNING_OLL, f2lSplitMs = split)
                }
            }
            TimerPhase.RUNNING_OLL -> {
                val split = (now - phaseStartMs).coerceAtLeast(10L)
                phaseStartMs = now
                _uiState.update {
                    it.copy(timerPhase = TimerPhase.RUNNING_PLL, ollSplitMs = split)
                }
            }
            TimerPhase.RUNNING_PLL -> {
                timerJob?.cancel()
                val split = (now - phaseStartMs).coerceAtLeast(10L)
                val state = _uiState.value
                val total = state.crossSplitMs + state.f2lSplitMs + state.ollSplitMs + split
                _uiState.update {
                    it.copy(
                        timerPhase = TimerPhase.COMPLETED,
                        pllSplitMs = split,
                        elapsedTotalMs = total
                    )
                }
                persistCompletedSolve(
                    crossMs = state.crossSplitMs,
                    f2lMs = state.f2lSplitMs,
                    ollMs = state.ollSplitMs,
                    pllMs = split
                )
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _uiState.update {
            it.copy(
                timerPhase = TimerPhase.IDLE,
                elapsedTotalMs = 0L,
                crossSplitMs = 0L,
                f2lSplitMs = 0L,
                ollSplitMs = 0L,
                pllSplitMs = 0L,
                savedSession = null
            )
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val now = System.currentTimeMillis()
                _uiState.update { it.copy(elapsedTotalMs = now - solveStartMs) }
                delay(30L)
            }
        }
    }

    private fun persistCompletedSolve(
        crossMs: Long,
        f2lMs: Long,
        ollMs: Long,
        pllMs: Long
    ) {
        val state = _uiState.value
        val sol = state.solution
        viewModelScope.launch {
            val saved = repository.recordSolveSession(
                sourceType = state.sourceType,
                scrambleNotation = state.scrambleNotation,
                crossMs = crossMs,
                f2lMs = f2lMs,
                ollMs = ollMs,
                pllMs = pllMs,
                crossMoveCount = sol?.crossMoves?.size ?: 0,
                totalMoveCount = sol?.totalMoveCount ?: 0,
                solutionNotation = sol?.fullNotation ?: ""
            )
            _uiState.update { it.copy(savedSession = saved) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoPlayJob?.cancel()
        timerJob?.cancel()
    }

    class Factory(
        private val repository: TrainingRepository,
        private val settingsRepository: SettingsRepository,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SolverTimerViewModel(repository, settingsRepository, appContainer) as T
        }
    }
}
