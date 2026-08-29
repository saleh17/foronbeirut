package com.foronbeirut.manakich

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/**
 * The shop itself — everything that is scenery rather than a control.
 *
 * All of it is drawn in the design canvas's 844 x 390 units, so a coordinate in
 * ART_BRIEF.md can be used here without conversion.
 */

private const val W = 844f
private const val H = 390f
private const val COUNTER_Y = 250f

private fun poly(u: Float, vararg xy: Float): Path = Path().apply {
    moveTo(xy[0] * u, xy[1] * u)
    var i = 2
    while (i < xy.size) {
        lineTo(xy[i] * u, xy[i + 1] * u)
        i += 2
    }
    close()
}

fun DrawScope.drawWall() {
    val u = size.width / W

    // ---- wall: coursed stone, warmer where the furn throws light
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFCDA97F), Color(0xFFB08A62))),
        size = Size(W * u, COUNTER_Y * u),
    )
    val courseH = 30f
    var y = 0f
    var row = 0
    while (y < COUNTER_Y) {
        var x = if (row % 2 == 0) 0f else -34f
        while (x < W) {
            drawRect(Color(0x14FFFFFF), topLeft = Offset(x * u, y * u), size = Size(66f * u, 2f * u))
            drawRect(Color(0x22000000), topLeft = Offset((x + 66f) * u, y * u), size = Size(2f * u, courseH * u))
            x += 68f
        }
        drawRect(Color(0x1E000000), topLeft = Offset(0f, (y + courseH - 2f) * u), size = Size(W * u, 2f * u))
        y += courseH
        row++
    }
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x59FFC46A), Color(0x00FFC46A)),
            center = Offset(120f * u, 150f * u),
            radius = 400f * u,
        ),
        size = Size(W * u, COUNTER_Y * u),
    )
    drawRect(
        Brush.verticalGradient(listOf(Color(0x4D2A1B12), Color(0x00000000))),
        size = Size(W * u, 70f * u),
    )

    // ---- the door they come and go through
    val dx = 706f
    val dw = 112f
    drawRect(Color(0xFF6B4A2E), topLeft = Offset((dx - 8f) * u, 44f * u), size = Size((dw + 16f) * u, 208f * u))
    drawRect(
        Color(0xFF2A1B12), topLeft = Offset((dx - 8f) * u, 44f * u),
        size = Size((dw + 16f) * u, 208f * u), style = Stroke(width = 4f * u),
    )
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF9DC6D4), Color(0xFF6E9FB0))),
        topLeft = Offset(dx * u, 52f * u), size = Size(dw * u, 118f * u),
    )
    drawLine(Color(0xFF6B4A2E), Offset((dx + dw / 2) * u, 52f * u), Offset((dx + dw / 2) * u, 170f * u), 5f * u)
    drawLine(Color(0xFF6B4A2E), Offset(dx * u, 111f * u), Offset((dx + dw) * u, 111f * u), 5f * u)
    drawRect(Color(0x33FFFFFF), topLeft = Offset((dx + 6f) * u, 58f * u), size = Size(34f * u, 46f * u))
    drawRect(Color(0xFF7E5A3A), topLeft = Offset(dx * u, 178f * u), size = Size(dw * u, 70f * u))
    drawCircle(Color(0xFFE0A339), radius = 5f * u, center = Offset((dx + dw - 14f) * u, 176f * u))

    // ---- the flag, hung between the furn and the queue
    val fx = 236f
    drawLine(Color(0xFF4A4038), Offset(fx * u, 52f * u), Offset(fx * u, 150f * u), 3f * u)
    val flag = poly(u, fx, 56f, fx + 54f, 62f, fx + 54f, 110f, fx, 104f)
    drawPath(flag, Color(0xFFF4F1E8))
    drawPath(poly(u, fx, 56f, fx + 54f, 62f, fx + 54f, 74f, fx, 68f), Color(0xFFD03A2C))
    drawPath(poly(u, fx, 92f, fx + 54f, 98f, fx + 54f, 110f, fx, 104f), Color(0xFFD03A2C))
    val cx = fx + 27f
    val cy = 86f
    drawPath(
        poly(u, cx, cy - 12f, cx + 11f, cy + 2f, cx + 5f, cy + 2f, cx + 13f, cy + 8f,
            cx - 13f, cy + 8f, cx - 5f, cy + 2f, cx - 11f, cy + 2f),
        Color(0xFF2F6B34),
    )
    drawRect(Color(0xFF2F6B34), topLeft = Offset((cx - 1.5f) * u, (cy + 8f) * u), size = Size(3f * u, 4f * u))
    drawPath(flag, Color(0xFF2A1B12), style = Stroke(width = 2.5f * u))

}

fun DrawScope.drawCounter() {
    val u = size.width / W

    // a lip you can see the edge of, then the front planking
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFD8A874), Color(0xFFC08E5E))),
        topLeft = Offset(0f, COUNTER_Y * u), size = Size(W * u, 16f * u),
    )
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF9A6E44), Color(0xFF5E4028))),
        topLeft = Offset(0f, (COUNTER_Y + 16f) * u),
        size = Size(W * u, (H - COUNTER_Y - 16f) * u),
    )
    drawLine(Color(0x55000000), Offset(0f, (COUNTER_Y + 16f) * u), Offset(W * u, (COUNTER_Y + 16f) * u), 3f * u)
    var px = 0f
    while (px < W) {
        drawLine(Color(0x22000000), Offset(px * u, (COUNTER_Y + 16f) * u), Offset(px * u, H * u), 2f * u)
        px += 62f
    }
    val dash = PathEffect.dashPathEffect(floatArrayOf(26f * u, 34f * u), 0f)
    for (gy in listOf(254f, 259f, 263f)) {
        drawLine(Color(0x1E000000), Offset(0f, gy * u), Offset(W * u, gy * u), 1.6f * u, pathEffect = dash)
    }
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x40FFC46A), Color(0x00FFC46A)),
            center = Offset(120f * u, COUNTER_Y * u),
            radius = 340f * u,
        ),
        topLeft = Offset(0f, COUNTER_Y * u), size = Size(W * u, (H - COUNTER_Y) * u),
    )
}

// ---------------------------------------------------------------- props

/** Glazed ceramic: terracotta body, cream band, cobalt stripe, a rim with a far edge. */
fun DrawScope.drawDoughBowl(balls: Int) {
    val u = size.width / 140f
    val h = size.height / u
    drawOval(Color(0x4D000000), topLeft = Offset(6f * u, (h - 10f) * u), size = Size(128f * u, 12f * u))
    val body = Path().apply {
        moveTo(6f * u, 18f * u)
        lineTo(134f * u, 18f * u)
        cubicTo(134f * u, (h - 4f) * u, 96f * u, h * u, 70f * u, h * u)
        cubicTo(44f * u, h * u, 6f * u, (h - 4f) * u, 6f * u, 18f * u)
        close()
    }
    drawPath(
        body,
        Brush.verticalGradient(
            listOf(Color(0xFFF0E6D0), Color(0xFFE6DAC0), Color(0xFFC07C51), Color(0xFF8A4F30), Color(0xFF5E3320)),
            startY = 18f * u, endY = h * u,
        ),
    )
    drawRect(Color(0xCC2F6FA8), topLeft = Offset(6f * u, 40f * u), size = Size(128f * u, 5f * u))
    drawRect(Color(0x662F6FA8), topLeft = Offset(6f * u, 50f * u), size = Size(128f * u, 2.5f * u))
    drawPath(body, Palette.Ink, style = Stroke(width = 4f * u))

    drawOval(
        Brush.verticalGradient(listOf(Color(0xFF4A2A17), Color(0xFF34190F)), startY = 6f * u, endY = 34f * u),
        topLeft = Offset(12f * u, 6f * u), size = Size(116f * u, 30f * u),
    )
    val spots = listOf(24f to 10f, 56f to 6f, 88f to 10f, 40f to 18f, 72f to 18f).take(balls.coerceIn(0, 5))
    for ((bx, by) in spots) {
        drawCircle(Color(0x59000000), radius = 13f * u, center = Offset((bx + 14f) * u, (by + 15f) * u))
        drawCircle(
            Brush.radialGradient(
                listOf(Color(0xFFFEF6E2), Color(0xFFDCC291)),
                center = Offset((bx + 8f) * u, (by + 8f) * u), radius = 22f * u,
            ),
            radius = 13f * u, center = Offset((bx + 14f) * u, (by + 13f) * u),
        )
        drawCircle(Palette.Ink, radius = 13f * u, center = Offset((bx + 14f) * u, (by + 13f) * u), style = Stroke(2.5f * u))
    }
    drawOval(Color(0xFFD8C49E), topLeft = Offset(8f * u, 2f * u), size = Size(124f * u, 34f * u), style = Stroke(width = 7f * u))
    drawOval(Palette.Ink, topLeft = Offset(8f * u, 2f * u), size = Size(124f * u, 34f * u), style = Stroke(width = 2.5f * u))
}

/** A stainless insert: rolled rim, deep well, the product filling from the bottom. */
fun DrawScope.drawTray(fill: Brush, level: Float) {
    val u = size.width / 100f
    val h = size.height / u
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFE4E8EC), Color(0xFFC2C8CE), Color(0xFF9BA2A9), Color(0xFF767D84))),
        size = Size(100f * u, h * u),
    )
    drawRect(Brush.verticalGradient(listOf(Color(0xFFF2F5F8), Color(0xFF9AA1A8))), size = Size(100f * u, 9f * u))
    val well = Rect(7f * u, 9f * u, 93f * u, (h - 5f) * u)
    drawRect(Color(0xFF3E444A), topLeft = well.topLeft, size = well.size)
    val fillH = (well.height * level).coerceIn(0f, well.height)
    drawRect(fill, topLeft = Offset(well.left, well.bottom - fillH), size = Size(well.width, fillH))
    drawRect(
        Brush.verticalGradient(listOf(Color(0x66FFFFFF), Color(0x00FFFFFF))),
        topLeft = Offset(well.left, well.bottom - fillH),
        size = Size(well.width, (5f * u).coerceAtMost(fillH)),
    )
    drawRect(
        Brush.verticalGradient(listOf(Color(0xA6000000), Color(0x00000000))),
        topLeft = well.topLeft, size = Size(well.width, 8f * u),
    )
    drawRect(Palette.Ink, size = Size(100f * u, h * u), style = Stroke(width = 3f * u))
}

/** The peel: steel blade with a highlight, wooden shaft and grip. */
fun DrawScope.drawPeel(slots: Int) {
    val u = size.width / 120f
    val h = size.height / u
    drawRect(
        Brush.horizontalGradient(listOf(Color(0xFFE4E8EC), Color(0xFFBCC1C7), Color(0xFF8E939A))),
        topLeft = Offset(0f, 6f * u), size = Size(96f * u, (h - 12f) * u),
    )
    drawRect(Color(0x47FFFFFF), topLeft = Offset(6f * u, 12f * u), size = Size(5f * u, (h - 24f) * u))
    drawRect(Palette.Ink, topLeft = Offset(0f, 6f * u), size = Size(96f * u, (h - 12f) * u), style = Stroke(width = 3f * u))
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFFC08E5C), Color(0xFF6F4A2E))),
        topLeft = Offset(96f * u, (h / 2 - 7f) * u), size = Size(20f * u, 14f * u),
    )
    drawRect(
        Palette.Ink, topLeft = Offset(96f * u, (h / 2 - 7f) * u),
        size = Size(20f * u, 14f * u), style = Stroke(width = 2.5f * u),
    )
    for (i in 0 until slots) {
        drawCircle(
            Color(0x552A1B12), radius = 12f * u,
            center = Offset((18f + i * 30f) * u, (h / 2) * u), style = Stroke(width = 2f * u),
        )
    }
}

/** One compartment of the khodra insert, with the produce heaped in it. */
fun DrawScope.drawKhodraWell(colour: Color, wanted: Boolean, on: Boolean) {
    val u = size.width / 40f
    val h = size.height / u
    drawRect(Color(0xFF3E444A), size = size)
    drawRect(colour, topLeft = Offset(2f * u, 6f * u), size = Size(36f * u, (h - 8f) * u))
    for ((hx, hy, r) in listOf(Triple(12f, 14f, 7f), Triple(27f, 20f, 6f), Triple(18f, 28f, 6.5f))) {
        drawCircle(Color(0x33FFFFFF), radius = r * u, center = Offset(hx * u, hy * u))
        drawCircle(Color(0x2E000000), radius = r * u, center = Offset((hx + 1.5f) * u, (hy + 2f) * u))
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x99000000), Color(0x00000000))), size = Size(40f * u, 7f * u))
    val edge = if (on) Palette.Good else if (wanted) Palette.Select else null
    if (edge != null) drawRect(edge, size = size, style = Stroke(width = 3f * u))
}

/** The wrapping bench: a marble slab with a sheet of paper waiting on it. */
fun DrawScope.drawBench() {
    val u = size.width / 200f
    val h = size.height / u
    drawRect(Brush.verticalGradient(listOf(Color(0xFFEDE9DE), Color(0xFFCFC9B8))), size = size)
    for (v in listOf(0.22f, 0.55f, 0.78f)) {
        drawLine(Color(0x1A2A1B12), Offset(10f * u, h * v * u), Offset(190f * u, (h * v - 8f) * u), 2f * u)
    }
    drawRect(Color(0xFFFAF6EA), topLeft = Offset(8f * u, 8f * u), size = Size(184f * u, (h - 16f) * u))
    drawRect(Palette.Ink, topLeft = Offset(8f * u, 8f * u), size = Size(184f * u, (h - 16f) * u), style = Stroke(width = 2f * u))
    drawRect(Palette.Ink, size = size, style = Stroke(width = 3f * u))
}
