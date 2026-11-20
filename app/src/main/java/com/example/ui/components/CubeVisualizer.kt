package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.tr
import com.example.rubik.CfopSolution
import com.example.rubik.CfopStage
import com.example.rubik.CfopStep
import com.example.rubik.CubeColor
import com.example.rubik.CubeMove
import com.example.rubik.CubeState
import com.example.rubik.F2lSolver
import com.example.rubik.FaceId
import com.example.rubik.MoveFace
import com.example.ui.theme.CubePlasticDark
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.StickerBlue
import com.example.ui.theme.StickerGreen
import com.example.ui.theme.StickerOrange
import com.example.ui.theme.StickerRed
import com.example.ui.theme.StickerWhite
import com.example.ui.theme.StickerYellow
import com.example.ui.theme.ToyOrange
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

fun CubeColor.toStickerColor(): Color = when (this) {
    CubeColor.WHITE -> StickerWhite
    CubeColor.YELLOW -> StickerYellow
    CubeColor.RED -> StickerRed
    CubeColor.ORANGE -> StickerOrange
    CubeColor.BLUE -> StickerBlue
    CubeColor.GREEN -> StickerGreen
}

private enum class RotationAxis { X, Y, Z }

private data class Point3D(val x: Float, val y: Float, val z: Float) {
    operator fun plus(other: Point3D) = Point3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Point3D) = Point3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scale: Float) = Point3D(x * scale, y * scale, z * scale)

    fun dot(other: Point3D): Float = x * other.x + y * other.y + z * other.z

    fun rotateX(angleDeg: Float): Point3D {
        if (angleDeg == 0f) return this
        val rad = Math.toRadians(angleDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return Point3D(x, y * c - z * s, y * s + z * c)
    }

    fun rotateY(angleDeg: Float): Point3D {
        if (angleDeg == 0f) return this
        val rad = Math.toRadians(angleDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return Point3D(x * c + z * s, y, -x * s + z * c)
    }

    fun rotateZ(angleDeg: Float): Point3D {
        if (angleDeg == 0f) return this
        val rad = Math.toRadians(angleDeg.toDouble())
        val c = cos(rad).toFloat()
        val s = sin(rad).toFloat()
        return Point3D(x * c - y * s, x * s + y * c, z)
    }

    fun rotateAxis(axis: RotationAxis, angleDeg: Float): Point3D = when (axis) {
        RotationAxis.X -> rotateX(angleDeg)
        RotationAxis.Y -> rotateY(angleDeg)
        RotationAxis.Z -> rotateZ(angleDeg)
    }

    fun project(width: Float, height: Float, fov: Float, viewerDistance: Float): Offset {
        val factor = fov / (viewerDistance + z).coerceAtLeast(1.2f)
        return Offset(x * factor + width / 2f, -y * factor + height / 2f)
    }
}

private data class CubieFaceSpec(
    val ix: Int,
    val iy: Int,
    val iz: Int,
    val faceId: FaceId,
    val faceletIndex: Int?,
    val plasticVerts: List<Point3D>,
    val stickerVerts: List<Point3D>?,
    val normal: Point3D,
    val center: Point3D
)

private val CUBIE_FACE_SPECS: List<CubieFaceSpec> by lazy { buildCubieFaceSpecs() }

private val CUBIE_STICKER_INDICES: Map<Triple<Int, Int, Int>, List<Int>> by lazy {
    CUBIE_FACE_SPECS
        .filter { it.faceletIndex != null }
        .groupBy(
            keySelector = { Triple(it.ix, it.iy, it.iz) },
            valueTransform = { it.faceletIndex!! }
        )
}

private fun findHighlightedCubies(cube: CubeState, step: CfopStep?): Set<Triple<Int, Int, Int>> {
    if (step == null) return emptySet()
    return when (step.stage) {
        CfopStage.F2L -> {
            val slot = F2lSolver.F2lSlot.entries.firstOrNull { it.label == step.caseLabel }
                ?: return emptySet()
            val targetEdge = setOf(slot.eColorF, slot.eColorS)
            val targetCorner = setOf(slot.cColorD, slot.cColorF, slot.cColorS)
            CUBIE_STICKER_INDICES.entries
                .filter { (_, indices) ->
                    val colors = indices.mapTo(mutableSetOf()) { cube.colorAt(it) }
                    colors == targetEdge || colors == targetCorner
                }
                .map { it.key }
                .toSet()
        }
        CfopStage.CROSS -> {
            CUBIE_STICKER_INDICES.entries
                .filter { (_, indices) ->
                    indices.size == 2 && indices.any { cube.colorAt(it) == CubeColor.WHITE }
                }
                .map { it.key }
                .toSet()
        }
        else -> emptySet()
    }
}

private fun buildCubieFaceSpecs(): List<CubieFaceSpec> {
    val result = mutableListOf<CubieFaceSpec>()
    val half = 0.475f
    val sHalf = 0.405f
    val chamfer = 0.17f

    for (ix in -1..1) {
        for (iy in -1..1) {
            for (iz in -1..1) {
                if (ix == 0 && iy == 0 && iz == 0) continue
                val cubieCenter = Point3D(ix.toFloat(), iy.toFloat(), iz.toFloat())

                for (faceId in FaceId.entries) {
                    val (normal, uAxis, vAxis) = when (faceId) {
                        FaceId.U -> Triple(Point3D(0f, 1f, 0f), Point3D(1f, 0f, 0f), Point3D(0f, 0f, -1f))
                        FaceId.D -> Triple(Point3D(0f, -1f, 0f), Point3D(1f, 0f, 0f), Point3D(0f, 0f, 1f))
                        FaceId.F -> Triple(Point3D(0f, 0f, -1f), Point3D(1f, 0f, 0f), Point3D(0f, -1f, 0f))
                        FaceId.B -> Triple(Point3D(0f, 0f, 1f), Point3D(-1f, 0f, 0f), Point3D(0f, -1f, 0f))
                        FaceId.R -> Triple(Point3D(1f, 0f, 0f), Point3D(0f, 0f, 1f), Point3D(0f, -1f, 0f))
                        FaceId.L -> Triple(Point3D(-1f, 0f, 0f), Point3D(0f, 0f, -1f), Point3D(0f, -1f, 0f))
                    }

                    val faceCenter = cubieCenter + normal * half
                    val plasticVerts = listOf(
                        faceCenter - uAxis * half - vAxis * half,
                        faceCenter + uAxis * half - vAxis * half,
                        faceCenter + uAxis * half + vAxis * half,
                        faceCenter - uAxis * half + vAxis * half
                    )

                    val faceletIdx = exteriorFaceletIndex(ix, iy, iz, faceId)
                    val stickerVerts = if (faceletIdx != null) {
                        val stickerCenter = cubieCenter + normal * (half + 0.008f)
                        val corners = listOf(
                            stickerCenter - uAxis * sHalf - vAxis * sHalf,
                            stickerCenter + uAxis * sHalf - vAxis * sHalf,
                            stickerCenter + uAxis * sHalf + vAxis * sHalf,
                            stickerCenter - uAxis * sHalf + vAxis * sHalf
                        )
                        // 8-point chamfered sticker polygon for realistic speedcube tiles
                        buildList<Point3D> {
                            for (i in 0 until 4) {
                                val a = corners[i]
                                val b = corners[(i + 1) % 4]
                                add(lerp3D(a, b, chamfer))
                                add(lerp3D(a, b, 1f - chamfer))
                            }
                        }
                    } else null

                    result.add(
                        CubieFaceSpec(
                            ix = ix,
                            iy = iy,
                            iz = iz,
                            faceId = faceId,
                            faceletIndex = faceletIdx,
                            plasticVerts = plasticVerts,
                            stickerVerts = stickerVerts,
                            normal = normal,
                            center = faceCenter
                        )
                    )
                }
            }
        }
    }
    return result
}

private fun exteriorFaceletIndex(ix: Int, iy: Int, iz: Int, faceId: FaceId): Int? = when (faceId) {
    FaceId.U -> if (iy == 1) (1 - iz) * 3 + (ix + 1) else null
    FaceId.R -> if (ix == 1) 9 + (1 - iy) * 3 + (iz + 1) else null
    FaceId.F -> if (iz == -1) 18 + (1 - iy) * 3 + (ix + 1) else null
    FaceId.D -> if (iy == -1) 27 + (iz + 1) * 3 + (ix + 1) else null
    FaceId.L -> if (ix == -1) 36 + (1 - iy) * 3 + (1 - iz) else null
    FaceId.B -> if (iz == 1) 45 + (1 - iy) * 3 + (1 - ix) else null
}

private fun CubieFaceSpec.isInLayer(face: MoveFace): Boolean = when (face) {
    MoveFace.U -> iy == 1
    MoveFace.D -> iy == -1
    MoveFace.R -> ix == 1
    MoveFace.L -> ix == -1
    MoveFace.F -> iz == -1
    MoveFace.B -> iz == 1
}

private fun MoveFace.rotationAxis(): RotationAxis = when (this) {
    MoveFace.U, MoveFace.D -> RotationAxis.Y
    MoveFace.R, MoveFace.L -> RotationAxis.X
    MoveFace.F, MoveFace.B -> RotationAxis.Z
}

private fun CubeMove.targetSliceAngleDeg(): Float {
    val baseCw = when (face) {
        MoveFace.U -> 90f
        MoveFace.D -> -90f
        MoveFace.R -> 90f
        MoveFace.L -> -90f
        MoveFace.F -> -90f
        MoveFace.B -> 90f
    }
    return when (quarterTurns) {
        1 -> baseCw
        2 -> baseCw * 2f
        else -> -baseCw
    }
}

/**
 * Interactive 3D perspective Rubik's Cube using Painter's algorithm, touch-to-orbit rotation
 * across all 6 faces, 3D edge-parallel move arrows, and animated 90/180-degree layer turns
 * during Play and step navigation.
 */
@Composable
fun Interactive3DCubeView(
    cube: CubeState,
    solution: CfopSolution?,
    currentStepIndex: Int,
    activeMove: CubeMove?,
    playStepDelayMs: Long,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "arrowPulse3D")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(680),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse3D"
    )

    val defaultAngleX = -25f
    val defaultAngleY = 35f
    var cameraAngleX by remember { mutableFloatStateOf(defaultAngleX) }
    var cameraAngleY by remember { mutableFloatStateOf(defaultAngleY) }
    val isDefaultAngle = abs(cameraAngleX - defaultAngleX) < 1.5f && abs(cameraAngleY - defaultAngleY) < 1.5f

    // Track step transitions to animate single-step forward/backward turns in 3D
    var lastObservedSolution by remember { mutableStateOf<CfopSolution?>(null) }
    var lastObservedStepIndex by remember { mutableIntStateOf(currentStepIndex) }
    var animBaseCube by remember { mutableStateOf(cube) }
    var animMove by remember { mutableStateOf<CubeMove?>(null) }
    var animStep by remember { mutableStateOf<CfopStep?>(null) }
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(solution, currentStepIndex) {
        val sol = solution
        val prevSol = lastObservedSolution
        val prevIdx = lastObservedStepIndex
        lastObservedSolution = sol
        lastObservedStepIndex = currentStepIndex

        if (sol != null && sol === prevSol && currentStepIndex == prevIdx + 1 && prevIdx in sol.steps.indices) {
            val step = sol.steps[prevIdx]
            animBaseCube = step.cubeBefore
            animMove = step.move
            animStep = step
            val durationMs = (playStepDelayMs * 0.62f).toInt().coerceIn(220, 480)
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            )
            animMove = null
            animStep = null
        } else if (sol != null && sol === prevSol && currentStepIndex == prevIdx - 1 && currentStepIndex in sol.steps.indices) {
            val step = sol.steps[currentStepIndex]
            animBaseCube = step.cubeAfter
            animMove = step.move.inverse()
            animStep = step
            val durationMs = (playStepDelayMs * 0.62f).toInt().coerceIn(220, 480)
            animProgress.snapTo(0f)
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
            )
            animMove = null
            animStep = null
        } else {
            animMove = null
            animStep = null
            animProgress.snapTo(0f)
        }
    }

    val renderedCube = if (animMove != null) animBaseCube else cube
    val activeSliceFace = animMove?.face
    val activeSliceAngle = if (animMove != null) {
        animMove!!.targetSliceAngleDeg() * animProgress.value
    } else 0f
    val displayStep = animStep ?: solution?.steps?.getOrNull(currentStepIndex)
    val highlightedCubies = remember(renderedCube, displayStep) {
        findHighlightedCubies(renderedCube, displayStep)
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("cube_3d_canvas")
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        cameraAngleY -= dragAmount.x * 0.42f
                        cameraAngleX = (cameraAngleX - dragAmount.y * 0.42f).coerceIn(-82f, 82f)
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val viewerDistance = 7.2f
            val fov = minOf(w, h) * 1.46f

            draw3DRubikCube(
                cube = renderedCube,
                cameraAngleX = cameraAngleX,
                cameraAngleY = cameraAngleY,
                viewerDistance = viewerDistance,
                fov = fov,
                activeSliceFace = activeSliceFace,
                activeSliceAngleDeg = activeSliceAngle,
                highlightedCubies = highlightedCubies,
                highlightPulse = pulse
            )

            // Show the 3D move arrow & badge when settled (not mid-rotation)
            if (activeMove != null && animMove == null) {
                draw3DMoveArrowOverlay(
                    move = activeMove,
                    cameraAngleX = cameraAngleX,
                    cameraAngleY = cameraAngleY,
                    viewerDistance = viewerDistance,
                    fov = fov,
                    pulse = pulse,
                    textMeasurer = textMeasurer
                )
            }
        }

        // Subtle drag hint or Reset Angle button
        if (!isDefaultAngle) {
            FilledTonalButton(
                onClick = {
                    cameraAngleX = defaultAngleX
                    cameraAngleY = defaultAngleY
                },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .testTag("reset_3d_camera_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = tr("Góc chuẩn", "Reset 3D"),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        } else {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SoftBlueSurface.copy(alpha = 0.85f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = RubikBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = tr("Vuốt để xoay 3D", "Drag to rotate 3D"),
                        style = MaterialTheme.typography.labelSmall,
                        color = InkSecondary
                    )
                }
            }
        }
    }
}

/**
 * Compatibility wrapper preserving existing IsometricCubeView signature while rendering in 3D.
 */
@Composable
fun IsometricCubeView(
    cube: CubeState,
    activeMove: CubeMove? = null,
    modifier: Modifier = Modifier
) {
    Interactive3DCubeView(
        cube = cube,
        solution = null,
        currentStepIndex = 0,
        activeMove = activeMove,
        playStepDelayMs = 1000L,
        modifier = modifier
    )
}

private data class ProjectedCubieFace(
    val plasticPts: List<Offset>,
    val stickerPts: List<Offset>?,
    val stickerColor: Color?,
    val isHighlighted: Boolean,
    val avgZ: Float
)

private fun DrawScope.draw3DRubikCube(
    cube: CubeState,
    cameraAngleX: Float,
    cameraAngleY: Float,
    viewerDistance: Float,
    fov: Float,
    activeSliceFace: MoveFace?,
    activeSliceAngleDeg: Float,
    highlightedCubies: Set<Triple<Int, Int, Int>>,
    highlightPulse: Float
) {
    val w = size.width
    val h = size.height
    val cameraOrigin = Point3D(0f, 0f, -viewerDistance)
    val sliceAxis = activeSliceFace?.rotationAxis()

    val visibleFaces = ArrayList<ProjectedCubieFace>(100)

    for (spec in CUBIE_FACE_SPECS) {
        val inRotatingSlice = activeSliceFace != null && spec.isInLayer(activeSliceFace) && activeSliceAngleDeg != 0f

        fun transformPoint(pt: Point3D): Point3D {
            val local = if (inRotatingSlice && sliceAxis != null) {
                pt.rotateAxis(sliceAxis, activeSliceAngleDeg)
            } else pt
            return local.rotateY(cameraAngleY).rotateX(cameraAngleX)
        }

        fun transformNormal(n: Point3D): Point3D {
            val local = if (inRotatingSlice && sliceAxis != null) {
                n.rotateAxis(sliceAxis, activeSliceAngleDeg)
            } else n
            return local.rotateY(cameraAngleY).rotateX(cameraAngleX)
        }

        val camNormal = transformNormal(spec.normal)
        val camCenter = transformPoint(spec.center)
        // Perspective back-face culling: skip faces pointing away from camera
        if (camNormal.dot(camCenter - cameraOrigin) >= 0f) continue

        val camPlastic = spec.plasticVerts.map(::transformPoint)
        val avgZ = (camPlastic[0].z + camPlastic[1].z + camPlastic[2].z + camPlastic[3].z) / 4f
        val plasticPts = camPlastic.map { it.project(w, h, fov, viewerDistance) }

        val stickerColor = spec.faceletIndex?.let { cube.colorAt(it).toStickerColor() }
        val stickerPts = if (stickerColor != null && spec.stickerVerts != null) {
            spec.stickerVerts.map { transformPoint(it).project(w, h, fov, viewerDistance) }
        } else null
        val isHighlighted = Triple(spec.ix, spec.iy, spec.iz) in highlightedCubies

        visibleFaces.add(
            ProjectedCubieFace(
                plasticPts = plasticPts,
                stickerPts = stickerPts,
                stickerColor = stickerColor,
                isHighlighted = isHighlighted,
                avgZ = avgZ
            )
        )
    }

    // Painter's Algorithm: draw farthest faces first, nearest faces last
    visibleFaces.sortByDescending { it.avgZ }

    val highlightRimColor = Color(0xFFD946EF) // Vivid magenta-purple (distinct from all 6 cube colors)
    val highlightPlasticGray = Color(0xFFCBD5E1) // Crisp silver-gray frame around target cubies
    val path = Path()
    for (face in visibleFaces) {
        path.reset()
        face.plasticPts.forEachIndexed { idx, pt ->
            if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
        }
        path.close()
        if (face.isHighlighted) {
            drawPath(path, color = highlightPlasticGray)
            drawPath(
                path,
                color = highlightRimColor,
                style = Stroke(width = 5.5f * highlightPulse, join = StrokeJoin.Round)
            )
        } else {
            drawPath(path, color = CubePlasticDark)
            drawPath(path, color = Color(0xFF0F1528), style = Stroke(width = 1.6f, join = StrokeJoin.Round))
        }

        if (face.stickerPts != null && face.stickerColor != null) {
            path.reset()
            face.stickerPts.forEachIndexed { idx, pt ->
                if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
            }
            path.close()
            drawPath(path, color = face.stickerColor)
            drawPath(
                path,
                color = if (face.isHighlighted) highlightRimColor.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.22f),
                style = Stroke(width = if (face.isHighlighted) 2.4f else 1.2f, join = StrokeJoin.Round)
            )
        }
    }
}

private enum class LabelPlacement {
    ABOVE,
    BELOW,
    LEFT,
    RIGHT
}

private fun DrawScope.draw3DMoveArrowOverlay(
    move: CubeMove,
    cameraAngleX: Float,
    cameraAngleY: Float,
    viewerDistance: Float,
    fov: Float,
    pulse: Float,
    textMeasurer: TextMeasurer
) {
    val w = size.width
    val h = size.height
    val arrowColor = if (move.quarterTurns == 2) ToyOrange else RubikBlue
    val cw = move.quarterTurns != 3 // 1=CW, 3=CCW, 2=180
    val strokeWidth = 9f * pulse

    fun proj3D(pt: Point3D): Offset =
        pt.rotateY(cameraAngleY).rotateX(cameraAngleX).project(w, h, fov, viewerDistance)

    when (move.face) {
        MoveFace.U -> {
            val pLeft = Point3D(-1.05f, 1.68f, -1.20f)
            val pRight = Point3D(+1.05f, 1.68f, -1.20f)
            val (s3, e3) = if (cw) pRight to pLeft else pLeft to pRight
            drawEdgeParallelArrow(listOf(proj3D(s3), proj3D(e3)), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(0f, 2.02f, -1.20f)),
                placement = LabelPlacement.ABOVE,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }

        MoveFace.D -> {
            val pLeft = Point3D(-1.05f, -1.70f, -1.20f)
            val pRight = Point3D(+1.05f, -1.70f, -1.20f)
            val (s3, e3) = if (cw) pLeft to pRight else pRight to pLeft
            drawEdgeParallelArrow(listOf(proj3D(s3), proj3D(e3)), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(0f, -2.04f, -1.20f)),
                placement = LabelPlacement.BELOW,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }

        MoveFace.L -> {
            val pTop = Point3D(-1.74f, +1.05f, -1.22f)
            val pBot = Point3D(-1.74f, -1.05f, -1.22f)
            val (s3, e3) = if (cw) pTop to pBot else pBot to pTop
            drawEdgeParallelArrow(listOf(proj3D(s3), proj3D(e3)), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(-1.95f, 0f, -1.22f)),
                placement = LabelPlacement.LEFT,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }

        MoveFace.F -> {
            // 2-segment corner arrow floating parallel to Front face edges
            val pBot = Point3D(-1.02f, -0.52f, -1.66f)
            val pCorner = Point3D(-1.02f, +1.02f, -1.66f)
            val pRight = Point3D(+0.62f, +1.02f, -1.66f)
            val pts = if (cw) listOf(pBot, pCorner, pRight) else listOf(pRight, pCorner, pBot)
            drawEdgeParallelArrow(pts.map(::proj3D), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(-1.78f, +0.25f, -1.66f)),
                placement = LabelPlacement.LEFT,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }

        MoveFace.R -> {
            // 2-segment corner arrow floating parallel to Right face edges, mirroring F/F'
            val pBot = Point3D(+1.66f, -0.52f, -1.02f)
            val pCorner = Point3D(+1.66f, +1.02f, -1.02f)
            val pBack = Point3D(+1.66f, +1.02f, +0.62f)
            val pts = if (cw) listOf(pBot, pCorner, pBack) else listOf(pBack, pCorner, pBot)
            drawEdgeParallelArrow(pts.map(::proj3D), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(+1.86f, +0.25f, +0.95f)),
                placement = LabelPlacement.RIGHT,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }

        MoveFace.B -> {
            val pLeft = Point3D(-1.05f, +1.68f, +1.36f)
            val pRight = Point3D(+1.05f, +1.68f, +1.36f)
            val (s3, e3) = if (cw) pRight to pLeft else pLeft to pRight
            drawEdgeParallelArrow(listOf(proj3D(s3), proj3D(e3)), arrowColor, strokeWidth)
            drawMoveLabelBadge(
                notation = move.notation,
                anchor = proj3D(Point3D(+1.32f, +1.76f, +1.36f)),
                placement = LabelPlacement.RIGHT,
                badgeColor = arrowColor,
                textMeasurer = textMeasurer
            )
        }
    }
}

private fun DrawScope.drawEdgeParallelArrow(
    points: List<Offset>,
    color: Color,
    strokeWidth: Float
) {
    if (points.size < 2) return
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }
    drawPath(
        path = path,
        color = Color.White,
        style = Stroke(width = strokeWidth + 6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    val prev = points[points.size - 2]
    val end = points.last()
    drawArrowHead(prev, end, Color.White, strokeWidth + 6f)
    drawArrowHead(prev, end, color, strokeWidth)
}

private fun DrawScope.drawArrowHead(
    from: Offset,
    tip: Offset,
    color: Color,
    strokeWidth: Float
) {
    val angle = atan2((tip.y - from.y).toDouble(), (tip.x - from.x).toDouble())
    val headLen = strokeWidth * 2.1f
    val spread = Math.toRadians(34.0)
    val leftWing = Offset(
        tip.x - (headLen * cos(angle - spread)).toFloat(),
        tip.y - (headLen * sin(angle - spread)).toFloat()
    )
    val rightWing = Offset(
        tip.x - (headLen * cos(angle + spread)).toFloat(),
        tip.y - (headLen * sin(angle + spread)).toFloat()
    )
    val headPath = Path().apply {
        moveTo(leftWing.x, leftWing.y)
        lineTo(tip.x, tip.y)
        lineTo(rightWing.x, rightWing.y)
    }
    drawPath(
        path = headPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawMoveLabelBadge(
    notation: String,
    anchor: Offset,
    placement: LabelPlacement,
    badgeColor: Color,
    textMeasurer: TextMeasurer
) {
    val measured = textMeasurer.measure(
        text = notation,
        style = TextStyle(
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
    )
    val padH = 12.dp.toPx()
    val padV = 5.dp.toPx()
    val boxW = measured.size.width + padH * 2f
    val boxH = measured.size.height + padV * 2f

    val topLeft = when (placement) {
        LabelPlacement.ABOVE -> Offset(anchor.x - boxW / 2f, anchor.y - boxH / 2f)
        LabelPlacement.BELOW -> Offset(anchor.x - boxW / 2f, anchor.y - boxH / 2f)
        LabelPlacement.LEFT -> Offset(anchor.x - boxW, anchor.y - boxH / 2f)
        LabelPlacement.RIGHT -> Offset(anchor.x, anchor.y - boxH / 2f)
    }

    val clampedX = topLeft.x.coerceIn(4f, (size.width - boxW - 4f).coerceAtLeast(4f))
    val clampedY = topLeft.y.coerceIn(4f, (size.height - boxH - 4f).coerceAtLeast(4f))
    val finalTopLeft = Offset(clampedX, clampedY)

    drawRoundRect(
        color = Color.White,
        topLeft = Offset(finalTopLeft.x - 2.5f, finalTopLeft.y - 2.5f),
        size = Size(boxW + 5f, boxH + 5f),
        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
    )
    drawRoundRect(
        color = badgeColor,
        topLeft = finalTopLeft,
        size = Size(boxW, boxH),
        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(finalTopLeft.x + padH, finalTopLeft.y + padV)
    )
}

private fun lerp3D(a: Point3D, b: Point3D, t: Float): Point3D =
    Point3D(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t)

/**
 * Compact 2D unfolded net of all 6 faces (U, L-F-R-B, D) so kids can inspect every side.
 */
@Composable
fun UnfoldedCubeNet(
    cube: CubeState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.size(48.dp))
            MiniFaceGrid(colors = cube.faceColors(FaceId.U), label = "U", modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.size(48.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            MiniFaceGrid(colors = cube.faceColors(FaceId.L), label = "L", modifier = Modifier.size(48.dp))
            MiniFaceGrid(colors = cube.faceColors(FaceId.F), label = "F", modifier = Modifier.size(48.dp))
            MiniFaceGrid(colors = cube.faceColors(FaceId.R), label = "R", modifier = Modifier.size(48.dp))
            MiniFaceGrid(colors = cube.faceColors(FaceId.B), label = "B", modifier = Modifier.size(48.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Spacer(modifier = Modifier.size(48.dp))
            MiniFaceGrid(colors = cube.faceColors(FaceId.D), label = "D", modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.size(48.dp))
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
fun MiniFaceGrid(
    colors: List<CubeColor>,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        color = CubePlasticDark
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(3.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellW = size.width / 3f
                val cellH = size.height / 3f
                val pad = cellW * 0.08f
                for (r in 0 until 3) {
                    for (c in 0 until 3) {
                        val color = colors.getOrNull(r * 3 + c)?.toStickerColor() ?: Color.Gray
                        drawRoundRect(
                            color = color,
                            topLeft = Offset(c * cellW + pad, r * cellH + pad),
                            size = Size(cellW - pad * 2, cellH - pad * 2),
                            cornerRadius = CornerRadius(4f, 4f)
                        )
                    }
                }
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
