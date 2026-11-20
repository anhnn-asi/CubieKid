# Đặc tả Kỹ thuật: Màn hình Luyện tập Công thức (Algorithm Trainer)

Tài liệu này mô tả kiến trúc, luồng hoạt động và mã nguồn Kotlin cho màn hình **Algorithm Trainer** trên ứng dụng Virtual Rubik 3D Android. Màn hình cho phép người dùng chọn từng thế OLL/PLL từ `cfop_algorithms.json`, hệ thống tự động xáo trộn khối 3D về đúng trạng thái của công thức đó để người dùng thực hành.

---

## 1. Kiến trúc & Cơ chế hoạt động

1. **Nguồn dữ liệu:** Đọc trực tiếp từ tệp `assets/cfop_algorithms.json` và lọc các công thức phù hợp với `targetLevel` trong `UserProfile`.
2. **Thiết lập thế cờ (Inverse Setup):**
   * Khi người dùng chọn một công thức (ví dụ *Sune*: `R U R' U R U2 R'`), hệ thống reset khối Rubik 3D về trạng thái hoàn chỉnh (Solved).
   * Thực thi chuỗi `setup_moves` (`R U2 R' U' R U' R'`) ngay lập tức (không animation) để đưa khối 3D vào đúng thế OLL/PLL cần luyện.
   * *Tùy chọn nâng cao (AUF - Adjust U Face):* Thêm ngẫu nhiên `U`, `U'`, hoặc `U2` sau `setup_moves` để luyện kỹ năng nhận diện góc nhìn (Recognition) như thi đấu thật.
3. **Kiểm tra hoàn thành (State Validation):**
   * Sau mỗi thao tác vuốt xoay trên `RubikView`, hệ thống kiểm tra trạng thái mặt trên (`U` - OLL) hoặc toàn bộ 6 mặt (`PLL`).
   * Ngay khi giải xong, đồng hồ dừng lại, lưu thời gian thực hành (Execution Time) và tự động xáo trộn lại sau 1 giây để lặp lại (Drill Mode).

---

## 2. Bố cục Giao diện (UI Wireframe)

```text
+-------------------------------------------------------+
|  [< Back]   OLL 27 - Sune (Con cá hướng phải)  [Hint] |  <- Top Bar & Nút bật/tắt gợi ý
+-------------------------------------------------------+
|                                                       |
|           [ Khu vực Khối Rubik 3D Tương tác ]          |  <- Chiếm 60% chiều cao màn hình
|          (Tự động setup đúng thế cờ cần luyện)        |
|                                                       |
+-------------------------------------------------------+
|   Công thức:  R  U  R'  U  R  U2  R'                  |  <- Ẩn mặc định, hiện khi bấm [Hint]
+-------------------------------------------------------+
|   Thời gian: 1.42s  |  Kỷ lục: 0.98s  |  Số lần: 14   |  <- Thống kê phiên luyện tập
+-------------------------------------------------------+
|   [ Chuyển thế khác ]         [ Xáo trộn lại (Next) ] |  <- Bottom Controls
+-------------------------------------------------------+
```

---

## 3. Mã nguồn Kotlin cốt lõi (`AlgorithmTrainerController.kt`)

```kotlin
package com.example.virtualrubik.trainer

import android.os.SystemClock
import com.example.virtualrubik.model.AlgorithmItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class TrainerMode {
    LEARN, // Hiển thị sẵn công thức & highlight bước xoay tiếp theo trên khối 3D
    DRILL  // Ẩn công thức, tự động bấm giờ và lặp lại liên tục để luyện phản xạ
}

data class TrainerUiState(
    val currentAlgorithm: AlgorithmItem? = null,
    val mode: TrainerMode = TrainerMode.LEARN,
    val isHintVisible: Boolean = true,
    val isRandomAufEnabled: Boolean = false,
    val elapsedMillis: Long = 0L,
    val bestMillis: Long? = null,
    val completedCount: Int = 0,
    val isSolved: Boolean = false
)

class AlgorithmTrainerController(
    private val applyInstantMoves: (String) -> Unit,
    private val resetCubeToSolved: () -> Unit,
    private val checkCubeSolvedForCategory: (String) -> Boolean
) {
    private val _uiState = MutableStateFlow(TrainerUiState())
    val uiState: StateFlow<TrainerUiState> = _uiState.asStateFlow()

    private var startTimeMillis: Long = 0L
    private var isTimerRunning: Boolean = false

    /**
     * Nạp một công thức mới và đưa khối Rubik 3D về đúng trạng thái thế cờ
     */
    fun loadAlgorithm(algorithm: AlgorithmItem, mode: TrainerMode = TrainerMode.LEARN) {
        isTimerRunning = false
        resetCubeToSolved()

        // Thực thi chuỗi đảo ngược để tạo thế cờ
        applyInstantMoves(algorithm.setupMoves)

        // Nếu bật Random AUF, xoay ngẫu nhiên mặt U để luyện nhận diện
        if (_uiState.value.isRandomAufEnabled) {
            val aufMoves = listOf("", "U", "U'", "U2")
            val randomAuf = aufMoves.random()
            if (randomAuf.isNotEmpty()) {
                applyInstantMoves(randomAuf)
            }
        }

        _uiState.update {
            it.copy(
                currentAlgorithm = algorithm,
                mode = mode,
                isHintVisible = (mode == TrainerMode.LEARN),
                elapsedMillis = 0L,
                isSolved = false
            )
        }
    }

    /**
     * Gọi hàm này mỗi khi người dùng thực hiện một bước xoay trên RubikView
     */
    fun onUserMovePerformed(categoryId: String) {
        val currentState = _uiState.value
        if (currentState.isSolved || currentState.currentAlgorithm == null) return

        // Bắt đầu tính giờ ngay ở bước xoay đầu tiên của người dùng
        if (!isTimerRunning) {
            startTimeMillis = SystemClock.elapsedRealtime()
            isTimerRunning = true
        }

        // Kiểm tra xem đã giải xong thế OLL hoặc PLL chưa
        if (checkCubeSolvedForCategory(categoryId)) {
            isTimerRunning = false
            val finalTime = SystemClock.elapsedRealtime() - startTimeMillis
            val currentBest = currentState.bestMillis
            val newBest = if (currentBest == null || finalTime < currentBest) finalTime else currentBest

            _uiState.update {
                it.copy(
                    elapsedMillis = finalTime,
                    bestMillis = newBest,
                    completedCount = it.completedCount + 1,
                    isSolved = true
                )
            }
        }
    }

    /**
     * Chuyển đổi ẩn/hiện công thức gợi ý
     */
    fun toggleHint() {
        _uiState.update { it.copy(isHintVisible = !it.isHintVisible) }
    }

    /**
     * Bật/tắt chế độ xoay ngẫu nhiên tầng trên (Random AUF)
     */
    fun toggleRandomAuf() {
        _uiState.update { it.copy(isRandomAufEnabled = !it.isRandomAufEnabled) }
        _uiState.value.currentAlgorithm?.let { loadAlgorithm(it, _uiState.value.mode) }
    }
}
```

---

## 4. Hướng dẫn tích hợp cho Dev

1. **Parser chuỗi bước xoay (`String -> Move`):**
   * Tách chuỗi `setup_moves` theo dấu cách (`split(" ")`).
   * Hỗ trợ đầy đủ ký hiệu chuẩn: `R, L, U, D, F, B` (xoay 1 tầng), `r, l, u, d, f, b` (xoay 2 tầng - Wide moves), `M, E, S` (xoay tầng giữa), `x, y, z` (xoay cả khối), kèm hậu tố `'` (ngược chiều kim đồng hồ) và `2` (xoay 180 độ).
2. **Điều kiện hoàn thành (`checkCubeSolvedForCategory`):**
   * Với nhóm `2_look_oll` hoặc `full_oll`: Chỉ cần kiểm tra 9 ô mặt trên (`U`) đều là màu Vàng (`YELLOW`), không cần các cạnh xung quanh khớp màu.
   * Với nhóm `2_look_pll` hoặc `full_pll`: Kiểm tra toàn bộ 6 mặt của khối Rubik đã đồng màu.
3. **Chế độ Học từng bước (Learn Mode Step-by-Step):**
   * Tách chuỗi `moves` thành mảng các bước. Highlight chữ cái của bước hiện tại trên thanh công thức và vẽ mũi tên 3D chỉ hướng xoay tương ứng lên `RubikView` cho đến khi người dùng vuốt đúng hướng.
