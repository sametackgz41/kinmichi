package app.tsuzuku.previews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun JourneyFrame(row: @Composable () -> Unit, sheet: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("BUGÜN EKRANINDA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 8.dp))
            row()
            MockRow("🚶", 0xFF3F7D5A, "Yürüyüş", "tamamlandı", done = true, trailing = { MockCheck(true) })
        }
        Spacer(Modifier.weight(1f))
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outlineVariant))
                sheet()
            }
        }
    }
}

@Composable
private fun TodayRing(fraction: Float, label: String) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(40.dp)) {
            val s = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = s)
            drawArc(primary, -90f, 360f * fraction, false, style = s)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SerifItalic(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontStyle = FontStyle.Italic),
    color = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** A: the usual row with a thin bar for the whole book; a big number to type the page. */
@Composable
internal fun JourneyOptionA() = JourneyFrame(row = {
    MockRow("📖", 0xFF5068A9, "Sefiller", "bugün 12 / 20 sayfa", trailing = { TodayRing(0.6f, "12") }, below = {
        Row(Modifier.padding(start = 56.dp, top = 8.dp, end = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { 184f / 320f }, modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)))
            Text("184/320", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 10.dp))
        }
    })
}) {
    Text("Kaçıncı sayfadasın?", style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        FilledTonalIconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Remove, null) }
        Text("184", style = MaterialTheme.typography.displayMedium, modifier = Modifier.padding(horizontal = 28.dp))
        FilledTonalIconButton(onClick = {}, modifier = Modifier.size(48.dp)) { Icon(Icons.Rounded.Add, null) }
    }
    Text("/ 320 sayfa · bugün +12", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterHorizontally))
    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
}

/** B: a taller card like a book on a shelf, with a finish-date guess. */
@Composable
internal fun JourneyOptionB() = JourneyFrame(row = {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(54.dp, 76.dp).clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp, topStart = 3.dp, bottomStart = 3.dp))
                    .background(Color(0xFF5068A9)),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.align(Alignment.CenterStart).width(5.dp).fillMaxSize().background(Color.Black.copy(alpha = 0.18f)))
                Text("📖", fontSize = 24.sp)
            }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text("Sefiller", style = MaterialTheme.typography.titleMedium)
                SerifItalic("bitmesine 136 sayfa · ~7 gün")
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(progress = { 184f / 320f }, modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)))
                    Text("%57", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                }
            }
            TodayRing(0.6f, "12")
        }
    }
}) {
    Text("Sefiller · nerede kaldın?", style = MaterialTheme.typography.titleLarge)
    Text("Son: 172. sayfa · dün", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("+5", "+10", "+20", "+50").forEach { l -> FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) { Text(l) } }
    }
    Row(verticalAlignment = Alignment.Bottom) {
        Text("184", style = MaterialTheme.typography.displaySmall)
        Text("  . sayfa  ·  bugün +12", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
    }
    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
}

/** C: a slider across the whole book; the row shows position and pace. */
@Composable
internal fun JourneyOptionC() = JourneyFrame(row = {
    MockRow("📖", 0xFF5068A9, "Sefiller", "184 / 320 · bugün +12", trailing = { TodayRing(0.6f, "%57") })
}) {
    Text("Kaçıncı sayfadasın?", style = MaterialTheme.typography.titleLarge)
    Row(verticalAlignment = Alignment.Bottom) {
        Text("184", style = MaterialTheme.typography.displaySmall)
        Text("  / 320", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 6.dp))
    }
    Slider(value = 184f / 320f, onValueChange = {})
    HorizontalDivider()
    Row {
        Column(Modifier.weight(1f)) {
            Text("Bugün", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+12 sayfa", style = MaterialTheme.typography.titleMedium)
        }
        Column(Modifier.weight(1f)) {
            Text("Bu hızla biter", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("15 Kasım", style = MaterialTheme.typography.titleMedium)
        }
    }
    Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Kaydet") }
}
