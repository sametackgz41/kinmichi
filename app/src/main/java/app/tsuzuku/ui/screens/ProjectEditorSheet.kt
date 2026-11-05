package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.Project
import app.tsuzuku.model.ProjectDraft
import java.time.LocalDate

val PROJECT_EMOJIS = listOf("📁", "🚀", "🎯", "💡", "🏗️", "📚", "💻", "🌿", "⭐", "🗻")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProjectEditorSheet(
    existing: Project? = null,
    onDismiss: () -> Unit,
    onSave: (ProjectDraft) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var description by remember { mutableStateOf(existing?.description.orEmpty()) }
    var selectedEmoji by remember { mutableStateOf(existing?.emoji ?: "📁") }
    var targetDaysAhead by remember { mutableStateOf<Int?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column {
                Text("計画 · PROJE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.4.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (existing == null) "Yeni Proje Başlat" else "Projeyi Düzenle",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Proje Başlığı") },
                placeholder = { Text("Örn: Tsuzuku 2.0 veya Tez Çalışması") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama / Kapsam") },
                placeholder = { Text("Bu projenin temel amacı nedir?") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
            )

            Text("Simge Seç", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PROJECT_EMOJIS.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .clickable { selectedEmoji = emoji },
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape,
                    ) {
                        BoxContent(emoji)
                    }
                }
            }

            Text("Hedef Bitiş Zamanı", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(null to "Yok", 14 to "2 Hafta", 30 to "1 Ay", 90 to "3 Ay").forEach { (days, label) ->
                    FilterChip(
                        selected = targetDaysAhead == days,
                        onClick = { targetDaysAhead = days },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismiss) {
                    Text("İptal")
                }
                Spacer(Modifier.size(8.dp))
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val targetDate = targetDaysAhead?.let { LocalDate.now().plusDays(it.toLong()) }
                                ?: existing?.targetDate
                            onSave(
                                ProjectDraft(
                                    title = title.trim(),
                                    description = description.trim(),
                                    emoji = selectedEmoji,
                                    targetDate = targetDate,
                                ),
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                ) {
                    Text("Kaydet")
                }
            }
        }
    }
}

@Composable
private fun BoxContent(emoji: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Text(emoji, fontSize = 20.sp)
    }
}
