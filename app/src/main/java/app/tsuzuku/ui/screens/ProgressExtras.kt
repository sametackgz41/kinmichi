package app.tsuzuku.ui.screens

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitHistoryRecord
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.PersonalMilestone
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** A number that counts up from zero the first time it is shown. */
@Composable
internal fun countUp(target: Float, durationMillis: Int = 900): Float {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(target) { anim.animateTo(target, tween(durationMillis, easing = FastOutSlowInEasing)) }
    return anim.value
}

// ---- Weekly summary --------------------------------------------------------------------------

data class WeekSummary(
    val rate: Float,
    val previousRate: Float,
    val completed: Int,
    val scheduled: Int,
    val perfectDays: Int,
    val bestDay: LocalDate?,
    val dayFractions: List<Pair<LocalDate, Float>>,
)

internal fun weekSummary(history: HabitHistorySnapshot, today: LocalDate): WeekSummary {
    val now = history.statistics(7, today)
    val before = history.statistics(7, today.minusDays(7))
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }.map { it to history.daySummary(it).progressFraction }
    val summaries = days.map { history.daySummary(it.first) }
    return WeekSummary(
        rate = now.completionRate,
        previousRate = before.completionRate,
        completed = now.completedCount,
        scheduled = now.scheduledCount,
        perfectDays = summaries.count { it.scheduledCount > 0 && it.completedCount >= it.scheduledCount },
        bestDay = summaries.filter { it.scheduledCount > 0 }.maxByOrNull { it.progressFraction }?.date,
        dayFractions = days,
    )
}

@Composable
internal fun WeeklySummaryCard(summary: WeekSummary, onShare: () -> Unit) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val shown = countUp(summary.rate)
    val delta = ((summary.rate - summary.previousRate) * 100).roundToInt()
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.week_summary_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, stringResource(R.string.share_progress)) }
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text("%${(shown * 100).roundToInt()}", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        delta > 0 -> stringResource(R.string.week_summary_up, delta)
                        delta < 0 -> stringResource(R.string.week_summary_down, -delta)
                        else -> stringResource(R.string.week_summary_same)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (delta >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 10.dp, bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            // Seven bars, one per day, growing in.
            Row(Modifier.fillMaxWidth().height(88.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                summary.dayFractions.forEach { (date, fraction) ->
                    val grown = countUp(fraction, 700)
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        Box(
                            Modifier.fillMaxWidth().height((60 * grown).coerceAtLeast(3f).dp).clip(RoundedCornerShape(8.dp))
                                .background(if (date == summary.bestDay) Color(0xFFB8902E) else MaterialTheme.colorScheme.primary.copy(alpha = 0.35f + 0.65f * fraction)),
                        )
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.week_summary_line, summary.completed, summary.scheduled, summary.perfectDays),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Month calendar: completion or mood ------------------------------------------------------

private val moodColors = listOf(Color(0xFFC0675B), Color(0xFFD9A066), Color(0xFFD9C68A), Color(0xFF8FB486), Color(0xFF4E9A6B))

@Composable
internal fun MonthCalendarCard(history: HabitHistorySnapshot, moods: Map<LocalDate, Int>, weekStartsMonday: Boolean = true) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    var monthText by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    var moodMode by rememberSaveable { mutableStateOf(false) }
    val month = YearMonth.parse(monthText)
    val today = LocalDate.now()
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { monthText = month.minusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, null) }
                Text(
                    month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) } + " " + month.year,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { if (month < YearMonth.now()) monthText = month.plusMonths(1).toString() }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                FilterChip(selected = !moodMode, onClick = { moodMode = false }, label = { Text(stringResource(R.string.calendar_mode_done)) })
                FilterChip(selected = moodMode, onClick = { moodMode = true }, label = { Text(stringResource(R.string.calendar_mode_mood)) })
            }
            val firstDow = if (weekStartsMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
            val headers = (0 until 7).map { firstDow.plus(it.toLong()) }
            Row(Modifier.fillMaxWidth()) {
                headers.forEach { d ->
                    Text(d.getDisplayName(TextStyle.NARROW, locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            val lead = (month.atDay(1).dayOfWeek.value - firstDow.value + 7) % 7
            val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    week.forEach { date ->
                        Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                            if (date != null) {
                                val future = date.isAfter(today)
                                val color = when {
                                    future -> Color.Transparent
                                    moodMode -> moods[date]?.let { moodColors[(it - 1).coerceIn(0, 4)] } ?: MaterialTheme.colorScheme.surfaceContainerHighest
                                    else -> {
                                        val s = history.daySummary(date)
                                        if (s.scheduledCount == 0) MaterialTheme.colorScheme.surfaceContainerHighest
                                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + 0.88f * s.progressFraction)
                                    }
                                }
                                Box(
                                    Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(color)
                                        .then(if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(10.dp)) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (future) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (moodMode) {
                Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf("😣", "😕", "😐", "🙂", "😄").forEachIndexed { i, face ->
                        Box(Modifier.size(12.dp).clip(CircleShape).background(moodColors[i]))
                        Text(face, fontSize = 13.sp, modifier = Modifier.padding(start = 3.dp, end = 10.dp))
                    }
                }
            }
        }
    }
}

// ---- Badge showcase --------------------------------------------------------------------------

@Composable
internal fun BadgeShowcase(milestones: List<PersonalMilestone>, title: @Composable (PersonalMilestone) -> String) {
    val earned = milestones.count { it.isUnlocked }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.badges_earned, earned, milestones.size), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        milestones.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { m ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                            .background(if (m.isUnlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier.size(54.dp).clip(CircleShape)
                                .background(if (m.isUnlocked) Color(0xFFD9B45A).copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center,
                        ) { Text(m.icon, fontSize = 26.sp, modifier = Modifier.alpha(if (m.isUnlocked) 1f else 0.35f)) }
                        Text(
                            title(m),
                            style = MaterialTheme.typography.labelMedium,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (!m.isUnlocked) {
                            LinearProgressIndicator(
                                progress = { m.progressFraction },
                                modifier = Modifier.padding(top = 8.dp).width(64.dp).height(4.dp).clip(RoundedCornerShape(2.dp)),
                            )
                            Text("${m.currentValue}/${m.targetValue}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// ---- Quitting: clean days and what it saves --------------------------------------------------

internal data class CleanDays(val current: Int, val best: Int, val total: Int)

/** Days at or under the limit (no entry counts as clean), from the habit's start until today. */
internal fun cleanDays(habit: Habit, records: List<HabitHistoryRecord>, today: LocalDate): CleanDays {
    val limit = habit.maximumTarget ?: 0.0
    val byDate = records.associateBy { it.date }
    val start = if (habit.createdOn == LocalDate.MIN) today.minusDays(365) else habit.createdOn
    var current = 0; var best = 0; var run = 0; var total = 0
    var d = start
    while (!d.isAfter(today)) {
        val r = byDate[d]
        val clean = r == null || r.status == HabitDayStatus.Skipped || r.progress <= limit
        if (clean) { run++; total++; best = maxOf(best, run) } else run = 0
        d = d.plusDays(1)
    }
    current = run
    return CleanDays(current, best, total)
}

private fun savingsPrefs(context: Context) = context.getSharedPreferences("quit_savings", Context.MODE_PRIVATE)

@Composable
internal fun CleanDaysCard(habit: Habit, records: List<HabitHistoryRecord>) {
    val context = LocalContext.current
    val clean = remember(habit, records) { cleanDays(habit, records, LocalDate.now()) }
    var perDay by rememberSaveable(habit.id) { mutableStateOf(savingsPrefs(context).getString(habit.id.toString(), "") ?: "") }
    val symbol = remember { runCatching { java.util.Currency.getInstance(context.resources.configuration.locales[0]).symbol }.getOrDefault("") }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(stringResource(R.string.clean_days_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${countUp(clean.current.toFloat()).roundToInt()}", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.clean_days_unit), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
            }
            Text(stringResource(R.string.clean_days_line, clean.best, clean.total), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = perDay,
                onValueChange = { v ->
                    perDay = v.filter { it.isDigit() || it == ',' || it == '.' }.take(8)
                    savingsPrefs(context).edit().putString(habit.id.toString(), perDay).apply()
                },
                label = { Text(stringResource(R.string.savings_per_day)) },
                suffix = { Text(symbol) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            perDay.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0 }?.let { amount ->
                Text(
                    stringResource(R.string.savings_total, formatHabitAmount(amount * clean.total) + " " + symbol),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}
