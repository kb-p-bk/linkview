package com.linkview.feature.cards

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkview.core.model.Card as DomainCard
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
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
    modifier: Modifier = Modifier
) {
    var isTagDropdownExpanded by remember { mutableStateOf(false) }

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
            // Row 1: Search Bar (pill) + Add Button + Backup Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChanged,
                    placeholder = { Text("Search cards...", maxLines = 1) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onCreateCardClick,
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Card")
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    IconButton(onClick = onNavigateToBackup) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Backup & Restore", modifier = Modifier.size(20.dp))
                    }
                }
            }

            // Row 2: Tag Search & Autocomplete + Multi-Tag Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Compact Tag Search Input with Autocomplete Dropdown
                Box {
                    OutlinedTextField(
                        value = uiState.tagSearchQuery,
                        onValueChange = {
                            onTagSearchQueryChanged(it)
                            isTagDropdownExpanded = it.isNotBlank()
                        },
                        placeholder = { Text("Search & add tag...", maxLines = 1, style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (uiState.tagSearchQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    onTagSearchQueryChanged("")
                                    isTagDropdownExpanded = false
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear tag query", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        singleLine = true,
                        modifier = Modifier.width(175.dp)
                    )

                    // Tag Autocomplete Dropdown
                    val tagQuery = uiState.tagSearchQuery.trim()
                    val matchingTags = uiState.allAvailableTags.filter { it.contains(tagQuery, ignoreCase = true) }
                    val showCreateOption = tagQuery.isNotBlank() && !uiState.allAvailableTags.any { it.equals(tagQuery, ignoreCase = true) }

                    DropdownMenu(
                        expanded = isTagDropdownExpanded && (matchingTags.isNotEmpty() || showCreateOption),
                        onDismissRequest = { isTagDropdownExpanded = false }
                    ) {
                        if (showCreateOption) {
                            DropdownMenuItem(
                                text = { Text("+ Create \"$tagQuery\"") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    onAddAndSelectTag(tagQuery)
                                    isTagDropdownExpanded = false
                                }
                            )
                        }
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

            // Row 3: Scope Filter Chips (only when actively searching)
            if (uiState.isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchScope.entries.forEach { scope ->
                        FilterChip(
                            selected = uiState.activeScope == scope,
                            onClick = { onScopeSelected(scope) },
                            label = { Text(scope.displayName) }
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
 */
@Composable
fun CardsGridContent(
    uiState: CardsUiState,
    selectedCardId: String?,
    onCardClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.isSearching) {
            val searchResults = uiState.filteredSearchResults
            if (searchResults.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No cards matching '${uiState.searchQuery}'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(searchResults, key = { it.card.id }) { item ->
                        SearchResultGridItem(
                            item = item,
                            searchQuery = uiState.searchQuery,
                            isSelected = item.card.id == (selectedCardId ?: uiState.selectedCardId),
                            onClick = { onCardClick(item.card.id) }
                        )
                    }
                }
            }
        } else {
            val cards = uiState.filteredCards
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        CardGridItem(
                            card = card,
                            isSelected = card.id == (selectedCardId ?: uiState.selectedCardId),
                            onClick = { onCardClick(card.id) }
                        )
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
            onNavigateToBackup = onNavigateToBackup
        )

        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )

        CardsGridContent(
            uiState = uiState,
            selectedCardId = selectedCardId,
            onCardClick = onCardClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun CardGridItem(
    card: DomainCard,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
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
                .fillMaxWidth()
                .padding(14.dp)
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
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
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
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            HighlightedText(
                text = item.card.title,
                highlight = searchQuery,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                ),
                modifier = Modifier.fillMaxWidth()
            )

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
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

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
    }
}
