package app.curator

import app.curator.domain.Guests
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** The visitors and their guest book (#6). */
class GuestsTest {
    private val v = Guests.View(true, listOf("Море" to 12, "Дача" to 3), exhibits = 8, kept = 400, show = "«Тёплый ноябрь»", wings = 2)

    @Test fun anEmptyMuseumHasNoVisitors() {
        val empty = Guests.View(true, emptyList(), 0, 0, null, 1)
        assertEquals(0, Guests.today(empty, 1))
        assertNull(Guests.write(empty, Random(1)))
    }

    @Test fun visitorsComeAndWriteAboutWhatIsThere() {
        assertTrue(Guests.today(v, 20_000) > 0)
        val lines = (1..200).mapNotNull { Guests.write(v, Random(it)) }
        assertTrue(lines.any { "«Море»" in it.second })
        assertTrue(lines.any { "«Тёплый ноябрь»" in it.second })
        assertTrue(lines.any { "3 фотографии" in it.second }) // the small hall
        assertTrue(lines.none { '{' in it.second })
    }

    @Test fun theMonthJustSortedIsTheNews() {
        val lines = (1..100).mapNotNull { Guests.write(v.copy(month = "март 2021"), Random(it)) }
        assertTrue(lines.count { "март 2021" in it.second } > 20)
    }

    @Test fun recentLinesAreNotRepeated() {
        val first = Guests.write(v, Random(5))
        assertNotNull(first)
        val all = (1..50).map { Guests.write(v, Random(it), listOf(first!!.second))!!.second }
        assertTrue(first!!.second !in all)
    }
}
