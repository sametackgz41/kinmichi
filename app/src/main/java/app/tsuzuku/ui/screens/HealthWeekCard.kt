package app.tsuzuku.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.tsuzuku.R
import app.tsuzuku.health.HealthSync
import app.tsuzuku.model.Habit
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle

private val HealthGreen = Color(0xFF3F7D5A)

/** Reads the last 30 days when the habit page opens (and again on return), then shows the card. */
@Composable
internal fun HealthWeekCardLoader(habit: Habit) {
    val context = LocalContext.current
    var days by remember(habit.id) { mutableStateOf<List<Pair<LocalDate, Double>>?>(null) }
    var readable by remember(habit.id) { mutableStateOf(true) }
    var refresh by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(habit.id) {
        refresh++
        onPauseOrDispose { }
    }
    val request = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.health.connect.client.PermissionController.createRequestPermissionResultContract(),
    ) { refresh++ }
    LaunchedEffect(habit.id, habit.healthMetric, refresh) {
        readable = HealthSync.canRead(context, habit.healthMetric)
        if (readable) days = HealthSync.history(context, habit.healthMetric, habit.unit)
    }
    if (!readable) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        ) {
            Text(stringResource(R.string.health_card_source), style = MaterialTheme.typography.labelLarge, color = HealthGreen)
            Text(stringResource(R.string.health_card_no_access), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
            TextButton(onClick = {
                val permission = HealthSync.permissionFor(habit.healthMetric)
                if (HealthSync.status(context) == HealthSync.Status.Ready && permission != null) request.launch(setOf(permission))
                else HealthSync.open(context)
            }) { Text(stringResource(R.string.health_card_allow)) }
        }
        return
    }
    days?.let { HealthWeekCard(habit, it) }
}

/** Today against the target, the week as bars with the goal line, this week's total and the 30-day average. */
@Composable
internal fun HealthWeekCard(habit: Habit, days: List<Pair<LocalDate, Double>>) {
    val locale = LocalConfiguration.current.locales[0]
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    fun fmt(v: Double) = number.format(if (v >= 100) Math.round(v).toDouble() else v)
    val today = days.lastOrNull()?.second ?: 0.0
    val week = days.takeLast(7)
    val monday = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
    val thisWeek = days.filter { !it.first.isBefore(monday) }.sumOf { it.second }
    val withData = days.filter { it.second > 0 }
    val average = if (withData.isEmpty()) 0.0 else withData.sumOf { it.second } / withData.size
    val target = habit.target
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.health_card_source), style = MaterialTheme.typography.labelLarge, color = HealthGreen)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(fmt(today), fontSize = 44.sp, lineHeight = 48.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold)
            Text(
                "  " + stringResource(R.string.health_card_today, fmt(target), habit.unit),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }
        val max = (week.maxOfOrNull { it.second } ?: 0.0).coerceAtLeast(target) * 1.1
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val w = size.width / 7f
            week.forEachIndexed { i, (_, v) ->
                val h = (size.height * (v / max)).toFloat()
                drawRoundRect(
                    if (v >= target) primary else primary.copy(alpha = 0.4f),
                    topLeft = Offset(i * w + w * 0.2f, size.height - h),
                    size = Size(w * 0.6f, h),
                    cornerRadius = CornerRadius(12f, 12f),
                )
            }
            if (target > 0) {
                val gy = (size.height * (1 - target / max)).toFloat()
                drawLine(outline, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Row {
            week.forEach { (day, _) ->
                Text(
                    day.dayOfWeek.getDisplayName(TextStyle.SHORT, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Row(Modifier.padding(top = 4.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.health_card_week), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${fmt(thisWeek)} ${habit.unit}".trim(), style = MaterialTheme.typography.titleMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.health_card_average), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.health_card_per_day, fmt(average)), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
