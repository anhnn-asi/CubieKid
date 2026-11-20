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
    fun `settings repository defaults to 1000ms play speed and switches language`() = runTest {
        val settingsRepo = com.example.data.SettingsRepository(db.cubeDao())

        val initial = settingsRepo.settings.first()
        assertEquals(1000L, initial.playStepDelayMs)
        assertEquals(com.example.data.AppLanguage.VI, initial.language)

        settingsRepo.setPlayStepDelayMs(2000L)
        settingsRepo.setLanguage(com.example.data.AppLanguage.EN)

        val updated = settingsRepo.settings.first()
        assertEquals(2000L, updated.playStepDelayMs)
        assertEquals(com.example.data.AppLanguage.EN, updated.language)
    }
}
