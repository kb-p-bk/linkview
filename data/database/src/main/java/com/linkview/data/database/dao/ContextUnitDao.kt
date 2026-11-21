package com.linkview.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.linkview.data.database.entity.ContextUnitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContextUnitDao {
    @Query("SELECT * FROM context_units WHERE sourceCardId = :cardId ORDER BY sortOrder ASC")
    fun observeUnitsForCard(cardId: String): Flow<List<ContextUnitEntity>>

    @Query("SELECT * FROM context_units WHERE sourceCardId = :cardId ORDER BY sortOrder ASC")
    suspend fun getUnitsForCard(cardId: String): List<ContextUnitEntity>

    @Query("SELECT * FROM context_units WHERE plainText LIKE '%' || :query || '%' COLLATE NOCASE")
    fun searchUnitsByText(query: String): Flow<List<ContextUnitEntity>>

    @Upsert
    suspend fun insertUnits(units: List<ContextUnitEntity>)

    @Query("DELETE FROM context_units WHERE sourceCardId = :cardId")
    suspend fun deleteUnitsForCard(cardId: String)

    @Query("DELETE FROM context_units WHERE id IN (:unitIds)")
    suspend fun deleteUnitsByIds(unitIds: List<String>)

    @Query("SELECT * FROM context_units WHERE rawText LIKE '%card:' || :cardId || '%'")
    suspend fun getUnitsReferencingCard(cardId: String): List<ContextUnitEntity>
}
