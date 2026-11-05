package app.tsuzuku.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.CalendarDaySummary
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.TodayHabit
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    history: HabitHistorySnapshot,
    weekStartsMonday: Boolean,
    onToggle: (Long, LocalDate) -> Unit,
    onIncrement: (Long, LocalDate) -> Unit,
    onDecrement: (Long, LocalDate) -> Unit,
) {
    val today = LocalDate.now()
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var selectedText by rememberSaveable { mutableStateOf(today.toString()) }
    var showDaySheet by rememberSaveable { mutableStateOf(false) }
    val displayedMonth = YearMonth.parse(monthText)
    val selectedDate = LocalDate.parse(selectedText)
    val selectedHabits = history.habitsForDay(selectedDate)
    val selectedSummary = history.daySummary(selectedDate)
    val locale = LocalLocale.current.platformLocale
    val monthTitle = displayedMonth.atDay(1)
        .format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
        .replaceFirstChar { it.titlecase(locale) }
    val selectedTitle = selectedDate
        .format(DateTimeFormatter.ofPattern("d MMMM, EEEE", locale))
        .replaceFirstChar { it.titlecase(locale) }

    if (showDaySheet) {
        ModalBottomSheet(onDismissRequest = { showDaySheet = false }) {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(selectedTitle, style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.calendar_goal_fraction, selectedSummary.completedCount, selectedSummary.scheduledCount),
                    color = MaterialTheme.colorScheme.secondary,
                )
                LinearProgressIndicator(progress = { selectedSummary.progressFraction }, modifier = Modifier.fillMaxWidth())
                if (selectedHabits.isEmpty()) {
                    Text(stringResource(R.string.no_habits_for_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    selectedHabits.forEach { item ->
                        CalendarHabitCard(
                            item = item,
                            editable = !selectedDate.isAfter(today),
                            onToggle = { onToggle(item.habit.id, selectedDate) },
                            onIncrement = { onIncrement(item.habit.id, selectedDate) },
                            onDecrement = { onDecrement(item.habit.id, selectedDate) },
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.calendar), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.calendar_history_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(shape = RoundedCornerShape(10.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = {
                            val previous = displayedMonth.minusMonths(1)
                            monthText = previous.toString()
                            selectedText = previous.atDay(1).toString()
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.previous_month))
                        }
                        Text(monthTitle, style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = {
                            val next = displayedMonth.plusMonths(1)
                            if (!next.atDay(1).isAfter(today)) {
                                monthText = next.toString()
                                selectedText = if (next == YearMonth.from(today)) today.toString() else next.atDay(1).toString()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next_month))
                        }
                    }
                    CalendarMonthGrid(
                        month = displayedMonth,
                        weekStartsMonday = weekStartsMonday,
                        selectedDate = selectedDate,
                        today = today,
                        summaryFor = history::daySummary,
                        onSelect = { selectedText = it.toString(); showDaySheet = true },
                    )
                }
            }
        }
        item {
            Surface(onClick = { showDaySheet = true }, shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(selectedTitle, style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (selectedDate == today) stringResource(R.string.today) else stringResource(R.string.selected_day),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            stringResource(
                                R.string.calendar_goal_fraction,
                                selectedSummary.completedCount,
                                selectedSummary.scheduledCount,
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    LinearProgressIndicator(
                        progress = { selectedSummary.progressFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (!selectedDate.isAfter(today)) {
                        Text(
                            stringResource(R.string.past_edit_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}

@Composable
private fun CalendarMonthGrid(
    month: YearMonth,
    weekStartsMonday: Boolean,
    selectedDate: LocalDate,
    today: LocalDate,
    summaryFor: (LocalDate) -> CalendarDaySummary,
    onSelect: (LocalDate) -> Unit,
) {
    val mondayFirstWeekdays = stringArrayResource(R.array.weekday_abbreviations).toList()
    val weekdays = if (weekStartsMonday) {
        mondayFirstWeekdays
    } else {
        listOf(mondayFirstWeekdays.last()) + mondayFirstWeekdays.dropLast(1)
    }
    Row(Modifier.fillMaxWidth()) {
        weekdays.forEach { day ->
            Box(Modifier.weight(1f).padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                Text(day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val offset = if (weekStartsMonday) {
        month.atDay(1).dayOfWeek.value - 1
    } else {
        month.atDay(1).dayOfWeek.value % 7
    }
    val cells = List<LocalDate?>(offset) { null } +
        (1..month.lengthOfMonth()).map(month::atDay)
    cells.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth()) {
            (week + List(7 - week.size) { null }).forEach { date ->
                if (date == null) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                } else {
                    val summary = summaryFor(date)
                    CalendarDayCell(
                        date = date,
                        summary = summary,
                        selected = date == selectedDate,
                        today = date == today,
                        enabled = !date.isAfter(today),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    summary: CalendarDaySummary,
    selected: Boolean,
    today: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = when {
        summary.progressFraction >= 1f -> MaterialTheme.colorScheme.primary
        summary.progressFraction > 0f -> MaterialTheme.colorScheme.primaryContainer
        summary.scheduledCount > 0 -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val contentColor = if (summary.progressFraction >= 1f) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, shape) else Modifier)
            .then(if (today && !selected) Modifier.border(1.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else 0.35f),
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = shape, color = color, contentColor = contentColor, modifier = Modifier.fillMaxSize()) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(date.dayOfMonth.toString(), fontWeight = if (today) FontWeight.Bold else FontWeight.Normal)
                    if (summary.scheduledCount > 0) {
                        Text(
                            "${summary.completedCount}/${summary.scheduledCount}",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHabitCard(
    item: TodayHabit,
    editable: Boolean,
    onToggle: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text(item.habit.emoji, modifier = Modifier.padding(11.dp), style = MaterialTheme.typography.titleLarge)
            }
            Column(Modifier.weight(1f)) {
                Text(item.habit.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (item.habit.type == HabitType.Check) {
                        stringResource(if (item.isComplete) R.string.habit_completed else R.string.pending_label)
                    } else {
                        stringResource(
                            R.string.content_progress_value,
                            formatLibraryAmount(item.progress),
                            formatLibraryAmount(item.habit.target),
                            item.habit.unit,
                        )
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.habit.type == HabitType.Check) {
                FilledIconButton(onClick = onToggle, enabled = editable) {
                    Icon(Icons.Rounded.Check, contentDescription = stringResource(R.string.complete_habit))
                }
            } else {
                OutlinedIconButton(onClick = onDecrement, enabled = editable && item.progress > 0) {
                    Icon(Icons.Rounded.Remove, contentDescription = stringResource(R.string.decrease_progress))
                }
                FilledIconButton(onClick = onIncrement, enabled = editable) {
                    Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.increase_progress))
                }
            }
        }
    }
}
