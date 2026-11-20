package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.LocalAppLanguage
import com.example.data.UserProfile
import com.example.data.tr
import com.example.tutorial.AlgorithmRepository
import com.example.tutorial.HandbookChapter
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
import kotlinx.coroutines.launch

@Composable
fun CfopHandbookContent(
    userProfile: UserProfile?,
    onLaunchTrainerAlgorithm: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val chapters = AlgorithmRepository.handbookChapters
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val expandedGroups = remember { mutableStateMapOf<String, Boolean>() }

    Column(modifier = modifier.fillMaxSize()) {
        // Quick Table of Contents Bar (Mục Lục Nhanh)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(chapters) { index, chapter ->
                Surface(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                        .testTag("handbook_toc_${chapter.number}"),
                    shape = RoundedCornerShape(12.dp),
                    color = SoftBlueSurface,
                    border = BorderStroke(1.dp, RubikBlue.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${chapter.number}. ${tr(chapter.shortTitleVi, chapter.shortTitleEn)}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = RubikBlue
                        )
                    }
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(chapters, key = { it.number }) { chapter ->
                if (chapter.number == 8) {
                    RoadmapChapterCard(chapter = chapter, userProfile = userProfile, lang = lang)
                } else {
                    StandardChapterCard(
                        chapter = chapter,
                        expandedGroups = expandedGroups,
                        onLaunchTrainerAlgorithm = onLaunchTrainerAlgorithm
                    )
                }
            }
        }
    }
}

@Composable
private fun StandardChapterCard(
    chapter: HandbookChapter,
    expandedGroups: MutableMap<String, Boolean>,
    onLaunchTrainerAlgorithm: (String) -> Unit
) {
    val isLargeChapter = chapter.number == 6 || chapter.number == 7
    ToyBlockCard(
        containerColor = Color.White,
        borderColor = when (chapter.number) {
            1, 2 -> RubikBlue.copy(alpha = 0.35f)
            3 -> ToyGreen.copy(alpha = 0.4f)
            4, 6 -> SunnyYellow.copy(alpha = 0.7f)
            else -> ToyOrange.copy(alpha = 0.45f)
        }
    ) {
        Text(
            text = tr(chapter.titleVi, chapter.titleEn),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = InkPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(chapter.summaryVi, chapter.summaryEn),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )

        Spacer(modifier = Modifier.height(10.dp))

        chapter.groups.forEachIndexed { idx, group ->
            val groupKey = "${chapter.number}_$idx"
            val isExpanded = expandedGroups[groupKey] ?: (!isLargeChapter || idx == 0)

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftBlueSurface.copy(alpha = 0.55f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedGroups[groupKey] = !isExpanded }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = tr(group.titleVi, group.titleEn),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = InkSecondary
                        )
                    }

                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val bullets = if (LocalAppLanguage.current == com.example.data.AppLanguage.VI) {
                            group.bulletsVi
                        } else {
                            group.bulletsEn
                        }
                        bullets.forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkPrimary,
                                modifier = Modifier.padding(vertical = 3.dp)
                            )
                        }

                        group.entries.forEach { entry ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, OutlineSoft),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = tr(entry.label, entry.labelEn),
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = InkPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = entry.formula,
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            color = RubikBlue,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        val note = tr(entry.noteVi, entry.noteEn)
                                        if (note.isNotEmpty()) {
                                            Text(
                                                text = note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = InkSecondary
                                            )
                                        }
                                    }
                                    if (entry.trainerAlgorithmId != null) {
                                        Surface(
                                            modifier = Modifier
                                                .height(40.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { onLaunchTrainerAlgorithm(entry.trainerAlgorithmId) },
                                            shape = RoundedCornerShape(10.dp),
                                            color = SoftGreenSurface
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayCircleOutline,
                                                    contentDescription = tr("Luyện ngay", "Practice"),
                                                    tint = ToyGreen,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = tr("Tập", "Drill"),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = ToyGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapChapterCard(
    chapter: HandbookChapter,
    userProfile: UserProfile?,
    lang: com.example.data.AppLanguage
) {
    ToyBlockCard(
        containerColor = Color.White,
        borderColor = ToyGreen.copy(alpha = 0.55f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ToyOrange)
            Text(
                text = tr(chapter.titleVi, chapter.titleEn),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = tr(chapter.summaryVi, chapter.summaryEn),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSecondary
        )
        Spacer(modifier = Modifier.height(10.dp))

        AlgorithmRepository.roadmapRows.forEach { row ->
            val isUserMatch = userProfile != null && (
                userProfile.targetLevel == row.toLevel || userProfile.currentLevel == row.fromLevel
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isUserMatch) SoftGreenSurface else SoftAmberSurface.copy(alpha = 0.5f),
                border = BorderStroke(
                    width = if (isUserMatch) 2.dp else 1.dp,
                    color = if (isUserMatch) ToyGreen else OutlineSoft
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${row.fromLevel.title(lang)} → ${row.toLevel.title(lang)}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isUserMatch) ToyGreen else InkPrimary
                        )
                        if (isUserMatch) {
                            Surface(shape = RoundedCornerShape(8.dp), color = ToyGreen) {
                                Text(
                                    text = tr("Mục tiêu của bạn", "Your Goal"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr(row.focusVi, row.focusEn),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = InkPrimary
                    )
                    Text(
                        text = tr(row.splitsVi, row.splitsEn),
                        fontFamily = JetBrainsMonoFontFamily,
                        style = MaterialTheme.typography.bodySmall,
                        color = RubikBlue
                    )
                    Text(
                        text = tr(row.coreSkillsVi, row.coreSkillsEn),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSecondary
                    )
                }
            }
        }
    }
}
