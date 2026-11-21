package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.core.model.ImportMode
import com.linkview.core.model.ImportSummary
import com.linkview.domain.repository.BackupRepository

class ImportCardsUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(jsonContent: String, mode: ImportMode): Result<ImportSummary> {
        return backupRepository.importJson(jsonContent, mode)
    }
}
