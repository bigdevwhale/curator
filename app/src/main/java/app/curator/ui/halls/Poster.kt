package app.curator.ui.halls

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import app.curator.R
import app.curator.container
import app.curator.data.Item
import java.io.File

/** The week's exhibition as a poster picture, sent wherever the person likes. */
fun sharePoster(ctx: Context, title: String, xs: List<Item>) {
    runCatching {
        val b = poster(ctx, title, xs)
        val f = File(File(ctx.cacheDir, "shots").apply { mkdirs() }, "exhibition.png")
        f.outputStream().use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".shots", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("image/png").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

fun poster(ctx: Context, title: String, xs: List<Item>): Bitmap {
    val w = 1080
    val h = 1350
    val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val c = Canvas(b)
    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0xFF2A1F1A.toInt(), 0xFF151010.toInt(), Shader.TileMode.CLAMP) })
    val narrow = ResourcesCompat.getFont(ctx, R.font.pt_sans_narrow_bold)
    val italic = ResourcesCompat.getFont(ctx, R.font.cormorant_italic)
    val caps = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = narrow; textSize = 40f; color = 0xFFC49A5C.toInt(); textAlign = Paint.Align.CENTER; letterSpacing = .25f }
    c.drawText(ctx.getString(R.string.show_caps).uppercase(), w / 2f, 150f, caps)
    val t = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = italic; textSize = 104f; color = 0xFFEFE6D2.toInt(); textAlign = Paint.Align.CENTER }
    while (t.measureText(title) > w - 120 && t.textSize > 50) t.textSize -= 4
    c.drawText(title, w / 2f, 290f, t)
    val n = xs.size.coerceIn(1, 3)
    val fw = if (n == 1) 560f else if (n == 2) 400f else 290f
    val fh = fw * 1.25f
    val gap = 40f
    var x = (w - n * fw - (n - 1) * gap) / 2
    val y = 400f + (640f - fh) / 2
    val gilt = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB08B4F.toInt() }
    val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF6B5228.toInt(); style = Paint.Style.STROKE; strokeWidth = 5f }
    for (it in xs.take(3)) {
        c.drawRect(x, y, x + fw, y + fh, gilt)
        val inner = RectF(x + 18, y + 18, x + fw - 18, y + fh - 18)
        c.drawRect(inner, edge)
        ctx.container.picture(it, 600)?.let { pic ->
            // Centre-crop the picture into the frame.
            val k = maxOf(inner.width() / pic.width, inner.height() / pic.height)
            val sw = inner.width() / k
            val sh = inner.height() / k
            val src = android.graphics.Rect(((pic.width - sw) / 2).toInt(), ((pic.height - sh) / 2).toInt(), ((pic.width + sw) / 2).toInt(), ((pic.height + sh) / 2).toInt())
            c.drawBitmap(pic, src, RectF(inner.left + 3, inner.top + 3, inner.right - 3, inner.bottom - 3), Paint(Paint.FILTER_BITMAP_FLAG))
        }
        x += fw + gap
    }
    val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = narrow; textSize = 34f; color = 0xFFC9BB9A.toInt(); textAlign = Paint.Align.CENTER; letterSpacing = .2f }
    c.drawText(ctx.getString(R.string.app_name).uppercase() + " · " + ctx.getString(R.string.museum_of).uppercase(), w / 2f, h - 110f, foot)
    return b
}
