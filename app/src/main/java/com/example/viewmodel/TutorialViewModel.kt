package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.data.UserProfile
import com.example.data.UserProfileRepository
import com.example.rubik.CubeColor
import com.example.rubik.CubeMove
import com.example.rubik.CubeState
import com.example.rubik.ExtendedMoveEngine
import com.example.rubik.FaceId
import com.example.scanner.ScanSoundPlayer
import com.example.tutorial.AlgorithmCategory
import com.example.tutorial.AlgorithmItem
import com.example.tutorial.AlgorithmRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TutorialTab {
    TRAINER,
    HANDBOOK
}

enum class TrainerMode {
    LEARN,
    DRILL
}

data class TutorialUiState(
    val activeTab: TutorialTab = TutorialTab.TRAINER,
    val categories: List<AlgorithmCategory> = emptyList(),
    val selectedCategoryId: String = "2_look_oll",
    val filterByTargetLevel: Boolean = false,
    val userProfile: UserProfile? = null,
    val currentAlgorithm: AlgorithmItem? = null,
    val mode: TrainerMode = TrainerMode.LEARN,
    val isHintVisible: Boolean = true,
    val isRandomAufEnabled: Boolean = false,
    val cube: CubeState = CubeState.solved(),
    val expectedMoves: List<String> = emptyList(),
    val nextMoveIndex: Int = 0,
    val userMoveHistory: List<String> = emptyList(),
    val elapsedMillis: Long = 0L,
    val bestMillis: Long? = null,
    val completedCount: Int = 0,
    val isSolved: Boolean = false,
    val manualMoveTrigger: Pair<Long, CubeMove?>? = null
)

class TutorialViewModel(
    private val algorithmRepository: AlgorithmRepository,
    private val userProfileRepository: UserProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TutorialUiState())
    val uiState: StateFlow<TutorialUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0L
    private var isTimerRunning: Boolean = false
    private var timerJob: Job? = null
    private var autoResetJob: Job? = null
    private var moveSeqCounter: Long = 0L
    private var soundEnabled: Boolean = true

    init {
        val loadedCategories = algorithmRepository.categories
        _uiState.update {
            it.copy(
                categories = loadedCategories,
                selectedCategoryId = loadedCategories.firstOrNull()?.id ?: "2_look_oll"
            )
        }
        val firstAlg = loadedCategories.firstOrNull()?.algorithms?.firstOrNull()
        if (firstAlg != null) {
            loadAlgorithm(firstAlg, TrainerMode.LEARN)
        }

        viewModelScope.launch {
            userProfileRepository.userProfile.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                soundEnabled = settings.soundEnabled
            }
        }
    }

    fun selectTab(tab: TutorialTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectCategory(categoryId: String) {
        val cat = _uiState.value.categories.firstOrNull { it.id == categoryId } ?: return
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        val filtered = visibleAlgorithmsFor(cat, _uiState.value.filterByTargetLevel, _uiState.value.userProfile)
        val firstAlg = filtered.firstOrNull() ?: cat.algorithms.firstOrNull()
        if (firstAlg != null) {
            loadAlgorithm(firstAlg, _uiState.value.mode)
        }
    }

    fun toggleFilterByTargetLevel() {
        val nextFilter = !_uiState.value.filterByTargetLevel
        _uiState.update { it.copy(filterByTargetLevel = nextFilter) }
    }

    fun setMode(mode: TrainerMode) {
        val alg = _uiState.value.currentAlgorithm ?: return
        loadAlgorithm(alg, mode)
    }

    fun loadAlgorithm(algorithm: AlgorithmItem, mode: TrainerMode = _uiState.value.mode) {
        timerJob?.cancel()
        autoResetJob?.cancel()
        isTimerRunning = false

        var cube = CubeState.solved()
        cube = ExtendedMoveEngine.applyAlgorithm(cube, algorithm.setupMoves)
        if (_uiState.value.isRandomAufEnabled) {
            val randomAuf = listOf("", "U", "U'", "U2").random()
            if (randomAuf.isNotEmpty()) {
                cube = ExtendedMoveEngine.applyToken(cube, randomAuf)
            }
        }

        val tokens = ExtendedMoveEngine.tokenize(algorithm.moves)
        _uiState.update {
            it.copy(
                activeTab = TutorialTab.TRAINER,
                selectedCategoryId = algorithm.categoryId,
                currentAlgorithm = algorithm,
                mode = mode,
                isHintVisible = (mode == TrainerMode.LEARN),
                cube = cube,
                expectedMoves = tokens,
                nextMoveIndex = 0,
                userMoveHistory = emptyList(),
                elapsedMillis = 0L,
                isSolved = false
            )
        }
    }

    fun openAlgorithmFromHandbook(algorithmId: String) {
        val found = _uiState.value.categories
            .asSequence()
            .flatMap { it.algorithms.asSequence() }
            .firstOrNull { it.id == algorithmId } ?: return
        loadAlgorithm(found, _uiState.value.mode)
    }

    fun reloadCurrentAlgorithm() {
        val alg = _uiState.value.currentAlgorithm ?: return
        loadAlgorithm(alg, _uiState.value.mode)
    }

    fun performMove(token: String) {
        val current = _uiState.value
        val alg = current.currentAlgorithm ?: return
        if (current.isSolved) return

        if (!isTimerRunning) {
            startTimeMillis = SystemClock.elapsedRealtime()
            isTimerRunning = true
            startTickerLoop()
        }

        val nextCube = ExtendedMoveEngine.applyToken(current.cube, token)
        moveSeqCounter += 1L
        val visualOuter = CubeMove.parse(token)

        val expectedToken = current.expectedMoves.getOrNull(current.nextMoveIndex)
        val nextIndex = if (expectedToken != null && expectedToken == token) {
            current.nextMoveIndex + 1
        } else {
            current.nextMoveIndex
        }

        val solved = isCubeSolvedForCategory(nextCube, alg.categoryId)
        if (solved) {
            isTimerRunning = false
            timerJob?.cancel()
            val finalTime = SystemClock.elapsedRealtime() - startTimeMillis
            val newBest = current.bestMillis?.let { minOf(it, finalTime) } ?: finalTime

            if (soundEnabled) {
                ScanSoundPlayer.playTrainerSolvedChime()
            }

            _uiState.update {
                it.copy(
                    cube = nextCube,
                    nextMoveIndex = current.expectedMoves.size,
                    userMoveHistory = it.userMoveHistory + token,
                    elapsedMillis = finalTime,
                    bestMillis = newBest,
                    completedCount = it.completedCount + 1,
                    isSolved = true,
                    manualMoveTrigger = moveSeqCounter to visualOuter
                )
            }

            if (current.mode == TrainerMode.DRILL) {
                autoResetJob?.cancel()
                autoResetJob = viewModelScope.launch {
                    delay(1000L)
                    reloadCurrentAlgorithm()
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    cube = nextCube,
                    nextMoveIndex = nextIndex,
                    userMoveHistory = it.userMoveHistory + token,
                    manualMoveTrigger = moveSeqCounter to visualOuter
                )
            }
        }
    }

    fun stepExpectedMove() {
        val current = _uiState.value
        val nextToken = current.expectedMoves.getOrNull(current.nextMoveIndex) ?: return
        performMove(nextToken)
    }

    fun toggleHint() {
        _uiState.update { it.copy(isHintVisible = !it.isHintVisible) }
    }

    fun toggleRandomAuf() {
        _uiState.update { it.copy(isRandomAufEnabled = !it.isRandomAufEnabled) }
        reloadCurrentAlgorithm()
    }

    fun visibleAlgorithmsFor(
        category: AlgorithmCategory,
        filterByTarget: Boolean,
        profile: UserProfile?
    ): List<AlgorithmItem> {
        if (!filterByTarget || profile == null) return category.algorithms
        val matching = category.algorithms.filter { profile.targetLevel in it.recommendedLevels }
        return matching.ifEmpty { category.algorithms }
    }

    private fun startTickerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && isTimerRunning) {
                _uiState.update { it.copy(elapsedMillis = SystemClock.elapsedRealtime() - startTimeMillis) }
                delay(32L)
            }
        }
    }

    companion object {
        fun isCubeSolvedForCategory(cube: CubeState, categoryId: String): Boolean {
            return if (categoryId == "2_look_oll" || categoryId == "full_oll") {
                cube.faceColors(FaceId.U).all { it == CubeColor.YELLOW } &&
                    cube.faceColors(FaceId.D).all { it == CubeColor.WHITE }
            } else {
                cube.isSolved()
            }
        }
    }

    class Factory(
        private val algorithmRepository: AlgorithmRepository,
        private val userProfileRepository: UserProfileRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return TutorialViewModel(
                algorithmRepository = algorithmRepository,
                userProfileRepository = userProfileRepository,
                settingsRepository = settingsRepository
            ) as T
        }
    }
}
