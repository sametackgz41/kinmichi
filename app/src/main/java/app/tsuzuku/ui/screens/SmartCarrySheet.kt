package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.PlanTask
import java.time.LocalDate

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartCarrySheet(
    task: PlanTask,
    onDismiss: () -> Unit,
    onDoToday: () -> Unit,
    onMoveTomorrow: () -> Unit,
    onMoveNextWeek: () -> Unit,
    onDivideIntoSubtasks: (List<String>) -> Unit,
    onDeleteOrCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isSplittingMode by remember { mutableStateOf(false) }
    var step1 by remember { mutableStateOf("") }
    var step2 by remember { mutableStateOf("") }
    var step3 by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column {
                Text(
                    "延期 · ERTELEME FARKINDALIĞI",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Bu görevi ${task.carryCount} kez erteledin",
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "\"${task.title}\"",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (isSplittingMode) {
                        "Görevi ilk 5 dakikada başlayabileceğin küçük checklist adımlarına ayırarak zihinsel direnci kırabilirsin:"
                    } else {
                        "Sürekli ertelenen işler genellikle ya gereğinden büyüktür ya da şu an anlamını yitirmiştir. Kendine yüklenmeden bir adım seç:"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (isSplittingMode) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = step1,
                        onValueChange = { step1 = it },
                        label = { Text("1. İlk Küçük Adım (5 dk)") },
                        placeholder = { Text("Örn. Taslak dosyayı aç / 1 sayfa oku") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = step2,
                        onValueChange = { step2 = it },
                        label = { Text("2. İkinci Adım (Opsiyonel)") },
                        placeholder = { Text("Örn. Başlıkları belirle") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )
                    OutlinedTextField(
                        value = step3,
                        onValueChange = { step3 = it },
                        label = { Text("3. Üçüncü Adım (Opsiyonel)") },
                        placeholder = { Text("Örn. İlk taslağı tamamla") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = { isSplittingMode = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Geri")
                        }
                        Button(
                            onClick = {
                                val steps = listOf(step1, step2, step3).map(String::trim).filter(String::isNotBlank)
                                if (steps.isNotEmpty()) {
                                    onDivideIntoSubtasks(steps)
                                    onDismiss()
                                }
                            },
                            enabled = step1.isNotBlank() || step2.isNotBlank() || step3.isNotBlank(),
                            modifier = Modifier.weight(1.5f),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text("Adımları Ekle & Başla")
                        }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CarryOptionItem(
                        icon = Icons.Rounded.PlayArrow,
                        title = "Bugün Yap",
                        subtitle = "Bugünün önceliği yap ve ilk 5 dakikasını başlat.",
                        onClick = { onDoToday(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Yarına Taşı",
                        subtitle = "Bugünün gündeminden çıkar, yarın taze bir başlangıç yap.",
                        onClick = { onMoveTomorrow(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Gelecek Haftaya Ertele",
                        subtitle = "Şu an zamanı değilse 7 gün sonraya ötele.",
                        onClick = { onMoveNextWeek(); onDismiss() },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.FormatListBulleted,
                        title = "Daha Küçük Parçalara Böl",
                        subtitle = "Alt görevler veya checklist adımları ekle.",
                        onClick = { isSplittingMode = true },
                    )
                    CarryOptionItem(
                        icon = Icons.Rounded.Close,
                        title = "Görevi İptal Et / Sil",
                        subtitle = "Artık gerekli değilse vicdan azabı çekmeden serbest bırak.",
                        onClick = { onDeleteOrCancel(); onDismiss() },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CarryOptionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                modifier = Modifier.size(38.dp),
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
