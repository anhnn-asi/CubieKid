package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.TrainingRepository
import com.example.data.tr
import com.example.ui.components.DailyGoalRing
import com.example.ui.components.PhaseSplitBar
import com.example.ui.components.ToyBlockCard
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.viewmodel.HomeUiState

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Timer
import com.example.data.LocalAppLanguage
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.SoftCoralSurface
import com.example.ui.theme.ToyPurple

@Composable
fun HomeScreen(
    state: HomeUiState,
    onOpenScanner: () -> Unit,
    onStartPractice: () -> Unit,
    onOpenTutorial: () -> Unit,
    onOpenCompetitionTimer: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenProfile: () -> Unit,
    onChangeDailyTarget: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val profile = state.userProfile
    val mode = profile?.trainingMode()

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ToyBlockCard(
                    containerColor = Color.White,
                    borderColor = RubikBlue.copy(alpha = 0.35f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SoftBlueSurface, SoftAmberSurface)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_cubie_hero),
                                contentDescription = tr("Linh vật Cubie chào mừng bé", "Cubie mascot welcoming you"),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(18.dp))
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = SunnyYellow
                                ) {
                                    Text(
                                        text = if (mode != null) tr(mode.viTitle, mode.enTitle) else "CFOP Kids Academy",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = InkPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (profile != null && profile.name.isNotBlank()) {
                                        tr("Xin chào ${profile.name}!", "Hello, ${profile.name}!")
                                    } else {
                                        tr("Xin chào nhà vô địch Rubik!", "Hello, Rubik's Champion!")
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = InkPrimary
                                )
                                Text(
                                    text = if (profile != null) {
                                        tr(
                                            "Mục tiêu: ${profile.targetLevel.title(lang)} • ${profile.targetLevel.recommendedMethod(lang)}",
                                            "Goal: ${profile.targetLevel.title(lang)} • ${profile.targetLevel.recommendedMethod(lang)}"
                                        )
                                    } else {
                                        tr(
                                            "Quay video 30s để giải CFOP tối ưu & luyện tốc độ mỗi ngày.",
                                            "Record a 30s video for optimal CFOP steps & daily speed training."
                                        )
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(onClick = onOpenProfile)
                                        .testTag("home_badges_pill"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, ToyOrange.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EmojiEvents,
                                            contentDescription = null,
                                            tint = ToyOrange,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tr(
                                                "Huy hiệu: ${state.unlockedBadgesCount}/${state.totalBadgesCount}",
                                                "Badges: ${state.unlockedBadgesCount}/${state.totalBadgesCount}"
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = InkPrimary
                                        )
                                        if (state.unseenBadgesCount > 0) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = com.example.ui.theme.ToyRed
                                            ) {
                                                Text(
                                                    text = tr("MỚI", "NEW"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onOpenScanner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                            .testTag("home_scan_cube_button"),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Quay Rubik Giải Ngay (30s)", "Scan & Solve Cube (30s)"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onStartPractice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("home_practice_scramble_button"),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Casino,
                            contentDescription = null,
                            tint = ToyOrange
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = tr("Tạo Scramble Tập Luyện CFOP", "Practice CFOP Scramble"),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                    }
                }
            }

            // Quick Launch Grid: Tutorial & Fullscreen WCA Competition Timer
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenTutorial)
                            .testTag("home_open_tutorial_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = SoftAmberSurface,
                        border = BorderStroke(1.5.dp, ToyOrange.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ToyOrange,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.MenuBook, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = tr("Học CFOP", "Learn CFOP"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InkPrimary
                            )
                            Text(
                                text = tr("Cẩm nang & Luyện tập", "Handbook & Practice"),
                                style = MaterialTheme.typography.bodySmall,
                                color = InkSecondary
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onOpenCompetitionTimer)
                            .testTag("home_open_wca_timer_button"),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF131B2E),
                        border = BorderStroke(1.5.dp, ToyPurple.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ToyPurple,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = tr("Đồng hồ Thi đấu", "Competition Timer"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (state.wcaTotalSolves > 0) {
                                    "PB: ${state.wcaPb.format()} • Ao5: ${state.wcaAo5.format()}"
                                } else {
                                    tr("Toàn màn hình • 15s WCA", "Fullscreen • 15s WCA Inspect")
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.78f)
                            )
                        }
                    }
                }
            }

            item {
                ToyBlockCard(containerColor = SoftGreenSurface, borderColor = ToyGreen.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        DailyGoalRing(
                            completed = state.todayCompleted,
                            target = state.todayTarget
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Mục tiêu hôm nay", "Today's Goal"),
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = tr("Chuỗi ngày", "Streak"),
                                    tint = ToyOrange,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = tr(
                                        "Chuỗi ${state.streakDays} ngày liên tiếp",
                                        "${state.streakDays}-day streak"
                                    ),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = InkPrimary
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = tr("Mục tiêu:", "Target:"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                                IconButton(
                                    onClick = { onChangeDailyTarget(state.todayTarget - 1) },
                                    modifier = Modifier.size(48.dp).testTag("decrease_goal_button")
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = tr("Giảm mục tiêu", "Decrease target"))
                                }
                                Text(
                                    text = "${state.todayTarget}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { onChangeDailyTarget(state.todayTarget + 1) },
                                    modifier = Modifier.size(48.dp).testTag("increase_goal_button")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = tr("Tăng mục tiêu", "Increase target"))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatMiniCard(
                        title = tr("Kỷ lục (PB)", "Best (PB)"),
                        value = state.bestTimeMs?.let { TrainingRepository.formatDurationMs(it) } ?: "--",
                        accent = SunnyYellow,
                        icon = Icons.Filled.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                    StatMiniCard(
                        title = tr("TB 5 lượt (Ao5)", "Avg of 5 (Ao5)"),
                        value = state.ao5Ms?.let { TrainingRepository.formatDurationMs(it) } ?: "--",
                        accent = ToyOrange,
                        icon = Icons.Filled.Bolt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Lần giải gần đây", "Recent Solves"),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    if (state.totalSolves > 0) {
                        OutlinedButton(
                            onClick = onOpenStats,
                            modifier = Modifier.testTag("home_view_all_stats_button")
                        ) {
                            Text(tr("Xem tất cả (${state.totalSolves})", "View all (${state.totalSolves})"))
                        }
                    }
                }
            }

            if (state.recentSessions.isEmpty()) {
                item {
                    ToyBlockCard(containerColor = SoftBlueSurface) {
                        Text(
                            text = tr("Bé chưa lưu lượt giải nào hôm nay!", "No solves saved today yet!"),
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "Hãy bấm Quay Rubik hoặc Tạo Scramble để thực hiện lượt giải CFOP đầu tiên và xem biểu đồ tốc độ nhé.",
                                "Tap Scan Cube or Practice CFOP to complete your first solve and track your speed!"
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                }
            } else {
                items(state.recentSessions, key = { it.id }) { session ->
                    ToyBlockCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = TrainingRepository.formatDurationMs(session.totalDurationMs),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (session.isPersonalBest) ToyOrange else InkPrimary
                                )
                                Text(
                                    text = tr(
                                        "${session.dateKey} • Cross ${session.crossMoveCount} bước",
                                        "${session.dateKey} • Cross ${session.crossMoveCount} moves"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSecondary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (session.sourceType == "CAMERA_VIDEO") SoftBlueSurface else SoftAmberSurface
                            ) {
                                Text(
                                    text = if (session.sourceType == "CAMERA_VIDEO") {
                                        tr("Quay 30s", "Camera 30s")
                                    } else {
                                        tr("Tự tập", "Practice")
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = InkPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
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
private fun StatMiniCard(
    title: String,
    value: String,
    accent: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    ToyBlockCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.22f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = InkPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = InkPrimary
        )
    }
}
