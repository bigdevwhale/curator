package app.curator.ui.act

import androidx.compose.ui.graphics.graphicsLayer
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.EventKind
import app.curator.data.Item
import app.curator.data.Media
import app.curator.data.Verdict
import app.curator.ru
import app.curator.ui.Home
import app.curator.ui.Nav
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Card
import app.curator.ui.components.Scrim
import app.curator.ui.components.Stamp
import app.curator.ui.components.Thumb
import app.curator.ui.openInGallery
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ActScreen(h: Home, nav: Nav) {
    val ctx = LocalContext.current
    val c = ctx.container
    val ru = ctx.ru
    val scope = rememberCoroutineScope()
    val act = h.act
    val bytes = act.sumOf { it.size }
    val no = h.chronicle.count { it.kind == EventKind.ACT } + 1
    var signing by remember { mutableStateOf<List<Item>>(emptyList()) }
    var signedName by remember { mutableStateOf(false) }
    val stamp = remember { Animatable(0f) }
    var look by remember { mutableStateOf<Item?>(null) }

    fun finish() {
        signedName = true
        scope.launch {
            stamp.snapTo(0f)
            stamp.animateTo(1f, spring(dampingRatio = .45f, stiffness = 300f))
            delay(900)
            c.write { settleSigning() }
            delay(300)
            stamp.snapTo(0f)
            signedName = false
        }
    }

    val ask = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        // Counted by what the gallery really lost, so a dialog that answers oddly costs nothing (#2).
        if (r.resultCode == Activity.RESULT_OK) finish() else { signing = emptyList(); c.write { settleSigning() } }
    }

    fun sign() {
        signing = act.toList()
        val all = act.toList()
        val real = all.filter { !it.fake }
        scope.launch {
            c.beginSigning(all)
            if (real.isEmpty()) finish()
            else runCatching { ask.launch(IntentSenderRequest.Builder(Media.deleteRequest(ctx, real)).build()) }
        }
    }

    LazyVerticalGrid(GridCells.Fixed(4), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), modifier = Modifier.fillMaxSize()) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 4.dp, top = 18.dp)) {
                Text(stringResource(R.string.act_form).uppercase(), style = T.Caps)
                Text(stringResource(R.string.act_title), style = T.Hero)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 12.dp).background(C.surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)).padding(start = 16.dp, end = 16.dp, top = 16.dp)) {
                Text(stringResource(R.string.act_no, no), style = T.Title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(if (act.isEmpty()) stringResource(R.string.act_empty_sub) else pluralStringResource(R.plurals.things, act.size, Fmt.n(act.size)) + " · " + Fmt.bytes(bytes, ru),
                    style = T.Label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(stringResource(if (act.isEmpty()) R.string.act_empty else R.string.act_hint), style = T.Small, modifier = Modifier.padding(top = 10.dp, bottom = 10.dp),
                    textAlign = if (act.isEmpty()) TextAlign.Center else TextAlign.Start)
            }
        }
        // The paper runs under the whole last row, so empty cells are padded with blanks.
        val cells: List<Item?> = act + List((4 - act.size % 4) % 4) { null }
        items(cells.size, key = { cells[it]?.id ?: -1L - it }) { k ->
            val x = cells[k]
            val i = k % 4
            Box(Modifier.background(C.surface).padding(3.dp)) {
                Box(Modifier.aspectRatio(.8f).then(if (x != null) Modifier.clickable { look = x } else Modifier)) {
                    if (x != null) {
                        Thumb(x, Modifier.fillMaxSize(), px = 240, shape = RoundedCornerShape(10.dp))
                        val days = ((h.now - x.decided) / 86_400_000L).toInt().coerceAtLeast(0)
                        if (days > 0) Text(pluralStringResource(R.plurals.days, days, days), style = T.Caps.copy(color = Color.White, letterSpacing = T.Caps.letterSpacing * 0f, fontSize = T.Caps.fontSize * .85f),
                            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp).background(Color(0xAA000000)).padding(horizontal = 3.dp))
                    }
                }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.background(C.surface, RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)).padding(16.dp)) {
                if (act.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(stringResource(R.string.curator_sign), style = T.Label, modifier = Modifier.weight(1f))
                        Box(Modifier.width(140.dp).height(30.dp).drawBehind { drawLine(C.ink, Offset(0f, size.height), Offset(size.width, size.height), 2f) }, contentAlignment = Alignment.BottomCenter) {
                            if (signedName) Text(stringResource(R.string.curator_name), style = T.Italic.copy(color = Color(0xFF1F3C8A)))
                        }
                    }
                    Btn(stringResource(R.string.sign_free, Fmt.bytes(bytes, ru)), ::sign, Modifier.fillMaxWidth().padding(top = 16.dp), Kind.RED, enabled = signing.isEmpty() || !signedName)
                    Text(stringResource(R.string.sign_hint), style = T.Small, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                }
            }
        }
    }

    if (stamp.value > 0f) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Stamp(stringResource(R.string.act_signed), C.red, Modifier.graphicsLayer { val k = 2.4f - 1.4f * stamp.value; scaleX = k; scaleY = k }, alpha = stamp.value.coerceIn(0f, 1f))
    }

    look?.let { x ->
        Scrim({ look = null }) {
            Card(Modifier.fillMaxWidth(), pad = 12.dp) {
                Thumb(x, Modifier.fillMaxWidth().aspectRatio(.8f), px = 1080, fit = true)
                Text(Fmt.bytes(x.size, ru) + (if (x.bucket.isNotBlank()) " · " + x.bucket else ""), style = T.Label, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Btn(stringResource(R.string.return_fonds), { c.write { decide(x, Verdict.KEEP) }; look = null }, Modifier.weight(1f), Kind.GREEN)
                    Btn(stringResource(R.string.leave_in_act), { look = null }, Modifier.weight(1f), Kind.GHOST)
                }
                if (!x.fake) Text(stringResource(R.string.open_gallery), style = T.Label.copy(color = C.accent), textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable { openInGallery(ctx, x) }.padding(top = 10.dp))
            }
        }
    }
}
