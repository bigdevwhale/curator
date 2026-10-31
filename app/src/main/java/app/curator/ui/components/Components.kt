package app.curator.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/** The room itself: the wall colour of the current theme. */
fun Modifier.room() = this.background(C.bg)

val Round = RoundedCornerShape(20.dp)
val RoundS = RoundedCornerShape(12.dp)

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
fun Thumb(item: Item, modifier: Modifier = Modifier, px: Int = 256, fit: Boolean = false, badge: Boolean = true, shape: Shape? = null) {
    val ctx = LocalContext.current
    val key = "${item.id}:$px"
    val bmp by produceState<ImageBitmap?>(Pictures.cache.get(key)?.asImageBitmap(), key) {
        // The same slot can show another thing next (the next card): never keep the old picture.
        val cached = Pictures.cache.get(key)?.asImageBitmap()
        value = cached
        if (cached == null) value = withContext(Dispatchers.IO) {
            ctx.container.picture(item, px)?.also { Pictures.cache.put(key, it) }?.asImageBitmap()
        }
    }
    Box(modifier.then(if (shape != null) Modifier.clip(shape) else Modifier).background(C.photoBg)) {
        bmp?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = if (fit) ContentScale.Fit else ContentScale.Crop) }
        if (badge && item.video) Text("▶ " + Fmt.duration(item.duration), style = T.Label.copy(color = Color.White, fontSize = 11.sp),
            modifier = Modifier.align(Alignment.BottomStart).padding(6.dp).background(Color(0x99000000), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
    }
}

/** A white card on the wall (a dark one at night). */
@Composable
fun Card(modifier: Modifier = Modifier, pad: Dp = 18.dp, color: Color = C.surface, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(Round).background(color).then(if (C.dark) Modifier.border(1.dp, C.line, Round) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(pad),
        content = content,
    )
}

enum class Kind { PRIMARY, ACCENT, RED, GREEN, GHOST }

@Composable
fun Btn(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, kind: Kind = Kind.PRIMARY, enabled: Boolean = true, icon: ImageVector? = null) {
    val shape = RoundedCornerShape(50)
    val (bg, fg) = when (kind) {
        Kind.PRIMARY -> C.ink to C.bg
        Kind.ACCENT -> C.accent to C.onAccent
        Kind.RED -> C.red to Color.White
        Kind.GREEN -> C.green to Color.White
        Kind.GHOST -> Color.Transparent to C.ink
    }
    Row(
        modifier.clip(shape).background(if (enabled) bg else bg.copy(alpha = if (kind == Kind.GHOST) 0f else .35f))
            .then(if (kind == Kind.GHOST) Modifier.border(1.dp, C.line, shape) else Modifier)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val col = if (enabled) fg else fg.copy(alpha = .55f)
        if (icon != null) Icon(icon, null, tint = col, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        Text(text, style = T.Button.copy(color = col), textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A round icon button. */
@Composable
fun IconBtn(icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier, tint: Color = C.ink, bg: Color = Color.Transparent, size: Dp = 44.dp, desc: String? = null) {
    Box(modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = tint, modifier = Modifier.size(size * .5f))
    }
}

/** A small-caps heading. */
@Composable
fun Caps(text: String, modifier: Modifier = Modifier, right: String? = null, onRight: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 10.dp), verticalAlignment = Alignment.Bottom) {
        Text(text.uppercase(), style = T.Caps, modifier = Modifier.weight(1f))
        if (right != null) Text(right, style = T.Label.copy(color = if (onRight != null) C.accent else C.muted),
            modifier = if (onRight != null) Modifier.clickable(onClick = onRight) else Modifier)
    }
}

@Composable
fun Bar(progress: Float, modifier: Modifier = Modifier, color: Color = C.accent, track: Color = C.surface2, h: Dp = 4.dp) {
    Box(modifier.height(h).fillMaxWidth().clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(h).clip(RoundedCornerShape(50)).background(color))
    }
}

/** An ink stamp, a little crooked. */
@Composable
fun Stamp(text: String, color: Color, modifier: Modifier = Modifier, style: TextStyle = T.Stamp) {
    Box(
        modifier.rotate(-10f).background(Color.White.copy(alpha = .85f), RoundedCornerShape(6.dp)).border(3.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 16.dp, vertical = 2.dp)
    ) { Text(text.uppercase(), style = style.copy(color = color)) }
}

/** The title row of a page: back arrow, title and subtitle, room for actions on the right. */
@Composable
fun TopBar(title: String, sub: String? = null, onBack: (() -> Unit)? = null, right: @Composable (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 6.dp, end = 10.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconBtn(Icons.AutoMirrored.Rounded.ArrowBack, onBack) else Box(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(start = 4.dp)) {
            Text(title, style = T.Heading, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (sub != null) Text(sub, style = T.Label, maxLines = 1)
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
            .padding(if (bottom) 0.dp else 16.dp),
        contentAlignment = if (bottom) Alignment.BottomCenter else Alignment.Center,
    ) {
        Box(Modifier.widthIn(max = 520.dp).clickable(remember { MutableInteractionSource() }, null) {}) { content() }
    }
}

/** Big numbers in a row, divided by hairlines. */
@Composable
fun Stats(cells: List<Pair<String, String>>, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        cells.forEachIndexed { i, (big, small) ->
            if (i > 0) Box(Modifier.width(1.dp).height(36.dp).background(C.line))
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(big, style = T.Number, maxLines = 1)
                Text(small, style = T.Label, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Text(text, style = T.Label.copy(color = if (on) C.bg else C.ink),
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) C.ink else Color.Transparent).border(1.dp, if (on) C.ink else C.line, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp))
}

/** A bottom sheet on the room's surface colour. */
@Composable
fun Sheet(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(C.surface)
            .then(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp)),
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 12.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(50)).background(C.line))
        content()
    }
}

@Composable
fun Gap(h: Dp) = Box(Modifier.height(h))

fun Modifier.soft(): Modifier = this.shadow(14.dp, Round, ambientColor = Color(0x33000000), spotColor = Color(0x33000000))
