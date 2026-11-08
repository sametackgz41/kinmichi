package app.tsuzuku.previews

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val week = listOf(6.1f, 9.4f, 7.8f, 10.2f, 5.3f, 8.9f, 6.2f)
private val days = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz")

@Composable
private fun DetailHead() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF3F7D5A).copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text("🚶", fontSize = 24.sp) }
        Column(Modifier.padding(start = 14.dp)) {
            Text("Günde 8.000 adım", style = MaterialTheme.typography.titleLarge)
            Text("🔥 12 gün seri", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Bars(highlightGoal: Boolean = true) {
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    val outline = MaterialTheme.colorScheme.outline
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val max = 11f
            val w = size.width / week.size
            week.forEachIndexed { i, v ->
                val h = size.height * v / max
                drawRoundRect(
                    if (v >= 8f) primary else primary.copy(alpha = 0.4f),
                    topLeft = Offset(i * w + w * 0.2f, size.height - h),
                    size = androidx.compose.ui.geometry.Size(w * 0.6f, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                )
            }
            if (highlightGoal) {
                val gy = size.height * (1 - 8f / max)
                drawLine(outline, Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
            }
            drawLine(track, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Row { days.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) } }
    }
}

@Composable
private fun Frame(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

/** A: a card on the habit page: today, the week as bars with the goal line, 30-day average. */
@Composable
internal fun HealthOptionA() = Frame {
    DetailHead()
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("💚  Health Connect'ten", style = MaterialTheme.typography.labelLarge, color = Color(0xFF3F7D5A), modifier = Modifier.weight(1f))
            Text("2 dk önce", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text("6.214", fontSize = 44.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold)
            Text("  / 8.000 adım bugün", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        }
        Bars()
        Row {
            Column(Modifier.weight(1f)) { Text("Bu hafta", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("53.900 adım", style = MaterialTheme.typography.titleMedium) }
            Column(Modifier.weight(1f)) { Text("30 gün ortalaması", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("7.120 / gün", style = MaterialTheme.typography.titleMedium) }
        }
    }
}

/** B: lightest touch: a tag on the Today row and one line on the habit page. */
@Composable
internal fun HealthOptionB() = Frame {
    Text("BUGÜN EKRANINDA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    MockRow("🚶", 0xFF3F7D5A, "Günde 8.000 adım", "💚 6.214 / 8.000 · otomatik", trailing = { MockCheck(false) })
    MockRow("😴", 0xFF5068A9, "7 saat uyku", "💚 6 sa 40 dk · dün gece", done = false, trailing = { MockCheck(false) })
    Spacer(Modifier.height(8.dp))
    Text("ALIŞKANLIK SAYFASINDA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Card {
        Text("💚 Health Connect'ten otomatik doluyor · son 30 günde ortalama 7.120 adım", style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = {}) { Text("Health Connect'te aç") }
    }
}

/** C: a fuller card: trend line over 30 days, which app the data came from, best day. */
@Composable
internal fun HealthOptionC() = Frame {
    DetailHead()
    Card {
        Text("Son 30 gün", style = MaterialTheme.typography.titleSmall)
        val primary = MaterialTheme.colorScheme.primary
        val pts = listOf(5.2f, 6.1f, 7.4f, 6.8f, 8.1f, 7.9f, 6.5f, 7.2f, 8.8f, 9.1f, 7.7f, 6.9f, 7.5f, 8.4f, 9.6f, 8.2f, 7.1f, 6.4f, 7.9f, 8.7f, 9.9f, 8.5f, 7.6f, 8.1f, 9.2f, 10.2f, 8.9f, 7.8f, 6.2f, 6.2f)
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val p = Path()
            pts.forEachIndexed { i, v ->
                val x = size.width * i / (pts.size - 1)
                val y = size.height * (1 - (v - 4f) / 7f)
                if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
            }
            drawPath(p, primary, style = Stroke(5f, cap = StrokeCap.Round))
            val gy = size.height * (1 - 4f / 7f)
            drawLine(Color(0xFF3F7D5A), Offset(0f, gy), Offset(size.width, gy), strokeWidth = 2f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f)))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Bugün" to "6.214", "En iyi gün" to "10.240", "Hedefi tuttu" to "19 / 30").forEach { (l, v) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(12.dp)) {
                    Text(l, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        Text("Kaynak: Samsung Health → Health Connect · 2 dk önce", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
