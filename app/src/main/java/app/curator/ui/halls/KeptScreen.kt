package app.curator.ui.halls

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Item
import app.curator.data.Source
import app.curator.data.Verdict
import app.curator.domain.Dates
import app.curator.locale
import app.curator.ru
import app.curator.ui.Nav
import app.curator.ui.Overlay
import app.curator.ui.components.Btn
import app.curator.ui.components.Card
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Scrim
import app.curator.ui.components.Thumb
import app.curator.ui.components.TopBar
import app.curator.ui.openInGallery
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import app.curator.work.Frame

/**
 * What was kept, of one [month] or (0) of all (#5). Before, a crate gone through opened straight into a second look,
 * so the same photos came round again and seemed doubled, and what was let stay could be seen nowhere.
 */
@Composable
fun KeptScreen(month: Int, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val xs by remember(month) { c.items.kept(month) }.collectAsState(emptyList())
    var look by remember { mutableStateOf<Item?>(null) }
    var captioning by remember { mutableStateOf<Item?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (month == 0) stringResource(R.string.kept_t) else Dates.monthName(month, ctx.locale),
            pluralStringResource(R.plurals.things, xs.size, Fmt.n(xs.size)), nav::close)
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), pad = 12.dp) {
                    Text(stringResource(if (month == 0) R.string.kept_b else R.string.kept_month_b), style = T.Small)
                    if (month != 0 && xs.any { it.verdict == Verdict.KEEP }) {
                        Text(stringResource(R.string.review_b), style = T.Small, modifier = Modifier.padding(top = 8.dp))
                        Btn(stringResource(R.string.review_go), { nav.open(Overlay.Crate(Source.Review(month, System.currentTimeMillis()))) },
                            Modifier.padding(top = 8.dp), Kind.GHOST)
                    }
                }
            }
            if (xs.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(R.string.kept_empty), style = T.Italic.copy(color = C.muted), textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
            }
            items(xs, key = { it.id }) { x ->
                Box(Modifier.aspectRatio(.8f).clickable { look = x }) {
                    Thumb(x, Modifier.fillMaxSize(), px = 300)
                    if (x.verdict == Verdict.EXHIBIT) Text("✦", style = T.Label.copy(color = C.accent), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                }
            }
        }
    }

    look?.let { x ->
        Scrim({ look = null }) {
            Card(Modifier.fillMaxWidth(), pad = 12.dp) {
                Thumb(x, Modifier.fillMaxWidth().aspectRatio(.8f), px = 1080, fit = true)
                if (x.caption.isNotBlank()) Text(x.caption, style = T.Italic, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(Fmt.bytes(x.size, ru) + (if (x.bucket.isNotBlank()) " · " + x.bucket else ""), style = T.Label, modifier = Modifier.weight(1f))
                    Text(stringResource(if (x.caption.isBlank()) R.string.caption_add else R.string.caption_edit), style = T.Label.copy(color = C.accent),
                        modifier = Modifier.clickable { captioning = x }.padding(start = 8.dp))
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (x.verdict == Verdict.EXHIBIT) Btn(stringResource(R.string.unexhibit), { c.write { decide(x, Verdict.KEEP) }; look = null }, Modifier.weight(1f), Kind.GHOST)
                    else Btn(stringResource(R.string.a_ex), { c.write { decide(x, Verdict.EXHIBIT) }; look = null }, Modifier.weight(1f), Kind.ACCENT)
                    Btn(stringResource(R.string.a_del), { c.write { decide(x, Verdict.ACT) }; look = null }, Modifier.weight(1f), Kind.RED)
                }
                if (!x.fake) Text(stringResource(R.string.open_gallery), style = T.Label.copy(color = C.accent), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { openInGallery(ctx, x) }.padding(top = 10.dp))
            }
        }
    }

    captioning?.let { x ->
        NameDialog(stringResource(R.string.caption_t), x.caption, { captioning = null }, max = 100, empty = true) { t ->
            captioning = null
            look = x.copy(caption = t)
            c.write { items.caption(x.id, t); Frame.refresh(app) }
        }
    }
}
