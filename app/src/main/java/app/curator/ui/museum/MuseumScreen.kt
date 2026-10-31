package app.curator.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import app.curator.ui.components.Fmt
import app.curator.ui.components.Paper
import app.curator.ui.components.Plaque
import app.curator.ui.components.Thumb
import app.curator.ui.openInGallery
import app.curator.ui.onboarding.askAccess
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

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.chronicle).uppercase(), style = T.Caps, modifier = Modifier.clickable { nav.open(Overlay.Chronicle) }.padding(10.dp))
                Box(Modifier.weight(1f))
                Text("⚙", style = T.Heading.copy(color = C.brass), modifier = Modifier.clickable { nav.open(Overlay.Settings) }.padding(horizontal = 12.dp, vertical = 4.dp))
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.museum_of).uppercase(), style = T.Caps)
                Text(stringResource(R.string.app_name), style = T.Hero)
                Text(ranks[rank] + " · " + pluralStringResource(R.plurals.described, described, Fmt.n(described)), style = T.Label.copy(color = C.onWall),
                    modifier = Modifier.padding(top = 2.dp))
            }
            Facade(Wings.open(s.freed), Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp))
            Plaque(Modifier.fillMaxWidth().padding(horizontal = 16.dp), listOf(
                Fmt.n(h.stats.fonds) to stringResource(R.string.in_fonds),
                Fmt.n(h.stats.exhibits) to stringResource(R.string.exhibits),
                Fmt.bytes(s.freed, ru) to stringResource(R.string.freed),
            ))
            val next = Wings.next(s.freed)
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                val wings = stringArrayResource(R.array.wings)
                Text(if (next == null) stringResource(R.string.wings_all) else stringResource(R.string.wing_next, wings[Wings.open(s.freed)], Fmt.bytes(next, ru)),
                    style = T.Label.copy(color = C.paper))
                Bar(Wings.progress(s.freed), Modifier.padding(top = 5.dp))
                Ranks.next(described)?.let { n ->
                    Text(stringResource(R.string.rank_next, ranks[rank + 1], Fmt.n(n - described)), style = T.Label.copy(color = C.onWall.copy(alpha = .8f)), modifier = Modifier.padding(top = 6.dp))
                }
            }
            if (h.access == Access.PARTIAL) Paper(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), onClick = ask) {
                Text(stringResource(R.string.partial_t), style = T.Strong)
                Text(stringResource(R.string.partial_b), style = T.Small)
            }
            if (h.act.isNotEmpty()) Paper(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), pad = 12.dp, onClick = { nav.tab = Tab.ACT }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.act_waits), style = T.Strong.copy(color = C.red))
                        Text(pluralStringResource(R.plurals.things, h.act.size, Fmt.n(h.act.size)) + " · " + Fmt.bytes(h.act.sumOf { it.size }, ru), style = T.Small)
                    }
                    Text("›", style = T.Title.copy(color = C.red))
                }
            }
            OnThisDay(h)
            Caps(stringResource(R.string.funds))
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(Fund.SHOTS to R.string.fund_shots, Fund.VIDEO to R.string.fund_video, Fund.CHATS to R.string.fund_chats, Fund.HEAVY to R.string.fund_heavy)) { (f, label) ->
                    val cnt = h.funds[f]
                    val n = cnt?.n ?: 0
                    Column(
                        Modifier.shadow(4.dp, RoundedCornerShape(3.dp)).background(if (n > 0) C.paper else C.paper.copy(alpha = .45f), RoundedCornerShape(3.dp))
                            .clickable(enabled = n > 0) { nav.open(Overlay.Crate(Source.Special(f))) }.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Text(stringResource(label), style = T.Strong)
                        Text(if (n > 0) Fmt.n(n) + " · " + Fmt.bytes(cnt!!.bytes, ru) else stringResource(R.string.fund_empty), style = T.Label)
                    }
                }
            }
            Caps(stringResource(R.string.crates), right = stringResource(R.string.by_months))
            if (h.months.isEmpty()) Text(stringResource(if (h.syncing) R.string.syncing else R.string.no_crates), style = T.Italic.copy(color = C.onWall),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
        items(h.months, key = { it.month }) { m -> CrateRow(m, ctx.locale, ru) { nav.open(Overlay.Crate(Source.Month(m.month))) } }
    }
}

@Composable
private fun CrateRow(m: MonthRow, locale: java.util.Locale, ru: Boolean, onClick: () -> Unit) {
    val done = m.pending == 0
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp).shadow(5.dp, RoundedCornerShape(3.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFB88F5A), C.crate, Color(0xFFB88F5A))), RoundedCornerShape(3.dp))
            .drawBehind {
                for (k in 1..2) drawRect(C.crateD, Offset(0f, size.height * k / 3f), androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx()))
            }
            .clickable(enabled = !done, onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).shadow(2.dp).background(C.paper).padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(Dates.monthName(m.month, locale), style = T.Heading)
            Text(pluralStringResource(R.plurals.things, m.total, Fmt.n(m.total)) + " · " + Fmt.bytes(m.bytes, ru) + " · " +
                if (done) stringResource(R.string.crate_done) else stringResource(R.string.crate_left, Fmt.n(m.pending)), style = T.Label)
            Bar(1f - m.pending.toFloat() / m.total.coerceAtLeast(1), Modifier.padding(top = 5.dp), color = C.green, track = Color(0x1A000000))
        }
        Text(if (done) "✓" else stringResource(R.string.crate_open), style = T.Button.copy(color = if (done) C.green else Color(0xFF3A2810)),
            textAlign = TextAlign.Center, modifier = Modifier.width(70.dp))
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
    Paper(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), pad = 10.dp, onClick = { openInGallery(ctx, it) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Thumb(it, Modifier.size(64.dp, 80.dp), px = 200)
            Column(Modifier.padding(start = 12.dp)) {
                Text(stringResource(R.string.otd_t).uppercase(), style = T.Caps.copy(color = C.brassD))
                Text(pluralStringResource(R.plurals.years_ago, years, years), style = T.Heading)
                Text(stringResource(if (it.verdict == app.curator.data.Verdict.EXHIBIT) R.string.otd_exhibit else R.string.otd_b), style = T.Small)
            }
        }
    }
    Box(Modifier.height(0.dp))
}
