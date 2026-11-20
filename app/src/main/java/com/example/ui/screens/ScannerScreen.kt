package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.AppLanguage
import com.example.data.LocalAppLanguage
import com.example.data.tr
import com.example.rubik.CubeColor
import com.example.scanner.RubikColorClassifier
import com.example.scanner.ScanSoundPlayer
import com.example.ui.components.MiniFaceGrid
import com.example.ui.components.ToyBlockCard
import com.example.ui.components.toStickerColor
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftCoralSurface
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.ui.theme.ToyRed
import com.example.viewmodel.ScanSoundEvent
import com.example.viewmodel.ScannerPhase
import com.example.viewmodel.ScannerUiState
import com.example.viewmodel.ScannerViewModel
import java.io.File
import java.util.concurrent.Executors

@Composable
fun ScannerScreen(
    state: ScannerUiState,
    soundEnabled: Boolean,
    viewModel: ScannerViewModel,
    onCubeScannedSuccess: () -> Unit,
    onSwitchToPractice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(viewModel, soundEnabled) {
        viewModel.soundEvents.collect { event ->
            if (!soundEnabled) return@collect
            when (event) {
                ScanSoundEvent.SINGLE_FACE_CAPTURED -> ScanSoundPlayer.playSingleFaceBeep()
                ScanSoundEvent.ALL_SIX_FACES_CAPTURED -> ScanSoundPlayer.playAllSixFacesBeep()
            }
        }
    }

    LaunchedEffect(state.phase) {
        if (state.phase is ScannerPhase.Success) {
            onCubeScannedSuccess()
            viewModel.resetToReady()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!hasCameraPermission) {
            ToyBlockCard(
                modifier = Modifier.widthIn(max = 520.dp),
                containerColor = Color.White
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cubie_hero),
                        contentDescription = tr("Cubie cần quyền camera", "Cubie needs camera permission"),
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Text(
                        text = tr("Cần Quyền Camera", "Camera Permission Needed"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = tr(
                            stringResource(R.string.camera_permission_rationale),
                            "Allow camera access to record a 30-second video of your Rubik's Cube and detect all 6 faces automatically."
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = InkSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("grant_camera_permission_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
                    ) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tr(stringResource(R.string.grant_camera_permission), "Allow Camera"))
                    }
                    OutlinedButton(
                        onClick = onSwitchToPractice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scanner_fallback_practice_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(tr("Dùng Scramble Tập Luyện", "Use Practice Scramble"))
                    }
                }
            }
        } else {
            when (val phase = state.phase) {
                is ScannerPhase.ValidationFailed -> {
                    ValidationFailedContent(
                        viReason = phase.viReason,
                        enReason = phase.enReason,
                        capturedCount = state.progressCount,
                        onRetry = { viewModel.resetToReady() },
                        onSwitchToPractice = onSwitchToPractice
                    )
                }
                is ScannerPhase.Validating -> {
                    ToyBlockCard(modifier = Modifier.widthIn(max = 460.dp)) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CircularProgressIndicator(color = RubikBlue)
                            Text(
                                text = tr(
                                    "Đang kiểm tra 54 ô màu & tính thuật toán CFOP...",
                                    "Validating 54 stickers & computing optimal CFOP..."
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    CameraVideoScannerView(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraVideoScannerView(
    state: ScannerUiState,
    viewModel: ScannerViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val lang = LocalAppLanguage.current
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var videoCapture by remember { mutableStateOf<VideoCapture<Recorder>?>(null) }
    var activeRecording by remember { mutableStateOf<Recording?>(null) }
    var tempVideoFile by remember { mutableStateOf<File?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        viewModel.attachStopRecordingCallback {
            try {
                activeRecording?.stop()
            } catch (_: Throwable) {
            }
            activeRecording = null
        }
        onDispose {
            try {
                activeRecording?.stop()
            } catch (_: Throwable) {
            }
            tempVideoFile?.delete()
            analysisExecutor.shutdown()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 640.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top status bar: 30s countdown + 6-face guide ring
        ToyBlockCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.remainingSeconds <= 8 && state.phase == ScannerPhase.Recording) {
                        SoftCoralSurface
                    } else {
                        RubikBlue.copy(alpha = 0.12f)
                    }
                ) {
                    Text(
                        text = "⏱ ${state.remainingSeconds}s / 30s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (state.remainingSeconds <= 8 && state.phase == ScannerPhase.Recording) ToyRed else RubikBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Text(
                    text = tr(
                        "Đã quét ${state.progressCount}/6 mặt",
                        "Scanned ${state.progressCount}/6 faces"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6-Face Guide Ring chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CubeColor.entries.forEach { color ->
                    val captured = color in state.capturedFaces
                    val colorLabel = if (lang == AppLanguage.VI) color.viName else color.enName
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            modifier = Modifier.size(36.dp),
                            shape = CircleShape,
                            color = color.toStickerColor(),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (captured) 3.dp else 1.5.dp,
                                color = if (captured) ToyGreen else InkSecondary.copy(alpha = 0.45f)
                            )
                        ) {
                            if (captured) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = tr("$colorLabel đã quét", "$colorLabel scanned"),
                                        tint = if (color == CubeColor.WHITE || color == CubeColor.YELLOW) InkPrimary else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = colorLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (captured) ToyGreen else InkSecondary
                        )
                    }
                }
            }
        }

        // CameraX Preview Box with 3x3 Alignment Reticle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val recorder = Recorder.Builder()
                                .setQualitySelector(QualitySelector.from(Quality.SD))
                                .build()
                            val vc = VideoCapture.withOutput(recorder)
                            videoCapture = vc

                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { ia ->
                                    ia.setAnalyzer(analysisExecutor) { imageProxy ->
                                        try {
                                            val result = RubikColorClassifier.analyzeFrame(imageProxy)
                                            viewModel.onCameraFrameAnalyzed(result)
                                        } finally {
                                            imageProxy.close()
                                        }
                                    }
                                }

                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                analysis,
                                vc
                            )
                        } catch (t: Throwable) {
                            Log.e("ScannerScreen", "CameraX bind error", t)
                            cameraError = t.localizedMessage ?: tr(lang, "Không khởi động được camera", "Unable to start camera")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )

            // 3x3 Guide Grid Overlay + Free-Rotation Ring
            Canvas(modifier = Modifier.fillMaxSize()) {
                val boxSize = minOf(size.width, size.height) * 0.52f
                val left = (size.width - boxSize) / 2f
                val top = (size.height - boxSize) / 2f
                val cell = boxSize / 3f

                // Outer rotation ring hint
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = boxSize * 0.68f,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 4f)
                )

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )

                for (i in 1..2) {
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(left + cell * i, top),
                        end = Offset(left + cell * i, top + boxSize),
                        strokeWidth = 3f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.7f),
                        start = Offset(left, top + cell * i),
                        end = Offset(left + boxSize, top + cell * i),
                        strokeWidth = 3f
                    )
                }
            }

            // Live detected 3x3 face preview in corner
            state.livePreviewGrid?.let { grid ->
                MiniFaceGrid(
                    colors = grid,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
                        .size(72.dp)
                )
            }

            if (cameraError != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = SoftCoralSurface
                ) {
                    Text(
                        text = cameraError ?: "",
                        color = ToyRed,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Hint & Record 30s Controls
        ToyBlockCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = tr(state.hintVi, state.hintEn),
                style = MaterialTheme.typography.titleMedium,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (state.phase == ScannerPhase.Recording) {
                Button(
                    onClick = { viewModel.finishRecordingAndValidate() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("scanner_stop_record_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToyOrange)
                ) {
                    Icon(Icons.Filled.Stop, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(
                            "Dừng & Kiểm Tra Ngay (${state.progressCount}/6)",
                            "Stop & Check Now (${state.progressCount}/6)"
                        )
                    )
                }
            } else {
                Button(
                    onClick = {
                        val vc = videoCapture
                        if (vc != null) {
                            val file = File(context.cacheDir, "cubiekid_scan_temp.mp4")
                            tempVideoFile = file
                            val opts = FileOutputOptions.Builder(file).build()
                            val pending = vc.output.prepareRecording(context, opts)
                            activeRecording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
                                if (event is VideoRecordEvent.Finalize) {
                                    file.delete()
                                }
                            }
                        }
                        viewModel.startRecordingSession()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("scanner_start_record_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToyRed)
                ) {
                    Icon(Icons.Filled.FiberManualRecord, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(
                            "Bắt Đầu Quay Video 30s",
                            "Start 30s Video Scan"
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ValidationFailedContent(
    viReason: String,
    enReason: String,
    capturedCount: Int,
    onRetry: () -> Unit,
    onSwitchToPractice: () -> Unit
) {
    ToyBlockCard(
        modifier = Modifier.widthIn(max = 520.dp),
        containerColor = Color.White,
        borderColor = ToyOrange
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_cubie_retry),
                contentDescription = tr("Cubie hướng dẫn quay video lại", "Cubie asks to record again"),
                modifier = Modifier
                    .size(160.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
            Text(
                text = tr(
                    "Ôi! Bé hãy quay lại video nhé ($capturedCount/6 mặt)",
                    "Oops! Let's record the video again ($capturedCount/6 faces)"
                ),
                style = MaterialTheme.typography.headlineMedium,
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftCoralSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = tr(viReason, enReason),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = ToyRed,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("scanner_retry_record_button"),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tr("Quay Video Lại (30s)", "Record Video Again (30s)"))
            }
            OutlinedButton(
                onClick = onSwitchToPractice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("retry_practice_fallback_button"),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text(tr("Chuyển sang Tự Luyện Scramble", "Switch to Practice Scramble"))
            }
        }
    }
}
