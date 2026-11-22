package com.linkview.feature.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkview.core.model.Card as DomainCard
import com.linkview.core.model.CardCollection
import com.linkview.core.model.CardSortOrder
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.core.ui.components.CompactSearchBar
import com.linkview.core.ui.components.HighlightedText

/**
 * Full-width Search & Tag Header Component.
 * Unified, cohesive design with clean single-line inputs and tag autocomplete.
 */
@Composable
fun CardsHeader(
    uiState: CardsUiState,
    onSearchQueryChanged: (String) -> Unit,
    onScopeSelected: (SearchScope) -> Unit,
    onTagSearchQueryChanged: (String) -> Unit,
    onClearTagFilters: () -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onAddAndSelectTag: (String) -> Unit,
    onCreateCardClick: () -> Unit,
    onNavigateToBackup: () -> Unit,
    onSelectCollection: (String) -> Unit = {},
    onCreateCollection: (String) -> Unit = {},
    onDeleteCollection: (String) -> Unit = {},
    onToggleHideStubs: () -> Unit = {},
    onSortOrderSelected: (CardSortOrder) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTagDropdownExpanded by remember { mutableStateOf(false) }
    var isCollectionMenuExpanded by remember { mutableStateOf(false) }
    var isSortMenuExpanded by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var collectionToDelete by remember { mutableStateOf<CardCollection?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Collection Switcher + Search Bar (pill) + Add Button + Backup Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Collection Switcher Button (pill)
                Box {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable { isCollectionMenuExpanded = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = "Collection",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = uiState.activeCollectionName,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 110.dp)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isCollectionMenuExpanded,
                        onDismissRequest = { isCollectionMenuExpanded = false }
                    ) {
                        Text(
                            text = "Collections",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        uiState.collections.forEach { collection ->
                            val isSelected = collection.id == uiState.activeCollectionId
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = collection.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }
                                },
                                trailingIcon = {
                                    if (collection.id != CardCollection.DEFAULT_ID) {
                                        IconButton(
                                            onClick = {
                                                isCollectionMenuExpanded = false
                                                collectionToDelete = collection
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Delete collection",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    isCollectionMenuExpanded = false
                                    onSelectCollection(collection.id)
                                }
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("New Collection...") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isCollectionMenuExpanded = false
                                showCreateDialog = true
                            }
                        )
                    }
                }

                CompactSearchBar(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholderText = "Search cards (or title:name)...",
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { onSearchQueryChanged("") },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    IconButton(
                        onClick = onCreateCardClick,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Card",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    IconButton(
                        onClick = onNavigateToBackup,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Backup & Restore", modifier = Modifier.size(20.dp))
                    }
                }
            }

            if (showCreateDialog) {
                var newCollectionName by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false },
                    title = { Text("New Collection") },
                    text = {
                        Column {
                            Text(
                                text = "Create a separate collection for isolated cards, references, and tags.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = newCollectionName,
                                onValueChange = { newCollectionName = it },
                                label = { Text("Collection Name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val clean = newCollectionName.trim()
                                if (clean.isNotEmpty()) {
                                    onCreateCollection(clean)
                                    showCreateDialog = false
                                }
                            },
                            enabled = newCollectionName.trim().isNotEmpty()
                        ) {
                            Text("Create")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            collectionToDelete?.let { col ->
                AlertDialog(
                    onDismissRequest = { collectionToDelete = null },
                    title = { Text("Delete Collection") },
                    text = {
                        Text("Are you sure you want to delete '${col.name}'? All cards and context units in this collection will be permanently deleted.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDeleteCollection(col.id)
                                collectionToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { collectionToDelete = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Row 2: Sort Button + Tag Search & Autocomplete + Multi-Tag Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Sort Icon with Dropdown Menu on the left
                Box {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        IconButton(
                            onClick = { isSortMenuExpanded = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Cards",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = isSortMenuExpanded,
                        onDismissRequest = { isSortMenuExpanded = false }
                    ) {
                        Text(
                            text = "Sort Cards",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )

                        CardSortOrder.entries.forEach { order ->
                            val isSelected = uiState.sortOrder == order
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = order.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                leadingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.size(18.dp))
                                    }
                                },
                                onClick = {
                                    isSortMenuExpanded = false
                                    onSortOrderSelected(order)
                                }
                            )
                        }
                    }
                }

                // Compact Tag Search Input with Autocomplete Dropdown
                Box {
                    CompactSearchBar(
                        value = uiState.tagSearchQuery,
                        onValueChange = {
                            onTagSearchQueryChanged(it)
                            isTagDropdownExpanded = it.isNotBlank()
                        },
                        placeholderText = "Filter tag...",
                        leadingIcon = {
                            Icon(
                                Icons.Default.Tag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (uiState.tagSearchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        onTagSearchQueryChanged("")
                                        isTagDropdownExpanded = false
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear tag query", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        modifier = Modifier.widthIn(min = 120.dp, max = 160.dp)
                    )

                    // Tag Autocomplete Dropdown
                    val tagQuery = uiState.tagSearchQuery.trim()
                    val allCandidateTags = (uiState.allAvailableTags + uiState.selectedTags).distinct()
                    val matchingTags = allCandidateTags.filter { it.contains(tagQuery, ignoreCase = true) }
                    val showCreateOption = tagQuery.isNotBlank() && !allCandidateTags.any { it.equals(tagQuery, ignoreCase = true) }

                    DropdownMenu(
                        expanded = isTagDropdownExpanded && (matchingTags.isNotEmpty() || showCreateOption),
                        onDismissRequest = { isTagDropdownExpanded = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        matchingTags.take(8).forEach { tag ->
                            val isSelected = uiState.selectedTags.any { it.equals(tag, ignoreCase = true) }
                            DropdownMenuItem(
                                text = { Text(tag) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                onClick = {
                                    onToggleTagFilter(tag)
                                    onTagSearchQueryChanged("")
                                    isTagDropdownExpanded = false
                                }
                            )
                        }
                        if (showCreateOption) {
                            DropdownMenuItem(
                                text = { Text("\"$tagQuery\"") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    onAddAndSelectTag(tagQuery)
                                    isTagDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Scrollable Tag Filter Chips
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedTags.isEmpty(),
                        onClick = onClearTagFilters,
                        label = { Text("All") }
                    )

                    // Hide Stubs Filter Chip (next to All, selected by default)
                    FilterChip(
                        selected = uiState.hideStubs,
                        onClick = onToggleHideStubs,
                        label = { Text("Hide Stubs") },
                        leadingIcon = if (uiState.hideStubs) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )

                    // Selected tags with checkmark & dismiss
                    uiState.selectedTags.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { onToggleTagFilter(tag) },
                            label = { Text(tag) },
                            leadingIcon = { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            trailingIcon = { Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        )
                    }

                    // Available unselected tags
                    val unselectedTags = uiState.filteredAvailableTags.filterNot { tag ->
                        uiState.selectedTags.any { it.equals(tag, ignoreCase = true) }
                    }
                    unselectedTags.take(12).forEach { tag ->
                        FilterChip(
                            selected = false,
                            onClick = { onToggleTagFilter(tag) },
                            label = { Text(tag) }
                        )
                    }
                }
            }

        }
    }
}

/**
 * Responsive Cards Grid Content (Below Header).
 * Displays cards in rows of 1, 2, or 3+ depending on screen width.
 * Automatically paginates cards (in batches of 1,000) to keep memory and rendering optimal.
 */
@Composable
fun CardsGridContent(
    uiState: CardsUiState,
    selectedCardId: String?,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLoadMore: () -> Unit = {}
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = if (maxWidth < 540.dp) {
            1
        } else {
            val minCardSize = 260.dp
            val horizontalSpacing = 10.dp
            val padding = 32.dp // 16.dp each side
            val availableWidth = maxWidth - padding
            maxOf(1, ((availableWidth + horizontalSpacing) / (minCardSize + horizontalSpacing)).toInt())
        }

        val listState = rememberLazyListState()

        val shouldLoadMore = remember {
            derivedStateOf {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleItem >= totalItems - 5
            }
        }

        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) {
                val hasMore = if (uiState.isSearching) uiState.hasMoreSearchResults else uiState.hasMoreCards
                if (hasMore) {
                    onLoadMore()
                }
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.isSearching) {
            val searchResults = uiState.displayedSearchResults
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (uiState.cleanSearchQuery.isBlank()) "Type a search term..." else "No cards matching '${uiState.cleanSearchQuery}'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val rows = searchResults.chunked(columns)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rows, key = { row -> "${columns}_${row.first().card.id}" }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (item in rowItems) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    SearchResultGridItem(
                                        item = item,
                                        searchQuery = uiState.cleanSearchQuery,
                                        isSelected = item.card.id == (selectedCardId ?: uiState.selectedCardId),
                                        onClick = { onCardClick(item.card.id) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            val emptySpaces = columns - rowItems.size
                            for (i in 0 until emptySpaces) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (uiState.hasMoreSearchResults) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Showing ${uiState.displayedSearchResults.size} of ${uiState.filteredSearchResults.size} results",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(onClick = onLoadMore) {
                                    Text("Load more")
                                }
                            }
                        }
                    } else if (uiState.filteredSearchResults.size > 1000) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All ${uiState.filteredSearchResults.size} results loaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        } else {
            val cards = uiState.displayedCards
            if (cards.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (uiState.selectedTags.isNotEmpty()) "No cards matching selected tags" else "No cards yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap New Card to create a card",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                val rows = cards.chunked(columns)
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(rows, key = { row -> "${columns}_${row.first().id}" }) { rowCards ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (card in rowCards) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                ) {
                                    CardGridItem(
                                        card = card,
                                        isSelected = card.id == (selectedCardId ?: uiState.selectedCardId),
                                        onClick = { onCardClick(card.id) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            val emptySpaces = columns - rowCards.size
                            for (i in 0 until emptySpaces) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    if (uiState.hasMoreCards) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Showing ${uiState.displayedCards.size} of ${uiState.filteredCards.size} cards",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                FilledTonalButton(onClick = onLoadMore) {
                                    Text("Load more")
                                }
                            }
                        }
                    } else if (uiState.filteredCards.size > 1000) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "All ${uiState.filteredCards.size} cards loaded",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Standard Phone Single-Pane Screen
 */
@Composable
fun CardsScreen(
    onCardClick: (cardId: String) -> Unit,
    onCreateCardClick: () -> Unit,
    onNavigateToBackup: () -> Unit,
    selectedCardId: String? = null,
    viewModel: CardsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        CardsHeader(
            uiState = uiState,
            onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
            onScopeSelected = { viewModel.onScopeSelected(it) },
            onTagSearchQueryChanged = { viewModel.onTagSearchQueryChanged(it) },
            onClearTagFilters = { viewModel.clearTagFilters() },
            onToggleTagFilter = { viewModel.toggleTagFilter(it) },
            onAddAndSelectTag = { viewModel.addAndSelectCustomTag(it) },
            onCreateCardClick = onCreateCardClick,
            onNavigateToBackup = onNavigateToBackup,
            onSelectCollection = { viewModel.selectCollection(it) },
            onCreateCollection = { viewModel.createCollection(it) },
            onDeleteCollection = { viewModel.deleteCollection(it) },
            onToggleHideStubs = { viewModel.toggleHideStubs() },
            onSortOrderSelected = { viewModel.onSortOrderSelected(it) }
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        CardsGridContent(
            uiState = uiState,
            selectedCardId = selectedCardId,
            onCardClick = onCardClick,
            modifier = Modifier.weight(1f),
            onLoadMore = { viewModel.loadMoreCards() }
        )
    }
}

@Composable
private fun CardGridItem(
    card: DomainCard,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = card.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )

            val tags = card.allTags
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultGridItem(
    item: SearchResultItem,
    searchQuery: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                HighlightedText(
                    text = item.card.title,
                    highlight = searchQuery,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (item.matchingSnippets.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    item.matchingSnippets.take(2).forEach { snippet ->
                        HighlightedText(
                            text = "“…$snippet…”",
                            highlight = searchQuery,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2
                        )
                    }
                }
            }

            val tags = item.card.allTags
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
