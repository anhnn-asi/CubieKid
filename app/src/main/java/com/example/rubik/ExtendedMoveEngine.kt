package com.example.rubik

/**
 * Supports executing outer-layer (U, R, F, D, L, B), wide (u, r, f, d, l, b),
 * slice (M, E, S), and whole-cube rotations (x, y, z) directly on [CubeState]
 * using exact 3D sticker coordinate transforms matching [CubeEngine] and the 3D visualizer.
 */
object ExtendedMoveEngine {

    private enum class Axis { X, Y, Z }

    private data class IntVec3(val x: Int, val y: Int, val z: Int) {
        fun rotate90(axis: Axis, cwSteps: Int): IntVec3 {
            var cur = this
            val n = ((cwSteps % 4) + 4) % 4
            repeat(n) {
                cur = when (axis) {
                    // +90 deg X (R direction): (x, y, z) -> (x, -z, y)
                    Axis.X -> IntVec3(cur.x, -cur.z, cur.y)
                    // +90 deg Y (U direction): (x, y, z) -> (z, y, -x)
                    Axis.Y -> IntVec3(cur.z, cur.y, -cur.x)
                    // +90 deg Z (B direction / -F direction): (x, y, z) -> (-y, x, z)
                    Axis.Z -> IntVec3(-cur.y, cur.x, cur.z)
                }
            }
            return cur
        }
    }

    private data class FaceletCoord(
        val index: Int,
        val pos: IntVec3,
        val normal: IntVec3
    )

    private val faceletCoords: List<FaceletCoord> = buildList {
        for (ix in -1..1) {
            for (iy in -1..1) {
                for (iz in -1..1) {
                    if (ix == 0 && iy == 0 && iz == 0) continue
                    val pos = IntVec3(ix, iy, iz)
                    if (iy == 1) add(FaceletCoord((1 - iz) * 3 + (ix + 1), pos, IntVec3(0, 1, 0)))
                    if (ix == 1) add(FaceletCoord(9 + (1 - iy) * 3 + (iz + 1), pos, IntVec3(1, 0, 0)))
                    if (iz == -1) add(FaceletCoord(18 + (1 - iy) * 3 + (ix + 1), pos, IntVec3(0, 0, -1)))
                    if (iy == -1) add(FaceletCoord(27 + (iz + 1) * 3 + (ix + 1), pos, IntVec3(0, -1, 0)))
                    if (ix == -1) add(FaceletCoord(36 + (1 - iy) * 3 + (1 - iz), pos, IntVec3(-1, 0, 0)))
                    if (iz == 1) add(FaceletCoord(45 + (1 - iy) * 3 + (1 - ix), pos, IntVec3(0, 0, 1)))
                }
            }
        }
    }

    private val coordToIndex: Map<Pair<IntVec3, IntVec3>, Int> =
        faceletCoords.associate { (it.pos to it.normal) to it.index }

    fun tokenize(algorithm: String): List<String> =
        algorithm.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun applyAlgorithm(cube: CubeState, algorithm: String): CubeState {
        var cur = cube
        for (tok in tokenize(algorithm)) {
            cur = applyToken(cur, tok)
        }
        return cur
    }

    fun applyToken(cube: CubeState, token: String): CubeState {
        val t = token.trim()
        if (t.isEmpty()) return cube
        val base = t[0]
        val q = when {
            t.endsWith("2") -> 2
            t.endsWith("'") -> 3
            else -> 1
        }
        val simple = CubeMove.parse(t)
        if (simple != null) return cube.applyMove(simple)

        return when (base) {
            'x' -> rotateCoordSubset(cube, Axis.X, cwSteps = q) { true }
            'y' -> rotateCoordSubset(cube, Axis.Y, cwSteps = q) { true }
            'z' -> rotateCoordSubset(cube, Axis.Z, cwSteps = -q) { true }
            'M' -> rotateCoordSubset(cube, Axis.X, cwSteps = -q) { it.x == 0 }
            'E' -> rotateCoordSubset(cube, Axis.Y, cwSteps = -q) { it.y == 0 }
            'S' -> rotateCoordSubset(cube, Axis.Z, cwSteps = -q) { it.z == 0 }
            'r' -> rotateCoordSubset(cube, Axis.X, cwSteps = q) { it.x >= 0 }
            'l' -> rotateCoordSubset(cube, Axis.X, cwSteps = -q) { it.x <= 0 }
            'u' -> rotateCoordSubset(cube, Axis.Y, cwSteps = q) { it.y >= 0 }
            'd' -> rotateCoordSubset(cube, Axis.Y, cwSteps = -q) { it.y <= 0 }
            'f' -> rotateCoordSubset(cube, Axis.Z, cwSteps = -q) { it.z <= 0 }
            'b' -> rotateCoordSubset(cube, Axis.Z, cwSteps = q) { it.z >= 0 }
            else -> cube
        }
    }

    /**
     * Maps any extended token to a representative outer [CubeMove] for drawing a 3D guide arrow.
     */
    fun toVisualGuideMove(token: String): CubeMove? {
        val t = token.trim()
        if (t.isEmpty()) return null
        CubeMove.parse(t)?.let { return it }
        val base = t[0]
        val q = when {
            t.endsWith("2") -> 2
            t.endsWith("'") -> 3
            else -> 1
        }
        return when (base) {
            'r', 'x' -> CubeState.moveFor(MoveFace.R, q)
            'l', 'M' -> CubeState.moveFor(MoveFace.L, q)
            'u', 'y' -> CubeState.moveFor(MoveFace.U, q)
            'd', 'E' -> CubeState.moveFor(MoveFace.D, q)
            'f', 'S', 'z' -> CubeState.moveFor(MoveFace.F, q)
            'b' -> CubeState.moveFor(MoveFace.B, q)
            else -> null
        }
    }

    private fun rotateCoordSubset(
        cube: CubeState,
        axis: Axis,
        cwSteps: Int,
        predicate: (IntVec3) -> Boolean
    ): CubeState {
        val next = cube.facelets.toMutableList()
        for (fc in faceletCoords) {
            if (!predicate(fc.pos)) continue
            val newPos = fc.pos.rotate90(axis, cwSteps)
            val newNorm = fc.normal.rotate90(axis, cwSteps)
            val dstIndex = coordToIndex[newPos to newNorm] ?: continue
            next[dstIndex] = cube.facelets[fc.index]
        }
        return CubeState(next)
    }
}
