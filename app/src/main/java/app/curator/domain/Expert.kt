package app.curator.domain

import app.curator.data.Item

/** What the expert can find wrong with a thing. Bits, since a dark screenshot is both. */
object Defect {
    const val SHOT = 1
    const val BLUR = 2
    const val DARK = 4
    const val DUP = 8
}

data class Measure(val sharp: Float, val bright: Float, val hash: Long)

/** Which defects the curator asked the expert to look for. [shotCrate]: the crate is the screenshots collection itself. */
data class Rules(
    val shots: Boolean = true,
    val blur: Boolean = true,
    val dark: Boolean = true,
    val dups: Boolean = true,
    val shotCrate: Boolean = false,
)

/** A crate after the expert: the rejects to confirm, and the cards to go through (a card of several is a series of near-identical shots). */
data class Sorted(val rejects: List<Pair<Item, Int>>, val cards: List<List<Item>>)

/**
 * The expert looks at a small grey thumbnail: how sharp (variance of the Laplacian), how bright, and a 64-bit
 * difference hash to tell near-identical shots. Everything happens on the phone.
 */
object Expert {
    /** Gap between shots that can still be one series. */
    const val SERIES_GAP = 90_000L
    private const val SERIES_HAM = 14
    private const val DUP_HAM = 3
    private const val DUP_GAP = 10_000L

    /** [gray] is 0..255, row by row, [w]×[h]. */
    fun measure(gray: FloatArray, w: Int, h: Int): Measure {
        require(gray.size == w * h && w >= 9 && h >= 8)
        var sum = 0.0
        for (v in gray) sum += v
        val bright = (sum / gray.size / 255.0).toFloat()

        var n = 0
        var m = 0.0
        var s2 = 0.0
        for (y in 1 until h - 1) for (x in 1 until w - 1) {
            val i = y * w + x
            val l = (gray[i - w] + gray[i + w] + gray[i - 1] + gray[i + 1] - 4 * gray[i]).toDouble()
            n++
            val d = l - m
            m += d / n
            s2 += d * (l - m)
        }
        val sharp = if (n > 1) (s2 / (n - 1)).toFloat() else 0f
        return Measure(sharp, bright, dhash(gray, w, h))
    }

    /** 9×8 cell averages; each bit says whether a cell is darker than its right neighbour. */
    fun dhash(gray: FloatArray, w: Int, h: Int): Long {
        val cells = FloatArray(72)
        for (r in 0 until 8) for (c in 0 until 9) {
            val x0 = c * w / 9
            val x1 = maxOf(x0 + 1, (c + 1) * w / 9)
            val y0 = r * h / 8
            val y1 = maxOf(y0 + 1, (r + 1) * h / 8)
            var s = 0f
            for (y in y0 until y1) for (x in x0 until x1) s += gray[y * w + x]
            cells[r * 9 + c] = s / ((x1 - x0) * (y1 - y0))
        }
        var bits = 0L
        for (r in 0 until 8) for (c in 0 until 8) {
            if (cells[r * 9 + c] < cells[r * 9 + c + 1]) bits = bits or (1L shl (r * 8 + c))
        }
        return bits
    }

    fun ham(a: Long, b: Long) = java.lang.Long.bitCount(a xor b)

    fun isShot(it: Item): Boolean {
        val p = (it.path + "/" + it.bucket).lowercase()
        return "screenshot" in p || "скриншот" in p || "screen_shot" in p
    }

    /** The defects of one thing on its own (duplicates need its neighbours). */
    fun flags(it: Item, rules: Rules, median: Float): Int {
        if (it.video) return 0
        var f = 0
        val shot = isShot(it)
        if (shot && rules.shots && !rules.shotCrate) f = f or Defect.SHOT
        if (it.analyzed && rules.dark && it.bright in 0f..0.08f) f = f or Defect.DARK
        if (it.analyzed && rules.blur && !shot && it.sharp >= 0 && (it.sharp < 8f || (median > 0 && it.sharp < median * .2f && it.sharp < 40f))) f = f or Defect.BLUR
        return f
    }

    fun sort(xs: List<Item>, rules: Rules): Sorted {
        val sharps = xs.filter { it.analyzed && !it.video && !isShot(it) }.map { it.sharp }.sorted()
        val median = if (sharps.isEmpty()) 0f else sharps[sharps.size / 2]
        val flags = HashMap<Long, Int>()
        for (x in xs) flags[x.id] = flags(x, rules, median)

        // Series: neighbours in time that look alike. Within a series an exact repeat is a duplicate.
        val cluster = HashMap<Long, Int>()
        val members = ArrayList<MutableList<Item>>()
        var cur: MutableList<Item>? = null
        for (x in xs.filter { !it.video && flags[it.id] == 0 }.sortedBy { it.taken }) {
            val prev = cur?.lastOrNull()
            val joins = prev != null && x.taken - prev.taken <= SERIES_GAP && x.hash != 0L && prev.hash != 0L && ham(x.hash, prev.hash) <= SERIES_HAM
            if (!joins) { cur = mutableListOf(); members += cur }
            val group = cur!!
            if (rules.dups) {
                val twin = group.firstOrNull { ham(it.hash, x.hash) <= DUP_HAM && (x.taken - it.taken <= DUP_GAP || it.size == x.size) }
                if (twin != null) {
                    // Keep the sharper of the two in the series; the other is the duplicate.
                    if (x.sharp > twin.sharp) {
                        flags[twin.id] = Defect.DUP
                        group[group.indexOf(twin)] = x
                    } else flags[x.id] = Defect.DUP
                    continue
                }
            }
            group += x
        }
        members.forEachIndexed { i, g -> for (m in g) cluster[m.id] = i }

        val rejects = xs.filter { (flags[it.id] ?: 0) != 0 }.map { it to flags.getValue(it.id) }
        val cards = ArrayList<List<Item>>()
        val used = HashSet<Int>()
        for (x in xs) {
            if ((flags[x.id] ?: 0) != 0) continue
            val ci = cluster[x.id]
            if (ci == null) { cards += listOf(x); continue }
            val g = members[ci]
            if (g.size < 2) { cards += listOf(x); continue }
            if (used.add(ci)) cards += g.toList()
        }
        return Sorted(rejects, cards)
    }
}
