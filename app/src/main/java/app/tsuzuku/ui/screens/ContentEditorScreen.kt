package app.tsuzuku.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.ContentCategory
import app.tsuzuku.model.ContentDraft
import app.tsuzuku.model.ContentStatus
import app.tsuzuku.model.LibraryContent
import app.tsuzuku.data.local.CategoryEntity
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun ContentEditorScreen(
    existing: LibraryContent? = null,
    customCategories: List<CategoryEntity> = emptyList(),
    existingTitles: List<String> = emptyList(),
    onBack: () -> Unit,
    onSave: (ContentDraft) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localizedUnits = ContentCategory.entries.associateWith { stringResource(defaultUnitRes(it)) }
    var step by rememberSaveable { mutableIntStateOf(0) }
    var categoryName by rememberSaveable { mutableStateOf(existing?.category?.name ?: ContentCategory.Book.name) }
    var title by rememberSaveable { mutableStateOf(existing?.title.orEmpty()) }
    var creator by rememberSaveable { mutableStateOf(existing?.creator.orEmpty()) }
    var coverUri by rememberSaveable { mutableStateOf(existing?.coverUri.orEmpty()) }
    var statusName by rememberSaveable { mutableStateOf(existing?.status?.name ?: ContentStatus.Planned.name) }
    var currentText by rememberSaveable { mutableStateOf(formatLibraryAmount(existing?.currentUnits ?: 0.0)) }
    var totalText by rememberSaveable { mutableStateOf(formatLibraryAmount(existing?.totalUnits ?: 0.0)) }
    var unit by rememberSaveable { mutableStateOf(existing?.unit ?: localizedUnits.getValue(ContentCategory.Book)) }
    var generalNote by rememberSaveable { mutableStateOf(existing?.generalNote.orEmpty()) }
    var tags by rememberSaveable { mutableStateOf(existing?.tags.orEmpty()) }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val localUri = withContext(Dispatchers.IO) {
                val directory = File(context.filesDir, "covers").apply { mkdirs() }
                val target = File(directory, "${UUID.randomUUID()}.image")
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use(input::copyTo) }
                    target.toURI().toString()
                }.getOrNull()
            }
            if (localUri != null) coverUri = localUri
        }
    }

    val category = ContentCategory.valueOf(categoryName)
    val status = ContentStatus.valueOf(statusName)
    val current = currentText.toLibraryDoubleOrNull() ?: 0.0
    val total = totalText.toLibraryDoubleOrNull() ?: 0.0
    val canContinue = when (step) {
        0 -> true
        1 -> title.isNotBlank()
        else -> total > 0.0 && unit.isNotBlank()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(if (existing == null) R.string.new_content else R.string.edit_content))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { if (step == 0) onBack() else step-- }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Text(
                        stringResource(R.string.step_fraction, step + 1, 3),
                        modifier = Modifier.padding(end = 16.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (step > 0) {
                    OutlinedButton(onClick = { step-- }, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.back))
                    }
                }
                Button(
                    enabled = canContinue,
                    onClick = {
                        if (step < 2) {
                            step++
                        } else {
                            onSave(
                                ContentDraft(
                                    title = title,
                                    category = category,
                                    status = status,
                                    creator = creator,
                                    coverUri = coverUri,
                                    currentUnits = current,
                                    totalUnits = total,
                                    unit = unit,
                                    generalNote = generalNote,
                                    firstSeasonTotal = if (category.usesSeasonsForUi()) total.toInt() else 0,
                                    tags = tags,
                                ),
                            )
                        }
                    },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(stringResource(if (step == 2) R.string.save_content else R.string.continue_label))
                    if (step < 2) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(PaddingValues(horizontal = 20.dp, vertical = 10.dp)),
        ) {
            LinearProgressIndicator(progress = { (step + 1) / 3f }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(22.dp))
            Text(stringResource(contentStepTitle(step)), style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(contentStepDescription(step)),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 5.dp, bottom = 18.dp),
            )
            Text(stringResource(R.string.live_preview), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            Card(shape = RoundedCornerShape(5.dp), modifier = Modifier.fillMaxWidth().padding(top = 7.dp, bottom = 20.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(categoryEmoji(category), style = MaterialTheme.typography.headlineSmall)
                    Column(Modifier.weight(1f)) {
                        Text(title.ifBlank { stringResource(R.string.content_title) }, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "$currentText / ${totalText.ifBlank { "0" }} $unit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(stringResource(statusLabelRes(status)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            when (step) {
                0 -> CategoryStep(
                    selected = category,
                    customCategories = customCategories,
                    selectedTags = tags,
                    onSelected = {
                        categoryName = it.name
                        unit = localizedUnits.getValue(it)
                        if (it == ContentCategory.Movie && total <= 0.0) totalText = "120"
                    },
                    onCustomSelected = { selected ->
                        categoryName = ContentCategory.Custom.name
                        unit = localizedUnits.getValue(ContentCategory.Custom)
                        val currentTags = tags.split(',').map(String::trim).filter(String::isNotBlank)
                        tags = (currentTags + selected.name).distinctBy(String::lowercase).joinToString(", ")
                    },
                )
                1 -> ContentInformationStep(
                    title = title,
                    onTitleChange = { title = it },
                    creator = creator,
                    onCreatorChange = { creator = it },
                    coverUri = coverUri,
                    onCoverUriChange = { coverUri = it },
                    onChooseFromGallery = { galleryLauncher.launch(arrayOf("image/*")) },
                    tags = tags,
                    onTagsChange = { tags = it.take(240) },
                    customCategories = customCategories,
                    duplicateTitle = title.isNotBlank() && existingTitles.any { it.trim().equals(title.trim(), true) && !it.equals(existing?.title, true) },
                )
                else -> ContentProgressStep(
                    category = category,
                    isEditing = existing != null,
                    status = status,
                    onStatusChange = { statusName = it.name },
                    current = currentText,
                    onCurrentChange = { currentText = it },
                    total = totalText,
                    onTotalChange = { totalText = it },
                    unit = unit,
                    onUnitChange = { unit = it },
                    generalNote = generalNote,
                    onGeneralNoteChange = { generalNote = it },
                )
            }
        }
    }
}

@Composable
private fun CategoryStep(
    selected: ContentCategory,
    customCategories: List<CategoryEntity>,
    selectedTags: String,
    onSelected: (ContentCategory) -> Unit,
    onCustomSelected: (CategoryEntity) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ContentCategory.entries.forEach { category ->
            Card(
                onClick = { onSelected(category) },
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(17.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(categoryEmoji(category), style = MaterialTheme.typography.headlineSmall)
                    Column {
                        Text(stringResource(categoryLabelRes(category)), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(categoryDescriptionRes(category)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (selected == category) Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        customCategories.forEach { category ->
            val active = selected == ContentCategory.Custom && selectedTags.split(',').any { it.trim().equals(category.name, true) }
            Card(onClick = { onCustomSelected(category) }, shape = RoundedCornerShape(8.dp)) {
                Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(category.emoji, style = MaterialTheme.typography.headlineSmall)
                    Column(Modifier.weight(1f)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.personal_category_description), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (active) Text("✓", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun ContentInformationStep(
    title: String,
    onTitleChange: (String) -> Unit,
    creator: String,
    onCreatorChange: (String) -> Unit,
    coverUri: String,
    onCoverUriChange: (String) -> Unit,
    onChooseFromGallery: () -> Unit,
    tags: String,
    onTagsChange: (String) -> Unit,
    customCategories: List<CategoryEntity>,
    duplicateTitle: Boolean,
) {
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.content_title)) },
        singleLine = true,
    )
    if (duplicateTitle) Text(stringResource(R.string.similar_content_warning), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = creator,
        onValueChange = onCreatorChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.creator_name)) },
        singleLine = true,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = coverUri,
        onValueChange = onCoverUriChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.cover_address)) },
        supportingText = { Text(stringResource(R.string.cover_address_hint)) },
        singleLine = true,
    )
    Spacer(Modifier.height(10.dp))
    OutlinedButton(onClick = onChooseFromGallery, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.PhotoLibrary, contentDescription = null)
        Text(stringResource(R.string.choose_cover_from_gallery), modifier = Modifier.padding(start = 8.dp))
    }
    Spacer(Modifier.height(10.dp))
    OutlinedTextField(
        value = tags,
        onValueChange = onTagsChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.tags_label)) },
        supportingText = { Text(stringResource(R.string.tags_help)) },
        singleLine = true,
    )
    Spacer(Modifier.height(8.dp))
    CategorySuggestions(customCategories, tags, onTagsChange)
}

@Composable
private fun ContentProgressStep(
    category: ContentCategory,
    isEditing: Boolean,
    status: ContentStatus,
    onStatusChange: (ContentStatus) -> Unit,
    current: String,
    onCurrentChange: (String) -> Unit,
    total: String,
    onTotalChange: (String) -> Unit,
    unit: String,
    onUnitChange: (String) -> Unit,
    generalNote: String,
    onGeneralNoteChange: (String) -> Unit,
) {
    Text(stringResource(R.string.content_status), style = MaterialTheme.typography.labelLarge)
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        ContentStatus.entries.forEach { option ->
            FilterChip(
                selected = status == option,
                onClick = { onStatusChange(option) },
                label = { Text(stringResource(statusLabelRes(option))) },
            )
        }
    }
    Spacer(Modifier.height(14.dp))
    if (category.usesSeasonsForUi() && isEditing) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Text(
                stringResource(R.string.season_progress_edit_hint),
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = current,
                onValueChange = onCurrentChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.current_progress)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
            OutlinedTextField(
                value = total,
                onValueChange = onTotalChange,
                modifier = Modifier.weight(1f),
                label = {
                    Text(
                        stringResource(
                            if (category.usesSeasonsForUi()) R.string.first_season_total else R.string.total_amount,
                        ),
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = unit,
        onValueChange = onUnitChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.unit_label)) },
        singleLine = true,
    )
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = generalNote,
        onValueChange = onGeneralNoteChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.general_note)) },
        minLines = 4,
    )
}

private fun contentStepTitle(step: Int): Int = when (step) {
    0 -> R.string.choose_category
    1 -> R.string.content_information
    else -> R.string.progress_and_notes
}

private fun contentStepDescription(step: Int): Int = when (step) {
    0 -> R.string.choose_category_description
    1 -> R.string.content_information_description
    else -> R.string.progress_and_notes_description
}

private fun categoryDescriptionRes(category: ContentCategory): Int = when (category) {
    ContentCategory.Book, ContentCategory.Novel, ContentCategory.Manga -> R.string.category_pages_description
    ContentCategory.Movie -> R.string.category_minutes_description
    ContentCategory.Series, ContentCategory.Anime -> R.string.category_seasons_description
    ContentCategory.Custom -> R.string.category_custom_description
}

internal fun ContentCategory.usesSeasonsForUi(): Boolean =
    this == ContentCategory.Series || this == ContentCategory.Anime

private fun String.toLibraryDoubleOrNull(): Double? = replace(',', '.').toDoubleOrNull()
