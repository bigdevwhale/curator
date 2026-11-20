package app.curator.work

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.RemoteViews
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.curator.R
import app.curator.container
import app.curator.data.Item
import app.curator.domain.Dates
import app.curator.ru
import app.curator.ui.components.Card
import app.curator.ui.components.room
import app.curator.ui.theme.CuratorTheme
import app.curator.ui.theme.T
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * The frame on the home screen (#4): a photo from the museum with its caption. It changes once a day by itself,
 * and on a tap. Each frame shows one source: every exhibit, everything kept, one hall, or this day in other years.
 */
object Frame {
    const val EXHIBITS = 0
    const val KEPT = 1
    const val HALL = 2
    const val THIS_DAY = 3
    private const val NEXT = "app.curator.frame.NEXT"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("frames", Context.MODE_PRIVATE)

    fun choose(ctx: Context, id: Int, kind: Int, hall: Long) {
        prefs(ctx).edit().putInt("kind_$id", kind).putLong("hall_$id", hall).remove("item_$id").apply()
    }

    fun forget(ctx: Context, ids: IntArray) {
        val e = prefs(ctx).edit()
        for (id in ids) listOf("kind_", "hall_", "item_", "day_").forEach { e.remove(it + id) }
        e.apply()
    }

    /** Every frame on the home screen draws again: a caption changed, a photo left the museum. */
    fun refresh(ctx: Context) {
        val m = AppWidgetManager.getInstance(ctx)
        val ids = m.getAppWidgetIds(ComponentName(ctx, FrameWidget::class.java))
        if (ids.isNotEmpty()) update(ctx, ids, next = false)
    }

    fun update(ctx: Context, ids: IntArray, next: Boolean, done: () -> Unit = {}) {
        val app = ctx.applicationContext
        scope.launch {
            try {
                for (id in ids) runCatching { draw(app, id, next) }
            } finally { done() }
        }
    }

    private suspend fun draw(ctx: Context, id: Int, next: Boolean) {
        val c = ctx.container
        val p = prefs(ctx)
        val kind = p.getInt("kind_$id", EXHIBITS)
        val hall = p.getLong("hall_$id", 0)
        val today = Dates.date(System.currentTimeMillis())
        val md = today.monthValue * 100 + today.dayOfMonth
        val cur = p.getLong("item_$id", 0)
        // the same photo all day, unless asked for another
        val kept = if (!next && p.getLong("day_$id", 0) == today.toEpochDay() && cur != 0L) c.items.get(cur)?.takeIf { it.kept } else null
        val item = kept ?: c.items.framed(kind, hall, md, cur) ?: c.items.framed(kind, hall, md, 0)
            ?: c.items.framed(KEPT, 0, md, cur) ?: c.items.framed(KEPT, 0, md, 0)
        val views = RemoteViews(ctx.packageName, R.layout.widget_frame)
        if (item == null) {
            views.setImageViewBitmap(R.id.photo, null)
            views.setTextViewText(R.id.caption, ctx.getString(R.string.frame_empty))
        } else {
            p.edit().putLong("item_$id", item.id).putLong("day_$id", today.toEpochDay()).apply()
            fit(c.picture(item, 720, 720))?.let { views.setImageViewBitmap(R.id.photo, it) }
            views.setTextViewText(R.id.caption, caption(ctx, item))
        }
        val tap = Intent(ctx, FrameWidget::class.java).setAction(NEXT).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
        views.setOnClickPendingIntent(R.id.frame, PendingIntent.getBroadcast(ctx, id, tap, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
        AppWidgetManager.getInstance(ctx).updateAppWidget(id, views)
    }

    /** The curator's own words, or the month it was taken in. */
    private fun caption(ctx: Context, x: Item): String = x.caption.ifBlank {
        Dates.monthName(x.month, if (ctx.ru) Locale.forLanguageTag("ru") else Locale.ENGLISH)
    }

    /** Small enough for the home screen to accept. */
    private fun fit(b: Bitmap?): Bitmap? {
        if (b == null) return null
        val k = 720f / maxOf(b.width, b.height)
        return if (k >= 1f) b else Bitmap.createScaledBitmap(b, (b.width * k).toInt().coerceAtLeast(1), (b.height * k).toInt().coerceAtLeast(1), true)
    }

    internal fun isNext(i: Intent) = i.action == NEXT
}

class FrameWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        Frame.update(context, ids, next = false) { pending.finish() }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (Frame.isNext(intent)) {
            val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val pending = goAsync()
                Frame.update(context, intArrayOf(id), next = true) { pending.finish() }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onDeleted(context: Context, ids: IntArray) = Frame.forget(context, ids)
}

/** Choosing what a new frame shows. */
class FrameSetup : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val id = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID
        setResult(RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        fun pick(kind: Int, hall: Long = 0) {
            Frame.choose(this, id, kind, hall)
            Frame.update(this, intArrayOf(id), next = true)
            setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id))
            finish()
        }
        setContent {
            val settings by remember { container.settings.settings }.collectAsState(null)
            val halls by remember { container.museum.halls() }.collectAsState(emptyList())
            CuratorTheme(settings?.theme ?: 0) {
                Column(Modifier.fillMaxSize().room().statusBarsPadding().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(stringResource(R.string.frame_t), style = T.Title, modifier = Modifier.padding(bottom = 4.dp))
                    Text(stringResource(R.string.frame_b), style = T.Small, modifier = Modifier.padding(bottom = 12.dp))
                    val rows = listOf(
                        Triple(Frame.EXHIBITS, 0L, getString(R.string.frame_exhibits)),
                        Triple(Frame.KEPT, 0L, getString(R.string.frame_kept)),
                        Triple(Frame.THIS_DAY, 0L, getString(R.string.frame_this_day)),
                    ) + halls.map { Triple(Frame.HALL, it.id, getString(R.string.frame_hall, it.name)) }
                    for ((kind, hall, label) in rows) Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), pad = 16.dp, onClick = { pick(kind, hall) }) {
                        Text(label, style = T.Strong)
                    }
                }
            }
        }
    }
}
