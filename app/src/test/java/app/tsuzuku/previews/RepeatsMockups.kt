package app.tsuzuku.previews

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private data class Slot(val name: String, val time: String, val done: Boolean)
private val pillSlots = listOf(Slot("Sabah", "08.00", true), Slot("Öğle", "13.00", false), Slot("Akşam", "21.00", false))

@Composable
internal fun MockRow(
    emoji: String,
    color: Long,
    name: String,
    context: String,
    done: Boolean = false,
    trailing: @Composable () -> Unit,
    below: (@Composable () -> Unit)? = null,
) {
    Surface(
        color = if (done) Color.Transparent else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (done) 0.5f else 1f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(42.dp).background(Color(color).copy(alpha = 0.16f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text(emoji, fontSize = 22.sp) }
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text(name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        context,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                trailing()
            }
            below?.invoke()
        }
    }
}

@Composable
internal fun MockCheck(done: Boolean) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(34.dp).clip(CircleShape)
                .background(if (done) MaterialTheme.colorScheme.primary else Color.Transparent)
                .then(if (done) Modifier else Modifier.border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            if (done) Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun SplitRing(parts: Int, filled: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(38.dp)) {
            val s = Stroke(5.dp.toPx(), cap = StrokeCap.Round)
            val gap = 22f
            val sweep = 360f / parts
            for (i in 0 until parts) drawArc(if (i < filled) primary else track, -90f + i * sweep + gap / 2, sweep - gap, false, style = s)
        }
        Text("$filled/$parts", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun TodayFrame(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 6.dp))
        content()
    }
}

@Composable
private fun OtherRows() {
    MockRow("📖", 0xFF5068A9, "Kitap oku", "20 sayfa · 21.00", trailing = { MockCheck(false) })
    MockRow("🚶", 0xFF3F7D5A, "Yürüyüş", "tamamlandı", done = true, trailing = { MockCheck(true) })
}

/** A: one row, a chip for each time of day. */
@Composable
internal fun RepeatsOptionA() = TodayFrame("Bugün") {
    MockRow("💊", 0xFFB5654A, "İlaç", "sıradaki: Öğle · 13.00", trailing = {
        Text("1/3", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
    }, below = {
        Row(Modifier.padding(top = 10.dp, end = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            pillSlots.forEachIndexed { i, s ->
                val next = i == 1
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (s.done) MaterialTheme.colorScheme.primary else if (next) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    border = if (s.done || next) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.weight(1f),
                ) {
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        if (s.done) Icon(Icons.Rounded.Check, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        Text(
                            " ${s.name} ${s.time}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (s.done) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    })
    OtherRows()
}

/** B: the usual row; the check becomes a ring in parts, each tap fills the next one. */
@Composable
internal fun RepeatsOptionB() = TodayFrame("Bugün") {
    MockRow("💊", 0xFFB5654A, "İlaç", "Sabah ✓ · sırada Öğle 13.00", trailing = { SplitRing(3, 1) })
    MockRow("🦷", 0xFF5068A9, "Diş fırçala", "Sabah ✓ · Akşam 22.30", trailing = { SplitRing(2, 1) })
    OtherRows()
}

/** C: the habit shows up once in each part of the day. */
@Composable
internal fun RepeatsOptionC() = TodayFrame("Bugün") {
    @Composable
    fun part(label: String) = Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    part("SABAH")
    MockRow("💊", 0xFFB5654A, "İlaç", "1/3 · 08.00", done = true, trailing = { MockCheck(true) })
    part("ÖĞLE")
    MockRow("💊", 0xFFB5654A, "İlaç", "2/3 · 13.00", trailing = { MockCheck(false) })
    MockRow("🚶", 0xFF3F7D5A, "Yürüyüş", "tamamlandı", done = true, trailing = { MockCheck(true) })
    part("AKŞAM")
    MockRow("💊", 0xFFB5654A, "İlaç", "3/3 · 21.00", trailing = { MockCheck(false) })
    MockRow("📖", 0xFF5068A9, "Kitap oku", "20 sayfa · 21.00", trailing = { MockCheck(false) })
}

/** The editor part, the same for every option. */
@Composable
internal fun RepeatsEditor() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Ne zaman?", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Günde birden fazla", style = MaterialTheme.typography.bodyLarge)
                Text("Her biri ayrı işaretlenir, ayrı hatırlatılır", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = true, onCheckedChange = {})
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Sabah · Akşam", "Sabah · Öğle · Akşam", "3 saatte bir").forEachIndexed { i, l ->
                FilterChip(selected = i == 1, onClick = {}, label = { Text(l) })
            }
        }
        listOf(Triple("☀️", "Sabah", "08.00"), Triple("🌤️", "Öğle", "13.00"), Triple("🌙", "Akşam", "21.00")).forEach { (e, n, t) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(e, fontSize = 20.sp)
                Text(n, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(t, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
                }
                IconButton(onClick = {}) { Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        TextButton(onClick = {}) {
            Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
            Text("Tekrar ekle", Modifier.padding(start = 6.dp))
        }
        Text("Hepsi bitince gün tamamlanmış sayılır ve seriye eklenir.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
