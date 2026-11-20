package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.LocalAppLanguage
import com.example.data.TrainingRepository
import com.example.data.tr
import com.example.rubik.CfopStage
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.InkSecondary
import com.example.ui.theme.OutlineSoft
import com.example.ui.theme.RubikBlue
import com.example.ui.theme.SoftAmberSurface
import com.example.ui.theme.SoftBlueSurface
import com.example.ui.theme.SoftGreenSurface
import com.example.ui.theme.SunnyYellow
import com.example.ui.theme.ToyGreen
import com.example.ui.theme.ToyOrange
import com.example.ui.theme.WarmSurface

@Composable
fun ToyBlockCard(
    modifier: Modifier = Modifier,
    containerColor: Color = WarmSurface,
    borderColor: Color = OutlineSoft,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = containerColor,
        border = BorderStroke(2.dp, borderColor),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

fun CfopStage.accentColor(): Color = when (this) {
    CfopStage.CROSS -> RubikBlue
    CfopStage.F2L -> ToyGreen
    CfopStage.OLL -> SunnyYellow
    CfopStage.PLL -> ToyOrange
}

fun CfopStage.softSurfaceColor(): Color = when (this) {
    CfopStage.CROSS -> SoftBlueSurface
    CfopStage.F2L -> SoftGreenSurface
    CfopStage.OLL -> SoftAmberSurface
    CfopStage.PLL -> Color(0xFFFFF0E2)
}

@Composable
fun CfopStageBadge(
    stage: CfopStage,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    val bg = if (isActive) stage.accentColor() else stage.softSurfaceColor()
    val fg = when {
        isActive && stage == CfopStage.OLL -> InkPrimary
        isActive -> Color.White
        else -> InkPrimary
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(1.5.dp, stage.accentColor())
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stage.enTitle,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            if (lang == AppLanguage.VI) {
                Text(
                    text = "• ${stage.viTitle}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg.copy(alpha = 0.88f)
                )
            }
        }
    }
}

@Composable
fun DailyGoalRing(
    completed: Int,
    target: Int,
    size: Dp = 84.dp,
    modifier: Modifier = Modifier
) {
    val safeTarget = target.coerceAtLeast(1)
    val fraction = (completed.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            drawArc(
                color = OutlineSoft,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = if (completed >= safeTarget) ToyGreen else RubikBlue,
                startAngle = -90f,
                sweepAngle = 360f * fraction,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$completed/$safeTarget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = InkPrimary
            )
            Text(
                text = tr("lượt", "solves"),
                style = MaterialTheme.typography.bodyMedium,
                color = InkSecondary
            )
        }
    }
}

@Composable
fun PhaseSplitBar(
    crossMs: Long,
    f2lMs: Long,
    ollMs: Long,
    pllMs: Long,
    modifier: Modifier = Modifier
) {
    val total = (crossMs + f2lMs + ollMs + pllMs).coerceAtLeast(1L).toFloat()
    val segments = listOf(
        Triple("Cross", crossMs, RubikBlue),
        Triple("F2L", f2lMs, ToyGreen),
        Triple("OLL", ollMs, SunnyYellow),
        Triple("PLL", pllMs, ToyOrange)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            var x = 0f
            val w = size.width
            val h = size.height
            segments.forEachIndexed { idx, (_, ms, color) ->
                val segW = (ms / total) * w
                if (segW > 1f) {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, 0f),
                        size = Size((segW - if (idx < 3) 4f else 0f).coerceAtLeast(2f), h),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
                x += segW
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            segments.forEach { (label, ms, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color = color)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = InkPrimary
                        )
                        Text(
                            text = TrainingRepository.formatDurationMs(ms),
                            style = MaterialTheme.typography.labelMedium,
                            color = InkSecondary
                        )
                    }
                }
            }
        }
    }
}
