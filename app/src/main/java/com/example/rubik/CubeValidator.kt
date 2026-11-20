package com.example.rubik

sealed class CubeValidationResult {
    data class Valid(val cube: CubeState) : CubeValidationResult()
    data class Invalid(val viReason: String, val enReason: String) : CubeValidationResult()
}

object CubeValidator {

    // 12 edges: pairs of (primary facelet, secondary facelet)
    private val EDGE_INDICES = arrayOf(
        1 to 46,  // UB
        5 to 10,  // UR
        7 to 19,  // UF
        3 to 37,  // UL
        28 to 25, // DF
        32 to 16, // DR
        34 to 52, // DB
        30 to 43, // DL
        23 to 12, // FR
        21 to 41, // FL
        48 to 14, // BR
        50 to 39  // BL
    )

    // 8 corners: triples in clockwise order around the corner
    private val CORNER_INDICES = arrayOf(
        Triple(8, 9, 20),   // UFR
        Triple(6, 18, 38),  // UFL
        Triple(0, 36, 47),  // UBL
        Triple(2, 45, 11),  // UBR
        Triple(29, 26, 15), // DFR
        Triple(27, 44, 24), // DFL
        Triple(33, 53, 42), // DBL
        Triple(35, 17, 51)  // DBR
    )

    // Expected physical pieces from a solved cube (U=Yellow, R=Orange, F=Green, D=White, L=Red, B=Blue)
    private val SOLVED_EDGE_COLORS = listOf(
        CubeColor.YELLOW to CubeColor.BLUE,   // UB
        CubeColor.YELLOW to CubeColor.ORANGE, // UR
        CubeColor.YELLOW to CubeColor.GREEN,  // UF
        CubeColor.YELLOW to CubeColor.RED,    // UL
        CubeColor.WHITE to CubeColor.GREEN,   // DF
        CubeColor.WHITE to CubeColor.ORANGE,  // DR
        CubeColor.WHITE to CubeColor.BLUE,    // DB
        CubeColor.WHITE to CubeColor.RED,     // DL
        CubeColor.GREEN to CubeColor.ORANGE,  // FR
        CubeColor.GREEN to CubeColor.RED,     // FL
        CubeColor.BLUE to CubeColor.ORANGE,   // BR
        CubeColor.BLUE to CubeColor.RED       // BL
    )

    private val SOLVED_CORNER_COLORS = listOf(
        Triple(CubeColor.YELLOW, CubeColor.ORANGE, CubeColor.GREEN), // UFR
        Triple(CubeColor.YELLOW, CubeColor.GREEN, CubeColor.RED),    // UFL
        Triple(CubeColor.YELLOW, CubeColor.RED, CubeColor.BLUE),     // UBL
        Triple(CubeColor.YELLOW, CubeColor.BLUE, CubeColor.ORANGE),  // UBR
        Triple(CubeColor.WHITE, CubeColor.GREEN, CubeColor.ORANGE),  // DFR
        Triple(CubeColor.WHITE, CubeColor.RED, CubeColor.GREEN),     // DFL
        Triple(CubeColor.WHITE, CubeColor.BLUE, CubeColor.RED),      // DBL
        Triple(CubeColor.WHITE, CubeColor.ORANGE, CubeColor.BLUE)    // DBR
    )

    /**
     * Given 6 captured 3x3 face grids keyed by center color (each potentially rotated 0..3 times
     * clockwise depending on how the child held the cube during the 30-second video),
     * searches all 4^6 = 4096 face rotations with early edge-pruning to find a physically valid 3x3 CubeState.
     */
    fun reconcileAndValidate(capturedFaces: Map<CubeColor, List<CubeColor>>): CubeValidationResult {
        if (capturedFaces.size < 6) {
            val missing = CubeColor.entries.filter { it !in capturedFaces }
                .joinToString(", ") { it.viName }
            return CubeValidationResult.Invalid(
                viReason = "Bé chưa quét đủ 6 mặt! Còn thiếu mặt tâm: $missing.",
                enReason = "Missing faces! Please scan all 6 centers."
            )
        }

        val orderedFaces = listOf(
            capturedFaces[CubeColor.YELLOW]!!,
            capturedFaces[CubeColor.ORANGE]!!,
            capturedFaces[CubeColor.GREEN]!!,
            capturedFaces[CubeColor.WHITE]!!,
            capturedFaces[CubeColor.RED]!!,
            capturedFaces[CubeColor.BLUE]!!
        )

        // Check global 9-sticker count per color before searching rotations
        val counts = mutableMapOf<CubeColor, Int>()
        orderedFaces.flatten().forEach { c -> counts[c] = (counts[c] ?: 0) + 1 }
        val badColor = CubeColor.entries.firstOrNull { (counts[it] ?: 0) != 9 }
        if (badColor != null) {
            val count = counts[badColor] ?: 0
            return CubeValidationResult.Invalid(
                viReason = "Màu ${badColor.viName} đang đếm được $count ô (cần đúng 9 ô). Hãy quay lại ở nơi ánh sáng đều hơn nhé!",
                enReason = "Detected $count ${badColor.enName} stickers (need 9). Try even lighting!"
            )
        }

        val rotatedVariants: List<List<List<CubeColor>>> = orderedFaces.map { grid ->
            (0 until 4).map { r -> rotateGridCW(grid, r) }
        }

        // Fast search over 4^6 = 4096 rotations with edge-pair validity pruning
        for (uRot in 0 until 4) {
            val uGrid = rotatedVariants[0][uRot]
            for (rRot in 0 until 4) {
                val rGrid = rotatedVariants[1][rRot]
                if (!isValidEdgePair(uGrid[5], rGrid[1])) continue
                for (fRot in 0 until 4) {
                    val fGrid = rotatedVariants[2][fRot]
                    if (!isValidEdgePair(uGrid[7], fGrid[1]) || !isValidEdgePair(fGrid[5], rGrid[3])) continue
                    for (dRot in 0 until 4) {
                        val dGrid = rotatedVariants[3][dRot]
                        if (!isValidEdgePair(dGrid[1], fGrid[7]) || !isValidEdgePair(dGrid[5], rGrid[7])) continue
                        for (lRot in 0 until 4) {
                            val lGrid = rotatedVariants[4][lRot]
                            if (!isValidEdgePair(uGrid[3], lGrid[1]) ||
                                !isValidEdgePair(fGrid[3], lGrid[5]) ||
                                !isValidEdgePair(dGrid[3], lGrid[7])
                            ) continue
                            for (bRot in 0 until 4) {
                                val bGrid = rotatedVariants[5][bRot]
                                val candidate = CubeState(uGrid + rGrid + fGrid + dGrid + lGrid + bGrid)
                                if (isPhysicallyValid(candidate)) {
                                    return CubeValidationResult.Valid(candidate)
                                }
                            }
                        }
                    }
                }
            }
        }

        return CubeValidationResult.Invalid(
            viReason = "Một số ô màu bị nhầm do chói sáng hoặc lệch góc (khối 3x3 không hợp lệ). Bé hãy quay video chậm lại nhé!",
            enReason = "Colors did not form a valid 3x3 cube. Please record again slowly under clear light!"
        )
    }

    fun isPhysicallyValid(cube: CubeState): Boolean {
        // 1. Check centers (U=Yellow, R=Orange, F=Green, D=White, L=Red, B=Blue)
        if (cube.colorAt(4) != CubeColor.YELLOW ||
            cube.colorAt(13) != CubeColor.ORANGE ||
            cube.colorAt(22) != CubeColor.GREEN ||
            cube.colorAt(31) != CubeColor.WHITE ||
            cube.colorAt(40) != CubeColor.RED ||
            cube.colorAt(49) != CubeColor.BLUE
        ) return false

        // 2. Check 12 edges permutation and orientation parity
        val edgePerm = IntArray(12)
        val edgeSeen = BooleanArray(12)
        var edgeOriSum = 0
        for (i in 0 until 12) {
            val (aIdx, bIdx) = EDGE_INDICES[i]
            val ca = cube.colorAt(aIdx)
            val cb = cube.colorAt(bIdx)
            var matched = -1
            var flip = 0
            for (j in 0 until 12) {
                val (sa, sb) = SOLVED_EDGE_COLORS[j]
                if (ca == sa && cb == sb) {
                    matched = j; flip = 0; break
                } else if (ca == sb && cb == sa) {
                    matched = j; flip = 1; break
                }
            }
            if (matched == -1 || edgeSeen[matched]) return false
            edgeSeen[matched] = true
            edgePerm[i] = matched
            edgeOriSum += flip
        }
        if (edgeOriSum % 2 != 0) return false

        // 3. Check 8 corners permutation and twist parity
        val cornerPerm = IntArray(8)
        val cornerSeen = BooleanArray(8)
        var cornerOriSum = 0
        for (i in 0 until 8) {
            val (i0, i1, i2) = CORNER_INDICES[i]
            val c0 = cube.colorAt(i0)
            val c1 = cube.colorAt(i1)
            val c2 = cube.colorAt(i2)
            var matched = -1
            var twist = 0
            for (j in 0 until 8) {
                val (s0, s1, s2) = SOLVED_CORNER_COLORS[j]
                if (c0 == s0 && c1 == s1 && c2 == s2) {
                    matched = j; twist = 0; break
                } else if (c0 == s2 && c1 == s0 && c2 == s1) {
                    matched = j; twist = 1; break
                } else if (c0 == s1 && c1 == s2 && c2 == s0) {
                    matched = j; twist = 2; break
                }
            }
            if (matched == -1 || cornerSeen[matched]) return false
            cornerSeen[matched] = true
            cornerPerm[i] = matched
            cornerOriSum += twist
        }
        if (cornerOriSum % 3 != 0) return false

        // 4. Permutation parity: edge parity == corner parity
        return permutationParity(edgePerm) == permutationParity(cornerPerm)
    }

    private fun isValidEdgePair(a: CubeColor, b: CubeColor): Boolean {
        if (a == b) return false
        return when (a) {
            CubeColor.YELLOW -> b != CubeColor.WHITE
            CubeColor.WHITE -> b != CubeColor.YELLOW
            CubeColor.RED -> b != CubeColor.ORANGE
            CubeColor.ORANGE -> b != CubeColor.RED
            CubeColor.BLUE -> b != CubeColor.GREEN
            CubeColor.GREEN -> b != CubeColor.BLUE
        }
    }

    private fun permutationParity(perm: IntArray): Int {
        var inversions = 0
        for (i in perm.indices) {
            for (j in i + 1 until perm.size) {
                if (perm[i] > perm[j]) inversions++
            }
        }
        return inversions % 2
    }

    fun rotateGridCW(grid: List<CubeColor>, times: Int): List<CubeColor> {
        var cur = grid
        repeat(((times % 4) + 4) % 4) {
            cur = listOf(
                cur[6], cur[3], cur[0],
                cur[7], cur[4], cur[1],
                cur[8], cur[5], cur[2]
            )
        }
        return cur
    }
}
