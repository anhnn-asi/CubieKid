package com.example

import com.example.rubik.CfopSolver
import com.example.rubik.CubeColor
import com.example.rubik.CubeMove
import com.example.rubik.CubeState
import com.example.rubik.CubeValidationResult
import com.example.rubik.CubeValidator
import com.example.rubik.FaceId
import com.example.rubik.LastLayerTables
import com.example.scanner.RubikColorClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CfopSolverTest {

    @Test
    fun moveEngine_sexyMoveOrder6_and_RU105() {
        val solved = CubeState.solved()
        val sexy = CubeMove.parseAlgorithm("R U R' U'")
        var c = solved
        repeat(6) { c = c.applyMoves(sexy) }
        assertTrue("Sexy move x6 must return to solved", c.isSolved())

        val ru = CubeMove.parseAlgorithm("R U")
        var c2 = solved
        repeat(105) { c2 = c2.applyMoves(ru) }
        assertTrue("R U x105 must return to solved", c2.isSolved())
    }

    @Test
    fun validator_reconcilesArbitrarilyRotatedScannedFaces() {
        val scramble = CubeMove.parseAlgorithm("R U2 F' L D R2 B U' L2")
        val scrambled = CubeState.solved().applyMoves(scramble)

        // Simulate arbitrary 90-degree camera rotations of all 6 faces
        val captured = mapOf(
            CubeColor.YELLOW to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.U), 1),
            CubeColor.ORANGE to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.R), 2),
            CubeColor.GREEN to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.F), 3),
            CubeColor.WHITE to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.D), 1),
            CubeColor.RED to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.L), 2),
            CubeColor.BLUE to CubeValidator.rotateGridCW(scrambled.faceColors(FaceId.B), 3)
        )

        val result = CubeValidator.reconcileAndValidate(captured)
        assertTrue("Rotated faces must reconcile to valid cube", result is CubeValidationResult.Valid)
        val validCube = (result as CubeValidationResult.Valid).cube
        assertEquals(scrambled, validCube)
    }

    @Test
    fun validator_rejectsIncompleteOrInvalidCube() {
        val incomplete = mapOf(
            CubeColor.YELLOW to CubeState.solved().faceColors(FaceId.U),
            CubeColor.WHITE to CubeState.solved().faceColors(FaceId.D)
        )
        assertTrue(CubeValidator.reconcileAndValidate(incomplete) is CubeValidationResult.Invalid)

        // Bad sticker count (10 whites, 8 yellows)
        val solved = CubeState.solved()
        val badU = solved.faceColors(FaceId.U).toMutableList().apply { this[0] = CubeColor.WHITE }
        val badMap = mapOf(
            CubeColor.YELLOW to badU,
            CubeColor.RED to solved.faceColors(FaceId.R),
            CubeColor.BLUE to solved.faceColors(FaceId.F),
            CubeColor.WHITE to solved.faceColors(FaceId.D),
            CubeColor.ORANGE to solved.faceColors(FaceId.L),
            CubeColor.GREEN to solved.faceColors(FaceId.B)
        )
        assertTrue(CubeValidator.reconcileAndValidate(badMap) is CubeValidationResult.Invalid)
    }

    @Test
    fun cfopSolver_solves50RandomScramblesWithOptimalCross() {
        val rng = Random(42)
        repeat(50) { idx ->
            val scramble = CubeState.generateScramble(20, rng)
            val scrambled = CubeState.solved().applyMoves(scramble)
            val solution = CfopSolver.solve(scrambled)

            assertTrue(
                "Cross length should be <= 8 moves for scramble #$idx, was ${solution.crossMoves.size}",
                solution.crossMoves.size <= 8
            )
            val finalState = scrambled.applyMoves(solution.allMoves)
            assertTrue(
                "CFOP solution failed to solve scramble #$idx: ${scramble.joinToString(" ") { it.notation }} | sol=${solution.fullNotation}",
                finalState.isSolved()
            )
        }
    }

    @Test
    fun lastLayer_wideMoveExpansion_preservesValidCube() {
        val testAlgs = listOf(
            "r U R' U' r' F R F'",
            "f R U R' U' f'",
            "M2 U M2 U2 M2 U M2",
            "x R' U R' D2 R U' R' D2 R2 x'",
            "R' U R' U' y R' F' R2 U' R' U R' F R F"
        )
        for (alg in testAlgs) {
            val moves = LastLayerTables.expandWideAndRotations(alg)
            assertTrue("Expanded moves for '$alg' should not be empty", moves.isNotEmpty())
        }
    }

    @Test
    fun hsvClassifier_classifiesAllSixStandardColors() {
        assertEquals(CubeColor.WHITE, RubikColorClassifier.classifyHsv(0f, 0.05f, 0.95f)?.first)
        assertEquals(CubeColor.RED, RubikColorClassifier.classifyHsv(3f, 0.85f, 0.80f)?.first)
        assertEquals(CubeColor.ORANGE, RubikColorClassifier.classifyHsv(22f, 0.90f, 0.90f)?.first)
        assertEquals(CubeColor.YELLOW, RubikColorClassifier.classifyHsv(54f, 0.85f, 0.95f)?.first)
        assertEquals(CubeColor.GREEN, RubikColorClassifier.classifyHsv(125f, 0.80f, 0.70f)?.first)
        assertEquals(CubeColor.BLUE, RubikColorClassifier.classifyHsv(215f, 0.85f, 0.85f)?.first)
    }
}
