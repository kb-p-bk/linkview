package com.linkview.data.repository

import com.linkview.core.common.DispatcherProvider
import com.linkview.core.model.Card
import com.linkview.core.model.SearchResultItem
import com.linkview.core.model.SearchScope
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.ContextUnitEntity
import com.linkview.domain.repository.SearchRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val cardDao: CardDao,
    private val contextUnitDao: ContextUnitDao,
    private val unitReferenceDao: UnitReferenceDao,
    private val dispatchers: DispatcherProvider
) : SearchRepository {

    override fun search(query: String, scope: SearchScope): Flow<List<SearchResultItem>> = flow {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            // Empty query: return all cards ordered by most recently updated
            val allCards = cardDao.observeAllCards()
            emit(emptyList())
            return@flow
        }

        val results = when (scope) {
            SearchScope.TITLE_ONLY -> searchTitleOnly(trimmed)
            SearchScope.CONTEXT_ONLY -> searchContextOnly(trimmed)
            SearchScope.TITLE_AND_CONTEXT -> searchTitleAndContext(trimmed)
        }
        emit(results)
    }.flowOn(dispatchers.io)

    private suspend fun searchTitleOnly(query: String): List<SearchResultItem> {
        val cardEntities = cardDao.autocompleteCardsAll(query, limit = 100)
        return cardEntities.map { cardEntity ->
            val incomingCount = unitReferenceDao.countIncomingBacklinks(cardEntity.id)
            val authoredUnits = contextUnitDao.getUnitsForCard(cardEntity.id)
            SearchResultItem(
                card = cardEntity.toDomain(),
                authoredUnitCount = authoredUnits.size,
                incomingBacklinkCount = incomingCount
            )
        }
    }

    private suspend fun searchContextOnly(query: String): List<SearchResultItem> {
        val matchingUnits = searchUnitsMatchingText(query)
        val groupedBySource = matchingUnits.groupBy { it.sourceCardId }

        val results = mutableListOf<SearchResultItem>()
        for ((sourceCardId, units) in groupedBySource) {
            val cardEntity = cardDao.getCardById(sourceCardId) ?: continue
            val snippets = units.map { it.plainText }
            val incomingCount = unitReferenceDao.countIncomingBacklinks(sourceCardId)
            val allAuthored = contextUnitDao.getUnitsForCard(sourceCardId)

            results.add(
                SearchResultItem(
                    card = cardEntity.toDomain(),
                    matchingSnippets = snippets,
                    authoredUnitCount = allAuthored.size,
                    incomingBacklinkCount = incomingCount
                )
            )
        }
        return results
    }

    private suspend fun searchTitleAndContext(query: String): List<SearchResultItem> {
        val titleMatches = cardDao.autocompleteCardsAll(query, limit = 100)
        val matchingUnits = searchUnitsMatchingText(query)
        val unitsBySource = matchingUnits.groupBy { it.sourceCardId }

        val resultMap = LinkedHashMap<String, SearchResultItem>()

        // 1. Process title matches first
        for (cardEntity in titleMatches) {
            val snippets = unitsBySource[cardEntity.id]?.map { it.plainText } ?: emptyList()
            val incomingCount = unitReferenceDao.countIncomingBacklinks(cardEntity.id)
            val allAuthored = contextUnitDao.getUnitsForCard(cardEntity.id)

            resultMap[cardEntity.id] = SearchResultItem(
                card = cardEntity.toDomain(),
                matchingSnippets = snippets,
                authoredUnitCount = allAuthored.size,
                incomingBacklinkCount = incomingCount
            )
        }

        // 2. Add remaining context matches not already present
        for ((sourceCardId, units) in unitsBySource) {
            if (!resultMap.containsKey(sourceCardId)) {
                val cardEntity = cardDao.getCardById(sourceCardId) ?: continue
                val snippets = units.map { it.plainText }
                val incomingCount = unitReferenceDao.countIncomingBacklinks(sourceCardId)
                val allAuthored = contextUnitDao.getUnitsForCard(sourceCardId)

                resultMap[sourceCardId] = SearchResultItem(
                    card = cardEntity.toDomain(),
                    matchingSnippets = snippets,
                    authoredUnitCount = allAuthored.size,
                    incomingBacklinkCount = incomingCount
                )
            }
        }

        return resultMap.values.toList()
    }

    private suspend fun searchUnitsMatchingText(query: String): List<ContextUnitEntity> {
        val allUnits = withContext(dispatchers.io) {
            // Search via query matching plainText
            val cards = cardDao.autocompleteCardsAll("", limit = 1000)
            val matches = mutableListOf<ContextUnitEntity>()
            for (card in cards) {
                val units = contextUnitDao.getUnitsForCard(card.id)
                for (unit in units) {
                    if (unit.plainText.contains(query, ignoreCase = true)) {
                        matches.add(unit)
                    }
                }
            }
            matches
        }
        return allUnits
    }
}
