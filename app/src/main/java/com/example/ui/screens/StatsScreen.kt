package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DailyGoalEntity
import com.example.data.TrainingRepository
import com.example.data.tr
import com.example.ui.components.PhaseSplitBar
import com.example.ui.components.ToyBlockCard
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.viewmodel.SessionFilter
import com.example.viewmodel.StatsUiState

@Composable
fun StatsScreen(
    state: StatsUiState,
    onFilterSelected: (SessionFilter) -> Unit,
    onDeleteSession: (Long) -> Unit,
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
            item {
                Text(
                    text = tr("Lịch Sử & Tiến Độ Hàng Ngày", "History & Daily Progress"),
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkPrimary
                )
                Text(
                    text = tr(
                        "Dữ liệu SQLite luyện tập và phân tích tốc độ CFOP",
                        "SQLite practice logs & CFOP step speed breakdown"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricBox(
                        label = tr("Kỷ lục PB", "Best PB"),
                        value = state.bestTimeMs?.let { TrainingRepository.formatDurationMs(it) } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = tr("TB 5 lượt", "Ao5"),
                        value = state.ao5Ms?.let { TrainingRepository.formatDurationMs(it) } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                    MetricBox(
                        label = tr("Trung bình", "Average"),
                        value = state.averageTimeMs?.let { TrainingRepository.formatDurationMs(it) } ?: "--",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                ToyBlockCard(containerColor = Color.White) {
                    Text(
                        text = tr("Tiến Độ Luyện Tập Theo Ngày", "Daily Practice Solves"),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (state.dailyGoals.isEmpty()) {
                        Text(
                            text = tr(
                                "Chưa có dữ liệu luyện tập hàng ngày. Hãy bấm giờ ít nhất 1 lượt nhé!",
                                "No daily practice data yet. Complete at least 1 timed solve!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    } else {
                        DailySolvesChart(goals = state.dailyGoals)
                    }
                }
            }

            state.phaseBreakdown?.let { breakdown ->
                item {
                    ToyBlockCard(containerColor = Color.White) {
                        Text(
                            text = tr("Phân Tích Tốc Độ Từng Bước CFOP", "CFOP Step Speed Breakdown"),
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        PhaseSplitBar(
                            crossMs = breakdown.avgCrossMs,
                            f2lMs = breakdown.avgF2lMs,
                            ollMs = breakdown.avgOllMs,
                            pllMs = breakdown.avgPllMs
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SoftAmberSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = ToyOrange
                                )
                                Column {
                                    Text(
                                        text = tr(
                                            "Gợi ý cải thiện (${breakdown.slowestStage.enTitle}):",
                                            "Coach tip (${breakdown.slowestStage.enTitle}):"
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = tr(breakdown.tipVi, breakdown.tipEn),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SessionFilter.entries.forEach { f ->
                        val label = when (f) {
                            SessionFilter.ALL -> tr("Tất cả", "All")
                            SessionFilter.CAMERA -> tr("Từ Camera", "Camera")
                            SessionFilter.PRACTICE -> tr("Tự tập", "Practice")
                        }
                        FilterChip(
                            selected = state.filter == f,
                            onClick = { onFilterSelected(f) },
                            label = { Text(label) },
                            modifier = Modifier.testTag("filter_${f.name.lowercase()}")
                        )
                    }
                }
            }

            if (state.sessions.isEmpty()) {
                item {
                    ToyBlockCard(containerColor = SoftBlueSurface) {
                        Text(
                            text = tr("Chưa có lịch sử phù hợp bộ lọc này.", "No solve history for this filter."),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                    }
                }
            } else {
                items(state.sessions, key = { it.id }) { session ->
                    ToyBlockCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = TrainingRepository.formatDurationMs(session.totalDurationMs),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = if (session.isPersonalBest) ToyOrange else InkPrimary
                                    )
                                    if (session.isPersonalBest) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SoftAmberSurface
                                        ) {
                                            Text(
                                                text = "PB",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = ToyOrange,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = tr(
                                        "${session.dateKey} • Cross ${session.crossMoveCount} bước • Tổng ${session.totalMoveCount} bước",
                                        "${session.dateKey} • Cross ${session.crossMoveCount} moves • Total ${session.totalMoveCount} moves"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("delete_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = tr("Xóa lượt giải", "Delete solve"),
                                    tint = InkSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        PhaseSplitBar(
                            crossMs = session.crossMs,
                            f2lMs = session.f2lMs,
                            ollMs = session.ollMs,
                            pllMs = session.pllMs
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ToyBlockCard(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )
    }
}

@Composable
private fun DailySolvesChart(goals: List<DailyGoalEntity>) {
    val shown = goals.takeLast(7)
    val maxCount = (shown.maxOfOrNull { maxOf(it.completedSolves, it.targetSolves) } ?: 5).coerceAtLeast(1)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val count = shown.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.48f).coerceAtMost(48f)
            shown.forEachIndexed { idx, goal ->
                val ratio = goal.completedSolves.toFloat() / maxCount.toFloat()
                val barH = (size.height * 0.88f * ratio).coerceAtLeast(8f)
                val x = idx * slotW + (slotW - barW) / 2f
                val y = size.height - barH
                val color = if (goal.completedSolves >= goal.targetSolves) ToyGreen else RubikBlue
                drawRoundRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            shown.forEach { g ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${g.completedSolves}",
                        style = MaterialTheme.typography.labelLarge,
                        color = InkPrimary
                    )
                    Text(
                        text = g.dateKey.takeLast(5),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSecondary
                    )
                }
            }
        }
    }
}
