package app.tsuzuku.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import app.tsuzuku.R
import app.tsuzuku.data.RoomHabitRepository
import app.tsuzuku.data.local.TsuzukuDatabase
import app.tsuzuku.model.TodayHabit
import app.tsuzuku.data.preferences.AppLocaleContext
import app.tsuzuku.ui.theme.TsuzukuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The full-screen alarm, over the lock screen: ringing page, then the task (if any), then a
 * good-morning page. Volume keys and Back do nothing while it rings.
 */
class HabitAlarmActivity : ComponentActivity() {
    private enum class Stage { Ringing, Mission, Morning }

    private var habitId = -1L
    private var stage by mutableStateOf(Stage.Ringing)
    private var morningHabits by mutableStateOf<List<TodayHabit>>(emptyList())
    private var streak by mutableStateOf<Int?>(null)
    private var snoozesLeft by mutableIntStateOf(0)

    private val closer = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (stage != Stage.Morning) finish()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ContextCompat.registerReceiver(this, closer, IntentFilter(ACTION_CLOSE), ContextCompat.RECEIVER_NOT_EXPORTED)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (stage == Stage.Morning) startDay()
            }
        })
        habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        val options = HabitAlarms.options(this, habitId)
        snoozesLeft = HabitAlarms.snoozesLeft(this, habitId)
        // Opened from the notification after the ringing already ended: nothing left to answer.
        if (AlarmRingService.ringingHabitId != habitId && savedInstanceState == null && AlarmRingService.ringingHabitId >= 0) {
            finish(); return
        }
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val emoji = intent.getStringExtra(EXTRA_EMOJI).orEmpty()
        val plan = intent.getStringExtra(EXTRA_PLAN).orEmpty()
        val date = LocalDate.now().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(resources.configuration.locales[0]))
        setContent {
            TsuzukuTheme(darkTheme = true) {
                when (stage) {
                    Stage.Ringing -> AlarmRingingPage(
                        time = LocalTime.now().let { app.tsuzuku.ui.formatClock(it.hour * 60 + it.minute) },
                        date = date,
                        emoji = emoji,
                        name = name,
                        plan = plan,
                        mission = options.mission,
                        snoozesLeft = snoozesLeft,
                        onStart = {
                            if (options.mission == AlarmMission.None) wakeUp() else {
                                HabitAlarms.hush(this)
                                stage = Stage.Mission
                            }
                        },
                        onSnooze = {
                            HabitAlarms.snooze(this, habitId)
                            finish()
                        },
                        onNotToday = {
                            HabitAlarms.stop(this, habitId)
                            finish()
                        },
                    )
                    Stage.Mission -> {
                        val progress = { HabitAlarms.hush(this) }
                        // Any task choice means the one mixed verification.
                        if (options.mission == AlarmMission.None) {
                            androidx.compose.runtime.LaunchedEffect(Unit) { wakeUp() }
                        } else {
                            VerificationFlow(onProgress = progress, onSolved = ::wakeUp)
                        }
                    }
                    Stage.Morning -> MorningPage(
                        greeting = stringResource(if (LocalTime.now().hour < 12) R.string.alarm_good_morning else R.string.alarm_hello),
                        date = date,
                        streak = streak,
                        habits = morningHabits,
                        onStartDay = ::startDay,
                    )
                }
            }
        }
    }

    /** Answered for good: quiet, tick off the wake-up habit, show the day ahead. */
    private fun wakeUp() {
        if (stage == Stage.Morning) return
        stage = Stage.Morning
        val id = habitId
        lifecycleScope.launch {
            val today = withContext(Dispatchers.IO) {
                HabitAlarms.finish(applicationContext, id)
                RoomHabitRepository(TsuzukuDatabase.getInstance(applicationContext).habitDao()).observeToday().first()
            }
            morningHabits = today.filterNot { it.habit.id == id }
            streak = today.firstOrNull { it.habit.id == id }?.takeIf { HabitAlarms.options(applicationContext, id).markDone }?.streakDays
        }
    }

    private fun startDay() {
        startActivity(Intent(this, app.tsuzuku.HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    // The volume keys must not silence or lower the alarm.
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        if (stage != Stage.Morning && keyCode in SWALLOWED_KEYS) true else super.onKeyDown(keyCode, event)

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean =
        if (stage != Stage.Morning && keyCode in SWALLOWED_KEYS) true else super.onKeyUp(keyCode, event)

    override fun onDestroy() {
        runCatching { unregisterReceiver(closer) }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EMOJI = "emoji"
        const val EXTRA_PLAN = "plan"
        const val ACTION_CLOSE = "app.tsuzuku.action.ALARM_CLOSE"
        private val SWALLOWED_KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
    }
}
