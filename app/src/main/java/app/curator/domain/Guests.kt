package app.curator.domain

import kotlin.random.Random

/**
 * The museum's visitors (#6): how many came today, and the lines they leave in the guest book.
 * A line is written after a crate is gone through and once a day, about what the museum holds now:
 * a hall by its name, the week's exhibition, the month just sorted, the new wing. Nothing is made up
 * about the photos themselves; the visitors only walk the halls the curator built.
 */
object Guests {
    /** What the visitors can see. [halls] are the halls' names with how many things hang in each. */
    data class View(
        val ru: Boolean,
        val halls: List<Pair<String, Int>>,
        val exhibits: Int,
        val kept: Int,
        val show: String?,
        val wings: Int,
        /** The month just gone through, as «март 2021», or null. */
        val month: String? = null,
    )

    /** How many came today: more with every exhibit and hall, a little different each day. */
    fun today(v: View, day: Long): Int {
        val base = v.exhibits * 2 + v.halls.size * 4 + v.kept / 40 + (v.wings - 1) * 3
        if (base == 0) return 0
        val r = Random(day * 7919 + base)
        return (base * (0.7 + r.nextDouble() * 0.6)).toInt().coerceIn(1, 999) + 2
    }

    private val WHO_RU = listOf(
        "Учительница литературы", "Школьник из шестого «Б»", "Дедушка с внучкой", "Студентка художественного училища",
        "Турист из Казани", "Ночной сторож", "Пара на первом свидании", "Фотограф-любитель", "Мама с коляской",
        "Строгий искусствовед", "Курьер, зашедший погреться", "Бывший одноклассник", "Соседка с третьего этажа",
        "Экскурсовод на пенсии", "Девочка лет семи", "Человек с зонтом", "Группа из клуба «Кому за шестьдесят»",
    )
    private val WHO_EN = listOf(
        "A literature teacher", "A boy from year six", "A grandfather with his granddaughter", "An art student",
        "A tourist from Lisbon", "The night watchman", "A couple on a first date", "An amateur photographer", "A mother with a pram",
        "A stern art critic", "A courier who came in to get warm", "An old classmate", "The neighbour from the third floor",
        "A retired guide", "A girl of about seven", "Someone with an umbrella", "The over-sixties walking club",
    )

    /** Lines that need nothing but an open door. */
    private val ANY = listOf(
        "Собирались на полчаса, а вышло на два часа. Спасибо!" to "Came in for half an hour, left after two. Thank you!",
        "Тихо, светло, и никто не торопит. Обязательно вернусь." to "Quiet, light, and nobody hurries you. We'll be back.",
        "Видно, что здесь каждую фотографию выбирали, а не просто складывали." to "You can tell every photo here was chosen, not just piled up.",
        "В гардеробе сказали, что раньше тут всё было заставлено ящиками. Не верится!" to "The cloakroom lady says this place used to be full of crates. Hard to believe!",
        "Смотришь на чужие воспоминания и вспоминаешь своё. Хороший музей." to "You look at someone's memories and remember your own. A good museum.",
        "Билет стоит дёшево, а впечатлений – на неделю." to "The ticket is cheap, and it'll stay with me all week.",
        "Пожалуйста, не закрывайтесь по понедельникам." to "Please don't close on Mondays.",
        "Самое красивое здесь – что всё настоящее." to "The best thing here is that it's all real.",
    )

    /** About one hall: {h} is its name. */
    private val HALL = listOf(
        "Дольше всего хочется стоять в зале «{h}»." to "We spent longest in “{h}”.",
        "В зале «{h}» не хватает лавочки. А фотографии чудесные." to "“{h}” needs a bench. The photos are lovely.",
        "Повесьте в «{h}» ещё что-нибудь, очень уж там хорошо." to "Hang something more in “{h}”, it's so good in there.",
        "Ребёнок спросил, почему в зале «{h}» все улыбаются. Ответа так и не нашлось." to "A child asked why everyone smiles in “{h}”. We didn't know what to say.",
        "Зал «{h}» – как письмо, которое приятно перечитать." to "“{h}” is like a letter you're glad to read again.",
        "Из «{h}» не хотелось уходить. Смотрительница не торопила." to "Nobody wanted to leave “{h}”. The attendant didn't rush us.",
    )

    /** About a small hall: {h}, and {n} things in it. */
    private val SMALL = listOf(
        "В зале «{h}» пока всего {n}. Ждём продолжения!" to "Only {n} in “{h}” so far. Waiting for more!",
    )

    /** About the week's exhibition: {s} is its title, already in quotes. */
    private val SHOW = listOf(
        "Выставка {s} – просто подарок. Пришлю друзей." to "The exhibition {s} is a gift. I'll send my friends.",
        "Ради выставки {s} стоило прийти." to "The exhibition {s} was worth the trip.",
        "Афиша {s} висит у меня на холодильнике." to "The poster for {s} is on my fridge now.",
    )

    /** After a month is gone through: {m} is the month. */
    private val MONTH = listOf(
        "Говорят, сегодня разобрали {m}. Чувствуется: в залах стало светлее." to "They say {m} was sorted today. You can feel it: the halls are lighter.",
        "Новые поступления за {m} – отдельное удовольствие." to "The new arrivals from {m} are a joy of their own.",
        "Пока разбирали {m}, можно было обойти всё дважды." to "While {m} was being sorted, we went round everything twice.",
        "За {m} теперь не стыдно. Видно, сколько было лишнего." to "{m} looks proud now. You can see how much was just clutter.",
    )

    /** When more than the first building stands. */
    private val WING = listOf(
        "Новое крыло пахнет краской и простором." to "The new wing smells of paint and space.",
        "Как просторно стало! Говорят, раньше тут было не пройти." to "So much room now! They say you couldn't get through here before.",
    )

    /** When there is almost nothing to see yet. */
    private val EARLY = listOf(
        "Залов пока немного, но начало хорошее." to "Not many halls yet, but a good start.",
        "Ждём, когда откроются новые залы." to "Looking forward to the new halls opening.",
    )

    private fun things(n: Int, ru: Boolean): String {
        if (!ru) return if (n == 1) "1 photo" else "$n photos"
        val a = n % 100; val b = n % 10
        val w = if (a in 11..14) "фотографий" else when (b) { 1 -> "фотография"; 2, 3, 4 -> "фотографии"; else -> "фотографий" }
        return "$n $w"
    }

    /** One line for the book, or null when the museum has nothing to show yet. [recent] are the last lines, not to repeat them. */
    fun write(v: View, r: Random, recent: List<String> = emptyList()): Pair<String, String>? {
        if (v.kept == 0 && v.exhibits == 0) return null
        fun pick(xs: List<Pair<String, String>>) = xs.map { if (v.ru) it.first else it.second }
        val pool = mutableListOf<String>()
        val month = v.month
        if (month != null) pool += pick(MONTH).map { it.replace("{m}", month) }.let { it + it } // the month just sorted is the news of the day
        val hung = v.halls.filter { it.second > 0 }
        for ((h, n) in hung) {
            pool += pick(HALL).map { it.replace("{h}", h) }
            if (n in 1..4) pool += pick(SMALL).map { it.replace("{h}", h).replace("{n}", things(n, v.ru)) }
        }
        v.show?.takeIf { v.exhibits > 0 }?.let { s -> pool += pick(SHOW).map { it.replace("{s}", s) } }
        if (v.wings > 1) pool += pick(WING)
        if (hung.size < 2 && v.exhibits < 10) pool += pick(EARLY)
        pool += pick(ANY)
        val fresh = pool.filter { it !in recent }.ifEmpty { pool }
        val text = fresh[r.nextInt(fresh.size)]
        val who = (if (v.ru) WHO_RU else WHO_EN).let { it[r.nextInt(it.size)] }
        return who to text
    }
}
