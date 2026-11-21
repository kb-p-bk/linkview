package com.linkview.domain.repository

import com.linkview.core.common.Result
import com.linkview.core.model.ImportMode
import com.linkview.core.model.ImportSummary

interface BackupRepository {
    suspend fun exportJson(): Result<String>
    suspend fun importJson(jsonContent: String, mode: ImportMode): Result<ImportSummary>
}
