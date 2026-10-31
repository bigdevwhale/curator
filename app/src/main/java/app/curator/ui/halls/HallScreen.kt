package app.curator.ui.halls

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Item
import app.curator.data.Media
import app.curator.data.Verdict
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.Overlay
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Card
import app.curator.ui.components.Scrim
import app.curator.ui.components.Thumb
import app.curator.ui.components.TopBar
import app.curator.ui.openInGallery
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun HallScreen(id: Long, h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val main = id == Overlay.EXHIBITS
    val hall = h.halls.firstOrNull { it.id == id }
    val inHall by remember(id) { if (main) flowOf(emptyList()) else c.items.inHall(id) }.collectAsState(emptyList())
    val xs = if (main) h.exhibits else inHall
    var look by remember { mutableStateOf<Item?>(null) }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf<String?>(null) }
    if (!main && hall == null) { androidx.compose.runtime.LaunchedEffect(Unit) { nav.close() }; return }

    val moveAsk = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        if (r.resultCode == Activity.RESULT_OK && hall != null) scope.launch {
            val n = c.moveHall(hall)
            note = ctx.resources.getQuantityString(R.plurals.moved, n, n, Media.folderName(hall.name))
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopBar(if (main) stringResource(R.string.main_hall) else hall!!.name,
            if (main) pluralStringResource(R.plurals.exhibits, xs.size, Fmt.n(xs.size)) else pluralStringResource(R.plurals.things, xs.size, Fmt.n(xs.size)), nav::close) {
            if (!main) Text("⋯", style = T.Title.copy(color = C.accent), modifier = Modifier.clickable { menu = true }.padding(horizontal = 12.dp))
        }
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            if (!main) item(span = { GridItemSpan(maxLineSpan) }) {
                Card(Modifier.fillMaxWidth().padding(bottom = 6.dp), pad = 12.dp) {
                    if (hall!!.folder != null) {
                        Text(stringResource(R.string.folder_set, "Pictures/" + hall.folder), style = T.Small)
                    } else {
                        Text(stringResource(R.string.folder_t), style = T.Strong)
                        Text(stringResource(R.string.folder_b, "Pictures/" + Media.folderName(hall.name)), style = T.Small, modifier = Modifier.padding(top = 2.dp))
                    }
                    if (xs.any { !it.fake } && xs.isNotEmpty()) Btn(stringResource(if (hall.folder != null) R.string.folder_again else R.string.folder_go), {
                        val real = xs.filter { !it.fake }
                        runCatching { moveAsk.launch(IntentSenderRequest.Builder(Media.writeRequest(ctx, real)).build()) }
                    }, Modifier.padding(top = 8.dp), Kind.GHOST)
                }
            }
            if (xs.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
                Text(stringResource(if (main) R.string.main_empty else R.string.hall_empty), style = T.Italic.copy(color = C.muted), textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp))
            }
            items(xs, key = { it.id }) { x ->
                Box(Modifier.aspectRatio(.8f).clickable { look = x }) {
                    Thumb(x, Modifier.fillMaxSize(), px = 300)
                    if (!main && x.verdict == Verdict.EXHIBIT) Text("✦", style = T.Label.copy(color = C.accent), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp))
                }
            }
        }
    }

    note?.let { n ->
        Scrim({ note = null }) { Card { Text(n, style = T.Body); Btn(stringResource(R.string.ok), { note = null }, Modifier.fillMaxWidth().padding(top = 12.dp)) } }
    }

    if (menu && hall != null) Scrim({ menu = false }) {
        Card(Modifier.fillMaxWidth()) {
            Text(hall.name, style = T.Title)
            Btn(stringResource(R.string.rename), { menu = false; renaming = true }, Modifier.fillMaxWidth().padding(top = 12.dp), Kind.GHOST)
            Btn(stringResource(R.string.close_hall), { menu = false; nav.close(); c.write { deleteHall(hall) } }, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.RED)
            Text(stringResource(R.string.close_hall_hint), style = T.Small, modifier = Modifier.padding(top = 8.dp))
        }
    }

    if (renaming && hall != null) NameDialog(stringResource(R.string.rename), hall.name, { renaming = false }) { n ->
        renaming = false
        c.write { museum.updateHall(hall.copy(name = n)) }
    }

    look?.let { x ->
        Scrim({ look = null }) {
            Card(Modifier.fillMaxWidth(), pad = 12.dp) {
                Thumb(x, Modifier.fillMaxWidth().aspectRatio(.8f), px = 1080, fit = true)
                Text(Fmt.bytes(x.size, ru) + (if (x.bucket.isNotBlank()) " · " + x.bucket else ""), style = T.Label, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (x.verdict == Verdict.EXHIBIT) Btn(stringResource(R.string.unexhibit), { c.write { decide(x, Verdict.KEEP) }; look = null }, Modifier.weight(1f), Kind.GHOST)
                    else Btn(stringResource(R.string.a_ex), { c.write { decide(x, Verdict.EXHIBIT) }; look = null }, Modifier.weight(1f), Kind.ACCENT)
                    Btn(stringResource(R.string.a_del), { c.write { decide(x, Verdict.ACT) }; look = null }, Modifier.weight(1f), Kind.RED)
                }
                if (!main) Btn(stringResource(R.string.out_of_hall), { c.write { decide(x, x.verdict, null) }; look = null }, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.GHOST)
                if (!x.fake) Text(stringResource(R.string.open_gallery), style = T.Label.copy(color = C.accent), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { openInGallery(ctx, x) }.padding(top = 10.dp))
            }
        }
    }
}
