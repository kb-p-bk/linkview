package com.linkview.data.repository

import com.linkview.core.common.DispatcherProvider
import com.linkview.core.common.Result
import com.linkview.core.common.runCatchingResult
import com.linkview.core.model.CardCollection
import com.linkview.data.database.dao.CollectionDao
import com.linkview.data.database.entity.CollectionEntity
import com.linkview.domain.repository.CollectionRepository
import com.linkview.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CollectionRepositoryImpl @Inject constructor(
    private val collectionDao: CollectionDao,
    private val preferencesRepository: PreferencesRepository,
    private val dispatchers: DispatcherProvider
) : CollectionRepository {

    override fun observeAllCollections(): Flow<List<CardCollection>> {
        return collectionDao.observeAllCollections().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override fun observeActiveCollectionId(): Flow<String> {
        return preferencesRepository.observeActiveCollectionId()
    }

    override suspend fun getActiveCollectionId(): String {
        return preferencesRepository.getActiveCollectionId()
    }

    override suspend fun setActiveCollectionId(id: String) {
        preferencesRepository.setActiveCollectionId(id)
    }

    override suspend fun createCollection(name: String): Result<CardCollection> = withContext(dispatchers.io) {
        runCatchingResult {
            val cleanName = name.trim()
            require(cleanName.isNotBlank()) { "Collection name cannot be blank." }

            val existing = collectionDao.getCollectionByName(cleanName)
            require(existing == null) { "A collection named '$cleanName' already exists." }

            val now = System.currentTimeMillis()
            val newId = UUID.randomUUID().toString()
            val collection = CardCollection(
                id = newId,
                name = cleanName,
                createdAt = now,
                updatedAt = now
            )
            collectionDao.upsertCollection(CollectionEntity.fromDomain(collection))
            setActiveCollectionId(newId)
            collection
        }
    }

    override suspend fun renameCollection(id: String, newName: String): Result<Unit> = withContext(dispatchers.io) {
        runCatchingResult {
            val cleanName = newName.trim()
            require(cleanName.isNotBlank()) { "Collection name cannot be blank." }
            require(id != CardCollection.DEFAULT_ID) { "Default collection cannot be renamed." }

            val existing = collectionDao.getCollectionById(id)
            requireNotNull(existing) { "Collection not found." }

            val duplicate = collectionDao.getCollectionByName(cleanName)
            require(duplicate == null || duplicate.id == id) { "A collection named '$cleanName' already exists." }

            val updated = existing.copy(
                name = cleanName,
                updatedAt = System.currentTimeMillis()
            )
            collectionDao.upsertCollection(updated)
        }
    }

    override suspend fun deleteCollection(id: String): Result<Unit> = withContext(dispatchers.io) {
        runCatchingResult {
            require(id != CardCollection.DEFAULT_ID) { "Default collection cannot be deleted." }
            val currentActive = preferencesRepository.getActiveCollectionId()
            if (currentActive == id) {
                preferencesRepository.setActiveCollectionId(CardCollection.DEFAULT_ID)
            }
            collectionDao.deleteCollection(id)
        }
    }
}
