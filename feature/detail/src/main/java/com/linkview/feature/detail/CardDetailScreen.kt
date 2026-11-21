package com.linkview.feature.detail

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.model.BacklinkItem
import com.linkview.core.ui.components.CompactSearchBar
import com.linkview.core.ui.components.ContextUnitEditorRow
import com.linkview.core.ui.components.RichMentionText
import kotlinx.coroutines.launch

private enum class ContextTab {
    CONTEXT,
    EXTRA_CONTEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    cardId: String,
    onNavigateBack: () -> Unit,
    onEditCard: (cardId: String) -> Unit,
    onNavigateToCard: (cardId: String) -> Unit,
    onDeleteSuccess: () -> Unit,
    showBackButton: Boolean = true,
    onClose: (() -> Unit)? = null,
    viewModel: CardDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showJsonDialog by remember { mutableStateOf(false) }
    var selectedContextTab by remember(cardId) { mutableStateOf(ContextTab.CONTEXT) }
    var searchQuery by rememberSaveable(cardId) { mutableStateOf("") }

    LaunchedEffect(cardId) {
        viewModel.loadCard(cardId)
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) {
            viewModel.resetDeleteSuccess()
            onDeleteSuccess()
        }
    }

    BackHandler(enabled = uiState.editingUnitIndex != null) {
        viewModel.cancelEditingUnit()
    }

    val initialScroll = remember(cardId) { viewModel.getScrollPosition(cardId) }
    val scrollState = rememberSaveable(cardId, saver = ScrollState.Saver) {
        ScrollState(initial = initialScroll)
    }

    LaunchedEffect(scrollState.value) {
        viewModel.saveScrollPosition(cardId, scrollState.value)
    }

    LaunchedEffect(cardId, uiState.cardWithContext) {
        val targetPos = viewModel.getScrollPosition(cardId)
        if (targetPos > 0 && scrollState.value != targetPos) {
            scrollState.scrollTo(targetPos)
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.cardWithContext?.card?.title.orEmpty(),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    if (showBackButton) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (uiState.cardWithContext != null) {
                        IconButton(onClick = { showJsonDialog = true }) {
                            Icon(Icons.Default.Code, contentDescription = "View & Copy Card JSON")
                        }
                        IconButton(onClick = { onEditCard(cardId) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Card")
                        }
                        IconButton(onClick = { viewModel.requestDelete() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Card")
                        }
                    }
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.Default.Close, contentDescription = "Close Detail")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.cardWithContext == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val cardWithContext = uiState.cardWithContext
        if (cardWithContext == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(uiState.errorMessage ?: "Select or create a card", style = MaterialTheme.typography.bodyLarge)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Tags
            Column {
                Text(
                    text = cardWithContext.card.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                val allTags = cardWithContext.card.allTags
                if (allTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Search within card details (context & extra context)
            CompactSearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholderText = if (selectedContextTab == ContextTab.CONTEXT) "Search context notes..." else "Search extra context...",
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            )

            val authoredUnitsWithIndex = remember(cardWithContext.authoredUnits) {
                cardWithContext.authoredUnits.mapIndexed { index, unit -> index to unit }
            }

            val filteredAuthoredUnits = remember(authoredUnitsWithIndex, searchQuery) {
                if (searchQuery.isBlank()) {
                    authoredUnitsWithIndex
                } else {
                    val query = searchQuery.trim()
                    authoredUnitsWithIndex.filter { (_, unit) ->
                        unit.plainText.contains(query, ignoreCase = true) ||
                        unit.rawText.contains(query, ignoreCase = true)
                    }
                }
            }

            val filteredIncomingBacklinks = remember(cardWithContext.incomingBacklinks, searchQuery) {
                if (searchQuery.isBlank()) {
                    cardWithContext.incomingBacklinks
                } else {
                    val query = searchQuery.trim()
                    cardWithContext.incomingBacklinks.filter { backlink ->
                        backlink.plainText.contains(query, ignoreCase = true) ||
                        backlink.rawText.contains(query, ignoreCase = true) ||
                        backlink.sourceCardTitle.contains(query, ignoreCase = true)
                    }
                }
            }

            // Context / Extra Context Switcher Row with Copy All Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val contextCountLabel = if (searchQuery.isBlank()) {
                        "${cardWithContext.authoredUnits.size}"
                    } else {
                        "${filteredAuthoredUnits.size}/${cardWithContext.authoredUnits.size}"
                    }

                    // Context Tab Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedContextTab == ContextTab.CONTEXT) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.clickable { selectedContextTab = ContextTab.CONTEXT }
                    ) {
                        Text(
                            text = "Context ($contextCountLabel)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedContextTab == ContextTab.CONTEXT) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedContextTab == ContextTab.CONTEXT) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    val extraCountLabel = if (searchQuery.isBlank()) {
                        "${cardWithContext.incomingBacklinks.size}"
                    } else {
                        "${filteredIncomingBacklinks.size}/${cardWithContext.incomingBacklinks.size}"
                    }

                    // Extra Context Tab Chip
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selectedContextTab == ContextTab.EXTRA_CONTEXT) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.clickable {
                            if (uiState.editingUnitIndex != null) {
                                viewModel.cancelEditingUnit()
                            }
                            selectedContextTab = ContextTab.EXTRA_CONTEXT
                        }
                    ) {
                        Text(
                            text = "Extra Context ($extraCountLabel)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selectedContextTab == ContextTab.EXTRA_CONTEXT) FontWeight.Bold else FontWeight.Medium,
                            color = if (selectedContextTab == ContextTab.EXTRA_CONTEXT) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Copy all context / extra context button
                IconButton(
                    onClick = {
                        if (selectedContextTab == ContextTab.CONTEXT) {
                            if (filteredAuthoredUnits.isEmpty()) {
                                val msg = if (searchQuery.isBlank()) "No context notes to copy" else "No matching context notes to copy"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                val fullText = filteredAuthoredUnits.joinToString("\n\n") { (_, u) ->
                                    MentionTokenizer.stripCardUuids(u.rawText)
                                }
                                clipboardManager.setText(AnnotatedString(fullText))
                                val msg = if (searchQuery.isBlank()) "Context copied to clipboard" else "Matching context copied to clipboard"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            if (filteredIncomingBacklinks.isEmpty()) {
                                val msg = if (searchQuery.isBlank()) "No extra context to copy" else "No matching extra context to copy"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            } else {
                                val fullText = filteredIncomingBacklinks.joinToString("\n\n") { backlink ->
                                    "From @${backlink.sourceCardTitle}:\n${MentionTokenizer.stripCardUuids(backlink.rawText)}"
                                }
                                clipboardManager.setText(AnnotatedString(fullText))
                                val msg = if (searchQuery.isBlank()) "Extra context copied to clipboard" else "Matching extra context copied to clipboard"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = if (selectedContextTab == ContextTab.CONTEXT) "Copy all context" else "Copy all extra context",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tab Content
            if (selectedContextTab == ContextTab.CONTEXT) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cardWithContext.authoredUnits.isEmpty()) {
                        Text(
                            text = "No context notes yet for this card.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (filteredAuthoredUnits.isEmpty()) {
                        Text(
                            text = "No context notes matching \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val activeIndex = uiState.editingUnitIndex
                        if (activeIndex != null && uiState.editableUnits.isNotEmpty()) {
                            // When editing a specific unit: render editable row for that unit, read cards for others
                            val editableUnitsWithIndex = uiState.editableUnits.mapIndexed { idx, u -> idx to u }
                            val displayedEditableUnits = if (searchQuery.isBlank()) {
                                editableUnitsWithIndex
                            } else {
                                val query = searchQuery.trim()
                                editableUnitsWithIndex.filter { (idx, u) ->
                                    idx == activeIndex || u.rawText.contains(query, ignoreCase = true)
                                }
                            }

                            displayedEditableUnits.forEach { (index, editableUnit) ->
                                if (index == activeIndex) {
                                    key(editableUnit.id) {
                                        ContextUnitEditorRow(
                                            index = index,
                                            unit = editableUnit,
                                            totalUnits = uiState.editableUnits.size,
                                            isMentionActive = uiState.activeMentionQuery != null,
                                            mentionQuery = uiState.activeMentionQuery,
                                            suggestions = uiState.mentionSuggestions,
                                            onTextChanged = { text, cursor ->
                                                viewModel.onUnitTextChanged(index, text, cursor)
                                            },
                                            onMoveUp = { viewModel.moveEditingUnit(index, index - 1) },
                                            onMoveDown = { viewModel.moveEditingUnit(index, index + 1) },
                                            onInsertBelow = { viewModel.insertUnitAfterEditing(index) },
                                            onDelete = { viewModel.deleteEditingUnit(index) },
                                            onSelectMention = { card -> viewModel.selectMention(card) },
                                            onQuickCreateMention = { title -> viewModel.quickCreateAndSelectMention(title) },
                                            onSave = { viewModel.saveEditingUnit() },
                                            onCancel = { viewModel.cancelEditingUnit() },
                                            isSaving = uiState.isSavingUnit
                                        )
                                    }
                                } else {
                                    ReadOnlyUnitCard(
                                        index = index,
                                        rawText = editableUnit.rawText,
                                        onNavigateToCard = onNavigateToCard,
                                        onEdit = { viewModel.startEditingUnit(index) },
                                        onInsertBelow = { viewModel.insertUnitAfterEditing(index) },
                                        onInsertAbove = { viewModel.insertUnitAbove(index) }
                                    )
                                }
                            }
                        } else {
                            // All units in normal read mode
                            filteredAuthoredUnits.forEach { (index, unit) ->
                                ReadOnlyUnitCard(
                                    index = index,
                                    rawText = unit.rawText,
                                    onNavigateToCard = onNavigateToCard,
                                    onEdit = { viewModel.startEditingUnit(index) },
                                    onInsertBelow = { viewModel.insertUnitBelow(index) },
                                    onInsertAbove = { viewModel.insertUnitAbove(index) }
                                )
                            }
                        }
                    }
                }
            } else {
                // Extra Context Tab Content
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cardWithContext.incomingBacklinks.isEmpty()) {
                        Text(
                            text = "No other cards mention @${cardWithContext.card.title} yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (filteredIncomingBacklinks.isEmpty()) {
                        Text(
                            text = "No extra context matching \"$searchQuery\".",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        filteredIncomingBacklinks.forEach { backlink ->
                            BacklinkUnitCard(
                                backlink = backlink,
                                onNavigateToCard = onNavigateToCard
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Card JSON view, copy & edit dialog
        if (showJsonDialog) {
            var editableJson by remember(cardWithContext) { mutableStateOf(formatCardToJson(cardWithContext)) }
            var jsonErrorMessage by remember { mutableStateOf<String?>(null) }
            var isSavingJson by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()

            AlertDialog(
                onDismissRequest = { if (!isSavingJson) showJsonDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Card JSON", style = MaterialTheme.typography.titleLarge)
                        FilledTonalButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(editableJson))
                                Toast.makeText(context, "JSON copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy")
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "You can edit this JSON directly or paste modified JSON below:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = editableJson,
                            onValueChange = {
                                editableJson = it
                                jsonErrorMessage = null
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 200.dp, max = 360.dp),
                            shape = RoundedCornerShape(8.dp)
                        )
                        if (jsonErrorMessage != null) {
                            Text(
                                text = jsonErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                isSavingJson = true
                                val result = viewModel.saveCardFromJson(cardWithContext.card.id, editableJson)
                                isSavingJson = false
                                when (result) {
                                    is Result.Success -> {
                                        showJsonDialog = false
                                        Toast.makeText(context, "Card updated successfully", Toast.LENGTH_SHORT).show()
                                    }
                                    is Result.Error -> {
                                        jsonErrorMessage = result.message ?: "Failed to save card JSON"
                                    }
                                    else -> Unit
                                }
                            }
                        },
                        enabled = !isSavingJson && editableJson.isNotBlank()
                    ) {
                        if (isSavingJson) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save Changes")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showJsonDialog = false },
                        enabled = !isSavingJson
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete confirmation dialog
        if (uiState.isConfirmingDelete) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelDelete() },
                title = { Text("Delete Card?") },
                text = {
                    Column {
                        Text("Are you sure you want to delete '${cardWithContext.card.title}'?")
                        if (uiState.referencingBacklinkCount > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.referencingBacklinkCount} context unit(s) from other cards reference this card.",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.cancelDelete() }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun ReadOnlyUnitCard(
    index: Int,
    rawText: String,
    onNavigateToCard: (String) -> Unit,
    onEdit: () -> Unit,
    onInsertBelow: () -> Unit,
    onInsertAbove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top Row: Unit index on top-left, Three-dots menu on top-right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#${index + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Unit options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add unit below") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                onInsertBelow()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add unit above") },
                            leadingIcon = {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                onInsertAbove()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                clipboardManager.setText(AnnotatedString(MentionTokenizer.stripCardUuids(rawText)))
                                Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            SelectionContainer {
                RichMentionText(
                    rawText = rawText,
                    onCardClick = onNavigateToCard
                )
            }
        }
    }
}

@Composable
private fun BacklinkUnitCard(
    backlink: BacklinkItem,
    onNavigateToCard: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jump Source Chip
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.clickable { onNavigateToCard(backlink.sourceCardId) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "From: ${backlink.sourceCardTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { isMenuExpanded = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Extra context options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy") },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                clipboardManager.setText(AnnotatedString(MentionTokenizer.stripCardUuids(backlink.rawText)))
                                Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Open '${backlink.sourceCardTitle}'") },
                            leadingIcon = {
                                Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                isMenuExpanded = false
                                onNavigateToCard(backlink.sourceCardId)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            SelectionContainer {
                RichMentionText(
                    rawText = backlink.rawText,
                    onCardClick = onNavigateToCard
                )
            }
        }
    }
}

private fun formatCardToJson(cardWithContext: com.linkview.core.model.CardWithContext): String {
    val card = cardWithContext.card
    val cleanTags = card.allTags
    val unitTexts = cardWithContext.authoredUnits.map { it.rawText }
    val tagsJson = cleanTags.joinToString(",\n    ") { "\"$it\"" }
    val unitsJson = unitTexts.joinToString(",\n    ") {
        val escaped = it.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        "\"$escaped\""
    }
    return buildString {
        append("{\n")
        append("  \"title\": \"${card.title.replace("\"", "\\\"")}\"")
        if (cleanTags.isNotEmpty()) {
            append(",\n  \"tags\": [\n    $tagsJson\n  ]")
        } else {
            append(",\n  \"tags\": []")
        }
        if (unitTexts.isNotEmpty()) {
            append(",\n  \"units\": [\n    $unitsJson\n  ]")
        } else {
            append(",\n  \"units\": []")
        }
        append("\n}")
    }
}
