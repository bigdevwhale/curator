package app.curator.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

private const val PENDING = "verdict IN ('NONE','LATER')"
private const val SHOTS = "(path LIKE '%screenshot%' OR bucket LIKE '%screenshot%' OR bucket LIKE '%скриншот%')"
private const val CHATS = "(path LIKE '%whatsapp%' OR path LIKE '%telegram%' OR path LIKE '%viber%' OR path LIKE '%signal%' OR path LIKE '%messenger%' OR bucket = 'VK')"

@Dao
interface ItemDao {
    @Query("""SELECT month, COUNT(*) AS total, SUM(CASE WHEN $PENDING THEN 1 ELSE 0 END) AS pending, SUM(size) AS bytes,
        SUM(CASE WHEN $PENDING THEN size ELSE 0 END) AS pendingBytes FROM items GROUP BY month ORDER BY month DESC""")
    fun months(): Flow<List<MonthRow>>

    @Query("""SELECT SUM(CASE WHEN verdict IN ('KEEP','EXHIBIT') THEN 1 ELSE 0 END) AS fonds,
        SUM(CASE WHEN verdict = 'EXHIBIT' THEN 1 ELSE 0 END) AS exhibits,
        SUM(CASE WHEN $PENDING THEN 1 ELSE 0 END) AS pending,
        SUM(CASE WHEN verdict = 'ACT' THEN 1 ELSE 0 END) AS act,
        COALESCE(SUM(CASE WHEN verdict = 'ACT' THEN size ELSE 0 END), 0) AS actBytes,
        COUNT(*) AS total FROM items""")
    fun stats(): Flow<Stats?>

    @Query("SELECT COUNT(*) AS n, COALESCE(SUM(size), 0) AS bytes FROM items WHERE $PENDING AND $SHOTS")
    fun shots(): Flow<Count>

    @Query("SELECT COUNT(*) AS n, COALESCE(SUM(size), 0) AS bytes FROM items WHERE $PENDING AND video = 1")
    fun videos(): Flow<Count>

    @Query("SELECT COUNT(*) AS n, COALESCE(SUM(size), 0) AS bytes FROM items WHERE $PENDING AND $CHATS")
    fun chats(): Flow<Count>

    @Query("SELECT COUNT(*) AS n, COALESCE(SUM(size), 0) AS bytes FROM items WHERE $PENDING AND size >= :min")
    fun heavy(min: Long = HEAVY): Flow<Count>

    @Query("SELECT * FROM items WHERE month = :month AND $PENDING ORDER BY verdict = 'LATER', taken LIMIT :n")
    suspend fun pendingMonth(month: Int, n: Int): List<Item>

    @Query("SELECT * FROM items WHERE $PENDING AND $SHOTS ORDER BY verdict = 'LATER', taken LIMIT :n")
    suspend fun pendingShots(n: Int): List<Item>

    @Query("SELECT * FROM items WHERE $PENDING AND video = 1 ORDER BY verdict = 'LATER', size DESC LIMIT :n")
    suspend fun pendingVideos(n: Int): List<Item>

    @Query("SELECT * FROM items WHERE $PENDING AND $CHATS ORDER BY verdict = 'LATER', taken LIMIT :n")
    suspend fun pendingChats(n: Int): List<Item>

    @Query("SELECT * FROM items WHERE $PENDING AND size >= :min ORDER BY verdict = 'LATER', size DESC LIMIT :n")
    suspend fun pendingHeavy(n: Int, min: Long = HEAVY): List<Item>

    /** Four things to show on a crate's lid: those still waiting first. */
    @Query("SELECT * FROM items WHERE month = :month AND verdict != 'ACT' ORDER BY $PENDING DESC, taken LIMIT 4")
    suspend fun cover(month: Int): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE month = :month AND taken < :taken")
    suspend fun before(month: Int, taken: Long): Int

    @Query("SELECT * FROM items WHERE verdict = 'ACT' ORDER BY decided DESC")
    fun act(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE verdict = 'EXHIBIT' ORDER BY decided DESC")
    fun exhibits(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE hall = :hall AND verdict IN ('KEEP','EXHIBIT') ORDER BY taken DESC")
    fun inHall(hall: Long): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE hall = :hall AND verdict IN ('KEEP','EXHIBIT')")
    suspend fun hallItems(hall: Long): List<Item>

    @Query("SELECT hall, COUNT(*) AS n FROM items WHERE hall IS NOT NULL AND verdict IN ('KEEP','EXHIBIT') GROUP BY hall")
    fun hallCounts(): Flow<List<HallCount>>

    /** Up to three things per hall for the plan's little frames. */
    @Query("SELECT * FROM items WHERE hall IS NOT NULL AND verdict IN ('KEEP','EXHIBIT') ORDER BY verdict = 'EXHIBIT' DESC, decided DESC")
    fun hung(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE md = :md AND taken < :before AND verdict IN ('KEEP','EXHIBIT','NONE') ORDER BY verdict = 'EXHIBIT' DESC, verdict = 'KEEP' DESC, taken DESC LIMIT 12")
    suspend fun onThisDay(md: Int, before: Long): List<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): Item?

    @Query("SELECT id, path, bucket, size FROM items")
    suspend fun brief(): List<app.curator.data.Brief>

    @Query("SELECT COUNT(*) FROM items")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM items WHERE $PENDING")
    suspend fun pendingCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(xs: List<Item>)

    @Update
    suspend fun update(x: Item)

    @Query("UPDATE items SET path = :path, bucket = :bucket, size = :size WHERE id = :id")
    suspend fun moved(id: Long, path: String, bucket: String, size: Long)

    @Query("UPDATE items SET verdict = :v, hall = :hall, decided = :at WHERE id = :id")
    suspend fun decide(id: Long, v: Verdict, hall: Long?, at: Long)

    @Query("UPDATE items SET analyzed = 1, sharp = :sharp, bright = :bright, hash = :hash WHERE id = :id")
    suspend fun analyzed(id: Long, sharp: Float, bright: Float, hash: Long)

    @Query("UPDATE items SET hall = NULL WHERE hall = :hall")
    suspend fun clearHall(hall: Long)

    @Query("DELETE FROM items WHERE id IN (:ids)")
    suspend fun delete(ids: List<Long>)

    companion object {
        /** A thing this heavy (20 MB) belongs to the "heavy" special collection. */
        const val HEAVY = 20L shl 20
    }
}

@Dao
interface MuseumDao {
    @Query("SELECT * FROM halls ORDER BY created")
    fun halls(): Flow<List<Hall>>

    @Query("SELECT * FROM halls ORDER BY created")
    suspend fun hallList(): List<Hall>

    @Insert
    suspend fun addHall(h: Hall): Long

    @Update
    suspend fun updateHall(h: Hall)

    @Query("DELETE FROM halls WHERE id = :id")
    suspend fun deleteHall(id: Long)

    @Query("SELECT * FROM shows ORDER BY week DESC")
    fun shows(): Flow<List<Show>>

    @Query("SELECT * FROM shows WHERE week = :week")
    suspend fun show(week: Long): Show?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putShow(s: Show)

    @Query("SELECT * FROM chronicle ORDER BY at DESC, id DESC")
    fun chronicle(): Flow<List<Event>>

    @Insert
    suspend fun log(e: Event)
}

@Database(entities = [Item::class, Hall::class, Show::class, Event::class], version = 1, exportSchema = true)
abstract class MuseumDb : RoomDatabase() {
    abstract fun items(): ItemDao
    abstract fun museum(): MuseumDao

    companion object {
        fun create(context: Context): MuseumDb = Room.databaseBuilder(context, MuseumDb::class.java, "museum.db").build()
    }
}
