package app.curator.ui

import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import app.curator.R
import app.curator.data.Item
import app.curator.data.Media
import app.curator.ui.components.IconBtn

/** A video played inside the app, over everything; Back or the cross closes it. */
@Composable
fun VideoPlayer(item: Item, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val view = remember { arrayOfNulls<VideoView>(1) }
    DisposableEffect(item.id) { onDispose { view[0]?.stopPlayback() } }
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(remember { MutableInteractionSource() }, null) {}) {
        AndroidView(
            factory = { ctx ->
                VideoView(ctx).apply {
                    view[0] = this
                    val controls = MediaController(ctx)
                    controls.setAnchorView(this)
                    setMediaController(controls)
                    setOnPreparedListener { it.isLooping = true; start() }
                    setOnErrorListener { _, _, _ -> onClose(); true }
                    setVideoURI(Media.uri(item))
                }
            },
            modifier = Modifier.align(Alignment.Center),
        )
        IconBtn(Icons.Rounded.Close, onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp),
            tint = Color.White, bg = Color(0x66000000), desc = stringResource(R.string.close))
    }
}
