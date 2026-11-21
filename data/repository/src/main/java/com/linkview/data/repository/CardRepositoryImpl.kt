package com.linkview.data.repository

import androidx.room.withTransaction
import com.linkview.core.common.DispatcherProvider
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.model.BacklinkItem
import com.linkview.core.model.Card
import com.linkview.core.model.CardWithContext
import com.linkview.core.model.ContextUnit
import com.linkview.data.database.LinkViewDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.ContextUnitEntity
import com.linkview.data.database.entity.UnitReferenceEntity
import com.linkview.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardRepositoryImpl @Inject constructor(
    private val database: LinkViewDatabase,
    private val cardDao: CardDao,
    private val contextUnitDao: ContextUnitDao,
    private val unitReferenceDao: UnitReferenceDao,
    private val dispatchers: DispatcherProvider
) : CardRepository {

    override fun observeAllCards(): Flow<List<Card>> {
        return cardDao.observeAllCards().map { entities ->
            entities.map { it.toDomain() }
        }.flowOn(dispatchers.io)
    }

    override fun observeCardWithContext(cardId: String): Flow<CardWithContext?> {
        return combine(
            cardDao.observeCardById(cardId),
            contextUnitDao.observeUnitsForCard(cardId),
            unitReferenceDao.observeIncomingBacklinks(cardId)
        ) { cardEntity, unitEntities, backlinkEntities ->
            if (cardEntity == null) return@combine null

            CardWithContext(
                card = cardEntity.toDomain(),
                authoredUnits = unitEntities.map { it.toDomain() },
                incomingBacklinks = backlinkEntities.map {
                    BacklinkItem(
                        unitId = it.unitId,
                        sourceCardId = it.sourceCardId,
                        sourceCardTitle = it.sourceCardTitle,
                        rawText = it.rawText,
                        plainText = it.plainText,
                        sortOrder = it.sortOrder
                    )
                }
            )
        }.flowOn(dispatchers.io)
    }

    override suspend fun getCardWithContext(cardId: String): CardWithContext? = withContext(dispatchers.io) {
        val card = cardDao.getCardById(cardId) ?: return@withContext null
        val units = contextUnitDao.getUnitsForCard(cardId)
        val backlinks = unitReferenceDao.getIncomingBacklinks(cardId)

        CardWithContext(
            card = card.toDomain(),
            authoredUnits = units.map { it.toDomain() },
            incomingBacklinks = backlinks.map {
                BacklinkItem(
                    unitId = it.unitId,
                    sourceCardId = it.sourceCardId,
                    sourceCardTitle = it.sourceCardTitle,
                    rawText = it.rawText,
                    plainText = it.plainText,
                    sortOrder = it.sortOrder
                )
            }
        )
    }

    override suspend fun getCardById(cardId: String): Card? = withContext(dispatchers.io) {
        cardDao.getCardById(cardId)?.toDomain()
    }

    override suspend fun getCardByTitle(title: String): Card? = withContext(dispatchers.io) {
        cardDao.getCardByExactTitle(title)?.toDomain()
    }

    override suspend fun saveCard(card: Card, units: List<ContextUnit>): Unit = withContext(dispatchers.io) {
        database.withTransaction {
            val existingCard = cardDao.getCardById(card.id)

            // If the card title was renamed, propagate title change to other cards' mentioning units
            if (existingCard != null && !existingCard.title.equals(card.title, ignoreCase = false)) {
                val referencingUnits = contextUnitDao.getUnitsReferencingCard(card.id)
                if (referencingUnits.isNotEmpty()) {
                    val updatedUnits = referencingUnits.map { unitEntity ->
                        val updatedRaw = MentionTokenizer.updateMentionLabels(unitEntity.rawText) { id ->
                            if (id == card.id) card.title else null
                        }
                        val updatedPlain = MentionTokenizer.toPlainText(updatedRaw)
                        unitEntity.copy(rawText = updatedRaw, plainText = updatedPlain)
                    }
                    contextUnitDao.insertUnits(updatedUnits)
                }
            }

            // Upsert card
            val cardEntity = CardEntity(
                id = card.id,
                title = card.title.trim(),
                type = card.type?.trim()?.ifBlank { null },
                createdAt = card.createdAt,
                updatedAt = System.currentTimeMillis()
            )
            cardDao.upsertCard(cardEntity)

            // Delete previous units for this card
            contextUnitDao.deleteUnitsForCard(card.id)

            // Prepare and insert updated context units
            if (units.isNotEmpty()) {
                val unitEntities = units.mapIndexed { index, unit ->
                    val plain = MentionTokenizer.toPlainText(unit.rawText)
                    ContextUnitEntity(
                        id = unit.id,
                        sourceCardId = card.id,
                        sortOrder = index,
                        rawText = unit.rawText,
                        plainText = plain
                    )
                }
                contextUnitDao.insertUnits(unitEntities)

                // Build unit_references junction links
                val references = mutableListOf<UnitReferenceEntity>()
                for (unit in unitEntities) {
                    val targetIds = MentionTokenizer.extractReferences(unit.rawText)
                    for (targetId in targetIds) {
                        references.add(UnitReferenceEntity(unitId = unit.id, targetCardId = targetId))
                    }
                }
                if (references.isNotEmpty()) {
                    unitReferenceDao.insertReferences(references)
                }
            }
        }
    }

    override suspend fun deleteCard(cardId: String): Unit = withContext(dispatchers.io) {
        cardDao.deleteCardById(cardId)
    }

    override suspend fun getIncomingBacklinkCount(targetCardId: String): Int = withContext(dispatchers.io) {
        unitReferenceDao.countIncomingBacklinks(targetCardId)
    }

    override suspend fun autocompleteCards(
        query: String,
        excludeCardId: String?,
        limit: Int
    ): List<Card> = withContext(dispatchers.io) {
        val trimmed = query.trim()
        val entities = if (excludeCardId != null) {
            cardDao.autocompleteCards(trimmed, excludeCardId, limit)
        } else {
            cardDao.autocompleteCardsAll(trimmed, limit)
        }
        entities.map { it.toDomain() }
    }

    override suspend fun getCardCount(): Int = withContext(dispatchers.io) {
        cardDao.getCardCount()
    }

    private fun CardEntity.toDomain(): Card {
        return Card(
            id = id,
            title = title,
            type = type,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private fun ContextUnitEntity.toDomain(): ContextUnit {
        return ContextUnit(
            id = id,
            sourceCardId = sourceCardId,
            sortOrder = sortOrder,
            rawText = rawText,
            plainText = plainText
        )
    }
}
