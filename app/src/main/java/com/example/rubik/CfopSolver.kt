package com.example.rubik

object CfopSolver {

    fun solve(initialCube: CubeState): CfopSolution {
        var cur = initialCube

        // 1. Optimal Fingertrick White Cross on D
        val crossResult = CrossSolver.solveOptimalCross(cur)
        val crossMoves = crossResult.moves
        cur = cur.applyMoves(crossMoves)

        // 2. F2L (4 Corner-Edge Pairs)
        val f2lSlots = F2lSolver.solveAllF2l(cur)
        val f2lMoves = mutableListOf<CubeMove>()
        val f2lStepsMeta = mutableListOf<Pair<CubeMove, String>>()
        for (slotSol in f2lSlots) {
            for (m in slotSol.moves) {
                f2lMoves.add(m)
                f2lStepsMeta.add(m to slotSol.slot.label)
            }
            cur = cur.applyMoves(slotSol.moves)
        }

        // 3. OLL (Yellow Top Orientation)
        val ollMatch = LastLayerTables.solveOll(cur)
        val ollMoves = ollMatch.moves
        cur = cur.applyMoves(ollMoves)

        // 4. PLL (Last Layer Permutation)
        val pllMatch = LastLayerTables.solvePll(cur)
        val pllMoves = pllMatch.moves

        // Build step-by-step animation states
        val steps = mutableListOf<CfopStep>()
        var animCube = initialCube
        var index = 1

        for (m in crossMoves) {
            val next = animCube.applyMove(m)
            steps.add(CfopStep(index++, CfopStage.CROSS, m, "Cross (${crossMoves.size} bước)", animCube, next))
            animCube = next
        }
        for ((m, label) in f2lStepsMeta) {
            val next = animCube.applyMove(m)
            steps.add(CfopStep(index++, CfopStage.F2L, m, label, animCube, next))
            animCube = next
        }
        for (m in ollMoves) {
            val next = animCube.applyMove(m)
            steps.add(CfopStep(index++, CfopStage.OLL, m, ollMatch.caseName, animCube, next))
            animCube = next
        }
        for (m in pllMoves) {
            val next = animCube.applyMove(m)
            steps.add(CfopStep(index++, CfopStage.PLL, m, pllMatch.caseName, animCube, next))
            animCube = next
        }

        return CfopSolution(
            initialCube = initialCube,
            crossMoves = crossMoves,
            f2lMoves = f2lMoves,
            ollMoves = ollMoves,
            pllMoves = pllMoves,
            ollCaseName = ollMatch.caseName,
            pllCaseName = pllMatch.caseName,
            crossFingertrickScore = crossResult.fingertrickCost,
            steps = steps
        )
    }
}
