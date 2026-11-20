package app.curator.ui.museum

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.data.Note
import app.curator.domain.Dates
import app.curator.domain.Guests
import app.curator.domain.Wings
import app.curator.locale
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.Overlay
import app.curator.ui.components.Card
import app.curator.ui.components.TopBar
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import java.time.format.DateTimeFormatter

/** What the visitors see today, for the count on the museum's front page. */
fun Home.guestView(ru: Boolean) = Guests.View(
    ru, halls.map { it.name to (hallCounts[it.id] ?: 0) }, stats.exhibits, stats.fonds,
    shows.firstOrNull { it.week == Dates.week(now) }?.title, Wings.open(s.freed),
)

/** The guest book on the front page (#6): how many came today and the last line someone left. */
@Composable
fun GuestbookCard(h: Home, nav: Nav) {
    val ru = LocalContext.current.ru
    val today = remember(h.stats, h.halls, h.hallCounts) { Guests.today(h.guestView(ru), Dates.date(h.now).toEpochDay()) }
    val last = h.notes.firstOrNull()
    if (today == 0 && last == null) return
    Card(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp), pad = 16.dp, onClick = { nav.open(Overlay.Guestbook) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.guestbook).uppercase(), style = T.Caps, modifier = Modifier.weight(1f))
            Text(pluralStringResource(R.plurals.visitors_today, today, today), style = T.Label)
        }
        if (last != null) {
            Text(last.text, style = T.Italic, modifier = Modifier.padding(top = 10.dp))
            Text("– " + last.who, style = T.Label, modifier = Modifier.padding(top = 4.dp))
        } else Text(stringResource(R.string.guestbook_empty), style = T.Italic.copy(color = C.muted), modifier = Modifier.padding(top = 10.dp))
    }
}

/** Every line the visitors left, the newest first. */
@Composable
fun GuestbookScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val fmt = DateTimeFormatter.ofPattern(if (ctx.ru) "d MMMM" else "MMMM d", ctx.locale)
    Column(Modifier.fillMaxSize()) {
        TopBar(stringResource(R.string.guestbook), stringResource(R.string.guestbook_sub), nav::close)
        if (h.notes.isEmpty()) Text(stringResource(R.string.guestbook_empty), style = T.Italic.copy(color = C.muted), modifier = Modifier.padding(24.dp))
        LazyColumn(Modifier.padding(horizontal = 16.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).background(C.surface),
            contentPadding = PaddingValues(vertical = 8.dp)) {
            items(h.notes, key = { it.id }) { n -> Entry(n, fmt) }
        }
    }
}

@Composable
private fun Entry(n: Note, fmt: DateTimeFormatter) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(n.text, style = T.Italic)
        Row(Modifier.padding(top = 4.dp)) {
            Text("– " + n.who, style = T.Label, modifier = Modifier.weight(1f))
            Text(Dates.date(n.at).format(fmt), style = T.Label)
        }
        Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp).background(C.line))
    }
}

