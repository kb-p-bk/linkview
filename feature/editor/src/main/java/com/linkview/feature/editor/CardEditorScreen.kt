package com.linkview.feature.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkview.core.model.Card
import com.linkview.core.model.EditableUnit
import com.linkview.core.ui.components.ContextUnitEditorRow

private val DEFAULT_CARD_TYPES = listOf("Person", "Event", "Place", "Institution", "Concept")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    cardId: String? = null,
    onNavigateBack: () -> Unit,
    onOpenExistingCard: (String) -> Unit = {},
    viewModel: CardEditorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var isTagDropdownOpen by remember { mutableStateOf(false) }

    DisposableEffect(cardId) {
        viewModel.loadCard(cardId)
        onDispose {
            viewModel.resetState()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.resetSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.cardId == null) "New Card" else "Edit Card")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(end = 16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Button(
                            onClick = { viewModel.save() },
                            enabled = !uiState.isSaving && uiState.title.isNotBlank() && uiState.duplicateCard == null,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading && uiState.title.isEmpty() && uiState.cardId != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        var headerTopInScroll by remember { mutableFloatStateOf(0f) }
        val isHeaderOutOfView by remember {
            derivedStateOf {
                headerTopInScroll > 0f && scrollState.value > headerTopInScroll
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Duplicate warning banner
            AnimatedVisibility(visible = uiState.duplicateCard != null) {
                uiState.duplicateCard?.let { existing ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "A card titled '${existing.title}' already exists.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            TextButton(onClick = { onOpenExistingCard(existing.id) }) {
                                Text("Open", color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                }
            }

            // Title input
            OutlinedTextField(
                value = uiState.title,
                onValueChange = { viewModel.onTitleChanged(it) },
                placeholder = { Text("Card title") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = (uiState.errorMessage != null && uiState.title.isBlank()) || uiState.duplicateCard != null
            )

            // Multi-Tag Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Tags (${uiState.tags.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Selected Tag Chips (Reorderable with long press and movement)
                if (uiState.tags.isNotEmpty()) {
                    ReorderableTagChips(
                        tags = uiState.tags,
                        onRemoveTag = { viewModel.removeTag(it) },
                        onMoveTag = { from, to -> viewModel.moveTag(from, to) }
                    )
                }

                // Inline Tag Input with Autocomplete
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.tagInputText,
                        onValueChange = {
                            viewModel.onTagInputChanged(it)
                            isTagDropdownOpen = it.isNotBlank()
                        },
                        placeholder = { Text("Filter tag...", maxLines = 1) },
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (uiState.tagInputText.isNotBlank()) {
                                    viewModel.addTag(uiState.tagInputText)
                                    isTagDropdownOpen = false
                                }
                            }
                        ),
                        trailingIcon = {
                            if (uiState.tagInputText.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onTagInputChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear tag input", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    val inputQuery = uiState.tagInputText.trim()
                    val matchingExisting = uiState.allExistingTags
                        .filter { it.contains(inputQuery, ignoreCase = true) && !uiState.tags.any { t -> t.equals(it, ignoreCase = true) } }
                    val isNewTag = inputQuery.isNotBlank() && !uiState.allExistingTags.any { it.equals(inputQuery, ignoreCase = true) }

                    DropdownMenu(
                        expanded = isTagDropdownOpen && (matchingExisting.isNotEmpty() || isNewTag),
                        onDismissRequest = { isTagDropdownOpen = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        matchingExisting.take(8).forEach { tag ->
                            DropdownMenuItem(
                                text = { Text(tag) },
                                onClick = {
                                    viewModel.addTag(tag)
                                    isTagDropdownOpen = false
                                }
                            )
                        }
                        if (isNewTag) {
                            DropdownMenuItem(
                                text = { Text("\"$inputQuery\"") },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                onClick = {
                                    viewModel.addTag(inputQuery)
                                    isTagDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                // Quick Tag Suggestions
                val suggestedTags = (DEFAULT_CARD_TYPES + uiState.allExistingTags).distinct().filterNot { tag ->
                    uiState.tags.any { it.equals(tag, ignoreCase = true) }
                }
                if (suggestedTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        suggestedTags.forEach { presetType ->
                            FilterChip(
                                selected = false,
                                onClick = { viewModel.addTag(presetType) },
                                label = { Text("+ $presetType") }
                            )
                        }
                    }
                }
            }

            // Context Units In-line Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        headerTopInScroll = coordinates.positionInParent().y
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Context Units (${uiState.units.size})",
                    style = MaterialTheme.typography.titleMedium
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    IconButton(
                        onClick = {
                            viewModel.addContextUnit()
                            coroutineScope.launch {
                                scrollState.animateScrollTo(scrollState.maxValue)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add context unit",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Context Units inputs
            uiState.units.forEachIndexed { index, unit ->
                key(unit.id) {
                    ContextUnitEditorRow(
                        index = index,
                        unit = unit,
                        totalUnits = uiState.units.size,
                        isMentionActive = uiState.activeUnitIndex == index,
                        mentionQuery = uiState.activeMentionQuery,
                        suggestions = if (uiState.activeUnitIndex == index) uiState.mentionSuggestions else emptyList(),
                        onTextChanged = { text, cursor ->
                            viewModel.onUnitTextChanged(index, text, cursor)
                        },
                        onMoveUp = { viewModel.moveContextUnit(index, index - 1) },
                        onMoveDown = { viewModel.moveContextUnit(index, index + 1) },
                        onInsertBelow = { viewModel.insertContextUnitAfter(index) },
                        onDelete = { viewModel.removeContextUnit(index) },
                        onSelectMention = { card -> viewModel.selectMention(card) },
                        onQuickCreateMention = { title -> viewModel.quickCreateAndSelectMention(title) }
                    )
                }
            }

            // Additional bottom button to quickly add context unit at the end
            OutlinedButton(
                onClick = {
                    viewModel.addContextUnit()
                    coroutineScope.launch {
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Context Unit")
            }

            Spacer(modifier = Modifier.height(48.dp))
        }

        // Sticky Header when in-line header is scrolled out of view
        AnimatedVisibility(
            visible = isHeaderOutOfView,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context Units (${uiState.units.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.addContextUnit()
                                coroutineScope.launch {
                                    scrollState.animateScrollTo(scrollState.maxValue)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add context unit",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun ReorderableTagChips(
    tags: List<String>,
    onRemoveTag: (String) -> Unit,
    onMoveTag: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val itemWidths = remember { mutableMapOf<Int, Int>() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEachIndexed { index, tag ->
            val isBeingDragged = draggedIndex == index
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isBeingDragged) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shadowElevation = if (isBeingDragged) 8.dp else 0.dp,
                modifier = Modifier
                    .zIndex(if (isBeingDragged) 10f else 1f)
                    .graphicsLayer {
                        if (isBeingDragged) {
                            translationX = dragOffset
                            scaleX = 1.08f
                            scaleY = 1.08f
                        }
                    }
                    .onGloballyPositioned { coordinates ->
                        itemWidths[index] = coordinates.size.width
                    }
                    .pointerInput(tags, index) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedIndex = index
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val currentIdx = draggedIndex ?: return@detectDragGesturesAfterLongPress
                                dragOffset += dragAmount.x

                                val currentWidth = itemWidths[currentIdx] ?: 100
                                if (dragOffset > 0 && currentIdx < tags.size - 1) {
                                    val nextWidth = itemWidths[currentIdx + 1] ?: currentWidth
                                    val threshold = nextWidth * 0.5f
                                    if (dragOffset > threshold) {
                                        onMoveTag(currentIdx, currentIdx + 1)
                                        draggedIndex = currentIdx + 1
                                        dragOffset -= nextWidth
                                    }
                                } else if (dragOffset < 0 && currentIdx > 0) {
                                    val prevWidth = itemWidths[currentIdx - 1] ?: currentWidth
                                    val threshold = -(prevWidth * 0.5f)
                                    if (dragOffset < threshold) {
                                        onMoveTag(currentIdx, currentIdx - 1)
                                        draggedIndex = currentIdx - 1
                                        dragOffset += prevWidth
                                    }
                                }
                            },
                            onDragEnd = {
                                draggedIndex = null
                                dragOffset = 0f
                            },
                            onDragCancel = {
                                draggedIndex = null
                                dragOffset = 0f
                            }
                        )
                    }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isBeingDragged) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    IconButton(
                        onClick = { onRemoveTag(tag) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove tag",
                            tint = if (isBeingDragged) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}


