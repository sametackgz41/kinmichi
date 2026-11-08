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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun Page(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        content()
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) { content() }
}

/** History A: a timeline card on the habit page. */
@Composable
internal fun HistoryOptionA() = Page("📖 Kitap oku") {
    Card {
        Text("Hedef geçmişi", style = MaterialTheme.typography.titleMedium)
        listOf(
            Triple("8 Kas'tan beri", "günde 30 sayfa", true),
            Triple("1 Eki – 7 Kas", "günde 20 sayfa", false),
            Triple("14 Ağu – 30 Eyl", "günde 10 sayfa", false),
        ).forEach { (range, goal, now) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(if (now) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                Column(Modifier.padding(start = 14.dp)) {
                    Text(goal, style = MaterialTheme.typography.bodyLarge)
                    Text(range, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Text(
            "Geçmiş günler kendi zamanındaki hedefle sayılır; hedefi yükseltmek eski serini bozmaz.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** History B: ask when the target changes; a single line on the habit page. */
@Composable
internal fun HistoryOptionB() = Page("Hedefi düzenle") {
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Yeni hedef: günde 30 sayfa", style = MaterialTheme.typography.titleLarge)
            Text("Geçmiş günler nasıl sayılsın?", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = true, onClick = {})
                Column {
                    Text("Bugünden itibaren", style = MaterialTheme.typography.bodyLarge)
                    Text("Eski günler 20 sayfayla kalır", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = false, onClick = {})
                Column {
                    Text("Tüm geçmişe uygula", style = MaterialTheme.typography.bodyLarge)
                    Text("Seriler ve oranlar yeniden hesaplanır", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {}) { Text("Vazgeç") }
                TextButton(onClick = {}) { Text("Kaydet") }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Text("ALIŞKANLIK SAYFASINDA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Text("Hedef 8 Kasım'da 20 → 30 sayfa oldu", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun BackupRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null)
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 14.dp))
    }
}

/** Backup A: its own card with the password fields right there. */
@Composable
internal fun BackupOptionA() = Page("Yedekleme") {
    BackupRow(Icons.Rounded.Upload, "Yedek oluştur (JSON)")
    BackupRow(Icons.Rounded.Download, "Yedeği geri yükle")
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = MaterialTheme.colorScheme.primary)
            Text("Şifreli tam yedek", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
        }
        Text("Parolasız kimse açamaz. Parolayı unutursan yedek de açılmaz.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField("parola123", {}, label = { Text("Parola (en az 8)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField("parola123", {}, label = { Text("Parola tekrar") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp)), color = Color(0xFF3F7D5A))
            Text("  orta", style = MaterialTheme.typography.labelMedium, color = Color(0xFF3F7D5A))
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Şifreli yedek oluştur") }
    }
}

/** Backup B: a "protect with a password" switch on the usual backup button. */
@Composable
internal fun BackupOptionB() = Page("Yedekleme") {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Upload, null)
            Text("Yedek oluştur", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Parolayla koru", style = MaterialTheme.typography.bodyLarge)
                Text("Dosya şifrelenir; açarken parola sorulur", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = true, onCheckedChange = {})
        }
        Button(onClick = {}, modifier = Modifier.fillMaxWidth()) { Text("Oluştur") }
    }
    BackupRow(Icons.Rounded.Download, "Yedeği geri yükle")
    Spacer(Modifier.height(8.dp))
    Text("OLUŞTUR'A BASINCA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 6.dp) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Yedek parolası", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField("parola123", {}, label = { Text("Parola (en az 8)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            OutlinedTextField("", {}, label = { Text("Parola tekrar") }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {}) { Text("Vazgeç") }
                TextButton(onClick = {}) { Text("Şifrele") }
            }
        }
    }
}
