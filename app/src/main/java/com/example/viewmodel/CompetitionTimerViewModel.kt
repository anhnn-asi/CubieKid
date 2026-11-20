package com.example.viewmodel

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.SettingsRepository
import com.example.data.WcaMetric
import com.example.data.WcaPenalty
import com.example.data.WcaSolveRecord
import com.example.data.WcaTimerRepository
import com.example.scanner.ScanSoundPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class CompetitionTimerState {
    IDLE,
    INSPECTION,
    SOLVING,
    FINISHED
}

data class CompetitionTimerUiState(
    val timerState: CompetitionTimerState = CompetitionTimerState.IDLE,
    val displayText: String = "0.00",
    val inspectionAlertBadge: String? = null,
    val isInspectionOvertime: Boolean = false,
    val scramble: String = CompetitionTimerViewModel.generateWcaScramble(),
    val soundEnabled: Boolean = true,
    val lastSavedRecord: WcaSolveRecord? = null,
    val totalSolves: Int = 0,
    val pb: WcaMetric = WcaMetric.None,
    val ao5: WcaMetric = WcaMetric.None,
    val ao12: WcaMetric = WcaMetric.None
)

class CompetitionTimerViewModel(
    private val wcaTimerRepository: WcaTimerRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompetitionTimerUiState())
    val uiState: StateFlow<CompetitionTimerUiState> = _uiState.asStateFlow()

    private val _hapticPulseEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 4)
    val hapticPulseEvents: SharedFlow<Boolean> = _hapticPulseEvents.asSharedFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var solveStartedAtRealtimeMs: Long = 0L
    private var currentPenalty: WcaPenalty = WcaPenalty.NONE
    private var fired8SecAlert = false
    private var fired12SecAlert = false

    init {
        viewModelScope.launch {
            wcaTimerRepository.allSolves.collect { solves ->
                _uiState.update { state ->
                    state.copy(
                        totalSolves = solves.size,
                        pb = wcaTimerRepository.computeBestPb(solves),
                        ao5 = wcaTimerRepository.computeWcaAo5(solves),
                        ao12 = wcaTimerRepository.computeWcaAo12(solves),
                        lastSavedRecord = solves.firstOrNull()
                    )
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(soundEnabled = settings.soundEnabled) }
            }
        }
    }

    /**
     * Triggered on immediate pointer DOWN when SOLVING for Stackmat-like stop accuracy,
     * with a 180ms debounce after entering SOLVING to prevent double-tap stops.
     */
    fun onTouchDown(): Boolean {
        if (_uiState.value.timerState != CompetitionTimerState.SOLVING) return false
        val sinceStart = SystemClock.elapsedRealtime() - solveStartedAtRealtimeMs
        if (sinceStart < STOP_DEBOUNCE_MS) return false
        stopSolving()
        return true
    }

    /**
     * Triggered on tap release for IDLE / FINISHED / INSPECTION transitions.
     */
    fun onScreenTap() {
        when (_uiState.value.timerState) {
            CompetitionTimerState.IDLE, CompetitionTimerState.FINISHED -> startInspection()
            CompetitionTimerState.INSPECTION -> startSolving()
            CompetitionTimerState.SOLVING -> {
                val sinceStart = SystemClock.elapsedRealtime() - solveStartedAtRealtimeMs
                if (sinceStart >= STOP_DEBOUNCE_MS) stopSolving()
            }
        }
    }

    fun cancelInspectionOrReset() {
        timerJob?.cancel()
        currentPenalty = WcaPenalty.NONE
        _uiState.update {
            it.copy(
                timerState = CompetitionTimerState.IDLE,
                displayText = "0.00",
                inspectionAlertBadge = null,
                isInspectionOvertime = false
            )
        }
    }

    fun regenerateScramble() {
        if (_uiState.value.timerState == CompetitionTimerState.SOLVING) return
        _uiState.update { it.copy(scramble = generateWcaScramble()) }
    }

    fun toggleLastPenalty(penalty: WcaPenalty) {
        val last = _uiState.value.lastSavedRecord ?: return
        val nextPenalty = if (last.penalty == penalty) WcaPenalty.NONE else penalty
        viewModelScope.launch {
            wcaTimerRepository.updatePenalty(last.id, nextPenalty)
            if (_uiState.value.timerState == CompetitionTimerState.FINISHED) {
                val updated = last.copy(penalty = nextPenalty)
                _uiState.update { it.copy(displayText = updated.getFormattedTime()) }
            }
        }
    }

    fun deleteLastSolve() {
        val last = _uiState.value.lastSavedRecord ?: return
        viewModelScope.launch {
            wcaTimerRepository.deleteSolve(last.id)
            if (_uiState.value.timerState == CompetitionTimerState.FINISHED) {
                _uiState.update {
                    it.copy(
                        timerState = CompetitionTimerState.IDLE,
                        displayText = "0.00"
                    )
                }
            }
        }
    }

    private fun startInspection() {
        timerJob?.cancel()
        currentPenalty = WcaPenalty.NONE
        fired8SecAlert = false
        fired12SecAlert = false
        val inspectionStart = SystemClock.elapsedRealtime()

        _uiState.update {
            it.copy(
                timerState = CompetitionTimerState.INSPECTION,
                displayText = "15",
                inspectionAlertBadge = null,
                isInspectionOvertime = false
            )
        }

        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsedSec = (SystemClock.elapsedRealtime() - inspectionStart) / 1000f
                if (elapsedSec >= 8f && !fired8SecAlert) {
                    fired8SecAlert = true
                    triggerInspectionAlert(isTwelveSecondAlert = false)
                    _uiState.update { it.copy(inspectionAlertBadge = "8s!") }
                }
                if (elapsedSec >= 12f && !fired12SecAlert) {
                    fired12SecAlert = true
                    triggerInspectionAlert(isTwelveSecondAlert = true)
                    _uiState.update { it.copy(inspectionAlertBadge = "12s!") }
                }

                when {
                    elapsedSec <= 15f -> {
                        val remaining = kotlin.math.ceil(15f - elapsedSec).toInt().coerceIn(0, 15)
                        _uiState.update {
                            it.copy(
                                displayText = remaining.toString(),
                                isInspectionOvertime = false
                            )
                        }
                    }
                    elapsedSec <= 17f -> {
                        currentPenalty = WcaPenalty.PLUS_TWO
                        _uiState.update {
                            it.copy(
                                displayText = "+2",
                                inspectionAlertBadge = "+2",
                                isInspectionOvertime = true
                            )
                        }
                    }
                    else -> {
                        currentPenalty = WcaPenalty.DNF
                        finishWithDnf()
                        break
                    }
                }
                delay(50L)
            }
        }
    }

    private fun triggerInspectionAlert(isTwelveSecondAlert: Boolean) {
        if (_uiState.value.soundEnabled) {
            ScanSoundPlayer.playInspectionWarningBeep(isTwelveSecondAlert)
        }
        _hapticPulseEvents.tryEmit(isTwelveSecondAlert)
    }

    private fun startSolving() {
        timerJob?.cancel()
        val now = SystemClock.elapsedRealtime()
        startTimeMillis = now
        solveStartedAtRealtimeMs = now
        _uiState.update {
            it.copy(
                timerState = CompetitionTimerState.SOLVING,
                displayText = "0.00",
                inspectionAlertBadge = null,
                isInspectionOvertime = false
            )
        }

        timerJob = viewModelScope.launch {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
                _uiState.update {
                    it.copy(displayText = WcaSolveRecord.formatWcaMillis(elapsed))
                }
                delay(16L)
            }
        }
    }

    private fun stopSolving() {
        timerJob?.cancel()
        val finalElapsed = SystemClock.elapsedRealtime() - startTimeMillis
        val usedScramble = _uiState.value.scramble
        val record = WcaSolveRecord(
            timeMillis = finalElapsed,
            penalty = currentPenalty,
            scramble = usedScramble,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                timerState = CompetitionTimerState.FINISHED,
                displayText = record.getFormattedTime(),
                scramble = generateWcaScramble(),
                inspectionAlertBadge = null,
                isInspectionOvertime = false
            )
        }
        viewModelScope.launch {
            wcaTimerRepository.saveSolve(record)
        }
    }

    private fun finishWithDnf() {
        timerJob?.cancel()
        val usedScramble = _uiState.value.scramble
        val record = WcaSolveRecord(
            timeMillis = 0L,
            penalty = WcaPenalty.DNF,
            scramble = usedScramble,
            timestamp = System.currentTimeMillis()
        )
        _uiState.update {
            it.copy(
                timerState = CompetitionTimerState.FINISHED,
                displayText = "DNF",
                scramble = generateWcaScramble(),
                inspectionAlertBadge = "DNF",
                isInspectionOvertime = true
            )
        }
        viewModelScope.launch {
            wcaTimerRepository.saveSolve(record)
        }
    }

    companion object {
        private const val STOP_DEBOUNCE_MS = 180L

        fun generateWcaScramble(length: Int = 20): String {
            val faces = arrayOf("U", "D", "L", "R", "F", "B")
            val modifiers = arrayOf("", "'", "2")
            val axisMap = mapOf("U" to 0, "D" to 0, "L" to 1, "R" to 1, "F" to 2, "B" to 2)

            val moves = mutableListOf<String>()
            var lastFace = ""
            var secondLastFace = ""

            while (moves.size < length) {
                val face = faces.random()
                if (face == lastFace) continue
                if (secondLastFace.isNotEmpty() &&
                    axisMap[face] == axisMap[lastFace] &&
                    axisMap[face] == axisMap[secondLastFace]
                ) continue

                moves.add(face + modifiers.random())
                secondLastFace = lastFace
                lastFace = face
            }
            return moves.joinToString(" ")
        }
    }

    class Factory(
        private val wcaTimerRepository: WcaTimerRepository,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CompetitionTimerViewModel(
                wcaTimerRepository = wcaTimerRepository,
                settingsRepository = settingsRepository
            ) as T
        }
    }
}
