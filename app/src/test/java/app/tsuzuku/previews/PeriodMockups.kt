package app.tsuzuku.previews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun PeriodFrame(content: @Composable () -> Unit, today: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ne sıklıkla?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        content()
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        Text("BUGÜN EKRANINDA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
        today()
    }
}

@Composable
private fun Stepper(value: Int, suffix: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Remove, null, Modifier.size(18.dp)) }
        Text("$value", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 12.dp))
        FilledTonalIconButton(onClick = {}, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Add, null, Modifier.size(18.dp)) }
        Text(suffix, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
    }
}

@Composable
private fun Periods(selected: Int) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        listOf("Günlük", "Haftalık", "Aylık", "Özel").forEachIndexed { i, l ->
            SegmentedButton(selected = i == selected, onClick = {}, shape = SegmentedButtonDefaults.itemShape(i, 4), label = { Text(l, maxLines = 1) })
        }
    }
}

@Composable
private fun DayDots(total: Int, done: Set<Int>, today: Int, size: Int = 22) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val on = i in done
            Box(
                Modifier.size(size.dp).clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest)
                    .then(if (i == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
            )
        }
    }
}

/** A: a fourth tab "Custom" and a sentence with two steppers. */
@Composable
internal fun PeriodOptionA() = PeriodFrame(content = {
    Periods(3)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Her", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Stepper(10, "günde")
        Stepper(4, "kez")
        Text("Bu dönem: 10 gün içinde 4 kez", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DayDots(10, setOf(0, 2), 3)
    }
}) {
    MockRow("🏊", 0xFF3F7D5A, "Yüzme", "bu dönem 2/4 · 7 gün kaldı", trailing = { MockCheck(false) })
}

/** B: ready-made rhythms as chips, plus "custom". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PeriodOptionB() = PeriodFrame(content = {
    Periods(3)
    Text("Hazır ritimler", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 4.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("2 günde 1", "3 günde 2", "10 günde 4", "2 haftada 3", "Özel…").forEachIndexed { i, l ->
            FilterChip(selected = i == 2, onClick = {}, label = { Text(l) })
        }
    }
    Text(
        "Dönemler alışkanlığın başladığı günden itibaren 10'ar gün olarak sayılır. Fazlası sonraki döneme devretmez.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}) {
    MockRow("🏊", 0xFF3F7D5A, "Yüzme", "7 gün kaldı", trailing = { MockCheck(false) }, below = {
        Row(Modifier.padding(start = 56.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            DayDots(4, setOf(0, 1), -1, size = 14)
            Text("2/4", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
        }
    })
}

@Composable
private fun PeriodRing(done: Int, target: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            val s = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = s)
            drawArc(primary, -90f, 360f * done / target, false, style = s)
        }
        Text("$done/$target", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

/** C: the sentence plus a strip of the next blocks so the rhythm is visible. */
@Composable
internal fun PeriodOptionC() = PeriodFrame(content = {
    Periods(3)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Stepper(4, "kez")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Stepper(10, "günde bir")
    }
    Text("Önümüzdeki dönemler", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 6.dp))
    listOf("8–17 Kas" to true, "18–27 Kas" to false, "28 Kas–7 Ara" to false).forEach { (label, current) ->
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                .background(if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(if (current) "şimdiki · 4 kez" else "4 kez", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}) {
    MockRow("🏊", 0xFF3F7D5A, "Yüzme", "10 günde 4 · 7 gün kaldı", trailing = { PeriodRing(2, 4) })
}
