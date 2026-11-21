package com.linkview.data.backup

import java.util.UUID

import androidx.room.withTransaction
import com.linkview.core.common.DispatcherProvider
import com.linkview.core.common.Result
import com.linkview.core.common.mention.MentionTokenizer
import com.linkview.core.common.runCatchingResult
import com.linkview.core.model.BackupCard
import com.linkview.core.model.BackupEnvelope
import com.linkview.core.model.BackupUnit
import com.linkview.core.model.Card
import com.linkview.core.model.CardCollection
import com.linkview.core.model.ImportMode
import com.linkview.core.model.ImportSummary
import com.linkview.data.database.LinkViewDatabase
import com.linkview.data.database.dao.CardDao
import com.linkview.data.database.dao.ContextUnitDao
import com.linkview.data.database.dao.UnitReferenceDao
import com.linkview.data.database.entity.CardEntity
import com.linkview.data.database.entity.CardTagEntity
import com.linkview.data.database.entity.ContextUnitEntity
import com.linkview.data.database.entity.UnitReferenceEntity
import com.linkview.domain.repository.BackupRepository
import com.linkview.domain.repository.PreferencesRepository
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val database: LinkViewDatabase,
    private val cardDao: CardDao,
    private val contextUnitDao: ContextUnitDao,
    private val unitReferenceDao: UnitReferenceDao,
    private val preferencesRepository: PreferencesRepository,
    private val serializer: JsonBackupSerializer,
    private val dispatchers: DispatcherProvider
) : BackupRepository {

    override suspend fun exportJson(): Result<String> = withContext(dispatchers.io) {
        runCatchingResult {
            val collectionId = preferencesRepository.getActiveCollectionId()
            val allCards = cardDao.getAllCardsList(collectionId)
            val backupCards = allCards.map { card ->
                val units = contextUnitDao.getUnitsForCard(card.id)
                val backupUnits = units.map { unit ->
                    val refs = MentionTokenizer.extractReferences(unit.rawText).toList()
                    BackupUnit(
                        id = unit.id,
                        sortOrder = unit.sortOrder,
                        rawText = unit.rawText,
                        references = refs
                    )
                }
                val domainCard = card.toDomain()
                BackupCard(
                    id = card.id,
                    title = card.title,
                    tags = domainCard.allTags,
                    type = card.type,
                    createdAt = card.createdAt,
                    updatedAt = card.updatedAt,
                    collectionId = card.collectionId,
                    contextUnits = backupUnits
                )
            }

            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val envelope = BackupEnvelope(
                schemaVersion = 1,
                exportedAt = isoFormat.format(Date()),
                cards = backupCards
            )

            serializer.encodeToString(envelope)
        }
    }

    override suspend fun exportHumanReadableJson(): Result<String> = withContext(dispatchers.io) {
        runCatchingResult {
            val collectionId = preferencesRepository.getActiveCollectionId()
            val allCards = cardDao.getAllCardsList(collectionId)
            val readableCards = allCards.map { card ->
                val units = contextUnitDao.getUnitsForCard(card.id)
                val domainCard = card.toDomain()
                com.linkview.core.model.ReadableCard(
                    title = card.title,
                    tags = domainCard.allTags,
                    units = units.map { it.rawText }
                )
            }
            serializer.encodeReadableCardsToString(readableCards)
        }
    }

    override suspend fun exportReferenceTitles(tags: List<String>): Result<List<String>> = withContext(dispatchers.io) {
        runCatchingResult {
            val collectionId = preferencesRepository.getActiveCollectionId()
            if (tags.isEmpty()) {
                cardDao.getAllTitles(collectionId)
            } else {
                cardDao.getTitlesByTags(collectionId, tags)
            }
        }
    }

    override suspend fun importJson(jsonContent: String, mode: ImportMode): Result<ImportSummary> = withContext(dispatchers.io) {
        runCatchingResult {
            val envelope = serializer.decodeFromString(jsonContent)
            require(envelope.schemaVersion == 1) {
                "Unsupported backup schema version: ${envelope.schemaVersion}"
            }

            val collectionId = preferencesRepository.getActiveCollectionId()
            var cardsImported = 0
            var unitsImported = 0
            var refsCreated = 0

            database.withTransaction {
                if (mode == ImportMode.REPLACE_ALL) {
                    cardDao.deleteAllCards(collectionId)
                }

                val now = System.currentTimeMillis()
                val titleToIdMap = mutableMapOf<String, String>()
                val processedTitles = mutableSetOf<String>()
                val skippedCardIds = mutableSetOf<String>()

                // Pass 1: Upsert all explicitly declared cards in envelope first
                for (card in envelope.cards) {
                    val cardTitle = card.title.trim()
                    if (cardTitle.isEmpty()) continue

                    val lowerTitle = cardTitle.lowercase()
                    val existingCard = cardDao.getCardByExactTitle(collectionId, cardTitle)

                    if (mode == ImportMode.SYNC_MERGE && existingCard != null) {
                        // Compare updatedAt: if local card is newer or equal, preserve local card
                        val incomingUpdatedAt = card.updatedAt
                        val localUpdatedAt = existingCard.updatedAt
                        if (incomingUpdatedAt > 0L && localUpdatedAt >= incomingUpdatedAt) {
                            titleToIdMap[lowerTitle] = existingCard.id
                            skippedCardIds.add(existingCard.id)
                            continue
                        }
                    }

                    val cardId = when {
                        card.id.isNotBlank() -> card.id
                        existingCard != null -> existingCard.id
                        titleToIdMap.containsKey(lowerTitle) -> titleToIdMap[lowerTitle]!!
                        else -> UUID.randomUUID().toString()
                    }
                    titleToIdMap[lowerTitle] = cardId

                    val createdAt = if (card.createdAt > 0L) card.createdAt else (existingCard?.createdAt ?: now)
                    val updatedAt = if (card.updatedAt > 0L) card.updatedAt else now
                    val domainCard = Card(
                        id = cardId,
                        title = cardTitle,
                        tags = card.allTags,
                        type = card.type?.trim()?.ifBlank { null },
                        createdAt = createdAt,
                        updatedAt = updatedAt,
                        collectionId = collectionId
                    )
                    cardDao.upsertCard(CardEntity.fromDomain(domainCard))
                    if (processedTitles.add(lowerTitle)) {
                        cardsImported++
                    }

                    // Sync tags
                    cardDao.deleteCardTagsForCard(cardId)
                    val tagEntities = domainCard.allTags.map { CardTagEntity(cardId = cardId, tag = it) }
                    if (tagEntities.isNotEmpty()) {
                        cardDao.insertCardTags(tagEntities)
                    }

                    if (mode == ImportMode.MERGE || mode == ImportMode.SYNC_MERGE) {
                        contextUnitDao.deleteUnitsForCard(cardId)
                    }
                }

                // Pass 2: Insert context units and wire cross-references
                for (card in envelope.cards) {
                    val cardTitle = card.title.trim()
                    if (cardTitle.isEmpty()) continue
                    val cardId = titleToIdMap[cardTitle.lowercase()] ?: card.id.ifBlank { null } ?: continue

                    // If local card was newer in SYNC_MERGE, keep local units and skip overwriting from file
                    if (skippedCardIds.contains(cardId)) {
                        continue
                    }

                    // Support both structured contextUnits and shorthand units: List<String>
                    val rawUnits = when {
                        card.contextUnits.isNotEmpty() -> card.contextUnits.mapIndexed { idx, u ->
                            val uId = if (u.id.isNotBlank()) u.id else UUID.randomUUID().toString()
                            u.copy(id = uId, sortOrder = idx)
                        }
                        card.units.isNotEmpty() -> card.units.mapIndexed { idx, text ->
                            com.linkview.core.model.BackupUnit(
                                id = UUID.randomUUID().toString(),
                                sortOrder = idx,
                                rawText = text
                            )
                        }
                        else -> emptyList()
                    }

                    if (rawUnits.isNotEmpty()) {
                        val unitEntities = rawUnits.map { unit ->
                            ContextUnitEntity(
                                id = unit.id,
                                sourceCardId = cardId,
                                sortOrder = unit.sortOrder,
                                rawText = unit.rawText,
                                plainText = MentionTokenizer.toPlainText(unit.rawText)
                            )
                        }
                        contextUnitDao.insertUnits(unitEntities)
                        unitsImported += unitEntities.size

                        val references = mutableListOf<UnitReferenceEntity>()
                        for (unit in rawUnits) {
                            val extracted = MentionTokenizer.extractReferences(unit.rawText)
                            val allRefs = (extracted + unit.references).distinct()
                            for (targetRef in allRefs) {
                                val cleanTarget = targetRef.trim()
                                if (cleanTarget.isEmpty()) continue

                                var targetCard = cardDao.getCardById(cleanTarget)?.takeIf { it.collectionId == collectionId }
                                    ?: cardDao.getCardByExactTitle(collectionId, cleanTarget)
                                    ?: titleToIdMap[cleanTarget.lowercase()]?.let { cardDao.getCardById(it) }

                                if (targetCard == null) {
                                    val stubId = UUID.randomUUID().toString()
                                    val stub = CardEntity(
                                        id = stubId,
                                        title = cleanTarget,
                                        type = null,
                                        tagsCsv = "",
                                        createdAt = now,
                                        updatedAt = now,
                                        collectionId = collectionId
                                    )
                                    cardDao.upsertCard(stub)
                                    titleToIdMap[cleanTarget.lowercase()] = stubId
                                    targetCard = stub
                                    if (processedTitles.add(cleanTarget.lowercase())) {
                                        cardsImported++
                                    }
                                }

                                references.add(
                                    UnitReferenceEntity(
                                        unitId = unit.id,
                                        targetCardId = targetCard.id
                                    )
                                )
                            }
                        }
                        if (references.isNotEmpty()) {
                            unitReferenceDao.insertReferences(references)
                            refsCreated += references.size
                        }
                    }
                }
            }

            ImportSummary(
                cardsImported = cardsImported,
                unitsImported = unitsImported,
                referencesCreated = refsCreated
            )
        }
    }
}
