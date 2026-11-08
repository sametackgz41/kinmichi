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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

private data class Binned(val emoji: String, val name: String, val color: Long, val daysLeft: Int, val note: String)

private val binned = listOf(
    Binned("📖", "Kitap oku", 0xFF5068A9, 28, "Seri 42 gün · 2 gün önce silindi"),
    Binned("🚿", "Soğuk duş", 0xFF3F7D5A, 19, "86 kayıt · 11 gün önce silindi"),
    Binned("🎸", "Gitar çalış", 0xFFB5654A, 4, "23 kayıt · 26 gün önce silindi"),
)

@Composable
private fun Header(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null) }
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun EmojiTile(b: Binned) {
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(b.color).copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) { Text(b.emoji, fontSize = 22.sp) }
}

/** A: the archive page gets a second tab. */
@Composable
internal fun TrashOptionA() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header("Arşiv ve çöp kutusu")
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            SegmentedButton(selected = false, onClick = {}, shape = SegmentedButtonDefaults.itemShape(0, 2), label = { Text("Arşiv · 2") })
            SegmentedButton(selected = true, onClick = {}, shape = SegmentedButtonDefaults.itemShape(1, 2), label = { Text("Çöp kutusu · 3") })
        }
        Text(
            "Silinenler geçmişleriyle 30 gün burada kalır, sonra tamamen silinir.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        )
        binned.forEach { b ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                EmojiTile(b)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(b.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${b.daysLeft} gün sonra silinir",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (b.daysLeft <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = {}) { Icon(Icons.Rounded.DeleteForever, null, tint = MaterialTheme.colorScheme.error) }
                FilledTonalButton(onClick = {}) {
                    Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp))
                    Text("Geri getir", Modifier.padding(start = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick = {}, modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        ) { Text("Çöp kutusunu boşalt", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun DaysRing(days: Int) {
    val primary = MaterialTheme.colorScheme.primary
    val error = MaterialTheme.colorScheme.error
    val track = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = Stroke(4.dp.toPx(), cap = StrokeCap.Round)
            drawArc(track, 0f, 360f, false, style = s)
            drawArc(if (days <= 7) error else primary, -90f, 360f * days / 30f, false, style = s)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
            Text("gün", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, lineHeight = 10.sp)
        }
    }
}

/** B: its own page in Settings, with a countdown ring on each habit. */
@Composable
internal fun TrashOptionB() {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Header("Çöp kutusu")
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(20.dp),
        ) {
            Text("🗑️", fontSize = 34.sp)
            Text("3 alışkanlık bekliyor", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Her biri silindiği günden 30 gün sonra geçmişiyle birlikte gider. En yakını 4 gün sonra.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            TextButton(onClick = {}, modifier = Modifier.offset(x = (-12).dp).padding(top = 4.dp)) {
                Text("Şimdi boşalt", color = MaterialTheme.colorScheme.error)
            }
        }
        binned.forEach { b ->
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiTile(b)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(b.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DaysRing(b.daysLeft)
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {}, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Restore, null, Modifier.size(18.dp)); Text("Geri getir", Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(onClick = {}, modifier = Modifier.weight(1f)) { Text("Kalıcı sil", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

/** C: grouped by when, swipe to bring back, and the undo bar right after deleting. */
@Composable
internal fun TrashOptionC() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Header("Çöp kutusu")
            Text(
                "Sağa kaydır: geri getir · Sola kaydır: kalıcı sil",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Text("BU HAFTA", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
            listOf(binned[0]).forEach { b ->
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0xFF3F7D5A))) {
                    Row(Modifier.padding(start = 20.dp).height(72.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Restore, null, tint = Color.White)
                        Text("Geri getir", color = Color.White, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    Row(
                        Modifier.offset(x = 130.dp).fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        EmojiTile(b)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(b.name, style = MaterialTheme.typography.titleMedium)
                            Text("${b.daysLeft} gün kaldı", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Text("DAHA ÖNCE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
            binned.drop(1).forEach { b ->
                Row(
                    Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow).padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    EmojiTile(b)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(b.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${b.daysLeft} gün kaldı",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (b.daysLeft <= 7) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        Snackbar(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            action = { TextButton(onClick = {}) { Text("GERİ AL", color = MaterialTheme.colorScheme.inversePrimary) } },
        ) { Text("“Kitap oku” çöp kutusuna taşındı") }
    }
}
