package app.tsuzuku.notifications

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import app.tsuzuku.R

/**
 * Plays the alarm. The sound comes from here, on the alarm stream at full volume, rather than
 * from the notification: the power and volume keys silence notification sounds, not this.
 */
class AlarmRingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var savedVolume = -1
    private var habitId = -1L
    private val handler = Handler(Looper.getMainLooper())
    private val audio by lazy { getSystemService(AudioManager::class.java) }
    private var hushedUntil = 0L
    private var startedAt = 0L
    private var rampUp = false

    /** How loud it should be now: full at once, or rising from a quarter over 30 seconds. */
    private fun targetVolume(): Int {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (!rampUp) return max
        val t = ((System.currentTimeMillis() - startedAt) / 30_000f).coerceIn(0f, 1f)
        return (max * (0.25f + 0.75f * t)).toInt().coerceIn(1, max)
    }

    /** Keeps the volume up and brings the sound back after a hush runs out. */
    private val keepLoud = object : Runnable {
        override fun run() {
            val want = targetVolume()
            if (audio.getStreamVolume(AudioManager.STREAM_ALARM) < want) audio.setStreamVolume(AudioManager.STREAM_ALARM, want, 0)
            val p = player
            if (p != null && !p.isPlaying && System.currentTimeMillis() >= hushedUntil) {
                p.start()
                startVibration()
            }
            handler.postDelayed(this, 1_000)
        }
    }
    private val giveUp = Runnable { stopSelf() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                val id = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
                if (id < 0 || id == habitId || habitId < 0) stopSelf()
                return START_NOT_STICKY
            }
            ACTION_HUSH -> {
                if (player == null) { stopSelf(); return START_NOT_STICKY }
                hushedUntil = System.currentTimeMillis() + intent.getIntExtra(EXTRA_SECONDS, 40) * 1_000L
                player?.takeIf { it.isPlaying }?.pause()
                vibrator?.cancel()
                return START_NOT_STICKY
            }
        }
        val id = intent?.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L) ?: -1L
        if (id < 0) { stopSelf(); return START_NOT_STICKY }
        val name = intent!!.getStringExtra(HabitAlarmActivity.EXTRA_NAME).orEmpty()
        val emoji = intent.getStringExtra(HabitAlarmActivity.EXTRA_EMOJI).orEmpty()
        val plan = intent.getStringExtra(HabitAlarmActivity.EXTRA_PLAN).orEmpty()
        habitId = id
        ringingHabitId = id
        startedAt = System.currentTimeMillis()
        rampUp = HabitAlarms.options(this, id).rampUp
        val notification = buildNotification(id, name, emoji, plan)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(HabitAlarms.notificationId(id), notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(HabitAlarms.notificationId(id), notification)
        }
        startSound()
        startVibration()
        handler.removeCallbacks(keepLoud)
        handler.post(keepLoud)
        handler.removeCallbacks(giveUp)
        handler.postDelayed(giveUp, GIVE_UP_MILLIS)
        // Over the lock screen the full-screen intent opens the page; try it directly too.
        runCatching { startActivity(HabitAlarms.pageIntent(this, id, name, emoji, plan)) }
        return START_NOT_STICKY
    }

    private fun buildNotification(id: Long, name: String, emoji: String, plan: String): Notification {
        val page = PendingIntent.getActivity(
            this,
            HabitAlarms.notificationId(id),
            HabitAlarms.pageIntent(this, id, name, emoji, plan),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val options = HabitAlarms.options(this, id)
        val builder = Notification.Builder(this, HabitAlarms.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$emoji $name")
            .setContentText(plan.ifBlank { getString(R.string.alarm_time_to_start) })
            .setCategory(Notification.CATEGORY_ALARM)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setFullScreenIntent(page, true)
            .setContentIntent(page)
        fun broadcast(kind: String) = PendingIntent.getBroadcast(
            this,
            HabitAlarms.notificationId(id) xor kind.hashCode(),
            Intent(this, HabitAlarmActionReceiver::class.java).setAction(kind).putExtra(HabitReminderReceiver.EXTRA_HABIT_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(label: String, intent: PendingIntent) = Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_notification), label, intent,
        ).build()
        builder.addAction(action(getString(if (options.mission == AlarmMission.None) R.string.alarm_start else R.string.alarm_start_mission), page))
        val left = HabitAlarms.snoozesLeft(this, id)
        if (left > 0) builder.addAction(action(getString(R.string.alarm_snooze_left, HabitAlarms.SNOOZE_MINUTES, left), broadcast(HabitAlarms.ACTION_SNOOZE)))
        if (options.mission == AlarmMission.None) builder.addAction(action(getString(R.string.alarm_not_today), broadcast(HabitAlarms.ACTION_DISMISS)))
        return builder.build()
    }

    private fun startSound() {
        if (player != null) return
        savedVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
        audio.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume(), 0)
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = runCatching {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        }.getOrNull()
    }

    private fun startVibration() {
        val v = vibrator ?: (if (Build.VERSION.SDK_INT >= 31) getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION") getSystemService(Vibrator::class.java)).also { vibrator = it }
        v.vibrate(
            VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0),
            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build(),
        )
    }

    override fun onDestroy() {
        ringingHabitId = -1L
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { stop(); release() }
        player = null
        vibrator?.cancel()
        if (savedVolume >= 0) runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, savedVolume, 0) }
        if (habitId >= 0) getSystemService(NotificationManager::class.java).cancel(HabitAlarms.notificationId(habitId))
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        /** The habit ringing right now, or -1. */
        @Volatile var ringingHabitId = -1L
            private set
        const val ACTION_STOP = "app.tsuzuku.action.ALARM_STOP"
        const val ACTION_HUSH = "app.tsuzuku.action.ALARM_HUSH"
        const val EXTRA_SECONDS = "seconds"
        /** Stop on its own after 20 minutes so a forgotten phone doesn't ring all day. */
        private const val GIVE_UP_MILLIS = 20 * 60_000L
    }
}
