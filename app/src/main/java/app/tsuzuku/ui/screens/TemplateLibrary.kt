package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.HabitType

/** One ready-made habit. Days are Monday first ("1111111" = every day). */
internal data class LibraryHabit(
    @StringRes val name: Int,
    val emoji: String,
    val type: HabitType,
    val target: Double = 1.0,
    @StringRes val unit: Int? = null,
    val step: Double = 1.0,
    @StringRes val tiny: Int,
    val days: String = "1111111",
    val reminderMinutes: Int? = null,
)

internal data class TemplatePack(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val emoji: String,
    val habits: List<LibraryHabit>,
)

internal val templatePacks = listOf(
    TemplatePack(R.string.pack_morning, R.string.pack_morning_desc, "☀️", listOf(
        LibraryHabit(R.string.tpl_morning_water, "💧", HabitType.Check, tiny = R.string.tpl_morning_water_tiny),
        LibraryHabit(R.string.tpl_stretch, "🧘", HabitType.Duration, 10.0, R.string.unit_minutes, 2.0, R.string.tpl_stretch_tiny),
        LibraryHabit(R.string.tpl_breakfast, "🍳", HabitType.Check, tiny = R.string.tpl_breakfast_tiny),
        LibraryHabit(R.string.tpl_plan_day, "📝", HabitType.Check, tiny = R.string.tpl_plan_day_tiny),
    )),
    TemplatePack(R.string.pack_language, R.string.pack_language_desc, "🗣️", listOf(
        LibraryHabit(R.string.tpl_vocab, "🗂️", HabitType.Count, 20.0, R.string.unit_words, 5.0, R.string.tpl_vocab_tiny),
        LibraryHabit(R.string.tpl_listen, "🎧", HabitType.Duration, 15.0, R.string.unit_minutes, 5.0, R.string.tpl_listen_tiny),
        LibraryHabit(R.string.tpl_speak, "💬", HabitType.Duration, 10.0, R.string.unit_minutes, 5.0, R.string.tpl_speak_tiny),
        LibraryHabit(R.string.tpl_read_lang, "📖", HabitType.Count, 5.0, R.string.unit_pages, 1.0, R.string.tpl_read_lang_tiny),
    )),
    TemplatePack(R.string.pack_fitness, R.string.pack_fitness_desc, "💪", listOf(
        LibraryHabit(R.string.tpl_walk, "🚶", HabitType.Duration, 30.0, R.string.unit_minutes, 5.0, R.string.tpl_walk_tiny),
        LibraryHabit(R.string.tpl_pushups, "💪", HabitType.Count, 20.0, R.string.unit_times, 5.0, R.string.tpl_pushups_tiny, days = "1010100"),
        LibraryHabit(R.string.tpl_plank, "🧱", HabitType.Duration, 2.0, R.string.unit_minutes, 1.0, R.string.tpl_plank_tiny, days = "1010100"),
        LibraryHabit(R.string.tpl_water, "🥤", HabitType.Count, 8.0, R.string.unit_glasses, 1.0, R.string.tpl_water_tiny),
    )),
    TemplatePack(R.string.pack_calm, R.string.pack_calm_desc, "🍃", listOf(
        LibraryHabit(R.string.tpl_meditate, "🧘", HabitType.Duration, 10.0, R.string.unit_minutes, 2.0, R.string.tpl_meditate_tiny),
        LibraryHabit(R.string.tpl_journal, "✍️", HabitType.Check, tiny = R.string.tpl_journal_tiny),
        LibraryHabit(R.string.tpl_gratitude, "🙏", HabitType.Check, tiny = R.string.tpl_gratitude_tiny),
        LibraryHabit(R.string.tpl_no_screen, "📵", HabitType.Check, tiny = R.string.tpl_no_screen_tiny, reminderMinutes = 21 * 60 + 30),
    )),
    TemplatePack(R.string.pack_sleep, R.string.pack_sleep_desc, "🌙", listOf(
        LibraryHabit(R.string.tpl_bed_on_time, "🌙", HabitType.Check, tiny = R.string.tpl_bed_on_time_tiny, reminderMinutes = 23 * 60),
        LibraryHabit(R.string.tpl_no_caffeine, "☕", HabitType.Check, tiny = R.string.tpl_no_caffeine_tiny),
        LibraryHabit(R.string.tpl_read_bed, "📚", HabitType.Count, 10.0, R.string.unit_pages, 1.0, R.string.tpl_read_bed_tiny),
        LibraryHabit(R.string.tpl_wind_down, "🛁", HabitType.Check, tiny = R.string.tpl_wind_down_tiny),
    )),
)

/** Entry point on the first step of a new habit. */
@Composable
internal fun TemplateLibraryCard(onOpen: () -> Unit) {
    Surface(onClick = onOpen, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("📚", fontSize = 30.sp)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(stringResource(R.string.templates_body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
            }
        }
    }
}

/** The library: packs of four habits; tap one to fill the editor, or add a whole pack. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TemplateLibrary(onBack: () -> Unit, onPick: (LibraryHabit) -> Unit, onAddPack: (TemplatePack) -> Unit) {
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.templates_title), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                stringResource(R.string.templates_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 12.dp, top = 2.dp),
            )
        }
        items(templatePacks) { pack ->
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) { Text(pack.emoji, fontSize = 24.sp) }
                    Column(Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(stringResource(pack.title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(pack.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pack.habits.forEach { habit ->
                        Surface(
                            onClick = { onPick(habit) },
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        ) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(habit.emoji, fontSize = 18.sp)
                                Column(Modifier.padding(start = 8.dp)) {
                                    Text(stringResource(habit.name), style = MaterialTheme.typography.labelLarge)
                                    Text(templateGoal(habit), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = { onAddPack(pack) }, modifier = Modifier.align(Alignment.End)) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.templates_add_all, pack.habits.size), modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
}

/** "10 min · every day" style summary under a template's name. */
@Composable
private fun templateGoal(habit: LibraryHabit): String {
    val amount = if (habit.type == HabitType.Check || habit.unit == null) null else "${habit.target.toInt()} ${stringResource(habit.unit)}"
    val days = if (habit.days == "1111111") stringResource(R.string.templates_every_day) else stringResource(R.string.templates_days_a_week, habit.days.count { it == '1' })
    return listOfNotNull(amount, days).joinToString(" · ")
}
