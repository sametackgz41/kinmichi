package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.HabitDirection
import app.tsuzuku.model.HabitDraft
import app.tsuzuku.model.HabitType

/** A ready-made habit people can add with one tap from the empty Today screen. */
internal data class HabitTemplate(
    val key: String,
    val emoji: String,
    @param:StringRes val nameRes: Int,
    val type: HabitType = HabitType.Check,
    val target: Double = 1.0,
    @param:StringRes val unitRes: Int? = null,
    val colorArgb: Long,
    val reminderMinutes: Int? = null,
)

internal val StarterHabitTemplates = listOf(
    HabitTemplate("water", "💧", R.string.template_water, HabitType.Count, 8.0, R.string.unit_glasses, 0xFF3E8EC2),
    HabitTemplate("read", "📖", R.string.template_read, HabitType.Duration, 20.0, R.string.unit_minutes, 0xFF5B7DB1, reminderMinutes = 21 * 60),
    HabitTemplate("walk", "🚶", R.string.template_walk, HabitType.Duration, 30.0, R.string.unit_minutes, 0xFF4E9A6B),
    HabitTemplate("stretch", "🧘", R.string.template_stretch, colorArgb = 0xFF8C6BB1, reminderMinutes = 8 * 60),
    HabitTemplate("early", "⏰", R.string.template_wake_early, colorArgb = 0xFFC2733E),
    HabitTemplate("screen", "📵", R.string.template_no_phone_bed, colorArgb = 0xFF6B7A8F, reminderMinutes = 22 * 60 + 30),
)

@Composable
internal fun HabitTemplate.toDraft(): HabitDraft = HabitDraft(
    name = stringResource(nameRes),
    emoji = emoji,
    colorArgb = colorArgb,
    type = type,
    direction = HabitDirection.Build,
    target = target,
    unit = unitRes?.let { stringResource(it) }.orEmpty(),
    step = 1.0,
    scheduleDaysMask = 127,
    reminderMinutes = reminderMinutes,
)

/**
 * First-run card: a few common habits added with one tap, plus a way to build your own.
 * Added templates disappear from the list so the user sees what changed.
 */
@Composable
internal fun StarterTemplatesCard(
    onAddTemplate: (HabitDraft) -> Unit,
    onCreateOwn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val added = remember { mutableStateListOf<String>() }
    val drafts = StarterHabitTemplates.associate { it.key to it.toDraft() }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.starter_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.starter_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                StarterHabitTemplates.filterNot { it.key in added }.forEach { template ->
                    val draft = drafts.getValue(template.key)
                    AssistChip(
                        onClick = {
                            added += template.key
                            onAddTemplate(draft)
                        },
                        label = { Text("${template.emoji}  ${draft.name}") },
                    )
                }
            }
            TextButton(onClick = onCreateOwn) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(R.string.create_own_habit), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}
