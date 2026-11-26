package app.curator.ui.crate

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.data.Item
import app.curator.domain.Dates
import app.curator.ru
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.IconBtn
import app.curator.ui.components.Kind
import app.curator.ui.components.Thumb
import app.curator.ui.theme.T
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter

private const val MAX_ZOOM = 5f
private const val TAP_ZOOM = 2.5f
private val SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss")

/**
 * The shots of a series one by one, over the whole screen, to tell near-identical frames apart (#7):
 * swipe between them, pinch or double-tap to look closer, and mark the ones to keep from here (#8).
 */
@Composable
fun Lens(card: List<Item>, start: Int, picks: Set<Long>, onPick: (Item) -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    val ru = LocalContext.current.ru
    val pager = rememberPagerState(start.coerceIn(0, card.lastIndex)) { card.size }
    var zoomed by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().background(Color.Black).clickable(remember { MutableInteractionSource() }, null) {}) {
        // Zoomed in, a swipe moves the picture, not to the next shot.
        HorizontalPager(pager, Modifier.fillMaxSize(), userScrollEnabled = !zoomed, key = { card[it].id }) { i ->
            val active = pager.settledPage == i
            ZoomPage(card[i], active) { if (active) zoomed = it }
        }
        val x = card[pager.currentPage]
        val on = x.id in picks
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0x99000000), Color.Transparent)))
                .statusBarsPadding().padding(start = 6.dp, end = 18.dp, top = 6.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBtn(Icons.Rounded.Close, onClose, tint = Color.White, desc = stringResource(R.string.close))
            Box(Modifier.weight(1f))
            Text("${pager.currentPage + 1} / ${card.size}", style = T.Heading.copy(color = Color.White))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xB3000000))))
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Seconds and size: what tells the frames of one burst apart when the eye cannot.
            val time = Instant.ofEpochMilli(x.taken).atZone(Dates.zone()).format(SECONDS)
            val dims = if (x.w > 0 && x.h > 0) " · ${x.w}×${x.h}" else ""
            Text("$time$dims · ${Fmt.bytes(x.size, ru)}", style = T.Label.copy(color = Color.White), textAlign = TextAlign.Center)
            Text(stringResource(R.string.lens_hint), style = T.Label.copy(color = Color.White.copy(alpha = .7f)), textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            Btn(stringResource(if (on) R.string.lens_picked else R.string.lens_pick), { onPick(x) },
                Modifier.fillMaxWidth(), if (on) Kind.GREEN else Kind.ACCENT, icon = if (on) Icons.Rounded.Check else null)
        }
    }
}

/** One shot that can be pinched, panned and double-tapped. [onZoomed] says whether it is zoomed in now. */
@Composable
private fun ZoomPage(item: Item, active: Boolean, onZoomed: (Boolean) -> Unit) {
    val scope = rememberCoroutineScope()
    var scale by remember { mutableFloatStateOf(1f) }
    var off by remember { mutableStateOf(Offset.Zero) }
    // Swiped away: it is back to whole when it comes round again.
    LaunchedEffect(active) { if (!active) { scale = 1f; off = Offset.Zero } }
    val zoomedIn = scale > 1.01f
    LaunchedEffect(zoomedIn, active) { if (active) onZoomed(zoomedIn) }

    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val bw = constraints.maxWidth.toFloat()
        val bh = constraints.maxHeight.toFloat()
        val center = Offset(bw / 2, bh / 2)
        fun clamp(o: Offset, s: Float): Offset {
            val mx = bw * (s - 1) / 2
            val my = bh * (s - 1) / 2
            return Offset(o.x.coerceIn(-mx, mx), o.y.coerceIn(-my, my))
        }
        // The point under [at] stays where it is while the scale goes from [s0] to [s1].
        fun around(at: Offset, o: Offset, s0: Float, s1: Float) = at - center - (at - center - o) * (s1 / s0)

        Thumb(
            item,
            Modifier.fillMaxSize()
                .pointerInput(bw, bh) {
                    detectTapGestures(onDoubleTap = { at ->
                        val s0 = scale
                        val o0 = off
                        val s1 = if (s0 > 1.05f) 1f else TAP_ZOOM
                        val o1 = clamp(around(at, o0, s0, s1), s1)
                        scope.launch { animate(0f, 1f, animationSpec = tween(220)) { t, _ -> scale = s0 + (s1 - s0) * t; off = o0 + (o1 - o0) * t } }
                    })
                }
                .pointerInput(bw, bh) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val ev = awaitPointerEvent()
                            // One finger on a whole picture is left to the pager; two fingers, or any on a zoomed one, are ours.
                            if (ev.changes.count { it.pressed } > 1 || scale > 1.01f) {
                                val s1 = (scale * ev.calculateZoom()).coerceIn(1f, MAX_ZOOM)
                                val at = ev.calculateCentroid()
                                off = clamp((if (at.isSpecified) around(at, off, scale, s1) else off) + ev.calculatePan(), s1)
                                scale = s1
                                ev.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (ev.changes.any { it.pressed })
                    }
                }
                .graphicsLayer { scaleX = scale; scaleY = scale; translationX = off.x; translationY = off.y },
            fit = true,
            badge = false,
            back = Color.Black,
            // Looked at closely, it is worth decoding more of the original.
            scale = if (scale > 1.4f) TAP_ZOOM else 1f,
        )
    }
}
