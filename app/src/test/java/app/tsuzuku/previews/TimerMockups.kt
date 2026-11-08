package app.tsuzuku.previews

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Shade = Color(0xFF1B1D22)
private val Card = Color(0xFF2A2D34)
private val OnCard = Color(0xFFE8E6E1)
private val Muted = Color(0xFFA9A7A1)
private val Accent = Color(0xFFB7C5F2)

@Composable
private fun ShadeFrame(caption: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(Shade).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("21:14", color = OnCard, fontSize = 44.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(start = 8.dp, top = 24.dp))
        Text("Pazar, 8 Kasım", color = Muted, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp, bottom = 16.dp))
        content()
        Spacer(Modifier.height(8.dp))
        Text(caption, color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
    }
}

@Composable
private fun NotifHeader() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Text("金", fontSize = 10.sp, color = Shade)
        }
        Text("  Kinmichi · sürüyor", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun NotifAction(label: String) {
    Text(label, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(end = 22.dp))
}

@Composable
private fun NotifCard(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Card).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NotifHeader()
        content()
    }
}

/** A: counts up, the goal and what is left underneath, a progress bar. */
@Composable
internal fun TimerOptionA() = ShadeFrame("Ekran kapalıyken de sayar. Hedefe gelince ses çıkarır ve kaydeder, istersen saymaya devam eder.") {
    NotifCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("📖", fontSize = 26.sp)
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Kitap oku", color = OnCard, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text("hedef 20 dk · 7:26 kaldı", color = Muted, fontSize = 13.sp)
            }
            Text("12:34", color = OnCard, fontSize = 30.sp, fontWeight = FontWeight.Light)
        }
        LinearProgressIndicator(progress = { 0.63f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Accent, trackColor = Color(0xFF41454E))
        Row { NotifAction("DURAKLAT"); NotifAction("BİTİR") }
    }
}

/** B: counts down to the goal, with +5 min. */
@Composable
internal fun TimerOptionB() = ShadeFrame("Geri sayar; bitince alışkanlık kendiliğinden tamamlanır. +5 dk ile hedefi o gün için uzatırsın.") {
    NotifCard {
        Text("📖  Kitap oku", color = OnCard, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("7:26", color = OnCard, fontSize = 52.sp, fontWeight = FontWeight.Light)
            Text("  kaldı", color = Muted, fontSize = 16.sp, modifier = Modifier.padding(bottom = 12.dp))
        }
        LinearProgressIndicator(progress = { 0.63f }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = Accent, trackColor = Color(0xFF41454E))
        Row { NotifAction("DURAKLAT"); NotifAction("+5 DK"); NotifAction("BİTİR") }
    }
}

/** C: a small notification plus a bar that stays at the bottom of the app, like a music player. */
@Composable
internal fun TimerOptionC() {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            ShadeFrame("Bildirim kısa ve sade; uygulamanın içinde de her sayfanın altında küçük bir çubuk durur.") {
                NotifCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📖  Kitap oku · ", color = OnCard, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Text("12:34 / 20:00", color = OnCard, fontSize = 16.sp)
                    }
                    Row { NotifAction("DURAKLAT"); NotifAction("BİTİR") }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(16.dp)) {
            Text("UYGULAMANIN İÇİNDE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 10.dp))
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer, shadowElevation = 3.dp) {
                Column {
                    Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("📖", fontSize = 22.sp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text("Kitap oku", style = MaterialTheme.typography.titleSmall)
                            Text("12:34 / 20:00", style = MaterialTheme.typography.bodySmall)
                        }
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Pause, null) }
                        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Stop, null) }
                    }
                    LinearProgressIndicator(progress = { 0.63f }, modifier = Modifier.fillMaxWidth().height(3.dp))
                }
            }
        }
    }
}
