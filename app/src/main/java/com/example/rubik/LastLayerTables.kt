package com.example.rubik

/**
 * Full 57-case OLL and 21-case PLL solver using rotational pattern matching (U0..U3 + AUF).
 */
object LastLayerTables {

    data class OllCase(val id: String, val name: String, val alg: String)
    data class PllCase(val id: String, val name: String, val alg: String)

    data class LastLayerMatch(
        val caseName: String,
        val moves: List<CubeMove>
    )

    // Top-layer sticker indices around U (viewed from U, F at bottom):
    // U face: 0..8, plus 12 side stickers:
    // B(47,46,45), R(11,10,9), F(20,19,18), L(38,37,36)
    private val OLL_MASK_INDICES = intArrayOf(
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        47, 46, 45, 11, 10, 9, 20, 19, 18, 38, 37, 36
    )

    // 12 side stickers in clockwise ring around U: F(18,19,20), R(9,10,11), B(45,46,47), L(36,37,38)
    // Wait: looking down at U clockwise:
    // F top: left->right is 18,19,20 -> right face front->back is 9,10,11 -> back face right->left is 45,46,47 -> left face back->front is 36,37,38
    private val PLL_RING_INDICES = intArrayOf(
        18, 19, 20,
        9, 10, 11,
        45, 46, 47,
        36, 37, 38
    )

    private val OLL_CASES = listOf(
        OllCase("OLL 1", "Runway (Dot)", "R U2 R2 F R F' U2 R' F R F'"),
        OllCase("OLL 2", "Zamboni (Dot)", "F R U R' U' F' f R U R' U' f'"),
        OllCase("OLL 3", "Anti-Mouse (Dot)", "f R U R' U' f' U' F R U R' U' F'"),
        OllCase("OLL 4", "Mouse (Dot)", "f R U R' U' f' U F R U R' U' F'"),
        OllCase("OLL 5", "Right Back Fat Sune", "r' U2 R U R' U r"),
        OllCase("OLL 6", "Right Front Fat Sune", "r U2 R' U' R U' r'"),
        OllCase("OLL 7", "Lightning Bolt", "r U R' U R U2 r'"),
        OllCase("OLL 8", "Left Lightning", "r' U' R U' R' U2 r"),
        OllCase("OLL 9", "Kite", "R U R' U' R' F R2 U R' U' F'"),
        OllCase("OLL 10", "Anti-Kite", "R U R' U R' F R F' R U2 R'"),
        OllCase("OLL 11", "Downstairs", "r U R' U R' F R F' R U2 r'"),
        OllCase("OLL 12", "Upstairs", "M' R' U' R U' R' U2 R U' R r'"),
        OllCase("OLL 13", "Gun", "F U R U' R2 F' R U R U' R'"),
        OllCase("OLL 14", "Anti-Gun", "R' F R U R' F' R F U' F'"),
        OllCase("OLL 15", "Squeegee", "r' U' r R' U' R U r' U r"),
        OllCase("OLL 16", "Anti-Squeegee", "r U r' R U R' U' r U' r'"),
        OllCase("OLL 17", "Slash (Dot)", "R U R' U R' F R F' U2 R' F R F'"),
        OllCase("OLL 18", "Crown (Dot)", "r U R' U R U2 r2 U' R U' R' U2 r"),
        OllCase("OLL 19", "Bunny (Dot)", "r' R U R U R' U' r R2 F R F'"),
        OllCase("OLL 20", "X Checkers (Dot)", "r U R' U' M2 U R U' R' U' M'"),
        OllCase("OLL 21", "H (Cross)", "R U2 R' U' R U R' U' R U' R'"),
        OllCase("OLL 22", "Pi (Cross)", "R U2 R2 U' R2 U' R2 U2 R"),
        OllCase("OLL 23", "Headlights (Cross)", "R2 D R' U2 R D' R' U2 R'"),
        OllCase("OLL 24", "Chameleon (Cross)", "r U R' U' r' F R F'"),
        OllCase("OLL 25", "Bowtie (Cross)", "F' r U R' U' r' F R"),
        OllCase("OLL 26", "Antisune (Cross)", "R U2 R' U' R U' R'"),
        OllCase("OLL 27", "Sune (Con Cá)", "R U R' U R U2 R'"),
        OllCase("OLL 28", "Stealth", "r U R' U' M U R U' R'"),
        OllCase("OLL 29", "Awkward Chameleon", "R U R' U' R U' R' F' U' F R U R'"),
        OllCase("OLL 30", "Anti-Awkward", "F R' F R2 U' R' U' R U R' F2"),
        OllCase("OLL 31", "Couch", "R' U' F U R U' R' F' R"),
        OllCase("OLL 32", "Anti-Couch", "L U F' U' L' U L F L'"),
        OllCase("OLL 33", "Key / Chữ T", "R U R' U' R' F R F'"),
        OllCase("OLL 34", "City", "R U R2 U' R' F R U R U' F'"),
        OllCase("OLL 35", "Fish Salad", "R U2 R2 F R F' R U2 R'"),
        OllCase("OLL 36", "Sea Mew", "L' U' L U' L' U L U L F' L' F"),
        OllCase("OLL 37", "Mounted Fish", "F R' F' R U R U' R'"),
        OllCase("OLL 38", "Mario", "R U R' U R U' R' U' R' F R F'"),
        OllCase("OLL 39", "Fung", "L F' L' U' L U F U' L'"),
        OllCase("OLL 40", "Anti-Fung", "R' F R U R' U' F' U R"),
        OllCase("OLL 41", "Awkward Fish", "R U R' U R U2 R' F R U R' U' F'"),
        OllCase("OLL 42", "Left Awkward Fish", "R' U' R U' R' U2 R F R U R' U' F'"),
        OllCase("OLL 43", "Anti-P", "F' U' L' U L F"),
        OllCase("OLL 44", "P Shape", "F U R U' R' F'"),
        OllCase("OLL 45", "T Shape (Chữ T)", "F R U R' U' F'"),
        OllCase("OLL 46", "C Shape", "R' U' R' F R F' U R"),
        OllCase("OLL 47", "Small L", "F' L' U' L U L' U' L U F"),
        OllCase("OLL 48", "Breakneck", "F R U R' U' R U R' U' F'"),
        OllCase("OLL 49", "Right Back Squeezy", "r U' r2 U r2 U r2 U' r"),
        OllCase("OLL 50", "Right Front Squeezy", "r' U r2 U' r2 U' r2 U r'"),
        OllCase("OLL 51", "Bottlecap", "f R U R' U' R U R' U' f'"),
        OllCase("OLL 52", "Rice Cooker", "R U R' U R d' R U' R' F'"),
        OllCase("OLL 53", "Frying Pan", "r' U' R U' R' U R U' R' U2 r"),
        OllCase("OLL 54", "Anti-Frying Pan", "r U R' U R U' R' U R U2 r'"),
        OllCase("OLL 55", "Highway", "R U2 R2 U' R U' R' U2 F R F'"),
        OllCase("OLL 56", "Streetlights", "r U r' U R U' R' U R U' R' r U' r'"),
        OllCase("OLL 57", "H Shape (Chữ H)", "R U R' U' M' U R U' r'")
    )

    private val PLL_CASES = listOf(
        PllCase("Aa", "Aa-Perm", "x R' U R' D2 R U' R' D2 R2 x'"),
        PllCase("Ab", "Ab-Perm", "x R2 D2 R U R' D2 R U' R x'"),
        PllCase("E", "E-Perm", "x' R U' R' D R U R' D' R U R' D R U' R' D' x"),
        PllCase("F", "F-Perm", "R' U' F' R U R' U' R' F R2 U' R' U' R U R' U R"),
        PllCase("Ga", "Ga-Perm", "R2 U R' U R' U' R U' R2 D U' R' U R D'"),
        PllCase("Gb", "Gb-Perm", "R' U' R U D' R2 U R' U R U' R U' R2 D"),
        PllCase("Gc", "Gc-Perm", "R2 U' R U' R U R' U R2 D' U R U' R' D"),
        PllCase("Gd", "Gd-Perm", "R U R' U' D R2 U' R U' R' U R' U R2 D'"),
        PllCase("H", "H-Perm (Đổi Chéo)", "M2 U M2 U2 M2 U M2"),
        PllCase("Ja", "Ja-Perm", "R' U L' U2 R U' R' U2 R L"),
        PllCase("Jb", "Jb-Perm", "R U R' F' R U R' U' R' F R2 U' R'"),
        PllCase("Na", "Na-Perm", "R U R' U R U R' F' R U R' U' R' F R2 U' R' U2 R U' R'"),
        PllCase("Nb", "Nb-Perm", "R' U R U' R' F' U' F R U R' F R' F' R U' R"),
        PllCase("Ra", "Ra-Perm", "R U' R' U' R U R D R' U' R D' R' U2 R'"),
        PllCase("Rb", "Rb-Perm", "R' U2 R U2 R' F R U R' U' R' F' R2"),
        PllCase("T", "T-Perm (Chữ T)", "R U R' U' R' F R2 U' R' U' R U R' F'"),
        PllCase("Ua", "Ua-Perm (3 Cạnh)", "R U' R U R U R U' R' U' R2"),
        PllCase("Ub", "Ub-Perm (3 Cạnh)", "R2 U R U R' U' R' U' R' U R'"),
        PllCase("V", "V-Perm", "R' U R' U' y R' F' R2 U' R' U R' F R F"),
        PllCase("Y", "Y-Perm", "F R U' R' U' R U R' F' R U R' U' R' F R F'"),
        PllCase("Z", "Z-Perm", "M' U M2 U M2 U M' U2 M2")
    )

    // Precomputed lookup map: 21-bit Yellow U-mask -> Pair(OllCase, list of U-aligned moves)
    private val ollMaskMap: Map<Int, Pair<String, List<CubeMove>>> by lazy { buildOllLookup() }

    // Precomputed lookup map: canonical relative side pattern -> Pair(PllName, moves)
    private val pllPatternMap: Map<String, Pair<String, List<CubeMove>>> by lazy { buildPllLookup() }

    fun solveOll(cube: CubeState): LastLayerMatch {
        val mask = computeOllMask(cube)
        if (mask == 0b111111111_000000000000) {
            return LastLayerMatch("OLL Skip!", emptyList())
        }
        val entry = ollMaskMap[mask]
        if (entry != null) {
            return LastLayerMatch(entry.first, entry.second)
        }
        // Fallback 2-look OLL if any unexpected case occurs
        return solveTwoLookOll(cube)
    }

    fun solvePll(cube: CubeState): LastLayerMatch {
        if (cube.isSolved()) {
            return LastLayerMatch("PLL Skip!", emptyList())
        }
        // Check pure AUF first
        for (auf in listOf(CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)) {
            if (cube.applyMove(auf).isSolved()) {
                return LastLayerMatch("AUF Skip!", listOf(auf))
            }
        }
        // Try matching 4 U pre-rotations
        val uMoves = listOf(null, CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)
        for (uPre in uMoves) {
            val rotated = if (uPre == null) cube else cube.applyMove(uPre)
            val key = computePllCanonicalKey(rotated)
            val match = pllPatternMap[key]
            if (match != null) {
                val postAuf = findAuf(rotated.applyMoves(match.second))
                val full = mutableListOf<CubeMove>()
                if (uPre != null) full.add(uPre)
                full.addAll(match.second)
                if (postAuf != null) full.add(postAuf)
                return LastLayerMatch(match.first, CubeState.simplifyMoves(full))
            }
        }
        return LastLayerMatch("PLL", emptyList())
    }

    private fun findAuf(cube: CubeState): CubeMove? {
        if (cube.isSolved()) return null
        for (m in listOf(CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)) {
            if (cube.applyMove(m).isSolved()) return m
        }
        return null
    }

    private fun computeOllMask(cube: CubeState): Int {
        var mask = 0
        for (i in OLL_MASK_INDICES.indices) {
            if (cube.colorAt(OLL_MASK_INDICES[i]) == CubeColor.YELLOW) {
                mask = mask or (1 shl (OLL_MASK_INDICES.size - 1 - i))
            }
        }
        return mask
    }

    private fun computePllCanonicalKey(cube: CubeState): String {
        val sb = StringBuilder(12)
        val map = HashMap<CubeColor, Char>(4)
        var nextCode = '0'
        for (idx in PLL_RING_INDICES) {
            val c = cube.colorAt(idx)
            val ch = map.getOrPut(c) { nextCode++ }
            sb.append(ch)
        }
        return sb.toString()
    }

    private fun buildOllLookup(): Map<Int, Pair<String, List<CubeMove>>> {
        val map = HashMap<Int, Pair<String, List<CubeMove>>>()
        val solved = CubeState.solved()
        val uMoves = listOf(null, CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)

        for (oll in OLL_CASES) {
            val algMoves = expandWideAndRotations(oll.alg)
            val invMoves = algMoves.reversed().map { it.inverse() }
            val baseCaseCube = solved.applyMoves(invMoves)

            for (uPre in uMoves) {
                val orientedCube = if (uPre == null) baseCaseCube else baseCaseCube.applyMove(uPre)
                val mask = computeOllMask(orientedCube)
                if (mask !in map) {
                    val solveMoves = if (uPre == null) algMoves else listOf(uPre.inverse()) + algMoves
                    map[mask] = "${oll.id} (${oll.name})" to CubeState.simplifyMoves(solveMoves)
                }
            }
        }
        return map
    }

    private fun buildPllLookup(): Map<String, Pair<String, List<CubeMove>>> {
        val map = HashMap<String, Pair<String, List<CubeMove>>>()
        val solved = CubeState.solved()

        for (pll in PLL_CASES) {
            val algMoves = expandWideAndRotations(pll.alg)
            val invMoves = algMoves.reversed().map { it.inverse() }
            val caseCube = solved.applyMoves(invMoves)
            val key = computePllCanonicalKey(caseCube)
            map[key] = pll.name to algMoves
        }
        return map
    }

    private fun solveTwoLookOll(cube: CubeState): LastLayerMatch {
        // Fallback: orient edges then match OLL map
        var cur = cube
        val moves = mutableListOf<CubeMove>()
        val uEdges = intArrayOf(1, 5, 7, 3)
        val yellowEdges = uEdges.count { cur.colorAt(it) == CubeColor.YELLOW }
        if (yellowEdges < 4) {
            val edgeAlg = when (yellowEdges) {
                0 -> expandWideAndRotations("F R U R' U' F' f R U R' U' f'")
                2 -> {
                    // Check line vs L-shape
                    if ((cur.colorAt(3) == CubeColor.YELLOW && cur.colorAt(5) == CubeColor.YELLOW) ||
                        (cur.colorAt(1) == CubeColor.YELLOW && cur.colorAt(7) == CubeColor.YELLOW)
                    ) {
                        val pre = if (cur.colorAt(1) == CubeColor.YELLOW) listOf(CubeMove.U) else emptyList()
                        pre + expandWideAndRotations("F R U R' U' F'")
                    } else {
                        var pre: CubeMove? = null
                        for (u in listOf(null, CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)) {
                            val test = if (u == null) cur else cur.applyMove(u)
                            if (test.colorAt(1) == CubeColor.YELLOW && test.colorAt(3) == CubeColor.YELLOW) {
                                pre = u; break
                            }
                        }
                        val prefix = if (pre != null) listOf(pre) else emptyList()
                        prefix + expandWideAndRotations("f R U R' U' f'")
                    }
                }
                else -> emptyList()
            }
            moves.addAll(edgeAlg)
            cur = cur.applyMoves(edgeAlg)
        }
        val second = ollMaskMap[computeOllMask(cur)]
        if (second != null) {
            moves.addAll(second.second)
            return LastLayerMatch(second.first, CubeState.simplifyMoves(moves))
        }
        return LastLayerMatch("OLL", CubeState.simplifyMoves(moves))
    }

    /**
     * Converts wide moves (r, l, f, d), slice moves (M, M', M2), and cube rotations (x, y, z)
     * into pure outer-layer moves (U, R, F, D, L, B) so the standard 6-face move engine
     * keeps D/U centers properly anchored without losing orientation!
     */
    fun expandWideAndRotations(alg: String): List<CubeMove> {
        val tokens = alg.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        // Track current frame orientation: maps local face (U,R,F,D,L,B) -> global MoveFace
        var frame = mapOf(
            'U' to MoveFace.U,
            'R' to MoveFace.R,
            'F' to MoveFace.F,
            'D' to MoveFace.D,
            'L' to MoveFace.L,
            'B' to MoveFace.B
        )
        val out = mutableListOf<CubeMove>()

        fun emit(localFace: Char, q: Int) {
            val globalFace = frame[localFace]!!
            out.add(CubeState.moveFor(globalFace, q))
        }

        fun rotateFrameX(q: Int) {
            repeat(((q % 4) + 4) % 4) {
                frame = mapOf(
                    'U' to frame['F']!!,
                    'F' to frame['D']!!,
                    'D' to frame['B']!!,
                    'B' to frame['U']!!,
                    'R' to frame['R']!!,
                    'L' to frame['L']!!
                )
            }
        }

        fun rotateFrameY(q: Int) {
            repeat(((q % 4) + 4) % 4) {
                frame = mapOf(
                    'U' to frame['U']!!,
                    'D' to frame['D']!!,
                    'F' to frame['R']!!,
                    'R' to frame['B']!!,
                    'B' to frame['L']!!,
                    'L' to frame['F']!!
                )
            }
        }

        fun rotateFrameZ(q: Int) {
            repeat(((q % 4) + 4) % 4) {
                frame = mapOf(
                    'F' to frame['F']!!,
                    'B' to frame['B']!!,
                    'U' to frame['L']!!,
                    'R' to frame['U']!!,
                    'D' to frame['R']!!,
                    'L' to frame['D']!!
                )
            }
        }

        for (tok in tokens) {
            val base = tok[0]
            val q = when {
                tok.endsWith("2") -> 2
                tok.endsWith("'") -> 3
                else -> 1
            }
            when (base) {
                'U', 'R', 'F', 'D', 'L', 'B' -> emit(base, q)
                'r' -> { emit('L', q); rotateFrameX(q) }
                'l' -> { emit('R', q); rotateFrameX(4 - q) }
                'f' -> { emit('B', q); rotateFrameZ(q) }
                'd' -> { emit('U', q); rotateFrameY(4 - q) }
                'M' -> { emit('R', q); emit('L', 4 - q); rotateFrameX(4 - q) }
                'x' -> rotateFrameX(q)
                'y' -> rotateFrameY(q)
                'z' -> rotateFrameZ(q)
            }
        }
        return CubeState.simplifyMoves(out)
    }
}
