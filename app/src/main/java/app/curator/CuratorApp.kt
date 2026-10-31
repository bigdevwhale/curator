package app.curator

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import app.curator.art.Fake
import app.curator.data.Access
import app.curator.data.DemoSeed
import app.curator.data.Event
import app.curator.data.EventKind
import app.curator.data.Fund
import app.curator.data.Hall
import app.curator.data.Item
import app.curator.data.Media
import app.curator.data.MuseumDb
import app.curator.data.SettingsStore
import app.curator.data.Show
import app.curator.data.Source
import app.curator.data.Verdict
import app.curator.domain.Dates
import app.curator.domain.Expert
import app.curator.domain.Ranks
import app.curator.domain.Rules
import app.curator.domain.Titles
import app.curator.domain.Wings
import app.curator.work.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale

/** A crate opened on the table: what the expert rejected and the cards to go through. */
data class Crate(
    val no: Int,
    val source: Source,
    val rejects: List<Pair<Item, Int>>,
    val cards: List<List<Item>>,
    val inv: Map<Long, String>,
)

/** A moment that deserves the whole screen. */
sealed interface Ceremony {
    data class Wing(val index: Int) : Ceremony
    data class Rank(val index: Int) : Ceremony
}

class AppContainer(context: Context) {
    val app: Context = context.applicationContext
    val db = MuseumDb.create(app)
    val items = db.items()
    val museum = db.museum()
    val settings = SettingsStore(app)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    /** Demo museum: nothing is read from the real gallery. Tests switch it on too. */
    var demo = BuildConfig.DEMO

    val ready = MutableStateFlow(false)
    val access = MutableStateFlow(if (demo) Access.FULL else Media.access(app))
    val syncing = MutableStateFlow(false)
    val ceremony = MutableStateFlow<Ceremony?>(null)
    private val syncLock = Mutex()

    fun write(block: suspend AppContainer.() -> Unit) {
        scope.launch { block() }
    }

    /** Brings the museum in line with the gallery: new things arrive in their crates, things deleted elsewhere leave. */
    suspend fun sync() {
        if (demo) return
        val a = Media.access(app)
        access.value = a
        if (a == Access.NONE) return
        syncLock.withLock {
            syncing.value = true
            try {
                val found = withContext(Dispatchers.IO) { Media.scan(app) }
                val brief = items.brief().associateBy { it.id }
                val fresh = found.filter { it.id !in brief }
                if (fresh.isNotEmpty()) fresh.chunked(500).forEach { items.insertAll(it) }
                // An empty answer is more likely a hiccup than an empty gallery: never wipe the museum on it.
                if (found.isNotEmpty() && a == Access.FULL) {
                    val seen = found.mapTo(HashSet()) { it.id }
                    val gone = brief.keys.filter { it !in seen }
                    gone.chunked(500).forEach { items.delete(it) }
                }
                for (f in found) {
                    val k = brief[f.id] ?: continue
                    if (k.path != f.path || k.bucket != f.bucket || k.size != f.size) items.moved(f.id, f.path, f.bucket, f.size)
                }
            } finally {
                syncing.value = false
            }
        }
    }

    /** A picture of the thing [px] wide at most: from the gallery, or drawn for a demo item. */
    fun picture(it: Item, px: Int): Bitmap? = if (it.fake) Fake.draw(it, px) else Media.thumb(app, it, px)

    private suspend fun analyze(xs: List<Item>) = withContext(Dispatchers.IO) {
        for (x in xs) {
            if (x.analyzed || x.video) continue
            val b = picture(x, 160) ?: continue
            val w = 96
            val h = (w * b.height / b.width.coerceAtLeast(1)).coerceIn(48, 192)
            val s = Bitmap.createScaledBitmap(b, w, h, true)
            val px = IntArray(w * h)
            s.getPixels(px, 0, w, 0, 0, w, h)
            val gray = FloatArray(px.size) { i ->
                val c = px[i]
                .299f * (c shr 16 and 255) + .587f * (c shr 8 and 255) + .114f * (c and 255)
            }
            val m = Expert.measure(gray, w, h)
            items.analyzed(x.id, m.sharp, m.bright, m.hash)
        }
    }

    /** Opens the next crate of [source]: up to the crate size, plus the rest of a series cut in half. */
    suspend fun crate(source: Source, ru: Boolean): Crate {
        val s = settings.now()
        val n = s.crate
        val raw = when (source) {
            is Source.Month -> items.pendingMonth(source.month, n + 30)
            is Source.Special -> when (source.fund) {
                Fund.SHOTS -> items.pendingShots(n + 30)
                Fund.VIDEO -> items.pendingVideos(n)
                Fund.CHATS -> items.pendingChats(n + 30)
                Fund.HEAVY -> items.pendingHeavy(n)
            }
        }
        val window = raw.take(n).toMutableList()
        for (x in raw.drop(n)) {
            if (x.verdict == Verdict.LATER || x.taken - window.last().taken > Expert.SERIES_GAP) break
            window += x
        }
        analyze(window)
        val fresh = window.mapNotNull { items.get(it.id) }
        val rules = Rules(s.flagShots, s.flagBlur, s.flagDark, s.flagDups, shotCrate = source == Source.Special(Fund.SHOTS))
        val sorted = Expert.sort(fresh, rules)
        val inv = fresh.associate { it.id to Dates.inventory(it.month, items.before(it.month, it.taken), ru) }
        return Crate(s.crateNo, source, sorted.rejects, sorted.cards, inv)
    }

    suspend fun decide(x: Item, v: Verdict, hall: Long? = x.hall) {
        val now = System.currentTimeMillis()
        items.decide(x.id, v, hall, now)
        if (v == Verdict.EXHIBIT) {
            val w = Dates.week(now)
            if (museum.show(w) == null) museum.putShow(Show(w, Titles.default(w, app.ru)))
        }
    }

    /** Puts a thing back as it was before the last decision. */
    suspend fun restore(x: Item) = items.decide(x.id, x.verdict, x.hall, x.decided)

    suspend fun closeCrate(no: Int, n: Int, label: String) {
        museum.log(Event(at = System.currentTimeMillis(), kind = EventKind.CRATE, n = n, text = label))
        settings.update { if (it.crateNo == no) it.copy(crateNo = no + 1) else it }
        checkRank()
    }

    private suspend fun described(): Int {
        val ids = items.count() - items.pendingCount()
        return ids + settings.now().writtenOff
    }

    private suspend fun checkRank() {
        val r = Ranks.of(described())
        val s = settings.now()
        if (r > s.rank) {
            settings.update { it.copy(rank = r) }
            museum.log(Event(at = System.currentTimeMillis(), kind = EventKind.RANK, n = r))
            ceremony.value = Ceremony.Rank(r)
        }
    }

    /** The act is signed and the gallery let go of the things: count the freed space, and maybe open a wing. */
    suspend fun signed(xs: List<Item>) {
        if (xs.isEmpty()) return
        val bytes = xs.sumOf { it.size }
        items.delete(xs.map { it.id })
        settings.update { it.copy(freed = it.freed + bytes, writtenOff = it.writtenOff + xs.size) }
        val now = System.currentTimeMillis()
        museum.log(Event(at = now, kind = EventKind.ACT, n = xs.size, bytes = bytes))
        val s = settings.now()
        val open = Wings.open(s.freed)
        if (open > s.wings) {
            settings.update { it.copy(wings = open) }
            for (i in s.wings until open) museum.log(Event(at = now + i, kind = EventKind.WING, n = i))
            ceremony.value = Ceremony.Wing(open - 1)
        } else checkRank()
    }

    suspend fun newHall(name: String): Hall {
        val h = Hall(name = name.trim(), created = System.currentTimeMillis())
        val id = museum.addHall(h)
        museum.log(Event(at = h.created, kind = EventKind.HALL, text = h.name))
        return h.copy(id = id)
    }

    suspend fun deleteHall(h: Hall) {
        items.clearHall(h.id)
        museum.deleteHall(h.id)
    }

    /** After the system let the museum move them: the hall's things go to Pictures/<name>/. */
    suspend fun moveHall(h: Hall): Int = withContext(Dispatchers.IO) {
        val folder = Media.folderName(h.name)
        val xs = items.hallItems(h.id).filter { !it.fake }
        val n = Media.move(app, xs, folder)
        museum.updateHall(h.copy(folder = folder))
        sync()
        n
    }

    suspend fun found() {
        val s = settings.now()
        if (s.founded == 0L) {
            val now = System.currentTimeMillis()
            settings.update { it.copy(founded = now) }
            museum.log(Event(at = now, kind = EventKind.FOUNDED))
        }
    }

    fun remind(on: Boolean) {
        write { settings.update { it.copy(remind = on) } }
        Reminder.schedule(app, on)
    }
}

class CuratorApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.scope.launch {
            try {
                if (BuildConfig.DEMO) DemoSeed.seedIfEmpty(this@CuratorApp, container)
            } finally {
                container.ready.value = true
            }
            if (container.settings.now().remind) runCatching { Reminder.schedule(this@CuratorApp, true) }
        }
    }
}

val Context.container: AppContainer get() = (applicationContext as CuratorApp).container

val Context.ru: Boolean get() = resources.configuration.locales[0].language == "ru"

val Context.locale: Locale get() = resources.configuration.locales[0]
