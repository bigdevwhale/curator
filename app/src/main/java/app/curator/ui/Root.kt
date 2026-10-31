package app.curator.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.curator.R
import app.curator.container
import app.curator.data.Access
import app.curator.data.Count
import app.curator.data.Event
import app.curator.data.Fund
import app.curator.data.Hall
import app.curator.data.Item
import app.curator.data.MonthRow
import app.curator.data.Settings
import app.curator.data.Show
import app.curator.data.Source
import app.curator.data.Stats
import app.curator.ui.act.ActScreen
import app.curator.ui.components.room
import app.curator.ui.crate.CrateScreen
import app.curator.ui.halls.HallScreen
import app.curator.ui.halls.HallsScreen
import app.curator.ui.museum.ChronicleScreen
import app.curator.ui.museum.CeremonyView
import app.curator.ui.museum.MuseumScreen
import app.curator.ui.onboarding.AccessScreen
import app.curator.ui.onboarding.Onboarding
import app.curator.ui.settings.SettingsPage
import app.curator.ui.theme.C
import app.curator.ui.theme.T

enum class Tab { MUSEUM, HALLS, ACT }

sealed interface Overlay {
    data class Crate(val source: Source) : Overlay
    /** A hall's things; [EXHIBITS] is the main hall with every exhibit. */
    data class Hall(val id: Long) : Overlay
    data object Settings : Overlay
    data object Chronicle : Overlay

    companion object { const val EXHIBITS = -1L }
}

/** Everything the screens read, collected once at the root. */
class Home(
    val s: Settings,
    val months: List<MonthRow>,
    val stats: Stats,
    val funds: Map<Fund, Count>,
    val act: List<Item>,
    val halls: List<Hall>,
    val hallCounts: Map<Long, Int>,
    val hung: List<Item>,
    val exhibits: List<Item>,
    val shows: List<Show>,
    val chronicle: List<Event>,
    val access: Access,
    val syncing: Boolean,
    val now: Long,
)

class Nav(tab: Tab, first: Overlay?) {
    var tab by mutableStateOf(tab)
    val stack = mutableStateListOf<Overlay>().apply { if (first != null) add(first) }
    fun open(o: Overlay) { stack.add(o) }
    fun close() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(o: Overlay) { close(); open(o) }
}

@Composable
fun CuratorRoot(s: Settings, now: Long = System.currentTimeMillis(), tab: Tab = Tab.MUSEUM, overlay: Overlay? = null) {
    val c = LocalContext.current.container
    val access by c.access.collectAsState()
    if (!s.onboarded) { Onboarding(); return }
    if (access == Access.NONE) { AccessScreen(); return }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { c.write { sync() } }

    val months by remember { c.items.months() }.collectAsState(null)
    val stats by remember { c.items.stats() }.collectAsState(null)
    val shots by remember { c.items.shots() }.collectAsState(Count(0, 0))
    val videos by remember { c.items.videos() }.collectAsState(Count(0, 0))
    val chats by remember { c.items.chats() }.collectAsState(Count(0, 0))
    val heavy by remember { c.items.heavy() }.collectAsState(Count(0, 0))
    val act by remember { c.items.act() }.collectAsState(emptyList())
    val halls by remember { c.museum.halls() }.collectAsState(emptyList())
    val counts by remember { c.items.hallCounts() }.collectAsState(emptyList())
    val hung by remember { c.items.hung() }.collectAsState(emptyList())
    val exhibits by remember { c.items.exhibits() }.collectAsState(emptyList())
    val shows by remember { c.museum.shows() }.collectAsState(emptyList())
    val chronicle by remember { c.museum.chronicle() }.collectAsState(emptyList())
    val syncing by c.syncing.collectAsState()
    val ceremony by c.ceremony.collectAsState()
    val nav = remember { Nav(tab, overlay) }

    val m = months ?: return
    val st = stats ?: Stats(0, 0, 0, 0, 0, 0)
    val h = Home(s, m, st, mapOf(Fund.SHOTS to shots, Fund.VIDEO to videos, Fund.CHATS to chats, Fund.HEAVY to heavy), act, halls,
        counts.associate { it.hall to it.n }, hung, exhibits, shows, chronicle, access, syncing, now)

    Box(Modifier.fillMaxSize().room()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (nav.tab) {
                    Tab.MUSEUM -> MuseumScreen(h, nav)
                    Tab.HALLS -> HallsScreen(h, nav)
                    Tab.ACT -> ActScreen(h, nav)
                }
            }
            TabBar(nav, act.size)
        }
        if (nav.stack.isNotEmpty()) BackHandler { nav.close() }
        for (o in nav.stack) Box(Modifier.fillMaxSize().room()) {
            when (o) {
                is Overlay.Crate -> CrateScreen(o.source, h, nav)
                is Overlay.Hall -> HallScreen(o.id, h, nav)
                Overlay.Settings -> SettingsPage(h, nav)
                Overlay.Chronicle -> ChronicleScreen(h, nav)
            }
        }
        ceremony?.let { cer -> CeremonyView(cer, h) { c.ceremony.value = null } }
    }
}

@Composable
private fun TabBar(nav: Nav, act: Int) {
    Column(Modifier.fillMaxWidth().background(C.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(C.line))
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceAround) {
            for ((t, icon, label) in listOf(
                Triple(Tab.MUSEUM, Icons.Outlined.AccountBalance, R.string.tab_museum),
                Triple(Tab.HALLS, Icons.Outlined.Collections, R.string.tab_halls),
                Triple(Tab.ACT, Icons.Outlined.Description, R.string.tab_act),
            )) {
                val on = nav.tab == t
                val col = if (on) C.ink else C.muted
                Column(Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, null) { nav.tab = t; nav.stack.clear() }.padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(if (on) C.surface2 else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp)) {
                            Icon(icon, null, tint = col, modifier = Modifier.size(24.dp))
                        }
                        if (t == Tab.ACT && act > 0) Text(act.toString(), style = T.Caps.copy(color = Color.White, letterSpacing = 0.sp),
                            modifier = Modifier.align(Alignment.TopEnd).background(C.red, RoundedCornerShape(50)).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                    Text(stringResource(label), style = T.Label.copy(color = col, fontSize = 12.sp), modifier = Modifier.padding(top = 2.dp))
                }
            }
        }
    }
}
