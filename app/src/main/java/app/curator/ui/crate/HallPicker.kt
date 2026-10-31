package app.curator.ui.crate

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Hall
import app.curator.ui.components.Btn
import app.curator.ui.components.Fmt
import app.curator.ui.components.Kind
import app.curator.ui.components.Scrim
import app.curator.ui.theme.C
import app.curator.ui.theme.T
import kotlinx.coroutines.launch

/** The museum's own monogram for a hall: its first letter in a brass ring. */
@Composable
fun Monogram(name: String, d: Int = 34) {
    Box(Modifier.size(d.dp).background(C.bg, CircleShape).border(1.5.dp, C.accent, CircleShape), contentAlignment = Alignment.Center) {
        Text(name.trim().take(1).uppercase(), style = T.Heading.copy(color = C.accent))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HallPicker(halls: List<Hall>, counts: Map<Long, Int>, onDismiss: () -> Unit, onPick: (Hall) -> Unit) {
    val c = LocalContext.current.container
    var adding by remember { mutableStateOf(halls.isEmpty()) }
    var name by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun create(n: String) {
        if (n.isBlank()) return
        scope.launch { onPick(c.newHall(n)) }
    }
    Scrim(onDismiss, bottom = true) {
        Column(Modifier.fillMaxWidth().background(C.surface, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).navigationBarsPadding().padding(18.dp)) {
            Text(stringResource(R.string.which_hall), style = T.Title)
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                for (h in halls) Row(Modifier.fillMaxWidth().clickable { onPick(h) }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Monogram(h.name)
                    Text(h.name, style = T.Heading, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Text(Fmt.n(counts[h.id] ?: 0), style = T.Label)
                }
            }
            if (!adding) Row(Modifier.fillMaxWidth().clickable { adding = true }.padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).border(1.5.dp, C.line, CircleShape), contentAlignment = Alignment.Center) { Text("+", style = T.Heading.copy(color = C.muted)) }
                Text(stringResource(R.string.new_hall), style = T.Heading.copy(color = C.muted), modifier = Modifier.padding(start = 12.dp))
            } else {
                Text(stringResource(R.string.new_hall_t).uppercase(), style = T.Caps.copy(color = C.accent), modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
                BasicTextField(name, { name = it.take(40) }, singleLine = true, textStyle = T.Heading, cursorBrush = SolidColor(C.ink),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { create(name) }),
                    modifier = Modifier.fillMaxWidth().border(1.dp, C.line, RoundedCornerShape(3.dp)).padding(10.dp),
                    decorationBox = { inner -> if (name.isEmpty()) Text(stringResource(R.string.new_hall_hint), style = T.Heading.copy(color = C.line)); inner() })
                val have = halls.map { it.name.lowercase() }.toSet()
                FlowRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (s in stringArrayResource(R.array.hall_ideas).filter { it.lowercase() !in have }) {
                        Text(s, style = T.Label.copy(color = C.ink), modifier = Modifier.border(1.dp, C.line, RoundedCornerShape(14.dp)).clickable { create(s) }.padding(horizontal = 10.dp, vertical = 5.dp))
                    }
                }
                Btn(stringResource(R.string.open_hall), { create(name) }, Modifier.fillMaxWidth().padding(top = 12.dp), Kind.GREEN, enabled = name.isNotBlank())
            }
            Btn(stringResource(R.string.cancel), onDismiss, Modifier.fillMaxWidth().padding(top = 8.dp), Kind.GHOST)
        }
    }
}
