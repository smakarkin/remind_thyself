package com.example.remindthyself

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat

object SystemStatusChecker {
    fun read(context: Context): SystemStatus {
        val notifications = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val exactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val batteryIgnored = power.isIgnoringBatteryOptimizations(context.packageName)

        return SystemStatus(
            notificationsEnabled = notifications,
            exactAlarmAllowed = exactAlarms,
            batteryOptimizationIgnored = batteryIgnored
        )
    }
}
