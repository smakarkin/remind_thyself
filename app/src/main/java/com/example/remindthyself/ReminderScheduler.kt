package com.example.remindthyself

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.random.Random

object ReminderScheduler {

    fun scheduleReminder(context: Context, reminder: ReminderItem) {
        if (!reminder.enabled) {
            cancelReminder(context, reminder.id)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = nextTriggerMillis(reminder.hour, reminder.minute)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, reminder.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
    }

    fun scheduleAll(context: Context, reminders: List<ReminderItem>) {
        reminders.forEach { scheduleReminder(context, it) }
    }

    fun cancelReminder(context: Context, reminderId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun nextTriggerMillis(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        val offsetSeconds = Random.nextInt(-300, 301)
        val randomized = target.plusSeconds(offsetSeconds.toLong())
        return randomized.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }

    const val EXTRA_REMINDER_ID = "extra_reminder_id"
}
