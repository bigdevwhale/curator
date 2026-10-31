package app.curator.ui.crate

import androidx.compose.animation.core.animate
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.ui.unit.sp
import app.curator.ui.components.IconBtn
import app.curator.ui.components.soft
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.Crate
import app.curator.R
import app.curator.container
import app.curator.data.Fund
import app.curator.data.Hall
import app.curator.data.Item
import app.curator.data.Source
import app.curator.data.Verdict
import app.curator.domain.Dates
import app.curator.domain.Defect
import app.curator.locale
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.Tab
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Card
import app.curator.ui.components.Scrim
import app.curator.ui.components.Stamp
import app.curator.ui.components.Thumb
import app.curator.ui.components.TopBar
import app.curator.ui.openInGallery
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

enum class Dir { DEL, KEEP, LATER, HALL, EX }

/** What one crate has given so far. */
data class Tally(val keep: Int = 0, val ex: Int = 0, val act: Int = 0, val hall: Int = 0, val later: Int = 0, val bytes: Long = 0) {
    operator fun plus(o: Tally) = Tally(keep + o.keep, ex + o.ex, act + o.act, hall + o.hall, later + o.later, bytes + o.bytes)
    operator fun minus(o: Tally) = Tally(keep - o.keep, ex - o.ex, act - o.act, hall - o.hall, later - o.later, bytes - o.bytes)
    val described get() = keep + ex + act + hall
}

private class Step(val card: List<Item>, val delta: Tally)

@Composable
fun CrateScreen(source: Source, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    var round by remember { mutableIntStateOf(0) }
    val crate by produceState<Crate?>(null, source, round) { value = null; value = c.crate(source, ru) }
    val label = when (source) {
        is Source.Month -> Dates.monthName(source.month, ctx.locale)
        is Source.Special -> stringResource(fundName(source.fund))
    }
    val cr = crate
    if (cr == null) {
        Column(Modifier.fillMaxSize()) {
            TopBar(label, null, nav::close)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.expert_looks), style = T.Italic.copy(color = C.muted), textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
        }
        return
    }
    // A new round is a new crate: everything below starts afresh.
    androidx.compose.runtime.key(cr.no, round) { Table(cr, label, h, nav) { round++ } }
}

fun fundName(f: Fund) = when (f) {
    Fund.SHOTS -> R.string.fund_shots
    Fund.VIDEO -> R.string.fund_video
    Fund.CHATS -> R.string.fund_chats
    Fund.HEAVY -> R.string.fund_heavy
}

@Composable
private fun Table(cr: Crate, label: String, h: Home, nav: Nav, next: () -> Unit) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val deck = remember { mutableStateListOf<List<Item>>().apply { addAll(cr.cards) } }
    val hist = remember { mutableStateListOf<Step>() }
    var tally by remember { mutableStateOf(Tally()) }
    var expert by remember { mutableStateOf(cr.rejects.isNotEmpty()) }
    var picking by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    var dx by remember { mutableFloatStateOf(0f) }
    var dy by remember { mutableFloatStateOf(0f) }
    var exStamp by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var fit by remember { mutableStateOf(false) }
    val total = cr.cards.sumOf { it.size } + cr.rejects.size
    val empty = cr.cards.isEmpty() && cr.rejects.isEmpty()
    val sLater = stringResource(R.string.later_note)

    LaunchedEffect(note) { if (note != null) { delay(1800); note = null } }

    fun commit(d: Dir, hall: Hall? = null) {
        val card = deck.firstOrNull() ?: return
        val x = card.first()
        val delta = when (d) {
            Dir.DEL -> Tally(act = 1, bytes = x.size)
            Dir.KEEP -> Tally(keep = 1)
            Dir.EX -> Tally(ex = 1)
            Dir.HALL -> Tally(hall = 1)
            Dir.LATER -> Tally(later = 1)
        }
        hist += Step(card, delta)
        tally += delta
        deck.removeAt(0)
        dx = 0f; dy = 0f; exStamp = false; fit = false
        c.write {
            when (d) {
                Dir.DEL -> decide(x, Verdict.ACT)
                Dir.KEEP -> decide(x, Verdict.KEEP)
                Dir.EX -> decide(x, Verdict.EXHIBIT)
                Dir.HALL -> decide(x, Verdict.KEEP, hall?.id)
                Dir.LATER -> decide(x, Verdict.LATER)
            }
        }
        if (d == Dir.LATER) note = sLater
    }

    fun fly(d: Dir, hall: Hall? = null) {
        if (busy || deck.isEmpty() || deck.first().size > 1) return
        busy = true
        scope.launch {
            if (d == Dir.EX) { exStamp = true; delay(380) }
            val (tx, ty) = when (d) {
                Dir.DEL -> 0f to -2400f
                Dir.KEEP -> 1600f to 0f
                Dir.LATER -> -1600f to 0f
                Dir.HALL -> 0f to 2400f
                Dir.EX -> 1100f to -2000f
            }
            val sx = dx
            val sy = dy
            launch { animate(sx, tx, animationSpec = tween(230)) { v, _ -> dx = v } }
            animate(sy, ty, animationSpec = tween(230)) { v, _ -> dy = v }
            commit(d, hall)
            busy = false
        }
    }

    fun back() {
        scope.launch { val sx = dx; val sy = dy; launch { animate(sx, 0f, animationSpec = tween(200)) { v, _ -> dx = v } }; animate(sy, 0f, animationSpec = tween(200)) { v, _ -> dy = v } }
    }

    fun act(d: Dir) {
        if (busy || deck.isEmpty()) return
        if (deck.first().size > 1) { note = ctx.getString(R.string.series_first); return }
        if (d == Dir.HALL) { back(); picking = true } else fly(d)
    }

    fun undo() {
        val s = hist.removeLastOrNull() ?: run { note = ctx.getString(R.string.nothing_to_undo); return }
        tally -= s.delta
        deck.add(0, s.card)
        dx = 0f; dy = 0f; exStamp = false
        c.write { for (x in s.card) restore(x) }
    }

    fun series(card: List<Item>, keep: Item?) {
        val off = if (keep == null) emptyList() else card.filter { it.id != keep.id }
        val delta = Tally(keep = card.size - off.size, act = off.size, bytes = off.sumOf { it.size })
        hist += Step(card, delta)
        tally += delta
        deck.removeAt(0)
        c.write { for (x in card) decide(x, if (x in off) Verdict.ACT else Verdict.KEEP) }
    }

    val done = deck.isEmpty() && !expert
    val left = deck.size
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.crate_no, cr.no) + " · " + label,
            if (empty) null else stringResource(R.string.crate_cards_left, left), nav::close) {
            IconBtn(Icons.AutoMirrored.Rounded.Undo, { undo() }, desc = stringResource(R.string.undo))
        }
        Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
            if (empty) Text(stringResource(R.string.crate_empty), style = T.Italic.copy(color = C.muted), textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
            deck.getOrNull(1)?.let { under ->
                Box(Modifier.fillMaxSize().graphicsLayer { scaleX = .95f; scaleY = .95f; translationY = 14.dp.toPx(); alpha = .8f }) { CardFace(under, cr, ru, null, false, false) {} }
            }
            deck.firstOrNull()?.let { top ->
                val key = top.first().id
                val dir = when {
                    exStamp -> Dir.EX
                    maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) < 20f -> null
                    kotlin.math.abs(dy) > kotlin.math.abs(dx) -> if (dy < 0) Dir.DEL else Dir.HALL
                    else -> if (dx > 0) Dir.KEEP else Dir.LATER
                }
                val strength = if (exStamp) 1f else (maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) / 260f).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxSize().graphicsLayer { translationX = dx; translationY = dy; rotationZ = dx / 40f }
                        .then(if (top.size > 1) Modifier else Modifier.pointerInput(key) {
                            val slop = viewConfiguration.touchSlop
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                var tot = Offset.Zero
                                val res = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                    var r = 0
                                    while (r == 0) {
                                        val ev = awaitPointerEvent()
                                        val ch = ev.changes.firstOrNull { it.id == down.id }
                                        if (ch == null || !ch.pressed) r = 2
                                        else {
                                            tot += ch.positionChange()
                                            if (tot.getDistance() > slop) { ch.consume(); r = 1 }
                                        }
                                    }
                                    r
                                }
                                when (res) {
                                    null -> fly(Dir.EX)
                                    2 -> if (top.first().video) openInGallery(ctx, top.first()) else fit = !fit
                                    else -> if (!busy) {
                                        dx = tot.x; dy = tot.y
                                        drag(down.id) { ch -> tot += ch.positionChange(); ch.consume(); dx = tot.x; dy = tot.y }
                                        val far = maxOf(kotlin.math.abs(dx), kotlin.math.abs(dy)) > 120.dp.toPx()
                                        val d = if (kotlin.math.abs(dy) > kotlin.math.abs(dx)) (if (dy < 0) Dir.DEL else Dir.HALL) else (if (dx > 0) Dir.KEEP else Dir.LATER)
                                        if (far) act(d) else back()
                                    }
                                }
                            }
                        })
                ) {
                    CardFace(top, cr, ru, dir?.let { it to strength }, fit, exStamp) { keep -> series(top, keep) }
                }
            }
        }
        if (!empty) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActBtn(Icons.Rounded.Close, stringResource(R.string.a_del), C.red) { act(Dir.DEL) }
                ActBtn(Icons.Outlined.Collections, stringResource(R.string.a_hall), C.ink2) { act(Dir.HALL) }
                ActBtn(Icons.Rounded.Star, stringResource(R.string.a_ex), C.accent) { act(Dir.EX) }
                ActBtn(Icons.Rounded.Check, stringResource(R.string.a_keep), C.green) { act(Dir.KEEP) }
            }
            Text(stringResource(R.string.gestures), style = T.Label.copy(fontSize = 12.sp), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }

    note?.let { n ->
        Box(Modifier.fillMaxSize().padding(bottom = 130.dp), contentAlignment = Alignment.BottomCenter) {
            Text(n, style = T.Label.copy(color = C.bg), modifier = Modifier.clip(RoundedCornerShape(50)).background(C.ink).padding(horizontal = 16.dp, vertical = 10.dp))
        }
    }

    if (expert) ExpertSheet(cr, total) { saved ->
        val off = cr.rejects.map { it.first }.filter { it.id !in saved }
        deck.addAll(0, cr.rejects.map { it.first }.filter { it.id in saved }.map { listOf(it) })
        tally += Tally(act = off.size, bytes = off.sumOf { it.size })
        c.write { for (x in off) decide(x, Verdict.ACT) }
        expert = false
    }

    if (picking) HallPicker(h.halls, h.hallCounts, onDismiss = { picking = false }) { hall -> picking = false; fly(Dir.HALL, hall) }

    if (done && !empty) {
        LaunchedEffect(Unit) { c.closeCrate(cr.no, tally.described, label) }
        val more = when (val s = cr.source) {
            is Source.Month -> (h.months.firstOrNull { it.month == s.month }?.pending ?: 0) > 0
            is Source.Special -> (h.funds[s.fund]?.n ?: 0) > 0
        }
        Summary(cr, label, tally, more, onNext = next, onAct = { nav.stack.clear(); nav.tab = Tab.ACT }, onHome = nav::close)
    }
}

@Composable
private fun ActBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(C.surface).border(1.dp, C.line, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, label, tint = color, modifier = Modifier.size(26.dp))
        }
        Text(label, style = T.Label.copy(fontSize = 12.sp), modifier = Modifier.padding(top = 6.dp))
    }
}

private val TIME = DateTimeFormatter.ofPattern("HH:mm")

/** One card on the table: a thing on its inventory card, or a series to choose the original from. */
@Composable
private fun CardFace(card: List<Item>, cr: Crate, ru: Boolean, stamp: Pair<Dir, Float>?, fit: Boolean, ex: Boolean, onSeries: (Item?) -> Unit) {
    val ctx = LocalContext.current
    val x = card.first()
    Column(
        Modifier.fillMaxSize().soft().clip(RoundedCornerShape(24.dp)).background(C.surface)
            .then(if (ex) Modifier.border(3.dp, C.accent, RoundedCornerShape(24.dp)) else if (C.dark) Modifier.border(1.dp, C.line, RoundedCornerShape(24.dp)) else Modifier)
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 14.dp)
    ) {
        if (card.size > 1) {
            var pick by remember(x.id) { mutableStateOf<Item?>(null) }
            Text(stringResource(R.string.series_t, card.size), style = T.Heading, modifier = Modifier.padding(start = 6.dp, top = 6.dp))
            Text(stringResource(R.string.series_b), style = T.Small, modifier = Modifier.padding(start = 6.dp, bottom = 10.dp))
            LazyVerticalGrid(GridCells.Fixed(if (card.size <= 4) 2 else 3), Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                itemsIndexed(card, key = { _, it -> it.id }) { _, it ->
                    val on = pick?.id == it.id
                    Box(Modifier.aspectRatio(.8f).clip(RoundedCornerShape(12.dp)).clickable { pick = it }.then(if (on) Modifier.border(3.dp, C.accent, RoundedCornerShape(12.dp)) else Modifier)
                        .alpha(if (pick != null && !on) .45f else 1f)) {
                        Thumb(it, Modifier.fillMaxSize(), px = 360, shape = RoundedCornerShape(12.dp))
                        if (on) Text(stringResource(R.string.original).uppercase(), style = T.Caps.copy(color = C.onAccent, letterSpacing = T.Caps.letterSpacing * .4f),
                            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp).background(C.accent).padding(horizontal = 5.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.keep_all), { onSeries(null) }, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.keep_original), { onSeries(pick) }, Modifier.weight(1f), Kind.GREEN, enabled = pick != null)
            }
            return@Column
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Thumb(x, Modifier.fillMaxSize(), px = 1080, fit = fit, shape = RoundedCornerShape(16.dp))
            stamp?.let { (d, a) ->
                val (text, col) = when (d) {
                    Dir.DEL -> R.string.s_del to C.red
                    Dir.KEEP -> R.string.s_keep to C.green
                    Dir.HALL -> R.string.s_hall to C.accent
                    Dir.LATER -> R.string.s_later to C.muted
                    Dir.EX -> R.string.s_ex to C.accent
                }
                Stamp(stringResource(text), col, Modifier.align(Alignment.TopCenter).padding(top = 36.dp).alpha(a))
            }
        }
        val d = Dates.date(x.taken)
        val dateFmt = DateTimeFormatter.ofPattern(if (ru) "d MMMM yyyy" else "MMMM d, yyyy", ctx.locale)
        Text(stringResource(R.string.inv_no, cr.inv[x.id] ?: "").uppercase(), style = T.Caps.copy(color = C.accent), modifier = Modifier.padding(start = 6.dp, top = 12.dp))
        Text(d.format(dateFmt) + ", " + java.time.Instant.ofEpochMilli(x.taken).atZone(Dates.zone()).format(TIME) +
            (if (x.bucket.isNotBlank()) " · " + x.bucket else "") + " · " + Fmt.bytes(x.size, ru), style = T.Body.copy(color = C.ink2), maxLines = 1,
            modifier = Modifier.padding(start = 6.dp, top = 2.dp))
    }
}

@Composable
private fun ExpertSheet(cr: Crate, total: Int, onDone: (Set<Long>) -> Unit) {
    val saved = remember { mutableStateListOf<Long>() }
    Scrim(null) {
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.expert_t), style = T.Title)
            Text(stringResource(R.string.expert_b, pluralStringResource(R.plurals.things, total, total), cr.rejects.size), style = T.Small, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            LazyVerticalGrid(GridCells.Fixed(3), Modifier.fillMaxWidth().heightIn(max = 380.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(cr.rejects, key = { _, it -> it.first.id }) { _, (x, f) ->
                    val on = x.id in saved
                    Box(Modifier.aspectRatio(.8f).clip(RoundedCornerShape(12.dp)).clickable { if (on) saved.remove(x.id) else saved.add(x.id) }.then(if (on) Modifier.border(3.dp, C.green, RoundedCornerShape(12.dp)) else Modifier)) {
                        Thumb(x, Modifier.fillMaxSize(), px = 300)
                        Text((if (on) stringResource(R.string.rescued) else stringResource(defectName(f))).uppercase(),
                            style = T.Caps.copy(color = Color.White, letterSpacing = T.Caps.letterSpacing * .3f), textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(if (on) C.green.copy(alpha = .9f) else C.red.copy(alpha = .88f)).padding(2.dp))
                    }
                }
            }
            val off = cr.rejects.size - saved.size
            Btn(if (off > 0) stringResource(R.string.expert_off, off) else stringResource(R.string.expert_none), { onDone(saved.toSet()) },
                Modifier.fillMaxWidth().padding(top = 14.dp), if (off > 0) Kind.RED else Kind.GREEN)
            Btn(stringResource(R.string.expert_all), { onDone(cr.rejects.map { it.first.id }.toSet()) }, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.GHOST)
        }
    }
}

fun defectName(f: Int) = when {
    f and Defect.DUP != 0 -> R.string.d_dup
    f and Defect.SHOT != 0 -> R.string.d_shot
    f and Defect.DARK != 0 -> R.string.d_dark
    else -> R.string.d_blur
}

@Composable
private fun Summary(cr: Crate, label: String, t: Tally, more: Boolean, onNext: () -> Unit, onAct: () -> Unit, onHome: () -> Unit) {
    val ru = LocalContext.current.ru
    Scrim(null) {
        Card(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.sum_caps).uppercase(), style = T.Caps.copy(color = C.accent), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(stringResource(R.string.sum_t), style = T.Title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            Text(stringResource(R.string.crate_no, cr.no) + " · " + label, style = T.Small, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            val cells = listOf(
                (t.keep + t.hall).toString() to stringResource(R.string.in_fonds),
                t.ex.toString() to stringResource(R.string.exhibits),
                t.act.toString() to stringResource(R.string.to_act),
                Fmt.bytes(t.bytes, ru) to stringResource(R.string.to_free),
            )
            for (row in cells.chunked(2)) Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for ((big, small) in row) Column(Modifier.weight(1f).background(C.surface2, RoundedCornerShape(14.dp)).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(big, style = T.Number)
                    Text(small, style = T.Label, textAlign = TextAlign.Center)
                }
            }
            Text(stringResource(if (t.ex > 0) R.string.sum_ex else if (t.later > 0) R.string.sum_later else R.string.sum_plain), style = T.Small,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            if (more) Btn(stringResource(R.string.next_crate), onNext, Modifier.fillMaxWidth().padding(top = 12.dp), Kind.GREEN)
            if (t.act > 0) Btn(stringResource(R.string.go_sign), onAct, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.ACCENT)
            Btn(stringResource(R.string.to_museum), onHome, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.GHOST)
        }
    }
}
