package com.linkview.domain.repository

import com.linkview.core.model.Card
import com.linkview.core.model.CardWithContext
import com.linkview.core.model.ContextUnit
import kotlinx.coroutines.flow.Flow

interface CardRepository {
    fun observeAllCards(): Flow<List<Card>>
    fun observeCardsByTags(tags: List<String>): Flow<List<Card>>
    fun observeAllTags(): Flow<List<String>>
    fun searchTags(query: String): Flow<List<String>>
    fun observeCardWithContext(cardId: String): Flow<CardWithContext?>
    suspend fun getCardWithContext(cardId: String): CardWithContext?
    suspend fun getCardById(cardId: String): Card?
    suspend fun getCardByTitle(title: String): Card?
    suspend fun saveCard(card: Card, units: List<ContextUnit>)
    suspend fun deleteCard(cardId: String)
    suspend fun getIncomingBacklinkCount(targetCardId: String): Int
    suspend fun autocompleteCards(query: String, excludeCardId: String? = null, limit: Int = 10): List<Card>
    suspend fun getCardCount(): Int
}
