package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.DailyReview
import app.tsuzuku.model.HabitType
import app.tsuzuku.model.MoodInsight
import app.tsuzuku.model.MoodInsights
import app.tsuzuku.model.TargetAdvice
import java.util.Locale

/** Mood scale shared by the quick check-in, the notification and the insights. */
internal val MoodFaces = listOf(1 to "😣", 2 to "😕", 3 to "😐", 4 to "🙂", 5 to "😄")

@Composable
private fun moodLabel(mood: Int): String = stringResource(
    when (mood) {
        1 -> R.string.mood_hard
        2 -> R.string.mood_tired
        3 -> R.string.mood_neutral
        4 -> R.string.mood_good
        else -> R.string.mood_great
    },
)

/** "Start small" suggestion: lower a target that's rarely reached, or step back up. */
@Composable
internal fun TargetAdviceCard(
    advice: TargetAdvice,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val habit = advice.habit
    val newAmount = formatHabitAmount(advice.newTarget)
    val unit = habit.unit.ifBlank { if (habit.type == HabitType.Duration) stringResource(R.string.unit_minutes) else "" }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "${habit.emoji} ${habit.name}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (advice.kind == TargetAdvice.Kind.Lower) {
                    stringResource(R.string.advice_lower, advice.hits, advice.days, "$newAmount $unit".trim())
                } else {
                    stringResource(R.string.advice_raise, "$newAmount $unit".trim())
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(end = 8.dp),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.advice_not_now), color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Button(onClick = onAccept, shapes = androidx.compose.material3.ButtonDefaults.shapes(), modifier = Modifier.padding(bottom = 6.dp)) {
                    Text(stringResource(R.string.advice_accept, "$newAmount $unit".trim()))
                }
            }
        }
    }
}

/** One-tap end-of-day mood. A note and the full review stay one tap away. */
@Composable
internal fun MoodCheckInCard(
    review: DailyReview?,
    onPick: (Int) -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 4.dp)) {
            AnimatedContent(targetState = review?.mood, label = "mood-title") { mood ->
                Text(
                    if (mood == null) stringResource(R.string.mood_question) else stringResource(R.string.mood_logged, moodLabel(mood)),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                MoodFaces.forEach { (value, face) ->
                    val selected = review?.mood == value
                    val scale by animateFloatAsState(
                        if (selected) 1.18f else 1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "mood-scale",
                    )
                    val label = moodLabel(value)
                    Box(
                        Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                            .selectable(selected = selected, role = Role.RadioButton, onClick = { onPick(value) })
                            // Screen readers hear the word, not the emoji.
                            .semantics { contentDescription = label }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(face, fontSize = 28.sp, modifier = Modifier.scale(scale).clearAndSetSemantics {})
                    }
                }
            }
            TextButton(onClick = onOpenDetails, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(if (review?.note.isNullOrBlank()) R.string.mood_add_note else R.string.mood_edit_note))
            }
        }
    }
}

/** "On days you walk, you feel better" — correlation from the person's own logs. */
@Composable
internal fun MoodInsightsCard(insights: MoodInsights, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Mood, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.mood_insights_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            when {
                insights.daysUntilReady > 0 -> {
                    Text(
                        pluralStringResource(R.plurals.mood_insights_collecting, insights.daysUntilReady, insights.daysUntilReady),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    androidx.compose.material3.LinearWavyProgressIndicator(
                        progress = { insights.loggedDays / app.tsuzuku.model.MOOD_MIN_DAYS.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                insights.insights.isEmpty() -> Text(
                    stringResource(R.string.mood_insights_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                else -> insights.insights.forEach { MoodInsightRow(it) }
            }
            Text(
                stringResource(R.string.mood_insights_footnote),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
            )
        }
    }
}

@Composable
private fun MoodInsightRow(insight: MoodInsight) {
    val better = insight.difference > 0
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    val fmt = { v: Double -> String.format(locale, "%.1f", v) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(insight.habit.emoji, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(if (better) R.string.mood_insight_better else R.string.mood_insight_worse, insight.habit.name),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                stringResource(R.string.mood_insight_detail, fmt(insight.withHabit), fmt(insight.withoutHabit)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
            )
        }
        Text(
            (if (better) "+" else "−") + fmt(kotlin.math.abs(insight.difference)),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (better) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
