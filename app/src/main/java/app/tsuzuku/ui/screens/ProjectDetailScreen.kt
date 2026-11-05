package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.model.Goal
import app.tsuzuku.model.PlanTask
import app.tsuzuku.model.ProjectDetail
import app.tsuzuku.model.ProjectStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    detail: ProjectDetail?,
    availableTasks: List<PlanTask>,
    availableGoals: List<Goal>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: (ProjectStatus) -> Unit,
    onAddMilestone: (String) -> Unit,
    onToggleMilestone: (Long, Boolean) -> Unit,
    onDeleteMilestone: (Long) -> Unit,
    onLinkTask: (Long) -> Unit,
    onUnlinkTask: (Long) -> Unit,
    onLinkGoal: (Long) -> Unit,
    onUnlinkGoal: (Long) -> Unit,
    onOpenTask: (Long) -> Unit,
    onOpenGoal: (Long) -> Unit,
) {
    if (detail == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Proje yükleniyor...", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val project = detail.project
    var showMenu by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var showLinkTaskDialog by remember { mutableStateOf(false) }
    var showLinkGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(project.emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(project.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Seçenekler")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Düzenle") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                            onClick = { showMenu = false; onEdit() },
                        )
                        val nextStatus = if (project.status == ProjectStatus.Completed) ProjectStatus.Active else ProjectStatus.Completed
                        DropdownMenuItem(
                            text = { Text(if (project.status == ProjectStatus.Completed) "Aktif Yap" else "Tamamlandı Olarak İşaretle") },
                            leadingIcon = { Icon(Icons.Rounded.CheckCircle, contentDescription = null) },
                            onClick = { showMenu = false; onToggleStatus(nextStatus) },
                        )
                        DropdownMenuItem(
                            text = { Text("Projeyi Sil", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { showMenu = false; onDelete() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Overview card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (project.status == ProjectStatus.Completed) "Tamamlandı" else "Devam Ediyor",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (project.status == ProjectStatus.Completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            )
                            val pct = (detail.progressFraction * 100).toInt()
                            Text("%$pct Tamamlandı", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        LinearProgressIndicator(
                            progress = { detail.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )

                        if (project.description.isNotBlank()) {
                            Text(project.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        project.targetDate?.let { date ->
                            Text("Hedef Tarih: $date", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            // Milestones Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Kilometre Taşları (${detail.milestones.count { it.completed }}/${detail.milestones.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showAddMilestoneDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Aşama Ekle")
                    }
                }
            }

            if (detail.milestones.isEmpty()) {
                item {
                    Text("Henüz bir kilometre taşı eklenmedi. Projeni ana aşamalara bölerek odaklanabilirsin.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(detail.milestones, key = { it.id }) { milestone ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = milestone.completed,
                                onCheckedChange = { onToggleMilestone(milestone.id, it) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                milestone.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (milestone.completed) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (milestone.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            IconButton(onClick = { onDeleteMilestone(milestone.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Sil", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Linked Tasks Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bağlı Görevler (${detail.linkedTasks.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showLinkTaskDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Görev Bağla")
                    }
                }
            }

            if (detail.linkedTasks.isEmpty()) {
                item {
                    Text("Bu projeye bağlı bir görev yok.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(detail.linkedTasks, key = { it.id }) { task ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenTask(task.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (task.completed) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (task.completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    task.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                                )
                                if (task.estimatedMinutes != null) {
                                    Text("${task.estimatedMinutes} dk", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                }
                            }
                            IconButton(onClick = { onUnlinkTask(task.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Bağlantıyı Kaldır", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Linked Goals Section
            item {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Bağlı Hedefler (${detail.linkedGoals.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showLinkGoalDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hedef Bağla")
                    }
                }
            }

            if (detail.linkedGoals.isEmpty()) {
                item {
                    Text("Bu projeye bağlı bir hedef bulunmuyor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(detail.linkedGoals, key = { it.id }) { goal ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenGoal(goal.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("🎯", fontSize = 18.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(goal.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${goal.currentValue} / ${goal.targetValue} ${goal.unit}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { onUnlinkGoal(goal.id) }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Bağlantıyı Kaldır", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Milestone Dialog
    if (showAddMilestoneDialog) {
        var milestoneTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMilestoneDialog = false },
            title = { Text("Yeni Aşama Ekle") },
            text = {
                OutlinedTextField(
                    value = milestoneTitle,
                    onValueChange = { milestoneTitle = it },
                    label = { Text("Aşama Başlığı") },
                    placeholder = { Text("Örn: Veritabanı tasarımını bitir") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (milestoneTitle.isNotBlank()) {
                            onAddMilestone(milestoneTitle.trim())
                            showAddMilestoneDialog = false
                        }
                    },
                    enabled = milestoneTitle.isNotBlank(),
                ) {
                    Text("Ekle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMilestoneDialog = false }) {
                    Text("İptal")
                }
            },
        )
    }

    // Link Task Dialog
    if (showLinkTaskDialog) {
        val linkedIds = detail.linkedTasks.map { it.id }.toSet()
        val unlinkedTasks = availableTasks.filter { it.id !in linkedIds }
        AlertDialog(
            onDismissRequest = { showLinkTaskDialog = false },
            title = { Text("Görev Bağla") },
            text = {
                if (unlinkedTasks.isEmpty()) {
                    Text("Bağlanabilecek başka görev bulunamadı.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(unlinkedTasks) { task ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLinkTask(task.id)
                                        showLinkTaskDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(task.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkTaskDialog = false }) { Text("Kapat") }
            },
        )
    }

    // Link Goal Dialog
    if (showLinkGoalDialog) {
        val linkedIds = detail.linkedGoals.map { it.id }.toSet()
        val unlinkedGoals = availableGoals.filter { it.id !in linkedIds }
        AlertDialog(
            onDismissRequest = { showLinkGoalDialog = false },
            title = { Text("Hedef Bağla") },
            text = {
                if (unlinkedGoals.isEmpty()) {
                    Text("Bağlanabilecek başka hedef bulunamadı.")
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(unlinkedGoals) { goal ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onLinkGoal(goal.id)
                                        showLinkGoalDialog = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(goal.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLinkGoalDialog = false }) { Text("Kapat") }
            },
        )
    }
}
