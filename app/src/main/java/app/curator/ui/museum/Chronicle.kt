package app.curator.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.Ceremony
import app.curator.R
import app.curator.data.Event
import app.curator.data.EventKind
import app.curator.domain.Dates
import app.curator.locale
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.art.Facade
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Scrim
import app.curator.ui.components.TopBar
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import java.time.format.DateTimeFormatter

@Composable
fun ChronicleScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern(if (ctx.ru) "d MMM yyyy" else "MMM d, yyyy", ctx.locale)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.chronicle), stringResource(R.string.chronicle_sub), nav::close)
        LazyColumn(Modifier.padding(horizontal = 16.dp).background(C.paper), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(h.chronicle, key = { it.id }) { e ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp)) {
                    Text(Dates.date(e.at).format(fmt), style = T.Label, modifier = Modifier.width(92.dp))
                    Text(line(e), style = T.Body, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun line(e: Event): String {
    val ru = LocalContext.current.ru
    return when (e.kind) {
        EventKind.FOUNDED -> stringResource(R.string.ev_founded)
        EventKind.CRATE -> stringResource(R.string.ev_crate, e.text) + " " + pluralStringResource(R.plurals.ev_described, e.n, e.n)
        EventKind.ACT -> stringResource(R.string.ev_act, pluralStringResource(R.plurals.things, e.n, Fmt.n(e.n)), Fmt.bytes(e.bytes, ru))
        EventKind.WING -> stringArrayResource(R.array.wing_opened).getOrElse(e.n) { "" }
        EventKind.HALL -> stringResource(R.string.ev_hall, e.text)
        EventKind.RANK -> stringResource(R.string.ev_rank, stringArrayResource(R.array.ranks).getOrElse(e.n) { "" })
    }
}

/** A wing opened or a rank given: the whole screen for a moment. */
@Composable
fun CeremonyView(c: Ceremony, h: Home, onDone: () -> Unit) {
    val ru = LocalContext.current.ru
    Scrim(null) {
        Column(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(C.wall, C.wallD))).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (c) {
                is Ceremony.Wing -> {
                    Text(stringResource(R.string.freed_caps, Fmt.bytes(h.s.freed, ru)).uppercase(), style = T.Caps)
                    Text(stringArrayResource(R.array.wing_opened)[c.index].trimEnd('.'), style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                    Facade(c.index + 1, Modifier.fillMaxWidth().padding(vertical = 12.dp), glow = c.index)
                    Text(stringArrayResource(R.array.wing_text)[c.index], style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center)
                    Btn(stringResource(R.string.ribbon), onDone, Modifier.fillMaxWidth().padding(top = 18.dp), Kind.BRASS)
                }
                is Ceremony.Rank -> {
                    Text(stringResource(R.string.rank_caps).uppercase(), style = T.Caps)
                    Text("✦", style = T.Hero.copy(color = C.gold), modifier = Modifier.padding(top = 8.dp))
                    Text(stringArrayResource(R.array.ranks)[c.index], style = T.Title.copy(color = C.paper), textAlign = TextAlign.Center)
                    Text(stringArrayResource(R.array.rank_text)[c.index], style = T.Body.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(top = 10.dp))
                    Btn(stringResource(R.string.accept), onDone, Modifier.fillMaxWidth().padding(top = 18.dp), Kind.BRASS)
                }
            }
        }
    }
}
