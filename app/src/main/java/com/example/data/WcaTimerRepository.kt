package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface WcaMetric {
    data object None : WcaMetric
    data object Dnf : WcaMetric
    data class Time(val millis: Long) : WcaMetric

    fun format(): String = when (this) {
        None -> "--"
        Dnf -> "DNF"
        is Time -> WcaSolveRecord.formatWcaMillis(millis)
    }
}

class WcaTimerRepository(private val cubeDao: CubeDao) {

    val allSolves: Flow<List<WcaSolveRecord>> = cubeDao.observeAllWcaSolves().map { rows ->
        rows.map { entity ->
            WcaSolveRecord(
                id = entity.id,
                timeMillis = entity.timeMillis,
                penalty = WcaPenalty.fromName(entity.penaltyName),
                scramble = entity.scramble,
                timestamp = entity.timestamp
            )
        }
    }

    suspend fun saveSolve(record: WcaSolveRecord): Long {
        return cubeDao.insertWcaSolve(
            WcaSolveEntity(
                timeMillis = record.timeMillis,
                penaltyName = record.penalty.name,
                scramble = record.scramble,
                timestamp = record.timestamp
            )
        )
    }

    suspend fun updatePenalty(id: Long, penalty: WcaPenalty) {
        cubeDao.updateWcaSolvePenalty(id = id, penaltyName = penalty.name)
    }

    suspend fun deleteSolve(id: Long) {
        cubeDao.deleteWcaSolveById(id)
    }

    fun computeBestPb(solves: List<WcaSolveRecord>): WcaMetric {
        val valid = solves.mapNotNull { it.effectiveTimeMs() }
        if (valid.isEmpty()) return if (solves.any { it.penalty == WcaPenalty.DNF }) WcaMetric.Dnf else WcaMetric.None
        return WcaMetric.Time(valid.min())
    }

    fun computeWcaAo5(solves: List<WcaSolveRecord>): WcaMetric = computeTrimmedAverage(solves, 5)

    fun computeWcaAo12(solves: List<WcaSolveRecord>): WcaMetric = computeTrimmedAverage(solves, 12)

    private fun computeTrimmedAverage(solves: List<WcaSolveRecord>, size: Int): WcaMetric {
        if (solves.size < size) return WcaMetric.None
        val window = solves.take(size)
        val dnfCount = window.count { it.penalty == WcaPenalty.DNF }
        if (dnfCount >= 2) return WcaMetric.Dnf

        val numericSorted = window.mapNotNull { it.effectiveTimeMs() }.sorted()
        // If 1 DNF: DNF is dropped as the worst; drop the fastest numeric time.
        val middle = if (dnfCount == 1) {
            numericSorted.drop(1)
        } else {
            numericSorted.drop(1).dropLast(1)
        }
        if (middle.isEmpty()) return WcaMetric.Dnf
        return WcaMetric.Time(middle.sum() / middle.size)
    }
}
