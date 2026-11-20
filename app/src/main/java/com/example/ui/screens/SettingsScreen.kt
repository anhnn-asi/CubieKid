package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.tr
import com.example.ui.components.ToyBlockCard
import com.example.ui.theme.CubePlasticDark
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.StickerGreen
import com.example.ui.theme.StickerWhite
import com.example.ui.theme.StickerYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import java.util.Locale
import kotlin.math.roundToLong

import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.data.LocalAppLanguage
import com.example.data.ProfileValidationError
import com.example.data.RubikLevel
import com.example.data.UserProfile
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.ToyRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    userProfile: UserProfile?,
    onSaveUserProfile: (
        name: String,
        age: Int,
        currentLevel: RubikLevel,
        targetLevel: RubikLevel,
        onResult: (ProfileValidationError?) -> Unit
    ) -> Unit,
    onSelectPlaySpeedMs: (Long) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val speedSec = settings.playStepDelayMs / 1000f
    val speedFormatted = String.format(Locale.US, "%.1fs", speedSec)

    var isEditingProfile by remember { mutableStateOf(false) }
    var draftName by remember(userProfile, isEditingProfile) { mutableStateOf(userProfile?.name ?: "") }
    var draftAge by remember(userProfile, isEditingProfile) { mutableIntStateOf(userProfile?.age ?: 15) }
    var draftCurrent by remember(userProfile, isEditingProfile) {
        mutableStateOf(userProfile?.currentLevel ?: RubikLevel.BEGINNER)
    }
    var draftTarget by remember(userProfile, isEditingProfile) {
        mutableStateOf(userProfile?.targetLevel ?: RubikLevel.SUB_60)
    }
    var profileError by remember { mutableStateOf<ProfileValidationError?>(null) }

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
                Text(
                    text = tr("Cài Đặt", "Settings"),
                    style = MaterialTheme.typography.headlineLarge,
                    color = InkPrimary
                )
                Text(
                    text = tr(
                        "Tùy chỉnh hồ sơ trình độ, tốc độ xoay, âm thanh và ngôn ngữ",
                        "Customize player profile, rotation speed, sound effects, and language"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }

            // 0. User Profile Card (SQLite-backed)
            item {
                ToyBlockCard(
                    containerColor = Color.White,
                    borderColor = RubikBlue.copy(alpha = 0.45f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftBlueSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.Person, contentDescription = null, tint = RubikBlue)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Hồ sơ", "Profile"),
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = if (userProfile != null) {
                                    tr(
                                        "${userProfile.name} (${userProfile.age} tuổi) • ${userProfile.currentLevel.title(lang)} → ${userProfile.targetLevel.title(lang)}",
                                        "${userProfile.name} (${userProfile.age} yrs) • ${userProfile.currentLevel.title(lang)} → ${userProfile.targetLevel.title(lang)}"
                                    )
                                } else {
                                    tr("Chưa có hồ sơ", "No profile saved")
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                isEditingProfile = !isEditingProfile
                                profileError = null
                            },
                            modifier = Modifier.testTag("settings_edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isEditingProfile) tr("Đóng", "Close") else tr("Sửa", "Edit"))
                        }
                    }

                    if (userProfile != null && !isEditingProfile) {
                        val mode = userProfile.trainingMode()
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SoftGreenSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = tr("Chế độ kích hoạt: ${mode.viTitle}", "Active Mode: ${mode.enTitle}"),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = ToyGreen
                                )
                                Text(
                                    text = userProfile.targetLevel.recommendedMethod(lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = InkPrimary
                                )
                            }
                        }
                    }

                    if (isEditingProfile) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = draftName,
                            onValueChange = {
                                draftName = it
                                profileError = null
                            },
                            label = { Text(tr("Tên / Nickname", "Name / Nickname")) },
                            singleLine = true,
                            isError = profileError == ProfileValidationError.NameTooShort,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_profile_name_input"),
                            shape = RoundedCornerShape(14.dp)
                        )
                        if (profileError == ProfileValidationError.NameTooShort) {
                            Text(
                                text = tr("Tên cần tối thiểu 2 ký tự.", "Name must be at least 2 characters."),
                                style = MaterialTheme.typography.bodySmall,
                                color = ToyRed
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = tr("Tuổi: $draftAge", "Age: $draftAge"),
                                style = MaterialTheme.typography.titleMedium,
                                color = InkPrimary
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { draftAge = (draftAge - 1).coerceIn(4, 99) },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = tr("Giảm tuổi", "Decrease age"))
                                }
                                OutlinedButton(
                                    onClick = { draftAge = (draftAge + 1).coerceIn(4, 99) },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = tr("Tăng tuổi", "Increase age"))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tr("Trình độ hiện tại", "Current Level"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            RubikLevel.entries.forEach { lvl ->
                                FilterChip(
                                    selected = draftCurrent == lvl,
                                    onClick = {
                                        draftCurrent = lvl
                                        val allowed = lvl.availableTargets()
                                        if (draftTarget !in allowed) draftTarget = allowed.first()
                                        profileError = null
                                    },
                                    label = { Text(lvl.title(lang)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = tr("Mục tiêu tiếp theo", "Target Level"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            draftCurrent.availableTargets().forEach { lvl ->
                                FilterChip(
                                    selected = draftTarget == lvl,
                                    onClick = {
                                        draftTarget = lvl
                                        profileError = null
                                    },
                                    label = { Text(lvl.title(lang)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                onSaveUserProfile(draftName, draftAge, draftCurrent, draftTarget) { err ->
                                    if (err == null) {
                                        isEditingProfile = false
                                    } else {
                                        profileError = err
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("settings_save_profile_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ToyGreen)
                        ) {
                            Text(tr("Lưu Hồ Sơ", "Save Profile"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Sound Effects Toggle Card
            item {
                ToyBlockCard(
                    containerColor = Color.White,
                    borderColor = if (settings.soundEnabled) ToyGreen.copy(alpha = 0.45f) else OutlineSoft
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (settings.soundEnabled) ToyGreen.copy(alpha = 0.14f) else SoftBlueSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (settings.soundEnabled) {
                                        Icons.AutoMirrored.Filled.VolumeUp
                                    } else {
                                        Icons.AutoMirrored.Filled.VolumeOff
                                    },
                                    contentDescription = null,
                                    tint = if (settings.soundEnabled) ToyGreen else InkSecondary
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Âm thanh khi quét video", "Video Scan Sound"),
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = tr(
                                    "Báo 1 tiếng khi nhận 1 mặt, 2 tiếng khi đủ 6 mặt",
                                    "1 beep per face detected, 2 beeps when all 6 faces are done"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                        Switch(
                            checked = settings.soundEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ToyGreen
                            ),
                            modifier = Modifier.testTag("settings_sound_switch")
                        )
                    }
                }
            }

            // 1. Play Speed Card
            item {
                ToyBlockCard(containerColor = Color.White, borderColor = ToyOrange.copy(alpha = 0.4f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftAmberSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.SlowMotionVideo,
                                    contentDescription = null,
                                    tint = ToyOrange
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = tr("Tốc độ nút Play", "Play Auto-Turn Speed"),
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = tr(
                                    "Mặc định 1.0 giây mỗi bước xoay",
                                    "Default 1.0 second per rotation"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ToyOrange
                        ) {
                            Text(
                                text = speedFormatted,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Slider(
                        value = settings.playStepDelayMs.toFloat(),
                        onValueChange = { raw ->
                            val snapped = ((raw / 250f).roundToLong() * 250L).coerceIn(500L, 3000L)
                            onSelectPlaySpeedMs(snapped)
                        },
                        valueRange = 500f..3000f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = ToyOrange,
                            activeTrackColor = ToyOrange
                        ),
                        modifier = Modifier.testTag("play_speed_slider")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AppSettings.PLAY_SPEED_OPTIONS_MS.forEach { optionMs ->
                            val label = when (optionMs) {
                                500L -> tr("0.5s (Nhanh)", "0.5s (Fast)")
                                1000L -> tr("1.0s (Mặc định)", "1.0s (Default)")
                                1500L -> tr("1.5s (Chậm)", "1.5s (Slow)")
                                2000L -> tr("2.0s (Rất chậm)", "2.0s (Very slow)")
                                else -> tr("3.0s (Từng bước)", "3.0s (Step-by-step)")
                            }
                            FilterChip(
                                selected = settings.playStepDelayMs == optionMs,
                                onClick = { onSelectPlaySpeedMs(optionMs) },
                                label = { Text(label) },
                                modifier = Modifier.testTag("speed_chip_${optionMs}")
                            )
                        }
                    }
                }
            }

            // 2. Language Card (Tiếng Việt / English)
            item {
                ToyBlockCard(containerColor = Color.White, borderColor = RubikBlue.copy(alpha = 0.4f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SoftBlueSurface,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    tint = RubikBlue
                                )
                            }
                        }
                        Column {
                            Text(
                                text = tr("Ngôn ngữ ứng dụng", "App Language"),
                                style = MaterialTheme.typography.titleLarge,
                                color = InkPrimary
                            )
                            Text(
                                text = tr(
                                    "Chọn Tiếng Việt hoặc English",
                                    "Choose Tiếng Việt or English"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LanguageOptionCard(
                            title = "Tiếng Việt",
                            subtitle = "Giao diện tiếng Việt",
                            selected = settings.language == AppLanguage.VI,
                            onClick = { onSelectLanguage(AppLanguage.VI) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_option_vi")
                        )
                        LanguageOptionCard(
                            title = "English",
                            subtitle = "English interface",
                            selected = settings.language == AppLanguage.EN,
                            onClick = { onSelectLanguage(AppLanguage.EN) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("lang_option_en")
                        )
                    }
                }
            }

            // 3. Default Cube Orientation Reference Card
            item {
                ToyBlockCard(containerColor = SoftBlueSurface) {
                    Text(
                        text = tr("Hướng cầm Rubik mặc định", "Default Cube Orientation"),
                        style = MaterialTheme.typography.titleLarge,
                        color = InkPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OrientationRow(tr("Mặt trước (Front): Tâm Xanh lá", "Front Center: Green"), StickerGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    OrientationRow(tr("Mặt đáy (D): Tâm Trắng (Cross)", "Bottom (D) Center: White (Cross)"), StickerWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    OrientationRow(tr("Mặt trên (U): Tâm Vàng (OLL / PLL)", "Top (U) Center: Yellow (OLL / PLL)"), StickerYellow)
                }
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) SoftBlueSurface else Color.White,
        border = BorderStroke(2.dp, if (selected) RubikBlue else OutlineSoft)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) RubikBlue else InkPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = ToyGreen
                )
            }
        }
    }
}

@Composable
private fun OrientationRow(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.size(16.dp),
            shape = CircleShape,
            color = color,
            border = BorderStroke(1.5.dp, CubePlasticDark)
        ) {}
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = InkPrimary
        )
    }
}
