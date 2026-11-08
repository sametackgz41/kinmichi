package app.tsuzuku.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.tsuzuku.R
import app.tsuzuku.health.HealthSync
import app.tsuzuku.model.Habit
import app.tsuzuku.model.HealthMetric
import java.text.NumberFormat
import java.time.LocalDate
import kotlinx.coroutines.launch

private val HealthGreen = Color(0xFF3F7D5A)

/** One kind of health data on the page: its last 7 days in its own unit, null when not allowed. */
data class HealthCardData(val metric: HealthMetric, val days: List<Pair<LocalDate, Double>>?)

private val PageMetrics = listOf(HealthMetric.Steps, HealthMetric.Distance, HealthMetric.Calories, HealthMetric.ExerciseDuration, HealthMetric.SleepDuration)

/** The unit each kind is read in on this page. */
private fun pageUnit(metric: HealthMetric) = when (metric) {
    HealthMetric.Distance -> "km"
    HealthMetric.ExerciseDuration, HealthMetric.SleepDuration -> "dk"
    else -> ""
}

private fun metricEmoji(metric: HealthMetric) = when (metric) {
    HealthMetric.Steps -> "🚶"
    HealthMetric.Distance -> "📏"
    HealthMetric.Calories -> "🔥"
    HealthMetric.ExerciseDuration -> "🏃"
    HealthMetric.SleepDuration -> "😴"
    HealthMetric.None -> "💚"
}

private fun metricLabel(metric: HealthMetric) = when (metric) {
    HealthMetric.Steps -> R.string.health_steps
    HealthMetric.Distance -> R.string.health_distance
    HealthMetric.Calories -> R.string.health_calories
    HealthMetric.ExerciseDuration -> R.string.health_exercise
    HealthMetric.SleepDuration -> R.string.health_sleep
    HealthMetric.None -> R.string.health_none
}

/** A starting habit for "make a habit from this": the right type, unit and a usual target. */
internal fun habitFromMetric(metric: HealthMetric, name: String, unit: String): Habit = Habit(
    id = 0,
    name = name,
    emoji = metricEmoji(metric),
    colorArgb = 0,
    type = if (metric == HealthMetric.ExerciseDuration || metric == HealthMetric.SleepDuration) app.tsuzuku.model.HabitType.Duration else app.tsuzuku.model.HabitType.Count,
    direction = app.tsuzuku.model.HabitDirection.Build,
    target = when (metric) {
        HealthMetric.Steps -> 8000.0
        HealthMetric.Distance -> 5.0
        HealthMetric.Calories -> 500.0
        HealthMetric.ExerciseDuration -> 30.0
        HealthMetric.SleepDuration -> 420.0
        HealthMetric.None -> 1.0
    },
    unit = unit,
    step = 1.0,
    scheduleDaysMask = 127,
    sortOrder = 0,
    healthMetric = metric,
)

/** Settings › Health: what Health Connect has for today and the week, and which habit uses it. */
@Composable
internal fun HealthPage(
    habits: List<Habit>,
    onBack: () -> Unit,
    onCreateHabit: (Habit) -> Unit,
    onOpenHabit: (Long) -> Unit,
    preview: List<HealthCardData>? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var cards by remember { mutableStateOf(preview) }
    var granted by remember { mutableIntStateOf(if (preview != null) preview.count { it.days != null } else 0) }
    var refresh by remember { mutableIntStateOf(0) }
    val request = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) { refresh++ }
    LifecycleResumeEffect(Unit) { refresh++; onPauseOrDispose { } }
    LaunchedEffect(refresh) {
        if (preview != null) return@LaunchedEffect
        val allowed = HealthSync.granted(context)
        granted = PageMetrics.count { HealthSync.permissionFor(it) in allowed }
        cards = PageMetrics.map { m ->
            HealthCardData(m, if (HealthSync.permissionFor(m) in allowed) HealthSync.history(context, m, pageUnit(m), days = 7) else null)
        }
    }
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    fun fmt(v: Double) = number.format(if (v >= 100) Math.round(v).toDouble() else v)
    val hm = stringResource(R.string.month_hm)
    val mOnly = stringResource(R.string.month_m)
    fun duration(minutes: Double): String {
        val m = Math.round(minutes).toInt()
        return if (m >= 60) hm.format(m / 60, m % 60) else mOnly.format(m)
    }
    LazyColumn(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding(),
        contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) }
                Text(stringResource(R.string.health_page_title), style = MaterialTheme.typography.titleLarge)
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(HealthGreen.copy(alpha = 0.12f)).padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💚", fontSize = 18.sp)
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(stringResource(R.string.health_page_connected), style = MaterialTheme.typography.titleSmall, color = HealthGreen)
                    Text(stringResource(R.string.health_page_allowed, granted, PageMetrics.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    scope.launch {
                        val allowed = HealthSync.granted(context)
                        if (allowed.containsAll(HealthSync.allPermissions)) HealthSync.open(context) else request.launch(HealthSync.allPermissions)
                    }
                }) { Text(stringResource(R.string.health_page_permissions)) }
            }
        }
        val list = cards
        if (list == null) {
            item { Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        } else {
            items(list, key = { it.metric.name }) { card ->
                val linked = habits.filter { it.healthMetric == card.metric }
                val days = card.days
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${metricEmoji(card.metric)}  ${stringResource(metricLabel(card.metric))}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (days == null) {
                            Text(stringResource(R.string.health_page_not_allowed), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                stringResource(R.string.health_card_allow),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { HealthSync.permissionFor(card.metric)?.let { request.launch(setOf(it)) } },
                            )
                        } else {
                            val today = days.lastOrNull()?.second ?: 0.0
                            val isTime = card.metric == HealthMetric.ExerciseDuration || card.metric == HealthMetric.SleepDuration
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    if (isTime) duration(today) else fmt(today),
                                    fontSize = 24.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold,
                                )
                                if (!isTime) Text(
                                    " " + when (card.metric) {
                                        HealthMetric.Steps -> stringResource(R.string.health_unit_steps)
                                        HealthMetric.Distance -> "km"
                                        else -> "kcal"
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 3.dp),
                                )
                            }
                            if (linked.isNotEmpty()) {
                                Text(
                                    "↳ " + linked.joinToString { it.name },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = HealthGreen,
                                    modifier = Modifier.clickable { onOpenHabit(linked.first().id) },
                                )
                            } else {
                                val name = stringResource(metricLabel(card.metric))
                                val unit = when (card.metric) {
                                    HealthMetric.Steps -> stringResource(R.string.health_unit_steps)
                                    HealthMetric.Distance -> "km"
                                    HealthMetric.Calories -> "kcal"
                                    else -> stringResource(R.string.unit_minutes)
                                }
                                Text(
                                    stringResource(R.string.health_page_create),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onCreateHabit(habitFromMetric(card.metric, name, unit)) },
                                )
                            }
                        }
                    }
                    if (days != null) {
                        val primary = MaterialTheme.colorScheme.primary
                        val max = (days.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(1.0)
                        Canvas(Modifier.size(110.dp, 48.dp)) {
                            val w = size.width / days.size.coerceAtLeast(1)
                            days.forEachIndexed { i, (_, v) ->
                                val h = (size.height * (v / max)).toFloat().coerceAtLeast(3f)
                                drawRoundRect(
                                    if (i == days.lastIndex) primary else primary.copy(alpha = 0.35f),
                                    Offset(i * w + w * 0.18f, size.height - h), Size(w * 0.64f, h), CornerRadius(6f, 6f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
