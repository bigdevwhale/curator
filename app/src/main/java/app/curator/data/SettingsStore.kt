package app.curator.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

data class Settings(
    val onboarded: Boolean = false,
    /** When the museum was founded (the first opening). */
    val founded: Long = 0,
    /** How many things go into one crate. */
    val crate: Int = 24,
    /** Bytes freed by every signed act together. */
    val freed: Long = 0,
    /** Things written off by signed acts: they count as described too. */
    val writtenOff: Int = 0,
    /** Wings already opened with a ribbon; a newly reached one gets its ceremony. */
    val wings: Int = 1,
    /** The curator's rank already announced. */
    val rank: Int = 0,
    /** The number on the next crate. */
    val crateNo: Int = 1,
    /** A morning note while crates are waiting. */
    val remind: Boolean = false,
    val flagShots: Boolean = true,
    val flagBlur: Boolean = true,
    val flagDark: Boolean = true,
    val flagDups: Boolean = true,
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        onboarded = this[ONBOARDED] ?: false,
        founded = this[FOUNDED] ?: 0L,
        crate = this[CRATE] ?: 24,
        freed = this[FREED] ?: 0L,
        writtenOff = this[WRITTEN_OFF] ?: 0,
        wings = this[WINGS] ?: 1,
        rank = this[RANK] ?: 0,
        crateNo = this[CRATE_NO] ?: 1,
        remind = this[REMIND] ?: false,
        flagShots = this[F_SHOTS] ?: true,
        flagBlur = this[F_BLUR] ?: true,
        flagDark = this[F_DARK] ?: true,
        flagDups = this[F_DUPS] ?: true,
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[ONBOARDED] = s.onboarded
            it[FOUNDED] = s.founded
            it[CRATE] = s.crate
            it[FREED] = s.freed
            it[WRITTEN_OFF] = s.writtenOff
            it[WINGS] = s.wings
            it[RANK] = s.rank
            it[CRATE_NO] = s.crateNo
            it[REMIND] = s.remind
            it[F_SHOTS] = s.flagShots
            it[F_BLUR] = s.flagBlur
            it[F_DARK] = s.flagDark
            it[F_DUPS] = s.flagDups
        }
    }

    private companion object {
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val FOUNDED = longPreferencesKey("founded")
        val CRATE = intPreferencesKey("crate")
        val FREED = longPreferencesKey("freed")
        val WRITTEN_OFF = intPreferencesKey("written_off")
        val WINGS = intPreferencesKey("wings")
        val RANK = intPreferencesKey("rank")
        val CRATE_NO = intPreferencesKey("crate_no")
        val REMIND = booleanPreferencesKey("remind")
        val F_SHOTS = booleanPreferencesKey("f_shots")
        val F_BLUR = booleanPreferencesKey("f_blur")
        val F_DARK = booleanPreferencesKey("f_dark")
        val F_DUPS = booleanPreferencesKey("f_dups")
    }
}
