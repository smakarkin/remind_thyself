package com.example.remindthyself

data class ReminderItem(
    val id: Int,
    val text: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean
)

data class AppSettings(
    val ringtoneUri: String?
)

data class SystemStatus(
    val notificationsEnabled: Boolean,
    val exactAlarmAllowed: Boolean,
    val batteryOptimizationIgnored: Boolean
)
