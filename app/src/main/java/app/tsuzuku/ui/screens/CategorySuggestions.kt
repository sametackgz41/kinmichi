package app.tsuzuku.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import app.tsuzuku.data.local.CategoryEntity

@Composable
internal fun CategorySuggestions(categories: List<CategoryEntity>, tags: String, onTagsChange: (String) -> Unit) {
    if (categories.isEmpty()) return
    val selected = tags.split(',', ';', '#').map(String::trim).filter(String::isNotBlank)
    Row(androidx.compose.ui.Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            val active = selected.any { it.equals(category.name, ignoreCase = true) }
            FilterChip(
                selected = active,
                onClick = {
                    val next = if (active) selected.filterNot { it.equals(category.name, true) } else selected + category.name
                    onTagsChange(next.distinctBy(String::lowercase).joinToString(", "))
                },
                label = { Text("${category.emoji} ${category.name}") },
            )
        }
    }
}
