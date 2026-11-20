package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.AppLanguage
import com.example.data.LocalAppLanguage
import com.example.data.ProfileValidationError
import com.example.data.RubikLevel
import com.example.data.UserProfile
import com.example.data.tr
import com.example.ui.components.ToyBlockCard
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.ui.theme.ToyRed
import com.example.viewmodel.OnboardingUiState
import com.example.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel,
    currentLanguage: AppLanguage,
    onSwitchLanguage: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = state.currentStep > 0) {
        viewModel.previousStep()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 640.dp)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header + Progress + Quick VI/EN toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_cubie_hero),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column {
                        Text(
                            text = "CubieKid CFOP",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary
                        )
                        Text(
                            text = tr("Bước ${state.currentStep + 1} / 3", "Step ${state.currentStep + 1} / 3"),
                            style = MaterialTheme.typography.labelLarge,
                            color = RubikBlue
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LanguageChipButton(
                        label = "VI",
                        selected = currentLanguage == AppLanguage.VI,
                        onClick = { onSwitchLanguage(AppLanguage.VI) }
                    )
                    LanguageChipButton(
                        label = "EN",
                        selected = currentLanguage == AppLanguage.EN,
                        onClick = { onSwitchLanguage(AppLanguage.EN) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { (state.currentStep + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = RubikBlue,
                trackColor = SoftBlueSurface
            )

            Spacer(modifier = Modifier.height(18.dp))

            AnimatedContent(
                targetState = state.currentStep,
                modifier = Modifier.weight(1f),
                label = "onboardingStep"
            ) { step ->
                when (step) {
                    0 -> StepOnePersonalInfo(state = state, viewModel = viewModel)
                    1 -> StepTwoCurrentLevel(
                        selected = state.currentLevel,
                        onSelect = { viewModel.selectCurrentLevel(it) }
                    )
                    else -> StepThreeTargetLevel(
                        state = state,
                        onSelectTarget = { viewModel.selectTargetLevel(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.currentStep > 0) {
                    OutlinedButton(
                        onClick = { viewModel.previousStep() },
                        modifier = Modifier
                            .height(54.dp)
                            .testTag("onboarding_back_button"),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(tr("Quay lại", "Back"))
                    }
                }
                Button(
                    onClick = {
                        if (state.currentStep < 2) viewModel.nextStep()
                        else viewModel.completeOnboarding()
                    },
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("onboarding_next_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.currentStep == 2) ToyGreen else RubikBlue
                    )
                ) {
                    Text(
                        text = if (state.currentStep < 2) {
                            tr("Tiếp tục", "Continue")
                        } else {
                            tr("Bắt đầu", "Start")
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (state.currentStep == 2) {
                            Icons.Filled.RocketLaunch
                        } else {
                            Icons.AutoMirrored.Filled.ArrowForward
                        },
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun StepOnePersonalInfo(
    state: OnboardingUiState,
    viewModel: OnboardingViewModel
) {
    val lang = LocalAppLanguage.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            ToyBlockCard(
                containerColor = Color.White,
                borderColor = RubikBlue.copy(alpha = 0.35f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Brush.linearGradient(listOf(SoftBlueSurface, SoftAmberSurface)))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SunnyYellow,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = InkPrimary)
                        }
                    }
                    Column {
                        Text(
                            text = tr("Chào mừng bạn mới!", "Welcome, Speedcuber!"),
                            style = MaterialTheme.typography.titleLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = tr(
                                "Cho Cubie biết tên và độ tuổi để cá nhân hóa lộ trình Rubik 3D nhé.",
                                "Tell Cubie your name and age to personalize your 3D Rubik roadmap."
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = tr("Tên hoặc Nickname *", "Your Name or Nickname *"),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = state.name,
                    onValueChange = { viewModel.updateName(it) },
                    placeholder = { Text(tr("Ví dụ: Minh Khôi", "e.g., Alex")) },
                    singleLine = true,
                    isError = state.validationError == ProfileValidationError.NameTooShort,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_name_input"),
                    shape = RoundedCornerShape(14.dp)
                )
                if (state.validationError == ProfileValidationError.NameTooShort) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tr("Vui lòng nhập tên tối thiểu 2 ký tự.", "Please enter at least 2 characters."),
                        style = MaterialTheme.typography.bodySmall,
                        color = ToyRed
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = tr("Độ tuổi của bạn", "Your Age"),
                    style = MaterialTheme.typography.titleMedium,
                    color = InkPrimary
                )
                Text(
                    text = tr(
                        "Giúp điều chỉnh độ chi tiết của hướng dẫn (gợi ý mặc định: 15)",
                        "Adjusts tutorial guidance detail (suggested default: 15)"
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.updateAge(state.age - 1) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SoftBlueSurface)
                            .testTag("onboarding_age_minus")
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = tr("Giảm tuổi", "Decrease age"))
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftAmberSurface,
                        border = BorderStroke(1.5.dp, ToyOrange.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = tr("${state.age} tuổi", "${state.age} yrs"),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = InkPrimary,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.updateAge(state.age + 1) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SoftBlueSurface)
                            .testTag("onboarding_age_plus")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = tr("Tăng tuổi", "Increase age"))
                    }
                }
            }
        }
    }
}

@Composable
private fun StepTwoCurrentLevel(
    selected: RubikLevel,
    onSelect: (RubikLevel) -> Unit
) {
    val lang = LocalAppLanguage.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = tr("Trình độ Rubik hiện tại của bạn?", "What is your current Rubik's level?"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Text(
                text = tr(
                    "Chọn mốc gần nhất với tốc độ giải hiện nay của bạn.",
                    "Choose the milestone closest to your current solve average."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        items(RubikLevel.entries) { level ->
            LevelOptionCard(
                level = level,
                lang = lang,
                selected = level == selected,
                accentColor = RubikBlue,
                onClick = { onSelect(level) },
                modifier = Modifier.testTag("onboarding_current_${level.name}")
            )
        }
    }
}

@Composable
private fun StepThreeTargetLevel(
    state: OnboardingUiState,
    onSelectTarget: (RubikLevel) -> Unit
) {
    val lang = LocalAppLanguage.current
    val previewProfile = UserProfile(
        name = state.name,
        age = state.age,
        currentLevel = state.currentLevel,
        targetLevel = state.targetLevel
    )
    val mode = previewProfile.trainingMode()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Text(
                text = tr("Mục tiêu tiếp theo bạn muốn đạt?", "What is your next speed goal?"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Text(
                text = tr(
                    "Chỉ hiển thị các mốc cao hơn trình độ hiện tại (${state.currentLevel.title(lang)}).",
                    "Filtered to milestones above your current level (${state.currentLevel.title(lang)})."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        items(state.availableTargetLevels) { level ->
            LevelOptionCard(
                level = level,
                lang = lang,
                selected = level == state.targetLevel,
                accentColor = ToyGreen,
                onClick = { onSelectTarget(level) },
                modifier = Modifier.testTag("onboarding_target_${level.name}")
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            ToyBlockCard(
                containerColor = SoftGreenSurface,
                borderColor = ToyGreen.copy(alpha = 0.55f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = ToyOrange)
                    Text(
                        text = tr("Lộ trình & Chế độ đề xuất", "Recommended Method & Mode"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = InkPrimary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ToyGreen
                ) {
                    Text(
                        text = tr(mode.viTitle, mode.enTitle),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = state.targetLevel.recommendedMethod(lang),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = InkPrimary
                )
                Text(
                    text = tr(mode.viBehavior, mode.enBehavior),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
        }
    }
}

@Composable
fun LevelOptionCard(
    level: RubikLevel,
    lang: AppLanguage,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) accentColor.copy(alpha = 0.1f) else Color.White,
        border = BorderStroke(2.dp, if (selected) accentColor else OutlineSoft)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = level.title(lang),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) accentColor else InkPrimary
                )
                Text(
                    text = level.description(lang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSecondary
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accentColor
                )
            }
        }
    }
}

@Composable
private fun LanguageChipButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) RubikBlue else Color.White,
        border = BorderStroke(1.5.dp, if (selected) RubikBlue else OutlineSoft)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else InkSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}
