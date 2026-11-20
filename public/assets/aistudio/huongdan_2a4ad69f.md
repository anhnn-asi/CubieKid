# Cẩm Nang Luyện Tập Rubik 3x3 Theo Phương Pháp CFOP

Tài liệu hướng dẫn chi tiết từng bước từ cơ bản đến nâng cao theo phương pháp **CFOP** (Cross - F2L - OLL - PLL), được tối ưu hóa cấu trúc để tích hợp trực tiếp vào ứng dụng Android (hỗ trợ điều hướng mục lục nhanh và hiển thị công thức chuẩn).

---

## Mục Lục Nhanh

1. [Quy Ước Ký Hiệu Xoay (Notation)](#1-quy-ước-ký-hiệu-xoay-notation)
2. [Bước 1: Giải Cross Tối Ưu Finger Tricks](#2-bước-1-giải-cross-tối-ưu-finger-tricks)
3. [Bước 2: Giải F2L (First Two Layers)](#3-bước-2-giải-f2l-first-two-layers)
4. [Bước 3: 2-Look OLL (Định Hướng Mặt Vàng Cơ Bản)](#4-bước-3-2-look-oll-định-hướng-mặt-vàng-cơ-bản)
5. [Bước 4: 2-Look PLL (Hoán Vị Tầng 3 Cơ Bản)](#5-bước-4-2-look-pll-hoán-vị-tầng-3-cơ-bản)
6. [Bước 5: Full OLL (57 Công Thức Chuẩn)](#6-bước-5-full-oll-57-công-thức-chuẩn)
7. [Bước 6: Full PLL (21 Công Thức Chuẩn)](#7-bước-6-full-pll-21-công-thức-chuẩn)
8. [Lộ Trình Luyện Tập Theo Từng Trình Độ](#8-lộ-trình-luyện-tập-theo-từng-trình-độ)

---

## 1. Quy Ước Ký Hiệu Xoay (Notation)

* **Mặt cơ bản:** `R` (Phải), `L` (Trái), `U` (Trên), `D` (Dưới), `F` (Trước), `B` (Sau).
* **Chiều xoay:** 
  * Chữ cái đứng một mình (ví dụ: `R`): Xoay **90° cùng chiều kim đồng hồ**.
  * Có dấu phẩy (ví dụ: `R'`): Xoay **90° ngược chiều kim đồng hồ**.
  * Có số 2 (ví dụ: `R2`): Xoay **180°**.
* **Xoay 2 tầng (Wide moves):** Chữ viết thường `r`, `l`, `u`, `d`, `f`, `b` (hoặc `Rw`, `Lw`...) nghĩa là xoay đồng thời mặt ngoài và tầng giữa liền kề.
* **Xoay tầng giữa:** `M` (cùng chiều `L`), `E` (cùng chiều `D`), `S` (cùng chiều `F`).
* **Xoay cả khối (Cube rotations):** `x` (theo chiều `R`), `y` (theo chiều `U`), `z` (theo chiều `F`).

---

## 2. Bước 1: Giải Cross Tối Ưu Finger Tricks

Mục tiêu của Cross là đưa 4 viên cạnh màu trắng về đúng vị trí ở mặt dưới cùng (`D`) trong **tối đa 8 bước xoay**.

### Nguyên tắc vàng để tối ưu tốc độ:
1. **Làm Cross ở mặt dưới (Cross on Bottom):** Tuyệt đối không làm dấu cộng trắng ở mặt trên rồi lật ngược khối lại. Làm ở đáy giúp bạn quan sát ngay được các cặp F2L đầu tiên ở tầng trên.
2. **Hạn chế tối đa xoay `B` và `D` (hoặc xoay cả khối):**
   * Xoay mặt `B` (mặt sau) làm gãy nhịp ngón tay (regrip) và che mất tầm nhìn. Nếu gặp viên cạnh ở mặt sau, hãy xoay nhẹ khối theo trục `y` hoặc `y'` để đưa nó về mặt `R`, `L` hoặc `F`, hoặc dùng tổ hợp `R' U' R` / `L U L'` để kéo xuống.
   * Chỉ dùng ngón áp út (Ring finger) tay trái đẩy `D` hoặc tay phải kéo `D'` để căn chỉnh tâm (Align Cross) ở bước cuối cùng.
3. **Giải tương đối (Relative Cross):** Không cần đưa từng viên cạnh về đúng tâm ngay lập tức. Hãy xếp 4 viên cạnh trắng đúng thứ tự màu tương đối (Đỏ đối diện Cam, Xanh lá đối diện Xanh dương), sau đó xoay một nhịp `D` / `D'` / `D2` duy nhất để khớp toàn bộ 4 tâm.
4. **Tận dụng 15 giây quan sát (Inspection):** Tính toán trọn vẹn đường đi của cả 4 viên cạnh trước khi chạm tay vào đồng hồ. Nhắm mắt giải Cross khi luyện tập để hình thành trí nhớ cơ bắp.

---

## 3. Bước 2: Giải F2L (First Two Layers)

F2L ghép đồng thời **1 viên góc trắng** và **1 viên cạnh tầng 2** thành một cặp rồi đưa vào rãnh (slot). Có 41 trường hợp chuẩn, nhưng bạn **không nên học vẹt công thức** mà hãy học theo tư duy trực quan (Intuitive F2L):

### Quy trình 3 bước xử lý mọi cặp F2L:
1. **Đưa cả 2 viên (Góc + Cạnh) lên tầng trên (`U`):** Nếu 1 trong 2 viên đang kẹt ở rãnh dưới, dùng công thức cơ bản `R U R'` (cho rãnh phải) hoặc `L' U' L` (cho rãnh trái) để đẩy lên tầng `U`.
2. **Tách đôi (nếu đang dính sai chiều) và thiết lập 1 trong 3 thế cơ bản:**
   * **Thế 1 (Mặt trắng của góc hướng sang ngang, 2 màu mặt trên khác nhau):** Đưa thẳng cặp này vào rãnh bằng 3 bước: `R U R'` hoặc `U R U' R'`.
   * **Thế 2 (Mặt trắng của góc hướng sang ngang, 2 màu mặt trên giống nhau):** Giấu viên góc xuống dưới, đưa viên cạnh ra xa để ghép thành thanh 2 viên, rồi đưa vào rãnh: `U' R U' R' U R U R'`.
   * **Thế 3 (Mặt trắng của góc hướng lên trời):** Đưa viên cạnh khớp với màu tâm bên hông, xoay cạnh ra xa góc để ghép đôi, sau đó đưa vào rãnh: `U R U2 R' U R U' R'`.
3. **Kỹ thuật nâng cao (Lookahead & Back-slotting):**
   * Ưu tiên giải 2 rãnh phía sau (`BL`, `BR`) trước để các rãnh trống còn lại nằm ở phía trước (`FL`, `FR`), giúp mắt dễ quan sát mà không cần xoay khối (`y`/`y'`).
   * Giảm tốc độ xoay tay một chút để mắt kịp nhìn tìm cặp F2L tiếp theo trong lúc tay đang giải cặp hiện tại.

---

## 4. Bước 3: 2-Look OLL (Định Hướng Mặt Vàng Cơ Bản)

Thay vì học ngay 57 công thức, 2-Look OLL chia việc làm full mặt vàng thành 2 bước nhỏ chỉ với **9 công thức**:

### Bước 3.1: Tạo dấu cộng vàng (Edge Orientation - 3 trường hợp)
* **Trường hợp 1 - Hình chữ I (Đường thẳng ngang):**
  `F (R U R' U') F'`
* **Trường hợp 2 - Hình chữ L nhỏ (Đặt ở góc 9 giờ và 12 giờ):**
  `f (R U R' U') f'` *(xoay 2 tầng trước)*
* **Trường hợp 3 - Dấu chấm (Chỉ có tâm vàng ở giữa):**
  `F (R U R' U') F' f (R U R' U') f'`

### Bước 3.2: Lật 4 góc vàng (Corner Orientation - 7 trường hợp)
1. **Sune (Con cá hướng đầu xuống góc dưới bên trái):**
   `R U R' U R U2 R'`
2. **Anti-Sune (Con cá hướng đầu lên góc trên bên phải):**
   `R U2 R' U' R U' R'`
3. **H (4 góc chưa lật, 2 cặp đèn pha đối diện):**
   `R U R' U R U' R' U R U2 R'`
4. **Pi (4 góc chưa lật, 1 cặp đèn pha bên trái, 2 viên còn lại hướng trước/sau):**
   `R U2 R2 U' R2 U' R2 U2 R`
5. **Headlights / U (2 góc trước chưa lật, màu vàng hướng thẳng về phía bạn):**
   `R2 D (R' U2 R) D' (R' U2 R')`
6. **Chameleon / T (2 góc trước chưa lật, màu vàng hướng sang 2 bên trái/phải):**
   `(r U R' U') (r' F R F')`
7. **Bowtie / L (2 góc chéo nhau chưa lật):**
   `F' (r U R' U') r' F R`

---

## 5. Bước 4: 2-Look PLL (Hoán Vị Tầng 3 Cơ Bản)

2-Look PLL hoàn thiện tầng cuối cùng chỉ với **6 công thức**:

### Bước 4.1: Đưa 4 góc về đúng vị trí (Corner Permutation - 2 trường hợp)
* **Aa-Perm (Đã có 1 cặp đèn pha đúng màu ở bên trái):**
  `x (R' U R') D2 (R U' R') D2 R2 x'`
* **Y-Perm (Không có cặp đèn pha nào đúng màu - đổi chéo góc):**
  `F (R U' R' U') (R U R' F') (R U R' U') (R' F R F')`

### Bước 4.2: Hoán vị 4 viên cạnh (Edge Permutation - 4 trường hợp)
* **Ua-Perm (Hoán vị 3 cạnh ngược chiều kim đồng hồ - mặt hoàn chỉnh ở sau):**
  `R U' R U R U R U' R' U' R2`
* **Ub-Perm (Hoán vị 3 cạnh cùng chiều kim đồng hồ - mặt hoàn chỉnh ở sau):**
  `R2 U R U R' U' R' U' R' U R'`
* **H-Perm (Đổi chéo 2 cặp cạnh đối diện):**
  `M2 U M2 U2 M2 U M2`
* **Z-Perm (Đổi 2 cặp cạnh kề nhau - trước/phải và sau/trái):**
  `M' U M2 U M2 U M' U2 M2`

---

## 6. Bước 5: Full OLL (57 Công Thức Chuẩn)

Để đạt mốc **Sub-15**, bạn cần nhận diện và giải mặt vàng chỉ trong 1 lần nhìn. Dưới đây là tổng hợp đầy đủ 57 công thức chia theo nhóm hình dạng:

### Nhóm 1: Không có cạnh đúng (Dot Cases - 8 công thức)
* **OLL 1:** `R U2 R2 F R F' U2 R' F R F'`
* **OLL 2:** `r U r' U2 r U2 R' U2 R U' r'`
* **OLL 3:** `r' R2 U R' U r U2 r' U M'`
* **OLL 4:** `M U' r U2 r' U' R U' R' M'`
* **OLL 17:** `F R' F' R2 r' U R U' R' U' M'`
* **OLL 18:** `r U R' U R U2 r2 U' R U' R' U2 r`
* **OLL 19:** `r' R U R U R' U' M' R' F R F'`
* **OLL 20:** `r U R' U' M2 U R U' R' U' M'`

### Nhóm 2: Tất cả cạnh đã đúng (Cross Cases - 7 công thức)
* **OLL 21 (H):** `R U R' U R U' R' U R U2 R'`
* **OLL 22 (Pi):** `R U2 R2 U' R2 U' R2 U2 R`
* **OLL 23 (U):** `R2 D' R U2 R' D R U2 R`
* **OLL 24 (T):** `r U R' U' r' F R F'`
* **OLL 25 (L):** `F' r U R' U' r' F R`
* **OLL 26 (Anti-Sune):** `R U2 R' U' R U' R'`
* **OLL 27 (Sune):** `R U R' U R U2 R'`

### Nhóm 3: Hình chữ T & Hình vuông (T & Square Shapes - 4 công thức)
* **OLL 33 (T1):** `(R U R' U') (R' F R F')`
* **OLL 45 (T2):** `F (R U R' U') F'`
* **OLL 5 (Square Left):** `l' U2 L U L' U l`
* **OLL 6 (Square Right):** `r U2 R' U' R U' r'`

### Nhóm 4: Hình chữ C, W và P (C, W & P Shapes - 8 công thức)
* **OLL 34 (C1):** `R U R2 U' R' F R U R U' F'`
* **OLL 46 (C2):** `R' U' R' F R F' U R`
* **OLL 51 (W1):** `F U R U' R' U R U' R' F'`
* **OLL 52 (W2):** `R U R' U R U' B U' B' R'`
* **OLL 31 (P1):** `R' U' F U R U' R' F' R`
* **OLL 32 (P2):** `L U F' U' L' U L F L'`
* **OLL 43 (P3):** `F' U' L' U L F`
* **OLL 44 (P4):** `F U R U' R' F'`

### Nhóm 5: Hình Con Cá (Fish Shapes - 4 công thức)
* **OLL 9:** `R U R' U' R' F R2 U R' U' F'`
* **OLL 10:** `R U R' U R' F R F' R U2 R'`
* **OLL 35:** `R U2 R2 F R F' R U2 R'`
* **OLL 37:** `F R' F' R U R U' R'`

### Nhóm 6: Hình Tia Chớp & Mũi Tên (Lightning & Awkward - 8 công thức)
* **OLL 7:** `r U R' U R U2 r'`
* **OLL 8:** `l' U' L U' L' U2 l`
* **OLL 11:** `r U R' U R' F R F' R U2 r'`
* **OLL 12:** `M' R' U' R U' R' U2 R U' R r'`
* **OLL 29:** `R U R' U' R U' R' F' U' F R U R'`
* **OLL 30:** `F R' F R2 U' R' U' R U R' F2`
* **OLL 41:** `R U R' U R U2 R' F R U R' U' F'`
* **OLL 42:** `R' U' R U' R' U2 R F R U R' U' F'`

### Nhóm 7: Hình Quân Mã (Knight Move Shapes - 4 công thức)
* **OLL 13:** `F U R U' R2 F' R U R U' R'`
* **OLL 14:** `R' F R U R' F' R F U' F'`
* **OLL 15:** `l' U' l L' U' L U l' U l`
* **OLL 16:** `r U r' R U R' U' r U' r'`

### Nhóm 8: Hình Chữ I & Chữ L Lớn (Line & Big L Shapes - 10 công thức)
* **OLL 55 (I1):** `R' F R U R U' R2 F' R2 U' R' U R U R'`
* **OLL 56 (I2):** `r' U' r U' R' U R U' R' U R r' U r`
* **OLL 57 (I3):** `R U R' U' M' U R U' r'`
* **OLL 53 (I4):** `l' U2 L U L' U' L U L' U l`
* **OLL 54 (I5):** `r U2 R' U' R U R' U' R U' r'`
* **OLL 47 (L1):** `R' U' R' F R F' R' F R F' U R`
* **OLL 48 (L2):** `F R U R' U' R U R' U' F'`
* **OLL 49 (L3):** `r U' r2 U r2 U r2 U' r`
* **OLL 50 (L4):** `r' U r2 U' r2 U' r2 U r'`
* **OLL 36 (L5):** `L' U' L U' L' U L U L F' L' F`

### Nhóm 9: Hình Đối Xứng Đặc Biệt (4 công thức còn lại)
* **OLL 28:** `r U R' U' r' R U R U' R'`
* **OLL 38:** `R U R' U R U' R' U' R' F R F'`
* **OLL 39:** `L F' L' U' L U F U' L'`
* **OLL 40:** `R' F R U R' U' F' U R`

---

## 7. Bước 6: Full PLL (21 Công Thức Chuẩn)

Hoàn thiện toàn bộ tầng 3 trong một bước duy nhất (1-Look PLL):

### Nhóm 1: Chỉ hoán vị cạnh (4 công thức)
1. **Ua-Perm:** `R U' R U R U R U' R' U' R2`
2. **Ub-Perm:** `R2 U R U R' U' R' U' R' U R'`
3. **H-Perm:** `M2 U M2 U2 M2 U M2`
4. **Z-Perm:** `M' U M2 U M2 U M' U2 M2`

### Nhóm 2: Chỉ hoán vị góc (3 công thức)
5. **Aa-Perm:** `x (R' U R') D2 (R U' R') D2 R2 x'`
6. **Ab-Perm:** `x R2 D2 (R U R') D2 (R U' R) x'`
7. **E-Perm:** `x' (R U' R' D) (R U R' D') (R U R' D) (R U' R' D') x`

### Nhóm 3: Hoán vị 1 cặp góc kề + 1 cặp cạnh (6 công thức)
8. **T-Perm:** `(R U R' U') R' F R2 U' R' U' (R U R' F')`
9. **F-Perm:** `(R' U' F') (R U R' U') R' F R2 U' R' U' (R U R' U R)`
10. **Ja-Perm:** `(R' U L' U2) (R U' R' U2 R) L`
11. **Jb-Perm:** `(R U R' F') (R U R' U') R' F R2 U' R'`
12. **Ra-Perm:** `(R U' R' U') (R U R D) (R' U' R D') (R' U2 R')`
13. **Rb-Perm:** `(R' U2 R U2) R' F (R U R' U') R' F' R2`

### Nhóm 4: Hoán vị 1 cặp góc chéo + 1 cặp cạnh (4 công thức)
14. **Y-Perm:** `F (R U' R' U') (R U R' F') (R U R' U') (R' F R F')`
15. **V-Perm:** `(R' U R' U') y (R' F' R2 U' R' U R' F R F)`
16. **Na-Perm:** `(R U R' U) (R U R' F') (R U R' U') R' F R2 U' R' U2 (R U' R')`
17. **Nb-Perm:** `(R' U R U') R' (F' U' F) (R U R' F) R' F' (R U' R)`

### Nhóm 5: Hoán vị vòng tròn 3 góc + 3 cạnh (G-Perms - 4 công thức)
18. **Ga-Perm:** `R2 U (R' U R' U') (R U' R2) D U' (R' U R D')`
19. **Gb-Perm:** `(F' U' F) (R2 u R' U) (R U' R u') R2`
20. **Gc-Perm:** `R2 U' (R U' R U) (R' U R2) D' U (R U' R' D)`
21. **Gd-Perm:** `D' (R U R' U') D (R2 U' R U') (R' U R' U) R2`

---

## 8. Lộ Trình Luyện Tập Theo Từng Trình Độ

Dữ liệu này dùng để đồng bộ với mục tiêu người dùng đã chọn trong màn hình **User Profile**:

| Mốc Trình Độ | Trọng Tâm Luyện Tập | Chỉ Số Phân Bổ Thời Gian Gợi Ý | Kỹ Thuật Cốt Lõi Cần Đạt |
| :--- | :--- | :--- | :--- |
| **Chưa biết giải $ightarrow$ Sub-60** | Làm quen ký hiệu xoay & phương pháp cơ bản (LBL) | Cross: 10s<br>Tầng 1 & 2: 30s<br>Tầng 3: 20s | Thuộc lòng quy tắc xoay `R U R' U'`, nhận diện màu tâm cố định. |
| **Sub-60 $ightarrow$ Sub-30** | Chuyển sang **CFOP cơ bản** (Intuitive F2L + 2-Look OLL/PLL) | Cross: 4s<br>F2L: 16s<br>OLL + PLL: 10s | Giải Cross ở mặt đáy, hiểu bản chất ghép cặp F2L mà không nhìn công thức. |
| **Sub-30 $ightarrow$ Sub-15** | Học thuộc **Full PLL (21)** + **Full OLL (57)** & Finger tricks | Cross: 2.5s<br>F2L: 7.5s<br>OLL + PLL: 5s | Loại bỏ động tác xoay cả khối thừa thãi, tập finger tricks bấm `U2`, `D'`, `M2`. |
| **Sub-15 $ightarrow$ Sub-10** | Thành thạo **Lookahead** & tối ưu F2L nâng cao (Back-slotting) | Cross: 1.5s<br>F2L: 5s<br>OLL + PLL: 3.5s | Nhìn trước cặp F2L tiếp theo khi tay đang giải cặp hiện tại, không dừng nghỉ (No pauses). |
| **Sub-10 $ightarrow$ Sub-6** | **X-Cross**, nhận diện PLL 2 mặt (2-Sided PLL Recognition) & TPS cao | Cross+1: 1.5s<br>F2L: 2.5s<br>OLL + PLL: 2s | Dự đoán được cặp F2L đầu tiên ngay trong 15s quan sát, đạt tốc độ 8–10 TPS. |
