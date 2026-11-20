package com.example.rubik

/**
 * Solves all 4 F2L slots (FR, FL, BR, BL) in greedy optimal order while preserving
 * the solved White Cross and any already-solved F2L slots.
 */
object F2lSolver {

    enum class F2lSlot(
        val label: String,
        val cD: Int, val cF: Int, val cS: Int,
        val cColorD: CubeColor, val cColorF: CubeColor, val cColorS: CubeColor,
        val eF: Int, val eS: Int,
        val eColorF: CubeColor, val eColorS: CubeColor,
        val triggers: List<List<CubeMove>>,
        val extractTrigger: List<CubeMove>
    ) {
        FR(
            label = "F2L (Xanh lá-Cam)",
            cD = 29, cF = 26, cS = 15,
            cColorD = CubeColor.WHITE, cColorF = CubeColor.GREEN, cColorS = CubeColor.ORANGE,
            eF = 23, eS = 12,
            eColorF = CubeColor.GREEN, eColorS = CubeColor.ORANGE,
            triggers = listOf(
                CubeMove.parseAlgorithm("R U R'"),
                CubeMove.parseAlgorithm("R U' R'"),
                CubeMove.parseAlgorithm("R U2 R'"),
                CubeMove.parseAlgorithm("F' U' F"),
                CubeMove.parseAlgorithm("F' U F"),
                CubeMove.parseAlgorithm("F' U2 F"),
                CubeMove.parseAlgorithm("R' F R F'")
            ),
            extractTrigger = CubeMove.parseAlgorithm("R U R'")
        ),
        FL(
            label = "F2L (Đỏ-Xanh lá)",
            cD = 27, cF = 44, cS = 24,
            cColorD = CubeColor.WHITE, cColorF = CubeColor.RED, cColorS = CubeColor.GREEN,
            eF = 21, eS = 41,
            eColorF = CubeColor.GREEN, eColorS = CubeColor.RED,
            triggers = listOf(
                CubeMove.parseAlgorithm("L' U' L"),
                CubeMove.parseAlgorithm("L' U L"),
                CubeMove.parseAlgorithm("L' U2 L"),
                CubeMove.parseAlgorithm("F U F'"),
                CubeMove.parseAlgorithm("F U' F'"),
                CubeMove.parseAlgorithm("F U2 F'"),
                CubeMove.parseAlgorithm("L F' L' F")
            ),
            extractTrigger = CubeMove.parseAlgorithm("L' U' L")
        ),
        BR(
            label = "F2L (Cam-Xanh dương)",
            cD = 35, cF = 17, cS = 51,
            cColorD = CubeColor.WHITE, cColorF = CubeColor.ORANGE, cColorS = CubeColor.BLUE,
            eF = 48, eS = 14,
            eColorF = CubeColor.BLUE, eColorS = CubeColor.ORANGE,
            triggers = listOf(
                CubeMove.parseAlgorithm("R' U' R"),
                CubeMove.parseAlgorithm("R' U R"),
                CubeMove.parseAlgorithm("R' U2 R"),
                CubeMove.parseAlgorithm("B U B'"),
                CubeMove.parseAlgorithm("B U' B'"),
                CubeMove.parseAlgorithm("B U2 B'"),
                CubeMove.parseAlgorithm("R B' R' B")
            ),
            extractTrigger = CubeMove.parseAlgorithm("R' U' R")
        ),
        BL(
            label = "F2L (Xanh dương-Đỏ)",
            cD = 33, cF = 53, cS = 42,
            cColorD = CubeColor.WHITE, cColorF = CubeColor.BLUE, cColorS = CubeColor.RED,
            eF = 50, eS = 39,
            eColorF = CubeColor.BLUE, eColorS = CubeColor.RED,
            triggers = listOf(
                CubeMove.parseAlgorithm("L U L'"),
                CubeMove.parseAlgorithm("L U' L'"),
                CubeMove.parseAlgorithm("L U2 L'"),
                CubeMove.parseAlgorithm("B' U' B"),
                CubeMove.parseAlgorithm("B' U B"),
                CubeMove.parseAlgorithm("B' U2 B"),
                CubeMove.parseAlgorithm("L' B L B'")
            ),
            extractTrigger = CubeMove.parseAlgorithm("L U L'")
        )
    }

    data class SlotSolution(
        val slot: F2lSlot,
        val moves: List<CubeMove>
    )

    fun solveAllF2l(initialCube: CubeState): List<SlotSolution> {
        var cube = initialCube
        val remaining = F2lSlot.entries.toMutableSet()
        val result = mutableListOf<SlotSolution>()

        // Remove already solved slots first
        val alreadySolved = remaining.filter { isSlotSolved(cube, it) }
        remaining.removeAll(alreadySolved.toSet())

        while (remaining.isNotEmpty()) {
            var bestSol: SlotSolution? = null
            var bestScore = Int.MAX_VALUE

            for (slot in remaining) {
                val moves = solveSingleSlot(cube, slot, remaining)
                val score = moves.sumOf { it.fingertrickCost }
                if (bestSol == null || score < bestScore) {
                    bestSol = SlotSolution(slot, moves)
                    bestScore = score
                }
            }

            val chosen = bestSol!!
            val simplified = CubeState.simplifyMoves(chosen.moves)
            result.add(SlotSolution(chosen.slot, simplified))
            cube = cube.applyMoves(simplified)
            remaining.remove(chosen.slot)
        }
        return result
    }

    private fun isSlotSolved(cube: CubeState, slot: F2lSlot): Boolean {
        return cube.colorAt(slot.cD) == slot.cColorD &&
            cube.colorAt(slot.cF) == slot.cColorF &&
            cube.colorAt(slot.cS) == slot.cColorS &&
            cube.colorAt(slot.eF) == slot.eColorF &&
            cube.colorAt(slot.eS) == slot.eColorS
    }

    private fun solveSingleSlot(
        startCube: CubeState,
        target: F2lSlot,
        unsolvedSlots: Set<F2lSlot>
    ): List<CubeMove> {
        if (isSlotSolved(startCube, target)) return emptyList()

        // Step 1: Ensure neither the target corner nor target edge is trapped inside another unsolved slot.
        var workingCube = startCube
        val prefixMoves = mutableListOf<CubeMove>()
        val otherUnsolved = unsolvedSlots.filter { it != target }

        for (guard in 0 until 3) {
            val blockingSlot = otherUnsolved.firstOrNull { other ->
                containsTargetPiece(workingCube, other, target)
            } ?: break

            // Pick a U setup if needed so extracting doesn't accidentally drop the other target piece into blockingSlot
            var chosenExtract: List<CubeMove>? = null
            for (uPre in listOf(null, CubeMove.U, CubeMove.U_PRIME, CubeMove.U2)) {
                val candidateSeq = if (uPre == null) blockingSlot.extractTrigger else listOf(uPre) + blockingSlot.extractTrigger
                val next = workingCube.applyMoves(candidateSeq)
                if (!containsTargetPiece(next, blockingSlot, target)) {
                    chosenExtract = candidateSeq
                    break
                }
            }
            val extract = chosenExtract ?: blockingSlot.extractTrigger
            prefixMoves.addAll(extract)
            workingCube = workingCube.applyMoves(extract)
        }

        // Step 2: BFS over {none, U, U', U2} + target.triggers (state space <= 150 states!)
        val uPrefixes = listOf(
            emptyList(),
            listOf(CubeMove.U),
            listOf(CubeMove.U_PRIME),
            listOf(CubeMove.U2)
        )

        data class Node(val cube: CubeState, val moves: List<CubeMove>)

        val visited = HashSet<Pair<Int, Int>>()
        val queue = ArrayDeque<Node>()
        val startKey = slotStateKey(workingCube, target)
        visited.add(startKey)
        queue.add(Node(workingCube, emptyList()))

        while (queue.isNotEmpty()) {
            val cur = queue.removeFirst()
            if (isSlotSolved(cur.cube, target)) {
                return CubeState.simplifyMoves(prefixMoves + cur.moves)
            }
            for (uPre in uPrefixes) {
                val afterU = if (uPre.isEmpty()) cur.cube else cur.cube.applyMoves(uPre)
                for (trig in target.triggers) {
                    val nextCube = afterU.applyMoves(trig)
                    val key = slotStateKey(nextCube, target)
                    if (visited.add(key)) {
                        queue.add(Node(nextCube, cur.moves + uPre + trig))
                    }
                }
            }
        }
        return CubeState.simplifyMoves(prefixMoves)
    }

    private fun containsTargetPiece(cube: CubeState, slotToCheck: F2lSlot, target: F2lSlot): Boolean {
        val cSet = setOf(cube.colorAt(slotToCheck.cD), cube.colorAt(slotToCheck.cF), cube.colorAt(slotToCheck.cS))
        val targetCorner = setOf(target.cColorD, target.cColorF, target.cColorS)
        if (cSet == targetCorner) return true

        val eSet = setOf(cube.colorAt(slotToCheck.eF), cube.colorAt(slotToCheck.eS))
        val targetEdge = setOf(target.eColorF, target.eColorS)
        return eSet == targetEdge
    }

    // Tracks target corner and target edge locations across U-layer (4 corners, 4 edges) + target slot
    private val U_CORNER_INDICES = arrayOf(
        Triple(8, 9, 20),  // UFR
        Triple(6, 18, 38), // UFL
        Triple(0, 36, 47), // UBL
        Triple(2, 45, 11)  // UBR
    )
    private val U_EDGE_INDICES = arrayOf(
        7 to 19, // UF
        5 to 10, // UR
        1 to 46, // UB
        3 to 37  // UL
    )

    private fun slotStateKey(cube: CubeState, target: F2lSlot): Pair<Int, Int> {
        var cCode = -1
        // Check target slot corner
        run {
            val a = cube.colorAt(target.cD)
            val b = cube.colorAt(target.cF)
            val c = cube.colorAt(target.cS)
            if (a == target.cColorD && b == target.cColorF && c == target.cColorS) cCode = 12
            else if (a == target.cColorS && b == target.cColorD && c == target.cColorF) cCode = 13
            else if (a == target.cColorF && b == target.cColorS && c == target.cColorD) cCode = 14
        }
        if (cCode == -1) {
            for (i in 0 until 4) {
                val (i0, i1, i2) = U_CORNER_INDICES[i]
                val a = cube.colorAt(i0)
                val b = cube.colorAt(i1)
                val c = cube.colorAt(i2)
                if (a == target.cColorD && b == target.cColorF && c == target.cColorS) { cCode = i * 3; break }
                if (a == target.cColorS && b == target.cColorD && c == target.cColorF) { cCode = i * 3 + 1; break }
                if (a == target.cColorF && b == target.cColorS && c == target.cColorD) { cCode = i * 3 + 2; break }
            }
        }

        var eCode = -1
        run {
            val a = cube.colorAt(target.eF)
            val b = cube.colorAt(target.eS)
            if (a == target.eColorF && b == target.eColorS) eCode = 8
            else if (a == target.eColorS && b == target.eColorF) eCode = 9
        }
        if (eCode == -1) {
            for (i in 0 until 4) {
                val (aIdx, bIdx) = U_EDGE_INDICES[i]
                val a = cube.colorAt(aIdx)
                val b = cube.colorAt(bIdx)
                if (a == target.eColorF && b == target.eColorS) { eCode = i * 2; break }
                if (a == target.eColorS && b == target.eColorF) { eCode = i * 2 + 1; break }
            }
        }
        return cCode to eCode
    }
}
