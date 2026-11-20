package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MilestoneRepository(private val cubeDao: CubeDao) {

    val milestoneOverview: Flow<MilestoneOverview> = combine(
        cubeDao.observeAllSessions(),
        cubeDao.observeRecentDailyGoals(),
        cubeDao.observeAllWcaSolves(),
        cubeDao.observeMilestones()
    ) { sessions, goals, wcaSolves, persisted ->
        computeOverview(
            sessions = sessions,
            goals = goals,
            wcaSolves = wcaSolves,
            persisted = persisted
        )
    }

    suspend fun syncMilestones(nowEpochMs: Long = System.currentTimeMillis()) {
        val sessions = cubeDao.observeAllSessions().first()
        val goals = cubeDao.observeRecentDailyGoals().first()
        val wcaSolves = cubeDao.observeAllWcaSolves().first()
        val persisted = cubeDao.getMilestonesOnce()
        val persistedMap = persisted.associateBy { it.id }

        val overview = computeOverview(
            sessions = sessions,
            goals = goals,
            wcaSolves = wcaSolves,
            persisted = persisted
        )

        val newlyUnlocked = overview.badges
            .filter { it.isUnlocked && persistedMap[it.definition.id]?.unlockedAtEpochMs == null }
            .map { badge ->
                MilestoneProgressEntity(
                    id = badge.definition.id,
                    unlockedAtEpochMs = nowEpochMs,
                    seenByUser = false
                )
            }

        if (newlyUnlocked.isNotEmpty()) {
            cubeDao.upsertMilestones(newlyUnlocked)
        }
    }

    suspend fun markAllSeen() {
        cubeDao.markAllMilestonesSeen()
    }

    companion object {
        fun computeOverview(
            sessions: List<SolveSessionEntity>,
            goals: List<DailyGoalEntity>,
            wcaSolves: List<WcaSolveEntity>,
            persisted: List<MilestoneProgressEntity>,
            nowEpochMs: Long = System.currentTimeMillis()
        ): MilestoneOverview {
            val validWcaSolves = wcaSolves.filter {
                WcaPenalty.fromName(it.penaltyName) != WcaPenalty.DNF
            }
            val totalSolves = sessions.size + validWcaSolves.size
            val streakDays = calculateUnifiedStreakDays(goals, wcaSolves, nowEpochMs)
            val cameraSolves = sessions.count { it.sourceType == "CAMERA_VIDEO" }
            val sub30Solves = sessions.count { it.totalDurationMs in 1L..29_999L } +
                validWcaSolves.count { entity ->
                    val effective = if (WcaPenalty.fromName(entity.penaltyName) == WcaPenalty.PLUS_TWO) {
                        entity.timeMillis + 2000L
                    } else {
                        entity.timeMillis
                    }
                    effective in 1L..29_999L
                }

            val persistedMap = persisted.associateBy { it.id }

            val badges = MilestoneCatalog.all.map { def ->
                val rawProgress = when (def.id) {
                    "first_solve", "solved_10", "solved_50", "solved_100" -> totalSolves
                    "streak_3", "streak_7" -> streakDays
                    "camera_explorer" -> cameraSolves
                    "speed_sub_30" -> sub30Solves
                    else -> 0
                }
                val saved = persistedMap[def.id]
                val wasPersistedUnlocked = saved?.unlockedAtEpochMs != null
                val conditionMetNow = rawProgress >= def.targetValue
                val isUnlocked = wasPersistedUnlocked || conditionMetNow
                val displayProgress = if (isUnlocked) def.targetValue else rawProgress.coerceIn(0, def.targetValue)
                val isNew = isUnlocked && (saved == null || !saved.seenByUser)

                MilestoneBadgeState(
                    definition = def,
                    currentProgress = displayProgress,
                    isUnlocked = isUnlocked,
                    unlockedAtEpochMs = saved?.unlockedAtEpochMs,
                    isNew = isNew
                )
            }

            return MilestoneOverview(
                badges = badges,
                totalSolves = totalSolves,
                currentStreakDays = streakDays
            )
        }

        fun calculateUnifiedStreakDays(
            goals: List<DailyGoalEntity>,
            wcaSolves: List<WcaSolveEntity>,
            nowEpochMs: Long = System.currentTimeMillis()
        ): Int {
            val completedDays = buildSet {
                goals.filter { it.completedSolves > 0 }.forEach { add(it.dateKey) }
                wcaSolves.forEach { add(TrainingRepository.formatDateKey(it.timestamp)) }
            }
            if (completedDays.isEmpty()) return 0

            val cal = Calendar.getInstance().apply { timeInMillis = nowEpochMs }
            val todayKey = TrainingRepository.formatDateKey(cal.timeInMillis)
            if (todayKey !in completedDays) {
                cal.add(Calendar.DAY_OF_YEAR, -1)
            }
            var streak = 0
            while (true) {
                val key = TrainingRepository.formatDateKey(cal.timeInMillis)
                if (key in completedDays) {
                    streak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
            return streak
        }
    }
}
