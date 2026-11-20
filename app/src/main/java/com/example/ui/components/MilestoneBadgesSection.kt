package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.MilestoneBadgeState
import com.example.data.MilestoneCategory
import com.example.data.MilestoneOverview
import com.example.data.tr
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
import com.example.ui.theme.ToyPurple
import com.example.ui.theme.ToyRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MilestoneBadgesSection(
    overview: MilestoneOverview,
    onAcknowledgeNewBadges: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBadge by remember { mutableStateOf<MilestoneBadgeState?>(null) }

    ToyBlockCard(
        modifier = modifier.testTag("profile_badges_card"),
        containerColor = Color.White,
        borderColor = SunnyYellow.copy(alpha = 0.85f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftAmberSurface,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ToyOrange)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("Huy hiệu thành tích", "Achievement Badges"),
                    style = MaterialTheme.typography.titleLarge,
                    color = InkPrimary
                )
                Text(
                    text = tr(
                        "Giải Rubik đều đặn mỗi ngày để sưu tầm trọn bộ huy hiệu!",
                        "Solve & practice daily to collect every badge!"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (overview.unlockedCount > 0) ToyGreen else SoftBlueSurface
            ) {
                Text(
                    text = "${overview.unlockedCount} / ${overview.totalCount}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = if (overview.unlockedCount > 0) Color.White else RubikBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }

        if (overview.unseenCount > 0) {
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SoftGreenSurface,
                border = BorderStroke(1.5.dp, ToyGreen.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onAcknowledgeNewBadges)
                    .testTag("badges_new_celebration_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ToyOrange)
                    Text(
                        text = tr(
                            "Tuyệt vời! Bạn vừa mở khóa ${overview.unseenCount} huy hiệu mới!",
                            "Awesome! You just unlocked ${overview.unseenCount} new badge(s)!"
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = ToyGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = tr("Tuyệt!", "Yay!"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = RubikBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2-column responsive badge grid
        val rows = overview.badges.chunked(2)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { badge ->
                        MilestoneBadgeTile(
                            badge = badge,
                            onClick = {
                                if (badge.isNew) onAcknowledgeNewBadges()
                                selectedBadge = badge
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    selectedBadge?.let { badge ->
        val accent = badgeAccentColor(badge.definition.id, badge.definition.category)
        AlertDialog(
            onDismissRequest = { selectedBadge = null },
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) {
                                Brush.linearGradient(listOf(accent, SunnyYellow))
                            } else {
                                Brush.linearGradient(listOf(SoftBlueSurface, SoftBlueSurface))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon(badge.definition.id),
                        contentDescription = null,
                        tint = if (badge.isUnlocked) Color.White else InkSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            },
            title = {
                Text(
                    text = tr(badge.definition.viTitle, badge.definition.enTitle),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = tr(badge.definition.viDescription, badge.definition.enDescription),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkPrimary,
                        textAlign = TextAlign.Center
                    )
                    if (badge.isUnlocked) {
                        val dateText = badge.unlockedAtEpochMs?.let {
                            SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it))
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = SoftGreenSurface) {
                            Text(
                                text = if (dateText != null) {
                                    tr("Đã đạt ngày $dateText ✓", "Unlocked on $dateText ✓")
                                } else {
                                    tr("Đã mở khóa ✓", "Unlocked ✓")
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ToyGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    } else {
                        val unit = tr(badge.definition.viUnit, badge.definition.enUnit)
                        Text(
                            text = tr(
                                "Tiến độ: ${badge.currentProgress} / ${badge.definition.targetValue} $unit",
                                "Progress: ${badge.currentProgress} / ${badge.definition.targetValue} $unit"
                            ),
                            fontFamily = JetBrainsMonoFontFamily,
                            style = MaterialTheme.typography.labelLarge,
                            color = RubikBlue
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBadge = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (badge.isUnlocked) ToyGreen else RubikBlue
                    )
                ) {
                    Text(tr("Đóng", "Got it!"))
                }
            }
        )
    }
}

@Composable
private fun MilestoneBadgeTile(
    badge: MilestoneBadgeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = badgeAccentColor(badge.definition.id, badge.definition.category)
    val bgColor by animateColorAsState(
        targetValue = if (badge.isUnlocked) accent.copy(alpha = 0.1f) else Color(0xFFF8FAFC),
        label = "badgeBg"
    )
    val borderColor = when {
        badge.isUnlocked -> accent.copy(alpha = 0.65f)
        else -> OutlineSoft
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag("badge_tile_${badge.definition.id}"),
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        border = BorderStroke(if (badge.isUnlocked) 2.dp else 1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(
                            if (badge.isUnlocked) {
                                Brush.linearGradient(listOf(accent, SunnyYellow.copy(alpha = 0.9f)))
                            } else {
                                Brush.linearGradient(listOf(SoftBlueSurface, SoftBlueSurface))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = badgeIcon(badge.definition.id),
                        contentDescription = tr(badge.definition.viTitle, badge.definition.enTitle),
                        tint = if (badge.isUnlocked) Color.White else InkSecondary.copy(alpha = 0.55f),
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (badge.isNew && badge.isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ToyRed,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = tr("MỚI", "NEW"),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (!badge.isUnlocked) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = tr("Chưa mở khóa", "Locked"),
                        tint = InkSecondary.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = tr("Đã đạt", "Unlocked"),
                        tint = ToyGreen,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.TopEnd)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = tr(badge.definition.viTitle, badge.definition.enTitle),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (badge.isUnlocked) InkPrimary else InkSecondary,
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            if (badge.isUnlocked) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accent.copy(alpha = 0.16f)
                ) {
                    Text(
                        text = tr("Đã đạt ✓", "Earned ✓"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            } else {
                LinearProgressIndicator(
                    progress = { badge.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = accent,
                    trackColor = OutlineSoft.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${badge.currentProgress}/${badge.definition.targetValue}",
                    fontFamily = JetBrainsMonoFontFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = InkSecondary
                )
            }
        }
    }
}

private fun badgeIcon(id: String): ImageVector = when (id) {
    "first_solve" -> Icons.Filled.Flag
    "solved_10" -> Icons.Filled.AutoAwesome
    "solved_50" -> Icons.Filled.WorkspacePremium
    "solved_100" -> Icons.Filled.EmojiEvents
    "streak_3" -> Icons.Filled.LocalFireDepartment
    "streak_7" -> Icons.Filled.Whatshot
    "camera_explorer" -> Icons.Filled.Videocam
    "speed_sub_30" -> Icons.Filled.Bolt
    else -> Icons.Filled.EmojiEvents
}

private fun badgeAccentColor(id: String, category: MilestoneCategory): Color = when (id) {
    "first_solve" -> ToyGreen
    "solved_10" -> RubikBlue
    "solved_50" -> ToyPurple
    "solved_100" -> ToyOrange
    "streak_3" -> ToyOrange
    "streak_7" -> ToyRed
    "camera_explorer" -> RubikBlue
    "speed_sub_30" -> ToyGreen
    else -> when (category) {
        MilestoneCategory.SOLVES -> RubikBlue
        MilestoneCategory.STREAK -> ToyOrange
        MilestoneCategory.SPEED -> ToyGreen
        MilestoneCategory.EXPLORER -> ToyPurple
    }
}
