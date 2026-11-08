package app.tsuzuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.mostEnjoyed
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** One habit's share of the month: how many days, and the amount if it counts something. */
data class MonthHabit(val emoji: String, val name: String, val days: Int)

/** A total for a counted or timed habit: "214 bardak", or minutes for a timed one. */
data class MonthTotal(val emoji: String, val name: String, val amount: Double, val unit: String, val minutes: Boolean)

/** Everything the month collage shows. */
data class MonthReviewData(
    val month: YearMonth,
    val completions: Int,
    val previousCompletions: Int,
    val activeDays: Int,
    val days: Int,
    val perfectDays: Int,
    val comebacks: Int,
    val bestStreak: Int,
    val bestStreakHabit: String,
    val top: List<MonthHabit>,
    val loved: MonthHabit?,
    val bestWeekday: DayOfWeek?,
    val bestWeekStart: LocalDate?,
    val bestWeekCount: Int,
    val totals: List<MonthTotal>,
    val averageMood: Float?,
    /** One entry per day of the month: share done (0..1), or null for a day with nothing due or still to come. */
    val dayLevels: List<Float?>,
    val goldDays: Set<Int>,
) {
    /** Change against last month in percent, or null when last month had nothing to compare with. */
    val change: Int? get() = if (previousCompletions <= 0) null else ((completions - previousCompletions) * 100f / previousCompletions).roundToInt()
}

/** In the first week of a month the review is about last month; later on, about this month so far. */
internal fun reviewMonth(today: LocalDate): YearMonth = if (today.dayOfMonth <= 7) YearMonth.from(today).minusMonths(1) else YearMonth.from(today)

/** Progress offers the month review in the first week of each month. */
internal fun monthReviewSeason(today: LocalDate): Boolean = today.dayOfMonth <= 7

internal fun monthReview(history: HabitHistorySnapshot, moods: Map<LocalDate, Int>, today: LocalDate, month: YearMonth = reviewMonth(today)): MonthReviewData {
    val from = month.atDay(1)
    val to = minOf(month.atEndOfMonth(), today)
    val habitsById = history.habits.associateBy { it.id }
    var completions = 0; var active = 0; var perfect = 0
    val doneDays = HashMap<Long, Int>()
    val runs = HashMap<Long, Int>(); val best = HashMap<Long, Int>()
    val weekdayDone = HashMap<DayOfWeek, Int>(); val weekdayDue = HashMap<DayOfWeek, Int>()
    val weekDone = HashMap<LocalDate, Int>()
    val levels = MutableList<Float?>(month.lengthOfMonth()) { null }
    val gold = HashSet<Int>()
    var d = from
    while (!d.isAfter(to)) {
        val dayHabits = history.habitsForDay(d)
        val due = dayHabits.filter { it.status != HabitDayStatus.Skipped }
        val done = due.count { it.isComplete }
        completions += done
        if (done > 0) active++
        if (due.isNotEmpty() && done >= due.size) perfect++
        if (due.isNotEmpty()) levels[d.dayOfMonth - 1] = done.toFloat() / due.size
        // Gold: back after a whole day with nothing done, the way the app draws comebacks elsewhere.
        if (done > 0) {
            val before = history.daySummary(d.minusDays(1))
            if (before.scheduledCount > 0 && before.completedCount == 0) gold += d.dayOfMonth
        }
        weekdayDone[d.dayOfWeek] = (weekdayDone[d.dayOfWeek] ?: 0) + done
        weekdayDue[d.dayOfWeek] = (weekdayDue[d.dayOfWeek] ?: 0) + due.size
        val weekStart = d.minusDays((d.dayOfWeek.value - 1).toLong())
        weekDone[weekStart] = (weekDone[weekStart] ?: 0) + done
        dayHabits.forEach { t ->
            val id = t.habit.id
            when {
                t.isComplete -> {
                    doneDays[id] = (doneDays[id] ?: 0) + 1
                    runs[id] = (runs[id] ?: 0) + 1
                    best[id] = maxOf(best[id] ?: 0, runs[id] ?: 0)
                }
                t.status != HabitDayStatus.Skipped -> runs[id] = 0
            }
        }
        d = d.plusDays(1)
    }
    // Last month, the same count of days, to compare like with like.
    val prev = month.minusMonths(1)
    var previous = 0
    var p = prev.atDay(1)
    val prevEnd = minOf(prev.atEndOfMonth(), prev.atDay(1).plusDays((to.dayOfMonth - 1).toLong()))
    while (!p.isAfter(prevEnd)) {
        previous += history.daySummary(p).completedCount
        p = p.plusDays(1)
    }
    val top = doneDays.entries.sortedByDescending { it.value }.take(3).mapNotNull { (id, n) ->
        habitsById[id]?.let { MonthHabit(it.emoji, it.name, n) }
    }
    val streak = best.maxByOrNull { it.value }
    val totals = history.habits.filter { it.type != HabitType.Check }.mapNotNull { habit ->
        val amount = history.records.filter { it.habitId == habit.id && !it.date.isBefore(from) && !it.date.isAfter(to) }.sumOf { it.progress }
        if (amount <= 0) null else MonthTotal(habit.emoji, habit.name, amount, habit.unit, habit.type == HabitType.Duration)
    }.sortedByDescending { t -> doneDays[history.habits.first { it.name == t.name && it.emoji == t.emoji }.id] ?: 0 }.take(2)
    val loved = history.mostEnjoyed(since = from, limit = 1).firstOrNull()?.first
    val monthMoods = moods.filterKeys { !it.isBefore(from) && !it.isAfter(to) }.values
    val bestWeekday = weekdayDue.filter { it.value > 0 }.maxByOrNull { (weekdayDone[it.key] ?: 0).toFloat() / it.value }?.key
    val bestWeek = weekDone.maxByOrNull { it.value }
    return MonthReviewData(
        month = month,
        completions = completions,
        previousCompletions = previous,
        activeDays = active,
        days = to.dayOfMonth,
        perfectDays = perfect,
        comebacks = history.comebackCount(from, to),
        bestStreak = streak?.value ?: 0,
        bestStreakHabit = streak?.let { habitsById[it.key]?.name }.orEmpty(),
        top = top,
        loved = loved?.let { MonthHabit(it.emoji, it.name, doneDays[it.id] ?: 0) },
        bestWeekday = bestWeekday,
        bestWeekStart = bestWeek?.key,
        bestWeekCount = bestWeek?.value ?: 0,
        totals = totals,
        averageMood = if (monthMoods.isEmpty()) null else monthMoods.average().toFloat(),
        dayLevels = levels,
        goldDays = gold,
    )
}

private val Ink = Color(0xFF121212)
private val White = Color.White
private val Gold = Color(0xFFE2C57C)
// The palettes' deep colours, one per tile.
private val Momiji = Color(0xFFB2472A)
private val Kin = Color(0xFF8A6A1F)
private val Yoru = Color(0xFF3B3F8F)
private val Matcha = Color(0xFF4E6B2E)
private val Sakura = Color(0xFFA2445E)
private val Umi = Color(0xFF1F6680)
private val Washi = Color(0xFF2E4A7D)
private val Sumi = Color(0xFF2A2A2A)

@Composable
private fun Tile(bg: Color, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(bg).padding(16.dp), content = content)
}

@Composable
private fun TileLabel(text: String, color: Color = White.copy(alpha = 0.85f)) =
    Text(text, color = color, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)

@Composable
private fun Big(text: String, size: Int, color: Color = White) =
    Text(text, color = color, fontSize = size.sp, lineHeight = (size * 1.08).sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)

@Composable
private fun Small(text: String) = Text(text, color = White.copy(alpha = 0.85f), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)

/** "9 sa 40 dk" or "214 bardak". */
@Composable
internal fun monthAmount(total: MonthTotal): String =
    if (total.minutes) {
        val m = total.amount.roundToInt()
        if (m >= 60) stringResource(R.string.month_hm, m / 60, m % 60) else stringResource(R.string.month_m, m)
    } else "${formatHabitAmount(total.amount)} ${total.unit}".trim()

/** The month as a colourful collage you scroll, then share. */
@Composable
internal fun MonthReviewScreen(data: MonthReviewData, onClose: () -> Unit, onShare: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val monthName = data.month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    val dayFmt = DateTimeFormatter.ofPattern("d MMM", locale)
    Column(
        Modifier.fillMaxSize().background(Ink).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("KINMICHI · ${monthName.uppercase(locale)} ${data.month.year}", color = White.copy(alpha = 0.65f), fontSize = 12.sp, letterSpacing = 1.2.sp)
                Big(stringResource(R.string.month_title), 32)
            }
            IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = White) }
        }
        // Completions and the change against last month.
        Row(Modifier.height(170.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Momiji, Modifier.weight(1.3f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_completions))
                Spacer(Modifier.weight(1f))
                Big(countUp(data.completions.toFloat()).roundToInt().toString(), 64)
            }
            Tile(Kin, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_vs_prev))
                Spacer(Modifier.weight(1f))
                val change = data.change
                Big(
                    when {
                        change == null -> stringResource(R.string.month_new)
                        change > 0 -> stringResource(R.string.month_up, change)
                        change < 0 -> stringResource(R.string.month_down, -change)
                        else -> "="
                    },
                    34,
                )
            }
        }
        // Active days, perfect days.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Washi, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_active))
                Spacer(Modifier.height(18.dp))
                Big("${data.activeDays}", 40)
                Small(stringResource(R.string.month_of_days, data.days))
            }
            Tile(Umi, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_perfect))
                Spacer(Modifier.height(18.dp))
                Big("${data.perfectDays}", 40)
                Small(stringResource(R.string.month_perfect_hint))
            }
        }
        // Longest streak, star of the month.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Yoru, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_streak))
                Spacer(Modifier.height(18.dp))
                Big("🔥 ${data.bestStreak}", 34)
                if (data.bestStreakHabit.isNotBlank()) Small(data.bestStreakHabit)
            }
            data.top.firstOrNull()?.let { star ->
                Tile(Matcha, Modifier.weight(1.4f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.month_star))
                    Spacer(Modifier.height(18.dp))
                    Big("${star.emoji} ${star.name}", 26)
                    Small(stringResource(R.string.month_days_n, star.days))
                }
            }
        }
        // The month as a calendar, comebacks in gold.
        Tile(Color(0xFF1E1E1E), Modifier.fillMaxWidth()) {
            TileLabel(stringResource(R.string.month_calendar))
            Spacer(Modifier.height(12.dp))
            val lead = data.month.atDay(1).dayOfWeek.value - 1
            val cells = List(lead) { -1 } + (1..data.month.lengthOfMonth()).toList()
            cells.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    week.forEach { day ->
                        val level = if (day > 0) data.dayLevels[day - 1] else null
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp)).background(
                                when {
                                    day <= 0 -> Color.Transparent
                                    day in data.goldDays -> Gold
                                    level == null -> White.copy(alpha = 0.07f)
                                    level <= 0f -> White.copy(alpha = 0.12f)
                                    else -> Color(0xFF7FA0E0).copy(alpha = 0.3f + 0.7f * level)
                                },
                            ),
                        )
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (data.goldDays.isNotEmpty()) Text(stringResource(R.string.month_gold_hint), color = Gold, fontSize = 12.sp)
        }
        // Top three.
        if (data.top.isNotEmpty()) Tile(Sumi, Modifier.fillMaxWidth()) {
            TileLabel(stringResource(R.string.month_top))
            Spacer(Modifier.height(10.dp))
            val most = data.top.maxOf { it.days }.coerceAtLeast(1)
            data.top.forEach { h ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 5.dp)) {
                    Text(h.emoji, fontSize = 18.sp)
                    Text(h.name, color = White, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp).width(110.dp))
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(White.copy(alpha = 0.12f))) {
                        Box(Modifier.fillMaxWidth(h.days.toFloat() / most).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(Gold))
                    }
                    Text("  ${h.days}", color = White, fontSize = 14.sp)
                }
            }
        }
        // Best weekday, best week.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data.bestWeekday?.let { day ->
                Tile(Sakura, Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.month_best_day))
                    Spacer(Modifier.height(18.dp))
                    Big(day.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }, 26)
                }
            }
            data.bestWeekStart?.let { start ->
                Tile(Momiji.copy(alpha = 0.85f), Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.month_best_week))
                    Spacer(Modifier.height(18.dp))
                    Big("${start.format(dayFmt)} – ${start.plusDays(6).format(dayFmt)}", 20)
                    Small(stringResource(R.string.month_completions_n, data.bestWeekCount))
                }
            }
        }
        // Totals, comebacks.
        Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (data.totals.isNotEmpty()) Tile(Umi.copy(alpha = 0.9f), Modifier.weight(1.4f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_totals))
                Spacer(Modifier.height(12.dp))
                data.totals.forEach { t ->
                    Big(monthAmount(t), 22)
                    Small("${t.emoji} ${t.name}")
                    Spacer(Modifier.height(6.dp))
                }
            }
            Tile(Sumi, Modifier.weight(1f).fillMaxHeight()) {
                TileLabel(stringResource(R.string.month_comebacks), Gold)
                Spacer(Modifier.height(18.dp))
                Big("✨ ${data.comebacks}", 32, Gold)
            }
        }
        // Enjoyed most, mood.
        if (data.loved != null || data.averageMood != null) Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data.loved?.let { l ->
                Tile(Sakura.copy(alpha = 0.85f), Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.month_loved))
                    Spacer(Modifier.height(18.dp))
                    Big("${l.emoji} ${l.name}", 22)
                }
            }
            data.averageMood?.let { mood ->
                Tile(Kin.copy(alpha = 0.9f), Modifier.weight(1f).fillMaxHeight()) {
                    TileLabel(stringResource(R.string.month_mood))
                    Spacer(Modifier.height(18.dp))
                    Big("${listOf("😣", "😕", "😐", "🙂", "😄")[(mood.roundToInt() - 1).coerceIn(0, 4)]} ${"%.1f".format(locale, mood)}", 30)
                }
            }
        }
        Button(
            onClick = onShare,
            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Ink),
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(top = 4.dp),
        ) {
            Icon(Icons.Rounded.Share, null)
            Text(stringResource(R.string.month_share), modifier = Modifier.padding(start = 8.dp))
        }
        Spacer(Modifier.height(12.dp))
    }
}

/** Entry card on Progress in the first week of the month. */
@Composable
internal fun MonthReviewEntryCard(month: YearMonth, onOpen: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val name = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) }
    Surface(onClick = onOpen, shape = RoundedCornerShape(28.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.background(Brush.horizontalGradient(listOf(Momiji, Kin, Yoru))).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🗓️", fontSize = 32.sp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.month_entry_title, name), color = White, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.month_entry_body), color = White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
