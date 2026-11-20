package com.example.rubik

import kotlin.random.Random

/**
 * Immutable 54-facelet 3x3 Rubik's Cube representation.
 * Indices: U(0..8), R(9..17), F(18..26), D(27..35), L(36..44), B(45..53).
 * Standard CFOP orientation: U=Yellow, R=Red, F=Blue, D=White, L=Orange, B=Green.
 */
data class CubeState(val facelets: List<CubeColor>) {

    init {
        require(facelets.size == 54) { "CubeState requires exactly 54 facelets" }
    }

    fun colorAt(index: Int): CubeColor = facelets[index]

    fun faceColors(face: FaceId): List<CubeColor> {
        val start = face.index * 9
        return facelets.subList(start, start + 9)
    }

    fun isSolved(): Boolean {
        for (face in 0 until 6) {
            val base = face * 9
            val center = facelets[base + 4]
            for (i in 0 until 9) {
                if (facelets[base + i] != center) return false
            }
        }
        return true
    }

    fun applyMove(move: CubeMove): CubeState {
        val next = facelets.toMutableList()
        val times = move.quarterTurns
        repeat(times) {
            applyQuarterTurnInPlace(next, move.face)
        }
        return CubeState(next)
    }

    fun applyMoves(moves: List<CubeMove>): CubeState {
        var cur = this
        for (m in moves) {
            cur = cur.applyMove(m)
        }
        return cur
    }

    companion object {
        fun solved(): CubeState {
            val list = MutableList(54) { idx ->
                when (idx / 9) {
                    0 -> CubeColor.YELLOW // U
                    1 -> CubeColor.ORANGE // R
                    2 -> CubeColor.GREEN  // F
                    3 -> CubeColor.WHITE  // D
                    4 -> CubeColor.RED    // L
                    else -> CubeColor.BLUE // B
                }
            }
            return CubeState(list)
        }

        fun generateScramble(length: Int = 20, random: Random = Random.Default): List<CubeMove> {
            val faces = MoveFace.entries
            val moves = mutableListOf<CubeMove>()
            var lastFace: MoveFace? = null
            var secondLastFace: MoveFace? = null
            while (moves.size < length) {
                val face = faces[random.nextInt(faces.size)]
                if (face == lastFace) continue
                if (secondLastFace == face && isOpposite(face, lastFace)) continue
                val suffix = random.nextInt(3) // 0=1, 1=3('), 2=2
                val move = when (face) {
                    MoveFace.U -> listOf(CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)[suffix]
                    MoveFace.R -> listOf(CubeMove.R, CubeMove.R_PRIME, CubeMove.R2)[suffix]
                    MoveFace.F -> listOf(CubeMove.F, CubeMove.F_PRIME, CubeMove.F2)[suffix]
                    MoveFace.D -> listOf(CubeMove.D, CubeMove.D_PRIME, CubeMove.D2)[suffix]
                    MoveFace.L -> listOf(CubeMove.L, CubeMove.L_PRIME, CubeMove.L2)[suffix]
                    MoveFace.B -> listOf(CubeMove.B, CubeMove.B_PRIME, CubeMove.B2)[suffix]
                }
                moves.add(move)
                secondLastFace = lastFace
                lastFace = face
            }
            return moves
        }

        private fun isOpposite(a: MoveFace, b: MoveFace?): Boolean = when (a) {
            MoveFace.U -> b == MoveFace.D
            MoveFace.D -> b == MoveFace.U
            MoveFace.R -> b == MoveFace.L
            MoveFace.L -> b == MoveFace.R
            MoveFace.F -> b == MoveFace.B
            MoveFace.B -> b == MoveFace.F
        }

        fun simplifyMoves(moves: List<CubeMove>): List<CubeMove> {
            val stack = mutableListOf<CubeMove>()
            for (m in moves) {
                if (stack.isNotEmpty() && stack.last().face == m.face) {
                    val prev = stack.removeAt(stack.lastIndex)
                    val combined = (prev.quarterTurns + m.quarterTurns) % 4
                    if (combined != 0) {
                        stack.add(moveFor(m.face, combined))
                    }
                } else {
                    stack.add(m)
                }
            }
            return stack
        }

        fun moveFor(face: MoveFace, quarterTurns: Int): CubeMove {
            val q = ((quarterTurns % 4) + 4) % 4
            return when (face) {
                MoveFace.U -> when (q) { 1 -> CubeMove.U; 2 -> CubeMove.U2; else -> CubeMove.U_PRIME }
                MoveFace.R -> when (q) { 1 -> CubeMove.R; 2 -> CubeMove.R2; else -> CubeMove.R_PRIME }
                MoveFace.F -> when (q) { 1 -> CubeMove.F; 2 -> CubeMove.F2; else -> CubeMove.F_PRIME }
                MoveFace.D -> when (q) { 1 -> CubeMove.D; 2 -> CubeMove.D2; else -> CubeMove.D_PRIME }
                MoveFace.L -> when (q) { 1 -> CubeMove.L; 2 -> CubeMove.L2; else -> CubeMove.L_PRIME }
                MoveFace.B -> when (q) { 1 -> CubeMove.B; 2 -> CubeMove.B2; else -> CubeMove.B_PRIME }
            }
        }

        private fun applyQuarterTurnInPlace(arr: MutableList<CubeColor>, face: MoveFace) {
            val base = face.indexOffset()
            val c0 = arr[base + 0]
            val c2 = arr[base + 2]
            val c8 = arr[base + 8]
            val c6 = arr[base + 6]
            arr[base + 2] = c0
            arr[base + 8] = c2
            arr[base + 6] = c8
            arr[base + 0] = c6

            val e1 = arr[base + 1]
            val e5 = arr[base + 5]
            val e7 = arr[base + 7]
            val e3 = arr[base + 3]
            arr[base + 5] = e1
            arr[base + 7] = e5
            arr[base + 3] = e7
            arr[base + 1] = e3

            when (face) {
                MoveFace.U -> {
                    cycle3(arr, 18, 19, 20, 36, 37, 38, 45, 46, 47, 9, 10, 11)
                }
                MoveFace.D -> {
                    cycle3(arr, 24, 25, 26, 15, 16, 17, 51, 52, 53, 42, 43, 44)
                }
                MoveFace.R -> {
                    cycle3(arr, 20, 23, 26, 2, 5, 8, 51, 48, 45, 29, 32, 35)
                }
                MoveFace.L -> {
                    cycle3(arr, 0, 3, 6, 18, 21, 24, 27, 30, 33, 53, 50, 47)
                }
                MoveFace.F -> {
                    cycle3(arr, 6, 7, 8, 9, 12, 15, 29, 28, 27, 44, 41, 38)
                }
                MoveFace.B -> {
                    cycle3(arr, 2, 1, 0, 36, 39, 42, 33, 34, 35, 17, 14, 11)
                }
            }
        }

        private fun MoveFace.indexOffset(): Int = when (this) {
            MoveFace.U -> 0
            MoveFace.R -> 9
            MoveFace.F -> 18
            MoveFace.D -> 27
            MoveFace.L -> 36
            MoveFace.B -> 45
        }

        private fun cycle3(
            arr: MutableList<CubeColor>,
            a0: Int, a1: Int, a2: Int,
            b0: Int, b1: Int, b2: Int,
            c0: Int, c1: Int, c2: Int,
            d0: Int, d1: Int, d2: Int
        ) {
            val t0 = arr[d0]
            val t1 = arr[d1]
            val t2 = arr[d2]
            arr[d0] = arr[c0]; arr[d1] = arr[c1]; arr[d2] = arr[c2]
            arr[c0] = arr[b0]; arr[c1] = arr[b1]; arr[c2] = arr[b2]
            arr[b0] = arr[a0]; arr[b1] = arr[a1]; arr[b2] = arr[a2]
            arr[a0] = t0; arr[a1] = t1; arr[a2] = t2
        }
    }
}
