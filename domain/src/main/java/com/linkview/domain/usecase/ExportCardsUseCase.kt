package com.linkview.domain.usecase

import com.linkview.core.common.Result
import com.linkview.domain.repository.BackupRepository

class ExportCardsUseCase(
    private val backupRepository: BackupRepository
) {
    suspend operator fun invoke(): Result<String> = backupRepository.exportJson()
    suspend fun exportHumanReadable(): Result<String> = backupRepository.exportHumanReadableJson()
    suspend fun exportReferenceTitles(tags: List<String> = emptyList()): Result<List<String>> =
        backupRepository.exportReferenceTitles(tags)
}
