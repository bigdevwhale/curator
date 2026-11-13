package app.curator.data

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId

/** How much of the gallery the museum may see. */
enum class Access { NONE, PARTIAL, FULL }

/** The phone's gallery through MediaStore: reading it, asking to delete or move things, and thumbnails. */
object Media {
    fun access(ctx: Context): Access {
        fun ok(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
        return when {
            Build.VERSION.SDK_INT >= 33 && ok(Manifest.permission.READ_MEDIA_IMAGES) -> Access.FULL
            Build.VERSION.SDK_INT >= 34 && ok(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) -> Access.PARTIAL
            Build.VERSION.SDK_INT < 33 && ok(Manifest.permission.READ_EXTERNAL_STORAGE) -> Access.FULL
            else -> Access.NONE
        }
    }

    fun permissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        Build.VERSION.SDK_INT >= 33 -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    fun uri(it: Item): Uri = ContentUris.withAppendedId(
        if (it.video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, it.id)

    /** Every photo and video the museum can see. */
    fun scan(ctx: Context): List<Item> {
        val zone = ZoneId.systemDefault()
        val out = ArrayList<Item>()
        val proj = arrayOf(
            MediaStore.Files.FileColumns._ID, MediaStore.Files.FileColumns.MEDIA_TYPE, MediaStore.MediaColumns.DATE_TAKEN,
            MediaStore.MediaColumns.DATE_ADDED, MediaStore.MediaColumns.DATE_MODIFIED, MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT, MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.RELATIVE_PATH, MediaStore.MediaColumns.DURATION,
        )
        val sel = "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}, ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})"
        ctx.contentResolver.query(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), proj, sel, null, null)?.use { c ->
            while (c.moveToNext()) {
                val taken = c.getLong(2).takeIf { it > 0 } ?: (c.getLong(3).takeIf { it > 0 } ?: c.getLong(4)) * 1000
                val d = Instant.ofEpochMilli(taken).atZone(zone).toLocalDate()
                out += Item(
                    id = c.getLong(0),
                    video = c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                    taken = taken,
                    month = d.year * 100 + d.monthValue,
                    md = d.monthValue * 100 + d.dayOfMonth,
                    // Some files come without a size in the media store; ask the file itself then (#2).
                    size = c.getLong(5).takeIf { it > 0 } ?: sizeOf(ctx, c.getLong(0), c.getInt(1) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                    w = c.getInt(6),
                    h = c.getInt(7),
                    bucket = c.getString(8) ?: "",
                    path = c.getString(9) ?: "",
                    duration = if (c.isNull(10)) 0 else c.getLong(10),
                )
            }
        }
        return out
    }

    private fun sizeOf(ctx: Context, id: Long, video: Boolean): Long = runCatching {
        val u = ContentUris.withAppendedId(if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        ctx.contentResolver.openFileDescriptor(u, "r")?.use { it.statSize }
    }.getOrNull()?.coerceAtLeast(0) ?: 0

    /** True while the gallery still has this photo or video. */
    fun exists(ctx: Context, id: Long, video: Boolean): Boolean = runCatching {
        val u = ContentUris.withAppendedId(if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        ctx.contentResolver.query(u, arrayOf(MediaStore.MediaColumns._ID), null, null, null)?.use { it.count > 0 } ?: false
    }.getOrDefault(true)

    /** True when a picture of [iw]×[ih] fills a [w]×[h] box without being stretched: covering it, or (if [fit]) fitted into it. */
    fun covers(iw: Int, ih: Int, w: Int, h: Int, fit: Boolean) = if (fit) iw >= w || ih >= h else iw >= w && ih >= h

    /**
     * A picture big enough for a [w]×[h] box ([fit] or cropped). Small ones and videos come from the system
     * thumbnail; bigger photos are decoded from the original, scaled down by a power of two, because the system
     * thumbnail stops at about 512 px and a full card looked smeared (#3).
     */
    fun thumb(ctx: Context, it: Item, w: Int, h: Int = w, fit: Boolean = false): Bitmap? {
        val system = { runCatching { ctx.contentResolver.loadThumbnail(uri(it), Size(maxOf(w, h), maxOf(w, h)), null) }.getOrNull() }
        if (it.video || maxOf(w, h) <= 256) return system()
        return runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri(it))) { d, info, _ ->
                val iw = info.size.width; val ih = info.size.height
                var k = 1
                while (covers(iw / (k * 2), ih / (k * 2), w, h, fit) || maxOf(iw, ih) / k > 4096) k *= 2
                d.setTargetSampleSize(k)
                d.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull() ?: system()
    }

    /** One system dialog to delete them all. */
    fun deleteRequest(ctx: Context, xs: List<Item>): IntentSender =
        MediaStore.createDeleteRequest(ctx.contentResolver, xs.map(::uri)).intentSender

    /** One system dialog to let the museum move them all. */
    fun writeRequest(ctx: Context, xs: List<Item>): IntentSender =
        MediaStore.createWriteRequest(ctx.contentResolver, xs.map(::uri)).intentSender

    /** Puts things into Pictures/<folder>/ so every gallery app shows the hall as an album. Returns how many moved. */
    fun move(ctx: Context, xs: List<Item>, folder: String): Int {
        val path = "Pictures/$folder/"
        var n = 0
        for (x in xs) {
            if (x.path == path) { n++; continue }
            val ok = runCatching {
                ctx.contentResolver.update(uri(x), ContentValues().apply { put(MediaStore.MediaColumns.RELATIVE_PATH, path) }, null, null) > 0
            }.getOrDefault(false)
            if (ok) n++
        }
        return n
    }

    /** A folder name that MediaStore accepts. */
    fun folderName(name: String) = name.trim().replace(Regex("[\\\\/:*?\"<>|]"), " ").replace(Regex("\\s+"), " ").take(60).ifBlank { "Curator" }
}
