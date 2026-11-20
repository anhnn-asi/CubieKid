package com.example.tutorial

import android.content.Context
import com.example.data.AppLanguage
import com.example.data.RubikLevel
import com.example.data.tr
import org.json.JSONObject

data class AlgorithmItem(
    val id: String,
    val name: String,
    val nameEn: String,
    val group: String,
    val moves: String,
    val setupMoves: String,
    val categoryId: String,
    val recommendedLevels: List<RubikLevel>
) {
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.EN) nameEn else name
}

data class AlgorithmCategory(
    val id: String,
    val name: String,
    val nameEn: String,
    val description: String,
    val descriptionEn: String,
    val algorithms: List<AlgorithmItem>
) {
    fun localizedName(lang: AppLanguage): String = if (lang == AppLanguage.EN) nameEn else name
    fun localizedDescription(lang: AppLanguage): String = if (lang == AppLanguage.EN) descriptionEn else description
}

data class HandbookAlgorithmEntry(
    val label: String,
    val formula: String,
    val noteVi: String = "",
    val noteEn: String = "",
    val trainerAlgorithmId: String? = null,
    val labelEn: String = label
)

data class HandbookGroup(
    val titleVi: String,
    val titleEn: String,
    val bulletsVi: List<String> = emptyList(),
    val bulletsEn: List<String> = emptyList(),
    val entries: List<HandbookAlgorithmEntry> = emptyList()
)

data class HandbookChapter(
    val number: Int,
    val shortTitleVi: String,
    val shortTitleEn: String,
    val titleVi: String,
    val titleEn: String,
    val summaryVi: String,
    val summaryEn: String,
    val groups: List<HandbookGroup>
)

data class LevelRoadmapRow(
    val fromLevel: RubikLevel,
    val toLevel: RubikLevel,
    val focusVi: String,
    val focusEn: String,
    val splitsVi: String,
    val splitsEn: String,
    val coreSkillsVi: String,
    val coreSkillsEn: String
)

class AlgorithmRepository(private val context: Context) {

    val categories: List<AlgorithmCategory> by lazy { loadCategoriesFromAssets() }

    private fun loadCategoriesFromAssets(): List<AlgorithmCategory> {
        val rawJson = context.assets.open("cfop_algorithms.json").bufferedReader().use { it.readText() }
        return parseJson(rawJson)
    }

    companion object {
        fun parseJson(rawJson: String): List<AlgorithmCategory> {
            val root = JSONObject(rawJson)
            val catArray = root.getJSONArray("categories")
            return buildList {
                for (i in 0 until catArray.length()) {
                    val catObj = catArray.getJSONObject(i)
                    val catId = catObj.getString("id")
                    val catName = catObj.getString("name")
                    val catNameEn = catObj.optString("name_en", "").ifBlank { stripVietnameseParen(catName) }
                    val catDesc = catObj.optString("description", "")
                    val catDescEn = catObj.optString("description_en", catDesc)
                    val algArray = catObj.getJSONArray("algorithms")
                    val items = buildList {
                        for (j in 0 until algArray.length()) {
                            val a = algArray.getJSONObject(j)
                            val lvls = buildList {
                                val arr = a.optJSONArray("recommended_levels")
                                if (arr != null) {
                                    for (k in 0 until arr.length()) {
                                        add(RubikLevel.fromName(arr.getString(k)))
                                    }
                                }
                            }
                            val algName = a.getString("name")
                            val algNameEn = a.optString("name_en", "").ifBlank { stripVietnameseParen(algName) }
                            add(
                                AlgorithmItem(
                                    id = a.getString("id"),
                                    name = algName,
                                    nameEn = algNameEn,
                                    group = a.optString("group", ""),
                                    moves = a.getString("moves"),
                                    setupMoves = a.getString("setup_moves"),
                                    categoryId = catId,
                                    recommendedLevels = lvls
                                )
                            )
                        }
                    }
                    add(
                        AlgorithmCategory(
                            id = catId,
                            name = catName,
                            nameEn = catNameEn,
                            description = catDesc,
                            descriptionEn = catDescEn,
                            algorithms = items
                        )
                    )
                }
            }
        }

        private fun stripVietnameseParen(text: String): String {
            return text.replace(Regex("\\s*\\([^)]*\\)\\s*$"), "").trim().ifEmpty { text }
        }

        val roadmapRows: List<LevelRoadmapRow> = listOf(
            LevelRoadmapRow(
                fromLevel = RubikLevel.BEGINNER,
                toLevel = RubikLevel.SUB_60,
                focusVi = "Làm quen ký hiệu xoay & phương pháp cơ bản (LBL)",
                focusEn = "Learn notation & basic Layer-by-Layer (LBL)",
                splitsVi = "Cross: 10s • Tầng 1&2: 30s • Tầng 3: 20s",
                splitsEn = "Cross: 10s • Layers 1&2: 30s • Last Layer: 20s",
                coreSkillsVi = "Thuộc lòng quy tắc R U R' U', nhận diện màu tâm cố định.",
                coreSkillsEn = "Master R U R' U' trigger and fixed center colors."
            ),
            LevelRoadmapRow(
                fromLevel = RubikLevel.SUB_60,
                toLevel = RubikLevel.SUB_30,
                focusVi = "Chuyển sang CFOP cơ bản (Intuitive F2L + 2-Look OLL/PLL)",
                focusEn = "Transition to CFOP (Intuitive F2L + 2-Look OLL/PLL)",
                splitsVi = "Cross: 4s • F2L: 16s • OLL + PLL: 10s",
                splitsEn = "Cross: 4s • F2L: 16s • OLL + PLL: 10s",
                coreSkillsVi = "Giải Cross ở mặt đáy, hiểu bản chất ghép cặp F2L mà không nhìn công thức.",
                coreSkillsEn = "Build Cross on bottom and pair F2L pieces intuitively."
            ),
            LevelRoadmapRow(
                fromLevel = RubikLevel.SUB_30,
                toLevel = RubikLevel.SUB_15,
                focusVi = "Học thuộc Full PLL (21) + Full OLL (57) & Fingertricks",
                focusEn = "Learn Full PLL (21) + Full OLL (57) & Fingertricks",
                splitsVi = "Cross: 2.5s • F2L: 7.5s • OLL + PLL: 5s",
                splitsEn = "Cross: 2.5s • F2L: 7.5s • OLL + PLL: 5s",
                coreSkillsVi = "Loại bỏ động tác xoay cả khối thừa, tập fingertricks U2, D', M2.",
                coreSkillsEn = "Eliminate excess cube rotations; practice U2, D', M2 flicks."
            ),
            LevelRoadmapRow(
                fromLevel = RubikLevel.SUB_15,
                toLevel = RubikLevel.SUB_10,
                focusVi = "Thành thạo Lookahead & tối ưu F2L nâng cao (Back-slotting)",
                focusEn = "Master Lookahead & Advanced F2L (Back-slotting)",
                splitsVi = "Cross: 1.5s • F2L: 5s • OLL + PLL: 3.5s",
                splitsEn = "Cross: 1.5s • F2L: 5s • OLL + PLL: 3.5s",
                coreSkillsVi = "Nhìn trước cặp F2L tiếp theo khi tay đang giải cặp hiện tại (No pauses).",
                coreSkillsEn = "Track the next F2L pair while inserting the current one with no pauses."
            ),
            LevelRoadmapRow(
                fromLevel = RubikLevel.SUB_10,
                toLevel = RubikLevel.SUB_6,
                focusVi = "X-Cross, nhận diện PLL 2 mặt (2-Sided PLL) & TPS cao",
                focusEn = "X-Cross, 2-Sided PLL Recognition & High TPS",
                splitsVi = "Cross+1: 1.5s • F2L: 2.5s • OLL + PLL: 2s",
                splitsEn = "Cross+1: 1.5s • F2L: 2.5s • OLL + PLL: 2s",
                coreSkillsVi = "Dự đoán cặp F2L đầu tiên trong 15s quan sát, đạt tốc độ 8–10 TPS.",
                coreSkillsEn = "Plan Cross + first F2L pair in 15s inspection; reach 8–10 TPS."
            )
        )

        val handbookChapters: List<HandbookChapter> = listOf(
            HandbookChapter(
                number = 1,
                shortTitleVi = "Ký hiệu",
                shortTitleEn = "Notation",
                titleVi = "1. Quy Ước Ký Hiệu Xoay (Notation)",
                titleEn = "1. Move Notation Reference",
                summaryVi = "Mặt mặc định: Front Xanh lá, Đáy Trắng (D), Trên Vàng (U).",
                summaryEn = "Default hold: Green Front, White Bottom (D), Yellow Top (U).",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Các nhóm ký hiệu chuẩn WCA",
                        titleEn = "Standard WCA Moves",
                        bulletsVi = listOf(
                            "Mặt cơ bản: R (Phải), L (Trái), U (Trên), D (Dưới), F (Trước), B (Sau).",
                            "Chiều xoay: Chữ đứng một mình (vd R) = 90° cùng chiều kim đồng hồ; có dấu phẩy (R') = 90° ngược chiều; có số 2 (R2) = 180°.",
                            "Xoay 2 tầng (Wide moves): r, l, u, d, f, b xoay đồng thời mặt ngoài + tầng giữa liền kề.",
                            "Xoay tầng giữa: M (theo chiều L), E (theo chiều D), S (theo chiều F).",
                            "Xoay cả khối: x (theo chiều R), y (theo chiều U), z (theo chiều F)."
                        ),
                        bulletsEn = listOf(
                            "Outer faces: R (Right), L (Left), U (Up), D (Down), F (Front), B (Back).",
                            "Direction: Single letter (R) = 90° clockwise; Prime (R') = 90° counter-clockwise; Double (R2) = 180°.",
                            "Wide moves: Lowercase r, l, u, d, f, b turn both outer layer and adjacent slice.",
                            "Slice moves: M (follows L), E (follows D), S (follows F).",
                            "Cube rotations: x (follows R), y (follows U), z (follows F)."
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 2,
                shortTitleVi = "Cross",
                shortTitleEn = "Cross",
                titleVi = "2. Bước 1: Giải Cross Tối Ưu Fingertricks",
                titleEn = "2. Step 1: Fingertrick-Optimal Cross",
                summaryVi = "Đưa 4 viên cạnh trắng về đúng đáy (D) trong tối đa 8 bước xoay.",
                summaryEn = "Place all 4 white edges on bottom (D) in 8 moves or less.",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "4 Nguyên tắc vàng tối ưu tốc độ",
                        titleEn = "4 Golden Rules for Speed",
                        bulletsVi = listOf(
                            "1. Làm Cross ở mặt dưới (Cross on Bottom): Tuyệt đối không làm dấu cộng trên rồi lật khối lại; giúp mắt thấy ngay F2L trên tầng U.",
                            "2. Hạn chế xoay B và D liên tục: Nếu viên ở mặt sau, dùng y/y' hoặc kéo bằng R' U' R / L U L'. Chỉ đẩy D/D' bằng ngón áp út ở nhịp căn chỉnh cuối.",
                            "3. Giải tương đối (Relative Cross): Xếp 4 cạnh đúng thứ tự màu tương đối (Đỏ đối Cam, Xanh lá đối Xanh dương) rồi xoay 1 nhịp D/D'/D2 khớp tâm.",
                            "4. Tận dụng 15 giây quan sát (Inspection): Lên kế hoạch trọn vẹn 4 viên cạnh trước khi chạm giờ."
                        ),
                        bulletsEn = listOf(
                            "1. Cross on Bottom: Never build the cross on top and flip; building on D keeps U open for first F2L lookahead.",
                            "2. Avoid awkward B turns: Prefer R' U' R or L U L' triggers, and save D/D' ring-finger flicks for final alignment.",
                            "3. Relative Cross: Insert all 4 edges in relative order (Red opp Orange, Green opp Blue), then finish with a single D/D'/D2 align.",
                            "4. Use 15s WCA Inspection: Plan all 4 cross edges completely before starting the timer."
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 3,
                shortTitleVi = "F2L",
                shortTitleEn = "F2L",
                titleVi = "3. Bước 2: Giải F2L (First Two Layers)",
                titleEn = "3. Step 2: Intuitive F2L",
                summaryVi = "Ghép đồng thời 1 góc trắng + 1 cạnh tầng 2 và đưa vào rãnh (slot).",
                summaryEn = "Pair each white corner with its middle edge and insert into its slot.",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Quy trình 3 bước & 3 thế cơ bản",
                        titleEn = "3-Step Process & 3 Base Cases",
                        bulletsVi = listOf(
                            "Bước 1: Đưa cả Góc + Cạnh lên tầng trên (U) bằng R U R' (rãnh phải) hoặc L' U' L (rãnh trái).",
                            "Bước 2: Tách đôi nếu dính sai chiều và thiết lập 1 trong 3 thế chuẩn bên dưới.",
                            "Bước 3 (Lookahead & Back-slotting): Ưu tiên giải 2 rãnh sau (BL, BR) trước để rãnh trống nằm phía trước (FL, FR) dễ quan sát."
                        ),
                        bulletsEn = listOf(
                            "Step 1: Bring both corner & edge to U using R U R' (right slot) or L' U' L (left slot).",
                            "Step 2: Separate bad pairs and set up one of the 3 basic insertion cases below.",
                            "Step 3 (Lookahead & Back-slotting): Fill back slots (BL, BR) first so remaining slots stay visible in front."
                        ),
                        entries = listOf(
                            HandbookAlgorithmEntry(
                                label = "Thế 1 (Trắng ngang, màu trên khác nhau)",
                                formula = "U R U' R'",
                                noteVi = "Hoặc R U R' đưa thẳng vào rãnh",
                                noteEn = "Or direct R U R' insert",
                                labelEn = "Case 1 (White facing side, mismatched top colors)"
                            ),
                            HandbookAlgorithmEntry(
                                label = "Thế 2 (Trắng ngang, màu trên giống nhau)",
                                formula = "U' R U' R' U R U R'",
                                noteVi = "Giấu góc rồi ghép thanh 2 viên",
                                noteEn = "Hide corner, pair edge, then insert",
                                labelEn = "Case 2 (White facing side, matching top colors)"
                            ),
                            HandbookAlgorithmEntry(
                                label = "Thế 3 (Trắng hướng lên trời)",
                                formula = "U R U2 R' U R U' R'",
                                noteVi = "Khớp màu cạnh với tâm bên hông trước",
                                noteEn = "Match edge side color to center first",
                                labelEn = "Case 3 (White facing up)"
                            )
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 4,
                shortTitleVi = "2-Look OLL",
                shortTitleEn = "2-Look OLL",
                titleVi = "4. Bước 3: 2-Look OLL (10 Công Thức)",
                titleEn = "4. Step 3: 2-Look OLL (10 Algs)",
                summaryVi = "Tạo chữ thập vàng (3 thế) rồi lật 4 góc vàng (7 thế) để hoàn thiện mặt U.",
                summaryEn = "Form the yellow cross (3 edge cases) then orient corners (7 cases).",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Bước 3.1: Tạo dấu cộng vàng (Edges)",
                        titleEn = "Step 3.1: Yellow Cross (Edges)",
                        entries = listOf(
                            HandbookAlgorithmEntry("Hình chữ I (Đường thẳng ngang)", "F R U R' U' F'", trainerAlgorithmId = "2oll_line", labelEn = "Line (Horizontal bar)"),
                            HandbookAlgorithmEntry("Hình chữ L nhỏ (Góc 9h & 12h)", "f R U R' U' f'", "Xoay 2 tầng trước (f)", "Wide front turn (f)", trainerAlgorithmId = "2oll_l_shape", labelEn = "L-Shape (9 & 12 o'clock)"),
                            HandbookAlgorithmEntry("Dấu chấm (Chỉ có tâm vàng)", "F R U R' U' F' f R U R' U' f'", trainerAlgorithmId = "2oll_dot", labelEn = "Dot (Center only)")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Bước 3.2: Lật 4 góc vàng (Corners)",
                        titleEn = "Step 3.2: Orient Corners",
                        entries = listOf(
                            HandbookAlgorithmEntry("Sune (Con cá hướng góc dưới trái)", "R U R' U R U2 R'", trainerAlgorithmId = "oll_27_sune", labelEn = "Sune (Fish pointing bottom-left)"),
                            HandbookAlgorithmEntry("Anti-Sune (Con cá hướng góc trên phải)", "R U2 R' U' R U' R'", trainerAlgorithmId = "oll_26_antisune", labelEn = "Anti-Sune (Fish pointing top-right)"),
                            HandbookAlgorithmEntry("H (2 cặp đèn pha đối diện)", "R U R' U R U' R' U R U2 R'", trainerAlgorithmId = "oll_21_h", labelEn = "H (2 opposite headlights)"),
                            HandbookAlgorithmEntry("Pi (1 đèn pha trái, 2 viên trước/sau)", "R U2 R2 U' R2 U' R2 U2 R", trainerAlgorithmId = "oll_22_pi", labelEn = "Pi (Left headlights, front/back bars)"),
                            HandbookAlgorithmEntry("Headlights / U (Đèn pha phía trước)", "R2 D R' U2 R D' R' U2 R'", trainerAlgorithmId = "oll_23_headlights", labelEn = "Headlights / U (Front headlights)"),
                            HandbookAlgorithmEntry("Chameleon / T (2 vàng hướng ngang)", "r U R' U' r' F R F'", trainerAlgorithmId = "oll_24_chameleon", labelEn = "Chameleon / T (Side yellow corners)"),
                            HandbookAlgorithmEntry("Bowtie / L (2 góc chéo chưa lật)", "F' r U R' U' r' F R", trainerAlgorithmId = "oll_25_bowtie", labelEn = "Bowtie / L (Diagonal corners)")
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 5,
                shortTitleVi = "2-Look PLL",
                shortTitleEn = "2-Look PLL",
                titleVi = "5. Bước 4: 2-Look PLL (6 Công Thức)",
                titleEn = "5. Step 4: 2-Look PLL (6 Algs)",
                summaryVi = "Đưa 4 góc về đúng chỗ (2 thế) rồi hoán vị 4 cạnh tầng 3 (4 thế).",
                summaryEn = "Permute corners first (2 cases) then permute edges (4 cases).",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Bước 4.1: Hoán vị góc (Corners)",
                        titleEn = "Step 4.1: Corner Permutation",
                        entries = listOf(
                            HandbookAlgorithmEntry("T-Perm / Aa-Perm (Có 1 cặp đèn pha bên trái)", "x R' U R' D2 R U' R' D2 R2 x'", "Hoặc dùng T-Perm khi đèn pha ở trái", "Or T-Perm with headlights left", trainerAlgorithmId = "pll_t_perm", labelEn = "T-Perm / Aa-Perm (Headlights on left)"),
                            HandbookAlgorithmEntry("Y-Perm (Không có đèn pha — đổi chéo)", "F R U' R' U' R U R' F' R U R' U' R' F R F'", trainerAlgorithmId = "pll_y_perm", labelEn = "Y-Perm (No headlights — diagonal swap)")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Bước 4.2: Hoán vị cạnh (Edges)",
                        titleEn = "Step 4.2: Edge Permutation",
                        entries = listOf(
                            HandbookAlgorithmEntry("Ua-Perm (3 cạnh ngược kim đồng hồ)", "R U' R U R U R U' R' U' R2", "Mặt hoàn chỉnh ở sau", "Solved bar at back", trainerAlgorithmId = "pll_ua_perm", labelEn = "Ua-Perm (3 edges counter-clockwise)"),
                            HandbookAlgorithmEntry("Ub-Perm (3 cạnh cùng kim đồng hồ)", "R2 U R U R' U' R' U' R' U R'", "Mặt hoàn chỉnh ở sau", "Solved bar at back", trainerAlgorithmId = "pll_ub_perm", labelEn = "Ub-Perm (3 edges clockwise)"),
                            HandbookAlgorithmEntry("H-Perm (Đổi chéo 2 cặp cạnh đối diện)", "M2 U M2 U2 M2 U M2", trainerAlgorithmId = "pll_h_perm", labelEn = "H-Perm (Swap opposite edges)"),
                            HandbookAlgorithmEntry("Z-Perm (Đổi 2 cặp cạnh kề nhau)", "M' U M2 U M2 U M' U2 M2", trainerAlgorithmId = "pll_z_perm", labelEn = "Z-Perm (Swap adjacent edges)")
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 6,
                shortTitleVi = "Full OLL (57)",
                shortTitleEn = "Full OLL (57)",
                titleVi = "6. Bước 5: Full OLL (57 Công Thức Chuẩn)",
                titleEn = "6. Step 5: Full OLL (57 Cases)",
                summaryVi = "Dành cho mục tiêu Sub-15: giải toàn bộ mặt vàng chỉ trong 1 lần nhìn.",
                summaryEn = "Essential for Sub-15: orient the entire yellow layer in 1 look.",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Nhóm 1: Dot Cases (8 công thức)",
                        titleEn = "Group 1: Dot Cases (8)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 1", "R U2 R2 F R F' U2 R' F R F'"),
                            HandbookAlgorithmEntry("OLL 2", "r U r' U2 r U2 R' U2 R U' r'"),
                            HandbookAlgorithmEntry("OLL 3", "r' R2 U R' U r U2 r' U M'"),
                            HandbookAlgorithmEntry("OLL 4", "M U' r U2 r' U' R U' R' M'"),
                            HandbookAlgorithmEntry("OLL 17", "F R' F' R2 r' U R U' R' U' M'"),
                            HandbookAlgorithmEntry("OLL 18", "r U R' U R U2 r2 U' R U' R' U2 r"),
                            HandbookAlgorithmEntry("OLL 19", "r' R U R U R' U' M' R' F R F'"),
                            HandbookAlgorithmEntry("OLL 20", "r U R' U' M2 U R U' R' U' M'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 2: Cross Cases (7 công thức)",
                        titleEn = "Group 2: Cross Cases (7)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 21 (H)", "R U R' U R U' R' U R U2 R'", trainerAlgorithmId = "oll_21_h"),
                            HandbookAlgorithmEntry("OLL 22 (Pi)", "R U2 R2 U' R2 U' R2 U2 R", trainerAlgorithmId = "oll_22_pi"),
                            HandbookAlgorithmEntry("OLL 23 (U)", "R2 D' R U2 R' D R U2 R", trainerAlgorithmId = "oll_23_headlights"),
                            HandbookAlgorithmEntry("OLL 24 (T)", "r U R' U' r' F R F'", trainerAlgorithmId = "oll_24_chameleon"),
                            HandbookAlgorithmEntry("OLL 25 (L)", "F' r U R' U' r' F R", trainerAlgorithmId = "oll_25_bowtie"),
                            HandbookAlgorithmEntry("OLL 26 (Anti-Sune)", "R U2 R' U' R U' R'", trainerAlgorithmId = "oll_26_antisune"),
                            HandbookAlgorithmEntry("OLL 27 (Sune)", "R U R' U R U2 R'", trainerAlgorithmId = "oll_27_sune")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 3: Chữ T & Hình Vuông (4 công thức)",
                        titleEn = "Group 3: T & Square Shapes (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 33 (T1)", "R U R' U' R' F R F'"),
                            HandbookAlgorithmEntry("OLL 45 (T2)", "F R U R' U' F'", trainerAlgorithmId = "2oll_line"),
                            HandbookAlgorithmEntry("OLL 5 (Square Left)", "l' U2 L U L' U l"),
                            HandbookAlgorithmEntry("OLL 6 (Square Right)", "r U2 R' U' R U' r'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 4: Hình chữ C, W và P (8 công thức)",
                        titleEn = "Group 4: C, W & P Shapes (8)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 34 (C1)", "R U R2 U' R' F R U R U' F'"),
                            HandbookAlgorithmEntry("OLL 46 (C2)", "R' U' R' F R F' U R"),
                            HandbookAlgorithmEntry("OLL 51 (W1)", "F U R U' R' U R U' R' F'"),
                            HandbookAlgorithmEntry("OLL 52 (W2)", "R U R' U R U' B U' B' R'"),
                            HandbookAlgorithmEntry("OLL 31 (P1)", "R' U' F U R U' R' F' R"),
                            HandbookAlgorithmEntry("OLL 32 (P2)", "L U F' U' L' U L F L'"),
                            HandbookAlgorithmEntry("OLL 43 (P3)", "F' U' L' U L F"),
                            HandbookAlgorithmEntry("OLL 44 (P4)", "F U R U' R' F'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 5: Hình Con Cá (4 công thức)",
                        titleEn = "Group 5: Fish Shapes (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 9", "R U R' U' R' F R2 U R' U' F'"),
                            HandbookAlgorithmEntry("OLL 10", "R U R' U R' F R F' R U2 R'"),
                            HandbookAlgorithmEntry("OLL 35", "R U2 R2 F R F' R U2 R'"),
                            HandbookAlgorithmEntry("OLL 37", "F R' F' R U R U' R'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 6: Tia Chớp & Mũi Tên (8 công thức)",
                        titleEn = "Group 6: Lightning & Awkward (8)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 7", "r U R' U R U2 r'"),
                            HandbookAlgorithmEntry("OLL 8", "l' U' L U' L' U2 l"),
                            HandbookAlgorithmEntry("OLL 11", "r U R' U R' F R F' R U2 r'"),
                            HandbookAlgorithmEntry("OLL 12", "M' R' U' R U' R' U2 R U' R r'"),
                            HandbookAlgorithmEntry("OLL 29", "R U R' U' R U' R' F' U' F R U R'"),
                            HandbookAlgorithmEntry("OLL 30", "F R' F R2 U' R' U' R U R' F2"),
                            HandbookAlgorithmEntry("OLL 41", "R U R' U R U2 R' F R U R' U' F'"),
                            HandbookAlgorithmEntry("OLL 42", "R' U' R U' R' U2 R F R U R' U' F'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 7: Hình Quân Mã (4 công thức)",
                        titleEn = "Group 7: Knight Move Shapes (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 13", "F U R U' R2 F' R U R U' R'"),
                            HandbookAlgorithmEntry("OLL 14", "R' F R U R' F' R F U' F'"),
                            HandbookAlgorithmEntry("OLL 15", "l' U' l L' U' L U l' U l"),
                            HandbookAlgorithmEntry("OLL 16", "r U r' R U R' U' r U' r'")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 8: Chữ I & Chữ L Lớn (10 công thức)",
                        titleEn = "Group 8: Line & Big L Shapes (10)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 55 (I1)", "R' F R U R U' R2 F' R2 U' R' U R U R'"),
                            HandbookAlgorithmEntry("OLL 56 (I2)", "r' U' r U' R' U R U' R' U R r' U r"),
                            HandbookAlgorithmEntry("OLL 57 (I3)", "R U R' U' M' U R U' r'"),
                            HandbookAlgorithmEntry("OLL 53 (I4)", "l' U2 L U L' U' L U L' U l"),
                            HandbookAlgorithmEntry("OLL 54 (I5)", "r U2 R' U' R U R' U' R U' r'"),
                            HandbookAlgorithmEntry("OLL 47 (L1)", "R' U' R' F R F' R' F R F' U R"),
                            HandbookAlgorithmEntry("OLL 48 (L2)", "F R U R' U' R U R' U' F'"),
                            HandbookAlgorithmEntry("OLL 49 (L3)", "r U' r2 U r2 U r2 U' r"),
                            HandbookAlgorithmEntry("OLL 50 (L4)", "r' U r2 U' r2 U' r2 U r'"),
                            HandbookAlgorithmEntry("OLL 36 (L5)", "L' U' L U' L' U L U L F' L' F")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 9: Đối Xứng Đặc Biệt (4 công thức)",
                        titleEn = "Group 9: Special Symmetric Shapes (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("OLL 28", "r U R' U' r' R U R U' R'"),
                            HandbookAlgorithmEntry("OLL 38", "R U R' U R U' R' U' R' F R F'"),
                            HandbookAlgorithmEntry("OLL 39", "L F' L' U' L U F U' L'"),
                            HandbookAlgorithmEntry("OLL 40", "R' F R U R' U' F' U R")
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 7,
                shortTitleVi = "Full PLL (21)",
                shortTitleEn = "Full PLL (21)",
                titleVi = "7. Bước 6: Full PLL (21 Công Thức Chuẩn)",
                titleEn = "7. Step 6: Full PLL (21 Cases)",
                summaryVi = "Hoán vị hoàn chỉnh tầng 3 chỉ trong 1 bước nhìn duy nhất (1-Look PLL).",
                summaryEn = "Complete the entire last layer permutation in 1 look.",
                groups = listOf(
                    HandbookGroup(
                        titleVi = "Nhóm 1: Chỉ hoán vị cạnh (4 công thức)",
                        titleEn = "Group 1: Edges Only (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("Ua-Perm", "R U' R U R U R U' R' U' R2", trainerAlgorithmId = "pll_ua_perm"),
                            HandbookAlgorithmEntry("Ub-Perm", "R2 U R U R' U' R' U' R' U R'", trainerAlgorithmId = "pll_ub_perm"),
                            HandbookAlgorithmEntry("H-Perm", "M2 U M2 U2 M2 U M2", trainerAlgorithmId = "pll_h_perm"),
                            HandbookAlgorithmEntry("Z-Perm", "M' U M2 U M2 U M' U2 M2", trainerAlgorithmId = "pll_z_perm")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 2: Chỉ hoán vị góc (3 công thức)",
                        titleEn = "Group 2: Corners Only (3)",
                        entries = listOf(
                            HandbookAlgorithmEntry("Aa-Perm", "x R' U R' D2 R U' R' D2 R2 x'", trainerAlgorithmId = "pll_aa_perm"),
                            HandbookAlgorithmEntry("Ab-Perm", "x R2 D2 R U R' D2 R U' R x'"),
                            HandbookAlgorithmEntry("E-Perm", "x' R U' R' D R U R' D' R U R' D R U' R' D' x")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 3: 1 cặp góc kề + 1 cặp cạnh (6 công thức)",
                        titleEn = "Group 3: Adjacent Corners + Edges (6)",
                        entries = listOf(
                            HandbookAlgorithmEntry("T-Perm", "R U R' U' R' F R2 U' R' U' R U R' F'", trainerAlgorithmId = "pll_t_perm"),
                            HandbookAlgorithmEntry("F-Perm", "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R"),
                            HandbookAlgorithmEntry("Ja-Perm", "R' U L' U2 R U' R' U2 R L"),
                            HandbookAlgorithmEntry("Jb-Perm", "R U R' F' R U R' U' R' F R2 U' R'", trainerAlgorithmId = "pll_jb_perm"),
                            HandbookAlgorithmEntry("Ra-Perm", "R U' R' U' R U R D R' U' R D' R' U2 R'", trainerAlgorithmId = "pll_ra_perm"),
                            HandbookAlgorithmEntry("Rb-Perm", "R' U2 R U2 R' F R U R' U' R' F' R2")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 4: 1 cặp góc chéo + 1 cặp cạnh (4 công thức)",
                        titleEn = "Group 4: Diagonal Corners + Edges (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("Y-Perm", "F R U' R' U' R U R' F' R U R' U' R' F R F'", trainerAlgorithmId = "pll_y_perm"),
                            HandbookAlgorithmEntry("V-Perm", "R' U R' U' y R' F' R2 U' R' U R' F R F"),
                            HandbookAlgorithmEntry("Na-Perm", "R U R' U R U R' F' R U R' U' R' F R2 U' R' U2 R U' R'"),
                            HandbookAlgorithmEntry("Nb-Perm", "R' U R U' R' F' U' F R U R' F R' F' R U' R")
                        )
                    ),
                    HandbookGroup(
                        titleVi = "Nhóm 5: G-Perms (4 công thức)",
                        titleEn = "Group 5: G-Perms (4)",
                        entries = listOf(
                            HandbookAlgorithmEntry("Ga-Perm", "R2 U R' U R' U' R U' R2 D U' R' U R D'"),
                            HandbookAlgorithmEntry("Gb-Perm", "F' U' F R2 u R' U R U' R u' R2"),
                            HandbookAlgorithmEntry("Gc-Perm", "R2 U' R U' R U R' U R2 D' U R U' R' D"),
                            HandbookAlgorithmEntry("Gd-Perm", "D' R U R' U' D R2 U' R U' R' U R' U R2")
                        )
                    )
                )
            ),
            HandbookChapter(
                number = 8,
                shortTitleVi = "Lộ trình",
                shortTitleEn = "Roadmap",
                titleVi = "8. Lộ Trình Luyện Tập Theo Trình Độ",
                titleEn = "8. Level Training Roadmap",
                summaryVi = "Đồng bộ trực tiếp với mục tiêu trong Hồ sơ của bạn.",
                summaryEn = "Synced directly with your profile level & target.",
                groups = emptyList() // Rendered with custom LevelRoadmap cards highlighted to user profile
            )
        )
    }
}
