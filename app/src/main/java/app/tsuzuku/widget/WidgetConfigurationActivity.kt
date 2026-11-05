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
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
        // The single-habit widget shows exactly one habit; the Today widget can show several.
        val singleHabit = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)
            ?.provider?.className == HabitWidgetReceiver::class.java.name
        setContent {
            TsuzukuTheme {
                val history by application.habitRepository.observeHistory().collectAsStateWithLifecycle(
                    initialValue = app.tsuzuku.model.HabitHistorySnapshot(emptyList(), emptyList()),
                )
                WidgetConfigurationScreen(
                    habits = history.habits,
                    singleHabit = singleHabit,
                    initialSelection = widgetSettings.selectedHabitIds(widgetId),
                    onSave = { ids ->
                        widgetSettings.save(widgetId, ids)
                        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                            TsuzukuWidgets.updateAll(this@WidgetConfigurationActivity)
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
    singleHabit: Boolean,
    initialSelection: Set<Long>,
    onSave: (Set<Long>) -> Unit,
) {
    val selected = remember { mutableStateListOf<Long>().apply { addAll(initialSelection) } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(stringResource(R.string.configure_widget), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(if (singleHabit) R.string.widget_pick_one else R.string.configure_widget_description),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        items(habits, key = Habit::id) { habit ->
            val checked = habit.id in selected
            Card(shape = MaterialTheme.shapes.large) {
                val rowModifier = if (singleHabit) {
                    Modifier.selectable(selected = checked, role = Role.RadioButton) {
                        selected.clear(); selected.add(habit.id)
                    }
                } else {
                    Modifier.toggleable(value = checked, role = Role.Checkbox) {
                        if (checked) selected.remove(habit.id) else selected.add(habit.id)
                    }
                }
                Row(
                    rowModifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(habit.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(habit.name, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    if (singleHabit) RadioButton(selected = checked, onClick = null) else Checkbox(checked = checked, onCheckedChange = null)
                }
            }
        }
        item {
            Button(
                onClick = { onSave(selected.toSet()) },
                enabled = !singleHabit || selected.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Text(stringResource(R.string.save_widget))
            }
        }
    }
}
