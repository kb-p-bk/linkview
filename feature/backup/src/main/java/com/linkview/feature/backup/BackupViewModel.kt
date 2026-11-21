package com.linkview.feature.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.linkview.core.common.Result
import com.linkview.core.model.ImportMode
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
    private val importCardsUseCase: ImportCardsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    fun exportData(onDataReady: (jsonContent: String) -> Unit) {
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
                            errorMessage = result.message ?: "Failed to generate export data."
                        )
                    }
                }
                is Result.Loading -> Unit
            }
        }
    }

    fun onExportSavedSuccess() {
        _uiState.update { it.copy(statusMessage = "Backup successfully exported!") }
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
