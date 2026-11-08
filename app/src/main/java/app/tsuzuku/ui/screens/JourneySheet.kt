package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.TodayHabit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.ceil
import kotlin.math.roundToInt

/** "Where are you?": a slider across the whole book, today's gain and a finish date at this pace. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JourneySheet(todayHabit: TodayHabit, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    if (todayHabit.habit.journeyTotal == null) return
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        JourneySheetContent(todayHabit, onSave, onDismiss)
    }
}

@Composable
internal fun JourneySheetContent(todayHabit: TodayHabit, onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    val habit = todayHabit.habit
    val total = habit.journeyTotal ?: return
    val before = todayHabit.journeyPosition
    var position by remember { mutableFloatStateOf(before.toFloat()) }
    val todayGain = (todayHabit.progress + (position - before)).coerceAtLeast(0.0)
    val locale = LocalConfiguration.current.locales[0]
    run {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("${habit.emoji} ${habit.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(R.string.journey_where), style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatHabitAmount(position.toDouble()), style = MaterialTheme.typography.displaySmall)
                Text(
                    "  / ${formatHabitAmount(total)} ${habit.unit}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp).weight(1f),
                )
                // Fine steps: the slider gets close, these land on the exact page.
                androidx.compose.material3.FilledTonalIconButton(onClick = { position = (position - 1).coerceAtLeast(0f) }) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Remove, stringResource(R.string.decrease_progress))
                }
                androidx.compose.material3.FilledTonalIconButton(onClick = { position = (position + 1).coerceAtMost(total.toFloat()) }) {
                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Rounded.Add, stringResource(R.string.increase_progress))
                }
            }
            Slider(
                value = position,
                onValueChange = { position = it.roundToInt().toFloat() },
                valueRange = 0f..total.toFloat(),
            )
            HorizontalDivider()
            Row {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.journey_today), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("+${formatHabitAmount(todayGain)} ${habit.unit}", style = MaterialTheme.typography.titleMedium)
                }
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.journey_finish_at), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val left = total - position
                    val pace = maxOf(todayHabit.journeyPace, habit.target.takeIf { todayHabit.journeyPace <= 0 } ?: 0.0)
                    Text(
                        when {
                            left <= 0 -> stringResource(R.string.journey_finished)
                            pace <= 0 -> "—"
                            else -> LocalDate.now().plusDays(ceil(left / pace).toLong())
                                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Button(onClick = { onSave(position.toDouble()); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.journey_save))
            }
        }
    }
}
