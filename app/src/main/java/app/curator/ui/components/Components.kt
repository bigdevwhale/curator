package app.curator.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.curator.container
import app.curator.data.Item
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/** The striped green wallpaper of the museum rooms. */
fun Modifier.wallpaper() = this.background(C.wall).drawBehind {
    val step = 24.dp.toPx()
    var x = step - 2.dp.toPx()
    while (x < size.width) {
        drawRect(C.wall2, Offset(x, 0f), androidx.compose.ui.geometry.Size(2.dp.toPx(), size.height))
        x += step
    }
}

object Fmt {
    fun bytes(b: Long, ru: Boolean): String {
        val mb = b / 1048576.0
        val nb = ' '
        return if (mb >= 1024) String.format(if (ru) Locale.forLanguageTag("ru") else Locale.ENGLISH, "%.1f", mb / 1024) + nb + (if (ru) "ГБ" else "GB")
        else if (mb >= 1) "%d".format(mb.toInt()) + nb + (if (ru) "МБ" else "MB")
        else "%d".format((b / 1024).coerceAtLeast(1)) + nb + (if (ru) "КБ" else "KB")
    }

    fun n(x: Int): String = "%,d".format(Locale.ENGLISH, x).replace(',', ' ')

    fun duration(ms: Long): String { val s = ms / 1000; return "%d:%02d".format(s / 60, s % 60) }
}

private object Pictures {
    val cache = object : LruCache<String, Bitmap>(48 shl 20) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }
}

/** A photo from the gallery (or the demo), loaded off the main thread and kept in a small cache. */
@Composable
fun Thumb(item: Item, modifier: Modifier = Modifier, px: Int = 256, fit: Boolean = false, badge: Boolean = true) {
    val ctx = LocalContext.current
    val key = "${item.id}:$px"
    val bmp by produceState<ImageBitmap?>(Pictures.cache.get(key)?.asImageBitmap(), key) {
        if (value == null) value = withContext(Dispatchers.IO) {
            ctx.container.picture(item, px)?.also { Pictures.cache.put(key, it) }?.asImageBitmap()
        }
    }
    Box(modifier.background(C.photoBg)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = if (fit) ContentScale.Fit else ContentScale.Crop) }
        if (badge && item.video) Text("▶ " + Fmt.duration(item.duration), style = T.Label.copy(color = Color.White, fontSize = 11.sp),
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(Color(0xAA000000), RoundedCornerShape(2.dp)).padding(horizontal = 4.dp))
    }
}

/** A sheet of inventory paper. */
@Composable
fun Paper(modifier: Modifier = Modifier, pad: Dp = 16.dp, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.shadow(10.dp, RoundedCornerShape(3.dp)).background(C.paper, RoundedCornerShape(3.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

enum class Kind { RED, BRASS, GREEN, GHOST, WALL }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.BRASS, enabled: Boolean = true) {
    val shape = RoundedCornerShape(3.dp)
    val (bg, fg) = when (kind) {
        Kind.RED -> Brush.verticalGradient(listOf(C.red, C.red)) to Color.White
        Kind.GREEN -> Brush.verticalGradient(listOf(C.green, C.green)) to Color.White
        Kind.BRASS -> Brush.verticalGradient(listOf(C.brassL, Color(0xFFA9814A))) to Color(0xFF2B1F10)
        Kind.GHOST -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)) to C.muted
        Kind.WALL -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent)) to C.brass
    }
    Box(
        modifier.clip(shape).background(bg).then(
            when (kind) {
                Kind.GHOST -> Modifier.border(1.dp, C.paperLine, shape)
                Kind.WALL -> Modifier.border(1.dp, C.brass.copy(alpha = .5f), shape)
                else -> Modifier
            }
        ).then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.background(Color(0x55000000)))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text.uppercase(), style = T.Button.copy(color = if (enabled) fg else fg.copy(alpha = .5f)), textAlign = TextAlign.Center) }
}

/** A small-caps heading on the wall. */
@Composable
fun Caps(text: String, modifier: Modifier = Modifier, right: String? = null) {
    Row(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 8.dp)) {
        Text(text.uppercase(), style = T.Caps, modifier = Modifier.weight(1f))
        if (right != null) Text(right.uppercase(), style = T.Caps.copy(color = C.onWall.copy(alpha = .7f)))
    }
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = C.gold, track: Color = Color(0x55000000)) {
    Box(modifier.height(6.dp).fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(6.dp).background(color))
    }
}

/** An ink stamp, a little crooked. */
@Composable
fun Stamp(text: String, color: Color, modifier: Modifier = Modifier, style: TextStyle = T.Stamp) {
    Box(
        modifier.rotate(-10f).background(C.paper.copy(alpha = .8f)).border(4.dp, color).padding(3.dp).border(1.5.dp, color)
            .padding(horizontal = 14.dp, vertical = 2.dp)
    ) { Text(text.uppercase(), style = style.copy(color = color)) }
}

/** The title row of a page laid over the wall. */
@Composable
fun TopBar(title: String, sub: String? = null, onBack: (() -> Unit)? = null, right: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Box(Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("‹", style = T.Title.copy(color = C.brass, fontSize = T.Title.fontSize * 1.2f))
        } else Box(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = T.Heading.copy(color = C.paper), maxLines = 1)
            if (sub != null) Text(sub, style = T.Label.copy(color = C.onWall), maxLines = 1)
        }
        right?.invoke()
    }
}

/** A dimmed layer over everything; tapping outside or Back closes it when [onDismiss] is given. */
@Composable
fun Scrim(onDismiss: (() -> Unit)?, bottom: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    if (onDismiss != null) BackHandler(onBack = onDismiss)
    Box(
        Modifier.fillMaxSize().background(C.scrim).clickable(remember { MutableInteractionSource() }, null) { onDismiss?.invoke() }
            .padding(if (bottom) 0.dp else 18.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 520.dp).clickable(remember { MutableInteractionSource() }, null) {}) { content() }
    }
}

/** A brass plate with engraved numbers. */
@Composable
fun Plaque(modifier: Modifier = Modifier, cells: List<Pair<String, String>>) {
    Row(
        modifier.shadow(6.dp, RoundedCornerShape(4.dp)).background(Brush.verticalGradient(listOf(C.brassL, Color(0xFFA9814A))), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0x66F5D9A0), RoundedCornerShape(4.dp)).padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        for ((big, small) in cells) Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(big, style = T.Number.copy(color = Color(0xFF2B1F10)), maxLines = 1)
            Text(small.uppercase(), style = T.Caps.copy(color = Color(0xFF3A2A14), letterSpacing = T.Caps.letterSpacing * .4f), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun Dot(color: Color, modifier: Modifier = Modifier, d: Dp = 8.dp) = Canvas(modifier.size(d)) { drawCircle(color) }

@Composable
fun Spacer(h: Dp) = Box(Modifier.height(h))
