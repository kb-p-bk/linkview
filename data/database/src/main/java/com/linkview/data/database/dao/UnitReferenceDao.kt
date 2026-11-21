package com.linkview.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linkview.data.database.entity.BacklinkQueryResult
import com.linkview.data.database.entity.UnitReferenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UnitReferenceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertReferences(references: List<UnitReferenceEntity>)

    @Query("DELETE FROM unit_references WHERE unitId = :unitId")
    suspend fun deleteReferencesForUnit(unitId: String)

    @Query("DELETE FROM unit_references WHERE unitId IN (:unitIds)")
    suspend fun deleteReferencesForUnits(unitIds: List<String>)

    @Query("""
        SELECT 
            cu.id AS unitId,
            cu.sourceCardId AS sourceCardId,
            c.title AS sourceCardTitle,
            cu.rawText AS rawText,
            cu.plainText AS plainText,
            cu.sortOrder AS sortOrder
        FROM unit_references ur
        INNER JOIN context_units cu ON ur.unitId = cu.id
        INNER JOIN cards c ON cu.sourceCardId = c.id
        WHERE ur.targetCardId = :targetCardId
          AND cu.sourceCardId != :targetCardId
        ORDER BY c.title ASC, cu.sortOrder ASC
    """)
    fun observeIncomingBacklinks(targetCardId: String): Flow<List<BacklinkQueryResult>>

    @Query("""
        SELECT 
            cu.id AS unitId,
            cu.sourceCardId AS sourceCardId,
            c.title AS sourceCardTitle,
            cu.rawText AS rawText,
            cu.plainText AS plainText,
            cu.sortOrder AS sortOrder
        FROM unit_references ur
        INNER JOIN context_units cu ON ur.unitId = cu.id
        INNER JOIN cards c ON cu.sourceCardId = c.id
        WHERE ur.targetCardId = :targetCardId
          AND cu.sourceCardId != :targetCardId
        ORDER BY c.title ASC, cu.sortOrder ASC
    """)
    suspend fun getIncomingBacklinks(targetCardId: String): List<BacklinkQueryResult>

    @Query("""
        SELECT COUNT(*)
        FROM unit_references ur
        INNER JOIN context_units cu ON ur.unitId = cu.id
        WHERE ur.targetCardId = :targetCardId
          AND cu.sourceCardId != :targetCardId
    """)
    suspend fun countIncomingBacklinks(targetCardId: String): Int
}
