package com.example.data

import com.example.rubik.CfopStage
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PhaseSpeedBreakdown(
    val avgCrossMs: Long,
    val avgF2lMs: Long,
    val avgOllMs: Long,
    val avgPllMs: Long,
    val slowestStage: CfopStage,
    val tipVi: String,
    val tipEn: String
)

class TrainingRepository(private val cubeDao: CubeDao) {

    val allSessions: Flow<List<SolveSessionEntity>> = cubeDao.observeAllSessions()
    val recentDailyGoals: Flow<List<DailyGoalEntity>> = cubeDao.observeRecentDailyGoals()

    fun todayDateKey(): String = formatDateKey(System.currentTimeMillis())

    fun observeTodayGoal(): Flow<DailyGoalEntity?> = cubeDao.observeDailyGoal(todayDateKey())

    suspend fun recordSolveSession(
        sourceType: String,
        scrambleNotation: String,
        crossMs: Long,
        f2lMs: Long,
        ollMs: Long,
        pllMs: Long,
        crossMoveCount: Int,
        totalMoveCount: Int,
        solutionNotation: String
    ): SolveSessionEntity {
        val now = System.currentTimeMillis()
        val dateKey = formatDateKey(now)
        val totalMs = crossMs + f2lMs + ollMs + pllMs
        val currentBest = cubeDao.getBestTimeMs()
        val isPb = currentBest == null || totalMs < currentBest

        val entity = SolveSessionEntity(
            createdAtEpochMs = now,
            dateKey = dateKey,
            sourceType = sourceType,
            scrambleNotation = scrambleNotation,
            totalDurationMs = totalMs,
            crossMs = crossMs,
            f2lMs = f2lMs,
            ollMs = ollMs,
            pllMs = pllMs,
            crossMoveCount = crossMoveCount,
            totalMoveCount = totalMoveCount,
            solutionNotation = solutionNotation,
            isPersonalBest = isPb
        )
        val id = cubeDao.insertSession(entity)

        val existingGoal = cubeDao.getDailyGoalOnce(dateKey)
        val updatedGoal = if (existingGoal == null) {
            DailyGoalEntity(
                dateKey = dateKey,
                targetSolves = 5,
                completedSolves = 1,
                bestTimeMs = totalMs,
                totalTimeMs = totalMs
            )
        } else {
            existingGoal.copy(
                completedSolves = existingGoal.completedSolves + 1,
                bestTimeMs = minOf(existingGoal.bestTimeMs ?: totalMs, totalMs),
                totalTimeMs = existingGoal.totalTimeMs + totalMs
            )
        }
        cubeDao.upsertDailyGoal(updatedGoal)
        return entity.copy(id = id)
    }

    suspend fun updateTodayTarget(targetSolves: Int) {
        val dateKey = todayDateKey()
        val clamped = targetSolves.coerceIn(1, 30)
        val existing = cubeDao.getDailyGoalOnce(dateKey)
        val updated = existing?.copy(targetSolves = clamped) ?: DailyGoalEntity(
            dateKey = dateKey,
            targetSolves = clamped
        )
        cubeDao.upsertDailyGoal(updated)
    }

    suspend fun deleteSession(id: Long) {
        cubeDao.deleteSessionById(id)
    }

    fun calculateStreakDays(goals: List<DailyGoalEntity>): Int {
        if (goals.isEmpty()) return 0
        val completedDays = goals.filter { it.completedSolves > 0 }.map { it.dateKey }.toSet()
        if (completedDays.isEmpty()) return 0

        val cal = Calendar.getInstance()
        var streak = 0
        // Check today; if today has 0 solves yet, allow streak starting from yesterday
        val todayKey = formatDateKey(cal.timeInMillis)
        if (todayKey !in completedDays) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        while (true) {
            val key = formatDateKey(cal.timeInMillis)
            if (key in completedDays) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun computeAo5(sessions: List<SolveSessionEntity>): Long? {
        if (sessions.size < 5) return null
        val last5 = sessions.take(5).map { it.totalDurationMs }.sorted()
        return (last5[1] + last5[2] + last5[3]) / 3L
    }

    fun computePhaseBreakdown(sessions: List<SolveSessionEntity>): PhaseSpeedBreakdown? {
        if (sessions.isEmpty()) return null
        val avgCross = sessions.map { it.crossMs }.average().toLong()
        val avgF2l = sessions.map { it.f2lMs }.average().toLong()
        val avgOll = sessions.map { it.ollMs }.average().toLong()
        val avgPll = sessions.map { it.pllMs }.average().toLong()

        // Ideal CFOP ratio is roughly Cross ~12%, F2L ~50%, OLL ~16%, PLL ~22%
        val total = (avgCross + avgF2l + avgOll + avgPll).coerceAtLeast(1L).toDouble()
        val crossRatioDiff = (avgCross / total) - 0.14
        val f2lRatioDiff = (avgF2l / total) - 0.50
        val ollRatioDiff = (avgOll / total) - 0.16
        val pllRatioDiff = (avgPll / total) - 0.20

        val slowest = listOf(
            CfopStage.CROSS to crossRatioDiff,
            CfopStage.F2L to f2lRatioDiff,
            CfopStage.OLL to ollRatioDiff,
            CfopStage.PLL to pllRatioDiff
        ).maxBy { it.second }.first

        val (tipVi, tipEn) = when (slowest) {
            CfopStage.CROSS ->
                "Bé thử nhìn trước 4 viên trắng trong 5 giây trước khi bấm giờ để ghép Cross mượt hơn nhé!" to
                    "Try planning all 4 white Cross pieces before starting the timer!"
            CfopStage.F2L ->
                "Pha F2L đang chiếm nhiều thời gian nhất. Bé xoay chậm lại một chút để mắt kịp tìm cặp góc-cạnh tiếp theo (Lookahead)!" to
                    "F2L takes the biggest slice. Slow down slightly to spot the next pair smoothly!"
            CfopStage.OLL ->
                "Bé ôn thêm các mẫu OLL hình Con Cá (Sune) và Chữ T để bật mặt Vàng thật nhanh nhé!" to
                    "Practice Sune and T-shape OLL triggers to flip the yellow top faster!"
            CfopStage.PLL ->
                "Tập lại ngón trỏ cho T-Perm và U-Perm sẽ giúp bé kết thúc PLL siêu tốc!" to
                    "Drill fingertricks on T-Perm and U-Perm to finish PLL super fast!"
        }

        return PhaseSpeedBreakdown(
            avgCrossMs = avgCross,
            avgF2lMs = avgF2l,
            avgOllMs = avgOll,
            avgPllMs = avgPll,
            slowestStage = slowest,
            tipVi = tipVi,
            tipEn = tipEn
        )
    }

    companion object {
        fun formatDateKey(epochMs: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date(epochMs))
        }

        fun formatDurationMs(ms: Long): String {
            val safe = ms.coerceAtLeast(0L)
            val totalSeconds = safe / 1000L
            val minutes = totalSeconds / 60L
            val seconds = totalSeconds % 60L
            val centis = (safe % 1000L) / 10L
            return if (minutes > 0) {
                String.format(Locale.US, "%d:%02d.%02d", minutes, seconds, centis)
            } else {
                String.format(Locale.US, "%02d.%02d s", seconds, centis)
            }
        }
    }
}
