package com.linkview.feature.backup

import com.linkview.core.model.ImportSummary

data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val pendingImportJson: String? = null,
    val showImportModeDialog: Boolean = false,
    val importSummary: ImportSummary? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val availableTags: List<String> = emptyList(),
    val selectedFilterTags: Set<String> = emptySet()
)
