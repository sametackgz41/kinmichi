package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.notifications.HabitTimer
import app.tsuzuku.ui.HabitTimerState

/** The running timer, at the bottom of every page like a music player. */
@Composable
internal fun HabitTimerBar(
    timer: HabitTimerState,
    onPauseResume: () -> Unit,
    onStop: () -> Unit,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shadowElevation = 3.dp,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column {
            Row(Modifier.padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(timer.emoji, fontSize = 22.sp)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(timer.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (timer.paused) {
                            stringResource(R.string.timer_paused, HabitTimer.clock(timer.elapsedSeconds), HabitTimer.clock(timer.maxSeconds))
                        } else {
                            "${HabitTimer.clock(timer.elapsedSeconds)} / ${HabitTimer.clock(timer.maxSeconds)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                IconButton(onClick = onPauseResume) {
                    Icon(
                        if (timer.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        stringResource(if (timer.paused) R.string.timer_resume else R.string.timer_pause),
                    )
                }
                IconButton(onClick = onStop) { Icon(Icons.Rounded.Stop, stringResource(R.string.timer_finish)) }
            }
            Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 6.dp)) {
                LinearProgressIndicator(
                    progress = { if (timer.maxSeconds <= 0) 0f else (timer.elapsedSeconds.toFloat() / timer.maxSeconds).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                )
            }
        }
    }
}
