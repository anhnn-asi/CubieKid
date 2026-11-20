package com.example.scanner

import android.graphics.Color
import androidx.camera.core.ImageProxy
import com.example.rubik.CubeColor
import kotlin.math.abs

sealed class FaceSampleResult {
    data class Detected(
        val center: CubeColor,
        val grid: List<CubeColor>,
        val confidence: Float
    ) : FaceSampleResult()

    data object Unstable : FaceSampleResult()
}

object RubikColorClassifier {

    /**
     * Samples the central 3x3 square region of a YUV_420_888 ImageProxy,
     * accounts for sensor rotationDegrees so row/col orientation matches the screen preview,
     * and classifies each of the 9 stickers into the 6 Rubik colors using HSV thresholds.
     */
    fun analyzeFrame(image: ImageProxy): FaceSampleResult {
        if (image.planes.size < 3) return FaceSampleResult.Unstable

        val width = image.width
        val height = image.height
        val rotation = ((image.imageInfo.rotationDegrees % 360) + 360) % 360

        val yPlane = image.planes[0]
        val uPlane = image.planes[1]
        val vPlane = image.planes[2]

        val yBuf = yPlane.buffer
        val uBuf = uPlane.buffer
        val vBuf = vPlane.buffer

        val yRowStride = yPlane.rowStride
        val yPixStride = yPlane.pixelStride
        val uvRowStride = uPlane.rowStride
        val uvPixStride = uPlane.pixelStride

        // Central square occupies 52% of the shorter dimension
        val boxSize = (minOf(width, height) * 0.52f).toInt().coerceAtLeast(30)
        val cellSize = boxSize / 3f
        val startX = (width - boxSize) / 2f
        val startY = (height - boxSize) / 2f

        val rawGrid = ArrayList<CubeColor>(9)
        var totalConfidence = 0f
        val hsv = FloatArray(3)

        // Screen grid (row 0..2, col 0..2) -> sensor coordinates
        for (screenRow in 0 until 3) {
            for (screenCol in 0 until 3) {
                // Map screen (col, row) to normalized [0..1] in upright box, then rotate inverse
                val nx = (screenCol + 0.5f) / 3f
                val ny = (screenRow + 0.5f) / 3f
                val (sxNorm, syNorm) = when (rotation) {
                    90 -> ny to (1f - nx)
                    180 -> (1f - nx) to (1f - ny)
                    270 -> (1f - ny) to nx
                    else -> nx to ny
                }

                val centerPx = (startX + sxNorm * boxSize).toInt().coerceIn(4, width - 5)
                val centerPy = (startY + syNorm * boxSize).toInt().coerceIn(4, height - 5)

                val patchRadius = (cellSize * 0.14f).toInt().coerceIn(2, 8)
                var rSum = 0
                var gSum = 0
                var bSum = 0
                var count = 0
                var lumaSum = 0
                var lumaSqSum = 0

                val step = maxOf(1, patchRadius / 2)
                for (dy in -patchRadius..patchRadius step step) {
                    for (dx in -patchRadius..patchRadius step step) {
                        val px = (centerPx + dx).coerceIn(0, width - 1)
                        val py = (centerPy + dy).coerceIn(0, height - 1)

                        val yVal = yBuf.get(py * yRowStride + px * yPixStride).toInt() and 0xFF
                        val uvIdx = (py / 2) * uvRowStride + (px / 2) * uvPixStride
                        val uVal = (uBuf.get(uvIdx).toInt() and 0xFF) - 128
                        val vVal = (vBuf.get(uvIdx).toInt() and 0xFF) - 128

                        val r = (yVal + 1.370705f * vVal).toInt().coerceIn(0, 255)
                        val g = (yVal - 0.337633f * uVal - 0.698001f * vVal).toInt().coerceIn(0, 255)
                        val b = (yVal + 1.732446f * uVal).toInt().coerceIn(0, 255)

                        rSum += r
                        gSum += g
                        bSum += b
                        lumaSum += yVal
                        lumaSqSum += yVal * yVal
                        count++
                    }
                }

                if (count == 0) return FaceSampleResult.Unstable

                val meanLuma = lumaSum.toFloat() / count
                val variance = (lumaSqSum.toFloat() / count) - (meanLuma * meanLuma)
                // Reject motion-blurred or specular border frames
                if (variance > 900f) return FaceSampleResult.Unstable

                val avgR = rSum / count
                val avgG = gSum / count
                val avgB = bSum / count
                Color.RGBToHSV(avgR, avgG, avgB, hsv)

                val (classified, conf) = classifyHsv(hsv[0], hsv[1], hsv[2])
                    ?: return FaceSampleResult.Unstable
                rawGrid.add(classified)
                totalConfidence += conf
            }
        }

        val avgConf = totalConfidence / 9f
        if (avgConf < 0.55f) return FaceSampleResult.Unstable
        return FaceSampleResult.Detected(
            center = rawGrid[4],
            grid = rawGrid,
            confidence = avgConf
        )
    }

    fun classifyHsv(hue: Float, sat: Float, value: Float): Pair<CubeColor, Float>? {
        if (value < 0.16f) return null

        // White: low saturation and bright
        if (sat < 0.26f && value >= 0.42f) {
            val conf = (1f - sat / 0.26f).coerceIn(0.6f, 1f)
            return CubeColor.WHITE to conf
        }
        // Ambiguous washed-out dark gray -> reject
        if (sat < 0.26f) return null

        return when {
            hue < 11f || hue >= 332f -> {
                val distFromBoundary = minOf(abs(hue - 11f), abs((if (hue < 11f) hue + 360f else hue) - 332f))
                CubeColor.RED to (0.65f + (distFromBoundary / 25f).coerceIn(0f, 0.35f))
            }
            hue in 11f..34f -> {
                val centerDist = abs(hue - 22f)
                CubeColor.ORANGE to (1f - (centerDist / 16f) * 0.35f)
            }
            hue in 34f..72f -> {
                val centerDist = abs(hue - 53f)
                CubeColor.YELLOW to (1f - (centerDist / 22f) * 0.35f)
            }
            hue in 72f..165f -> {
                val centerDist = abs(hue - 120f)
                CubeColor.GREEN to (1f - (centerDist / 50f) * 0.35f)
            }
            hue in 165f..265f -> {
                val centerDist = abs(hue - 215f)
                CubeColor.BLUE to (1f - (centerDist / 55f) * 0.35f)
            }
            else -> null
        }
    }
}
