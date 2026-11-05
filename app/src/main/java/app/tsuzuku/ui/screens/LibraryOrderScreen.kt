package app.tsuzuku.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.tsuzuku.R
import app.tsuzuku.model.LibraryItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryOrderScreen(
    items: List<LibraryItem>,
    onBack: () -> Unit,
    onMove: (Long, Int) -> Unit,
) {
    val ordered = items.sortedBy { it.content.sortOrder }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reorder_library)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(stringResource(R.string.reorder_library_description), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            itemsIndexed(ordered, key = { _, item -> item.content.id }) { index, item ->
                Column {
                    Row(
                        Modifier.fillMaxWidth().padding(13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(categoryEmoji(item.content.category), style = MaterialTheme.typography.headlineSmall)
                        Text(item.content.title, modifier = Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onMove(item.content.id, -1) }, enabled = index > 0) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { onMove(item.content.id, 1) }, enabled = index < ordered.lastIndex) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = stringResource(R.string.move_down))
                        }
                    }
                }
            }
        }
    }
}
