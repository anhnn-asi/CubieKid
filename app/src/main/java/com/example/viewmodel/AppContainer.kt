package com.example.viewmodel

import android.content.Context
import com.example.data.AppDatabase
import com.example.data.MilestoneRepository
import com.example.data.SettingsRepository
import com.example.data.TrainingRepository
import com.example.data.UserProfileRepository
import com.example.data.WcaTimerRepository
import com.example.rubik.CubeState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ActiveCubeChallenge(
    val cube: CubeState,
    val sourceType: String, // "CAMERA_VIDEO" or "PRACTICE_SCRAMBLE"
    val scrambleNotation: String
)

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    private val database = AppDatabase.getInstance(context)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val trainingRepository = TrainingRepository(database.cubeDao())
    val settingsRepository = SettingsRepository(database.cubeDao())
    val userProfileRepository = UserProfileRepository(database.cubeDao())
    val wcaTimerRepository = WcaTimerRepository(database.cubeDao())
    val milestoneRepository = MilestoneRepository(database.cubeDao())

    init {
        appScope.launch {
            milestoneRepository.milestoneOverview.collect { overview ->
                if (overview.badges.any { it.isUnlocked && it.unlockedAtEpochMs == null }) {
                    milestoneRepository.syncMilestones()
                }
            }
        }
    }

    private val _activeChallenge = MutableStateFlow(createPracticeChallenge())
    val activeChallenge: StateFlow<ActiveCubeChallenge> = _activeChallenge.asStateFlow()

    fun setScannedCube(cube: CubeState) {
        _activeChallenge.value = ActiveCubeChallenge(
            cube = cube,
            sourceType = "CAMERA_VIDEO",
            scrambleNotation = "30s Video Scan"
        )
    }

    fun generateNewPracticeCube(): ActiveCubeChallenge {
        val challenge = createPracticeChallenge()
        _activeChallenge.value = challenge
        return challenge
    }

    companion object {
        fun createPracticeChallenge(): ActiveCubeChallenge {
            val scramble = CubeState.generateScramble(16)
            val cube = CubeState.solved().applyMoves(scramble)
            return ActiveCubeChallenge(
                cube = cube,
                sourceType = "PRACTICE_SCRAMBLE",
                scrambleNotation = scramble.joinToString(" ") { it.notation }
            )
        }
    }
}
