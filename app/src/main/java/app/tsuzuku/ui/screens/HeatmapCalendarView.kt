package app.tsuzuku.ui.screens

import app.tsuzuku.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.ActivityHeatmapDay
import app.tsuzuku.model.Habit
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun HeatmapCalendarView(
    days: List<ActivityHeatmapDay>,
    habits: List<Habit> = emptyList(),
    selectedHabitId: Long? = null,
    onSelectHabit: (Long?) -> Unit = {},
) {
    var selectedDay by remember { mutableStateOf<ActivityHeatmapDay?>(null) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState(Int.MAX_VALUE) // Start scrolled to current day

    // Group days by week columns (Monday-based)
    val groupedWeeks = remember(days) {
        if (days.isEmpty()) emptyList()
        else {
            val sorted = days.sortedBy { it.date }
            val firstDate = sorted.first().date
            val startMonday = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val daysMap = sorted.associateBy { it.date }

            val weeks = mutableListOf<List<ActivityHeatmapDay>>()
            var currentMonday = startMonday
            val lastDate = sorted.last().date

            while (!currentMonday.isAfter(lastDate)) {
                val week = (0..6).map { offset ->
                    val date = currentMonday.plusDays(offset.toLong())
                    daysMap[date] ?: ActivityHeatmapDay(
                        date = date,
                        intensity = 0,
                        habitsCompleted = 0,
                        tasksCompleted = 0,
                        focusMinutes = 0,
                        dailyReviewDone = false,
                    )
                }
                weeks.add(week)
                currentMonday = currentMonday.plusWeeks(1)
            }
            weeks
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = selectedHabitId == null,
                onClick = { onSelectHabit(null) },
                label = { Text(stringResource(R.string.overall_activity)) },
            )
            habits.forEach { habit ->
                FilterChip(
                    selected = selectedHabitId == habit.id,
                    onClick = { onSelectHabit(habit.id) },
                    label = { Text("${habit.emoji} ${habit.name}") },
                )
            }
        }

        // Grid container
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
        ) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.heatmap_mark),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        (0..4).forEach { lvl ->
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(heatmapColorForIntensity(lvl, primaryColor, MaterialTheme.colorScheme.surfaceVariant)),
                            )
                        }
                        Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Heatmap matrix
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Day of week labels (Mon, Wed, Fri, Sun) in the app's locale.
                    val dayLabelLocale = LocalLocale.current.platformLocale
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        DayOfWeek.entries.forEachIndexed { index, day ->
                            Box(
                                modifier = Modifier.height(14.dp),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (index % 2 == 0) {
                                    Text(
                                        day.getDisplayName(java.time.format.TextStyle.SHORT, dayLabelLocale),
                                        style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }

                    // Weeks columns
                    groupedWeeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            week.forEach { day ->
                                val isSelected = selectedDay?.date == day.date
                                val isToday = day.date == LocalDate.now()
                                val color = heatmapColorForIntensity(
                                    day.intensity,
                                    primaryColor,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(3.dp))
                                            } else if (isToday) {
                                                Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
                                            } else {
                                                Modifier
                                            },
                                        )
                                        .clickable { selectedDay = day },
                                )
                            }
                        }
                    }
                }

                // Selected day detail card
                selectedDay?.let { day ->
                    val locale = LocalLocale.current.platformLocale
                    val formatted = day.date.format(DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", locale))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(formatted, style = MaterialTheme.typography.titleSmall)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.heatmap_habits_completed, day.habitsCompleted), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.heatmap_tasks, day.tasksCompleted), style = MaterialTheme.typography.bodySmall)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(stringResource(R.string.heatmap_focus_minutes, day.focusMinutes), style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (day.dailyReviewDone) stringResource(R.string.heatmap_review_done) else stringResource(R.string.heatmap_review_missing),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (day.dailyReviewDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun heatmapColorForIntensity(intensity: Int, primary: Color, emptyColor: Color): Color = when (intensity) {
    0 -> emptyColor
    1 -> primary.copy(alpha = 0.25f)
    2 -> primary.copy(alpha = 0.50f)
    3 -> primary.copy(alpha = 0.75f)
    else -> primary
}
