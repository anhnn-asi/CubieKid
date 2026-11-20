package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.WcaSolveRecord
import com.example.data.tr
import com.example.rubik.ExtendedMoveEngine
import com.example.ui.components.Interactive3DCubeView
import com.example.ui.components.ToyBlockCard
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.viewmodel.TrainerMode
import com.example.viewmodel.TutorialTab
import com.example.viewmodel.TutorialUiState
import com.example.viewmodel.TutorialViewModel

@Composable
fun TutorialScreen(
    state: TutorialUiState,
    viewModel: TutorialViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp)
        ) {
            // Top Bar + Sub-Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("tutorial_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Quay lại", "Back"),
                        tint = InkPrimary
                    )
                }
                Text(
                    text = tr("Học CFOP & Luyện Thế", "CFOP Tutorial & Trainer"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TutorialTabPill(
                    title = tr("Luyện Thế 3D", "3D Trainer"),
                    icon = Icons.Filled.School,
                    selected = state.activeTab == TutorialTab.TRAINER,
                    onClick = { viewModel.selectTab(TutorialTab.TRAINER) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutorial_tab_trainer")
                )
                TutorialTabPill(
                    title = tr("Cẩm Nang CFOP", "CFOP Handbook"),
                    icon = Icons.Filled.MenuBook,
                    selected = state.activeTab == TutorialTab.HANDBOOK,
                    onClick = { viewModel.selectTab(TutorialTab.HANDBOOK) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("tutorial_tab_handbook")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (state.activeTab) {
                TutorialTab.TRAINER -> {
                    AlgorithmTrainerBody(
                        state = state,
                        viewModel = viewModel,
                        modifier = Modifier.weight(1f)
                    )
                }
                TutorialTab.HANDBOOK -> {
                    CfopHandbookContent(
                        userProfile = state.userProfile,
                        onLaunchTrainerAlgorithm = { algId ->
                            viewModel.openAlgorithmFromHandbook(algId)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AlgorithmTrainerBody(
    state: TutorialUiState,
    viewModel: TutorialViewModel,
    modifier: Modifier = Modifier
) {
    val selectedCategory = state.categories.firstOrNull { it.id == state.selectedCategoryId }
    val algorithms = selectedCategory?.let {
        viewModel.visibleAlgorithmsFor(it, state.filterByTargetLevel, state.userProfile)
    } ?: emptyList()

    val currentAlg = state.currentAlgorithm
    val nextExpectedToken = state.expectedMoves.getOrNull(state.nextMoveIndex)
    val guideMove = if (state.mode == TrainerMode.LEARN && state.isHintVisible && !state.isSolved && nextExpectedToken != null) {
        ExtendedMoveEngine.toVisualGuideMove(nextExpectedToken)
    } else null

    val quickMoveTokens = buildMovePadForAlgorithm(state.expectedMoves)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Category + Level Filter + Mode Switch
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.categories, key = { it.id }) { cat ->
                    FilterChip(
                        selected = cat.id == state.selectedCategoryId,
                        onClick = { viewModel.selectCategory(cat.id) },
                        label = { Text(cat.name) },
                        modifier = Modifier.testTag("trainer_cat_${cat.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(algorithms, key = { it.id }) { alg ->
                    val selected = alg.id == currentAlg?.id
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.loadAlgorithm(alg, state.mode) },
                        label = { Text(alg.name, maxLines = 1) },
                        modifier = Modifier.testTag("trainer_alg_${alg.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = state.mode == TrainerMode.LEARN,
                    onClick = { viewModel.setMode(TrainerMode.LEARN) },
                    label = { Text(tr("Học (Learn)", "Learn")) },
                    modifier = Modifier.testTag("trainer_mode_learn")
                )
                FilterChip(
                    selected = state.mode == TrainerMode.DRILL,
                    onClick = { viewModel.setMode(TrainerMode.DRILL) },
                    label = { Text(tr("Luyện phản xạ (Drill)", "Drill")) },
                    modifier = Modifier.testTag("trainer_mode_drill")
                )
                FilterChip(
                    selected = state.isRandomAufEnabled,
                    onClick = { viewModel.toggleRandomAuf() },
                    label = { Text("Random AUF") },
                    modifier = Modifier.testTag("trainer_random_auf")
                )
            }
        }

        // Main 3D Interactive Rubik's Cube Card
        item {
            ToyBlockCard(
                containerColor = Color.White,
                borderColor = if (state.isSolved) ToyGreen.copy(alpha = 0.7f) else RubikBlue.copy(alpha = 0.4f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentAlg?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = currentAlg?.group ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = InkSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = { viewModel.toggleHint() },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("trainer_hint_button")
                    ) {
                        Icon(
                            imageVector = if (state.isHintVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Gợi ý", "Hint"))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Interactive3DCubeView(
                        cube = state.cube,
                        solution = null,
                        currentStepIndex = state.nextMoveIndex,
                        activeMove = guideMove,
                        playStepDelayMs = 600L,
                        onUserSwipeToken = { token -> viewModel.performMove(token) },
                        lastManualMoveTrigger = state.manualMoveTrigger,
                        modifier = Modifier.fillMaxSize()
                    )

                    if (state.isSolved) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = ToyGreen.copy(alpha = 0.94f),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text(
                                    text = tr(
                                        "Hoàn thành! (${WcaSolveRecord.formatWcaMillis(state.elapsedMillis)}s)",
                                        "Solved! (${WcaSolveRecord.formatWcaMillis(state.elapsedMillis)}s)"
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Text(
                    text = tr(
                        "Vuốt trực tiếp trên ô màu để xoay mặt, hoặc bấm phím xoay bên dưới.",
                        "Swipe directly on any sticker to turn a layer, or tap the move keys below."
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Algorithm Formula Strip (hidden in Drill mode unless Hint toggled)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (state.isHintVisible) SoftAmberSurface else SoftBlueSurface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isHintVisible) {
                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tr("Công thức:", "Alg:"),
                                style = MaterialTheme.typography.labelLarge,
                                color = InkSecondary
                            )
                            state.expectedMoves.forEachIndexed { idx, tok ->
                                val isCurrent = !state.isSolved && idx == state.nextMoveIndex
                                val isDone = state.isSolved || idx < state.nextMoveIndex
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isCurrent -> ToyOrange
                                        isDone -> ToyGreen.copy(alpha = 0.22f)
                                        else -> Color.White
                                    }
                                ) {
                                    Text(
                                        text = tok,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isCurrent) Color.White else InkPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = tr("Công thức đang ẩn (Chế độ Drill) — bấm [Gợi ý] để xem", "Formula hidden (Drill Mode) — tap [Hint] to reveal"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        // Move Control Keypad + Step Helper
        item {
            ToyBlockCard(containerColor = Color.White, borderColor = OutlineSoft) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Phím xoay nhanh", "Quick Move Pad"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                    if (state.mode == TrainerMode.LEARN && !state.isSolved && nextExpectedToken != null) {
                        Button(
                            onClick = { viewModel.stepExpectedMove() },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToyGreen),
                            modifier = Modifier.testTag("trainer_step_next_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(tr("Xoay $nextExpectedToken", "Do $nextExpectedToken"))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickMoveTokens.forEach { tok ->
                        val isNextHint = state.mode == TrainerMode.LEARN && state.isHintVisible && tok == nextExpectedToken && !state.isSolved
                        Surface(
                            modifier = Modifier
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { viewModel.performMove(tok) }
                                .testTag("move_pad_$tok"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isNextHint) SunnyYellow else SoftBlueSurface,
                            border = BorderStroke(
                                width = if (isNextHint) 2.dp else 1.dp,
                                color = if (isNextHint) ToyOrange else OutlineSoft
                            )
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 14.dp)
                            ) {
                                Text(
                                    text = tok,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Session Stats & Reshuffle Footer
        item {
            ToyBlockCard(containerColor = SoftGreenSurface, borderColor = ToyGreen.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrainerStatColumn(
                        label = tr("Thời gian", "Time"),
                        value = "${WcaSolveRecord.formatWcaMillis(state.elapsedMillis)}s"
                    )
                    TrainerStatColumn(
                        label = tr("Kỷ lục", "Best"),
                        value = state.bestMillis?.let { "${WcaSolveRecord.formatWcaMillis(it)}s" } ?: "--"
                    )
                    TrainerStatColumn(
                        label = tr("Số lần", "Count"),
                        value = state.completedCount.toString()
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.reloadCurrentAlgorithm() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("trainer_reshuffle_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RubikBlue)
                ) {
                    Icon(Icons.Filled.Casino, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(tr("Xáo trộn lại thế cờ (Next)", "Reset & Scramble Case"))
                }
            }
        }
    }
}

@Composable
private fun TrainerStatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = InkSecondary)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge,
            color = InkPrimary
        )
    }
}

@Composable
private fun TutorialTabPill(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) RubikBlue else Color.White,
        border = BorderStroke(1.5.dp, if (selected) RubikBlue else OutlineSoft)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else InkSecondary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else InkPrimary,
                maxLines = 1
            )
        }
    }
}

private fun buildMovePadForAlgorithm(expectedMoves: List<String>): List<String> {
    val defaults = listOf(
        "U", "U'", "U2",
        "R", "R'", "R2",
        "F", "F'",
        "L", "L'",
        "D", "D'"
    )
    val extras = expectedMoves.filter { it !in defaults }.distinct()
    return (defaults + extras).distinct()
}
