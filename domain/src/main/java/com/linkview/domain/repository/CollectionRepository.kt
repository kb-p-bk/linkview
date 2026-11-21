package com.linkview.domain.repository

import com.linkview.core.common.Result
import com.linkview.core.model.CardCollection
import kotlinx.coroutines.flow.Flow

interface CollectionRepository {
    fun observeAllCollections(): Flow<List<CardCollection>>
    fun observeActiveCollectionId(): Flow<String>
    suspend fun getActiveCollectionId(): String
    suspend fun setActiveCollectionId(id: String)
    suspend fun createCollection(name: String): Result<CardCollection>
    suspend fun renameCollection(id: String, newName: String): Result<Unit>
    suspend fun deleteCollection(id: String): Result<Unit>
}
