package com.example.remindthyself

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getIntExtra(ReminderScheduler.EXTRA_REMINDER_ID, -1)
        if (reminderId == -1) return

        val repository = ReminderRepository(context)
        val reminders = repository.getReminders()
        val reminder = reminders.firstOrNull { it.id == reminderId && it.enabled } ?: return
        val settings = repository.getSettings()

        NotificationHelper.createChannel(context)
        NotificationHelper.showReminder(context, reminder)
        playSoundOnce(context, settings.ringtoneUri)

        ReminderScheduler.scheduleReminder(context, reminder)
    }

    private fun playSoundOnce(context: Context, ringtoneUri: String?) {
        val uri = ringtoneUri?.let { Uri.parse(it) }
            ?: android.provider.Settings.System.DEFAULT_NOTIFICATION_URI
        val player = MediaPlayer.create(context, uri) ?: return
        player.setOnCompletionListener { mp ->
            mp.release()
        }
        player.start()
    }
}
