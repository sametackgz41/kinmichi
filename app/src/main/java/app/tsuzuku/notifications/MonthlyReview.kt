package app.tsuzuku.notifications

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import app.tsuzuku.HomeActivity
import app.tsuzuku.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.format.TextStyle

/** On the 1st of the month at 10.00, one note: last month's review is ready. */
internal fun nextMonthReviewTime(now: ZonedDateTime): ZonedDateTime {
    val thisMonth = now.withDayOfMonth(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
    return if (thisMonth.isAfter(now)) thisMonth else thisMonth.plusMonths(1)
}

object MonthlyReview {
    private const val REQUEST_CODE = 0x40E0
    private const val NOTIFICATION_ID = 0x40E0
    const val ACTION_SHOW = "app.tsuzuku.action.MONTH_REVIEW_SHOW"

    fun schedule(context: Context) {
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextMonthReviewTime(ZonedDateTime.now()).toInstant().toEpochMilli(),
            PendingIntent.getBroadcast(
                context, REQUEST_CODE,
                Intent(context, MonthlyReviewReceiver::class.java).setAction(ACTION_SHOW),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    internal fun show(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        HabitNotifications.createChannel(context)
        val locale = context.resources.configuration.locales[0]
        val month = YearMonth.from(LocalDate.now()).minusMonths(1).month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
            .replaceFirstChar { it.titlecase(locale) }
        val open = PendingIntent.getActivity(
            context, REQUEST_CODE,
            Intent(context, HomeActivity::class.java).setAction(HomeActivity.ACTION_MONTH_REVIEW).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, HabitNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.month_entry_title, month))
            .setContentText(context.getString(R.string.month_notif_body))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }
}

class MonthlyReviewReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == MonthlyReview.ACTION_SHOW) MonthlyReview.show(context)
        MonthlyReview.schedule(context)
    }
}
