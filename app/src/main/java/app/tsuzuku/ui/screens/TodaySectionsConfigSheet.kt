package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.tsuzuku.R

data class SectionItem(val id: String, val title: String, val icon: ImageVector)

val ALL_SECTIONS = listOf(
    SectionItem("summary", "Günlük Özet", Icons.Rounded.PieChart),
    SectionItem("habits", "Alışkanlıklar", Icons.Rounded.Repeat),
    SectionItem("plan", "Bugünkü Plan", Icons.Rounded.Checklist),
    SectionItem("goals", "Hedef İlerlemeleri", Icons.Rounded.Flag),
    SectionItem("library", "Devam Eden İçerikler", Icons.Rounded.MenuBook),
    SectionItem("focus", "Odaklanma Bilgisi", Icons.Rounded.Timer),
    SectionItem("health", "Sağlık Bilgileri", Icons.Rounded.HealthAndSafety),
    SectionItem("routines", "Rutinler", Icons.Rounded.Repeat),
    SectionItem("intelligence", "Tsuzuku Intelligence", Icons.Rounded.Lightbulb),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodaySectionsConfigSheet(
    currentOrder: String,
    currentHidden: String,
    onSave: (newOrder: String, newHidden: String) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val initialList = remember(currentOrder) {
        val orderKeys = currentOrder.split(',').filter(String::isNotBlank)
        val ordered = orderKeys.mapNotNull { key -> ALL_SECTIONS.firstOrNull { it.id == key } }
        val remaining = ALL_SECTIONS.filter { s -> s.id !in orderKeys }
        ordered + remaining
    }

    var sections by remember { mutableStateOf(initialList) }
    var hiddenSet by remember { mutableStateOf(currentHidden.split(',').filter(String::isNotBlank).toSet()) }

    fun notifyChange() {
        val newOrder = sections.joinToString(",") { it.id }
        val newHidden = hiddenSet.joinToString(",")
        onSave(newOrder, newHidden)
    }

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.customize_sections),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Bölümleri sıralayın ve görünürlüklerini ayarlayın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = null)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(sections, key = { _, s -> s.id }) { index, section ->
                    val isVisible = section.id !in hiddenSet

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isVisible) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    section.icon,
                                    contentDescription = null,
                                    tint = if (isVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isVisible) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isVisible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            val mutable = sections.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index - 1, item)
                                            sections = mutable
                                            notifyChange()
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        if (index < sections.size - 1) {
                                            val mutable = sections.toMutableList()
                                            val item = mutable.removeAt(index)
                                            mutable.add(index + 1, item)
                                            sections = mutable
                                            notifyChange()
                                        }
                                    },
                                    enabled = index < sections.size - 1,
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }

                                Spacer(modifier = Modifier.size(6.dp))

                                Switch(
                                    checked = isVisible,
                                    onCheckedChange = { checked ->
                                        hiddenSet = if (checked) hiddenSet - section.id else hiddenSet + section.id
                                        notifyChange()
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
