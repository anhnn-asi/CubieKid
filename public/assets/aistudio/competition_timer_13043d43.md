# Đặc tả Kỹ thuật: Màn hình Timer Thi đấu Chuẩn WCA (Competition Timer)

Tài liệu này mô tả chi tiết luồng hoạt động, máy trạng thái (State Machine), giao diện toàn màn hình (Fullscreen) và logic lưu trữ kết quả cho màn hình bấm giờ thi đấu Rubik chuẩn WCA trên Android (Kotlin).

---

## 1. Yêu cầu Giao diện & Trải nghiệm (UI/UX)

* **Chế độ hiển thị (Fullscreen Immersive):** Ẩn thanh trạng thái (Status Bar) và thanh điều hướng (Navigation Bar) của hệ thống để tránh chạm nhầm khi đang thi đấu. Khóa màn hình luôn sáng (`FLAG_KEEP_SCREEN_ON`).
* **Bố cục tối giản (Minimalist Layout):**
  * **Góc trên bên trái:** Một nút **Exit** kích thước nhỏ (icon `X` hoặc mũi tên quay lại) có độ mờ nhẹ (alpha ~0.5) để thoát về màn hình trước đó. *Lưu ý: Ẩn hoặc khóa nút này trong lúc đồng hồ đang chạy giải để tránh thoát nhầm.*
  * **Phần trên màn hình:** Chuỗi ký hiệu xáo trộn (**Scramble**) chuẩn WCA 20 bước (ví dụ: `R U R' U' F2 D L2 B' ...`). Tự động ẩn đi khi bắt đầu bước vào giai đoạn giải.
  * **Trung tâm màn hình:** Đồng hồ đếm giờ kích thước lớn, sử dụng font chữ đơn cách (`FontFamily.Monospace` hoặc `tabular-nums`) để các con số không bị rung lắc khi nhảy mili-giây.
* **Vùng cảm ứng (Touch Area):** Toàn bộ diện tích màn hình (trừ nút Exit) là vùng nhận thao tác chạm (`Tap`).

---

## 2. Máy trạng thái (Timer State Machine)

Quy trình thi đấu gồm **4 trạng thái chính** chuyển đổi tuần tự theo thao tác chạm (Tap) của người dùng:

| Trạng thái (`TimerState`) | Hiển thị trên màn hình | Màu chữ đồng hồ | Hành động chuyển trạng thái |
| :--- | :--- | :--- | :--- |
| **`IDLE` (Chờ sẵn sàng)** | Hiện chuỗi **Scramble** + `0.00` (hoặc kết quả lần trước) + nút Exit. | Trắng / Xám nhạt | **Tap màn hình** $ightarrow$ Chuyển sang `INSPECTION` (Bắt đầu đếm ngược 15s). |
| **`INSPECTION` (Quan sát 15s)** | Đếm ngược từ **`15`** về **`0`**. Hiện cảnh báo âm thanh/rung tại mốc **8s** và **12s** (chuẩn WCA). | Vàng / Cam (Đỏ nếu quá 15s) | **Tap màn hình** $ightarrow$ Dừng đếm ngược, chuyển sang `SOLVING` (Bắt đầu tính giờ giải). |
| **`SOLVING` (Đang giải)** | Đồng hồ đếm tăng dần chính xác từng mili-giây (`mm:ss.SS`). **Ẩn Scramble và nút Exit.** | Xanh lá | **Tap bất kỳ đâu trên màn hình** $ightarrow$ Dừng đồng hồ, chuyển sang `FINISHED`. |
| **`FINISHED` (Hoàn thành)** | Hiển thị thời gian giải cuối cùng + cộng phạt (nếu có) + sinh **Scramble mới** + hiện lại nút Exit. | Trắng / Xanh dương | Tự động lưu kết quả vào DB. **Tap màn hình** $ightarrow$ Bắt đầu lượt `INSPECTION` mới. |

### Quy tắc xử lý phạt thời gian (WCA Inspection Penalty)
* **Từ `0.00s` đến `15.00s`:** Hợp lệ, không phạt.
* **Từ `15.01s` đến `17.00s`:** Phạt **`+2` giây** vào thành tích giải cuối cùng (hiển thị dạng `14.52+`).
* **Quá `17.00s`:** Tính là **`DNF`** (Did Not Finish - Không hoàn thành lượt giải).

---

## 3. Cấu trúc Dữ liệu Kết quả (`SolveRecord`)

Mỗi lần hoàn thành lượt giải (trạng thái `FINISHED`), hệ thống tự động lưu một bản ghi vào cơ sở dữ liệu cục bộ (Room Database hoặc DataStore/JSON):

```kotlin
enum class Penalty {
    NONE,
    PLUS_TWO,
    DNF
}

data class SolveRecord(
    val id: Long = System.currentTimeMillis(),
    val timeMillis: Long,           // Thời gian giải thực tế (mili-giây)
    val penalty: Penalty = Penalty.NONE,
    val scramble: String,           // Chuỗi scramble của lượt giải đó
    val timestamp: Long = System.currentTimeMillis()
) {
    // Thời gian hiển thị sau khi tính phạt
    fun getFormattedTime(): String {
        return when (penalty) {
            Penalty.DNF -> "DNF"
            Penalty.PLUS_TWO -> formatMillis(timeMillis + 2000) + "+"
            Penalty.NONE -> formatMillis(timeMillis)
        }
    }

    private fun formatMillis(ms: Long): String {
        val minutes = (ms / 60000)
        val seconds = (ms % 60000) / 1000
        val centis = (ms % 1000) / 10
        return if (minutes > 0) {
            String.format("%d:%02d.%02d", minutes, seconds, centis)
        } else {
            String.format("%d.%02d", seconds, centis)
        }
    }
}
```

---

## 4. Mã nguồn Logic Timer & Scramble (Kotlin)

Đoạn mã dưới đây cung cấp bộ sinh Scramble chuẩn WCA 20 bước (tránh lặp mặt liên tiếp như `R R'` hoặc lặp trục đối diện vô nghĩa như `R L R`) và bộ đếm thời gian độ chính xác cao dùng `SystemClock.elapsedRealtime()`:

```kotlin
import android.os.SystemClock
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TimerState {
    IDLE, INSPECTION, SOLVING, FINISHED
}

class CompetitionTimerManager(
    private val scope: CoroutineScope,
    private val onSaveResult: (SolveRecord) -> Unit
) {
    private val _state = MutableStateFlow(TimerState.IDLE)
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private val _displayText = MutableStateFlow("0.00")
    val displayText: StateFlow<String> = _displayText.asStateFlow()

    private val _scramble = MutableStateFlow(generateWcaScramble())
    val scramble: StateFlow<String> = _scramble.asStateFlow()

    private var timerJob: Job? = null
    private var startTimeMillis: Long = 0L
    private var currentPenalty: Penalty = Penalty.NONE

    // Xử lý sự kiện Tap toàn màn hình
    fun onScreenTap() {
        when (_state.value) {
            TimerState.IDLE, TimerState.FINISHED -> startInspection()
            TimerState.INSPECTION -> startSolving()
            TimerState.SOLVING -> stopSolving()
        }
    }

    // Bước 1: Bắt đầu 15 giây quan sát
    private fun startInspection() {
        timerJob?.cancel()
        _state.value = TimerState.INSPECTION
        currentPenalty = Penalty.NONE
        val inspectionStart = SystemClock.elapsedRealtime()

        timerJob = scope.launch {
            while (isActive) {
                val elapsedSec = (SystemClock.elapsedRealtime() - inspectionStart) / 1000f
                when {
                    elapsedSec <= 15f -> {
                        val remaining = kotlin.math.ceil(15f - elapsedSec).toInt()
                        _displayText.value = remaining.toString()
                    }
                    elapsedSec <= 17f -> {
                        currentPenalty = Penalty.PLUS_TWO
                        _displayText.value = "+2"
                    }
                    else -> {
                        currentPenalty = Penalty.DNF
                        _displayText.value = "DNF"
                        finishWithDnf()
                        break
                    }
                }
                delay(50L)
            }
        }
    }

    // Bước 2: Tap để bắt đầu giải
    private fun startSolving() {
        timerJob?.cancel()
        _state.value = TimerState.SOLVING
        startTimeMillis = SystemClock.elapsedRealtime()

        timerJob = scope.launch {
            while (isActive) {
                val elapsed = SystemClock.elapsedRealtime() - startTimeMillis
                _displayText.value = formatTime(elapsed)
                delay(16L) // Cập nhật ~60fps
            }
        }
    }

    // Bước 3: Tap để kết thúc và lưu kết quả
    private fun stopSolving() {
        timerJob?.cancel()
        val finalElapsed = SystemClock.elapsedRealtime() - startTimeMillis
        _state.value = TimerState.FINISHED

        val record = SolveRecord(
            timeMillis = finalElapsed,
            penalty = currentPenalty,
            scramble = _scramble.value
        )
        _displayText.value = record.getFormattedTime()
        onSaveResult(record)

        // Tạo scramble mới cho lượt tiếp theo
        _scramble.value = generateWcaScramble()
    }

    private fun finishWithDnf() {
        _state.value = TimerState.FINISHED
        val record = SolveRecord(
            timeMillis = 0L,
            penalty = Penalty.DNF,
            scramble = _scramble.value
        )
        onSaveResult(record)
        _scramble.value = generateWcaScramble()
    }

    private fun formatTime(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        val centis = (ms % 1000) / 10
        return if (minutes > 0) {
            String.format("%d:%02d.%02d", minutes, seconds, centis)
        } else {
            String.format("%d.%02d", seconds, centis)
        }
    }

    companion object {
        // Bộ sinh Scramble 3x3 chuẩn WCA 20 bước
        fun generateWcaScramble(length: Int = 20): String {
            val faces = arrayOf("U", "D", "L", "R", "F", "B")
            val modifiers = arrayOf("", "'", "2")
            val axisMap = mapOf("U" to 0, "D" to 0, "L" to 1, "R" to 1, "F" to 2, "B" to 2)

            val moves = mutableListOf<String>()
            var lastFace = ""
            var secondLastFace = ""

            while (moves.size < length) {
                val face = faces.random()
                // Không lặp mặt liền kề (vd: R R') và không lặp trục đối diện 3 lần (vd: R L R)
                if (face == lastFace) continue
                if (secondLastFace.isNotEmpty() &&
                    axisMap[face] == axisMap[lastFace] &&
                    axisMap[face] == axisMap[secondLastFace]
                ) continue

                moves.add(face + modifiers.random())
                secondLastFace = lastFace
                lastFace = face
            }
            return moves.joinToString(" ")
        }
    }
}
```

---

## 5. Lưu ý Triển khai cho Developer

1. **Chống chạm đúp (Debounce):** Thêm khoảng trễ `150ms`–`200ms` ngay sau khi chuyển sang trạng thái `SOLVING` để tránh việc người dùng chạm tay hơi lâu lúc bắt đầu giải khiến đồng hồ dừng ngay lập tức.
2. **Âm thanh & Rung phản hồi:** Sử dụng `ToneGenerator` hoặc `Vibrator` phát tín hiệu ngắn tại giây thứ **8** và giây thứ **12** trong giai đoạn `INSPECTION` để người chơi căn thời gian mà không cần nhìn chằm chằm vào số đếm ngược.
3. **Lưu trữ lịch sử:** Kết nối callback `onSaveResult(record)` với **Room Database** để tính toán các chỉ số thống kê như **PB (Personal Best)**, **Ao5 (Average of 5)** và **Ao12 (Average of 12)** hiển thị ở màn hình chính.
