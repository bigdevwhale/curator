package app.curator.art

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import app.curator.data.Item
import kotlin.random.Random

/**
 * The demo gallery: little made-up photographs drawn from a seed (landscapes, the sea, a city at dusk, a cat,
 * a plate, a portrait, a screenshot, a receipt, a dark pocket shot, a blurred one). No real photo is ever read for it.
 */
object Fake {
    private const val W = 300f
    private const val H = 400f
    private val cache = object : android.util.LruCache<String, Bitmap>(24 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun draw(it: Item, px: Int): Bitmap {
        val key = "${it.seed}:${it.art}:${it.w}:$px"
        cache.get(key)?.let { b -> return b }
        val wide = it.w > it.h
        val bw = px.coerceAtLeast(16)
        val bh = if (wide) bw * 3 / 4 else bw * 4 / 3
        val blur = it.art == "blur"
        // A blurred photo is drawn small and blown up.
        val sw = if (blur) (bw / 14).coerceAtLeast(6) else bw
        val sh = if (blur) (bh / 14).coerceAtLeast(8) else bh
        val b = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.scale(sw / W, sh / H)
        val r = Random(it.seed)
        val kind = if (blur) listOf("land", "face", "city", "cat")[r.nextInt(4)] else it.art
        // The jitter makes a series: the same scene, a hand's width to the side.
        val j = if (it.seed >= 1_000_000) (it.seed % 10).toFloat() else 0f
        scene(c, kind, Random(if (blur) it.seed else baseSeed(it.seed)), j * 7f)
        val out = if (blur) Bitmap.createScaledBitmap(b, bw, bh, true) else b
        cache.put(key, out)
        return out
    }

    /** Series share a base seed (the seed with its last decimal digit dropped), so their scenes match. */
    private fun baseSeed(seed: Int) = if (seed >= 1_000_000) seed / 10 else seed

    private fun p(color: Long, alpha: Int = 255) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt(); this.alpha = alpha }

    private fun <T> Random.pick(xs: List<T>) = xs[nextInt(xs.size)]

    private fun sky(c: Canvas, top: Long, bottom: Long, h: Float = H) {
        val paint = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h, top.toInt(), bottom.toInt(), Shader.TileMode.CLAMP) }
        c.drawRect(0f, 0f, W, H, paint)
    }

    private fun scene(c: Canvas, kind: String, r: Random, dx: Float) {
        when (kind) {
            "land" -> {
                val sk = r.pick(listOf(0xFFF4A261 to 0xFFFDE2B8, 0xFF6FA8DC to 0xFFD9ECF7, 0xFFB48EAD to 0xFFF6D5C0, 0xFF88B4A8 to 0xFFEEF3E2))
                sky(c, sk.first, sk.second)
                c.drawCircle(70 + r.nextFloat() * 160 + dx, 90 + r.nextFloat() * 60, 18 + r.nextFloat() * 14, p(0xFFFFF6DD, 230))
                val cols = listOf(0xFF6F8F99, 0xFF47675F, 0xFF2F4A3C)
                for (k in 0 until 3) {
                    val base = 190f + k * 55
                    val path = Path().apply {
                        moveTo(-10f, H)
                        var x = -10f
                        while (x <= 320f) { lineTo(x + dx, base - r.nextFloat() * 70); x += 40f }
                        lineTo(320f, H); close()
                    }
                    c.drawPath(path, p(cols[k]))
                }
                c.drawRect(0f, 340f, W, H, p(0xFF3C5A3A))
            }
            "sea" -> {
                sky(c, r.pick(listOf(0xFF9EC5E8, 0xFFF7B267, 0xFFC8B6E2)), 0xFFFBEEDD, 230f)
                val sx = 90 + r.nextFloat() * 120 + dx
                c.drawCircle(sx, 170f, 26f, p(0xFFFFF4D6))
                c.drawRect(0f, 230f, W, H, Paint().apply { shader = LinearGradient(0f, 230f, 0f, H, 0xFF3D7EA6.toInt(), 0xFF173B57.toInt(), Shader.TileMode.CLAMP) })
                for (k in 0 until 7) c.drawRect(sx - 30 + r.nextFloat() * 40, 240f + k * 16, sx - 30 + r.nextFloat() * 40 + 20 + r.nextFloat() * 30, 243f + k * 16, p(0xFFFFF4D6, 150))
                val bx = 60 + r.nextFloat() * 160 + dx
                c.drawPath(Path().apply { moveTo(bx, 228f); lineTo(bx + 20, 180f); lineTo(bx + 22, 228f); close() }, p(0xFFFFFFFF))
                c.drawRect(bx - 8, 227f, bx + 30, 233f, p(0xFF222222))
            }
            "city" -> {
                sky(c, 0xFF2B2D5C, r.pick(listOf(0xFFE48A6A, 0xFFD9737C, 0xFFF0A35E)))
                var x = -5f + dx
                val win = p(0xFFF7D37A)
                while (x < W) {
                    val w = 28 + r.nextFloat() * 42
                    val h = 120 + r.nextFloat() * 190
                    c.drawRect(x, H - h, x + w, H, p(0xFF1D1F33))
                    var wy = H + 10 - h
                    while (wy < 390) {
                        var wx = x + 5
                        while (wx < x + w - 6) { if (r.nextFloat() < .35f) c.drawRect(wx, wy, wx + 5, wy + 7, win); wx += 10 }
                        wy += 14
                    }
                    x += w + 3
                }
            }
            "cat" -> {
                c.drawColor(r.pick(listOf(0xFFD9C3A5, 0xFFA7B8C9, 0xFFC9A9A6)).toInt())
                val col = p(r.pick(listOf(0xFFE09A4F, 0xFF666666, 0xFFEEEEEE, 0xFF2B2B2B)))
                val cx = 150 + dx
                c.drawOval(RectF(10f, 296f, 290f, 384f), p(0xFF000000, 34))
                c.drawOval(RectF(cx - 92, 223f, cx + 92, 347f), col)
                c.drawCircle(cx, 195f, 56f, col)
                c.drawPath(Path().apply { moveTo(cx - 50, 170f); lineTo(cx - 38, 118f); lineTo(cx - 12, 150f); close() }, col)
                c.drawPath(Path().apply { moveTo(cx + 50, 170f); lineTo(cx + 38, 118f); lineTo(cx + 12, 150f); close() }, col)
                for (s in listOf(-20f, 20f)) {
                    c.drawOval(RectF(cx + s - 9, 179f, cx + s + 9, 201f), p(0xFFA7C94A))
                    c.drawOval(RectF(cx + s - 3, 181f, cx + s + 3, 199f), p(0xFF111111))
                }
                c.drawPath(Path().apply { moveTo(cx - 6, 212f); lineTo(cx + 6, 212f); lineTo(cx, 219f); close() }, p(0xFFDD7777))
            }
            "food" -> {
                c.drawColor(0xFF8B5A3C.toInt())
                for (k in 0 until 6) c.drawRect(0f, k * 70f, W, k * 70f + 2, p(0xFF6D432B))
                c.drawCircle(150 + dx, 210f, 112f, p(0xFFE9E4D8))
                c.drawCircle(150 + dx, 210f, 84f, p(0xFFFBFAF6))
                for (k in 0 until 6) c.drawCircle(120 + r.nextFloat() * 60 + dx, 180 + r.nextFloat() * 60, 14 + r.nextFloat() * 20,
                    p(r.pick(listOf(0xFFD9534F, 0xFFF0AD4E, 0xFF5CB85C, 0xFFE8C07D, 0xFFA0522D))))
                c.drawCircle(262f, 56f, 34f, p(0xFFF4F1EA))
                c.drawCircle(262f, 56f, 24f, p(0xFF5A3825))
            }
            "face" -> {
                c.drawColor(r.pick(listOf(0xFF9FB7C9, 0xFFC7B7A3, 0xFF7D9B86, 0xFFE0C9A6)).toInt())
                val cx = 150 + dx
                c.drawOval(RectF(cx - 135, 330f, cx + 135, 530f), p(r.pick(listOf(0xFF36454F, 0xFF8B3A3A, 0xFF2E5E4E, 0xFFC9A227))))
                c.drawRect(cx - 22, 250f, cx + 22, 310f, p(0xFFD9A07A))
                c.drawOval(RectF(cx - 62, 122f, cx + 62, 278f), p(r.pick(listOf(0xFFF1C7A5, 0xFFE2B08A, 0xFFC48B62))))
                c.drawPath(Path().apply {
                    moveTo(cx - 66, 200f); quadTo(cx, 80f, cx + 66, 200f); quadTo(cx + 40, 140f, cx, 145f); quadTo(cx - 40, 140f, cx - 66, 200f); close()
                }, p(r.pick(listOf(0xFF3B2A20, 0xFF8A5A2B, 0xFFD8B26E, 0xFF222222))))
                c.drawCircle(cx - 22, 200f, 5f, p(0xFF222222))
                c.drawCircle(cx + 22, 200f, 5f, p(0xFF222222))
                c.drawPath(Path().apply { moveTo(cx - 22, 238f); quadTo(cx, 256f, cx + 22, 238f) },
                    p(0xFFA4554A).apply { style = Paint.Style.STROKE; strokeWidth = 4f })
            }
            "shot" -> {
                c.drawColor(0xFFFFFFFF.toInt())
                c.drawRect(0f, 0f, W, 22f, p(0xFFF2F2F2))
                c.drawRect(10f, 7f, 40f, 15f, p(0xFF999999))
                c.drawRect(250f, 7f, 290f, 15f, p(0xFF999999))
                c.drawRect(0f, 22f, W, 72f, p(r.pick(listOf(0xFF2F80ED, 0xFF27AE60, 0xFF9B51E0))))
                c.drawRect(50f, 40f, 170f, 52f, p(0xFFFFFFFF, 150))
                var y = 88f
                repeat(8) {
                    val w = 90 + r.nextFloat() * 130
                    val me = r.nextBoolean()
                    val x = if (me) W - w - 12 else 12f
                    c.drawRoundRect(RectF(x, y, x + w, y + 30), 12f, 12f, p(if (me) 0xFFDCF8C6 else 0xFFECECF0))
                    y += 38
                }
            }
            "doc" -> {
                c.drawColor(0xFF6D5E50.toInt())
                c.save()
                c.rotate(-6 + r.nextFloat() * 12, 150f, 200f)
                c.drawRect(55f, 40f, 245f, 360f, p(0xFFF8F6F0))
                for (k in 0 until 16) c.drawRect(70f, 64f + k * 17, 70 + 60 + r.nextFloat() * 100, 69f + k * 17, p(0xFFBBBBBB))
                c.restore()
            }
            else -> {
                c.drawColor(0xFF09090B.toInt())
                val x = 100 + r.nextFloat() * 100
                val y = 150 + r.nextFloat() * 100
                c.drawOval(RectF(x - 90, y - 70, x + 90, y + 70), Paint().apply {
                    shader = RadialGradient(x, y, 90f, 0xFF2C2A30.toInt(), 0xFF09090B.toInt(), Shader.TileMode.CLAMP)
                })
                repeat(4) { c.drawCircle(r.nextFloat() * W, r.nextFloat() * H, 2f, p(0xFFF6D27A, 180)) }
            }
        }
    }
}
