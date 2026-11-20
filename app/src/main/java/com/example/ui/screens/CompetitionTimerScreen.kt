package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.WcaPenalty
import com.example.data.tr
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.ui.theme.ToyRed
import com.example.viewmodel.CompetitionTimerState
import com.example.viewmodel.CompetitionTimerUiState
import com.example.viewmodel.CompetitionTimerViewModel

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun CompetitionTimerScreen(
    state: CompetitionTimerUiState,
    viewModel: CompetitionTimerViewModel,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = LocalHapticFeedback.current

    // Keep screen on & enable true Fullscreen Immersive Mode while active
    DisposableEffect(view, context) {
        val window = context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(Unit) {
        viewModel.hapticPulseEvents.collect { isTwelveSec ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (isTwelveSec) {
                kotlinx.coroutines.delay(90L)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
    }

    BackHandler {
        when (state.timerState) {
            CompetitionTimerState.SOLVING -> {
                // Prevent accidental exit while timer is running
            }
            CompetitionTimerState.INSPECTION -> {
                viewModel.cancelInspectionOrReset()
            }
            CompetitionTimerState.IDLE, CompetitionTimerState.FINISHED -> {
                onExit()
            }
        }
    }

    val isSolving = state.timerState == CompetitionTimerState.SOLVING
    val timerColor = when (state.timerState) {
        CompetitionTimerState.IDLE -> Color(0xFFF1F5F9)
        CompetitionTimerState.INSPECTION -> when {
            state.isInspectionOvertime -> ToyRed
            state.inspectionAlertBadge == "12s!" -> ToyOrange
            else -> SunnyYellow
        }
        CompetitionTimerState.SOLVING -> ToyGreen
        CompetitionTimerState.FINISHED -> Color(0xFF60A5FA)
    }

    val bgBrush = Brush.radialGradient(
        colors = listOf(
            when (state.timerState) {
                CompetitionTimerState.IDLE -> Color(0xFF172038)
                CompetitionTimerState.INSPECTION -> if (state.isInspectionOvertime) Color(0xFF3A1219) else Color(0xFF2B2210)
                CompetitionTimerState.SOLVING -> Color(0xFF0A2A1C)
                CompetitionTimerState.FINISHED -> Color(0xFF112340)
            },
            Color(0xFF070B14)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val stoppedOnDown = viewModel.onTouchDown()
                    val up = waitForUpOrCancellation()
                    if (!stoppedOnDown && up != null) {
                        viewModel.onScreenTap()
                    }
                }
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .testTag("wca_timer_touch_area")
    ) {
        // Top Bar: Exit (semi-transparent, hidden during SOLVING) + Scramble
        if (!isSolving) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(0.55f)
                            .clip(CircleShape)
                            .clickable(onClick = onExit)
                            .testTag("wca_timer_exit_button"),
                        color = Color.White.copy(alpha = 0.14f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = tr("Thoát Timer WCA", "Exit WCA Timer"),
                                tint = Color.White
                            )
                        }
                    }

                    if (state.timerState != CompetitionTimerState.INSPECTION) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = tr("WCA 3x3 Scramble", "WCA 3x3 Scramble"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                IconButton(
                                    onClick = { viewModel.regenerateScramble() },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("wca_new_scramble_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = tr("Đổi Scramble", "New Scramble"),
                                        tint = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SunnyYellow.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = state.inspectionAlertBadge ?: tr("Quan sát 15s", "15s Inspection"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = timerColor,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = state.scramble,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        lineHeight = 26.sp,
                        color = Color.White.copy(alpha = 0.94f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("wca_scramble_text")
                    )
                }
            }
        }

        // Center Precision Monospace Clock
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = state.displayText,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 78.sp,
                    color = timerColor,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1,
                modifier = Modifier.testTag("wca_clock_text")
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when (state.timerState) {
                    CompetitionTimerState.IDLE -> tr(
                        "Chạm bất kỳ đâu để bắt đầu 15s quan sát WCA",
                        "Tap anywhere to start 15s WCA Inspection"
                    )
                    CompetitionTimerState.INSPECTION -> tr(
                        "Đang quan sát (8s / 12s báo hiệu) • Chạm để BẮT ĐẦU GIẢI",
                        "Inspecting (8s / 12s alerts) • Tap to START SOLVE"
                    )
                    CompetitionTimerState.SOLVING -> tr(
                        "Chạm bất kỳ đâu để DỪNG ĐỒNG HỒ",
                        "Tap anywhere to STOP TIMER"
                    )
                    CompetitionTimerState.FINISHED -> tr(
                        "Đã lưu kết quả • Chạm màn hình để bắt đầu lượt mới",
                        "Result saved • Tap anywhere for next solve"
                    )
                },
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = if (isSolving) 0.55f else 0.78f),
                textAlign = TextAlign.Center
            )
        }

        // Bottom Stats & Quick Penalty Bar (hidden during SOLVING)
        if (!isSolving) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.timerState == CompetitionTimerState.FINISHED && state.lastSavedRecord != null) {
                    val lastPenalty = state.lastSavedRecord.penalty
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PenaltyToggleChip(
                            label = "+2",
                            selected = lastPenalty == WcaPenalty.PLUS_TWO,
                            activeColor = ToyOrange,
                            onClick = { viewModel.toggleLastPenalty(WcaPenalty.PLUS_TWO) },
                            modifier = Modifier.testTag("wca_penalty_plus2")
                        )
                        PenaltyToggleChip(
                            label = "DNF",
                            selected = lastPenalty == WcaPenalty.DNF,
                            activeColor = ToyRed,
                            onClick = { viewModel.toggleLastPenalty(WcaPenalty.DNF) },
                            modifier = Modifier.testTag("wca_penalty_dnf")
                        )
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { viewModel.deleteLastSolve() }
                                .testTag("wca_delete_last"),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Filled.DeleteOutline,
                                    contentDescription = tr("Xóa lượt vừa xong", "Delete last solve"),
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = tr("Xóa", "Delete"),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WcaStatBox("PB", state.pb.format(), Modifier.weight(1f))
                    WcaStatBox("Ao5", state.ao5.format(), Modifier.weight(1f))
                    WcaStatBox("Ao12", state.ao12.format(), Modifier.weight(1f))
                    WcaStatBox(tr("Lượt", "Solves"), state.totalSolves.toString(), Modifier.weight(0.85f))
                }
            }
        }
    }
}

@Composable
private fun PenaltyToggleChip(
    label: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) activeColor else Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, if (selected) activeColor else Color.White.copy(alpha = 0.25f))
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun WcaStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.65f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = TextStyle(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontFeatureSettings = "tnum"
                ),
                maxLines = 1
            )
        }
    }
}
