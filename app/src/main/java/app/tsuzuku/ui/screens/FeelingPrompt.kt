package app.tsuzuku.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.Feeling
import app.tsuzuku.model.Habit

/** Whether to ask how a habit felt after completing it (on by default, off in Settings). */
object FeelingPromptSetting {
    private const val PREFS = "feeling_prompt"
    private const val KEY_ENABLED = "enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }
}

/**
 * A small card that appears after a habit is completed: one tap on a face saves how it felt.
 * Ignoring it is fine; it goes away by itself.
 */
@Composable
fun FeelingPrompt(habit: Habit, onPick: (Feeling) -> Unit, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.widthIn(max = 520.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.feeling_question, habit.name),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close_label)) }
            }
            Row(
                Modifier.fillMaxWidth().padding(end = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Feeling.entries.forEach { feeling ->
                    FilledTonalButton(
                        onClick = { onPick(feeling) },
                        modifier = Modifier.weight(1f),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(feeling.emoji, fontSize = 22.sp)
                            Text(stringResource(feeling.labelRes), style = MaterialTheme.typography.labelMedium, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

val Feeling.labelRes: Int
    get() = when (this) {
        Feeling.Hard -> R.string.feeling_hard
        Feeling.Okay -> R.string.feeling_okay
        Feeling.Good -> R.string.feeling_good
    }

/** On a habit's detail page: how it has usually felt over the last 90 days. */
@Composable
fun FeelingSummaryCard(summary: app.tsuzuku.model.FeelingSummary, modifier: Modifier = Modifier) {
    val usual = summary.usual ?: return
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.feeling_card_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.feeling_usually, usual.emoji, stringResource(usual.labelRes)),
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(Feeling.Good to summary.good, Feeling.Okay to summary.okay, Feeling.Hard to summary.hard).forEach { (feeling, count) ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (feeling == usual) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(feeling.emoji, fontSize = 20.sp)
                            Text("$count", style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(feeling.labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.feeling_card_footer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** On Progress: the habits that have felt best lately. */
@Composable
fun MostEnjoyedCard(items: List<Pair<Habit, app.tsuzuku.model.FeelingSummary>>, modifier: Modifier = Modifier) {
    if (items.isEmpty()) return
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.most_enjoyed_title), style = MaterialTheme.typography.titleMedium)
            items.forEach { (habit, summary) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(habit.emoji, fontSize = 20.sp, modifier = Modifier.size(32.dp))
                    Text(habit.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(
                        stringResource(R.string.most_enjoyed_share, summary.good, summary.total),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
