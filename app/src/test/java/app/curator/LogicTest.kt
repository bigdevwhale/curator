package app.curator

import app.curator.data.Item
import app.curator.domain.Dates
import app.curator.domain.Defect
import app.curator.domain.Expert
import app.curator.domain.Ranks
import app.curator.domain.Rules
import app.curator.domain.Wings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LogicTest {
    private fun img(w: Int, h: Int, f: (Int, Int) -> Float) = FloatArray(w * h) { f(it % w, it / w) }

    @Test fun sharpEdgesScoreHigherThanSoftOnes() {
        val r = Random(1)
        val noisy = Expert.measure(img(96, 128) { _, _ -> r.nextFloat() * 255 }, 96, 128)
        val soft = Expert.measure(img(96, 128) { x, _ -> x * 2.5f }, 96, 128)
        assertTrue(noisy.sharp > 1000f)
        assertTrue(soft.sharp < 1f)
    }

    @Test fun brightnessIsTheMean() {
        val m = Expert.measure(img(96, 128) { _, _ -> 25.5f }, 96, 128)
        assertEquals(.1f, m.bright, 1e-4f)
    }

    @Test fun nearlyTheSamePictureHasANearHash() {
        val a = img(96, 128) { x, y -> ((x * 7 + y * 3) % 255).toFloat() }
        val b = img(96, 128) { x, y -> ((x * 7 + y * 3) % 255).toFloat() + if (x == 5 && y == 5) 40f else 0f }
        val c = img(96, 128) { x, y -> ((y * 11 + x) % 255).toFloat() }
        val ha = Expert.dhash(a, 96, 128)
        assertEquals(0, Expert.ham(ha, Expert.dhash(b, 96, 128)))
        assertTrue(Expert.ham(ha, Expert.dhash(c, 96, 128)) > 14)
    }

    private var nextId = 1L
    private fun item(at: Long, hash: Long = Random(at).nextLong(), sharp: Float = 200f, bright: Float = .5f, path: String = "DCIM/Camera/", video: Boolean = false, size: Long = 3_000_000) =
        Item(id = nextId++, taken = at, month = 202103, md = 314, size = size, path = path, bucket = path.trimEnd('/').substringAfterLast('/'),
            analyzed = true, sharp = sharp, bright = bright, hash = hash, video = video)

    @Test fun theExpertSortsRejectsSeriesAndSingles() {
        val base = 0x5A5A_1234_F0F0_0F0FL
        val xs = listOf(
            item(0),
            item(600_000, path = "Pictures/Screenshots/"),
            item(1_200_000, hash = base),
            item(1_205_000, hash = base xor (0b11_1111L shl 8)),
            item(1_212_000, hash = base xor (0b111_1111L shl 20)),
            item(2_000_000, sharp = 3f),
            item(2_600_000, bright = .03f),
            item(3_200_000, video = true, sharp = 1f, bright = .01f),
            item(4_000_000),
        )
        val s = Expert.sort(xs, Rules())
        val flags = s.rejects.associate { it.first.id to it.second }
        assertEquals(Defect.SHOT, flags[2])
        assertEquals(Defect.BLUR, flags[6])
        assertEquals(Defect.DARK, flags[7])
        assertNull("videos are never rejects", flags[8])
        assertEquals(listOf(1L), s.cards[0].map { it.id })
        assertEquals("a series becomes one card", listOf(3L, 4L, 5L), s.cards[1].map { it.id })
        assertEquals(listOf(8L), s.cards[2].map { it.id })
        assertEquals(listOf(9L), s.cards[3].map { it.id })
    }

    @Test fun anExactRepeatIsADuplicateAndTheSharperOneStays() {
        val h = 0x1234_5678_9ABC_DEF0L
        val a = item(0, hash = h, sharp = 100f)
        val b = item(2_000, hash = h xor 1, sharp = 300f)
        val s = Expert.sort(listOf(a, b), Rules())
        assertEquals(listOf(a.id to Defect.DUP), s.rejects.map { it.first.id to it.second })
        assertEquals(listOf(listOf(b.id)), s.cards.map { c -> c.map { it.id } })
    }

    @Test fun inTheScreenshotCrateScreenshotsAreNotRejects() {
        val xs = listOf(item(0, path = "Pictures/Screenshots/"), item(600_000, path = "Pictures/Screenshots/"))
        assertTrue(Expert.sort(xs, Rules(shotCrate = true)).rejects.isEmpty())
        assertEquals(2, Expert.sort(xs, Rules()).rejects.size)
        assertTrue(Expert.sort(xs, Rules(shots = false)).rejects.isEmpty())
    }

    @Test fun wingsOpenWithFreedSpace() {
        val mb = 1L shl 20
        assertEquals(1, Wings.open(0))
        assertEquals(2, Wings.open(250 * mb))
        assertEquals(3, Wings.open(2300 * mb))
        assertEquals(260 * mb, Wings.next(2300 * mb))
        assertEquals(6, Wings.open(20_000 * mb))
        assertNull(Wings.next(20_000 * mb))
    }

    @Test fun ranksGrowWithDescribedThings() {
        assertEquals(0, Ranks.of(0))
        assertEquals(1, Ranks.of(100))
        assertEquals(2, Ranks.of(1999))
        assertEquals(5, Ranks.of(99_999))
    }

    @Test fun inventoryNumbers() {
        assertEquals("КП-2021/03-0147", Dates.inventory(202103, 146, true))
        assertEquals("INV-2026/10-0001", Dates.inventory(202610, 0, false))
    }
}
