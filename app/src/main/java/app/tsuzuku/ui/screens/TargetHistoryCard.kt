package app.tsuzuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Each target the habit has had and when, newest first; past days were counted with their own. */
@Composable
internal fun TargetHistoryCard(habit: Habit) {
    val locale = LocalConfiguration.current.locales[0]
    val fmt = DateTimeFormatter.ofPattern("d MMM", locale)
    val start = habit.activeStartEpochDay?.let(LocalDate::ofEpochDay) ?: habit.createdOn
    // Periods oldest first: [from, until] with the target of that time.
    val changes = habit.targetHistory.sortedBy { it.changedOn }
    val periods = buildList {
        var from = start
        changes.forEach { change ->
            add(Triple(from, change.changedOn.minusDays(1), change.oldTarget))
            from = change.changedOn
        }
        add(Triple(from, null, habit.target))
    }.reversed()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.target_history_title), style = MaterialTheme.typography.titleMedium)
        periods.forEachIndexed { i, (from, until, target) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(12.dp).clip(CircleShape)
                        .background(if (i == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
                Column(Modifier.padding(start = 14.dp)) {
                    Text(
                        if (habit.type == HabitType.Check) formatHabitAmount(target) else "${formatHabitAmount(target)} ${habit.unit}".trim(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        if (until == null) stringResource(R.string.target_history_since, from.format(fmt))
                        else "${from.format(fmt)} – ${until.format(fmt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Text(
            stringResource(R.string.target_history_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
