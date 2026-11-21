package com.linkview.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.core.model.ImportMode
import com.linkview.domain.repository.CardRepository
import com.linkview.domain.usecase.ExportCardsUseCase
import com.linkview.domain.usecase.ImportCardsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val exportCardsUseCase: ExportCardsUseCase,
    private val importCardsUseCase: ImportCardsUseCase,
    private val cardRepository: CardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            cardRepository.observeAllTags().collect { tags ->
                _uiState.update { it.copy(availableTags = tags) }
            }
        }
    }

    fun toggleFilterTag(tag: String) {
        _uiState.update { current ->
            val updated = if (current.selectedFilterTags.contains(tag)) {
                current.selectedFilterTags - tag
            } else {
                current.selectedFilterTags + tag
            }
            current.copy(selectedFilterTags = updated)
        }
    }

    fun clearFilterTags() {
        _uiState.update { it.copy(selectedFilterTags = emptySet()) }
    }

    fun exportTitles(asJson: Boolean, onDataReady: (formattedText: String, count: Int) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            val tags = _uiState.value.selectedFilterTags.toList()
            when (val result = exportCardsUseCase.exportReferenceTitles(tags)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isExporting = false) }
                    val titles = result.data
                    val formatted = if (asJson) {
                        "[\n" + titles.joinToString(",\n") { "  \"${it.replace("\"", "\\\"")}\"" } + "\n]"
                    } else {
                        titles.joinToString("\n") { "- $it" }
                    }
                    onDataReady(formatted, titles.size)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.message ?: "Failed to export card titles."
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun exportFullBackup(onDataReady: (jsonContent: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            when (val result = exportCardsUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isExporting = false) }
                    onDataReady(result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.message ?: "Failed to generate full backup data."
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun exportHumanReadable(onDataReady: (jsonContent: String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null, statusMessage = null) }
            when (val result = exportCardsUseCase.exportHumanReadable()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isExporting = false) }
                    onDataReady(result.data)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isExporting = false,
                            errorMessage = result.message ?: "Failed to generate readable export data."
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun exportData(onDataReady: (jsonContent: String) -> Unit) = exportFullBackup(onDataReady)

    fun onExportSavedSuccess(message: String = "Backup successfully exported!") {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun onFileSelectedForImport(jsonString: String) {
        if (jsonString.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Selected file is empty.") }
            return
        }
        _uiState.update {
            it.copy(
                pendingImportJson = jsonString,
                showImportModeDialog = true,
                errorMessage = null,
                statusMessage = null
            )
        }
    }

    fun cancelImport() {
        _uiState.update {
            it.copy(
                pendingImportJson = null,
                showImportModeDialog = false
            )
        }
    }

    fun confirmImport(mode: ImportMode) {
        val json = _uiState.value.pendingImportJson ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isImporting = true,
                    showImportModeDialog = false,
                    errorMessage = null
                )
            }
            when (val result = importCardsUseCase(json, mode)) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            pendingImportJson = null,
                            importSummary = result.data,
                            statusMessage = "Import completed successfully!"
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            pendingImportJson = null,
                            errorMessage = result.message ?: "Failed to import backup file."
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun dismissSummary() {
        _uiState.update { it.copy(importSummary = null) }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun dismissStatus() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}
