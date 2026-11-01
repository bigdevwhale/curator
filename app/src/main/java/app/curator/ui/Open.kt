package app.curator.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.curator.data.Item
import app.curator.data.Media

/** The video playing on top of everything, if any (see [VideoPlayer]). */
object Player {
    var item by mutableStateOf<Item?>(null)
}

/** Plays a video right here; shows a photo in the phone's own viewer. Demo things have nothing to open. */
fun openInGallery(ctx: Context, it: Item): Boolean {
    if (it.fake) return false
    if (it.video) { Player.item = it; return true }
    return runCatching {
        ctx.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Media.uri(it), "image/*")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }.isSuccess
}

fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
