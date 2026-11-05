package app.tsuzuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.data.local.CategoryEntity

private val categoryPalette = listOf(0xFF5068A9, 0xFF3F7D5A, 0xFFB84A3A, 0xFF8A6A3E, 0xFF6B5B95, 0xFF39747C)

@Composable
fun CategoryManagerScreen(categories: List<CategoryEntity>, onBack: () -> Unit, onCreate: (String, String, Long) -> Unit, onDelete: (CategoryEntity) -> Unit) {
    var name by remember { mutableStateOf("") }
    var emoji by remember { mutableStateOf("◆") }
    var color by remember { mutableLongStateOf(categoryPalette.first()) }
    var pendingDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { ScreenHeader(stringResource(R.string.categories_and_tags), onBack) }
        item { Text(stringResource(R.string.categories_description), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it.take(32) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.category_name)) }, singleLine = true)
                OutlinedTextField(emoji, { emoji = it.take(4) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.emoji_label)) }, singleLine = true)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    categoryPalette.forEach { swatch ->
                        Box(Modifier.size(if (color == swatch) 42.dp else 34.dp).background(Color(swatch), CircleShape).clickable { color = swatch })
                    }
                }
                Button(enabled = name.isNotBlank(), onClick = { onCreate(name, emoji, color); name = "" }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.create_category)) }
            }
        }
        item { Text(stringResource(R.string.your_categories), style = MaterialTheme.typography.titleLarge) }
        if (categories.isEmpty()) item { Text(stringResource(R.string.no_custom_categories), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(categories.size, key = { categories[it].id }) { index ->
            val item = categories[index]
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).background(Color(item.colorArgb), CircleShape), contentAlignment = Alignment.Center) { Text(item.emoji) }
                Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { pendingDelete = item }) { Icon(Icons.Rounded.DeleteOutline, stringResource(R.string.delete_label)) }
            }
        }
    }
    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_category)) },
            text = { Text(stringResource(R.string.delete_category_confirmation, item.name)) },
            confirmButton = { TextButton(onClick = { onDelete(item); pendingDelete = null }) { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
