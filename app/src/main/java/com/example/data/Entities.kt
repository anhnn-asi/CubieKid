package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solve_sessions")
data class SolveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long,
    val dateKey: String,
    val sourceType: String, // "CAMERA_VIDEO" or "PRACTICE_SCRAMBLE"
    val scrambleNotation: String,
    val totalDurationMs: Long,
    val crossMs: Long,
    val f2lMs: Long,
    val ollMs: Long,
    val pllMs: Long,
    val crossMoveCount: Int,
    val totalMoveCount: Int,
    val solutionNotation: String,
    val isPersonalBest: Boolean = false
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val dateKey: String,
    val targetSolves: Int = 5,
    val completedSolves: Int = 0,
    val bestTimeMs: Long? = null,
    val totalTimeMs: Long = 0L
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val playStepDelayMs: Long = 1000L,
    val languageCode: String = "vi"
)
