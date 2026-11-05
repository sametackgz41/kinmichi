package app.tsuzuku.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.ContentCategory
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.LibraryCollection
import app.tsuzuku.model.LibraryItem
import coil3.compose.SubcomposeAsyncImage

@Composable
fun LibraryScreen(
    items: List<LibraryItem>,
    collections: List<LibraryCollection> = emptyList(),
    selectedCollectionId: Long? = null,
    selectedCollectionContentIds: List<Long> = emptyList(),
    onSelectCollection: (Long?) -> Unit = {},
    onCreateCollection: (name: String, emoji: String) -> Unit = { _, _ -> },
    onDeleteCollection: (Long) -> Unit = {},
    onAddContent: () -> Unit,
    onOpenContent: (Long) -> Unit,
    onReorderContent: () -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    var statusName by rememberSaveable { mutableStateOf<String?>(null) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var gridMode by rememberSaveable { mutableStateOf(true) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var collectionToDelete by remember { mutableStateOf<LibraryCollection?>(null) }

    val selectedCategory = categoryName?.let(ContentCategory::valueOf)
    val selectedStatus = statusName?.let(ContentStatus::valueOf)

    val filteredItems = items.filter { item ->
        val matchesQuery = query.isBlank() || item.content.title.contains(query, ignoreCase = true) ||
            item.content.creator.contains(query, ignoreCase = true)
        val matchesCategory = selectedCategory == null || item.content.category == selectedCategory
        val matchesStatus = selectedStatus == null || item.content.status == selectedStatus
        val matchesFavorites = !favoritesOnly || item.content.isFavorite
        val matchesCollection = selectedCollectionId == null || item.content.id in selectedCollectionContentIds

        matchesQuery && matchesCategory && matchesStatus && matchesFavorites && matchesCollection
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("本棚 · LIBRARY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, letterSpacing = 1.6.sp)
                    Text(stringResource(R.string.library), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        stringResource(R.string.library_item_count, items.size),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { gridMode = !gridMode }) {
                        Icon(
                            if (gridMode) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                            contentDescription = null,
                        )
                    }
                    IconButton(onClick = onReorderContent) {
                        Icon(Icons.Rounded.SwapVert, contentDescription = stringResource(R.string.reorder_library))
                    }
                }
            }
        }

        // Collections horizontal row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedCollectionId == null && !favoritesOnly,
                    onClick = {
                        favoritesOnly = false
                        onSelectCollection(null)
                    },
                    label = { Text(stringResource(R.string.all_categories)) },
                )
                FilterChip(
                    selected = favoritesOnly,
                    onClick = {
                        favoritesOnly = !favoritesOnly
                        if (favoritesOnly) onSelectCollection(null)
                    },
                    label = { Text("❤️ Favorilerim") },
                )
                collections.forEach { col ->
                    FilterChip(
                        selected = selectedCollectionId == col.id && !favoritesOnly,
                        onClick = {
                            favoritesOnly = false
                            if (selectedCollectionId == col.id) onSelectCollection(null) else onSelectCollection(col.id)
                        },
                        label = { Text("${col.emoji} ${col.name} (${col.itemCount})") },
                    )
                }
                TextButton(onClick = { showCreateCollectionDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.new_collection), style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // Active collection indicator with delete option
        if (selectedCollectionId != null) {
            val activeCol = collections.firstOrNull { it.id == selectedCollectionId }
            if (activeCol != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.collection_filter_label, activeCol.emoji, activeCol.name),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            IconButton(
                                onClick = { collectionToDelete = activeCol },
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.DeleteOutline,
                                    contentDescription = stringResource(R.string.delete_collection),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_library)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { categoryName = null },
                    label = { Text(stringResource(R.string.all_categories)) },
                )
                ContentCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { categoryName = category.name },
                        label = { Text("${categoryEmoji(category)} ${stringResource(categoryLabelRes(category))}") },
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { statusName = null },
                    label = { Text(stringResource(R.string.all_statuses)) },
                )
                ContentStatus.entries.forEach { status ->
                    FilterChip(
                        selected = selectedStatus == status,
                        onClick = { statusName = status.name },
                        label = { Text(stringResource(statusLabelRes(status))) },
                    )
                }
            }
        }

        if (filteredItems.isEmpty()) {
            item {
                EmptyLibraryCard(hasAnyItems = items.isNotEmpty(), onAddContent = onAddContent)
            }
        } else {
            if (gridMode) {
                items(filteredItems.chunked(2), key = { row -> row.joinToString("-") { it.content.id.toString() } }) { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { item ->
                            LibraryGridCard(item, { onOpenContent(item.content.id) }, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(filteredItems, key = { it.content.id }) { item ->
                    LibraryContentCard(item = item, onClick = { onOpenContent(item.content.id) })
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }

    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onConfirm = { name, emoji ->
                onCreateCollection(name, emoji)
                showCreateCollectionDialog = false
            },
        )
    }

    if (collectionToDelete != null) {
        val col = collectionToDelete!!
        AlertDialog(
            onDismissRequest = { collectionToDelete = null },
            title = { Text(stringResource(R.string.delete_collection)) },
            text = { Text(stringResource(R.string.delete_collection_message, col.name)) },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteCollection(col.id)
                    collectionToDelete = null
                }) {
                    Text("Sil", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { collectionToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedEmoji by remember { mutableStateOf("📁") }
    val presetEmojis = listOf("📁", "⭐", "📖", "🎯", "🔥", "💡", "🎨", "🎬", "✨", "🌸")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_new_collection)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.collection_name)) },
                    placeholder = { Text(stringResource(R.string.collection_name_example)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.choose_icon), style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    presetEmojis.forEach { emoji ->
                        Surface(
                            onClick = { selectedEmoji = emoji },
                            shape = CircleShape,
                            color = if (selectedEmoji == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(emoji, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name.trim(), selectedEmoji) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.create_action))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LibraryGridCard(item: LibraryItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val content = item.content
    Column(modifier.clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(218.dp).clip(RoundedCornerShape(7.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (content.coverUri.isNullOrBlank()) {
                CoverFallback(content.category)
            } else {
                SubcomposeAsyncImage(
                    model = content.coverUri,
                    contentDescription = content.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    loading = { CoverFallback(content.category) },
                    error = { CoverFallback(content.category) },
                )
            }
            if (content.isFavorite) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(24.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("❤️", fontSize = 11.sp)
                    }
                }
            }
            if (content.rating != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp),
                ) {
                    Text(
                        "★ ${formatRating(content.rating)}",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        Text(content.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(statusLabelRes(content.status)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
            if (content.rewatchDesired) {
                Text(stringResource(R.string.revisit_short), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        LinearProgressIndicator(
            progress = { content.progressFraction },
            modifier = Modifier.fillMaxWidth().height(3.dp),
            gapSize = 0.dp,
            drawStopIndicator = {},
        )
    }
}

@Composable
private fun EmptyLibraryCard(hasAnyItems: Boolean, onAddContent: () -> Unit) {
    Card(onClick = onAddContent, shape = RoundedCornerShape(10.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("📚", style = MaterialTheme.typography.displaySmall)
            Text(
                stringResource(if (hasAnyItems) R.string.no_filter_results else R.string.library_empty_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                stringResource(if (hasAnyItems) R.string.change_filters else R.string.library_empty_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LibraryContentCard(item: LibraryItem, onClick: () -> Unit) {
    val content = item.content
    Card(onClick = onClick, shape = RoundedCornerShape(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.size(width = 86.dp, height = 122.dp).clip(RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (content.coverUri.isNullOrBlank()) {
                    CoverFallback(content.category)
                } else {
                    SubcomposeAsyncImage(
                        model = content.coverUri,
                        contentDescription = content.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = { CoverFallback(content.category) },
                        error = { CoverFallback(content.category) },
                    )
                }
                if (content.isFavorite) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("❤️", fontSize = 10.sp)
                        }
                    }
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            content.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (content.creator.isNotBlank()) {
                            Text(
                                content.creator,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        StatusBadge(content.status)
                        if (content.rating != null) {
                            Text(
                                "★ ${formatRating(content.rating)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                Text(
                    "${stringResource(categoryLabelRes(content.category))} · " +
                        stringResource(
                            R.string.content_progress_value,
                            formatLibraryAmount(content.currentUnits),
                            formatLibraryAmount(content.totalUnits),
                            content.unit,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LinearProgressIndicator(
                    progress = { content.progressFraction },
                    modifier = Modifier.fillMaxWidth(),
                )
                if (content.usesSeasons && item.seasons.isNotEmpty()) {
                    Text(
                        pluralStringResource(R.plurals.season_count, item.seasons.size, item.seasons.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun CoverFallback(category: ContentCategory) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(categoryEmoji(category), style = MaterialTheme.typography.headlineLarge)
        }
    }
}

@Composable
internal fun StatusBadge(status: ContentStatus) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = when (status) {
            ContentStatus.Completed -> MaterialTheme.colorScheme.secondaryContainer
            ContentStatus.Dropped -> MaterialTheme.colorScheme.errorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
    ) {
        Text(
            stringResource(statusLabelRes(status)),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@StringRes
internal fun categoryLabelRes(category: ContentCategory): Int = when (category) {
    ContentCategory.Book -> R.string.category_book
    ContentCategory.Novel -> R.string.category_novel
    ContentCategory.Manga -> R.string.category_manga
    ContentCategory.Movie -> R.string.category_movie
    ContentCategory.Series -> R.string.category_series
    ContentCategory.Anime -> R.string.category_anime
    ContentCategory.Custom -> R.string.category_custom
}

@StringRes
internal fun statusLabelRes(status: ContentStatus): Int = when (status) {
    ContentStatus.Planned -> R.string.status_planned
    ContentStatus.InProgress -> R.string.status_in_progress
    ContentStatus.Completed -> R.string.status_completed
    ContentStatus.Paused -> R.string.status_paused
    ContentStatus.Dropped -> R.string.status_dropped
}

internal fun categoryEmoji(category: ContentCategory): String = when (category) {
    ContentCategory.Book -> "📘"
    ContentCategory.Novel -> "📕"
    ContentCategory.Manga -> "📖"
    ContentCategory.Movie -> "🎬"
    ContentCategory.Series -> "📺"
    ContentCategory.Anime -> "🌸"
    ContentCategory.Custom -> "◆"
}

@StringRes
internal fun defaultUnitRes(category: ContentCategory): Int = when (category) {
    ContentCategory.Book, ContentCategory.Novel, ContentCategory.Manga -> R.string.unit_pages
    ContentCategory.Movie -> R.string.unit_minutes
    ContentCategory.Series, ContentCategory.Anime -> R.string.unit_episodes
    ContentCategory.Custom -> R.string.unit_items
}

internal fun formatLibraryAmount(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", value)

internal fun formatRating(rating: Float): String =
    if (rating % 1.0f == 0.0f) rating.toInt().toString() else String.format(java.util.Locale.getDefault(), "%.1f", rating)
