package app.tsuzuku.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.tsuzuku.MainActivity
import app.tsuzuku.R
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class HabitReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, -1L)
        if (habitId < 0) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dao = TsuzukuDatabase.getInstance(context).habitDao()
                when (intent.action) {
                    ACTION_COMPLETE -> {
                        val repository = RoomHabitRepository(dao)
                        val today = repository.observeToday().first().firstOrNull { it.habit.id == habitId }
                        // Never undo: the button means "done", even if the app was used in between.
                        if (today != null && !today.isComplete) {
                            if (today.habit.type == app.tsuzuku.model.HabitType.Check) {
                                repository.toggleHabit(habitId)
                            } else {
                                repository.addProgress(habitId, (today.habit.target - today.progress).coerceAtLeast(0.0))
                            }
                        }
                        context.getSystemService(NotificationManager::class.java).cancel(habitId.hashCode())
                        app.tsuzuku.widget.TsuzukuWidgets.updateAll(context)
                    }
                    ACTION_ADD_AMOUNT -> {
                        val typed = android.app.RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_AMOUNT)?.toString()
                        val amount = typed?.replace(',', '.')?.trim()?.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            RoomHabitRepository(dao).addProgress(habitId, amount)
                            app.tsuzuku.widget.TsuzukuWidgets.updateAll(context)
                        }
                        // Answering a reply field must update or remove the notification, or it keeps spinning.
                        context.getSystemService(NotificationManager::class.java).cancel(habitId.hashCode())
                    }
                    ACTION_REMIND -> {
                        val habit = dao.getHabit(habitId) ?: return@launch
                        HabitReminderScheduler(context, dao).schedule(habit)
                        val doneToday = RoomHabitRepository(dao).observeToday().first()
                            .firstOrNull { it.habit.id == habitId }?.isComplete == true
                        if (doneToday) return@launch
                        if (HabitAlarms.isOn(context, habitId)) {
                            HabitAlarms.ring(context, habitId, habit.name, habit.emoji, habit.intention)
                            return@launch
                        }
                        HabitNotifications.show(
                            context,
                            habitId,
                            habit.name,
                            habit.emoji,
                            habit.reminderRepeatMinutes,
                            plan = habit.intention,
                            amountUnit = habit.unit.takeIf { habit.type != app.tsuzuku.model.HabitType.Check.name }?.ifBlank { " " },
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "app.tsuzuku.action.REMIND_HABIT"
        const val ACTION_COMPLETE = "app.tsuzuku.action.COMPLETE_HABIT"
        const val ACTION_ADD_AMOUNT = "app.tsuzuku.action.ADD_AMOUNT"
        const val KEY_AMOUNT = "amount"
        const val EXTRA_HABIT_ID = "habit_id"
    }
}

class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                HabitReminderScheduler(context, TsuzukuDatabase.getInstance(context).habitDao()).rescheduleAll()
                WeeklySummary.schedule(context)
                MoodReminder.schedule(context)
                StreakGuard.schedule(context)
                SpreadReminders.rescheduleAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}

object HabitNotifications {
    const val CHANNEL_ID = "habit_reminders"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.reminder_channel_description)
            },
        )
    }

    fun show(context: Context, habitId: Long, name: String, emoji: String, repeatMinutes: Int? = null, plan: String = "", amountUnit: String? = null) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val openIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val completeIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            Intent(context, HabitReminderReceiver::class.java).apply {
                action = HabitReminderReceiver.ACTION_COMPLETE
                putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji $name")
            .setContentText(plan.ifBlank { context.getString(R.string.reminder_notification_text) })
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification),
                    context.getString(R.string.complete_habit),
                    completeIntent,
                ).build(),
            )
        if (amountUnit != null) {
            // Type how much straight into the notification, e.g. "3" glasses.
            val amountIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() xor 0x08000000,
                Intent(context, HabitReminderReceiver::class.java).apply {
                    action = HabitReminderReceiver.ACTION_ADD_AMOUNT
                    putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            val unitLabel = amountUnit.trim()
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification),
                    context.getString(R.string.notif_enter_amount),
                    amountIntent,
                ).addRemoteInput(
                    android.app.RemoteInput.Builder(HabitReminderReceiver.KEY_AMOUNT)
                        .setLabel(if (unitLabel.isEmpty()) context.getString(R.string.notif_amount_hint_plain) else context.getString(R.string.notif_amount_hint, unitLabel))
                        .build(),
                ).build(),
            )
        }
        if (repeatMinutes != null) {
            val snoozeIntent = PendingIntent.getBroadcast(
                context,
                habitId.hashCode() xor 0x20000000,
                Intent(context, HabitReminderSnoozeReceiver::class.java).apply {
                    putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, habitId)
                    putExtra(HabitReminderSnoozeReceiver.EXTRA_MINUTES, repeatMinutes)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(context, R.drawable.ic_notification),
                    context.getString(R.string.remind_again_minutes, repeatMinutes),
                    snoozeIntent,
                ).build(),
            )
        }
        context.getSystemService(NotificationManager::class.java).notify(habitId.hashCode(), builder.build())
    }
}

class HabitReminderSnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        if (habitId < 0 || minutes <= 0) return
        HabitReminderScheduler(context, TsuzukuDatabase.getInstance(context).habitDao()).snooze(habitId, minutes)
        context.getSystemService(NotificationManager::class.java).cancel(habitId.hashCode())
    }

    companion object {
        const val EXTRA_MINUTES = "repeat_minutes"
    }
}
