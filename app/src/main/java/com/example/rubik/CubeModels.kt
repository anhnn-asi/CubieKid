package com.example.rubik

enum class CubeColor(
    val code: Char,
    val viName: String,
    val enName: String
) {
    YELLOW('U', "Vàng", "Yellow"),
    RED('R', "Đỏ", "Red"),
    BLUE('F', "Xanh dương", "Blue"),
    WHITE('D', "Trắng", "White"),
    ORANGE('L', "Cam", "Orange"),
    GREEN('B', "Xanh lá", "Green")
}

enum class FaceId(
    val index: Int,
    val defaultColor: CubeColor,
    val viName: String,
    val enName: String
) {
    U(0, CubeColor.YELLOW, "Mặt Trên (Vàng)", "Up (Yellow)"),
    R(1, CubeColor.ORANGE, "Mặt Phải (Cam)", "Right (Orange)"),
    F(2, CubeColor.GREEN, "Mặt Trước (Xanh lá)", "Front (Green)"),
    D(3, CubeColor.WHITE, "Mặt Đáy (Trắng)", "Down (White)"),
    L(4, CubeColor.RED, "Mặt Trái (Đỏ)", "Left (Red)"),
    B(5, CubeColor.BLUE, "Mặt Sau (Xanh dương)", "Back (Blue)")
}

enum class MoveFace(val symbol: String, val viLabel: String, val enLabel: String) {
    U("U", "Mặt trên", "Top"),
    R("R", "Mặt phải", "Right"),
    F("F", "Mặt trước", "Front"),
    D("D", "Mặt đáy", "Bottom"),
    L("L", "Mặt trái", "Left"),
    B("B", "Mặt sau", "Back")
}

enum class CubeMove(
    val notation: String,
    val face: MoveFace,
    val quarterTurns: Int,
    val viInstruction: String,
    val enInstruction: String,
    val fingertrickTipVi: String,
    val fingertrickTipEn: String,
    val fingertrickCost: Int
) {
    U("U", MoveFace.U, 1, "Xoay mặt trên sang trái", "Turn top layer clockwise", "Ngón trỏ phải gạt U", "Right index flick U", 9),
    U_PRIME("U'", MoveFace.U, 3, "Xoay mặt trên sang phải", "Turn top layer counter-clockwise", "Ngón trỏ trái gạt U'", "Left index flick U'", 9),
    U2("U2", MoveFace.U, 2, "Xoay mặt trên 180 độ", "Turn top layer twice", "Đúp trỏ-giữa phải U2", "Double index-middle U2", 13),

    R("R", MoveFace.R, 1, "Xoay mặt phải lên trên", "Turn right side up", "Cổ tay phải xoay R lên", "Right wrist roll R up", 9),
    R_PRIME("R'", MoveFace.R, 3, "Xoay mặt phải xuống dưới", "Turn right side down", "Cổ tay phải kéo R' xuống", "Right wrist pull R' down", 9),
    R2("R2", MoveFace.R, 2, "Xoay mặt phải 180 độ", "Turn right side twice", "Xoay nhanh R2 cổ tay phải", "Quick double right turn R2", 13),

    F("F", MoveFace.F, 1, "Xoay mặt trước theo chiều kim đồng hồ", "Turn front clockwise", "Ngón trỏ phải ấn F", "Right index push F", 11),
    F_PRIME("F'", MoveFace.F, 3, "Xoay mặt trước ngược chiều kim đồng hồ", "Turn front counter-clockwise", "Ngón cái phải đẩy F'", "Right thumb push F'", 11),
    F2("F2", MoveFace.F, 2, "Xoay mặt trước 180 độ", "Turn front twice", "Đẩy liên tiếp 2 nhịp F2", "Double flick F2", 16),

    D("D", MoveFace.D, 1, "Xoay mặt đáy sang phải", "Turn bottom clockwise", "Ngón áp út trái đẩy D", "Left ring push D", 10),
    D_PRIME("D'", MoveFace.D, 3, "Xoay mặt đáy sang trái", "Turn bottom counter-clockwise", "Ngón áp út phải đẩy D'", "Right ring push D'", 10),
    D2("D2", MoveFace.D, 2, "Xoay mặt đáy 180 độ", "Turn bottom twice", "Đúp áp út-út D2", "Ring-pinky double D2", 14),

    L("L", MoveFace.L, 1, "Xoay mặt trái xuống dưới", "Turn left side down", "Cổ tay trái xoay L xuống", "Left wrist roll L down", 10),
    L_PRIME("L'", MoveFace.L, 3, "Xoay mặt trái lên trên", "Turn left side up", "Cổ tay trái xoay L' lên", "Left wrist roll L' up", 10),
    L2("L2", MoveFace.L, 2, "Xoay mặt trái 180 độ", "Turn left side twice", "Xoay nhanh L2 tay trái", "Double left turn L2", 14),

    B("B", MoveFace.B, 1, "Xoay mặt sau sang trái", "Turn back clockwise", "Ngón trỏ trái móc B", "Left index pull B", 22),
    B_PRIME("B'", MoveFace.B, 3, "Xoay mặt sau sang phải", "Turn back counter-clockwise", "Ngón trỏ phải móc B'", "Right index pull B'", 22),
    B2("B2", MoveFace.B, 2, "Xoay mặt sau 180 độ", "Turn back twice", "Xoay mặt sau B2 hai nhịp", "Double back turn B2", 26);

    fun inverse(): CubeMove = when (this) {
        U -> U_PRIME
        U_PRIME -> U
        U2 -> U2
        R -> R_PRIME
        R_PRIME -> R
        R2 -> R2
        F -> F_PRIME
        F_PRIME -> F
        F2 -> F2
        D -> D_PRIME
        D_PRIME -> D
        D2 -> D2
        L -> L_PRIME
        L_PRIME -> L
        L2 -> L2
        B -> B_PRIME
        B_PRIME -> B
        B2 -> B2
    }

    companion object {
        private val byNotation = entries.associateBy { it.notation }
        fun parse(token: String): CubeMove? = byNotation[token.trim()]
        fun parseAlgorithm(alg: String): List<CubeMove> =
            alg.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.mapNotNull { parse(it) }
    }
}

enum class CfopStage(
    val viTitle: String,
    val enTitle: String,
    val badgeCode: String,
    val viSummary: String
) {
    CROSS("Chữ Thập Đáy", "Cross", "C", "Ghép 4 cạnh trắng ở đáy với ít bước xoay nhất"),
    F2L("2 Tầng Đầu", "F2L", "F", "Ghép từng cặp góc-cạnh vào 4 khe tầng 1 & 2"),
    OLL("Lật Mặt Vàng", "OLL", "O", "Đưa toàn bộ mặt trên thành màu vàng"),
    PLL("Hoán Vị Cuối", "PLL", "P", "Sắp xếp đúng vị trí các viên tầng trên cùng")
}

data class CfopStep(
    val stepIndex: Int,
    val stage: CfopStage,
    val move: CubeMove,
    val caseLabel: String,
    val cubeBefore: CubeState,
    val cubeAfter: CubeState
)

data class CfopSolution(
    val initialCube: CubeState,
    val crossMoves: List<CubeMove>,
    val f2lMoves: List<CubeMove>,
    val ollMoves: List<CubeMove>,
    val pllMoves: List<CubeMove>,
    val ollCaseName: String,
    val pllCaseName: String,
    val crossFingertrickScore: Int,
    val steps: List<CfopStep>
) {
    val allMoves: List<CubeMove>
        get() = crossMoves + f2lMoves + ollMoves + pllMoves
    val totalMoveCount: Int
        get() = allMoves.size
    val fullNotation: String
        get() = allMoves.joinToString(" ") { it.notation }
}
