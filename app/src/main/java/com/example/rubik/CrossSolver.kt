package com.example.rubik

/**
 * Optimal fingertrick-aware White Cross solver on Down (D) face using IDA*
 * with exact 2-edge-pair BFS distance tables.
 */
object CrossSolver {

    // White Cross edges in solved state:
    // Slot 4 = DF (28=White, 25=Blue)
    // Slot 5 = DR (32=White, 16=Red)
    // Slot 6 = DB (34=White, 52=Green)
    // Slot 7 = DL (30=White, 43=Orange)
    private val EDGE_FACELETS = arrayOf(
        1 to 46,  // 0: UB
        5 to 10,  // 1: UR
        7 to 19,  // 2: UF
        3 to 37,  // 3: UL
        28 to 25, // 4: DF (White-Blue)
        32 to 16, // 5: DR (White-Red)
        34 to 52, // 6: DB (White-Green)
        30 to 43, // 7: DL (White-Orange)
        23 to 12, // 8: FR
        21 to 41, // 9: FL
        48 to 14, // 10: BR
        50 to 39  // 11: BL
    )

    private val SOLVED_POSE_DF = 4 * 2 // 8
    private val SOLVED_POSE_DR = 5 * 2 // 10
    private val SOLVED_POSE_DB = 6 * 2 // 12
    private val SOLVED_POSE_DL = 7 * 2 // 14

    // edgeTrans[pose 0..23][moveOrdinal 0..17] -> new pose 0..23
    private val edgeTrans: Array<IntArray> by lazy { buildEdgeTransitions() }

    // Exact minimum moves to solve (DF, DR) -> 24 * 24 = 576
    private val distDFDR: IntArray by lazy { buildPairDistanceTable(SOLVED_POSE_DF, SOLVED_POSE_DR) }

    // Exact minimum moves to solve (DB, DL) -> 24 * 24 = 576
    private val distDBDL: IntArray by lazy { buildPairDistanceTable(SOLVED_POSE_DB, SOLVED_POSE_DL) }

    data class CrossResult(
        val moves: List<CubeMove>,
        val fingertrickCost: Int
    )

    fun solveOptimalCross(cube: CubeState): CrossResult {
        val df = findEdgePose(cube, CubeColor.WHITE, CubeColor.GREEN)
        val dr = findEdgePose(cube, CubeColor.WHITE, CubeColor.ORANGE)
        val db = findEdgePose(cube, CubeColor.WHITE, CubeColor.BLUE)
        val dl = findEdgePose(cube, CubeColor.WHITE, CubeColor.RED)

        if (df == SOLVED_POSE_DF && dr == SOLVED_POSE_DR && db == SOLVED_POSE_DB && dl == SOLVED_POSE_DL) {
            return CrossResult(emptyList(), 0)
        }

        val minH = heuristic(df, dr, db, dl)
        var bestMoves: List<CubeMove>? = null
        var bestCost = Int.MAX_VALUE
        var minSolutionDepth = -1

        val maxSearchDepth = 8
        for (depth in minH..maxSearchDepth) {
            // If we already found a great fingertrick-friendly solution at minSolutionDepth, stop.
            // Otherwise allow 1 extra move only to replace awkward B-moves with R/U/F/L/D flow.
            if (minSolutionDepth != -1 && depth > minSolutionDepth + 1) break
            if (minSolutionDepth != -1 && bestCost <= minSolutionDepth * 12) break

            val path = ArrayList<CubeMove>(depth)
            search(
                df = df, dr = dr, db = db, dl = dl,
                remaining = depth,
                lastFace = null,
                secondLastFace = null,
                path = path,
                onSolution = { candidate ->
                    if (minSolutionDepth == -1) minSolutionDepth = candidate.size
                    val cost = computeFingertrickScore(candidate)
                    if (bestMoves == null ||
                        candidate.size < bestMoves!!.size && cost <= bestCost + 10 ||
                        (candidate.size <= bestMoves!!.size + 1 && cost < bestCost - 8) ||
                        (candidate.size == bestMoves!!.size && cost < bestCost)
                    ) {
                        bestMoves = candidate.toList()
                        bestCost = cost
                    }
                }
            )
        }

        val chosen = bestMoves ?: emptyList()
        return CrossResult(chosen, if (chosen.isEmpty()) 0 else bestCost)
    }

    private fun search(
        df: Int, dr: Int, db: Int, dl: Int,
        remaining: Int,
        lastFace: MoveFace?,
        secondLastFace: MoveFace?,
        path: MutableList<CubeMove>,
        onSolution: (List<CubeMove>) -> Unit
    ) {
        val h = heuristic(df, dr, db, dl)
        if (h > remaining) return
        if (remaining == 0) {
            if (df == SOLVED_POSE_DF && dr == SOLVED_POSE_DR && db == SOLVED_POSE_DB && dl == SOLVED_POSE_DL) {
                onSolution(path)
            }
            return
        }

        for (move in CubeMove.entries) {
            val face = move.face
            if (face == lastFace) continue
            // Commutative opposite-face canonicalization: U before D, R before L, F before B
            if (lastFace != null && isOppositeCanonicalSkip(lastFace, face)) continue
            if (secondLastFace == face && isOpposite(face, lastFace)) continue

            val mOrd = move.ordinal
            val nDf = edgeTrans[df][mOrd]
            val nDr = edgeTrans[dr][mOrd]
            val nDb = edgeTrans[db][mOrd]
            val nDl = edgeTrans[dl][mOrd]

            path.add(move)
            search(nDf, nDr, nDb, nDl, remaining - 1, face, lastFace, path, onSolution)
            path.removeAt(path.lastIndex)
        }
    }

    private fun heuristic(df: Int, dr: Int, db: Int, dl: Int): Int {
        val h1 = distDFDR[df * 24 + dr]
        val h2 = distDBDL[db * 24 + dl]
        return maxOf(h1, h2)
    }

    private fun computeFingertrickScore(moves: List<CubeMove>): Int {
        var score = 0
        var prev: CubeMove? = null
        for (m in moves) {
            score += m.fingertrickCost
            // Reward smooth R/U or L/U alternating triggers
            if (prev != null) {
                val pair = prev.face to m.face
                if (pair == (MoveFace.R to MoveFace.U) || pair == (MoveFace.U to MoveFace.R) ||
                    pair == (MoveFace.L to MoveFace.U) || pair == (MoveFace.U to MoveFace.L) ||
                    pair == (MoveFace.R to MoveFace.D) || pair == (MoveFace.D to MoveFace.R)
                ) {
                    score -= 2
                }
            }
            prev = m
        }
        return score
    }

    private fun isOppositeCanonicalSkip(prev: MoveFace, curr: MoveFace): Boolean = when (prev) {
        MoveFace.D -> curr == MoveFace.U
        MoveFace.L -> curr == MoveFace.R
        MoveFace.B -> curr == MoveFace.F
        else -> false
    }

    private fun isOpposite(a: MoveFace, b: MoveFace?): Boolean = when (a) {
        MoveFace.U -> b == MoveFace.D
        MoveFace.D -> b == MoveFace.U
        MoveFace.R -> b == MoveFace.L
        MoveFace.L -> b == MoveFace.R
        MoveFace.F -> b == MoveFace.B
        MoveFace.B -> b == MoveFace.F
    }

    private fun findEdgePose(cube: CubeState, cPrimary: CubeColor, cSecondary: CubeColor): Int {
        for (slot in 0 until 12) {
            val (a, b) = EDGE_FACELETS[slot]
            val ca = cube.colorAt(a)
            val cb = cube.colorAt(b)
            if (ca == cPrimary && cb == cSecondary) return slot * 2
            if (ca == cSecondary && cb == cPrimary) return slot * 2 + 1
        }
        return 0
    }

    private fun buildEdgeTransitions(): Array<IntArray> {
        val table = Array(24) { IntArray(CubeMove.entries.size) }
        val solved = CubeState.solved()
        for (slot in 0 until 12) {
            val (a, b) = EDGE_FACELETS[slot]
            val primaryColor = solved.colorAt(a)
            val secondaryColor = solved.colorAt(b)
            for (move in CubeMove.entries) {
                val moved = solved.applyMove(move)
                val newPose = findEdgePose(moved, primaryColor, secondaryColor)
                table[slot * 2][move.ordinal] = newPose
                table[slot * 2 + 1][move.ordinal] = newPose xor 1
            }
        }
        return table
    }

    private fun buildPairDistanceTable(goalA: Int, goalB: Int): IntArray {
        val dist = IntArray(24 * 24) { -1 }
        val queue = IntArray(24 * 24)
        var head = 0
        var tail = 0
        val startIdx = goalA * 24 + goalB
        dist[startIdx] = 0
        queue[tail++] = startIdx

        while (head < tail) {
            val state = queue[head++]
            val d = dist[state]
            val a = state / 24
            val b = state % 24
            for (mOrd in 0 until CubeMove.entries.size) {
                val na = edgeTrans[a][mOrd]
                val nb = edgeTrans[b][mOrd]
                val nextIdx = na * 24 + nb
                if (dist[nextIdx] == -1) {
                    dist[nextIdx] = d + 1
                    queue[tail++] = nextIdx
                }
            }
        }
        return dist
    }
}
