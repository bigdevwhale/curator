package app.curator.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Access
import app.curator.data.Fund
import app.curator.data.Item
import app.curator.data.MonthRow
import app.curator.data.Source
import app.curator.data.Verdict
import app.curator.domain.Dates
import app.curator.domain.Ranks
import app.curator.domain.Wings
import app.curator.locale
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.Overlay
import app.curator.ui.Tab
import app.curator.ui.art.Facade
import app.curator.ui.components.Bar
import app.curator.ui.components.Caps
import app.curator.ui.components.Card
import app.curator.ui.components.Fmt
import app.curator.ui.components.IconBtn
import app.curator.ui.components.RoundS
import app.curator.ui.components.Stats
import app.curator.ui.components.Thumb
import app.curator.ui.onboarding.askAccess
import app.curator.ui.openInGallery
import app.curator.ui.theme.C
import app.curator.ui.theme.T

@Composable
fun MuseumScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val ru = ctx.ru
    val s = h.s
    val described = h.stats.total - h.stats.pending + s.writtenOff
    val rank = Ranks.of(described)
    val ranks = stringArrayResource(R.array.ranks)
    val ask = askAccess()

    LazyColumn(Modifier.fillMaxSize().statusBarsPadding(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.museum_of).uppercase(), style = T.Caps, modifier = Modifier.weight(1f))
                IconBtn(Icons.Outlined.History, { nav.open(Overlay.Chronicle) }, desc = stringResource(R.string.chronicle))
                IconBtn(Icons.Outlined.Settings, { nav.open(Overlay.Settings) }, desc = stringResource(R.string.settings))
            }
            Text(stringResource(R.string.app_name), style = T.Hero, modifier = Modifier.padding(horizontal = 20.dp))
            Text(ranks[rank] + " · " + pluralStringResource(R.plurals.described, described, Fmt.n(described)), style = T.Label,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 2.dp))

            Card(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp), pad = 0.dp) {
                Facade(Wings.open(s.freed), Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 22.dp, bottom = 6.dp))
                val next = Wings.next(s.freed)
                val wings = stringArrayResource(R.array.wings)
                Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                    Text(if (next == null) stringResource(R.string.wings_all) else stringResource(R.string.wing_next, wings[Wings.open(s.freed)], Fmt.bytes(next, ru)), style = T.Label.copy(color = C.ink2))
                    Bar(Wings.progress(s.freed), Modifier.padding(top = 8.dp))
                }
                Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
                Stats(listOf(
                    Fmt.n(h.stats.fonds) to stringResource(R.string.in_fonds),
                    Fmt.n(h.stats.exhibits) to stringResource(R.string.exhibits),
                    Fmt.bytes(s.freed, ru) to stringResource(R.string.freed),
                ), Modifier.fillMaxWidth().padding(vertical = 16.dp))
            }
            Ranks.next(described)?.let { n ->
                Text(stringResource(R.string.rank_next, ranks[rank + 1], Fmt.n(n - described)), style = T.Small, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 10.dp))
            }

            if (h.access == Access.PARTIAL) Banner(Icons.Outlined.Lock, stringResource(R.string.partial_t), stringResource(R.string.partial_b), C.accentSoft, C.accent, ask)
            if (h.act.isNotEmpty()) Banner(Icons.Outlined.Description, stringResource(R.string.act_waits),
                pluralStringResource(R.plurals.things, h.act.size, Fmt.n(h.act.size)) + " · " + Fmt.bytes(h.act.sumOf { it.size }, ru), C.redSoft, C.red) { nav.tab = Tab.ACT }
            OnThisDay(h)

            Caps(stringResource(R.string.funds))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listOf(
                    Triple(Fund.SHOTS, R.string.fund_shots, Icons.Outlined.PhoneAndroid), Triple(Fund.VIDEO, R.string.fund_video, Icons.Outlined.Videocam),
                    Triple(Fund.CHATS, R.string.fund_chats, Icons.Outlined.Chat), Triple(Fund.HEAVY, R.string.fund_heavy, Icons.Outlined.SdStorage),
                )) { (f, label, icon) ->
                    val cnt = h.funds[f]
                    val n = cnt?.n ?: 0
                    Card(Modifier.width(150.dp), pad = 14.dp, onClick = if (n > 0) ({ nav.open(Overlay.Crate(Source.Special(f))) }) else null) {
                        Icon(icon, null, tint = if (n > 0) C.accent else C.muted, modifier = Modifier.size(22.dp))
                        Text(stringResource(label), style = T.Strong.copy(color = if (n > 0) C.ink else C.muted), modifier = Modifier.padding(top = 10.dp), maxLines = 1)
                        Text(if (n > 0) Fmt.n(n) + " · " + Fmt.bytes(cnt!!.bytes, ru) else stringResource(R.string.fund_empty), style = T.Label)
                    }
                }
            }
            Caps(stringResource(R.string.crates), right = stringResource(R.string.by_months))
            if (h.months.isEmpty()) Text(stringResource(if (h.syncing) R.string.syncing else R.string.no_crates), style = T.Italic.copy(color = C.muted),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
        items(h.months, key = { it.month }) { m ->
            CrateRow(m, ctx.locale, ru, onReview = { nav.open(Overlay.Crate(Source.Review(m.month, System.currentTimeMillis()))) }) { nav.open(Overlay.Crate(Source.Month(m.month))) }
        }
    }
}

@Composable
private fun Banner(icon: ImageVector, title: String, sub: String, bg: androidx.compose.ui.graphics.Color, fg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), pad = 14.dp, color = bg, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(fg), contentAlignment = Alignment.Center) { Icon(icon, null, tint = C.surface, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = T.Strong.copy(color = fg))
                Text(sub, style = T.Small.copy(color = C.ink2))
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = fg)
        }
    }
}

/** A crate of one month: its lid shows four of its things. */
@Composable
private fun CrateRow(m: MonthRow, locale: java.util.Locale, ru: Boolean, onReview: () -> Unit, onClick: () -> Unit) {
    val c = LocalContext.current.container
    val done = m.pending == 0
    val cover by produceState<List<Item>>(emptyList(), m.month, m.pending) { value = c.items.cover(m.month) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), pad = 10.dp, onClick = if (done) onReview else onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.size(76.dp).clip(RoundS), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (r in 0..1) Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    for (k in 0..1) {
                        val x = cover.getOrNull(r * 2 + k)
                        if (x != null) Thumb(x, Modifier.weight(1f).aspectRatio(1f), px = 120, badge = false)
                        else Box(Modifier.weight(1f).aspectRatio(1f).background(C.surface2))
                    }
                }
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(Dates.monthName(m.month, locale), style = T.Heading)
                Text(pluralStringResource(R.plurals.things, m.total, Fmt.n(m.total)) + " · " + Fmt.bytes(m.bytes, ru), style = T.Label)
                Bar(1f - m.pending.toFloat() / m.total.coerceAtLeast(1), Modifier.padding(top = 8.dp), color = C.green)
                Text(if (done) stringResource(R.string.crate_done) else stringResource(R.string.crate_left, Fmt.n(m.pending)),
                    style = T.Label.copy(color = if (done) C.green else C.muted), modifier = Modifier.padding(top = 4.dp))
            }
            // A crate gone through can be opened again to look at what was kept (#1).
            if (done) Text(stringResource(R.string.review), style = T.Label.copy(color = C.ink), modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, C.line, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp))
            else Text(stringResource(R.string.crate_open), style = T.Label.copy(color = C.bg), modifier = Modifier.clip(RoundedCornerShape(50)).background(C.ink).padding(horizontal = 12.dp, vertical = 7.dp))
        }
    }
}

/** A thing from this very day in an earlier year, out of the storeroom for a moment. */
@Composable
private fun OnThisDay(h: Home) {
    val ctx = LocalContext.current
    val c = ctx.container
    val today = Dates.date(h.now)
    val found by produceState<Item?>(null, today) {
        val start = today.withDayOfYear(1).atStartOfDay(Dates.zone()).toInstant().toEpochMilli()
        value = c.items.onThisDay(today.monthValue * 100 + today.dayOfMonth, start).firstOrNull()
    }
    val it = found ?: return
    val years = today.year - Dates.date(it.taken).year
    Card(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), pad = 0.dp, onClick = { openInGallery(ctx, it) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(it, Modifier.size(96.dp, 112.dp), px = 300)
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(stringResource(R.string.otd_t).uppercase(), style = T.Caps.copy(color = C.accent))
                Text(pluralStringResource(R.plurals.years_ago, years, years), style = T.Heading, modifier = Modifier.padding(top = 2.dp))
                Text(stringResource(if (it.verdict == Verdict.EXHIBIT) R.string.otd_exhibit else R.string.otd_b), style = T.Small)
            }
        }
    }
}

@Suppress("unused")
private fun Modifier.outline() = this.border(1.dp, C.line, RoundS)
