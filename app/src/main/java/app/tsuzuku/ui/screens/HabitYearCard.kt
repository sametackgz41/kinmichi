package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HeatDay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields

private const val WEEKS = 53
private val CELL = 13.dp
private val GAP = 3.dp

/**
 * A habit's last 12 months at a glance: streak numbers on top, then one square per day
 * (columns are weeks) shaded by how much of the target was reached. Tap a day for details.
 */
@Composable
internal fun HabitYearCard(
    habit: Habit,
    records: List<HabitHistoryRecord>,
    today: LocalDate = LocalDate.now(),
) {
    val locale = LocalConfiguration.current.locales[0]
    val history = remember(habit, records) { HabitHistorySnapshot(listOf(habit), records) }
    val stat = remember(history, today) { history.statistics(365, today).habits.firstOrNull() }
    val streak = remember(history, today) { history.streak(habit, today) }

    val firstDayOfWeek = WeekFields.of(locale).firstDayOfWeek
    val gridStart = remember(today, firstDayOfWeek) {
        today.with(TemporalAdjusters.previousOrSame(firstDayOfWeek)).minusWeeks((WEEKS - 1).toLong())
    }
    val days = remember(history, gridStart, today) {
        (0 until WEEKS * 7).map { offset ->
            val date = gridStart.plusDays(offset.toLong())
            date to if (date.isAfter(today)) null else history.heatDay(habit, date)
        }
    }
    val doneCount = days.count { it.second == HeatDay.Done }
    var selected by remember(habit.id) { mutableStateOf<LocalDate?>(null) }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(vertical = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                YearStat(streak.current.toString(), stringResource(R.string.current_streak))
                YearStat(maxOf(stat?.bestStreak ?: 0, streak.best).toString(), stringResource(R.string.best_streak))
                YearStat("${((stat?.completionRate ?: 0f) * 100).toInt()}%", stringResource(R.string.completion_rate))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.year_heatmap_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            YearGrid(
                days = days,
                gridStart = gridStart,
                accent = Color(habit.colorArgb),
                selected = selected,
                onSelect = { selected = if (selected == it) null else it },
                summary = pluralStringResource(R.plurals.year_heatmap_summary, doneCount, doneCount),
                locale = locale,
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(targetState = selected, label = "heat-selected") { date ->
                val text = if (date == null) {
                    pluralStringResource(R.plurals.year_heatmap_summary, doneCount, doneCount)
                } else {
                    val day = days.firstOrNull { it.first == date }?.second
                    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)) +
                        " · " + heatDayLabel(day, habit)
                }
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun YearStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun YearGrid(
    days: List<Pair<LocalDate, HeatDay?>>,
    gridStart: LocalDate,
    accent: Color,
    selected: LocalDate?,
    onSelect: (LocalDate) -> Unit,
    summary: String,
    locale: java.util.Locale,
) {
    val scroll = rememberScrollState()
    // Open on the most recent weeks; older months are a swipe away.
    LaunchedEffect(scroll.maxValue) { scroll.scrollTo(scroll.maxValue) }
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) { reveal.animateTo(1f, tween(700)) }

    val empty = MaterialTheme.colorScheme.surfaceContainerHighest
    val off = MaterialTheme.colorScheme.surfaceContainer
    val outline = MaterialTheme.colorScheme.outline
    val highlight = MaterialTheme.colorScheme.onSurface
    val density = LocalDensity.current
    val cellPx = with(density) { CELL.toPx() }
    val stepPx = with(density) { (CELL + GAP).toPx() }
    val gridWidth = CELL * WEEKS + GAP * (WEEKS - 1)

    Box(Modifier.horizontalScroll(scroll).padding(horizontal = 16.dp)) {
        Column {
            // Month initials above the first week of each month.
            Box(Modifier.width(gridWidth).height(16.dp)) {
                for (week in 0 until WEEKS) {
                    val weekStart = gridStart.plusWeeks(week.toLong())
                    // Skip the last two weeks: there is no room left for a label there.
                    if (weekStart.dayOfMonth <= 7 && week < WEEKS - 2) {
                        Text(
                            weekStart.month.getDisplayName(TextStyle.SHORT, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = (CELL + GAP) * week),
                        )
                    }
                }
            }
            Canvas(
                Modifier
                    .size(gridWidth, CELL * 7 + GAP * 6)
                    .semantics { contentDescription = summary }
                    .pointerInput(gridStart) {
                        detectTapGestures { offset ->
                            val col = (offset.x / stepPx).toInt()
                            val row = (offset.y / stepPx).toInt()
                            if (col in 0 until WEEKS && row in 0..6) {
                                val index = col * 7 + row
                                if (days.getOrNull(index)?.second != null) onSelect(days[index].first)
                            }
                        }
                    },
            ) {
                val corner = CornerRadius(cellPx * 0.28f)
                days.forEachIndexed { index, (date, day) ->
                    if (day == null) return@forEachIndexed
                    val col = index / 7
                    // Cells fade in column by column, newest last.
                    val appear = ((reveal.value * WEEKS) - col * 0.6f).coerceIn(0f, 1f)
                    if (appear <= 0f) return@forEachIndexed
                    val topLeft = Offset(col * stepPx, (index % 7) * stepPx)
                    val size = Size(cellPx, cellPx)
                    val color = when (day) {
                        HeatDay.Off -> off
                        HeatDay.Skipped -> empty
                        HeatDay.Done -> accent
                        is HeatDay.Partial -> if (day.fraction <= 0f) empty else lerp(empty, accent, 0.25f + day.fraction * 0.5f)
                    }
                    drawRoundRect(color.copy(alpha = color.alpha * appear), topLeft, size, corner)
                    if (day == HeatDay.Skipped) {
                        drawCircle(outline.copy(alpha = appear), radius = cellPx * 0.14f, center = topLeft + Offset(cellPx / 2, cellPx / 2))
                    }
                    if (date == selected) {
                        drawRoundRect(highlight, topLeft, size, corner, style = Stroke(width = cellPx * 0.14f))
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Box(Modifier.padding(horizontal = 16.dp)) { Legend(empty = empty, accent = accent, outline = outline) }
}

@Composable
private fun Legend(empty: Color, accent: Color, outline: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.heatmap_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        listOf(0f, 0.4f, 0.75f, 1f).forEach { level ->
            Canvas(Modifier.size(10.dp)) {
                drawRoundRect(if (level == 0f) empty else lerp(empty, accent, level), cornerRadius = CornerRadius(size.minDimension * 0.28f))
            }
        }
        Text(stringResource(R.string.heatmap_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Canvas(Modifier.size(10.dp)) {
            drawRoundRect(empty, cornerRadius = CornerRadius(size.minDimension * 0.28f))
            drawCircle(outline, radius = size.minDimension * 0.14f)
        }
        Text(stringResource(R.string.skipped_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun heatDayLabel(day: HeatDay?, habit: Habit): String = when (day) {
    null, HeatDay.Off -> stringResource(R.string.heat_day_off)
    HeatDay.Skipped -> stringResource(R.string.skipped_label)
    HeatDay.Done -> stringResource(R.string.heat_day_done)
    is HeatDay.Partial -> if (day.fraction <= 0f) {
        stringResource(R.string.heat_day_missed)
    } else {
        stringResource(R.string.heat_day_partial, "${(day.fraction * 100).toInt()}%")
    }
}
