package app.curator.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** What the curator decided about a thing. NONE and LATER are still waiting in their crate. */
enum class Verdict { NONE, KEEP, EXHIBIT, ACT, LATER }

/**
 * One photo or video from the phone's gallery, as the museum knows it.
 * [id] is the MediaStore id. A demo item has a [seed] and an [art] kind and is drawn in code instead of read from the phone.
 */
@Entity(tableName = "items", indices = [Index("month"), Index("verdict"), Index("hall")])
data class Item(
    @PrimaryKey val id: Long,
    val video: Boolean = false,
    /** When it was taken (or added, if the camera didn't say), in ms. */
    val taken: Long,
    /** yyyymm of [taken] in local time: the crate it arrived in. */
    val month: Int,
    /** mmdd of [taken]: for "this day in other years". */
    val md: Int,
    val size: Long,
    val w: Int = 0,
    val h: Int = 0,
    /** The folder's name as the gallery shows it: Camera, Screenshots, WhatsApp Images… */
    val bucket: String = "",
    /** MediaStore RELATIVE_PATH, e.g. "DCIM/Camera/". */
    val path: String = "",
    val duration: Long = 0,
    val verdict: Verdict = Verdict.NONE,
    /** The hall it hangs in, if any. */
    val hall: Long? = null,
    val decided: Long = 0,
    /** The expert has looked at it: [sharp], [bright] and [hash] are filled. */
    val analyzed: Boolean = false,
    val sharp: Float = -1f,
    val bright: Float = -1f,
    val hash: Long = 0,
    val seed: Int = 0,
    val art: String = "",
) {
    val fake get() = seed != 0
    val pending get() = verdict == Verdict.NONE || verdict == Verdict.LATER
    val kept get() = verdict == Verdict.KEEP || verdict == Verdict.EXHIBIT
}

/** A room of the museum: the person's own album. [folder] is set once its things were moved to a real folder. */
@Entity(tableName = "halls")
data class Hall(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val created: Long,
    val folder: String? = null,
)

/** The exhibition of a week: its title. The pictures are that week's exhibits. [week] is the epoch day of its Monday. */
@Entity(tableName = "shows")
data class Show(
    @PrimaryKey val week: Long,
    val title: String,
)

enum class EventKind { FOUNDED, CRATE, ACT, WING, HALL, RANK }

/** A line in the museum's chronicle. */
@Entity(tableName = "chronicle")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val kind: EventKind,
    val n: Int = 0,
    val bytes: Long = 0,
    val text: String = "",
)

data class MonthRow(val month: Int, val total: Int, val pending: Int, val bytes: Long, val pendingBytes: Long)

data class Stats(val fonds: Int, val exhibits: Int, val pending: Int, val act: Int, val actBytes: Long, val total: Int)

data class Brief(val id: Long, val path: String, val bucket: String, val size: Long)

data class Count(val n: Int, val bytes: Long)

data class HallCount(val hall: Long, val n: Int)

/** The special collections: crates that cut across the months. */
enum class Fund { SHOTS, VIDEO, CHATS, HEAVY }

/** Where a crate's things come from. */
sealed interface Source {
    data class Month(val month: Int) : Source
    data class Special(val fund: Fund) : Source
    /** A month gone through once more: what was kept there before [since] (#1). */
    data class Review(val month: Int, val since: Long) : Source
}
