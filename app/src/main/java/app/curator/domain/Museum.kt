package app.curator.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/** The building grows with the space set free: each wing opens at so many bytes. */
object Wings {
    private const val MB = 1L shl 20
    val AT = longArrayOf(0, 250 * MB, 1024 * MB, 2560 * MB, 5120 * MB, 10240 * MB)
    val COUNT = AT.size

    /** How many parts of the building stand (the main block always does). */
    fun open(freed: Long) = AT.count { freed >= it }

    /** Bytes still needed for the next wing, or null when everything is built. */
    fun next(freed: Long): Long? = AT.firstOrNull { it > freed }?.let { it - freed }

    /** 0..1 of the way from the last wing to the next. */
    fun progress(freed: Long): Float {
        val i = open(freed)
        if (i >= COUNT) return 1f
        val a = AT[i - 1]
        val b = AT[i]
        return ((freed - a).toFloat() / (b - a)).coerceIn(0f, 1f)
    }
}

/** The curator's rank grows with the things described (kept, hung or written off). */
object Ranks {
    val AT = intArrayOf(0, 100, 500, 2000, 5000, 12000)

    fun of(described: Int) = AT.indexOfLast { described >= it }.coerceAtLeast(0)

    fun next(described: Int): Int? = AT.firstOrNull { it > described }
}

object Dates {
    fun zone(): ZoneId = ZoneId.systemDefault()

    fun date(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone()).toLocalDate()

    /** The epoch day of the Monday of that week. */
    fun week(ms: Long): Long = date(ms).with(DayOfWeek.MONDAY).toEpochDay()

    fun weekStartMs(week: Long): Long = LocalDate.ofEpochDay(week).atStartOfDay(zone()).toInstant().toEpochMilli()

    fun monthName(month: Int, locale: Locale): String {
        val m = java.time.Month.of(month % 100)
        return m.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + month / 100
    }

    /** "КП-2021/03-0147": the inventory number of the [index]-th (0-based) thing of the month. */
    fun inventory(month: Int, index: Int, ru: Boolean): String =
        "%s-%04d/%02d-%04d".format(if (ru) "КП" else "INV", month / 100, month % 100, index + 1)
}

/** A first title for the week's exhibition; the curator may rename it. */
object Titles {
    private val RU = listOf("Тёплый", "Тихий", "Светлый", "Ясный", "Долгий", "Неспешный", "Солнечный", "Шумный", "Простой", "Домашний", "Золотой", "Ранний", "Поздний", "Лёгкий")
    private val EN = listOf("A Warm", "A Quiet", "A Bright", "A Clear", "A Long", "An Easy", "A Sunny", "A Noisy", "A Simple", "A Homely", "A Golden", "An Early", "A Late", "A Light")

    fun default(week: Long, ru: Boolean): String {
        val d = LocalDate.ofEpochDay(week).plusDays(3)
        val i = Math.floorMod(week / 7, RU.size.toLong()).toInt()
        return if (ru) "«${RU[i]} ${d.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.forLanguageTag("ru"))}»"
        else "“${EN[i]} ${d.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale.ENGLISH)}”"
    }
}
