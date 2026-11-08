package app.tsuzuku.previews

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFC9A34A)
private val serif get() = app.tsuzuku.ui.theme.SerifDisplay

@Composable
private fun Avatar(size: Int = 72, emoji: String = "🦊", ring: Boolean = false) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
            .then(if (ring) Modifier.border(3.dp, Gold, CircleShape) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size * 0.48).sp) }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier = Modifier, gold: Boolean = false) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(12.dp)) {
        Text(value, fontSize = 24.sp, fontFamily = serif, fontWeight = FontWeight.Bold, color = if (gold) Color(0xFF8A6A1F) else MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingsTop() {
    Column(Modifier.fillMaxWidth()) {
        Text("SETTINGS'TE EN ÜSTTE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 8.dp))
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(56)
                Column(Modifier.padding(start = 14.dp).weight(1f)) {
                    Text("Samet", style = MaterialTheme.typography.titleLarge)
                    Text("41samet49@gmail.com", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("🔥 12  ·  ✨ 47  ·  84 gün", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8A6A1F), modifier = Modifier.padding(top = 2.dp))
                }
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null)
            }
        }
    }
}

/** A: an identity card: avatar, name, member since, the numbers that matter, badges. */
@Composable
internal fun ProfileOptionA() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsTop()
        Spacer(Modifier.height(4.dp))
        Text("PROFİL SAYFASI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceContainerLow)))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box { Avatar(92) ; Box(Modifier.align(Alignment.BottomEnd).size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp)) } }
            Text("Samet", style = MaterialTheme.typography.headlineMedium, fontFamily = serif, modifier = Modifier.padding(top = 10.dp))
            Text("41samet49@gmail.com", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("14 Ağustos 2026'dan beri yolda · 84 gün", style = MaterialTheme.typography.labelMedium, color = Color(0xFF8A6A1F), modifier = Modifier.padding(top = 6.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("1.248", "tamamlama", Modifier.weight(1f)); Stat("63", "en uzun seri", Modifier.weight(1f)); Stat("47", "geri dönüş", Modifier.weight(1f), gold = true)
        }
        Text("Rozetler", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("🌱", "🔥", "✨", "🏔️", "🌕").forEachIndexed { i, e ->
                Box(Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(16.dp)).background(if (i < 4) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                    Text(e, fontSize = 24.sp, color = if (i < 4) Color.Unspecified else Color.Gray)
                }
            }
        }
    }
}

/** B: the journey: a golden path of milestones from the first day to today. */
@Composable
internal fun ProfileOptionB() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsTop()
        Spacer(Modifier.height(4.dp))
        Text("PROFİL SAYFASI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(64, ring = true)
            Column(Modifier.padding(start = 14.dp)) {
                Text("Samet", style = MaterialTheme.typography.headlineSmall, fontFamily = serif)
                Text("Altın yolda 84. gün", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF8A6A1F))
            }
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp)) {
            listOf(
                Triple("Bugün", "5 alışkanlık, 🔥 12 gün seri", true),
                Triple("2 Kas", "✨ 47. geri dönüş", false),
                Triple("19 Eki", "🏔️ İlk 30 günlük seri: Su iç", false),
                Triple("3 Eyl", "📖 Kitap oku başladı", false),
                Triple("14 Ağu", "🌱 Yolun başı", false),
            ).forEachIndexed { i, (d, t, now) ->
                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 14.dp)) {
                        Box(Modifier.size(14.dp).clip(CircleShape).background(if (now) Gold else MaterialTheme.colorScheme.outlineVariant))
                        if (i < 4) Box(Modifier.size(2.dp, 38.dp).background(Gold.copy(alpha = 0.5f)))
                    }
                    Column(Modifier.padding(top = 0.dp)) {
                        Text(t, style = MaterialTheme.typography.bodyLarge)
                        Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("1.248", "tamamlama", Modifier.weight(1f)); Stat("286", "aktif gün", Modifier.weight(1f)); Stat("47", "geri dönüş", Modifier.weight(1f), gold = true)
        }
    }
}

/** C: plain and useful: who you are, three numbers, and the things you change about yourself. */
@Composable
internal fun ProfileOptionC() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsTop()
        Spacer(Modifier.height(4.dp))
        Text("PROFİL SAYFASI", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Avatar(88)
            Text("Samet", style = MaterialTheme.typography.headlineSmall, fontFamily = serif, modifier = Modifier.padding(top = 8.dp))
            Text("41samet49@gmail.com · Google", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Stat("84", "gündür", Modifier.weight(1f)); Stat("🔥 12", "şu anki seri", Modifier.weight(1f)); Stat("✨ 47", "geri dönüş", Modifier.weight(1f), gold = true)
        }
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column {
                listOf("Adın" to "Samet", "Profil simgesi" to "🦊 · Momiji", "Selamlama" to "İyi akşamlar, Samet", "Uygulama kilidi" to "Parmak izi açık", "Hesap ve yedekleme" to "Google · son eşitleme 2 dk önce").forEach { (t, v) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t, style = MaterialTheme.typography.bodyLarge)
                            Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// ---------- Opening screen and lock ----------

@Composable
private fun Enso(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary) {
    Canvas(modifier) {
        drawArc(color, -80f, 330f * progress, false, style = Stroke(size.minDimension * 0.07f, cap = StrokeCap.Round))
    }
}

@Composable
private fun SplashFrame(content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) { content() }
}

/** Splash A: a brush ensō draws itself, the name settles in, then Today. */
@Composable
internal fun SplashOptionA() = SplashFrame {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Enso(0.85f, Modifier.size(170.dp))
            Text("金", fontSize = 54.sp, fontFamily = serif, color = Gold)
        }
        Text("Kinmichi", fontSize = 40.sp, fontFamily = serif, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 22.dp))
        Text("金道 · altın yol", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text("ensō 0,6 sn'de çizilir · toplam ~1 sn · dokununca geçer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.BottomCenter).padding(30.dp))
}

/** Splash B: a personal hello: the greeting, today's date and how the day stands. */
@Composable
internal fun SplashOptionB() = SplashFrame {
    Column(Modifier.padding(32.dp)) {
        Text("8 Kasım, Pazar", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("İyi akşamlar,", fontSize = 44.sp, fontFamily = serif, lineHeight = 48.sp)
        Text("Samet.", fontSize = 44.sp, fontFamily = serif, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, color = Gold, lineHeight = 48.sp)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) { Enso(0.2f, Modifier.size(54.dp)); Text("1/5", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
            Column {
                Text("Bugün 4 alışkanlık kaldı", style = MaterialTheme.typography.bodyLarge)
                Text("🔥 12 günlük seri seni bekliyor", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A6A1F))
            }
        }
    }
    Text("~1,2 sn · selam günün saatine göre değişir", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.BottomCenter).padding(30.dp))
}

/** Splash C: the golden path from the icon: steps light up to today's gold one. */
@Composable
internal fun SplashOptionC() = SplashFrame {
    val ink = MaterialTheme.colorScheme.primary
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(220.dp, 180.dp)) {
            val pts = listOf(0.1f to 0.85f, 0.25f to 0.72f, 0.42f to 0.66f, 0.55f to 0.52f, 0.66f to 0.36f, 0.8f to 0.25f, 0.9f to 0.12f)
            pts.forEachIndexed { i, (x, y) ->
                val c = Offset(size.width * x, size.height * y)
                val last = i == pts.lastIndex
                drawCircle(if (last) Gold else ink.copy(alpha = 0.35f + i * 0.08f), radius = if (last) 16f else 10f, center = c)
            }
        }
        Text("Kinmichi", fontSize = 36.sp, fontFamily = serif, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 18.dp))
        Text("Bugünün adımı hep parlar.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Text("adımlar sırayla yanar, sonuncusu altın · ~1 sn", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.align(Alignment.BottomCenter).padding(30.dp))
}

/** The new lock screen: same calm look, fingerprint asked at once, PIN as a fallback. */
@Composable
internal fun LockNew() = SplashFrame {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(28.dp)) {
        Avatar(76)
        Text("Tekrar hoş geldin, Samet", style = MaterialTheme.typography.titleLarge, fontFamily = serif, modifier = Modifier.padding(top = 14.dp))
        Spacer(Modifier.height(36.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Fingerprint, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text("Parmak izinle aç", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
        Text("Açılınca kendiliğinden sorulur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) { repeat(4) { Box(Modifier.size(14.dp).clip(CircleShape).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)) } }
        TextButton(onClick = {}, modifier = Modifier.padding(top = 8.dp)) { Text("PIN ile aç") }
    }
}
