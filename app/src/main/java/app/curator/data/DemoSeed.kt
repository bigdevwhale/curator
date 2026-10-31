package app.curator.data

import android.content.Context
import app.curator.AppContainer
import app.curator.domain.Dates
import app.curator.domain.Ranks
import app.curator.domain.Titles
import app.curator.ru
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * `-Pdemo`: a museum a few months old over a made-up gallery drawn in code. Last month's crate is still sealed and
 * has a bit of everything in it: rejects for the expert, a series, a video, a duplicate. The act waiting for a
 * signature is just enough to open the Rotunda.
 */
object DemoSeed {
    private const val MB = 1L shl 20
    private val zone: ZoneId = ZoneId.systemDefault()

    suspend fun seedIfEmpty(ctx: Context, c: AppContainer) {
        if (c.items.count() > 0) return
        val ru = ctx.ru
        val r = Random(7)
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zone)
        var id = 1L
        val out = ArrayList<Item>()

        fun item(at: Long, art: String, size: Long = (2 * MB + r.nextLong(3 * MB)), video: Boolean = false, seed: Int = 1000 + r.nextInt(900_000),
                 hash: Long = r.nextLong(), sharp: Float = 150f + r.nextFloat() * 120, bright: Float = .35f + r.nextFloat() * .3f,
                 chat: Boolean = false): Item {
            val d = Dates.date(at)
            val (path, bucket) = when {
                art == "shot" -> "Pictures/Screenshots/" to "Screenshots"
                chat -> "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/" to "WhatsApp Images"
                else -> "DCIM/Camera/" to "Camera"
            }
            return Item(id = id++, video = video, taken = at, month = d.year * 100 + d.monthValue, md = d.monthValue * 100 + d.dayOfMonth,
                size = size, w = 3000, h = 4000, bucket = bucket, path = path, duration = if (video) 42_000 else 0,
                analyzed = true, sharp = sharp, bright = bright, hash = hash, seed = seed, art = art)
        }

        fun flip(h: Long, bits: Int): Long { var x = h; repeat(bits) { x = x xor (1L shl r.nextInt(64)) }; return x }

        // Last month, still sealed: the showcase crate.
        val m1 = today.minusMonths(1).withDayOfMonth(1)
        var t = m1.atTime(9, 12).atZone(zone).toInstant().toEpochMilli()
        fun step() { t += (40 + r.nextInt(400)) * 60_000L }
        val plan = listOf("land", "sea", "shot", "face", "cat", "SERIES", "blur", "food", "VIDEO", "city", "dark", "sea", "DUP", "cat",
            "shot", "doc", "land", "face", "food", "city", "blur", "sea", "cat", "land", "face", "sea", "food", "land", "city", "cat")
        for (k in plan) {
            when (k) {
                "SERIES" -> {
                    val base = 123_456
                    val h = r.nextLong()
                    for (i in 0 until 4) { out += item(t + i * 6_000L, "face", seed = base * 10 + i, hash = flip(h, 4)) }
                    t += 24_000L
                }
                "VIDEO" -> out += item(t, "land", size = 58 * MB, video = true)
                "DUP" -> { val src = out.last(); out += src.copy(id = id++, taken = src.taken + 2_000, hash = flip(src.hash, 1), sharp = src.sharp - 30) }
                "blur" -> out += item(t, "blur", sharp = 3f)
                "dark" -> out += item(t, "dark", bright = .04f, sharp = 20f)
                "shot" -> out += item(t, "shot", size = 400_000L + r.nextLong(600_000))
                else -> out += item(t, k)
            }
            step()
        }
        for (k in 0 until 34) { out += item(t, listOf("land", "sea", "face", "cat", "food", "city")[r.nextInt(6)], chat = r.nextInt(6) == 0); step() }

        // Older months, partly described.
        val halls = listOf(if (ru) "Семья" else "Family", if (ru) "Путешествия" else "Travels", if (ru) "Кот Бублик" else "Biscuit the cat",
            if (ru) "Застолья" else "Feasts", if (ru) "Город" else "The city")
        val founded = now - 92L * 86_400_000
        val hallIds = halls.mapIndexed { i, n -> c.museum.addHall(Hall(name = n, created = founded + i * 3_600_000L)) }
        val hallOf = mapOf("face" to 0, "sea" to 1, "land" to 1, "cat" to 2, "food" to 3, "city" to 4)
        val older = listOf(0 to (40 to .35f), 2 to (52 to 1f), 3 to (120 to .05f), 7 to (64 to 0f), 19 to (88 to .6f), 31 to (40 to 1f), 55 to (70 to .3f))
        val decidedSoFar = ArrayList<Item>()
        for ((ago, spec) in older) {
            val (n, done) = spec
            val first = today.minusMonths(ago.toLong()).withDayOfMonth(1)
            val last = if (ago == 0) today.dayOfMonth else first.lengthOfMonth()
            val span = (last - 1).coerceAtLeast(1) * 86_400_000L
            val start = first.atTime(8, 0).atZone(zone).toInstant().toEpochMilli()
            val month = (0 until n).map { i ->
                val at = start + span * i / n + r.nextLong(3_600_000)
                val roll = r.nextInt(100)
                when {
                    roll < 8 -> item(at, "shot", size = 500_000L)
                    roll < 12 -> item(at, "blur", sharp = 3f)
                    roll < 15 -> item(at, "land", size = (30 + r.nextLong(90)) * MB, video = true)
                    else -> item(at, listOf("land", "sea", "face", "cat", "food", "city", "doc")[r.nextInt(7)], chat = r.nextInt(7) == 0)
                }
            }
            val k = (n * done).toInt()
            month.forEachIndexed { i, x ->
                if (i < k && x.art != "shot" && x.art != "blur") {
                    val ex = r.nextInt(100) < 9
                    val hall = hallOf[x.art]?.takeIf { r.nextInt(100) < 70 }?.let { hallIds[it] }
                    val d = x.copy(verdict = if (ex) Verdict.EXHIBIT else Verdict.KEEP, hall = hall, decided = founded + r.nextLong(now - founded - 86_400_000L * 8))
                    out += d
                    decidedSoFar += d
                } else if (i < k) {
                    // Rejects of described months were already written off: they are gone from the gallery.
                } else out += x
            }
        }

        // This week's exhibits, for the exhibition.
        val week = Dates.week(now)
        decidedSoFar.filter { it.art in setOf("sea", "face", "cat", "land") }.shuffled(r).take(3).forEachIndexed { i, x ->
            val j = out.indexOfFirst { it.id == x.id }
            out[j] = x.copy(verdict = Verdict.EXHIBIT, decided = now - (i + 1) * 5_400_000L)
        }

        // The act on the table: nine things, two of them heavy videos.
        val actPlan = listOf("shot" to 1 * MB, "blur" to 4 * MB, "land" to 96 * MB, "dark" to 2 * MB, "shot" to 1 * MB, "city" to 124 * MB, "doc" to 2 * MB, "blur" to 4 * MB, "face" to 3 * MB)
        val actStart = today.minusMonths(4).withDayOfMonth(3).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        actPlan.forEachIndexed { i, (art, size) ->
            val x = item(actStart + i * 7_200_000L, art, size = size, video = size > 50 * MB, sharp = if (art == "blur") 3f else 200f)
            out += x.copy(verdict = Verdict.ACT, decided = now - i * 3_600_000L)
        }

        c.items.insertAll(out)

        for (i in 1..3) {
            val w = week - i * 7
            c.museum.putShow(Show(w, Titles.default(w, ru)))
        }
        c.museum.putShow(Show(week, Titles.default(week, ru)))

        val log = c.museum
        log.log(Event(at = founded, kind = EventKind.FOUNDED))
        halls.forEachIndexed { i, n -> log.log(Event(at = founded + i * 3_600_000L, kind = EventKind.HALL, text = n)) }
        val months = listOf(55, 31, 19, 2, 3)
        months.forEachIndexed { i, ago ->
            val m = today.minusMonths(ago.toLong())
            log.log(Event(at = founded + (i + 1) * 9 * 86_400_000L, kind = EventKind.CRATE, n = 24, text = Dates.monthName(m.year * 100 + m.monthValue, ctx.resources.configuration.locales[0])))
        }
        log.log(Event(at = founded + 20 * 86_400_000L, kind = EventKind.ACT, n = 212, bytes = 410 * MB))
        log.log(Event(at = founded + 20 * 86_400_000L + 1, kind = EventKind.WING, n = 1))
        log.log(Event(at = founded + 51 * 86_400_000L, kind = EventKind.ACT, n = 600, bytes = 1890 * MB))
        log.log(Event(at = founded + 51 * 86_400_000L + 1, kind = EventKind.WING, n = 2))

        val described = out.count { !it.pending } + 812
        c.settings.update {
            it.copy(onboarded = true, founded = founded, crateNo = 14, freed = 2300 * MB, writtenOff = 812, wings = 3, rank = Ranks.of(described))
        }
    }
}
