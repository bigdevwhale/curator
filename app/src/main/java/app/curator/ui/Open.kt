package app.curator.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import app.curator.data.Item
import app.curator.data.Media

/** Shows the thing in the phone's own viewer (a video plays there). Demo things have nothing to open. */
fun openInGallery(ctx: Context, it: Item): Boolean {
    if (it.fake) return false
    return runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Media.uri(it), if (it.video) "video/*" else "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
