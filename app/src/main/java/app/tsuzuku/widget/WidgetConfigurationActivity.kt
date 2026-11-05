package app.tsuzuku.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.tsuzuku.R
import app.tsuzuku.TsuzukuApplication
import app.tsuzuku.data.preferences.AppLocaleContext
import app.tsuzuku.model.Habit
import app.tsuzuku.ui.theme.TsuzukuTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WidgetConfigurationActivity : ComponentActivity() {
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocaleContext.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) { finish(); return }
        val application = application as TsuzukuApplication
        val widgetSettings = WidgetSettings(this)
        setContent {
            TsuzukuTheme {
                val history by application.habitRepository.observeHistory().collectAsStateWithLifecycle(
                    initialValue = app.tsuzuku.model.HabitHistorySnapshot(emptyList(), emptyList()),
                )
                WidgetConfigurationScreen(
                    habits = history.habits,
                    initialSelection = widgetSettings.selectedHabitIds(widgetId),
                    initialPalette = widgetSettings.palette(widgetId),
                    onSave = { ids, palette ->
                        widgetSettings.save(widgetId, ids, palette)
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            TsuzukuWidgetProvider.updateWidget(this@WidgetConfigurationActivity, AppWidgetManager.getInstance(this@WidgetConfigurationActivity), widgetId)
                        }
                        setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                        finish()
                    },
                )
            }
        }
    }
}

@Composable
private fun WidgetConfigurationScreen(
    habits: List<Habit>,
    initialSelection: Set<Long>,
    initialPalette: WidgetPalette,
    onSave: (Set<Long>, WidgetPalette) -> Unit,
) {
    val selected = remember { mutableStateListOf<Long>().apply { addAll(initialSelection) } }
    var palette by remember { mutableStateOf(initialPalette) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(stringResource(R.string.configure_widget), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.configure_widget_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { Text(stringResource(R.string.widget_palette), style = MaterialTheme.typography.titleMedium) }
        items(WidgetPalette.entries) { option ->
            Row(
                Modifier.fillMaxWidth().clickable { palette = option }.padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = palette == option, onClick = { palette = option })
                Text(stringResource(option.labelRes()), modifier = Modifier.padding(start = 8.dp))
            }
        }
        item { Text(stringResource(R.string.widget_habits), style = MaterialTheme.typography.titleMedium) }
        item { Text(stringResource(R.string.widget_habit_limit), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(habits, key = Habit::id) { habit ->
            val checked = habit.id in selected
            val enabled = checked || selected.size < 4
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    Modifier.fillMaxWidth().clickable(enabled = enabled) {
                        if (checked) selected.remove(habit.id) else selected.add(habit.id)
                    }.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(habit.name, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            if (checked) selected.remove(habit.id) else if (enabled) selected.add(habit.id)
                        },
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            Button(onClick = { onSave(selected.toSet(), palette) }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save_widget))
            }
        }
    }
}

private fun WidgetPalette.labelRes(): Int = when (this) {
    WidgetPalette.Indigo -> R.string.palette_indigo
    WidgetPalette.Light -> R.string.palette_light
    WidgetPalette.Sage -> R.string.palette_sage
}
