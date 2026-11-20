package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solve_sessions")
data class SolveSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpochMs: Long,
    val dateKey: String,
    val sourceType: String, // "CAMERA_VIDEO" or "PRACTICE_SCRAMBLE"
    val scrambleNotation: String,
    val totalDurationMs: Long,
    val crossMs: Long,
    val f2lMs: Long,
    val ollMs: Long,
    val pllMs: Long,
    val crossMoveCount: Int,
    val totalMoveCount: Int,
    val solutionNotation: String,
    val isPersonalBest: Boolean = false
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val dateKey: String,
    val targetSolves: Int = 5,
    val completedSolves: Int = 0,
    val bestTimeMs: Long? = null,
    val totalTimeMs: Long = 0L
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val playStepDelayMs: Long = 1000L,
    val languageCode: String = "vi",
    val soundEnabled: Boolean = true
)

enum class RubikLevel(
    val viTitle: String,
    val enTitle: String,
    val viDescription: String,
    val enDescription: String,
    val viRecommendedMethod: String,
    val enRecommendedMethod: String
) {
    BEGINNER(
        viTitle = "Chưa biết giải",
        enTitle = "Beginner",
        viDescription = "Mới bắt đầu, cần học cách giải cơ bản",
        enDescription = "Just starting out, learn the basics",
        viRecommendedMethod = "Layer-by-Layer (LBL cơ bản)",
        enRecommendedMethod = "Basic Layer-by-Layer (LBL)"
    ),
    SUB_60(
        viTitle = "Dưới 60 giây (Sub-60)",
        enTitle = "Under 60s (Sub-60)",
        viDescription = "Đã biết giải cơ bản (LBL)",
        enDescription = "Knows basic LBL solving",
        viRecommendedMethod = "Cải thiện Lookahead cơ bản & Fingertricks",
        enRecommendedMethod = "Basic Lookahead & Fingertricks"
    ),
    SUB_30(
        viTitle = "Dưới 30 giây (Sub-30)",
        enTitle = "Under 30s (Sub-30)",
        viDescription = "Đang làm quen với CFOP (F2L, OLL, PLL)",
        enDescription = "Getting familiar with CFOP (F2L, OLL, PLL)",
        viRecommendedMethod = "F2L trực quan & 2-Look OLL/PLL",
        enRecommendedMethod = "Intuitive F2L & 2-Look OLL/PLL"
    ),
    SUB_15(
        viTitle = "Dưới 15 giây (Sub-15)",
        enTitle = "Under 15s (Sub-15)",
        viDescription = "Thành thạo Full CFOP & Lookahead",
        enDescription = "Fluent in Full CFOP & Lookahead",
        viRecommendedMethod = "Full OLL/PLL & tối ưu hóa F2L",
        enRecommendedMethod = "Full OLL/PLL & F2L Optimization"
    ),
    SUB_10(
        viTitle = "Dưới 10 giây (Sub-10)",
        enTitle = "Under 10s (Sub-10)",
        viDescription = "Tối ưu hóa Fingertricks & X-Cross",
        enDescription = "Optimizing Fingertricks & X-Cross",
        viRecommendedMethod = "X-Cross, COLL & tốc độ TPS cao",
        enRecommendedMethod = "X-Cross, COLL & High TPS"
    ),
    SUB_6(
        viTitle = "Dưới 6 giây (Sub-6)",
        enTitle = "Under 6s (Sub-6)",
        viDescription = "Cấp độ thi đấu chuyên nghiệp",
        enDescription = "Pro competition speedcuber",
        viRecommendedMethod = "ZBLL, VLS & dự đoán toàn bộ Cross + F2L 1",
        enRecommendedMethod = "ZBLL, VLS & Full Cross + F2L-1 Inspection"
    );

    fun title(lang: AppLanguage): String = tr(lang, viTitle, enTitle)
    fun description(lang: AppLanguage): String = tr(lang, viDescription, enDescription)
    fun recommendedMethod(lang: AppLanguage): String = tr(lang, viRecommendedMethod, enRecommendedMethod)

    fun availableTargets(): List<RubikLevel> {
        val higher = entries.filter { it.ordinal > this.ordinal }
        return higher.ifEmpty { listOf(SUB_6) }
    }

    companion object {
        fun fromName(name: String?): RubikLevel =
            entries.firstOrNull { it.name == name } ?: BEGINNER
    }
}

enum class CubieTrainingMode(
    val viTitle: String,
    val enTitle: String,
    val viBehavior: String,
    val enBehavior: String,
    val defaultShowArrows: Boolean
) {
    GUIDED_MODE(
        viTitle = "Guided Mode (Học giải)",
        enTitle = "Guided Mode",
        viBehavior = "Bật mũi tên 3D chỉ hướng từng bước; ưu tiên hiểu công thức trước khi bấm giờ.",
        enBehavior = "3D directional arrows active step-by-step; focus on mastering moves without pressure.",
        defaultShowArrows = true
    ),
    PRACTICE_CFOP(
        viTitle = "Practice CFOP",
        enTitle = "Practice CFOP",
        viBehavior = "Luyện F2L trực quan & 2-Look OLL/PLL; tự động xáo trộn theo từng nhóm thế cờ.",
        enBehavior = "Intuitive F2L & 2-Look OLL/PLL training; algorithm-grouped drills.",
        defaultShowArrows = true
    ),
    SPEEDCUBING_WCA(
        viTitle = "Speedcubing WCA",
        enTitle = "Speedcubing WCA",
        viBehavior = "Scramble 20 bước chuẩn WCA; thử thách Full OLL/PLL & đồng hồ thi đấu toàn màn hình.",
        enBehavior = "20-move WCA Scrambles, Full OLL/PLL drills & fullscreen WCA timer.",
        defaultShowArrows = false
    ),
    PRO_ANALYTICS(
        viTitle = "Pro Analytics",
        enTitle = "Pro Analytics",
        viBehavior = "Full Speedcubing WCA + chỉ số TPS (Turns/s) & phân tích từng pha Cross, F2L, OLL, PLL.",
        enBehavior = "Full WCA Speedcubing + TPS analytics & Cross/F2L/OLL/PLL split tracking.",
        defaultShowArrows = false
    )
}

data class UserProfile(
    val name: String = "",
    val age: Int = 15,
    val currentLevel: RubikLevel = RubikLevel.BEGINNER,
    val targetLevel: RubikLevel = RubikLevel.SUB_60
) {
    fun trainingMode(): CubieTrainingMode = when {
        currentLevel == RubikLevel.BEGINNER && targetLevel == RubikLevel.SUB_60 -> CubieTrainingMode.GUIDED_MODE
        currentLevel.ordinal <= RubikLevel.SUB_60.ordinal && targetLevel.ordinal <= RubikLevel.SUB_30.ordinal ->
            CubieTrainingMode.PRACTICE_CFOP
        currentLevel.ordinal <= RubikLevel.SUB_30.ordinal && targetLevel == RubikLevel.SUB_15 ->
            CubieTrainingMode.SPEEDCUBING_WCA
        else -> CubieTrainingMode.PRO_ANALYTICS
    }
}

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val currentLevelName: String,
    val targetLevelName: String,
    val updatedAtEpochMs: Long
)

enum class WcaPenalty {
    NONE,
    PLUS_TWO,
    DNF;

    companion object {
        fun fromName(name: String?): WcaPenalty =
            entries.firstOrNull { it.name == name } ?: NONE
    }
}

@Entity(tableName = "wca_solves")
data class WcaSolveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timeMillis: Long,
    val penaltyName: String = WcaPenalty.NONE.name,
    val scramble: String,
    val timestamp: Long
)

data class WcaSolveRecord(
    val id: Long = 0L,
    val timeMillis: Long,
    val penalty: WcaPenalty = WcaPenalty.NONE,
    val scramble: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun effectiveTimeMs(): Long? = when (penalty) {
        WcaPenalty.DNF -> null
        WcaPenalty.PLUS_TWO -> timeMillis + 2000L
        WcaPenalty.NONE -> timeMillis
    }

    fun getFormattedTime(): String = when (penalty) {
        WcaPenalty.DNF -> "DNF"
        WcaPenalty.PLUS_TWO -> formatWcaMillis(timeMillis + 2000L) + "+"
        WcaPenalty.NONE -> formatWcaMillis(timeMillis)
    }

    companion object {
        fun formatWcaMillis(ms: Long): String {
            val safe = ms.coerceAtLeast(0L)
            val minutes = safe / 60000L
            val seconds = (safe % 60000L) / 1000L
            val centis = (safe % 1000L) / 10L
            return if (minutes > 0) {
                java.lang.String.format(java.util.Locale.US, "%d:%02d.%02d", minutes, seconds, centis)
            } else {
                java.lang.String.format(java.util.Locale.US, "%d.%02d", seconds, centis)
            }
        }
    }
}
