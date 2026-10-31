package app.curator.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import app.curator.ui.theme.C

/**
 * The museum building, drawn in a 360×214 box. Part 0 is the main block with its portico; then the left wing,
 * the gallery on the right, the rotunda on the roof, the observatory tower and the sculpture garden. A part not
 * yet open stands as a dashed outline in scaffolding. [glow] lights one part up (the one just opened).
 */
@Composable
fun Facade(open: Int, modifier: Modifier = Modifier, glow: Int = -1) {
    Canvas(modifier.aspectRatio(360f / 214f)) {
        val k = size.width / 360f
        withTransform({ scale(k, k, Offset.Zero) }) { building(open, glow) }
    }
}

private val stone = Color(0xFFE2D5B8)
private val stoneLine = Color(0xFF8F6B38)
private val column = Color(0xFFF2EAD8)
private val dash = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))

private fun DrawScope.part(open: Boolean, path: Path) {
    if (open) {
        drawPath(path, stone)
        drawPath(path, stoneLine, style = Stroke(1.5f))
    } else drawPath(path, C.paper.copy(alpha = .4f), style = Stroke(1.2f, pathEffect = dash))
}

private fun rect(x: Float, y: Float, w: Float, h: Float) = Path().apply { addRect(Rect(x, y, x + w, y + h)) }

private fun poly(vararg p: Float) = Path().apply {
    moveTo(p[0], p[1])
    var i = 2
    while (i < p.size) { lineTo(p[i], p[i + 1]); i += 2 }
    close()
}

private fun DrawScope.window(open: Boolean, x: Float, y: Float, w: Float, h: Float, arch: Boolean = false) {
    if (!open) return
    if (arch) {
        val p = Path().apply {
            moveTo(x, y + h); lineTo(x, y + w / 2)
            arcTo(Rect(x, y, x + w, y + w), 180f, 180f, false)
            lineTo(x + w, y + h); close()
        }
        drawPath(p, C.lamp)
    } else drawRect(C.lamp, Offset(x, y), Size(w, h))
}

private fun DrawScope.scaffold(open: Boolean, x1: Float, y1: Float, x2: Float, y2: Float) {
    if (open) return
    val c = C.brass.copy(alpha = .45f)
    for (k in 0..3) { val x = x1 + k * (x2 - x1) / 3; drawLine(c, Offset(x, y1), Offset(x, y2), 1f) }
    drawLine(c, Offset(x1, y2), Offset(x2, y1), 1f)
    drawLine(c, Offset(x1, (y1 + y2) / 2), Offset(x2, (y1 + y2) / 2), 1f)
}

private fun DrawScope.halo(on: Boolean, cx: Float, cy: Float, r: Float) {
    if (on) drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(C.lamp.copy(alpha = .55f), Color.Transparent), Offset(cx, cy), r), r, Offset(cx, cy))
}

private fun DrawScope.building(open: Int, glow: Int) {
    fun o(i: Int) = i < open
    drawRect(C.wallD, Offset(0f, 186f), Size(360f, 28f))

    halo(glow == 1, 82f, 140f, 70f)
    halo(glow == 2, 278f, 140f, 70f)
    halo(glow == 3, 180f, 20f, 60f)
    halo(glow == 4, 337f, 110f, 60f)
    halo(glow == 5, 23f, 160f, 45f)

    // 5: the sculpture garden.
    part(o(5), Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(6f, 160f, 40f, 186f, 10f, 10f)) })
    if (o(5)) {
        drawRect(stone, Offset(20f, 132f), Size(6f, 28f))
        drawCircle(stone, 6f, Offset(23f, 128f))
    } else scaffold(false, 6f, 140f, 40f, 186f)

    // 1: the left wing.
    part(o(1), rect(42f, 108f, 80f, 78f))
    part(o(1), poly(38f, 108f, 82f, 92f, 126f, 108f))
    for (x in listOf(54f, 74f, 94f)) { window(o(1), x, 124f, 12f, 22f); window(o(1), x, 154f, 12f, 22f) }
    scaffold(o(1), 42f, 108f, 122f, 186f)

    // 2: the gallery.
    part(o(2), rect(238f, 108f, 80f, 78f))
    part(o(2), poly(234f, 108f, 278f, 92f, 322f, 108f))
    for (x in listOf(250f, 270f, 290f)) window(o(2), x, 124f, 13f, 44f, arch = true)
    scaffold(o(2), 238f, 108f, 318f, 186f)

    // 4: the observatory.
    part(o(4), rect(322f, 70f, 30f, 116f))
    part(o(4), Path().apply { moveTo(318f, 72f); arcTo(Rect(318f, 53f, 356f, 91f), 180f, 180f, false); close() })
    window(o(4), 331f, 90f, 12f, 16f); window(o(4), 331f, 120f, 12f, 16f)
    scaffold(o(4), 322f, 60f, 352f, 186f)

    // 3: the rotunda.
    part(o(3), rect(152f, 28f, 56f, 22f))
    part(o(3), Path().apply { moveTo(146f, 30f); quadraticTo(180f, -10f, 214f, 30f); close() })
    for (x in listOf(160f, 176f, 192f)) window(o(3), x, 34f, 8f, 12f)
    scaffold(o(3), 148f, 4f, 212f, 50f)

    // 0: the main block, always there.
    drawPath(rect(122f, 80f, 116f, 100f), stone)
    drawPath(rect(122f, 80f, 116f, 100f), stoneLine, style = Stroke(1.5f))
    drawPath(poly(112f, 80f, 180f, 44f, 248f, 80f), stone)
    drawPath(poly(112f, 80f, 180f, 44f, 248f, 80f), stoneLine, style = Stroke(1.5f))
    drawRect(Color(0xFFC9B48E), Offset(122f, 80f), Size(116f, 7f))
    for (x in listOf(133f, 155f, 177f, 199f, 221f)) {
        drawRect(column, Offset(x, 88f), Size(8f, 84f))
        drawRect(Color(0xFFA88A5C), Offset(x, 88f), Size(8f, 84f), style = Stroke(.8f))
    }
    val door = Path().apply { moveTo(170f, 180f); lineTo(170f, 150f); arcTo(Rect(170f, 140f, 190f, 160f), 180f, 180f, false); lineTo(190f, 180f); close() }
    drawPath(door, C.lamp)
    drawCircle(Color(0xFF8F6B38), 7f, Offset(180f, 66f), style = Stroke(1.2f))
    drawRect(Color(0xFFCDBB98), Offset(112f, 180f), Size(136f, 6f))
    drawRect(Color(0xFFB9A680), Offset(102f, 186f), Size(156f, 6f))
}
