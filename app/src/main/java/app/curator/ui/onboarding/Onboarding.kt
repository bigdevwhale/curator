package app.curator.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Item
import app.curator.data.Media
import app.curator.ui.art.Facade
import app.curator.ui.components.Btn
import app.curator.ui.components.Kind
import app.curator.ui.components.Stamp
import app.curator.ui.components.Thumb
import app.curator.ui.components.room
import app.curator.ui.components.soft
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Asks for the gallery; afterwards the museum looks again and takes stock. */
@Composable
fun askAccess(after: () -> Unit = {}): () -> Unit {
    val c = LocalContext.current.container
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        c.write { sync() }
        after()
    }
    return { launcher.launch(Media.permissions()) }
}

private enum class G { UP, RIGHT, DOWN, LEFT, HOLD, PICK }

/** One lesson of the training crate: a made-up thing and the gesture it teaches. */
private class Lesson(val g: G, val title: Int, val hint: Int, val stamp: Int, val items: List<Item>)

/** Training photos are bundled stock photographs from Unsplash (see README), never the person's own. */
private fun fake(id: Long, name: String) = Item(id = id, taken = 0, month = 202401, md = 101, size = 3_000_000, seed = 1, art = "asset:training/$name.jpg", w = 3, h = 4)

private val LESSONS = listOf(
    Lesson(G.UP, R.string.tr_up_t, R.string.tr_up_b, R.string.s_del, listOf(fake(-11, "up"))),
    Lesson(G.RIGHT, R.string.tr_right_t, R.string.tr_right_b, R.string.s_keep, listOf(fake(-12, "right"))),
    Lesson(G.DOWN, R.string.tr_down_t, R.string.tr_down_b, R.string.s_hall, listOf(fake(-13, "down"))),
    Lesson(G.LEFT, R.string.tr_left_t, R.string.tr_left_b, R.string.s_later, listOf(fake(-14, "left"))),
    Lesson(G.HOLD, R.string.tr_hold_t, R.string.tr_hold_b, R.string.s_ex, listOf(fake(-15, "hold"))),
    Lesson(G.PICK, R.string.tr_pick_t, R.string.tr_pick_b, R.string.original, (0 until 4).map { fake(-20L - it, "s$it") }),
)

@Composable
fun Onboarding(start: Int = 0) {
    val c = LocalContext.current.container
    var page by remember { mutableIntStateOf(start) }
    val finish = { c.write { found(); settings.update { it.copy(onboarded = true, trained = true) } } }
    val asked = askAccess { finish() }
    // The demo museum reads no real gallery, so it doesn't ask for one.
    val ask = if (c.demo) ({ finish() }) else asked
    Box(Modifier.fillMaxSize().room().statusBarsPadding().navigationBarsPadding()) {
        when (page) {
            0 -> Intro({ page = 1 }, { page = 2 })
            1 -> Training { page = 2 }
            else -> Finale(ask)
        }
    }
}

@Composable
private fun Intro(onStart: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(stringResource(R.string.museum_of).uppercase(), style = T.Caps, modifier = Modifier.padding(top = 12.dp))
        Text(stringResource(R.string.app_name), style = T.Hero)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Facade(3, Modifier.fillMaxWidth(), glow = 2) }
        Text(stringResource(R.string.ob_1t), style = T.Title)
        Text(stringResource(R.string.ob_1b), style = T.Body.copy(color = C.ink2), modifier = Modifier.padding(top = 10.dp, bottom = 28.dp))
        Btn(stringResource(R.string.tr_start), onStart, Modifier.fillMaxWidth())
        Text(stringResource(R.string.tr_skip), style = T.Label, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).clickable(onClick = onSkip).padding(14.dp))
    }
}

@Composable
private fun Training(onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var stamp by remember { mutableStateOf(false) }
    var miss by remember { mutableStateOf(false) }
    var pick by remember { mutableIntStateOf(-1) }
    val scope = rememberCoroutineScope()
    val lesson = LESSONS[step]
    val shake = remember { Animatable(0f) }

    fun next() {
        dx = 0f; dy = 0f; stamp = false; miss = false; pick = -1
        if (step + 1 < LESSONS.size) step++ else onDone()
    }

    fun pass(tx: Float, ty: Float) = scope.launch {
        stamp = true
        delay(if (lesson.g == G.HOLD) 450 else 150)
        val sx = dx
        val sy = dy
        launch { animate(sx, tx, animationSpec = tween(260)) { v, _ -> dx = v } }
        animate(sy, ty, animationSpec = tween(260)) { v, _ -> dy = v }
        delay(120)
        next()
    }

    fun fail() = scope.launch {
        miss = true
        val sx = dx
        val sy = dy
        launch { animate(sx, 0f, animationSpec = spring()) { v, _ -> dx = v } }
        launch { animate(sy, 0f, animationSpec = spring()) { v, _ -> dy = v } }
        for (k in listOf(14f, -12f, 8f, -4f, 0f)) shake.animateTo(k, tween(50))
    }

    fun released(x: Float, y: Float, far: Float) {
        val g = when {
            maxOf(abs(x), abs(y)) < far -> null
            abs(y) > abs(x) -> if (y < 0) G.UP else G.DOWN
            else -> if (x > 0) G.RIGHT else G.LEFT
        }
        when {
            g == null -> fail()
            g != lesson.g -> fail()
            g == G.UP -> pass(0f, -2600f)
            g == G.DOWN -> pass(0f, 2600f)
            g == G.RIGHT -> pass(1800f, 0f)
            else -> pass(-1800f, 0f)
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (i in LESSONS.indices) Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(50)).background(if (i <= step) C.accent else C.line))
        }
        Text(stringResource(R.string.tr_caps, step + 1, LESSONS.size).uppercase(), style = T.Caps, modifier = Modifier.padding(top = 18.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            GestureIcon(lesson.g)
            Text(stringResource(lesson.title), style = T.Title, modifier = Modifier.padding(start = 12.dp))
        }
        Text(stringResource(if (miss) R.string.tr_miss else lesson.hint), style = T.Body.copy(color = if (miss) C.red else C.ink2), modifier = Modifier.padding(top = 6.dp, bottom = 16.dp))

        Box(Modifier.weight(1f).fillMaxWidth().padding(bottom = 24.dp), contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(.78f).graphicsLayer { translationX = dx + shake.value; translationY = dy; rotationZ = dx / 40f }
                    .soft().clip(RoundedCornerShape(24.dp)).background(C.surface).padding(10.dp)
                    .then(if (lesson.g == G.PICK) Modifier else Modifier.pointerInput(step) {
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            var tot = Offset.Zero
                            val res = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                var r = 0
                                while (r == 0) {
                                    val ch = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                                    if (ch == null || !ch.pressed) r = 2 else {
                                        tot += ch.positionChange()
                                        if (tot.getDistance() > viewConfiguration.touchSlop) { ch.consume(); r = 1 }
                                    }
                                }
                                r
                            }
                            when (res) {
                                null -> if (lesson.g == G.HOLD) pass(900f, -2200f) else fail()
                                2 -> fail()
                                else -> {
                                    dx = tot.x; dy = tot.y
                                    drag(down.id) { ch -> tot += ch.positionChange(); ch.consume(); dx = tot.x; dy = tot.y }
                                    released(dx, dy, 110.dp.toPx())
                                }
                            }
                        }
                    })
            ) {
                if (lesson.g == G.PICK) {
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (r in 0..1) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (k in 0..1) {
                                val i = r * 2 + k
                                val on = pick == i
                                Box(Modifier.weight(1f).fillMaxSize().clip(RoundedCornerShape(14.dp)).alpha(if (pick >= 0 && !on) .45f else 1f)
                                    .then(if (on) Modifier.border(3.dp, C.accent, RoundedCornerShape(14.dp)) else Modifier)
                                    .clickable { if (pick < 0) { pick = i; scope.launch { delay(700); next() } } }) {
                                    Thumb(lesson.items[i], Modifier.fillMaxSize(), px = 360)
                                }
                            }
                        }
                    }
                } else {
                    Thumb(lesson.items[0], Modifier.fillMaxSize(), px = 900, shape = RoundedCornerShape(16.dp))
                }
                if (stamp && lesson.g != G.PICK) Stamp(stringResource(lesson.stamp), when (lesson.g) {
                    G.UP -> C.red; G.RIGHT -> C.green; G.HOLD -> C.accent; G.DOWN -> C.ink2; else -> C.muted
                }, Modifier.align(Alignment.TopCenter).padding(top = 32.dp))
            }
        }
    }
}

@Composable
private fun GestureIcon(g: G) {
    val (icon: ImageVector, col) = when (g) {
        G.UP -> Icons.Rounded.ArrowUpward to C.red
        G.RIGHT -> Icons.AutoMirrored.Rounded.ArrowForward to C.green
        G.DOWN -> Icons.Rounded.ArrowDownward to C.ink2
        G.LEFT -> Icons.AutoMirrored.Rounded.ArrowBack to C.muted
        G.HOLD -> Icons.Rounded.Star to C.accent
        G.PICK -> Icons.Rounded.TouchApp to C.accent
    }
    Box(Modifier.size(44.dp).clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = col, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Finale(ask: () -> Unit) {
    val s = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(250); s.animateTo(1f, spring(dampingRatio = .45f, stiffness = 300f)) }
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            Stamp(stringResource(R.string.act_signed), C.red, Modifier.graphicsLayer { val k = 2.2f - 1.2f * s.value; scaleX = k; scaleY = k; alpha = s.value.coerceIn(0f, 1f) })
        }
        Text(stringResource(R.string.ob_3t), style = T.Title)
        Text(stringResource(R.string.ob_3b), style = T.Body.copy(color = C.ink2), modifier = Modifier.padding(top = 10.dp))
        Text(stringResource(R.string.ob_private), style = T.Small, modifier = Modifier.padding(top = 10.dp, bottom = 28.dp))
        Btn(stringResource(R.string.ob_go), ask, Modifier.fillMaxWidth())
    }
}

/** The gallery is closed to the museum: ask again, or send the person to the app's settings. */
@Composable
fun AccessScreen() {
    val ctx = LocalContext.current
    val ask = askAccess()
    Column(Modifier.fillMaxSize().room().statusBarsPadding().navigationBarsPadding().padding(24.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Facade(1, Modifier.fillMaxWidth()) }
        Text(stringResource(R.string.closed_t), style = T.Title)
        Text(stringResource(R.string.closed_b), style = T.Body.copy(color = C.ink2), modifier = Modifier.padding(top = 10.dp, bottom = 28.dp))
        Btn(stringResource(R.string.closed_ask), ask, Modifier.fillMaxWidth())
        Btn(stringResource(R.string.closed_settings), {
            runCatching { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.GHOST)
    }
    Box(Modifier.width(0.dp))
}
