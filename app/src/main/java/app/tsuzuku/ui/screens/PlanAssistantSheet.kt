package app.tsuzuku.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.PlanDraftProposal
import app.tsuzuku.model.PlanMode
import app.tsuzuku.model.ProposedTaskDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanAssistantSheet(
    proposal: PlanDraftProposal?,
    isGenerating: Boolean,
    onGeneratePlan: (prompt: String, mode: PlanMode) -> Unit,
    onApplyPlan: (selectedTasks: List<ProposedTaskDraft>) -> Unit,
    onToggleTaskSelection: (index: Int) -> Unit,
    onUpdateTaskTitle: (index: Int, newTitle: String) -> Unit,
    onUpdateTaskDuration: (index: Int, deltaMinutes: Int) -> Unit,
    onAddSubtask: (taskIndex: Int, subtask: String) -> Unit,
    onRemoveSubtask: (taskIndex: Int, subtaskIndex: Int) -> Unit,
    onDismissProposal: () -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedMode by remember { mutableStateOf(PlanMode.Balanced) }
    var promptInput by remember { mutableStateOf("") }
    var expandedEditIndex by remember { mutableIntStateOf(-1) }
    var newSubtaskInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .imePadding(),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column {
                        Text("AI Plan Asistanı", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Modlar & Önizleme-Onay Mekanizması", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Kapat")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            if (proposal == null) {
                // Input mode
                Text(
                    "Plan Modunu Seç",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Plan Mode selection chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PlanMode.entries.forEach { mode ->
                        FilterChip(
                            selected = selectedMode == mode,
                            onClick = { selectedMode = mode },
                            label = { Text("${mode.iconEmoji} ${mode.title}") },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        selectedMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp),
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("İsteğe Bağlı Ek Talimat (Örn: 'Yarın öğleden sonra')") },
                    placeholder = { Text("Varsayılan akışı kullanmak için boş bırakabilirsin") },
                    maxLines = 2,
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onGeneratePlan(promptInput, selectedMode) },
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Taslak Hazırlanıyor...")
                    } else {
                        Text("Taslak Plan Oluştur (Önizle)")
                    }
                }
            } else {
                // Proposal Preview Mode (User Approval Required)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(proposal.mode.iconEmoji, fontSize = 16.sp)
                            Text(proposal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            proposal.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Safety banner
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("🛡️", fontSize = 14.sp)
                        Text(
                            "Görevler henüz plana yazılmadı. Görevlere dokunarak başlık, süre ve alt adımları düzenleyebilirsin.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("Önerilen Görevler (${proposal.proposedTasks.size})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(proposal.proposedTasks, key = { _, task -> task.id }) { index, task ->
                        val isChecked = task.isSelected
                        val isExpanded = expandedEditIndex == index

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                            ),
                            border = if (isChecked) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = { onToggleTaskSelection(index) },
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { expandedEditIndex = if (isExpanded) -1 else index },
                                    ) {
                                        Text(
                                            task.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                "⏱️ ${task.estimatedMinutes} dk",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            if (task.subtasks.isNotEmpty()) {
                                                Text(
                                                    "• ${task.subtasks.size} alt adım",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { expandedEditIndex = if (isExpanded) -1 else index }) {
                                        Icon(
                                            Icons.Rounded.Edit,
                                            contentDescription = "Düzenle",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }

                                // Expandable Inline Editor
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        HorizontalDivider()

                                        OutlinedTextField(
                                            value = task.title,
                                            onValueChange = { onUpdateTaskTitle(index, it) },
                                            label = { Text("Görev Başlığı") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                        )

                                        // Duration adjuster
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text("Süre: ${task.estimatedMinutes} dk", style = MaterialTheme.typography.bodySmall)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                OutlinedButton(onClick = { onUpdateTaskDuration(index, -15) }) { Text("-15") }
                                                OutlinedButton(onClick = { onUpdateTaskDuration(index, 15) }) { Text("+15") }
                                            }
                                        }

                                        // Subtasks list
                                        if (task.subtasks.isNotEmpty()) {
                                            Text("Alt Adımlar:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                            task.subtasks.forEachIndexed { subIndex, sub ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                ) {
                                                    Text("• $sub", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                                    IconButton(onClick = { onRemoveSubtask(index, subIndex) }) {
                                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Sil", modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                        }

                                        // Add subtask input
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            OutlinedTextField(
                                                value = newSubtaskInput,
                                                onValueChange = { newSubtaskInput = it },
                                                placeholder = { Text("Yeni alt adım ekle...") },
                                                modifier = Modifier.weight(1f),
                                                singleLine = true,
                                            )
                                            IconButton(
                                                onClick = {
                                                    if (newSubtaskInput.isNotBlank()) {
                                                        onAddSubtask(index, newSubtaskInput)
                                                        newSubtaskInput = ""
                                                    }
                                                },
                                            ) {
                                                Icon(Icons.Rounded.Add, contentDescription = "Ekle")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val selectedTasks = proposal.proposedTasks.filter { it.isSelected }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    OutlinedButton(
                        onClick = onDismissProposal,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Vazgeç")
                    }
                    Button(
                        onClick = {
                            onApplyPlan(selectedTasks)
                            onClose()
                        },
                        enabled = selectedTasks.isNotEmpty(),
                        modifier = Modifier.weight(1.5f),
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Seçilenleri Plana Ekle (${selectedTasks.size})")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
