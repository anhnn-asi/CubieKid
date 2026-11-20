package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TrainingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: TrainingRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TrainingRepository(db.cubeDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CubieKid CFOP", appName)
    }

    @Test
    fun `record solve session updates sqlite daily goal and pb`() = runTest {
        val saved = repository.recordSolveSession(
            sourceType = "CAMERA_VIDEO",
            scrambleNotation = "R U R' U'",
            crossMs = 2500L,
            f2lMs = 8000L,
            ollMs = 3000L,
            pllMs = 3500L,
            crossMoveCount = 5,
            totalMoveCount = 42,
            solutionNotation = "R U R'"
        )
        assertTrue(saved.isPersonalBest)
        assertEquals(17000L, saved.totalDurationMs)

        val sessions = repository.allSessions.first()
        assertEquals(1, sessions.size)

        val todayGoal = repository.observeTodayGoal().first()
        assertEquals(1, todayGoal?.completedSolves)
        assertEquals(17000L, todayGoal?.bestTimeMs)
    }

    @Test
    fun `settings repository defaults to 1000ms play speed and persists sound toggles`() = runTest {
        val settingsRepo = com.example.data.SettingsRepository(db.cubeDao())

        val initial = settingsRepo.settings.first()
        assertEquals(1000L, initial.playStepDelayMs)
        assertEquals(com.example.data.AppLanguage.VI, initial.language)
        assertTrue(initial.soundEnabled)
        assertTrue(initial.moveSoundEnabled)

        settingsRepo.setPlayStepDelayMs(2000L)
        settingsRepo.setLanguage(com.example.data.AppLanguage.EN)
        settingsRepo.setSoundEnabled(false)
        settingsRepo.setMoveSoundEnabled(false)

        val updated = settingsRepo.settings.first()
        assertEquals(2000L, updated.playStepDelayMs)
        assertEquals(com.example.data.AppLanguage.EN, updated.language)
        assertEquals(false, updated.soundEnabled)
        assertEquals(false, updated.moveSoundEnabled)

        // Verify audio trigger methods run safely
        com.example.scanner.ScanSoundPlayer.playRubikMoveClick(isDoubleTurn = false)
        com.example.scanner.ScanSoundPlayer.playRubikMoveClick(isDoubleTurn = true)
        com.example.scanner.ScanSoundPlayer.playSingleFaceBeep()
        com.example.scanner.ScanSoundPlayer.playAllSixFacesBeep()
    }

    @Test
    fun `user profile sqlite repository validates and saves profile with training mode mapping`() = runTest {
        val profileRepo = com.example.data.UserProfileRepository(db.cubeDao())
        assertEquals(null, profileRepo.userProfile.first())

        val invalidName = profileRepo.saveProfile(
            com.example.data.UserProfile(
                name = "A",
                age = 15,
                currentLevel = com.example.data.RubikLevel.BEGINNER,
                targetLevel = com.example.data.RubikLevel.SUB_60
            )
        )
        assertTrue(invalidName.isFailure)

        val invalidTarget = profileRepo.saveProfile(
            com.example.data.UserProfile(
                name = "Minh Khoi",
                age = 12,
                currentLevel = com.example.data.RubikLevel.SUB_30,
                targetLevel = com.example.data.RubikLevel.SUB_60
            )
        )
        assertTrue(invalidTarget.isFailure)

        val savedRes = profileRepo.saveProfile(
            com.example.data.UserProfile(
                name = "  Minh Khoi  ",
                age = 12,
                currentLevel = com.example.data.RubikLevel.SUB_60,
                targetLevel = com.example.data.RubikLevel.SUB_30
            )
        )
        assertTrue(savedRes.isSuccess)

        val stored = profileRepo.userProfile.first()
        assertEquals("Minh Khoi", stored?.name)
        assertEquals(12, stored?.age)
        assertEquals(com.example.data.RubikLevel.SUB_60, stored?.currentLevel)
        assertEquals(com.example.data.RubikLevel.SUB_30, stored?.targetLevel)
        assertEquals(com.example.data.CubieTrainingMode.PRACTICE_CFOP, stored?.trainingMode())
    }

    @Test
    fun `wca timer repository persists solves and computes PB Ao5 and DNF rules accurately`() = runTest {
        val wcaRepo = com.example.data.WcaTimerRepository(db.cubeDao())

        // 5 solves newest first: 12000, DNF, 14000 (+2 -> 16000), 11000, 15000
        // Sorted numeric: [11000, 12000, 15000, 16000], DNF is worst -> drop 11000 (best) and DNF (worst)
        // Average of remaining [12000, 15000, 16000] = 43000 / 3 = 14333ms
        val times = listOf(15000L to com.example.data.WcaPenalty.NONE,
            11000L to com.example.data.WcaPenalty.NONE,
            14000L to com.example.data.WcaPenalty.PLUS_TWO,
            0L to com.example.data.WcaPenalty.DNF,
            12000L to com.example.data.WcaPenalty.NONE
        )
        times.forEachIndexed { idx, (ms, pen) ->
            wcaRepo.saveSolve(
                com.example.data.WcaSolveRecord(
                    timeMillis = ms,
                    penalty = pen,
                    scramble = "R U R' U'",
                    timestamp = 1000L + idx
                )
            )
        }

        val solves = wcaRepo.allSolves.first()
        assertEquals(5, solves.size)
        assertEquals("11.00", wcaRepo.computeBestPb(solves).format())
        assertEquals("14.33", wcaRepo.computeWcaAo5(solves).format())
        assertEquals("--", wcaRepo.computeWcaAo12(solves).format())

        // Adding a 2nd DNF makes Ao5 = DNF
        wcaRepo.saveSolve(
            com.example.data.WcaSolveRecord(
                timeMillis = 0L,
                penalty = com.example.data.WcaPenalty.DNF,
                scramble = "F R U R' U' F'",
                timestamp = 2000L
            )
        )
        val solvesWithTwoDnfs = wcaRepo.allSolves.first()
        assertEquals("DNF", wcaRepo.computeWcaAo5(solvesWithTwoDnfs).format())
    }

    @Test
    fun `every cfop algorithm in bundled json sets up and solves accurately via ExtendedMoveEngine`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val algRepo = com.example.tutorial.AlgorithmRepository(context)
        val categories = algRepo.categories
        assertEquals(3, categories.size)

        var totalTested = 0
        for (cat in categories) {
            for (alg in cat.algorithms) {
                val setupCube = com.example.rubik.ExtendedMoveEngine.applyAlgorithm(
                    com.example.rubik.CubeState.solved(),
                    alg.setupMoves
                )
                val solvedCube = com.example.rubik.ExtendedMoveEngine.applyAlgorithm(
                    setupCube,
                    alg.moves
                )
                assertTrue(
                    "Algorithm ${alg.id} (${alg.name}) failed solve check",
                    com.example.viewmodel.TutorialViewModel.isCubeSolvedForCategory(solvedCube, cat.id)
                )
                val enName = alg.localizedName(com.example.data.AppLanguage.EN)
                assertTrue(
                    "English algorithm name contains non-ASCII Vietnamese text: $enName",
                    enName.all { it.code < 128 }
                )
                totalTested++
            }
            val enCatName = cat.localizedName(com.example.data.AppLanguage.EN)
            assertTrue(
                "English category name contains non-ASCII Vietnamese text: $enCatName",
                enCatName.all { it.code < 128 }
            )
        }
        assertTrue(totalTested >= 19)
    }

    @Test
    fun `wca scramble generator produces 20 non-redundant moves`() {
        val scramble = com.example.viewmodel.CompetitionTimerViewModel.generateWcaScramble(20)
        val tokens = scramble.split(" ").filter { it.isNotEmpty() }
        assertEquals(20, tokens.size)
        for (i in 1 until tokens.size) {
            assertTrue("Adjacent same face in $scramble", tokens[i][0] != tokens[i - 1][0])
        }
    }

    @Test
    fun `milestone repository tracks 100 solves, 7 day streak, and persists unlocked badges`() = runTest {
        val dao = db.cubeDao()
        val milestoneRepo = com.example.data.MilestoneRepository(dao)

        // Create 7 consecutive days of daily goals ending today + 100 WCA/CFOP solves
        val cal = java.util.Calendar.getInstance()
        val nowMs = cal.timeInMillis
        for (dayOffset in 0 until 7) {
            val dayCal = java.util.Calendar.getInstance().apply {
                timeInMillis = nowMs
                add(java.util.Calendar.DAY_OF_YEAR, -dayOffset)
            }
            val dateKey = TrainingRepository.formatDateKey(dayCal.timeInMillis)
            dao.upsertDailyGoal(
                com.example.data.DailyGoalEntity(
                    dateKey = dateKey,
                    targetSolves = 5,
                    completedSolves = 1,
                    bestTimeMs = 25000L,
                    totalTimeMs = 25000L
                )
            )
        }
        repeat(99) { idx ->
            dao.insertWcaSolve(
                com.example.data.WcaSolveEntity(
                    timeMillis = 28500L,
                    penaltyName = com.example.data.WcaPenalty.NONE.name,
                    scramble = "R U R' U'",
                    timestamp = nowMs - idx * 1000L
                )
            )
        }
        dao.insertSession(
            com.example.data.SolveSessionEntity(
                createdAtEpochMs = nowMs,
                dateKey = TrainingRepository.formatDateKey(nowMs),
                sourceType = "CAMERA_VIDEO",
                scrambleNotation = "30s Video Scan",
                totalDurationMs = 27000L,
                crossMs = 4000L,
                f2lMs = 12000L,
                ollMs = 5000L,
                pllMs = 6000L,
                crossMoveCount = 6,
                totalMoveCount = 45,
                solutionNotation = "R U R'",
                isPersonalBest = true
            )
        )

        milestoneRepo.syncMilestones(nowEpochMs = nowMs)
        val overview = milestoneRepo.milestoneOverview.first()
        assertEquals(100, overview.totalSolves)
        assertEquals(7, overview.currentStreakDays)
        assertEquals(8, overview.unlockedCount)
        assertEquals(8, overview.unseenCount)

        val solved100 = overview.badges.first { it.definition.id == "solved_100" }
        val streak7 = overview.badges.first { it.definition.id == "streak_7" }
        assertTrue(solved100.isUnlocked)
        assertTrue(streak7.isUnlocked)
        assertEquals("Solved 100 Times", solved100.definition.enTitle)
        assertEquals("Practice Streak 7 Days", streak7.definition.enTitle)

        // Mark all seen and verify unseenCount drops to 0 in SQLite
        milestoneRepo.markAllSeen()
        val afterSeen = milestoneRepo.milestoneOverview.first()
        assertEquals(0, afterSeen.unseenCount)
        assertEquals(8, afterSeen.unlockedCount)
    }
}
