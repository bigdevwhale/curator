package app.curator.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.curator.MainActivity
import app.curator.R
import app.curator.container
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** One morning note while crates are still sealed: a crate takes a couple of minutes. */
class Reminder(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        val s = c.settings.now()
        if (!s.remind || !s.onboarded) return Result.success()
        c.sync()
        val left = c.items.pendingCount()
        if (left > 0) notify(applicationContext, left, s.crateNo)
        schedule(applicationContext, true)
        return Result.success()
    }

    companion object {
        private const val NAME = "morning"
        private const val CHANNEL = "morning"
        const val HOUR = 10

        fun schedule(ctx: Context, on: Boolean) {
            val wm = WorkManager.getInstance(ctx)
            if (!on) { wm.cancelUniqueWork(NAME); return }
            val now = LocalDateTime.now()
            var at = now.withHour(HOUR).withMinute(0).withSecond(0)
            if (!at.isAfter(now)) at = at.plusDays(1)
            val delay = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
            wm.enqueueUniqueWork(NAME, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<Reminder>().setInitialDelay(delay, TimeUnit.MILLISECONDS).build())
        }

        fun notify(ctx: Context, left: Int, no: Int) {
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            val nm = ctx.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel(CHANNEL, ctx.getString(R.string.n_channel), NotificationManager.IMPORTANCE_DEFAULT))
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            nm.notify(1, NotificationCompat.Builder(ctx, CHANNEL).setSmallIcon(R.drawable.ic_notify)
                .setContentTitle(ctx.getString(R.string.n_title, no))
                .setContentText(ctx.resources.getQuantityString(R.plurals.n_body, left, left))
                .setContentIntent(open).setAutoCancel(true).build())
        }
    }
}
