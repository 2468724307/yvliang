package com.yuliang.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import com.yuliang.app.ui.theme.YuliangSizes

enum class YuliangIcon { HOME, BILLS, STATISTICS, PROFILE, ADD }

/** A single rounded outline family for navigation and quick add. */
@Composable
fun YuliangIcon(kind: YuliangIcon, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    Canvas(modifier.size(YuliangSizes.iconNormal)) {
        val w = size.width
        val h = size.height
        val stroke = w / 12f
        val line: (Float, Float, Float, Float) -> Unit = { x1, y1, x2, y2 ->
            drawLine(ink, Offset(w * x1, h * y1), Offset(w * x2, h * y2), strokeWidth = stroke, cap = StrokeCap.Round)
        }
        when (kind) {
            YuliangIcon.HOME -> {
                val roof = Path().apply { moveTo(w * .16f, h * .45f); lineTo(w * .5f, h * .16f); lineTo(w * .84f, h * .45f) }
                drawPath(roof, ink, style = Stroke(stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
                line(.24f, .42f, .24f, .84f); line(.24f, .84f, .76f, .84f); line(.76f, .84f, .76f, .42f)
                line(.42f, .84f, .42f, .62f); line(.42f, .62f, .58f, .62f); line(.58f, .62f, .58f, .84f)
            }
            YuliangIcon.BILLS -> {
                line(.2f, .24f, .8f, .24f); line(.2f, .5f, .8f, .5f); line(.2f, .76f, .64f, .76f)
            }
            YuliangIcon.STATISTICS -> {
                line(.18f, .82f, .82f, .82f)
                line(.28f, .72f, .28f, .54f); line(.5f, .72f, .5f, .2f); line(.72f, .72f, .72f, .38f)
            }
            YuliangIcon.PROFILE -> {
                drawCircle(ink, radius = w * .14f, center = Offset(w * .5f, h * .32f), style = Stroke(stroke))
                drawArc(ink, startAngle = 195f, sweepAngle = 150f, useCenter = false,
                    topLeft = Offset(w * .22f, h * .46f), size = androidx.compose.ui.geometry.Size(w * .56f, h * .45f),
                    style = Stroke(stroke, cap = StrokeCap.Round))
            }
            YuliangIcon.ADD -> { line(.5f, .18f, .5f, .82f); line(.18f, .5f, .82f, .5f) }
        }
    }
}
