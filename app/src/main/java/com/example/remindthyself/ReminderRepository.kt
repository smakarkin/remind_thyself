package com.example.remindthyself

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ReminderRepository(context: Context) {
    private val prefs = context.getSharedPreferences("remind_thyself", Context.MODE_PRIVATE)

    fun getReminders(): List<ReminderItem> {
        val raw = prefs.getString(KEY_REMINDERS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    ReminderItem(
                        id = item.getInt("id"),
                        text = item.getString("text"),
                        hour = item.getInt("hour"),
                        minute = item.getInt("minute"),
                        enabled = item.getBoolean("enabled")
                    )
                )
            }
        }
    }

    fun saveReminders(items: List<ReminderItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("hour", item.hour)
                    put("minute", item.minute)
                    put("enabled", item.enabled)
                }
            )
        }
        prefs.edit().putString(KEY_REMINDERS, array.toString()).apply()
    }

    fun getSettings(): AppSettings =
        AppSettings(ringtoneUri = prefs.getString(KEY_RINGTONE_URI, null))

    fun saveSettings(settings: AppSettings) {
        prefs.edit().putString(KEY_RINGTONE_URI, settings.ringtoneUri).apply()
    }

    private companion object {
        const val KEY_REMINDERS = "reminders"
        const val KEY_RINGTONE_URI = "ringtone_uri"
    }
}
