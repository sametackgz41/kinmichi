package app.tsuzuku.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.Habit
import app.tsuzuku.model.LibraryItem
import app.tsuzuku.model.PlanTask
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

private const val SCOPE_ALL = "all"
private const val SCOPE_HABIT = "habit"
private const val SCOPE_TASK = "task"
private const val SCOPE_CONTENT = "content"
private data class SearchHit(val type: String, val id: Long, val title: String, val subtitle: String, val tags: String)
private data class SavedSearch(val id: Long, val name: String, val query: String, val scope: String)

@Composable
fun UnifiedSearchScreen(
    habits: List<Habit>, tasks: List<PlanTask>, contents: List<LibraryItem>,
    onBack: () -> Unit, onOpenHabit: (Long) -> Unit, onOpenTask: (Long) -> Unit, onOpenContent: (Long) -> Unit,
) {
    val context = LocalContext.current
    val store = remember { context.getSharedPreferences("saved-searches", android.content.Context.MODE_PRIVATE) }
    var query by rememberSaveable { mutableStateOf("") }
    var scope by rememberSaveable { mutableStateOf(SCOPE_ALL) }
    var saved by remember { mutableStateOf(readSavedSearches(store.getString("filters", null), store.getStringSet("queries", emptySet()).orEmpty())) }
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    var filterName by rememberSaveable { mutableStateOf("") }
    val all = remember(habits, tasks, contents) { createSearchHits(habits, tasks, contents) }
    val normalized = normalizeSearchText(query)
    val results = all.filter { hit ->
        (scope == SCOPE_ALL || scope == hit.type) && (normalized.isBlank() || searchMatches(hit, normalized))
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { ScreenHeader(stringResource(R.string.search_everything), onBack) }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it.take(160) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_hint_all)) }, singleLine = true,
            )
        }
        item { SearchScopeRow(scope) { scope = it } }
        if (query.isNotBlank()) item {
            AssistChip(onClick = {
                filterName = query.trim()
                showSaveDialog = true
            }, label = { Text(stringResource(R.string.save_search)) })
        }
        if (saved.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                saved.forEach { value ->
                    InputChip(
                        selected = false,
                        onClick = { query = value.query; scope = value.scope },
                        label = { Text(value.name) },
                        trailingIcon = {
                            IconButton(onClick = {
                                saved = saved.filterNot { it.id == value.id }
                                writeSavedSearches(store, saved)
                            }) { Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.delete_saved_search)) }
                        },
                    )
                }
            }
        }
        item { Text(stringResource(R.string.result_count, results.size), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(results.size, key = { "${results[it].type}-${results[it].id}" }) { index ->
            val hit = results[index]
            SearchResultRow(hit) {
                if (hit.type == SCOPE_HABIT) onOpenHabit(hit.id)
                else if (hit.type == SCOPE_TASK) onOpenTask(hit.id)
                else onOpenContent(hit.id)
            }
        }
    }

    if (showSaveDialog) AlertDialog(
        onDismissRequest = { showSaveDialog = false },
        title = { Text(stringResource(R.string.name_saved_search)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(filterName, { filterName = it.take(50) }, label = { Text(stringResource(R.string.filter_name)) }, singleLine = true)
                Text(stringResource(R.string.saved_filter_summary, query.trim(), scopeLabel(scope)), style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(enabled = filterName.isNotBlank(), onClick = {
                val item = SavedSearch(System.currentTimeMillis(), filterName.trim(), query.trim(), scope)
                saved = (saved + item).takeLast(12)
                writeSavedSearches(store, saved)
                showSaveDialog = false
            }) { Text(stringResource(R.string.save_label)) }
        },
        dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun SearchScopeRow(selected: String, onSelect: (String) -> Unit) {
    val scopes = listOf(
        SCOPE_ALL to R.string.all_label,
        SCOPE_HABIT to R.string.habits,
        SCOPE_TASK to R.string.plan,
        SCOPE_CONTENT to R.string.library,
    )
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        scopes.forEach { item -> FilterChip(selected = selected == item.first, onClick = { onSelect(item.first) }, label = { Text(stringResource(item.second)) }) }
    }
}

@Composable
private fun SearchResultRow(hit: SearchHit, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp)) {
        Text(hit.title, style = MaterialTheme.typography.titleMedium)
        if (hit.subtitle.isNotBlank()) Text(hit.subtitle, maxLines = 1, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hit.tags.isNotBlank()) Text(hit.tags.split(',').joinToString("  ") { "#$it" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

private fun createSearchHits(habits: List<Habit>, tasks: List<PlanTask>, contents: List<LibraryItem>): List<SearchHit> = buildList {
    habits.forEach { add(SearchHit(SCOPE_HABIT, it.id, it.name, it.generalNote, it.tags)) }
    tasks.forEach { add(SearchHit(SCOPE_TASK, it.id, it.title, it.note, it.tags)) }
    contents.forEach { item -> add(SearchHit(SCOPE_CONTENT, item.content.id, item.content.title, item.content.creator + " " + item.content.generalNote, item.content.tags)) }
}

private fun normalizeSearchText(value: String): String = value.trim().lowercase(Locale.forLanguageTag("tr-TR"))
private fun searchMatches(hit: SearchHit, query: String): Boolean =
    normalizeSearchText(hit.title).contains(query) || normalizeSearchText(hit.subtitle).contains(query) || normalizeSearchText(hit.tags).contains(query)

@Composable
private fun scopeLabel(scope: String): String = when (scope) {
    SCOPE_HABIT -> stringResource(R.string.habits)
    SCOPE_TASK -> stringResource(R.string.plan)
    SCOPE_CONTENT -> stringResource(R.string.library)
    else -> stringResource(R.string.all_label)
}

private fun readSavedSearches(json: String?, legacy: Set<String>): List<SavedSearch> = runCatching {
    val array = JSONArray(json ?: "[]")
    (0 until array.length()).map { index ->
        val item = array.getJSONObject(index)
        SavedSearch(item.getLong("id"), item.getString("name"), item.getString("query"), item.optString("scope", SCOPE_ALL))
    }
}.getOrDefault(emptyList()).ifEmpty {
    legacy.sorted().mapIndexed { index, query -> SavedSearch(index.toLong(), query, query, SCOPE_ALL) }
}

private fun writeSavedSearches(store: android.content.SharedPreferences, values: List<SavedSearch>) {
    val array = JSONArray()
    values.forEach { value -> array.put(JSONObject().put("id", value.id).put("name", value.name).put("query", value.query).put("scope", value.scope)) }
    store.edit().putString("filters", array.toString()).remove("queries").apply()
}
