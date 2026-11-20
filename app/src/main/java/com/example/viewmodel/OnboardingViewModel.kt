package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ProfileValidationError
import com.example.data.ProfileValidationException
import com.example.data.RubikLevel
import com.example.data.UserProfile
import com.example.data.UserProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UserProfileLoadState {
    data object Loading : UserProfileLoadState
    data object NeedsOnboarding : UserProfileLoadState
    data class Ready(val profile: UserProfile) : UserProfileLoadState
}

data class OnboardingUiState(
    val currentStep: Int = 0, // 0 = Personal Info, 1 = Current Level, 2 = Target Level
    val name: String = "",
    val age: Int = 15,
    val currentLevel: RubikLevel = RubikLevel.BEGINNER,
    val targetLevel: RubikLevel = RubikLevel.SUB_60,
    val validationError: ProfileValidationError? = null,
    val isSaving: Boolean = false
) {
    val availableTargetLevels: List<RubikLevel>
        get() = currentLevel.availableTargets()
}

class OnboardingViewModel(
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val profileLoadState: StateFlow<UserProfileLoadState> = userProfileRepository.userProfile
        .map { profile ->
            if (profile == null) UserProfileLoadState.NeedsOnboarding
            else UserProfileLoadState.Ready(profile)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserProfileLoadState.Loading
        )

    private val _onboardingState = MutableStateFlow(OnboardingUiState())
    val onboardingState: StateFlow<OnboardingUiState> = _onboardingState.asStateFlow()

    fun updateName(newName: String) {
        _onboardingState.update { it.copy(name = newName, validationError = null) }
    }

    fun updateAge(newAge: Int) {
        _onboardingState.update {
            it.copy(age = newAge.coerceIn(4, 99), validationError = null)
        }
    }

    fun selectCurrentLevel(level: RubikLevel) {
        _onboardingState.update { state ->
            val targets = level.availableTargets()
            val adjustedTarget = if (state.targetLevel in targets) state.targetLevel else targets.first()
            state.copy(
                currentLevel = level,
                targetLevel = adjustedTarget,
                validationError = null
            )
        }
    }

    fun selectTargetLevel(level: RubikLevel) {
        _onboardingState.update { it.copy(targetLevel = level, validationError = null) }
    }

    fun nextStep() {
        val state = _onboardingState.value
        if (state.currentStep == 0) {
            if (state.name.trim().length < 2) {
                _onboardingState.update { it.copy(validationError = ProfileValidationError.NameTooShort) }
                return
            }
            if (state.age !in 3..120) {
                _onboardingState.update { it.copy(validationError = ProfileValidationError.InvalidAge) }
                return
            }
        }
        if (state.currentStep < 2) {
            _onboardingState.update { it.copy(currentStep = it.currentStep + 1, validationError = null) }
        }
    }

    fun previousStep() {
        _onboardingState.update {
            it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0), validationError = null)
        }
    }

    fun completeOnboarding() {
        val state = _onboardingState.value
        val draft = UserProfile(
            name = state.name,
            age = state.age,
            currentLevel = state.currentLevel,
            targetLevel = state.targetLevel
        )
        val validationError = userProfileRepository.validate(draft)
        if (validationError != null) {
            _onboardingState.update { it.copy(validationError = validationError) }
            return
        }
        viewModelScope.launch {
            _onboardingState.update { it.copy(isSaving = true) }
            val result = userProfileRepository.saveProfile(draft)
            result.onFailure { throwable ->
                val err = (throwable as? ProfileValidationException)?.error
                _onboardingState.update { it.copy(isSaving = false, validationError = err) }
            }.onSuccess {
                _onboardingState.update { it.copy(isSaving = false) }
            }
        }
    }

    class Factory(
        private val userProfileRepository: UserProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return OnboardingViewModel(userProfileRepository) as T
        }
    }
}
