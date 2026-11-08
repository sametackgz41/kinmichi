package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.HabitDayStatus
import app.tsuzuku.model.HabitHistorySnapshot
import app.tsuzuku.model.mostEnjoyed
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import kotlin.math.roundToInt

/** Everything the year-in-review cards show. */
data class YearReviewData(
    val year: Int,
    val completions: Int,
    val activeDays: Int,
    val perfectDays: Int,
    val comebacks: Int,
    val bestStreak: Int,
    val bestStreakHabit: String,
    val topHabit: String,
    val topHabitEmoji: String,
    val topHabitCount: Int,
    val bestMonth: YearMonth?,
    val bestMonthRate: Float,
    val loved: String?,
    val lovedEmoji: String?,
    val averageMood: Float?,
)

/** The review is for this year, or last year when opened in January. */
internal fun reviewYear(today: LocalDate): Int = if (today.monthValue == 1) today.year - 1 else today.year

/** When Progress offers the review: mid-November to mid-January. */
internal fun yearReviewSeason(today: LocalDate): Boolean =
    (today.monthValue == 11 && today.dayOfMonth >= 15) || today.monthValue == 12 || (today.monthValue == 1 && today.dayOfMonth <= 15)

internal fun yearReview(history: HabitHistorySnapshot, moods: Map<LocalDate, Int>, today: LocalDate): YearReviewData {
    val year = reviewYear(today)
    val from = LocalDate.of(year, 1, 1)
    val to = minOf(LocalDate.of(year, 12, 31), today)
    var completions = 0; var active = 0; var perfect = 0
    val monthDone = HashMap<YearMonth, Int>(); val monthSched = HashMap<YearMonth, Int>()
    var d = from
    while (!d.isAfter(to)) {
        val s = history.daySummary(d)
        completions += s.completedCount
        if (s.completedCount > 0) active++
        if (s.scheduledCount > 0 && s.completedCount >= s.scheduledCount) perfect++
        val ym = YearMonth.from(d)
        monthDone[ym] = (monthDone[ym] ?: 0) + s.completedCount
        monthSched[ym] = (monthSched[ym] ?: 0) + s.scheduledCount
        d = d.plusDays(1)
    }
    val best = monthSched.filter { it.value > 0 }.maxByOrNull { (monthDone[it.key] ?: 0).toFloat() / it.value }
    val streaks = history.habits.map { it to history.streak(it, to) }
    val longest = streaks.maxByOrNull { it.second.best }
    val counts = history.records.filter { it.status == HabitDayStatus.Completed && !it.date.isBefore(from) && !it.date.isAfter(to) }
        .groupingBy { it.habitId }.eachCount()
    val topId = counts.maxByOrNull { it.value }?.key
    val top = history.habits.firstOrNull { it.id == topId }
    val loved = history.mostEnjoyed(since = from, limit = 1).firstOrNull()?.first
    val yearMoods = moods.filterKeys { !it.isBefore(from) && !it.isAfter(to) }.values
    return YearReviewData(
        year = year,
        completions = completions,
        activeDays = active,
        perfectDays = perfect,
        comebacks = history.comebackCount(from, to),
        bestStreak = longest?.second?.best ?: 0,
        bestStreakHabit = longest?.first?.name.orEmpty(),
        topHabit = top?.name.orEmpty(),
        topHabitEmoji = top?.emoji.orEmpty(),
        topHabitCount = topId?.let { counts[it] } ?: 0,
        bestMonth = best?.key,
        bestMonthRate = best?.let { (monthDone[it.key] ?: 0).toFloat() / it.value } ?: 0f,
        loved = loved?.name,
        lovedEmoji = loved?.emoji,
        averageMood = if (yearMoods.isEmpty()) null else yearMoods.average().toFloat(),
    )
}

/** Story-style cards: tap the right side for the next one, the left side to go back. */
@Composable
internal fun YearReviewScreen(data: YearReviewData, onClose: () -> Unit, onShare: () -> Unit, startPage: Int = 0) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val pages = buildList {
        add(0) // intro
        add(1) // completions & days
        add(2) // longest streak
        add(3) // comebacks in gold
        if (data.topHabit.isNotBlank()) add(4)
        if (data.bestMonth != null) add(5)
        if (data.loved != null || data.averageMood != null) add(6)
        add(7) // finale
    }
    var index by rememberSaveable { mutableIntStateOf(startPage.coerceIn(0, pages.lastIndex)) }
    val gradients = listOf(
        listOf(Color(0xFF1C2E52), Color(0xFF2E4A7D)),
        listOf(Color(0xFF2E4A7D), Color(0xFF3B5E9A)),
        listOf(Color(0xFF4A2A0C), Color(0xFFB5651D)),
        listOf(Color(0xFF3D2E05), Color(0xFF8A6A1F)),
        listOf(Color(0xFF1F3209), Color(0xFF4E6B2E)),
        listOf(Color(0xFF14164A), Color(0xFF3B3F8F)),
        listOf(Color(0xFF4A1426), Color(0xFFA2445E)),
        listOf(Color(0xFF1C2E52), Color(0xFF8A6A1F)),
    )
    val page = pages[index]
    Box(
        Modifier.fillMaxSize().background(Brush.verticalGradient(gradients[page]))
            .pointerInput(pages.size) {
                detectTapGestures { offset ->
                    if (offset.x > size.width * 0.35f) { if (index < pages.lastIndex) index++ } else if (index > 0) index--
                }
            },
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(20.dp)) {
            // Progress segments, like stories.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                pages.indices.forEach { i ->
                    Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = if (i <= index) 0.95f else 0.3f)))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                Text("Kinmichi · ${data.year}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color.White) }
            }
            AnimatedContent(targetState = page, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "year", modifier = Modifier.weight(1f)) { p ->
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp), verticalArrangement = Arrangement.Center) {
                    when (p) {
                        0 -> Story(emoji = "🎍", big = data.year.toString(), line = stringResource(R.string.year_intro))
                        1 -> Story(emoji = "✅", big = countUp(data.completions.toFloat()).roundToInt().toString(),
                            line = stringResource(R.string.year_completions, data.activeDays))
                        2 -> Story(emoji = "🔥", big = stringResource(R.string.year_days, countUp(data.bestStreak.toFloat()).roundToInt()),
                            line = if (data.bestStreakHabit.isBlank()) stringResource(R.string.year_streak_plain) else stringResource(R.string.year_streak, data.bestStreakHabit))
                        3 -> Story(emoji = "✨", big = countUp(data.comebacks.toFloat()).roundToInt().toString(), line = stringResource(R.string.year_comebacks), accent = Color(0xFFE2C57C))
                        4 -> Story(emoji = data.topHabitEmoji.ifBlank { "⭐" }, big = data.topHabit, line = stringResource(R.string.year_top_habit, data.topHabitCount), bigSize = 44)
                        5 -> Story(
                            emoji = "📅",
                            big = data.bestMonth!!.month.getDisplayName(TextStyle.FULL_STANDALONE, locale).replaceFirstChar { it.titlecase(locale) },
                            line = stringResource(R.string.year_best_month, (data.bestMonthRate * 100).roundToInt()),
                            bigSize = 52,
                        )
                        6 -> if (data.loved != null) {
                            Story(emoji = data.lovedEmoji ?: "💛", big = data.loved, line = stringResource(R.string.year_loved), bigSize = 44)
                        } else {
                            Story(emoji = listOf("😣", "😕", "😐", "🙂", "😄")[((data.averageMood ?: 3f).roundToInt() - 1).coerceIn(0, 4)],
                                big = "%.1f".format(data.averageMood ?: 0f), line = stringResource(R.string.year_mood))
                        }
                        else -> {
                            Story(emoji = "🌅", big = stringResource(R.string.year_thanks_big), line = stringResource(R.string.year_thanks, data.perfectDays), bigSize = 44)
                            Spacer(Modifier.height(28.dp))
                            Button(
                                onClick = onShare,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF1C2E52)),
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                            ) {
                                Icon(Icons.Rounded.Share, null)
                                Text(stringResource(R.string.year_share), modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                }
            }
            Text(stringResource(R.string.year_tap_hint), color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Story(emoji: String, big: String, line: String, accent: Color = Color.White, bigSize: Int = 88) {
    Text(emoji, fontSize = 56.sp)
    Spacer(Modifier.height(18.dp))
    Text(big, color = accent, fontSize = bigSize.sp, fontWeight = FontWeight.Bold, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, lineHeight = (bigSize * 1.1).sp)
    Spacer(Modifier.height(12.dp))
    Text(line, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleLarge)
}

/** Entry card on Progress during the review season. */
@Composable
internal fun YearReviewEntryCard(year: Int, onOpen: () -> Unit) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF1C2E52), Color(0xFF8A6A1F)))).padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🎍", fontSize = 34.sp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.year_entry_title, year), color = Color.White, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.year_entry_body), color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
