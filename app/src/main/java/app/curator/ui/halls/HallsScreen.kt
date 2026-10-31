package app.curator.ui.halls

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Item
import app.curator.data.Show
import app.curator.domain.Dates
import app.curator.domain.Titles
import app.curator.domain.Wings
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.Overlay
import app.curator.ui.components.Btn
import app.curator.ui.components.Caps
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Paper
import app.curator.ui.components.Scrim
import app.curator.ui.components.Thumb
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.launch

@Composable
fun HallsScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val week = Dates.week(h.now)
    val byWeek = h.exhibits.groupBy { Dates.week(it.decided) }
    val title = h.shows.firstOrNull { it.week == week }?.title ?: Titles.default(week, ru)
    var renaming by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val rotunda = Wings.open(h.s.freed) >= 4

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(top = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.halls_caps).uppercase(), style = T.Caps)
                Text(stringResource(R.string.halls_title), style = T.Hero.copy(fontSize = T.Title.fontSize * 1.15f))
            }
            val now = byWeek[week].orEmpty().take(3)
            Poster(stringResource(if (rotunda) R.string.show_in_rotunda else R.string.show_in_hall), title, now, Modifier.padding(16.dp),
                onTitle = { renaming = true }, onShare = if (now.isNotEmpty()) ({ sharePoster(ctx, title, now) }) else null)
            val past = h.shows.filter { it.week < week && byWeek[it.week].orEmpty().isNotEmpty() }
            if (past.isNotEmpty()) {
                Caps(stringResource(R.string.past_shows))
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(past, key = { it.week }) { s -> PastShow(s, byWeek[s.week].orEmpty()) }
                }
            }
            Caps(stringResource(R.string.plan), right = pluralStringResource(R.plurals.halls, h.halls.size, h.halls.size))
            Column(Modifier.padding(horizontal = 16.dp).shadow(10.dp).background(C.paper).border(3.dp, C.ink).padding(8.dp)) {
                Room(stringResource(R.string.main_hall), pluralStringResource(R.plurals.exhibits, h.stats.exhibits, Fmt.n(h.stats.exhibits)), h.exhibits.take(4), Modifier.fillMaxWidth()) {
                    nav.open(Overlay.Hall(Overlay.EXHIBITS))
                }
                for (row in (h.halls.map { it as Any } + "+").chunked(2)) Row(Modifier.fillMaxWidth()) {
                    for (cell in row) {
                        if (cell is app.curator.data.Hall) {
                            val n = h.hallCounts[cell.id] ?: 0
                            Room(cell.name, pluralStringResource(R.plurals.things, n, Fmt.n(n)), h.hung.filter { it.hall == cell.id }.take(3), Modifier.weight(1f)) {
                                nav.open(Overlay.Hall(cell.id))
                            }
                        } else Box(Modifier.weight(1f).heightIn(min = 104.dp).border(1.dp, C.ink.copy(alpha = .5f)).clickable { adding = true }, contentAlignment = Alignment.Center) {
                            Text("+ " + stringResource(R.string.new_hall), style = T.Label)
                        }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }
            Text(stringResource(R.string.halls_hint), style = T.Small.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
        }
    }

    if (renaming) NameDialog(stringResource(R.string.show_rename), title.trim('«', '»', '“', '”'), { renaming = false }) { n ->
        renaming = false
        c.write { museum.putShow(Show(week, if (ru) "«$n»" else "“$n”")) }
    }
    if (adding) NameDialog(stringResource(R.string.new_hall_t), "", { adding = false }) { n ->
        adding = false
        scope.launch { val hall = c.newHall(n); nav.open(Overlay.Hall(hall.id)) }
    }
}

@Composable
fun Poster(where: String, title: String, xs: List<Item>, modifier: Modifier = Modifier, onTitle: (() -> Unit)? = null, onShare: (() -> Unit)? = null) {
    Column(
        modifier.fillMaxWidth().shadow(10.dp).background(Brush.verticalGradient(listOf(Color(0xFF2A1F1A), Color(0xFF151010)))).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(where.uppercase(), style = T.Caps)
        Text(title, style = T.Italic.copy(color = C.paper, fontSize = T.Title.fontSize), textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp).then(if (onTitle != null) Modifier.clickable(onClick = onTitle) else Modifier))
        if (xs.isEmpty()) Text(stringResource(R.string.show_empty), style = T.Small.copy(color = C.onWall), textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
        else Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { for (x in xs) Frame(x, 88.dp) }
        if (onShare != null || onTitle != null) Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (onTitle != null) Text(stringResource(R.string.show_rename).uppercase(), style = T.Caps.copy(color = C.onWall), modifier = Modifier.clickable(onClick = onTitle))
            if (onShare != null) Text(stringResource(R.string.show_share).uppercase(), style = T.Caps.copy(color = C.gold), modifier = Modifier.clickable(onClick = onShare))
        }
    }
}

/** A gilt frame around a thing. */
@Composable
fun Frame(x: Item, w: Dp) {
    Box(Modifier.width(w).height(w * 1.25f).shadow(6.dp).background(Color(0xFFB08B4F)).padding(5.dp).border(1.5.dp, Color(0xFF6B5228))) {
        Thumb(x, Modifier.fillMaxWidth().height(w * 1.25f), px = 300, badge = false)
    }
}

@Composable
private fun PastShow(s: Show, xs: List<Item>) {
    Column(Modifier.width(170.dp).background(Color(0xFF1C1512)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(s.title, style = T.Italic.copy(color = C.paper, fontSize = T.Italic.fontSize * .85f), maxLines = 1, textAlign = TextAlign.Center)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) { for (x in xs.take(3)) Frame(x, 42.dp) }
    }
}

@Composable
private fun Room(name: String, sub: String, xs: List<Item>, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.heightIn(min = 104.dp).border(1.dp, C.ink.copy(alpha = .7f)).clickable(onClick = onClick).padding(10.dp)) {
        Text(name, style = T.Heading, maxLines = 1)
        Text(sub, style = T.Label)
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (x in xs) Box(Modifier.size(34.dp, 42.dp).border(2.dp, C.brassD)) { Thumb(x, Modifier.size(34.dp, 42.dp), px = 120, badge = false) }
        }
    }
}

@Composable
fun NameDialog(title: String, initial: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by remember { mutableStateOf(initial) }
    Scrim(onDismiss) {
        Paper(Modifier.fillMaxWidth()) {
            Text(title, style = T.Title)
            BasicTextField(name, { name = it.take(48) }, singleLine = true, textStyle = T.Heading, cursorBrush = SolidColor(C.ink),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).border(1.dp, C.paperLine, RoundedCornerShape(3.dp)).padding(10.dp))
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Btn(stringResource(R.string.cancel), onDismiss, Modifier.weight(1f), Kind.GHOST)
                Btn(stringResource(R.string.save), { if (name.isNotBlank()) onDone(name.trim()) }, Modifier.weight(1f), Kind.GREEN, enabled = name.isNotBlank())
            }
        }
    }
}
