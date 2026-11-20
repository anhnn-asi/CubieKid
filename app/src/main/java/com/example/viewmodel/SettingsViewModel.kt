package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.AppSettings
import com.example.data.ProfileValidationError
import com.example.data.ProfileValidationException
import com.example.data.RubikLevel
import com.example.data.SettingsRepository
import com.example.data.UserProfile
import com.example.data.UserProfileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val userProfileRepository: UserProfileRepository
) : ViewModel() {

    val uiState: StateFlow<AppSettings> = settingsRepository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppSettings()
    )

    val userProfile: StateFlow<UserProfile?> = userProfileRepository.userProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun setPlayStepDelayMs(delayMs: Long) {
        viewModelScope.launch {
            settingsRepository.setPlayStepDelayMs(delayMs)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSoundEnabled(enabled)
        }
    }

    fun updateUserProfile(
        name: String,
        age: Int,
        currentLevel: RubikLevel,
        targetLevel: RubikLevel,
        onResult: (ProfileValidationError?) -> Unit
    ) {
        viewModelScope.launch {
            val res = userProfileRepository.saveProfile(
                UserProfile(
                    name = name,
                    age = age,
                    currentLevel = currentLevel,
                    targetLevel = targetLevel
                )
            )
            res.onSuccess {
                onResult(null)
            }.onFailure { err ->
                onResult((err as? ProfileValidationException)?.error ?: ProfileValidationError.NameTooShort)
            }
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val userProfileRepository: UserProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, userProfileRepository) as T
        }
    }
}
