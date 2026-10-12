package com.trackmyfit.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.trackmyfit.app.MainActivity
import com.trackmyfit.app.R
import com.trackmyfit.app.TrackMyFitApp
import com.trackmyfit.app.data.AppSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Notifier {
    const val CH_REST = "rest_timer"
    const val CH_REST_DONE = "rest_done"
    const val CH_REMIND = "reminders"
    private const val ID_REST = 1001
    private const val ID_REMIND = 1002

    fun createChannels(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH_REST, "Rest timer (countdown)", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the rest countdown while you're in other apps"
            setShowBadge(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REST_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alerts you when rest time is over"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 300, 150, 300, 150, 500)
        })
        nm.createNotificationChannel(NotificationChannel(CH_REMIND, "Workout reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Reminds you on your gym days"
        })
    }

    fun canPost(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(ctx: Context, tab: String = "log"): PendingIntent =
        PendingIntent.getActivity(
            ctx, tab.hashCode(),
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP).putExtra("tab", tab),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Ongoing notification with a live countdown (system chronometer, no service needed). */
    fun showRestCountdown(ctx: Context, endAtMillis: Long, exerciseName: String?) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_REST)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle("Resting")
            .setContentText(exerciseName?.let { "Next set: $it" } ?: "Get ready for the next set")
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setWhen(endAtMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setTimeoutAfter((endAtMillis - System.currentTimeMillis()).coerceAtLeast(1000))
            .setContentIntent(openApp(ctx))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_REST, n) } catch (_: SecurityException) {}
    }

    fun showRestDone(ctx: Context) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_REST_DONE)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle("Rest over")
            .setContentText("Time for your next set 💪")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setTimeoutAfter(60_000)
            .setContentIntent(openApp(ctx))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_REST, n) } catch (_: SecurityException) {}
    }

    fun cancelRest(ctx: Context) = NotificationManagerCompat.from(ctx).cancel(ID_REST)

    fun showReminder(ctx: Context, title: String, text: String) {
        if (!canPost(ctx)) return
        val n = NotificationCompat.Builder(ctx, CH_REMIND)
            .setSmallIcon(R.drawable.ic_stat_dumbbell)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        try { NotificationManagerCompat.from(ctx).notify(ID_REMIND, n) } catch (_: SecurityException) {}
    }
}

/** Schedules the next daily reminder check with AlarmManager (inexact, battery friendly). */
object ReminderScheduler {
    private fun pending(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 2001, Intent(ctx, ReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun schedule(ctx: Context, s: AppSettings) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pending(ctx))
        if (!s.remindersEnabled || s.reminderDays.isEmpty()) return
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(s.reminderHour, s.reminderMinute)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val millis = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        // One-shot alarm with a 10-minute delivery window (no exact-alarm permission needed).
        // The receiver re-schedules the next one, and checks whether today is a selected gym day.
        am.setWindow(AlarmManager.RTC_WAKEUP, millis, 10 * 60 * 1000L, pending(ctx))
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as TrackMyFitApp
        val data = app.repository.data.value
        val s = data.settings
        ReminderScheduler.schedule(context, s) // queue tomorrow's check
        if (!s.remindersEnabled) return
        val today = LocalDate.now()
        if (today.dayOfWeek.value !in s.reminderDays) return
        val day = data.days[today.toString()]
        if (day?.hasWorkout == true || day?.labels?.any { it.equals("Rest", true) } == true) return
        val plan = day?.labels?.takeIf { it.isNotEmpty() }?.joinToString(" · ")
        Notifier.showReminder(
            context,
            "Gym time${data.profile.name.takeIf { it.isNotBlank() }?.let { ", $it" } ?: ""}!",
            plan?.let { "Today: $it. Tap to start logging." } ?: "It's one of your training days. Tap to log your workout.",
        )
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val app = context.applicationContext as TrackMyFitApp
            ReminderScheduler.schedule(context, app.repository.data.value.settings)
        }
    }
}
