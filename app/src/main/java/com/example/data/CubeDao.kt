package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CubeDao {

    @Query("SELECT * FROM solve_sessions ORDER BY createdAtEpochMs DESC")
    fun observeAllSessions(): Flow<List<SolveSessionEntity>>

    @Query("SELECT * FROM solve_sessions WHERE dateKey = :dateKey ORDER BY createdAtEpochMs DESC")
    fun observeSessionsForDate(dateKey: String): Flow<List<SolveSessionEntity>>

    @Query("SELECT * FROM daily_goals ORDER BY dateKey DESC LIMIT 14")
    fun observeRecentDailyGoals(): Flow<List<DailyGoalEntity>>

    @Query("SELECT * FROM daily_goals WHERE dateKey = :dateKey LIMIT 1")
    fun observeDailyGoal(dateKey: String): Flow<DailyGoalEntity?>

    @Query("SELECT * FROM daily_goals WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getDailyGoalOnce(dateKey: String): DailyGoalEntity?

    @Query("SELECT MIN(totalDurationMs) FROM solve_sessions")
    suspend fun getBestTimeMs(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SolveSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyGoal(goal: DailyGoalEntity)

    @Query("DELETE FROM solve_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: AppSettingsEntity)
}
