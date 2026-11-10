package app.curator.art

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import app.curator.data.Item
import kotlin.random.Random

/**
 * The demo gallery: stock photos bundled only into demo and render builds (app/src/demo/assets/demo/<kind>/), picked
 * by the item's seed. Blurred and dark shots are real photos spoiled on the spot; a screenshot is drawn. No photo
 * from the phone is ever read for it.
 */
object Fake {
    private const val W = 300f
    private const val H = 400f
    private val cache = object : android.util.LruCache<String, Bitmap>(24 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
    private val lists = HashMap<String, List<String>>()

    fun draw(it: Item, px: Int, assets: AssetManager): Bitmap {
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
        val r = Random(if (blur) it.seed else baseSeed(it.seed))
        val kind = if (blur) listOf("land", "face", "city", "dog")[r.nextInt(4)] else it.art
        // The jitter makes a series: the same scene, a hand's width to the side.
        val j = if (it.seed >= 1_000_000) it.seed % 10 else 0
        if (kind == "shot") {
            c.scale(sw / W, sh / H)
            screenshot(c, r)
        } else if (!photo(c, assets, kind, r, j)) {
            c.drawColor(0xFF8A8A8A.toInt())
        }
        val out = if (blur) Bitmap.createScaledBitmap(b, bw, bh, true) else b
        cache.put(key, out)
        return out
    }

    /** Series share a base seed (the seed with its last decimal digit dropped), so their scenes match. */
    private fun baseSeed(seed: Int) = if (seed >= 1_000_000) seed / 10 else seed

    /** A stock photo of [kind] cropped to fill [c]; a series frame [j] is zoomed in a little and moved along. */
    private fun photo(c: Canvas, assets: AssetManager, kind: String, r: Random, j: Int): Boolean {
        val files = synchronized(lists) {
            lists.getOrPut(kind) { runCatching { assets.list("demo/$kind")?.sorted() }.getOrNull().orEmpty() }
        }
        if (files.isEmpty()) return false
        val path = "demo/$kind/${files[r.nextInt(files.size)]}"
        val src = runCatching {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            assets.open(path).use { BitmapFactory.decodeStream(it, null, o) }
            var k = 1
            while (o.outWidth / (k * 2) >= c.width) k *= 2
            assets.open(path).use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = k }) }
        }.getOrNull() ?: return false
        val aim = c.width.toFloat() / c.height
        var cw = src.width.toFloat()
        var ch = cw / aim
        if (ch > src.height) { ch = src.height.toFloat(); cw = ch * aim }
        if (j > 0) { cw *= .9f; ch *= .9f }
        val x = ((src.width - cw) / 2 + (j - 1.5f) * cw * .025f).coerceIn(0f, src.width - cw)
        val y = (src.height - ch) / 2
        c.drawBitmap(src, Rect(x.toInt(), y.toInt(), (x + cw).toInt(), (y + ch).toInt()),
            RectF(0f, 0f, c.width.toFloat(), c.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        return true
    }

    private fun p(color: Long) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color.toInt() }

    /** A chat screenshot: status bar, a coloured app bar and speech bubbles. */
    private fun screenshot(c: Canvas, r: Random) {
        c.drawColor(0xFFFFFFFF.toInt())
        c.drawRect(0f, 0f, W, 22f, p(0xFFF2F2F2))
        c.drawRect(10f, 7f, 40f, 15f, p(0xFF999999))
        c.drawRect(250f, 7f, 290f, 15f, p(0xFF999999))
        c.drawRect(0f, 22f, W, 72f, p(listOf(0xFF2F80ED, 0xFF27AE60, 0xFF9B51E0)[r.nextInt(3)]))
        c.drawRect(50f, 40f, 170f, 52f, p(0x96FFFFFF))
        var y = 88f
        repeat(8) {
            val w = 90 + r.nextFloat() * 130
            val me = r.nextBoolean()
            val x = if (me) W - w - 12 else 12f
            c.drawRoundRect(RectF(x, y, x + w, y + 30), 12f, 12f, p(if (me) 0xFFDCF8C6 else 0xFFECECF0))
            y += 38
        }
    }
}
