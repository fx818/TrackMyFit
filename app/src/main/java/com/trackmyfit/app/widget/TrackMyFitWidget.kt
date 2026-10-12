package com.trackmyfit.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.trackmyfit.app.MainActivity
import com.trackmyfit.app.R
import com.trackmyfit.app.TrackMyFitApp
import com.trackmyfit.app.data.AppData
import com.trackmyfit.app.data.Stats
import java.text.NumberFormat
import java.time.LocalDate

class TrackMyFitWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val data = (context.applicationContext as TrackMyFitApp).repository.data.value
        ids.forEach { manager.updateAppWidget(it, build(context, data)) }
    }

    companion object {
        fun updateAll(context: Context, data: AppData) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, TrackMyFitWidget::class.java))
            if (ids.isEmpty()) return
            val views = build(context, data)
            ids.forEach { manager.updateAppWidget(it, views) }
        }

        private fun pi(context: Context, tab: String, req: Int) = PendingIntent.getActivity(
            context, req,
            Intent(context, MainActivity::class.java).putExtra("tab", tab).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        fun build(context: Context, data: AppData): RemoteViews {
            val today = LocalDate.now()
            val day = data.days[today.toString()]
            val v = RemoteViews(context.packageName, R.layout.widget_trackmyfit)
            val title = when {
                day?.hasWorkout == true -> (day.labels.joinToString(" · ").ifEmpty { "Workout" }) + " ✓"
                day?.labels?.any { it.equals("Rest", true) } == true -> "Rest day"
                day?.labels?.isNotEmpty() == true -> "Today: " + day.labels.joinToString(" · ")
                else -> "Ready to train?"
            }
            val sub = if (day?.hasWorkout == true) "${day.entries.size} exercises · ${Stats.setCount(day)} sets" else "Tap to log today's workout"
            v.setTextViewText(R.id.w_title, title)
            v.setTextViewText(R.id.w_sub, sub)
            v.setTextViewText(R.id.w_streak, Stats.currentStreak(data, today).toString())
            v.setTextViewText(R.id.w_week, "${Stats.workoutsThisWeek(data, today)}/${data.settings.weeklyGoal}")
            val steps = data.stepsCache?.takeIf { it.date == today.toString() }?.steps
            v.setTextViewText(R.id.w_steps, steps?.let { NumberFormat.getIntegerInstance().format(it) } ?: "—")
            v.setOnClickPendingIntent(R.id.w_root, pi(context, "log", 3001))
            v.setOnClickPendingIntent(R.id.w_weight_btn, pi(context, "weight", 3002))
            return v
        }
    }
}
