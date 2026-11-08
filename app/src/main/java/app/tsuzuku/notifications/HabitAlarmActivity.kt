package app.tsuzuku.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.tsuzuku.R
import app.tsuzuku.ui.theme.TsuzukuTheme
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The full-screen page an alarm habit opens, over the lock screen. */
class HabitAlarmActivity : ComponentActivity() {
    private var habitId = -1L
    private val closer = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
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
        habitId = intent.getLongExtra(HabitReminderReceiver.EXTRA_HABIT_ID, -1L)
        // A notification button that opens this page with an answer already chosen.
        intent.getStringExtra(EXTRA_ANSWER)?.let { answer(it); return }
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        val emoji = intent.getStringExtra(EXTRA_EMOJI).orEmpty()
        val plan = intent.getStringExtra(EXTRA_PLAN).orEmpty()
        val time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        setContent {
            TsuzukuTheme(darkTheme = true) {
                AlarmPage(time, emoji, name, plan, onAnswer = ::answer)
            }
        }
    }

    private fun answer(kind: String) {
        if (habitId >= 0) HabitAlarms.answer(this, habitId, kind)
        finish()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(closer) }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EMOJI = "emoji"
        const val EXTRA_PLAN = "plan"
        const val EXTRA_ANSWER = "answer"
        const val ACTION_CLOSE = "app.tsuzuku.action.ALARM_CLOSE"
    }
}

@Composable
internal fun AlarmPage(time: String, emoji: String, name: String, plan: String, onAnswer: (String) -> Unit) {
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text(time, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onSurface)
        Text(
            stringResource(R.string.alarm_time_to_start),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(40.dp))
        Text(emoji, fontSize = 72.sp)
        Spacer(Modifier.height(12.dp))
        Text(name, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
        if (plan.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(plan, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.weight(1.4f))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onAnswer(HabitAlarms.ACTION_START) }, modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Text(stringResource(R.string.alarm_start), style = MaterialTheme.typography.titleMedium)
            }
            FilledTonalButton(onClick = { onAnswer(HabitAlarms.ACTION_SNOOZE) }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.alarm_snooze, HabitAlarms.SNOOZE_MINUTES))
            }
            TextButton(onClick = { onAnswer(HabitAlarms.ACTION_DISMISS) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.alarm_not_today))
            }
        }
    }
}
