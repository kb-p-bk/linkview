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

                // Pass 1: Upsert all explicitly declared cards in envelope first
                for (card in envelope.cards) {
                    val cardTitle = card.title.trim()
                    if (cardTitle.isEmpty()) continue

                    val lowerTitle = cardTitle.lowercase()
                    val existingCard = cardDao.getCardByExactTitle(collectionId, cardTitle)

                    val cardId = when {
                        existingCard != null -> existingCard.id
                        card.id.isNotBlank() -> card.id
                        titleToIdMap.containsKey(lowerTitle) -> titleToIdMap[lowerTitle]!!
                        else -> UUID.randomUUID().toString()
                    }
                    titleToIdMap[lowerTitle] = cardId

                    val mergedTags: List<String>
                    val mergedType: String?
                    val createdAt: Long
                    val updatedAt: Long

                    if (mode == ImportMode.SYNC_MERGE && existingCard != null) {
                        val localTags = existingCard.toDomain().allTags
                        val incomingTags = card.allTags
                        mergedTags = (localTags + incomingTags).distinct()
                        mergedType = existingCard.type?.ifBlank { null } ?: card.type?.trim()?.ifBlank { null }
                        createdAt = if (existingCard.createdAt > 0L) existingCard.createdAt else (if (card.createdAt > 0L) card.createdAt else now)
                        updatedAt = maxOf(existingCard.updatedAt, card.updatedAt, now)
                    } else {
                        mergedTags = card.allTags
                        mergedType = card.type?.trim()?.ifBlank { null }
                        createdAt = if (card.createdAt > 0L) card.createdAt else (existingCard?.createdAt ?: now)
                        updatedAt = if (card.updatedAt > 0L) card.updatedAt else now
                    }

                    val domainCard = Card(
                        id = cardId,
                        title = cardTitle,
                        tags = mergedTags,
                        type = mergedType,
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

                    if (mode == ImportMode.MERGE) {
                        contextUnitDao.deleteUnitsForCard(cardId)
                    }
                }

                // Pass 2: Insert context units and wire cross-references
                for (card in envelope.cards) {
                    val cardTitle = card.title.trim()
                    if (cardTitle.isEmpty()) continue
                    val cardId = titleToIdMap[cardTitle.lowercase()] ?: card.id.ifBlank { null } ?: continue

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

                    val unitsToProcess = if (mode == ImportMode.SYNC_MERGE) {
                        val existingUnits = contextUnitDao.getUnitsForCard(cardId)
                        if (existingUnits.isEmpty()) {
                            rawUnits
                        } else {
                            val existingIds = existingUnits.map { it.id }.toSet()
                            val existingTexts = existingUnits.map { it.rawText.trim() }.toSet()
                            var nextSortOrder = (existingUnits.maxOfOrNull { it.sortOrder } ?: -1) + 1

                            val newUnits = mutableListOf<com.linkview.core.model.BackupUnit>()
                            for (incoming in rawUnits) {
                                val trimmed = incoming.rawText.trim()
                                val matchesExisting = existingIds.contains(incoming.id) || existingTexts.contains(trimmed)
                                if (!matchesExisting) {
                                    val newId = if (incoming.id.isNotBlank() && !existingIds.contains(incoming.id)) {
                                        incoming.id
                                    } else {
                                        UUID.randomUUID().toString()
                                    }
                                    newUnits.add(
                                        incoming.copy(
                                            id = newId,
                                            sortOrder = nextSortOrder++
                                        )
                                    )
                                }
                            }
                            newUnits
                        }
                    } else {
                        rawUnits
                    }

                    if (unitsToProcess.isNotEmpty()) {
                        val unitEntities = unitsToProcess.map { unit ->
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
                        for (unit in unitsToProcess) {
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
