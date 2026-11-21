package com.linkview.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linkview.data.database.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {
    @Query("SELECT * FROM cards ORDER BY updatedAt DESC")
    fun observeAllCards(): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    fun observeCardById(id: String): Flow<CardEntity?>

    @Query("SELECT * FROM cards WHERE id = :id LIMIT 1")
    suspend fun getCardById(id: String): CardEntity?

    @Query("SELECT * FROM cards WHERE title = :title COLLATE NOCASE LIMIT 1")
    suspend fun getCardByExactTitle(title: String): CardEntity?

    @Query("SELECT * FROM cards WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title ASC")
    fun searchCardsByTitle(query: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE title LIKE '%' || :query || '%' COLLATE NOCASE AND id != :excludeCardId ORDER BY title ASC LIMIT :limit")
    suspend fun autocompleteCards(query: String, excludeCardId: String, limit: Int): List<CardEntity>

    @Query("SELECT * FROM cards WHERE title LIKE '%' || :query || '%' COLLATE NOCASE ORDER BY title ASC LIMIT :limit")
    suspend fun autocompleteCardsAll(query: String, limit: Int): List<CardEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCard(card: CardEntity)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun deleteCardById(id: String)

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    @Query("SELECT * FROM cards ORDER BY createdAt ASC")
    suspend fun getAllCardsList(): List<CardEntity>

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()
}
