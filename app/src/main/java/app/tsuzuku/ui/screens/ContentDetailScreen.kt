package app.tsuzuku.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.tsuzuku.R
import app.tsuzuku.model.ContentCategory
import app.tsuzuku.model.ContentSeason
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.LibraryCollection
import app.tsuzuku.model.LibraryDetail
import coil3.compose.SubcomposeAsyncImage
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ContentDetailScreen(
    detail: LibraryDetail?,
    availableCollections: List<LibraryCollection> = emptyList(),
    contentCollectionIds: List<Long> = emptyList(),
    onToggleCollection: (collectionId: Long, isInCollection: Boolean) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onProgress: (Long, Double) -> Unit,
    onStatus: (Long, ContentStatus) -> Unit,
    onSaveGeneralNote: (Long, String) -> Unit,
    onAddShortNote: (Long, String) -> Unit,
    onDeleteShortNote: (Long) -> Unit,
    onAddSeason: (Long, String, Int) -> Unit,
    onSeasonProgress: (Long, Int) -> Unit,
    onDeleteContent: (Long) -> Unit,
    onUpdateReview: (
        id: Long,
        rating: Float?,
        isFavorite: Boolean,
        reviewNote: String,
        startDate: Long?,
        finishDate: Long?,
        rewatch: Boolean,
    ) -> Unit = { _, _, _, _, _, _, _ -> },
) {
    val item = detail?.item
    val content = item?.content
    var generalNote by rememberSaveable(content?.id) { mutableStateOf(content?.generalNote.orEmpty()) }
    var shortNote by rememberSaveable(content?.id) { mutableStateOf("") }
    var showSeasonDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Tsuzuku 1.9 Review & Rating states
    var rating by rememberSaveable(content?.id) { mutableStateOf(content?.rating) }
    var isFavorite by rememberSaveable(content?.id) { mutableStateOf(content?.isFavorite ?: false) }
    var reviewNote by rememberSaveable(content?.id) { mutableStateOf(content?.reviewNote.orEmpty()) }
    var rewatchDesired by rememberSaveable(content?.id) { mutableStateOf(content?.rewatchDesired ?: false) }
    var startDate by rememberSaveable(content?.id) { mutableStateOf(content?.startDateEpochDay) }
    var finishDate by rememberSaveable(content?.id) { mutableStateOf(content?.finishDateEpochDay) }
    var reviewSavedMessage by remember { mutableStateOf(false) }

    LaunchedEffect(content?.generalNote) {
        if (content != null && generalNote != content.generalNote) generalNote = content.generalNote
    }

    LaunchedEffect(content?.reviewNote) {
        if (content != null) {
            rating = content.rating
            isFavorite = content.isFavorite
            reviewNote = content.reviewNote
            rewatchDesired = content.rewatchDesired
            startDate = content.startDateEpochDay
            finishDate = content.finishDateEpochDay
        }
    }

    LaunchedEffect(reviewSavedMessage) {
        if (reviewSavedMessage) {
            kotlinx.coroutines.delay(2000)
            reviewSavedMessage = false
        }
    }

    fun saveReviewState() {
        if (content != null) {
            onUpdateReview(
                content.id,
                rating,
                isFavorite,
                reviewNote,
                startDate,
                finishDate,
                rewatchDesired,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        content?.title ?: stringResource(R.string.content_detail),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (content != null) {
                        IconButton(onClick = {
                            isFavorite = !isFavorite
                            saveReviewState()
                        }) {
                            Icon(
                                if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite_label),
                                tint = if (isFavorite) Color(0xFFE91E63) else LocalContentColor.current,
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.delete_content))
                        }
                        IconButton(onClick = { onEdit(content.id) }) {
                            Icon(Icons.Rounded.Edit, contentDescription = stringResource(R.string.edit_content))
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (detail == null || content == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.loading_content))
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(width = 105.dp, height = 150.dp).clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (content.coverUri.isNullOrBlank()) {
                            DetailCoverFallback(content.category)
                        } else {
                            SubcomposeAsyncImage(
                                model = content.coverUri,
                                contentDescription = content.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                loading = { DetailCoverFallback(content.category) },
                                error = { DetailCoverFallback(content.category) },
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(content.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (content.creator.isNotBlank()) {
                            Text(content.creator, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "${categoryEmoji(content.category)} ${stringResource(categoryLabelRes(content.category))}",
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusBadge(content.status)
                            if (isFavorite) {
                                Text("❤️ Favori", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Collections membership chips
            if (availableCollections.isNotEmpty()) {
                item {
                    Text(stringResource(R.string.collections_label), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        availableCollections.forEach { col ->
                            val isIn = col.id in contentCollectionIds
                            FilterChip(
                                selected = isIn,
                                onClick = { onToggleCollection(col.id, isIn) },
                                label = { Text("${col.emoji} ${col.name}") },
                            )
                        }
                    }
                }
            }

            // Progress Section
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(stringResource(R.string.total_progress), style = MaterialTheme.typography.titleMedium)
                                Text(
                                    stringResource(
                                        R.string.content_progress_value,
                                        formatLibraryAmount(content.currentUnits),
                                        formatLibraryAmount(content.totalUnits),
                                        content.unit,
                                    ),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Text("%${(content.progressFraction * 100).toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(progress = { content.progressFraction }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)))
                        if (!content.usesSeasons) {
                            val step = if (content.category == ContentCategory.Movie) 5.0 else 1.0
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { onProgress(content.id, -step) },
                                    enabled = content.currentUnits > 0,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.Remove, contentDescription = null)
                                    Text(formatLibraryAmount(step), modifier = Modifier.padding(start = 5.dp))
                                }
                                Button(
                                    onClick = { onProgress(content.id, step) },
                                    enabled = content.currentUnits < content.totalUnits,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Rounded.Add, contentDescription = null)
                                    Text(formatLibraryAmount(step), modifier = Modifier.padding(start = 5.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Status chips
            item {
                Text(stringResource(R.string.content_status), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    ContentStatus.entries.forEach { status ->
                        FilterChip(
                            selected = content.status == status,
                            onClick = { onStatus(content.id, status) },
                            label = { Text(stringResource(statusLabelRes(status))) },
                        )
                    }
                }
            }

            // Tsuzuku 1.9: Review, Rating & Dates Card
            item {
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(stringResource(R.string.review_and_reflection), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        // 5-Star Rating
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.rating_label), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(60.dp))
                            (1..5).forEach { star ->
                                IconButton(
                                    onClick = {
                                        rating = if (rating == star.toFloat()) null else star.toFloat()
                                        saveReviewState()
                                    },
                                    modifier = Modifier.size(36.dp),
                                ) {
                                    Text(
                                        if (rating != null && rating!! >= star) "★" else "☆",
                                        fontSize = 24.sp,
                                        color = if (rating != null && rating!! >= star) Color(0xFFFFB300) else MaterialTheme.colorScheme.outlineVariant,
                                    )
                                }
                            }
                            rating?.let {
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "${formatRating(it)} / 5",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        // Dates: Start & Finish
                        val locale = LocalConfiguration.current.locales[0]
                        val dtf = remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy", locale) }
                        val todayEpoch = remember { LocalDate.now().toEpochDay() }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(stringResource(R.string.start_date_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    startDate?.let { LocalDate.ofEpochDay(it).format(dtf) } ?: stringResource(R.string.not_specified),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (startDate == null) {
                                TextButton(onClick = {
                                    startDate = todayEpoch
                                    saveReviewState()
                                }) { Text(stringResource(R.string.started_today)) }
                            } else {
                                TextButton(onClick = {
                                    startDate = null
                                    saveReviewState()
                                }) { Text(stringResource(R.string.clear_label)) }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text(stringResource(R.string.finish_date_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    finishDate?.let { LocalDate.ofEpochDay(it).format(dtf) } ?: stringResource(R.string.not_specified),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                            if (finishDate == null) {
                                TextButton(onClick = {
                                    finishDate = todayEpoch
                                    saveReviewState()
                                }) { Text(stringResource(R.string.finished_today)) }
                            } else {
                                TextButton(onClick = {
                                    finishDate = null
                                    saveReviewState()
                                }) { Text(stringResource(R.string.clear_label)) }
                            }
                        }

                        // Rewatch toggle
                        FilterChip(
                            selected = rewatchDesired,
                            onClick = {
                                rewatchDesired = !rewatchDesired
                                saveReviewState()
                            },
                            label = { Text(stringResource(R.string.want_to_revisit)) },
                        )

                        // Review Note
                        OutlinedTextField(
                            value = reviewNote,
                            onValueChange = { reviewNote = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.critique_title)) },
                            placeholder = { Text(stringResource(R.string.critique_placeholder)) },
                            minLines = 3,
                            maxLines = 6,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (reviewSavedMessage) {
                                Text(stringResource(R.string.saved_check), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            Button(onClick = {
                                saveReviewState()
                                reviewSavedMessage = true
                            }) {
                                Text(stringResource(R.string.save_review))
                            }
                        }
                    }
                }
            }

            // Seasons
            if (content.usesSeasons) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.seasons), style = MaterialTheme.typography.titleLarge)
                        FilledIconButton(onClick = { showSeasonDialog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_season))
                        }
                    }
                }
                if (item.seasons.isEmpty()) {
                    item { Text(stringResource(R.string.no_seasons), color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(item.seasons, key = { "season_${it.id}" }) { season ->
                        SeasonCard(season = season, onProgress = onSeasonProgress)
                    }
                }
            }

            // General note
            item {
                Text(stringResource(R.string.general_note), style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = generalNote,
                    onValueChange = { generalNote = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    placeholder = { Text(stringResource(R.string.general_note_hint)) },
                    minLines = 3,
                )
                Button(
                    onClick = { onSaveGeneralNote(content.id, generalNote) },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(stringResource(R.string.save_note))
                }
            }

            // Short notes
            item {
                Text(stringResource(R.string.short_notes), style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = shortNote,
                        onValueChange = { shortNote = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.short_note_hint)) },
                        maxLines = 3,
                    )
                    FilledIconButton(
                        onClick = {
                            onAddShortNote(content.id, shortNote)
                            shortNote = ""
                        },
                        enabled = shortNote.isNotBlank(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.add_note))
                    }
                }
            }
            items(detail.notes, key = { "note_${it.id}" }) { note ->
                val locale = LocalLocale.current.platformLocale
                val date = Instant.ofEpochMilli(note.createdAt)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", locale))
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(note.text)
                            Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onDeleteShortNote(note.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = stringResource(R.string.delete_note))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }

    if (showSeasonDialog && content != null) {
        AddSeasonDialog(
            onDismiss = { showSeasonDialog = false },
            onSave = { title, total ->
                onAddSeason(content.id, title, total)
                showSeasonDialog = false
            },
        )
    }
    if (showDeleteDialog && content != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_content)) },
            text = { Text(stringResource(R.string.delete_content_confirmation, content.title)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDeleteContent(content.id)
                }) { Text(stringResource(R.string.delete_label), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SeasonCard(season: ContentSeason, onProgress: (Long, Int) -> Unit) {
    Card(shape = RoundedCornerShape(8.dp)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(season.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(R.string.episode_progress, season.currentEpisodes, season.totalEpisodes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            LinearProgressIndicator(
                progress = { if (season.totalEpisodes > 0) season.currentEpisodes.toFloat() / season.totalEpisodes else 0f },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onProgress(season.id, -1) },
                    enabled = season.currentEpisodes > 0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = null)
                    Text("1", modifier = Modifier.padding(start = 5.dp))
                }
                Button(
                    onClick = { onProgress(season.id, 1) },
                    enabled = season.currentEpisodes < season.totalEpisodes,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("1", modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun DetailCoverFallback(category: ContentCategory) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(categoryEmoji(category), style = MaterialTheme.typography.displayMedium)
        }
    }
}

@Composable
private fun AddSeasonDialog(
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var episodesText by rememberSaveable { mutableStateOf("") }
    val episodes = episodesText.toIntOrNull() ?: 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_season)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.season_title_optional)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = episodesText,
                    onValueChange = { episodesText = it.filter(Char::isDigit) },
                    label = { Text(stringResource(R.string.total_episodes)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, episodes) },
                enabled = title.isNotBlank() && episodes > 0,
            ) {
                Text(stringResource(R.string.add_label))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
