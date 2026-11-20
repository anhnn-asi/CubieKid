package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.LocalAppLanguage
import com.example.data.TrainingRepository
import com.example.data.tr
import com.example.rubik.CfopSolution
import com.example.rubik.CfopStage
import com.example.rubik.CubeMove
import com.example.rubik.F2lSolver
import java.util.Locale
import com.example.ui.components.CfopStageBadge
import com.example.ui.components.Interactive3DCubeView
import com.example.ui.components.PhaseSplitBar
import com.example.ui.components.ToyBlockCard
import com.example.ui.components.UnfoldedCubeNet
import com.example.ui.components.accentColor
import com.example.ui.components.softSurfaceColor
import com.example.ui.components.toStickerColor
import com.example.ui.theme.CubePlasticDark
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.viewmodel.SolverTabMode
import com.example.viewmodel.SolverTimerUiState
import com.example.viewmodel.SolverTimerViewModel
import com.example.viewmodel.TimerPhase

@Composable
fun SolverTimerScreen(
    state: SolverTimerUiState,
    viewModel: SolverTimerViewModel,
    onOpenStats: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Switch Header (CFOP Guide vs 4-Phase Step Timer)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeTabButton(
                        title = tr("Hướng Dẫn CFOP", "CFOP Guide"),
                        selected = state.activeTab == SolverTabMode.GUIDE,
                        onClick = { viewModel.selectTab(SolverTabMode.GUIDE) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_cfop_guide")
                    )
                    ModeTabButton(
                        title = tr("Bấm Giờ 4 Pha", "4-Phase Timer"),
                        selected = state.activeTab == SolverTabMode.TIMER,
                        onClick = { viewModel.selectTab(SolverTabMode.TIMER) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("tab_step_timer")
                    )
                }
            }

            // Active Scramble / Source Banner
            item {
                ToyBlockCard(containerColor = SoftBlueSurface) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (state.sourceType == "CAMERA_VIDEO") {
                                    tr("Rubik Từ Camera 30s", "Scanned from 30s Video")
                                } else {
                                    tr("Scramble Luyện Tập", "Practice Scramble")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = RubikBlue
                            )
                            Text(
                                text = state.scrambleNotation,
                                style = MaterialTheme.typography.labelMedium,
                                color = InkPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        FilledTonalButton(
                            onClick = { viewModel.generateNewPracticeScramble() },
                            modifier = Modifier.height(48.dp).testTag("new_scramble_button")
                        ) {
                            Icon(Icons.Filled.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Mới", "New"), maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            if (state.isComputingSolution) {
                item {
                    ToyBlockCard {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(color = RubikBlue)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                tr(
                                    "Đang tìm lộ trình CFOP tối ưu fingertrick...",
                                    "Finding optimal fingertrick CFOP solution..."
                                )
                            )
                        }
                    }
                }
            } else if (state.activeTab == SolverTabMode.GUIDE) {
                item {
                    CfopGuideContent(state = state, viewModel = viewModel)
                }
            } else {
                item {
                    StepTimerContent(
                        state = state,
                        viewModel = viewModel,
                        onOpenStats = onOpenStats
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) RubikBlue else Color.White,
        border = BorderStroke(2.dp, if (selected) RubikBlue else RubikBlue.copy(alpha = 0.25f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) Color.White else InkPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CfopGuideContent(
    state: SolverTimerUiState,
    viewModel: SolverTimerViewModel
) {
    val solution = state.solution ?: return
    val steps = solution.steps
    val currentStep = if (state.currentStepIndex < steps.size) steps[state.currentStepIndex] else null
    var showNet by remember { mutableStateOf(false) }
    val lang = LocalAppLanguage.current
    val playSpeedFormatted = String.format(Locale.US, "%.1fs", state.playStepDelayMs / 1000f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Optimal CFOP Summary Card
        ToyBlockCard(containerColor = Color.White) {
            Text(
                text = tr(
                    "Lộ Trình CFOP Tối Ưu (${solution.totalMoveCount} bước)",
                    "Optimal CFOP Solution (${solution.totalMoveCount} moves)"
                ),
                style = MaterialTheme.typography.titleLarge,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CfopStage.entries.forEach { stage ->
                    val active = currentStep?.stage == stage
                    CfopStageBadge(
                        stage = stage,
                        isActive = active,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { viewModel.jumpToStage(stage) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            val crossNotation = solution.crossMoves.joinToString(" ") { it.notation }
                .ifEmpty { tr("Đã xong!", "Already solved!") }
            Text(
                text = tr(
                    "• Cross tối ưu fingertrick: ${solution.crossMoves.size} bước ($crossNotation)",
                    "• Optimal fingertrick Cross: ${solution.crossMoves.size} moves ($crossNotation)"
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = RubikBlue
            )
            Text(
                text = "• OLL: ${solution.ollCaseName}  |  PLL: ${solution.pllCaseName}",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }

        // 3D Isometric Cube + Active Move Overlay
        ToyBlockCard(containerColor = Color.White) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (currentStep != null) {
                        tr(
                            "Bước ${state.currentStepIndex + 1}/${steps.size} • ${currentStep.stage.enTitle}",
                            "Step ${state.currentStepIndex + 1}/${steps.size} • ${currentStep.stage.enTitle}"
                        )
                    } else {
                        tr("Hoàn Thành Khối Rubik!", "Cube Solved!")
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = SoftAmberSurface
                ) {
                    Text(
                        text = "Play $playSpeedFormatted",
                        style = MaterialTheme.typography.labelMedium,
                        color = ToyOrange,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                IconButton(
                    onClick = { showNet = !showNet },
                    modifier = Modifier.size(48.dp).testTag("toggle_cube_net_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.GridView,
                        contentDescription = tr("Hiện/ẩn sơ đồ 6 mặt", "Show/hide 6-face net"),
                        tint = if (showNet) RubikBlue else InkSecondary
                    )
                }
            }

            val frontCenter = state.displayedCube.colorAt(22)
            val bottomCenter = state.displayedCube.colorAt(31)
            val frontName = if (lang == AppLanguage.VI) frontCenter.viName else frontCenter.enName
            val bottomName = if (lang == AppLanguage.VI) bottomCenter.viName else bottomCenter.enName
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftBlueSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OrientationDotChip(
                        title = tr("Front: Tâm $frontName", "Front: $frontName Center"),
                        color = frontCenter.toStickerColor()
                    )
                    OrientationDotChip(
                        title = tr("Đáy (D): $bottomName", "Bottom (D): $bottomName"),
                        color = bottomCenter.toStickerColor()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            FingertrickChunksBar(
                solution = solution,
                currentStepIndex = state.currentStepIndex,
                isAutoPlaying = state.isAutoPlaying,
                onSelectChunk = { startIdx -> viewModel.jumpToStep(startIdx) }
            )

            Interactive3DCubeView(
                cube = state.displayedCube,
                solution = solution,
                currentStepIndex = state.currentStepIndex,
                activeMove = currentStep?.move,
                playStepDelayMs = state.playStepDelayMs,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            )

            if (showNet) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UnfoldedCubeNet(cube = state.displayedCube)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (currentStep != null) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftAmberSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = currentStep.stage.accentColor(),
                            modifier = Modifier.size(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = currentStep.move.notation,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = if (currentStep.stage == CfopStage.OLL) InkPrimary else Color.White
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr(
                                    currentStep.move.viInstruction,
                                    currentStep.move.enInstruction
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Text(
                                text = tr(
                                    "Fingertrick: ${currentStep.move.fingertrickTipVi}",
                                    "Fingertrick: ${currentStep.move.fingertrickTipEn}"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = ToyOrange,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentStep.caseLabel,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                            val highlightHint = when (currentStep.stage) {
                                CfopStage.F2L -> {
                                    val slot = F2lSolver.F2lSlot.entries.firstOrNull { it.label == currentStep.caseLabel }
                                    if (slot != null) {
                                        val c1 = if (lang == AppLanguage.VI) slot.eColorF.viName else slot.eColorF.enName
                                        val c2 = if (lang == AppLanguage.VI) slot.eColorS.viName else slot.eColorS.enName
                                        val white = if (lang == AppLanguage.VI) "Trắng" else "White"
                                        tr(
                                            "Viền nổi bật: viên $c1-$c2 & $c1-$c2-$white",
                                            "Highlighted: $c1-$c2 & $c1-$c2-$white cubies"
                                        )
                                    } else null
                                }
                                CfopStage.CROSS -> tr(
                                    "Viền nổi bật: 4 viên cạnh Trắng đáy",
                                    "Highlighted: 4 White Cross edges"
                                )
                                else -> null
                            }
                            if (highlightHint != null) {
                                Text(
                                    text = highlightHint,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC026D3)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftGreenSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = tr(
                            "Tuyệt vời! Bé đã giải xong toàn bộ khối Rubik theo chuẩn CFOP!",
                            "Awesome! You solved the entire Rubik's Cube with optimal CFOP!"
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = ToyGreen,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.resetSteps() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.size(50.dp).testTag("guide_reset_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Replay, contentDescription = tr("Về đầu", "Reset"))
                }
                OutlinedButton(
                    onClick = { viewModel.stepBackward() },
                    enabled = state.currentStepIndex > 0,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("guide_prev_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(tr("Trước", "Prev"), maxLines = 1, softWrap = false)
                }
                Button(
                    onClick = { viewModel.toggleAutoPlay() },
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.size(50.dp).testTag("guide_autoplay_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ToyOrange)
                ) {
                    Icon(
                        imageVector = if (state.isAutoPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (state.isAutoPlaying) tr("Tạm dừng", "Pause") else tr("Tự động phát", "Auto Play")
                    )
                }
                Button(
                    onClick = { viewModel.stepForward() },
                    enabled = state.currentStepIndex < steps.size,
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.weight(1f).height(50.dp).testTag("guide_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
                ) {
                    Text(tr("Tiếp", "Next"), maxLines = 1, softWrap = false)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StepTimerContent(
    state: SolverTimerUiState,
    viewModel: SolverTimerViewModel,
    onOpenStats: () -> Unit
) {
    val tapZoneColor = when (state.timerPhase) {
        TimerPhase.IDLE -> RubikBlue
        TimerPhase.RUNNING_CROSS -> RubikBlue
        TimerPhase.RUNNING_F2L -> ToyGreen
        TimerPhase.RUNNING_OLL -> SunnyYellow
        TimerPhase.RUNNING_PLL -> ToyOrange
        TimerPhase.COMPLETED -> ToyGreen
    }
    val tapPrompt = when (state.timerPhase) {
        TimerPhase.IDLE -> tr("CHẠM ĐỂ BẮT ĐẦU (CROSS)", "TAP TO START (CROSS)")
        TimerPhase.RUNNING_CROSS -> tr("ĐANG GIẢI CROSS\nChạm khi xong Cross → F2L", "SOLVING CROSS\nTap when done → F2L")
        TimerPhase.RUNNING_F2L -> tr("ĐANG GIẢI F2L\nChạm khi xong F2L → OLL", "SOLVING F2L\nTap when done → OLL")
        TimerPhase.RUNNING_OLL -> tr("ĐANG GIẢI OLL\nChạm khi xong OLL → PLL", "SOLVING OLL\nTap when done → PLL")
        TimerPhase.RUNNING_PLL -> tr("ĐANG GIẢI PLL\nChạm để HOÀN THÀNH!", "SOLVING PLL\nTap to FINISH!")
        TimerPhase.COMPLETED -> tr("HOÀN THÀNH! CHẠM ĐỂ TẬP LẠI", "COMPLETED! TAP TO RETRY")
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 4 Phase Live Split Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SplitChip("Cross", state.crossSplitMs, state.timerPhase == TimerPhase.RUNNING_CROSS, Modifier.weight(1f))
            SplitChip("F2L", state.f2lSplitMs, state.timerPhase == TimerPhase.RUNNING_F2L, Modifier.weight(1f))
            SplitChip("OLL", state.ollSplitMs, state.timerPhase == TimerPhase.RUNNING_OLL, Modifier.weight(1f))
            SplitChip("PLL", state.pllSplitMs, state.timerPhase == TimerPhase.RUNNING_PLL, Modifier.weight(1f))
        }

        // Giant Kid-Friendly Tap-to-Advance Pad
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 210.dp)
                .clip(RoundedCornerShape(28.dp))
                .clickable { viewModel.onTimerPadTapped() }
                .testTag("step_timer_tap_pad"),
            shape = RoundedCornerShape(28.dp),
            color = tapZoneColor,
            shadowElevation = 4.dp
        ) {
            val contentColor = if (state.timerPhase == TimerPhase.RUNNING_OLL) InkPrimary else Color.White
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (state.timerPhase == TimerPhase.IDLE) Icons.Filled.Timer else Icons.Filled.TouchApp,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TrainingRepository.formatDurationMs(state.elapsedTotalMs),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 44.sp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = tapPrompt,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (state.timerPhase != TimerPhase.IDLE && state.timerPhase != TimerPhase.COMPLETED) {
            OutlinedButton(
                onClick = { viewModel.resetTimer() },
                modifier = Modifier.fillMaxWidth().height(48.dp).testTag("reset_timer_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(tr("Hủy & Đặt Lại Đồng Hồ", "Cancel & Reset Timer"))
            }
        }

        // Completed & Saved to SQLite Celebration Card
        if (state.timerPhase == TimerPhase.COMPLETED) {
            ToyBlockCard(containerColor = Color.White, borderColor = SunnyYellow) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cubie_trophy),
                        contentDescription = tr("Cubie chúc mừng bé hoàn thành", "Cubie celebrates your solve"),
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (state.savedSession?.isPersonalBest == true) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = SunnyYellow
                            ) {
                                Text(
                                    text = tr("KỶ LỤC MỚI (PB)!", "NEW PERSONAL BEST!"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(
                            text = tr("Đã lưu vào Nhật Ký SQLite!", "Saved to SQLite Log!"),
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = tr("Phân tích tốc độ 4 pha CFOP:", "CFOP 4-phase speed breakdown:"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                PhaseSplitBar(
                    crossMs = state.crossSplitMs,
                    f2lMs = state.f2lSplitMs,
                    ollMs = state.ollSplitMs,
                    pllMs = state.pllSplitMs
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.generateNewPracticeScramble() },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("completed_next_scramble"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(tr("Scramble Mới", "New Scramble"))
                    }
                    Button(
                        onClick = onOpenStats,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("completed_view_progress"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
                    ) {
                        Text(tr("Xem Tiến Độ", "View Progress"))
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitChip(
    label: String,
    ms: Long,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (isActive) SoftAmberSurface else Color.White,
        border = BorderStroke(2.dp, if (isActive) ToyOrange else RubikBlue.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = InkPrimary
            )
            Text(
                text = if (ms > 0L) TrainingRepository.formatDurationMs(ms) else "--",
                style = MaterialTheme.typography.labelMedium,
                color = if (ms > 0L) RubikBlue else InkSecondary
            )
        }
    }
}

@Composable
private fun OrientationDotChip(
    title: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
            color = color,
            border = BorderStroke(1.5.dp, CubePlasticDark)
        ) {}
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = InkPrimary
        )
    }
}

private data class FingertrickChunk(
    val label: String,
    val stage: CfopStage,
    val startStepIndex: Int,
    val moves: List<CubeMove>
) {
    val endStepIndexExclusive: Int
        get() = startStepIndex + moves.size
}

private fun buildFingertrickChunks(solution: CfopSolution): List<FingertrickChunk> {
    val steps = solution.steps
    if (steps.isEmpty()) return emptyList()

    val chunks = mutableListOf<FingertrickChunk>()
    var i = 0
    var f2lCounter = 0
    while (i < steps.size) {
        val first = steps[i]
        var j = i + 1
        while (j < steps.size &&
            steps[j].stage == first.stage &&
            steps[j].caseLabel == first.caseLabel
        ) {
            j++
        }
        val chunkLabel = when (first.stage) {
            CfopStage.CROSS -> "Cross"
            CfopStage.F2L -> {
                f2lCounter++
                "F2L-$f2lCounter"
            }
            CfopStage.OLL -> "OLL"
            CfopStage.PLL -> "PLL"
        }
        chunks.add(
            FingertrickChunk(
                label = chunkLabel,
                stage = first.stage,
                startStepIndex = i,
                moves = steps.subList(i, j).map { it.move }
            )
        )
        i = j
    }
    return chunks
}

@Composable
private fun FingertrickChunksBar(
    solution: CfopSolution,
    currentStepIndex: Int,
    isAutoPlaying: Boolean,
    onSelectChunk: (Int) -> Unit
) {
    val chunks = remember(solution) { buildFingertrickChunks(solution) }
    if (chunks.isEmpty()) return

    val activeChunkIndex = chunks.indexOfFirst {
        currentStepIndex >= it.startStepIndex && currentStepIndex < it.endStepIndexExclusive
    }
    val listState = rememberLazyListState()

    LaunchedEffect(activeChunkIndex) {
        if (activeChunkIndex >= 0) {
            listState.animateScrollToItem(activeChunkIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fingertrick_chunks_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val activeChunk = chunks.getOrNull(activeChunkIndex)
        if (activeChunk != null) {
            val activeMoveOffset = currentStepIndex - activeChunk.startStepIndex
            val stageColor = activeChunk.stage.accentColor()
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = activeChunk.stage.softSurfaceColor(),
                border = BorderStroke(1.5.dp, stageColor.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = stageColor
                    ) {
                        Text(
                            text = activeChunk.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (activeChunk.stage == CfopStage.OLL) InkPrimary else Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = buildAnnotatedString {
                            append("(")
                            activeChunk.moves.forEachIndexed { idx, move ->
                                if (idx > 0) append(" ")
                                if (idx == activeMoveOffset) {
                                    withStyle(
                                        SpanStyle(
                                            color = if (isAutoPlaying || activeChunk.stage != CfopStage.OLL) ToyOrange else RubikBlue,
                                            fontWeight = FontWeight.ExtraBold,
                                            background = Color.White
                                        )
                                    ) {
                                        append(" ${move.notation} ")
                                    }
                                } else if (idx < activeMoveOffset) {
                                    withStyle(SpanStyle(color = InkSecondary.copy(alpha = 0.65f))) {
                                        append(move.notation)
                                    }
                                } else {
                                    withStyle(SpanStyle(color = InkPrimary, fontWeight = FontWeight.SemiBold)) {
                                        append(move.notation)
                                    }
                                }
                            }
                            append(")")
                        },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(chunks) { idx, chunk ->
                val isActive = idx == activeChunkIndex
                val isDone = currentStepIndex >= chunk.endStepIndexExclusive
                val accent = chunk.stage.accentColor()
                val notationStr = chunk.moves.joinToString(" ") { it.notation }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isActive -> accent
                        isDone -> SoftGreenSurface
                        else -> Color.White
                    },
                    border = BorderStroke(
                        width = if (isActive) 2.dp else 1.dp,
                        color = when {
                            isActive -> accent
                            isDone -> ToyGreen.copy(alpha = 0.5f)
                            else -> RubikBlue.copy(alpha = 0.22f)
                        }
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectChunk(chunk.startStepIndex) }
                        .testTag("fingertrick_chunk_${idx}")
                ) {
                    val textColor = when {
                        isActive && chunk.stage == CfopStage.OLL -> InkPrimary
                        isActive -> Color.White
                        isDone -> ToyGreen
                        else -> InkPrimary
                    }
                    Text(
                        text = "${chunk.label} ($notationStr)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
