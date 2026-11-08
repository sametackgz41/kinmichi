package app.tsuzuku.previews

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Green = Color(0xFF3F7D5A)
private data class M(val emoji: String, val name: String, val today: String, val unit: String, val week: List<Float>, val linked: String?)
private val metrics = listOf(
    M("🚶", "Adım", "6.214", "adım", listOf(.6f, .9f, .7f, 1f, .5f, .85f, .62f), "Günde 8.000 adım"),
    M("📏", "Mesafe", "4,3", "km", listOf(.5f, .8f, .6f, .9f, .4f, .7f, .5f), null),
    M("🔥", "Kalori", "1.850", "kcal", listOf(.7f, .8f, .75f, .9f, .65f, .8f, .7f), null),
    M("🏃", "Egzersiz", "32", "dk", listOf(.3f, .6f, 0f, .8f, .2f, .5f, .4f), "Koşu"),
    M("😴", "Uyku", "6 sa 40 dk", "", listOf(.8f, .7f, .9f, .6f, .85f, .95f, .75f), "7 saat uyku"),
)

@Composable
private fun Head() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, Modifier.padding(end = 12.dp))
        Text("Sağlık", style = MaterialTheme.typography.titleLarge)
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Green.copy(alpha = 0.12f)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("💚", fontSize = 18.sp)
        Column(Modifier.padding(start = 10.dp).weight(1f)) {
            Text("Health Connect bağlı", style = MaterialTheme.typography.titleSmall, color = Green)
            Text("5 veri türüne izin var · 2 dk önce güncellendi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("İzinler", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun MiniBars(week: List<Float>, modifier: Modifier = Modifier) {
    val p = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val w = size.width / week.size
        week.forEachIndexed { i, v ->
            val h = size.height * v.coerceAtLeast(0.04f)
            drawRoundRect(if (i == week.lastIndex) p else p.copy(alpha = 0.35f), Offset(i * w + w * 0.18f, size.height - h), Size(w * 0.64f, h), CornerRadius(6f, 6f))
        }
    }
}

/** A: one card per kind of data: today's value, the week as bars, and which habit uses it. */
@Composable
internal fun HealthPageA() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Head()
        metrics.forEach { m ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${m.emoji}  ${m.name}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(m.today, fontSize = 24.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold)
                        Text(" ${m.unit}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 3.dp))
                    }
                    Text(m.linked?.let { "↳ $it" } ?: "+ Bununla alışkanlık oluştur", style = MaterialTheme.typography.labelMedium,
                        color = if (m.linked != null) Green else MaterialTheme.colorScheme.primary)
                }
                MiniBars(m.week, Modifier.size(110.dp, 48.dp))
            }
        }
    }
}

@Composable
private fun Ring(fraction: Float, label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    val p = MaterialTheme.colorScheme.primary
    val t = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(72.dp)) {
                val s = Stroke(9f, cap = StrokeCap.Round)
                drawArc(t, 0f, 360f, false, style = s)
                drawArc(p, -90f, 360f * fraction, false, style = s)
            }
            Text(emoji, fontSize = 22.sp)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** B: rings for today against your goals, then a week chart for the one you pick. */
@Composable
internal fun HealthPageB() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Head()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ring(0.78f, "/ 8.000 adım", "6.214", "🚶", Modifier.weight(1f))
            Ring(0.64f, "/ 50 dk egzersiz", "32 dk", "🏃", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ring(0.95f, "/ 7 sa uyku", "6 sa 40 dk", "😴", Modifier.weight(1f))
            Ring(0.7f, "kalori", "1.850", "🔥", Modifier.weight(1f))
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Adım", "Uyku", "Kalori").forEachIndexed { i, l -> FilterChip(selected = i == 0, onClick = {}, label = { Text(l) }) }
            }
            MiniBars(metrics[0].week, Modifier.fillMaxWidth().height(110.dp).padding(top = 8.dp))
            Text("Bu hafta 53.900 adım · ortalama 7.120 / gün", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** C: one line for today, then a row per kind that opens its own detail. */
@Composable
internal fun HealthPageC() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Head()
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(18.dp)) {
            Text("Bugün", style = MaterialTheme.typography.labelLarge)
            Text("6.214 adım", fontSize = 36.sp, fontFamily = app.tsuzuku.ui.theme.SerifDisplay, fontWeight = FontWeight.Bold)
            Text("4,3 km · 1.850 kcal · 32 dk egzersiz · dün gece 6 sa 40 dk uyku", style = MaterialTheme.typography.bodyMedium)
        }
        Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                metrics.forEach { m ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(m.emoji, fontSize = 20.sp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.bodyLarge)
                            Text(m.linked?.let { "Bağlı: $it" } ?: "Bir alışkanlığa bağla", style = MaterialTheme.typography.bodySmall,
                                color = if (m.linked != null) Green else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${m.today} ${m.unit}".trim(), style = MaterialTheme.typography.titleSmall)
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
