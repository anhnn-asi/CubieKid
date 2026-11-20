package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.rubik.CubeColor
import com.example.rubik.CubeState
import com.example.rubik.CubeValidationResult
import com.example.rubik.CubeValidator
import com.example.scanner.FaceSampleResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ScanSoundEvent {
    SINGLE_FACE_CAPTURED,
    ALL_SIX_FACES_CAPTURED
}

sealed interface ScannerPhase {
    data object Ready : ScannerPhase
    data object Recording : ScannerPhase
    data object Validating : ScannerPhase
    data class ValidationFailed(val viReason: String, val enReason: String) : ScannerPhase
    data class Success(val cube: CubeState) : ScannerPhase
}

data class ScannerUiState(
    val phase: ScannerPhase = ScannerPhase.Ready,
    val remainingSeconds: Int = MAX_SCAN_SECONDS,
    val capturedFaces: Map<CubeColor, List<CubeColor>> = emptyMap(),
    val liveDetectedCenter: CubeColor? = null,
    val livePreviewGrid: List<CubeColor>? = null,
    val hintVi: String = "Bấm nút Quay Video 30s rồi xoay đều 6 mặt Rubik trước khung vuông!",
    val hintEn: String = "Tap Record 30s and rotate all 6 faces inside the frame!"
) {
    val progressCount: Int get() = capturedFaces.size

    companion object {
        const val MAX_SCAN_SECONDS = 30
    }
}

class ScannerViewModel(
    private val appContainer: AppContainer
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    private val _soundEvents = MutableSharedFlow<ScanSoundEvent>(extraBufferCapacity = 8)
    val soundEvents: SharedFlow<ScanSoundEvent> = _soundEvents.asSharedFlow()

    private var countdownJob: Job? = null
    private var lastFrameMs: Long = 0L
    private var streakCenter: CubeColor? = null
    private var streakGrid: List<CubeColor>? = null
    private var streakCount: Int = 0

    // Callback wired by UI to stop CameraX VideoCapture recording cleanly
    private var onRequestStopRecording: (() -> Unit)? = null

    fun attachStopRecordingCallback(callback: () -> Unit) {
        onRequestStopRecording = callback
    }

    fun startRecordingSession() {
        countdownJob?.cancel()
        streakCenter = null
        streakGrid = null
        streakCount = 0

        _uiState.value = ScannerUiState(
            phase = ScannerPhase.Recording,
            remainingSeconds = ScannerUiState.MAX_SCAN_SECONDS,
            capturedFaces = emptyMap(),
            hintVi = "Đưa mặt đầu tiên vào khung vuông và giữ 1 giây nhé!",
            hintEn = "Hold the first face inside the square for 1 second!"
        )

        countdownJob = viewModelScope.launch {
            for (sec in ScannerUiState.MAX_SCAN_SECONDS downTo 1) {
                _uiState.update { it.copy(remainingSeconds = sec) }
                delay(1000L)
                if (_uiState.value.phase != ScannerPhase.Recording) return@launch
            }
            // Reached 30-second limit
            _uiState.update { it.copy(remainingSeconds = 0) }
            finishRecordingAndValidate()
        }
    }

    fun onCameraFrameAnalyzed(result: FaceSampleResult) {
        val current = _uiState.value
        if (current.phase != ScannerPhase.Recording) return

        val now = System.currentTimeMillis()
        if (now - lastFrameMs < 130L) return
        lastFrameMs = now

        when (result) {
            is FaceSampleResult.Unstable -> {
                streakCount = 0
                _uiState.update { it.copy(liveDetectedCenter = null, livePreviewGrid = null) }
            }
            is FaceSampleResult.Detected -> {
                if (streakCenter == result.center && streakGrid == result.grid) {
                    streakCount++
                } else {
                    streakCenter = result.center
                    streakGrid = result.grid
                    streakCount = 1
                }

                _uiState.update {
                    it.copy(
                        liveDetectedCenter = result.center,
                        livePreviewGrid = result.grid
                    )
                }

                // Require 3 consistent reads (~400ms steady hold) to lock a face
                if (streakCount >= 3) {
                    val updatedFaces = current.capturedFaces.toMutableMap()
                    val isNewFace = result.center !in updatedFaces
                    updatedFaces[result.center] = result.grid

                    val nextMissing = CubeColor.entries.firstOrNull { it !in updatedFaces }
                    val hintPair = if (nextMissing != null) {
                        "Tuyệt vời! Xoay tiếp mặt tâm ${nextMissing.viName} (${updatedFaces.size}/6)" to
                            "Great! Now show the ${nextMissing.enName} center (${updatedFaces.size}/6)"
                    } else {
                        "Đủ 6 mặt rồi! Đang kiểm tra khối Rubik..." to
                            "All 6 faces captured! Validating cube..."
                    }

                    _uiState.update {
                        it.copy(
                            capturedFaces = updatedFaces,
                            hintVi = hintPair.first,
                            hintEn = hintPair.second
                        )
                    }

                    if (isNewFace) {
                        if (updatedFaces.size == 6) {
                            _soundEvents.tryEmit(ScanSoundEvent.ALL_SIX_FACES_CAPTURED)
                            finishRecordingAndValidate()
                        } else {
                            _soundEvents.tryEmit(ScanSoundEvent.SINGLE_FACE_CAPTURED)
                        }
                    }
                }
            }
        }
    }

    fun finishRecordingAndValidate() {
        if (_uiState.value.phase != ScannerPhase.Recording) return
        countdownJob?.cancel()
        _uiState.update { it.copy(phase = ScannerPhase.Validating) }
        onRequestStopRecording?.invoke()
        validateCapturedFaces()
    }

    fun validateCapturedFaces() {
        val faces = _uiState.value.capturedFaces
        viewModelScope.launch {
            when (val res = CubeValidator.reconcileAndValidate(faces)) {
                is CubeValidationResult.Valid -> {
                    appContainer.setScannedCube(res.cube)
                    _uiState.update { it.copy(phase = ScannerPhase.Success(res.cube)) }
                }
                is CubeValidationResult.Invalid -> {
                    _uiState.update {
                        it.copy(
                            phase = ScannerPhase.ValidationFailed(
                                viReason = res.viReason,
                                enReason = res.enReason
                            )
                        )
                    }
                }
            }
        }
    }

    fun resetToReady() {
        countdownJob?.cancel()
        onRequestStopRecording?.invoke()
        _uiState.value = ScannerUiState(phase = ScannerPhase.Ready)
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }

    class Factory(
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ScannerViewModel(appContainer) as T
        }
    }
}
