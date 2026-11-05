package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.ActivityHeatmapDay
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitPeriodStat
import app.tsuzuku.model.LibraryItem
import app.tsuzuku.model.MonthlySummary
import app.tsuzuku.model.PersonalMilestone
import app.tsuzuku.model.PersonalRecords
import app.tsuzuku.model.SkipReasonType
import app.tsuzuku.model.WeeklySummary
import app.tsuzuku.model.YearlySummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class StatisticsTab(@param:StringRes val labelRes: Int) {
    General(R.string.stats_tab_general),
    Heatmap(R.string.stats_tab_heatmap),
    Reviews(R.string.stats_tab_reviews),
    Records(R.string.stats_tab_records),
    SkipReasons(R.string.stats_tab_skips),
}

enum class ReviewSubTab(@param:StringRes val labelRes: Int) {
    Weekly(R.string.weekly),
    Monthly(R.string.monthly),
    Yearly(R.string.yearly),
}

@Composable
fun StatisticsScreen(
    history: HabitHistorySnapshot,
    libraryItems: List<LibraryItem>,
    heatmapDays: List<ActivityHeatmapDay> = emptyList(),
    selectedHeatmapHabitId: Long? = null,
    onSelectHeatmapHabitId: (Long?) -> Unit = {},
    weeklySummary: WeeklySummary? = null,
    onPreviousWeek: () -> Unit = {},
    onNextWeek: () -> Unit = {},
    onSaveWeeklyNote: (LocalDate, String) -> Unit = { _, _ -> },
    monthlySummary: MonthlySummary? = null,
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSaveMonthlyNote: (YearMonth, String) -> Unit = { _, _ -> },
    yearlySummary: YearlySummary? = null,
    onPreviousYear: () -> Unit = {},
    onNextYear: () -> Unit = {},
    onSaveYearlyNote: (Int, String) -> Unit = { _, _ -> },
    personalRecords: PersonalRecords? = null,
    skipReasonStats: Map<SkipReasonType, Int> = emptyMap(),
    initialTab: StatisticsTab = StatisticsTab.General,
) {
    var currentTab by rememberSaveable(initialTab) { mutableStateOf(initialTab) }
    var reviewSubTab by rememberSaveable { mutableStateOf(ReviewSubTab.Weekly) }

    // General tab filters
    var days by rememberSaveable { mutableIntStateOf(30) }
    var selectedTag by rememberSaveable { mutableStateOf("") }
    var libraryCategory by rememberSaveable { mutableStateOf("") }

    val stats = history.statistics(days)
    val availableTags = stats.habits.flatMap { it.habit.tags.split(',') }.map(String::trim).filter(String::isNotBlank).distinct().sorted()
    val visibleHabitStats = if (selectedTag.isBlank()) stats.habits else stats.habits.filter { stat -> stat.habit.tags.split(',').any { it.trim().equals(selectedTag, true) } }
    val filteredScheduled = visibleHabitStats.sumOf(HabitPeriodStat::scheduledCount)
    val filteredCompleted = visibleHabitStats.sumOf(HabitPeriodStat::completedCount)
    val filteredRate = if (filteredScheduled == 0) 0f else filteredCompleted.toFloat() / filteredScheduled
    val libraryCategories = libraryItems.map { it.content.category }.distinct()
    val visibleLibrary = if (libraryCategory.isBlank()) libraryItems else libraryItems.filter { it.content.category.name == libraryCategory }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.statistics), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.statistics_history_subtitle), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatisticsTab.entries.forEach { tab ->
                    FilterChip(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        }

        when (currentTab) {
            StatisticsTab.General -> {
                if (availableTags.isNotEmpty()) {
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = selectedTag.isBlank(), onClick = { selectedTag = "" }, label = { Text(stringResource(R.string.all_label)) })
                            availableTags.forEach { tag -> FilterChip(selected = selectedTag == tag, onClick = { selectedTag = tag }, label = { Text("#$tag") }) }
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(7, 30, 90).forEach { option ->
                            FilterChip(
                                selected = days == option,
                                onClick = { days = option },
                                label = { Text(stringResource(R.string.last_days, option)) },
                            )
                        }
                    }
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(22.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text(stringResource(R.string.completion_rate), style = MaterialTheme.typography.titleMedium)
                                Text("%${(filteredRate * 100).toInt()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                                Text(
                                    stringResource(R.string.completed_scheduled, filteredCompleted, filteredScheduled),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { filteredRate },
                                    modifier = Modifier.size(96.dp),
                                    strokeWidth = 10.dp,
                                )
                                Text("%${(filteredRate * 100).toInt()}", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        StatisticMiniCard(value = stats.activeDays.toString(), label = stringResource(R.string.active_days), modifier = Modifier.weight(1f))
                        StatisticMiniCard(value = stats.currentStreak.toString(), label = stringResource(R.string.current_streak), modifier = Modifier.weight(1f))
                        StatisticMiniCard(value = stats.bestStreak.toString(), label = stringResource(R.string.best_streak), modifier = Modifier.weight(1f))
                    }
                }
                item { Text(stringResource(R.string.habit_breakdown), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                if (visibleHabitStats.isEmpty()) {
                    item { Text(stringResource(R.string.no_statistics_yet), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(visibleHabitStats.size) { index ->
                        HabitStatisticCard(visibleHabitStats[index])
                    }
                }
                item { Text(stringResource(R.string.library_summary), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) }
                if (libraryCategories.size > 1) {
                    item {
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = libraryCategory.isBlank(), onClick = { libraryCategory = "" }, label = { Text(stringResource(R.string.all_label)) })
                            libraryCategories.forEach { category -> FilterChip(selected = libraryCategory == category.name, onClick = { libraryCategory = category.name }, label = { Text(stringResource(categoryLabelRes(category))) }) }
                        }
                    }
                }
                item { LibraryStatisticCard(visibleLibrary) }
            }

            StatisticsTab.Heatmap -> {
                item {
                    HeatmapCalendarView(
                        days = heatmapDays,
                        habits = history.habits,
                        selectedHabitId = selectedHeatmapHabitId,
                        onSelectHabit = onSelectHeatmapHabitId,
                    )
                }
            }

            StatisticsTab.Reviews -> {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        ReviewSubTab.entries.forEach { subTab ->
                            FilterChip(
                                selected = reviewSubTab == subTab,
                                onClick = { reviewSubTab = subTab },
                                label = { Text(stringResource(subTab.labelRes)) },
                            )
                        }
                    }
                }

                when (reviewSubTab) {
                    ReviewSubTab.Weekly -> {
                        weeklySummary?.let { summary ->
                            item {
                                val locale = LocalConfiguration.current.locales[0]
                                val endDay = summary.weekStart.plusDays(6)
                                val weekRange = remember(summary.weekStart, locale) {
                                    val f = DateTimeFormatter.ofPattern("d MMM", locale)
                                    "${summary.weekStart.format(f)} - ${endDay.format(f)} ${endDay.year}"
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = onPreviousWeek) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.previous_week))
                                    }
                                    Text(weekRange, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    IconButton(onClick = onNextWeek) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next_week))
                                    }
                                }
                            }
                            item {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(stringResource(R.string.weekly_completion_rate), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("%${(summary.habitCompletionRate * 100).toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                        }
                                        summary.changeRatePercentage?.let { change ->
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (change >= 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.vs_last_week, if (change >= 0) "+$change" else "$change"),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(stringResource(R.string.most_successful), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            Text(
                                                summary.bestHabit?.let { "${it.habit.emoji} ${it.habit.name}" } ?: "—",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                    Card(modifier = Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) {
                                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(stringResource(R.string.could_improve), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                            Text(
                                                summary.strugglingHabit?.let { "${it.habit.emoji} ${it.habit.name}" } ?: "—",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }
                                }
                            }
                            item {
                                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)) {
                                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(stringResource(R.string.task_focus_balance), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.completed_tasks_of, summary.completedTasksCount, summary.plannedTasksCount), style = MaterialTheme.typography.bodySmall)
                                            Text(stringResource(R.string.postponed_count, summary.carriedTasksCount), style = MaterialTheme.typography.bodySmall)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.focus_duration_minutes, summary.focusDurationMinutes), style = MaterialTheme.typography.bodySmall)
                                            Text(stringResource(R.string.goal_steps_count, summary.goalProgressCount), style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                            item {
                                ReviewNoteCard(
                                    initialNote = summary.note,
                                    label = stringResource(R.string.weekly_reflection_note),
                                    onSave = { onSaveWeeklyNote(summary.weekStart, it) },
                                )
                            }
                        }
                    }

                    ReviewSubTab.Monthly -> {
                        monthlySummary?.let { summary ->
                            item {
                                val locale = LocalConfiguration.current.locales[0]
                                val monthName = remember(summary.yearMonth, locale) {
                                    summary.yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = onPreviousMonth) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.previous_month))
                                    }
                                    Text(monthName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    IconButton(onClick = onNextMonth) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next_month))
                                    }
                                }
                            }
                            item {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Column {
                                                Text(stringResource(R.string.monthly_success_rate), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("%${(summary.habitCompletionRate * 100).toInt()}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(stringResource(R.string.focus_hours_minutes, summary.focusDurationMinutes / 60, summary.focusDurationMinutes % 60), style = MaterialTheme.typography.bodyMedium)
                                                Text(stringResource(R.string.completed_tasks_value, summary.completedTasksCount), style = MaterialTheme.typography.bodyMedium)
                                            }
                                        }
                                    }
                                }
                            }
                            if (summary.topHabits.isNotEmpty()) {
                                item {
                                    Text(stringResource(R.string.habit_ranking), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                                items(summary.topHabits.take(5)) { habitStat ->
                                    HabitStatisticCard(habitStat)
                                }
                            }
                            item {
                                ReviewNoteCard(
                                    initialNote = summary.note,
                                    label = stringResource(R.string.monthly_reflection),
                                    onSave = { onSaveMonthlyNote(summary.yearMonth, it) },
                                )
                            }
                        }
                    }

                    ReviewSubTab.Yearly -> {
                        yearlySummary?.let { summary ->
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    IconButton(onClick = onPreviousYear) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.previous_year))
                                    }
                                    Text(summary.year.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                    IconButton(onClick = onNextYear) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = stringResource(R.string.next_year))
                                    }
                                }
                            }
                            item {
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text(stringResource(R.string.yearly_summary_milestones), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.total_habits_label), style = MaterialTheme.typography.bodyMedium)
                                            Text("${summary.totalHabitCompletions} kez", fontWeight = FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.total_focus_label), style = MaterialTheme.typography.bodyMedium)
                                            Text("${summary.totalFocusMinutes / 60} saat", fontWeight = FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.completed_goals_label), style = MaterialTheme.typography.bodyMedium)
                                            Text("${summary.completedGoalsCount}", fontWeight = FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.finished_books_content_label), style = MaterialTheme.typography.bodyMedium)
                                            Text("${summary.completedLibraryItemsCount}", fontWeight = FontWeight.Bold)
                                        }
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text(stringResource(R.string.longest_chain_label), style = MaterialTheme.typography.bodyMedium)
                                            Text(stringResource(R.string.days_value, summary.bestStreakDays), fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            item {
                                ReviewNoteCard(
                                    initialNote = summary.note,
                                    label = stringResource(R.string.yearly_review_vision),
                                    onSave = { onSaveYearlyNote(summary.year, it) },
                                )
                            }
                        }
                    }
                }
            }

            StatisticsTab.Records -> {
                personalRecords?.let { records ->
                    item {
                        Text(stringResource(R.string.personal_records), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecordTile(
                                    icon = "🏆",
                                    title = stringResource(R.string.longest_chain),
                                    value = stringResource(R.string.days_value_records, records.longestHabitStreak),
                                    subtitle = records.longestHabitName,
                                    modifier = Modifier.weight(1f),
                                )
                                RecordTile(
                                    icon = "⚡",
                                    title = stringResource(R.string.most_productive_day),
                                    value = records.mostProductiveDayName,
                                    subtitle = "${records.mostProductiveDayCount} tamamlama",
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                RecordTile(
                                    icon = "⏱️",
                                    title = stringResource(R.string.total_focus),
                                    value = "${records.totalFocusHours} saat",
                                    subtitle = stringResource(R.string.deep_work),
                                    modifier = Modifier.weight(1f),
                                )
                                RecordTile(
                                    icon = "🎯",
                                    title = stringResource(R.string.goals_achieved),
                                    value = "${records.totalGoalsCompleted}",
                                    subtitle = stringResource(R.string.content_finished_count, records.totalLibraryCompleted),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(stringResource(R.string.milestones_and_badges), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    }
                    items(records.milestones) { milestone ->
                        MilestoneBadgeCard(milestone)
                    }
                }
            }

            StatisticsTab.SkipReasons -> {
                val totalSkips = skipReasonStats.values.sum()
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(stringResource(R.string.habit_skip_analysis), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(R.string.habit_skip_analysis_desc, totalSkips),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(SkipReasonType.entries) { reason ->
                    val count = skipReasonStats[reason] ?: 0
                    val fraction = if (totalSkips > 0) count.toFloat() / totalSkips else 0f
                    Card(shape = RoundedCornerShape(10.dp)) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${reason.icon} ${reason.defaultLabel}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    "$count kez (%${(fraction * 100).toInt()})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun RecordTile(
    icon: String,
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(icon, fontSize = 20.sp)
            Text(title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MilestoneBadgeCard(milestone: PersonalMilestone) {
    val progress = (milestone.currentValue.toFloat() / milestone.targetValue).coerceIn(0f, 1f)
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (milestone.isUnlocked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (milestone.isUnlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(milestone.icon, fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(milestone.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    if (milestone.isUnlocked) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primary) {
                            Text(
                                stringResource(R.string.earned_label),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    } else {
                        Text(
                            "${milestone.currentValue} / ${milestone.targetValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(milestone.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewNoteCard(
    initialNote: String,
    label: String,
    onSave: (String) -> Unit,
) {
    var text by remember(initialNote) { mutableStateOf(initialNote) }
    var savedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(savedMessage) {
        if (savedMessage) {
            kotlinx.coroutines.delay(2000)
            savedMessage = false
        }
    }

    Card(shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.reflection_placeholder)) },
                minLines = 3,
                maxLines = 6,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (savedMessage) {
                    Text(stringResource(R.string.saved_check), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                Button(onClick = {
                    onSave(text)
                    savedMessage = true
                }) {
                    Text(stringResource(R.string.save_note))
                }
            }
        }
    }
}

@Composable
private fun StatisticMiniCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HabitStatisticCard(stat: HabitPeriodStat) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stat.habit.emoji, style = MaterialTheme.typography.titleLarge)
                    Column {
                        Text(stat.habit.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.completed_scheduled, stat.completedCount, stat.scheduledCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text("%${(stat.completionRate * 100).toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            LinearProgressIndicator(progress = { stat.completionRate }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape))
            Text(
                stringResource(R.string.streak_pair, stat.currentStreak, stat.bestStreak),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryStatisticCard(items: List<LibraryItem>) {
    val completed = items.count { it.content.status == ContentStatus.Completed }
    val inProgress = items.count { it.content.status == ContentStatus.InProgress }
    Card(shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(items.size.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.total_content), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.library_status_counts, inProgress, completed), style = MaterialTheme.typography.titleMedium)
                    Text(
                        pluralStringResource(R.plurals.category_count, items.map { it.content.category }.distinct().size, items.map { it.content.category }.distinct().size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (items.isNotEmpty()) {
                items.groupingBy { it.content.category }.eachCount().entries
                    .sortedByDescending { it.value }
                    .forEach { (category, count) ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${categoryEmoji(category)} ${stringResource(categoryLabelRes(category))}")
                            Text(count.toString(), fontWeight = FontWeight.Bold)
                        }
                    }
            } else {
                Text(stringResource(R.string.library_empty_statistics), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
