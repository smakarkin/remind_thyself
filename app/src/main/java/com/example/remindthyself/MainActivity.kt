package com.example.remindthyself

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        NotificationHelper.createChannel(this)

        setContent {
            MaterialTheme {
                ReminderSettingsScreen()
            }
        }
    }
}

@Composable
private fun ReminderSettingsScreen() {
    val context = LocalContext.current
    val repository = remember { ReminderRepository(context) }
    var reminders by remember { mutableStateOf(repository.getReminders()) }
    var settings by remember { mutableStateOf(repository.getSettings()) }
    var status by remember { mutableStateOf(SystemStatusChecker.read(context)) }
    var previewRingtone by remember { mutableStateOf<Ringtone?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        status = SystemStatusChecker.read(context)
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        settings = settings.copy(ringtoneUri = uri?.toString())
        repository.saveSettings(settings)
    }

    fun persistAndReschedule(newList: List<ReminderItem>) {
        reminders = newList
        repository.saveReminders(newList)
        ReminderScheduler.scheduleAll(context, newList)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Напоминания", style = MaterialTheme.typography.headlineSmall)
            }

            items(reminders, key = { it.id }) { reminder ->
                ReminderCard(
                    reminder = reminder,
                    onChange = { updated ->
                        persistAndReschedule(reminders.map { if (it.id == updated.id) updated else it })
                    },
                    onDelete = {
                        ReminderScheduler.cancelReminder(context, reminder.id)
                        persistAndReschedule(reminders.filterNot { it.id == reminder.id })
                    }
                )
            }

            item {
                Button(
                    enabled = reminders.size < 10,
                    onClick = {
                        val id = generateId(reminders)
                        val newReminder = ReminderItem(id, "", 9, 0, true)
                        persistAndReschedule(reminders + newReminder)
                    }
                ) {
                    Text("Добавить напоминание")
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Звук", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Выбранный URI: ${settings.ringtoneUri ?: "системный по умолчанию"}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_NOTIFICATION
                            )
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                settings.ringtoneUri?.let(Uri::parse)
                            )
                        }
                        ringtonePickerLauncher.launch(intent)
                    }) {
                        Text("Выбрать звук")
                    }

                    Button(onClick = {
                        previewRingtone?.stop()
                        val uri = settings.ringtoneUri?.let(Uri::parse)
                            ?: Settings.System.DEFAULT_NOTIFICATION_URI
                        previewRingtone = RingtoneManager.getRingtone(context, uri)
                        previewRingtone?.play()
                    }) { Text("Прослушать") }

                    Button(onClick = { previewRingtone?.stop() }) { Text("Стоп") }
                }
            }

            item { HorizontalDivider() }

            item {
                Text("Системные проверки", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                StatusLine("Уведомления", status.notificationsEnabled)
                StatusLine("Точные alarm", status.exactAlarmAllowed)
                StatusLine("Без оптимизации батареи", status.batteryOptimizationIgnored)
                Spacer(modifier = Modifier.height(8.dp))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !status.notificationsEnabled) {
                    Button(onClick = {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }) { Text("Разрешить уведомления") }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    val powerManager = context.getSystemService(PowerManager::class.java)
                    if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
                        val intent = Intent(
                            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                }) {
                    Text("Отключить оптимизацию батареи")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                }) {
                    Text("Разрешить точные alarms")
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { status = SystemStatusChecker.read(context) }) {
                    Text("Проверить сейчас")
                }
            }
        }
    }
}

@Composable
private fun ReminderCard(
    reminder: ReminderItem,
    onChange: (ReminderItem) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = reminder.text,
                onValueChange = { onChange(reminder.copy(text = it)) },
                label = { Text("Текст") }
            )

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute))
                Button(onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            onChange(reminder.copy(hour = hour, minute = minute))
                        },
                        reminder.hour,
                        reminder.minute,
                        true
                    ).show()
                }) { Text("Время") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Вкл")
                    Switch(
                        checked = reminder.enabled,
                        onCheckedChange = { onChange(reminder.copy(enabled = it)) }
                    )
                }

                Button(onClick = onDelete) { Text("Удалить") }
            }
        }
    }
}

@Composable
private fun StatusLine(name: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = ok, onCheckedChange = null)
        Text("$name: ${if (ok) "OK" else "Требует внимания"}")
    }
}

private fun generateId(items: List<ReminderItem>): Int {
    var candidate: Int
    do {
        candidate = Random.nextInt(1000, 999999)
    } while (items.any { it.id == candidate })
    return candidate
}
