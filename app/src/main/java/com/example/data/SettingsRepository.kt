package com.example.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val code: String, val label: String) {
    VI("vi", "Tiếng Việt"),
    EN("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: VI
    }
}

data class AppSettings(
    val playStepDelayMs: Long = DEFAULT_PLAY_STEP_DELAY_MS,
    val language: AppLanguage = AppLanguage.VI
) {
    companion object {
        const val DEFAULT_PLAY_STEP_DELAY_MS = 1000L
        val PLAY_SPEED_OPTIONS_MS = listOf(500L, 1000L, 1500L, 2000L, 3000L)
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.VI }

@Composable
fun tr(vi: String, en: String): String =
    if (LocalAppLanguage.current == AppLanguage.VI) vi else en

fun tr(lang: AppLanguage, vi: String, en: String): String =
    if (lang == AppLanguage.VI) vi else en

class SettingsRepository(private val cubeDao: CubeDao) {

    val settings: Flow<AppSettings> = cubeDao.observeSettings().map { entity ->
        if (entity == null) {
            AppSettings()
        } else {
            AppSettings(
                playStepDelayMs = entity.playStepDelayMs.coerceIn(300L, 5000L),
                language = AppLanguage.fromCode(entity.languageCode)
            )
        }
    }

    suspend fun setPlayStepDelayMs(delayMs: Long) {
        val clamped = delayMs.coerceIn(300L, 5000L)
        val existing = cubeDao.getSettingsOnce() ?: AppSettingsEntity()
        cubeDao.upsertSettings(existing.copy(id = 1, playStepDelayMs = clamped))
    }

    suspend fun setLanguage(language: AppLanguage) {
        val existing = cubeDao.getSettingsOnce() ?: AppSettingsEntity()
        cubeDao.upsertSettings(existing.copy(id = 1, languageCode = language.code))
    }
}
