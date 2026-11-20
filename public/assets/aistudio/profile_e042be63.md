# User Profile & Onboarding Specification (Android Kotlin)

Tài liệu đặc tả kỹ thuật cho luồng thu thập thông tin người dùng (Onboarding), lưu trữ dữ liệu cục bộ bằng Jetpack DataStore và cá nhân hóa trải nghiệm khối Rubik 3D ảo.

---

## 1. Mục tiêu tính năng
- Thu thập thông tin cơ bản (Tên, Tuổi) và đánh giá trình độ giải Rubik hiện tại của người dùng ngay lần đầu mở ứng dụng.
- Xác định mục tiêu luyện tập tiếp theo của người dùng để tự động điều chỉnh giao diện và chế độ hoạt động của khối Rubik 3D (`RubikView`).
- Lưu trữ cục bộ an toàn, bất đồng bộ bằng `Jetpack DataStore Preferences`.

---

## 2. Cấu trúc dữ liệu (Data Models)

```kotlin
package com.example.virtualrubik.model

enum class RubikLevel(
    val title: String,
    val description: String,
    val recommendedMethod: String
) {
    BEGINNER(
        title = "Chưa biết giải",
        description = "Mới bắt đầu, cần học cách giải cơ bản",
        recommendedMethod = "Layer-by-Layer (LBL cơ bản)"
    ),
    SUB_60(
        title = "Dưới 60 giây (Sub-60)",
        description = "Đã biết giải cơ bản (LBL)",
        recommendedMethod = "Cải thiện Lookahead cơ bản & Fingertricks"
    ),
    SUB_30(
        title = "Dưới 30 giây (Sub-30)",
        description = "Đang làm quen với CFOP (F2L, OLL, PLL)",
        recommendedMethod = "F2L trực quan & 2-Look OLL/PLL"
    ),
    SUB_15(
        title = "Dưới 15 giây (Sub-15)",
        description = "Thành thạo Full CFOP & Lookahead",
        recommendedMethod = "Full OLL/PLL & tối ưu hóa F2L"
    ),
    SUB_10(
        title = "Dưới 10 giây (Sub-10)",
        description = "Tối ưu hóa Fingertricks & X-Cross",
        recommendedMethod = "X-Cross, COLL & tốc độ TPS cao"
    ),
    SUB_6(
        title = "Dưới 6 giây (Sub-6)",
        description = "Cấp độ thi đấu chuyên nghiệp",
        recommendedMethod = "ZBLL, VLS & dự đoán toàn bộ Cross + F2L 1"
    )
}

data class UserProfile(
    val name: String = "",
    val age: Int = 0,
    val currentLevel: RubikLevel = RubikLevel.BEGINNER,
    val targetLevel: RubikLevel = RubikLevel.SUB_60
)
```

---

## 3. Luồng UX/UI Onboarding (3 Bước)
Dev triển khai màn hình Onboarding dạng Horizontal Pager (hoặc Navigation Compose 3 bước) chỉ hiển thị khi `userProfileFlow` trả về `null`:

1. **Bước 1: Thông tin cá nhân**
   - Input `name`: TextField nhập tên hoặc nickname (bắt buộc, tối thiểu 2 ký tự).
   - Input `age`: Number picker hoặc TextField số tuổi (mặc định gợi ý 15, dùng để điều chỉnh độ chi tiết của hướng dẫn).
2. **Bước 2: Trình độ hiện tại (`currentLevel`)**
   - Hiển thị danh sách thẻ chọn (Single-choice cards) tương ứng với các giá trị trong `enum class RubikLevel`.
3. **Bước 3: Mục tiêu & Lộ trình đề xuất (`targetLevel`)**
   - Tự động lọc và chỉ hiển thị các cấp độ **cao hơn** `currentLevel` đã chọn ở Bước 2 (ví dụ: chọn `SUB_60` ở Bước 2 thì Bước 3 hiển thị `SUB_30`, `SUB_15`, `SUB_10`, `SUB_6`).
   - Hiển thị thẻ tóm tắt `recommendedMethod` tương ứng với mục tiêu để tạo động lực cho người dùng trước khi nhấn nút **"Bắt đầu"**.

---

## 4. Lưu trữ dữ liệu với Jetpack DataStore

### Gradle Dependency (`app/build.gradle.kts`)
```kotlin
dependencies {
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
```

### Repository Implementation (`UserPreferencesRepository.kt`)
```kotlin
package com.example.virtualrubik.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.virtualrubik.model.RubikLevel
import com.example.virtualrubik.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_rubik_prefs")

class UserPreferencesRepository(private val context: Context) {

    companion object {
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_AGE = intPreferencesKey("user_age")
        val CURRENT_LEVEL = stringPreferencesKey("current_level")
        val TARGET_LEVEL = stringPreferencesKey("target_level")
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[USER_NAME] = profile.name.trim()
            prefs[USER_AGE] = profile.age
            prefs[CURRENT_LEVEL] = profile.currentLevel.name
            prefs[TARGET_LEVEL] = profile.targetLevel.name
        }
    }

    val userProfileFlow: Flow<UserProfile?> = context.dataStore.data.map { prefs ->
        val name = prefs[USER_NAME] ?: return@map null
        val age = prefs[USER_AGE] ?: 15
        val current = runCatching {
            RubikLevel.valueOf(prefs[CURRENT_LEVEL] ?: RubikLevel.BEGINNER.name)
        }.getOrDefault(RubikLevel.BEGINNER)
        
        val target = runCatching {
            RubikLevel.valueOf(prefs[TARGET_LEVEL] ?: RubikLevel.SUB_60.name)
        }.getOrDefault(RubikLevel.SUB_60)

        UserProfile(
            name = name,
            age = age,
            currentLevel = current,
            targetLevel = target
        )
    }

    suspend fun clearProfile() {
        context.dataStore.edit { it.clear() }
    }
}
```

---

## 5. Quy tắc ánh xạ dữ liệu sang khối Rubik 3D (`RubikView`)
Sau khi lưu `UserProfile`, màn hình chính đọc dữ liệu và cấu hình chế độ hiển thị cho khối Rubik 3D theo bảng sau:

| Trình độ / Mục tiêu | Chế độ mặc định | Hành vi trên khối Rubik 3D (`RubikView`) |
| :--- | :--- | :--- |
| `BEGINNER` -> `SUB_60` | **Guided Mode** (Học giải) | Bật mũi tên 3D chỉ hướng xoay từng bước theo công thức LBL; tắt bộ đếm giờ áp lực. |
| `SUB_60` -> `SUB_30` | **Practice CFOP** | Tích hợp công cụ luyện F2L/OLL/PLL; tự động xáo trộn (Scramble) theo từng nhóm công thức. |
| `SUB_30` -> `SUB_15` | **Speedcubing WCA** | Tự động Scramble 20 bước chuẩn WCA; ẩn toàn bộ mũi tên gợi ý; kích hoạt Timer ngay khi chạm xoay bước đầu. |
| `SUB_15` -> `SUB_10` / `SUB_6` | **Pro Analytics** | Kích hoạt đầy đủ tính năng của Speedcubing WCA + đo chỉ số TPS (Turns Per Second) và chia thời gian từng giai đoạn (Cross, F2L, OLL, PLL). |

---

## 6. Yêu cầu bổ sung cho Dev
- **Màn hình Cài đặt / Hồ sơ (Profile Settings):** Cho phép người dùng xem lại thông tin, cập nhật `currentLevel` / `targetLevel` khi họ đã đạt được mục tiêu cũ.
- **Validation:** Không cho phép lưu nếu `name` rỗng hoặc `targetLevel.ordinal <= currentLevel.ordinal` (trừ trường hợp đã ở mức cao nhất `SUB_6`).
