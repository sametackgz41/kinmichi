package app.tsuzuku.previews

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val serif get() = app.tsuzuku.ui.theme.SerifDisplay

/** A: story cards like the year review; each month has its own palette (October: Momiji). */
@Composable
internal fun MonthOptionA() {
    Row(Modifier.fillMaxSize().background(Color(0xFF111111)).padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(
            Triple(listOf(Color(0xFF5C2A1E), Color(0xFFB2472A)), "🍁", Triple("Ekim", "Ayın nasıl geçti, bir bakalım.", 64)),
            Triple(listOf(Color(0xFF3D2E05), Color(0xFF8A6A1F)), "📈", Triple("+%18", "Eylül'e göre daha çok gün tamamladın. 22 gün, 3 mükemmel gün.", 64)),
            Triple(listOf(Color(0xFF1F3209), Color(0xFF4E6B2E)), "💧", Triple("Su iç", "Ayın yıldızı: 27 gün. En uzun serin 14 gün.", 44)),
        ).forEachIndexed { i, (grad, emoji, t) ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(22.dp)).background(Brush.verticalGradient(grad)).padding(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(6) { k -> Box(Modifier.weight(1f).height(3.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = if (k <= i) 0.95f else 0.3f))) }
                }
                Text("Kinmichi · Ekim", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                Spacer(Modifier.weight(1f))
                Text(emoji, fontSize = 40.sp)
                Text(t.first, color = if (i == 1) Color(0xFFE2C57C) else Color.White, fontSize = (t.third * 0.62).sp, fontFamily = serif, fontWeight = FontWeight.Bold)
                Text(t.second, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp, lineHeight = 19.sp)
                Spacer(Modifier.weight(1.2f))
            }
        }
    }
}

/** B: one calm page you scroll, like a monthly letter. */
@Composable
internal fun MonthOptionB() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("EKİM ÖZETİ", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text("İyi bir aydı.", style = MaterialTheme.typography.headlineMedium, fontFamily = serif)
            }
            Icon(Icons.Rounded.Close, null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("84" to "tamamlama", "22" to "aktif gün", "+%18" to "Eylül'e göre").forEach { (n, l) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp)) {
                    Text(n, fontSize = 28.sp, fontFamily = serif, fontWeight = FontWeight.Bold, color = if (n.startsWith("+")) Color(0xFF3F7D5A) else MaterialTheme.colorScheme.onSurface)
                    Text(l, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp)) {
            Text("Ay takvimin", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(10.dp))
            val levels = listOf(0, 2, 3, 3, 1, 3, 2, 3, 3, 3, 0, 1, 3, 3, 2, 3, 3, 3, 3, 2, 1, 3, 3, 3, 4, 3, 2, 3, 3, 3, 1)
            levels.chunked(7).forEach { week ->
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
                    week.forEach { l ->
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(8.dp)).background(
                                when (l) {
                                    4 -> Color(0xFFC9A34A)
                                    0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f + l * 0.25f)
                                },
                            ),
                        )
                    }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("En çok yaptıkların", style = MaterialTheme.typography.titleSmall)
            listOf(Triple("💧", "Su iç", 27), Triple("🧘", "Meditasyon", 21), Triple("📖", "Kitap oku", 16)).forEach { (e, n, c) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e, fontSize = 18.sp)
                    Text(n, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 10.dp).width(96.dp))
                    Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
                        Box(Modifier.fillMaxWidth(c / 31f).fillMaxHeight().clip(RoundedCornerShape(5.dp)).background(MaterialTheme.colorScheme.primary))
                    }
                    Text(" $c", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Share, null); Text("  Paylaş") }
    }
}

/** C: everything on one colourful collage, ready to share. */
@Composable
internal fun MonthOptionC() {
    @Composable
    fun Tile(bg: Color, modifier: Modifier, content: @Composable () -> Unit) =
        Column(modifier.clip(RoundedCornerShape(24.dp)).background(bg).padding(14.dp)) { content() }
    val w = Color.White
    Column(Modifier.fillMaxSize().background(Color(0xFF121212)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Ekim'in", color = w.copy(alpha = 0.7f), fontSize = 16.sp)
        Text("Kinmichi özeti", color = w, fontSize = 32.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
        Row(Modifier.height(170.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Color(0xFFB2472A), Modifier.weight(1.3f).fillMaxHeight()) {
                Text("tamamlama", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("84", color = w, fontSize = 64.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
            }
            Tile(Color(0xFF8A6A1F), Modifier.weight(1f).fillMaxHeight()) {
                Text("Eylül'e göre", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("+%18", color = w, fontSize = 36.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
            }
        }
        Row(Modifier.height(150.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Color(0xFF3B3F8F), Modifier.weight(1f).fillMaxHeight()) {
                Text("en uzun seri", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("🔥 14", color = w, fontSize = 34.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
                Text("Meditasyon", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
            }
            Tile(Color(0xFF4E6B2E), Modifier.weight(1.4f).fillMaxHeight()) {
                Text("ayın yıldızı", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("💧 Su iç", color = w, fontSize = 28.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
                Text("27 gün", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
        Row(Modifier.height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Tile(Color(0xFFA2445E), Modifier.weight(1f).fillMaxHeight()) {
                Text("en keyiflisi", color = w.copy(alpha = 0.85f), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("😄 Koşu", color = w, fontSize = 22.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
            }
            Tile(Color(0xFF2A2A2A), Modifier.weight(1f).fillMaxHeight()) {
                Text("geri dönüş", color = Color(0xFFE2C57C), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text("✨ 3", color = Color(0xFFE2C57C), fontSize = 30.sp, fontFamily = serif, fontWeight = FontWeight.Bold)
            }
        }
        Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = w, contentColor = Color.Black), modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Rounded.Share, null); Text("  Hikâyende paylaş")
        }
    }
}
