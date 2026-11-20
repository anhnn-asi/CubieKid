package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

sealed interface ProfileValidationError {
    data object NameTooShort : ProfileValidationError
    data object InvalidAge : ProfileValidationError
    data object InvalidTargetLevel : ProfileValidationError
}

class ProfileValidationException(val error: ProfileValidationError) : IllegalArgumentException()

class UserProfileRepository(private val cubeDao: CubeDao) {

    val userProfile: Flow<UserProfile?> = cubeDao.observeUserProfile().map { entity ->
        entity?.let {
            val current = RubikLevel.fromName(it.currentLevelName)
            val parsedTarget = RubikLevel.fromName(it.targetLevelName)
            val safeTarget = if (current == RubikLevel.SUB_6) {
                RubikLevel.SUB_6
            } else if (parsedTarget.ordinal <= current.ordinal) {
                current.availableTargets().first()
            } else {
                parsedTarget
            }
            UserProfile(
                name = it.name,
                age = it.age,
                currentLevel = current,
                targetLevel = safeTarget
            )
        }
    }

    fun validate(profile: UserProfile): ProfileValidationError? {
        val trimmed = profile.name.trim()
        if (trimmed.length < 2) return ProfileValidationError.NameTooShort
        if (profile.age !in 3..120) return ProfileValidationError.InvalidAge
        if (profile.currentLevel != RubikLevel.SUB_6 &&
            profile.targetLevel.ordinal <= profile.currentLevel.ordinal
        ) {
            return ProfileValidationError.InvalidTargetLevel
        }
        return null
    }

    suspend fun saveProfile(profile: UserProfile): Result<UserProfile> {
        val error = validate(profile)
        if (error != null) {
            return Result.failure(ProfileValidationException(error))
        }
        val clean = profile.copy(name = profile.name.trim())
        cubeDao.upsertUserProfile(
            UserProfileEntity(
                id = 1,
                name = clean.name,
                age = clean.age,
                currentLevelName = clean.currentLevel.name,
                targetLevelName = clean.targetLevel.name,
                updatedAtEpochMs = System.currentTimeMillis()
            )
        )
        return Result.success(clean)
    }

    suspend fun clearProfile() {
        cubeDao.clearUserProfile()
    }
}
