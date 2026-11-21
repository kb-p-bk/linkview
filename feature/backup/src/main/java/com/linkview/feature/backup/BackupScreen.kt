package com.linkview.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.linkview.core.ui.components.CompactSearchBar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.linkview.core.model.ImportMode
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    var pendingExportSuccessMsg by remember { mutableStateOf("Backup successfully exported!") }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedJsonText by remember { mutableStateOf("") }
    var tagSearchQuery by remember { mutableStateOf("") }
    var isTagDropdownExpanded by remember { mutableStateOf(false) }

    // SAF File Creator for Export
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
                }
                viewModel.onExportSavedSuccess(pendingExportSuccessMsg)
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                pendingExportJson = null
            }
        }
    }

    // SAF File Picker for Import
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                    val content = reader.readText()
                    viewModel.onFileSelectedForImport(content)
                }
            } catch (e: Exception) {
                // Handled in VM
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Backup & Portability") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                text = "Export or Import your entire knowledge base in standard, human-readable JSON.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Status message banner
            uiState.statusMessage?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissStatus() }) {
                            Text("✕")
                        }
                    }
                }
            }

            // Error message banner
            uiState.errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.dismissError() }) {
                            Text("✕", color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Human-Readable Export Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Readable Cards (JSON)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Clean format",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clean, human-readable JSON array of cards with titles, tags, and context units. Free of database UUIDs, timestamps, and schema wrappers. Perfect for reading, sharing, external edits, and scripting.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportHumanReadable { jsonString ->
                                    pendingExportJson = jsonString
                                    pendingExportSuccessMsg = "Readable cards successfully saved!"
                                    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                    exportFileLauncher.launch("linkview-cards-readable-$dateStr.json")
                                }
                            },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Save Readable JSON")
                        }

                        FilledTonalButton(
                            onClick = {
                                viewModel.exportHumanReadable { jsonString ->
                                    clipboardManager.setText(AnnotatedString(jsonString))
                                    viewModel.onExportSavedSuccess("Readable cards JSON copied to clipboard!")
                                }
                            },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy JSON")
                        }
                    }
                }
            }

            // Full System Backup Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Full System Backup (JSON)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Complete database archive including internal UUIDs, context unit ordering, timestamps, and schema versioning. Use this format for lossless backup and full restoration on another device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.exportFullBackup { jsonString ->
                                pendingExportJson = jsonString
                                pendingExportSuccessMsg = "Full system backup successfully saved!"
                                val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                                exportFileLauncher.launch("linkview-full-backup-$dateStr.json")
                            }
                        },
                        enabled = !uiState.isExporting && !uiState.isImporting
                    ) {
                        if (uiState.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Export System Backup")
                    }
                }
            }

            // Reference Titles Index Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reference Titles Index",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Reference Index",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Export a lightweight list of existing card titles for all or selected tags. Useful as a canonical index of names for external cross-referencing and writing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (uiState.availableTags.isNotEmpty()) {
                        Text(
                            text = if (uiState.selectedFilterTags.isEmpty()) "Filter by tag (All cards selected):" else "Filtered tags (${uiState.selectedFilterTags.size} active):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Tag Search Input with Autocomplete Dropdown
                            Box {
                                CompactSearchBar(
                                    value = tagSearchQuery,
                                    onValueChange = {
                                        tagSearchQuery = it
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
                                        if (tagSearchQuery.isNotEmpty()) {
                                            IconButton(
                                                onClick = {
                                                    tagSearchQuery = ""
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

                                val cleanQuery = tagSearchQuery.trim()
                                val matchingTags = uiState.availableTags.filter { it.contains(cleanQuery, ignoreCase = true) }

                                DropdownMenu(
                                    expanded = isTagDropdownExpanded && matchingTags.isNotEmpty(),
                                    onDismissRequest = { isTagDropdownExpanded = false },
                                    properties = PopupProperties(focusable = false)
                                ) {
                                    matchingTags.take(8).forEach { tag ->
                                        val isSelected = uiState.selectedFilterTags.contains(tag)
                                        DropdownMenuItem(
                                            text = { Text(tag) },
                                            leadingIcon = if (isSelected) {
                                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null,
                                            onClick = {
                                                viewModel.toggleFilterTag(tag)
                                                tagSearchQuery = ""
                                                isTagDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Scrollable Tag Filter Chips in a single clean row
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = uiState.selectedFilterTags.isEmpty(),
                                    onClick = { viewModel.clearFilterTags() },
                                    label = { Text("All") }
                                )

                                val displayCandidateTags = if (tagSearchQuery.isBlank()) {
                                    (uiState.selectedFilterTags + uiState.availableTags).distinct()
                                } else {
                                    (uiState.selectedFilterTags + uiState.availableTags.filter { it.contains(tagSearchQuery.trim(), ignoreCase = true) }).distinct()
                                }

                                displayCandidateTags.forEach { tag ->
                                    val isSelected = uiState.selectedFilterTags.contains(tag)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.toggleFilterTag(tag) },
                                        label = { Text(tag) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportTitles(asJson = false) { text, count ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    viewModel.onExportSavedSuccess("Copied $count card titles (Markdown list) to clipboard!")
                                }
                            },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy List")
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.exportTitles(asJson = true) { text, count ->
                                    clipboardManager.setText(AnnotatedString(text))
                                    viewModel.onExportSavedSuccess("Copied $count card titles (JSON array) to clipboard!")
                                }
                            },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Copy JSON")
                        }
                    }
                }
            }

            // Import Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Import Backup or Cards",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Restore data from a JSON file or paste JSON directly. Automatically recognizes Full System Backups, single cards, or Readable Card arrays.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                importFileLauncher.launch(arrayOf("application/json", "text/*"))
                            },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            } else {
                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Import File")
                        }

                        Button(
                            onClick = { showPasteDialog = true },
                            enabled = !uiState.isExporting && !uiState.isImporting
                        ) {
                            Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Paste JSON")
                        }
                    }
                }
            }
        }

        // Paste JSON Dialog
        if (showPasteDialog) {
            AlertDialog(
                onDismissRequest = { showPasteDialog = false },
                title = { Text("Paste Card or Backup JSON") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Paste a single card, an array of cards, or backup JSON. Missing IDs and backlinks will be auto-generated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = pastedJsonText,
                            onValueChange = { pastedJsonText = it },
                            placeholder = { Text("{\n  \"title\": \"Card Title\",\n  \"tags\": [\"Tag\"],\n  \"units\": [\"Notes with @[Mentions]\"]\n}") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pastedJsonText.isNotBlank()) {
                                viewModel.onFileSelectedForImport(pastedJsonText)
                                showPasteDialog = false
                                pastedJsonText = ""
                            }
                        },
                        enabled = pastedJsonText.isNotBlank()
                    ) {
                        Text("Import / Merge")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Import Mode Decision Dialog (Merge vs Replace All)
        if (uiState.showImportModeDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.cancelImport() },
                title = { Text("Choose Import Mode") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("How would you like to import this backup file?")
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("1. Merge / Upsert", fontWeight = FontWeight.Bold)
                                Text("Adds new cards and updates matching existing cards without deleting anything else.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("2. Replace All", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text("Wipes current database tables and restores purely from the imported file.", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.confirmImport(ImportMode.MERGE) }) {
                        Text("Merge")
                    }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = { viewModel.confirmImport(ImportMode.REPLACE_ALL) }) {
                            Text("Replace All", color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { viewModel.cancelImport() }) {
                            Text("Cancel")
                        }
                    }
                }
            )
        }

        // Import Summary Dialog
        uiState.importSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                title = { Text("Import Summary") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Backup successfully imported and backlinks synchronized!")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("• Cards imported: ${summary.cardsImported}")
                        Text("• Context units imported: ${summary.unitsImported}")
                        Text("• Cross-references indexed: ${summary.referencesCreated}")
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.dismissSummary() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}
