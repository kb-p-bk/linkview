package com.linkview.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.CardTagEntity
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

    // Tag operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCardTags(tags: List<CardTagEntity>)

    @Query("DELETE FROM card_tags WHERE cardId = :cardId")
    suspend fun deleteCardTagsForCard(cardId: String)

    @Query("""
        SELECT tag FROM (
            SELECT ct.tag AS tag, MAX(c.updatedAt) AS maxUpdated, COUNT(ct.cardId) AS cardCount
            FROM card_tags ct
            JOIN cards c ON ct.cardId = c.id
            GROUP BY ct.tag
            UNION
            SELECT type AS tag, updatedAt AS maxUpdated, 1 AS cardCount
            FROM cards
            WHERE type IS NOT NULL AND type != ''
        )
        GROUP BY tag
        ORDER BY MAX(maxUpdated) DESC, SUM(cardCount) DESC, tag COLLATE NOCASE ASC
    """)
    fun observeAllTags(): Flow<List<String>>

    @Query("""
        SELECT tag FROM (
            SELECT ct.tag AS tag, MAX(c.updatedAt) AS maxUpdated, COUNT(ct.cardId) AS cardCount
            FROM card_tags ct
            JOIN cards c ON ct.cardId = c.id
            WHERE ct.tag LIKE '%' || :query || '%' COLLATE NOCASE
            GROUP BY ct.tag
            UNION
            SELECT type AS tag, updatedAt AS maxUpdated, 1 AS cardCount
            FROM cards
            WHERE type IS NOT NULL AND type != '' AND type LIKE '%' || :query || '%' COLLATE NOCASE
        )
        GROUP BY tag
        ORDER BY 
            CASE 
                WHEN LOWER(tag) = LOWER(:query) THEN 0
                WHEN LOWER(tag) LIKE LOWER(:query) || '%' THEN 1
                ELSE 2 
            END,
            MAX(maxUpdated) DESC, 
            SUM(cardCount) DESC, 
            tag COLLATE NOCASE ASC
        LIMIT 20
    """)
    fun searchTags(query: String): Flow<List<String>>

    @Query("SELECT tag FROM card_tags WHERE cardId = :cardId")
    suspend fun getTagsForCard(cardId: String): List<String>

    @Query("""
        SELECT * FROM cards 
        WHERE id IN (
            SELECT cardId FROM card_tags 
            WHERE tag IN (:tags) 
            GROUP BY cardId 
            HAVING COUNT(DISTINCT tag) = :tagCount
        ) 
        ORDER BY updatedAt DESC
    """)
    fun observeCardsByTags(tags: List<String>, tagCount: Int): Flow<List<CardEntity>>
}
